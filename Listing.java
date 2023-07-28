import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Scanner;
import java.time.format.DateTimeParseException;

public class Listing {
  public static ArrayList<String> listingTypes = new ArrayList<String>(
      Arrays.asList("house", "apartment", "guesthouse", "hotel", "room"));
  private static double defaultDistance = 10;

  /*
   * int sin, String type, double lat, double lon, String postalcode, String city,
   * String country, ArrayList<String> amenities, ArrayList<DateCost>
   * availabilityList
   */
  public static void createListing(Scanner scanner) throws ClassNotFoundException, SQLException {
    System.out.print("Enter the SIN of the host: ");
    int sin = scanner.nextInt();
    scanner.nextLine();
    if (!SqlDAO.getInstance().checkUserExists(sin)) {
      System.out.println("INVALID USER SIN");
      return;
    }

    System.out.println();
    boolean valid = false;
    String type = "";
    while (!valid) {
      System.out.print("Enter the type of listing " + listingTypes + " : ");
      type = scanner.nextLine();
      if (listingTypes.contains(type.toLowerCase())) {
        valid = true;
      }
    }

    System.out.println();
    System.out.print("Enter the latitude of the listing: ");
    double lat = scanner.nextDouble();
    scanner.nextLine();

    System.out.println();
    System.out.print("Enter the longitude of the listing: ");
    double lon = scanner.nextDouble();
    scanner.nextLine();

    System.out.println();
    System.out.print("Enter the postal code: ");
    String postal = scanner.nextLine();

    System.out.println();
    System.out.print("Enter your city: ");
    String city = scanner.nextLine();

    System.out.println();
    System.out.print("Enter your country: ");
    String country = scanner.nextLine();

    boolean done = false;
    HashSet<String> amenities = new HashSet<String>(); // no duplicates
    while (!done) {
      System.out.println("Select amenities from the list below for your listing: (exit: e) ");
      showAmenities();
      String amenity = scanner.nextLine();
      if (amenity.equals("e") || amenity.equals("exit")) {
        done = true;
        break;
      }
      if (SqlDAO.getInstance().checkAmenityExists(amenity)) {
        amenities.add(amenity);
      } else {
        System.out.println("This amenity does not exist");
      }
    }

    done = false;
    // dont want duplicate date entries
    ArrayList<DateCost> availabilities = new ArrayList<DateCost>();
    int numInserted = 0;
    while (!done) {
      boolean invalid = false;

      System.out.println();
      System.out.print("Enter the starting date of the range (YYYY-MM-DD): ");
      String start = scanner.next();

      System.out.println();
      System.out.print("Enter the end date of the range (YYYY-MM-DD): ");
      String end = scanner.next();

      System.out.println();
      System.out.print("Enter cost per day: ");
      double cost = scanner.nextDouble();
      LocalDate startDate = null, endDate = null;
      scanner.nextLine();
      try {
        startDate = LocalDate.parse(start, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        endDate = LocalDate.parse(end, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
      } catch (DateTimeParseException e) {
        System.out.println("Wrong date format! Correct format is YYYY-MM-DD!");
        invalid = true;
      }

      // check for overlapping / duplicate date entries
      for (DateCost dc : availabilities) {
        String s = dc.getStartDate().toString();
        String e = dc.getEndDate().toString();
        if (isDateRangeValid(s, e) && isDateInRange(s, e, start, end)) {
          System.out
              .println("Date range conflicts with existing date range: " + dc.getStartDate() + " - " + dc.getEndDate());
          invalid = true;
          break;
        }
      }

      if (!invalid) {
        // Insert a date into the database for each date in the given start-end range
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        LocalDate rangeStart = startDate;
        LocalDate rangeEnd = endDate;

        availabilities.add(
            new DateCost(Date.valueOf(rangeStart.format(formatter)), Date.valueOf(rangeEnd.format(formatter)), cost));

        numInserted += 1;

        // Give chance to stop inserting:
        System.out.println("Do you want to continue entering dates? (c) or quit? (q): ");
        String choice = scanner.nextLine();
        while (!(choice.equals("c") || choice.equals("q"))) {
          System.out.println("Please enter a valid choice (c/q): ");
          choice = scanner.nextLine();
        }
        if (choice.equals("q") && numInserted > 0) {
          done = true;
        } else if (choice.equals("q") && numInserted <= 0) {
          System.out.println("Sorry, you need to insert at least one valid date range");
        } else {
          System.out.println("Continue entering dates...");
        }
      }
    }

    // Now done inserting all available days in list, can insert the listing
    SqlDAO.getInstance().insertListing(sin, type, lat, lon, postal, city, country, amenities, availabilities);
  }

  public static void showAmenities() throws ClassNotFoundException, SQLException {
    ResultSet rs = SqlDAO.getInstance().getAmenities();
    int index = 0;
    // Extract data from result set
    while (rs.next()) {
      // Retrieve by column name
      String amenity = rs.getString("amenity_name");
      // Display values
      System.out.format("%-30s", amenity);
      index++;
      if (index % 4 == 0) {
        System.out.println();
      }
    }
    System.out.println();
    rs.close();
  }

  public static boolean isDateInRange(String start, String end, String startDateString, String endDateString) {
    LocalDate existingStart = LocalDate.parse(start);
    LocalDate existingEnd = LocalDate.parse(end);
    LocalDate startDate = LocalDate.parse(startDateString);
    LocalDate endDate = LocalDate.parse(endDateString);

    return existingStart.isEqual(startDate) || existingStart.isEqual(endDate)
        || (existingStart.isAfter(startDate) && existingStart.isBefore(endDate))
        || existingEnd.isEqual(startDate) || existingEnd.isEqual(endDate)
        || (existingEnd.isAfter(startDate) && existingEnd.isBefore(endDate));
  }

  public static boolean isDateRangeValid(String start, String end) {
    LocalDate startDate = LocalDate.parse(start);
    LocalDate endDate = LocalDate.parse(end);
    return startDate.equals(endDate) || startDate.isBefore(endDate);
  }

  public static ArrayList<ListingObject> getListingSet() throws ClassNotFoundException, SQLException {
    ResultSet rs = SqlDAO.getInstance().getListings();
    ArrayList<ListingObject> listings = new ArrayList<ListingObject>();
    while (rs.next()) {
      ListingObject listing = new ListingObject(rs.getInt("listing_id"),
          -1.0,
          rs.getDouble("latitude"),
          rs.getDouble("longitude"),
          -1.0,
          rs.getString("type"),
          rs.getString("postal_code"),
          rs.getString("city"),
          rs.getString("country"));
      listings.add(listing);
    }
    return listings;
  }

  public static void printListingSet(ArrayList<ListingObject> listings) {
    System.out.format("%1$-10s%2$-10s%3$-15s%4$-15s%5$-15s%6$-15s%7$-15s%8$-15s%9$-15s%10$-15s\n",
        "Distance", "Cost", "Start", "End", "Type", "City", "Postal code", "Country", "Latitude", "Longitude");
    for (ListingObject listing : listings) {
      System.out.println(listing.toString());
    }
  }

  public static void listingSearch(Scanner scanner) throws ClassNotFoundException, SQLException {
    String input = "";

    ArrayList<InputKey> filters = new ArrayList<InputKey>();
    filters.add(new InputKey("d", "distance"));
    filters.add(new InputKey("p", "postal code"));
    filters.add(new InputKey("t", "type"));
    filters.add(new InputKey("c", "city"));
    filters.add(new InputKey("co", "country"));
    filters.add(new InputKey("w", "window of availability"));
    filters.add(new InputKey("pr", "price range"));

    ArrayList<InputKey> sortings = new ArrayList<InputKey>();
    sortings.add(new InputKey("1", "ascending distance"));
    sortings.add(new InputKey("2", "descending distance"));
    sortings.add(new InputKey("3", "ascending cost"));
    sortings.add(new InputKey("4", "descending cost"));

    // Filters can stack, sorting methods cannot
    // Filter: by price range, by amenities, by window of availability, by exact
    // address [c,a,w,e]
    // Sort: distance, price
    ArrayList<ListingObject> listings = getListingSet();
    getNextAvailableCost(listings);

    while (!input.equals("q")) {
      System.out.println("You have the following filters active:");
      String msg = "  ";
      for (InputKey filter : filters) {
        if (filter.isPressed())
          msg = msg + filter.getDescription() + ", ";
      }
      System.out.println(msg.substring(0, msg.length() - 2));

      System.out.println("Press the button to select/deselect filters, [q] to continue:");
      for (InputKey filter : filters) {
        System.out.println("[" + filter.getKey() + "] to search by " + filter.getDescription());
      }
      input = scanner.nextLine();
      for (InputKey filter : filters) {
        if (input.toLowerCase().equals(filter.getKey() + "")) {
          filter.setPressed(!filter.isPressed());
        }
      }
      System.out.println();
    }

    for (InputKey filter : filters) {
      if (filter.isPressed()) {
        switch (filter.getDescription()) {
          case "distance":
            getDistance(scanner, listings);
            listings = filterByDist(scanner, listings);
            break;
          case "postal code":
            listings = filterByPostal(scanner, listings);
            break;
          case "type":
            listings = filterByType(scanner, listings);
            break;
          case "city":
            listings = filterByCity(scanner, listings);
            break;
          case "country":
            listings = filterByCountry(scanner, listings);
            break;
          case "window of availability":
            listings = filterByAvailability(scanner, listings);
            break;
          case "price range":
            listings = filterByPriceRange(scanner, listings);
            break;
          default:
            System.out.println("Invalid operation");
        }
        System.out.println();
      }
    }
    System.out.println();
    if (listings.size() == 0) {
      System.out.println("No results were found.");
      return;
    } else {
      printListingSet(listings);
    }
    System.out.println();

    input = "";
    String sortingDesc = "";
    while (!input.equals("q")) {
      System.out.println("How would you like to sort? [q] to quit: ");
      for (InputKey sorting : sortings) {
        System.out.println("[" + sorting.getKey() + "] to sort by " + sorting.getDescription());
      }
      input = scanner.nextLine();
      for (InputKey sorting : sortings) {
        if (input.toLowerCase().equals(sorting.getKey() + ""))
          sortingDesc = sorting.getDescription();
      }

      switch (sortingDesc) {
        case "ascending distance":
          sortByDistLow(listings);
          break;
        case "descending distance":
          sortByDistHigh(listings);
          break;
        case "ascending cost":
          sortByCostLow(listings);
          break;
        case "descending cost":
          sortByCostHigh(listings);
          break;
        default:
          System.out.println();
          System.out.println("Invalid operation");
      }
      System.out.println();
      printListingSet(listings);
      System.out.println();
    }
  }

  public static ArrayList<ListingObject> filterByAvailability(Scanner scanner, ArrayList<ListingObject> listings) throws ClassNotFoundException, SQLException{
    LocalDate startDate, endDate;
    while (true) {
      System.out.print("Enter the start date of the range (YYYY-MM-DD): ");
      String start = scanner.next();
      scanner.nextLine();

      System.out.print("Enter the end date of the range (YYYY-MM-DD): ");
      String end = scanner.next();
      scanner.nextLine();
      
      try {
        startDate = LocalDate.parse(start, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        endDate = LocalDate.parse(end, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        break;
      } catch (DateTimeParseException e) {
        System.out.println("Wrong date format! Correct format is YYYY-MM-DD!");
      }
    }

    getAvailabilitiesByRange(listings, startDate.toString(), endDate.toString());

    // If there exists a period that encloses the given dates, update price and date
    // to enclosing date other wise change cost to -1
    ArrayList<ListingObject> filtered = new ArrayList<>();
    for (ListingObject listing : listings) {
      if (!listing.getStartDate().equalsIgnoreCase("Null"))
        filtered.add(listing);
    }
    return filtered;
  }

  public static void getAvailabilitiesByRange(ArrayList<ListingObject> listings, String start, String end) throws ClassNotFoundException, SQLException{
    SqlDAO dao = SqlDAO.getInstance();
    ResultSet rs;
    String startDate, endDate;
    double cost;

    for (ListingObject listing : listings) {
      rs = dao.getRangeContainsDate(listing.getId(), start, end);
      startDate = "Null";
      endDate = "Null";
      cost = -1;
      if(rs.next()){
        startDate = rs.getString("start");
        endDate = rs.getString("end");
        cost = rs.getDouble("cost");
      }
      listing.setCost(cost);
      listing.setStartDate(startDate);
      listing.setEndDate(endDate);
    }
  }

  public static ArrayList<ListingObject> filterByPriceRange(Scanner scanner, ArrayList<ListingObject> listings) {
    System.out.print("Enter min price: ");
    double min = scanner.nextDouble();
    scanner.nextLine();

    System.out.print("Enter max price: ");
    double max = scanner.nextDouble();
    scanner.nextLine();

    ArrayList<ListingObject> filtered = new ArrayList<>();
    for (ListingObject listing : listings) {
      if (listing.getCost() <= max && listing.getCost() >= min)
        filtered.add(listing);
    }
    return filtered;
  }

  public static ArrayList<ListingObject> filterByType(Scanner scanner, ArrayList<ListingObject> listings) {
    System.out.print("Enter listing type " + listingTypes + ": ");
    String input = scanner.nextLine();

    ArrayList<ListingObject> filtered = new ArrayList<>();
    for (ListingObject listing : listings) {
      if (listing.getType().equalsIgnoreCase(input))
        filtered.add(listing);
    }
    return filtered;
  }

  public static ArrayList<ListingObject> filterByCity(Scanner scanner, ArrayList<ListingObject> listings) {
    System.out.print("Enter listing city: ");
    String input = scanner.nextLine();

    ArrayList<ListingObject> filtered = new ArrayList<>();
    for (ListingObject listing : listings) {
      if (listing.getCity().equalsIgnoreCase(input))
        filtered.add(listing);
    }
    return filtered;
  }

  public static ArrayList<ListingObject> filterByCountry(Scanner scanner, ArrayList<ListingObject> listings) {
    System.out.print("Enter listing country: ");
    String input = scanner.nextLine();

    ArrayList<ListingObject> filtered = new ArrayList<>();
    for (ListingObject listing : listings) {
      if (listing.getCountry().equalsIgnoreCase(input))
        filtered.add(listing);
    }
    return filtered;
  }

  /*
   * Asks user for latitude, longitude using scanner, then updates each the
   * distance of listing in listings
   */
  public static void getDistance(Scanner scanner, ArrayList<ListingObject> listings)
      throws ClassNotFoundException, SQLException {
    System.out.print("Enter the latitude: ");
    double lat = scanner.nextDouble();
    scanner.nextLine();

    System.out.print("Enter the longitude: ");
    double lon = scanner.nextDouble();
    scanner.nextLine();

    for (ListingObject listing : listings) {
      double d = distanceBetween(lat, lon, listing.getLat(), listing.getLon());
      listing.setDist(d);
    }
  }

  /*
   * Asks the user for a distance using scanner and returns a filtered list of
   * listings where
   * any listing farther than distance is removed
   */
  public static ArrayList<ListingObject> filterByDist(Scanner scanner, ArrayList<ListingObject> listings) {
    System.out.print("Enter distance(km): ");
    String input = scanner.nextLine();

    double distance;
    try {
      distance = Integer.parseInt(input);
    } catch (NumberFormatException e) {
      System.out.println("Number not detected. Default value of " + defaultDistance + "km used instead.");
      System.out.println();
      distance = defaultDistance;
    }

    ArrayList<ListingObject> filtered = new ArrayList<ListingObject>();
    for (ListingObject listing : listings) {
      if (listing.getDist() <= distance) {
        filtered.add(listing);
      }
    }
    return filtered;
  }

  /*
   * Sorts the listings by cost from lowest to highest
   */
  public static void sortByDistLow(ArrayList<ListingObject> listings) {
    Collections.sort(listings, new Comparator<ListingObject>() {
      @Override
      public int compare(ListingObject l1, ListingObject l2) {
        if (l1.getDist() > l2.getDist())
          return 1;
        if (l1.getDist() == l2.getDist())
          return 0;
        return -1;
      }
    });
  }

  /*
   * Sorts the listings by distance from highest to lowest
   */
  public static void sortByDistHigh(ArrayList<ListingObject> listings) {
    Collections.sort(listings, new Comparator<ListingObject>() {
      @Override
      public int compare(ListingObject l1, ListingObject l2) {
        if (l1.getDist() > l2.getDist())
          return -1;
        if (l1.getDist() == l2.getDist())
          return 0;
        return 1;
      }
    });
  }

  /*
   * Asks user for a date using scanner, then updates the cost of each listing in
   * listings
   */
  public static void getCost(Scanner scanner, ArrayList<ListingObject> listings)
      throws SQLException, ClassNotFoundException {
    // Ask user for a date
    System.out.print("\nEnter a date(YYYY-MM-DD): ");
    String input = scanner.nextLine();
    Date date;
    try {
      date = Date.valueOf(LocalDate.parse(input, DateTimeFormatter.ofPattern("yyyy-MM-dd")));
    } catch (DateTimeParseException e) {
      System.out.println("Wrong date format! Today is used instead.");
      date = Date.valueOf(LocalDate.now(ZoneId.of("America/Toronto")));
    }
    getCostOfDate(listings, date);
  }

  /*
   * Adds the cost of listing on the day date to all listings
   */
  public static void getCostOfDate(ArrayList<ListingObject> listings, Date date)
      throws SQLException, ClassNotFoundException {
    SqlDAO dao = SqlDAO.getInstance();
    ResultSet rs;
    double cost;
    String datestrString = date.toString();

    for (ListingObject listing : listings) {
      rs = dao.getRangeOverlapFromDate(listing.getId(), datestrString, datestrString);
      cost = -1;
      if (rs.next()) {
        cost = rs.getDouble("cost");
      }
      listing.setCost(cost);
    }
  }

  public static void getNextAvailableCost(ArrayList<ListingObject> listings)
      throws SQLException, ClassNotFoundException {
    SqlDAO dao = SqlDAO.getInstance();
    ResultSet rs;

    for (ListingObject listing : listings) {
      rs = dao.getAvailabilitiesFromListing(listing.getId());
      if (rs.next()) {
        listing.setCost(rs.getDouble("cost"));
        listing.setStartDate(rs.getString("start"));
        listing.setEndDate(rs.getString("end"));
      }
    }
  }

  /*
   * Sorts the listings by cost from lowest to highest
   */
  public static void sortByCostLow(ArrayList<ListingObject> listings) {
    Collections.sort(listings, new Comparator<ListingObject>() {
      @Override
      public int compare(ListingObject l1, ListingObject l2) {
        if (l1.getCost() < 0)
          return 1;
        if (l1.getCost() > l2.getCost())
          return 1;
        if (l1.getCost() == l2.getCost())
          return 0;
        return -1;
      }
    });
  }

  /*
   * Sorts the listings by cost from highest to lowest
   */
  public static void sortByCostHigh(ArrayList<ListingObject> listings) throws SQLException, ClassNotFoundException {
    Collections.sort(listings, new Comparator<ListingObject>() {
      @Override
      public int compare(ListingObject l1, ListingObject l2) {
        if (l1.getCost() < l2.getCost())
          return 1;
        if (l1.getCost() == l2.getCost())
          return 0;
        return -1;
      }
    });
  }

  /*
   * Asks the user for postal code using scanner, then filters listings by FSA
   * code
   * 
   * Idk how postal works in other countries, this prob only works for canada
   */
  public static ArrayList<ListingObject> filterByPostal(Scanner scanner, ArrayList<ListingObject> listings)
      throws SQLException, ClassNotFoundException {
    System.out.print("Enter postal code: ");
    String input = scanner.nextLine();
    String fsa = input.substring(0, 3).toUpperCase();

    ArrayList<ListingObject> filtered = new ArrayList<ListingObject>();
    for (ListingObject listing : listings) {
      if (listing.getPos().contains(fsa))
        filtered.add(listing);
    }
    return filtered;
  }

  /*
   * Returns the distance in km between lat1, lon1 and lat2, lon2 using the
   * Haversine formula
   */
  public static double distanceBetween(double lat1, double lon1, double lat2, double lon2) {
    double rlat1 = Math.toRadians(lat1);
    double rlat2 = Math.toRadians(lat2);
    double radius = 6371;

    double dlat = rlat2 - rlat1;
    double dlon = Math.toRadians(lon2 - lon1);

    double a = Math.sin(dlat / 2) * Math.sin(dlat / 2) + Math.cos(rlat1) * Math.cos(rlat2) *
        Math.sin(dlon / 2) * Math.sin(dlon / 2);
    double c = Math.asin(Math.sqrt(a));
    return 2 * c * radius;
  }

  public static void updateListing(Scanner scanner) throws ClassNotFoundException, SQLException {
    // Before any updates, need to check that the given availability is not booked
    // Get the sin of the host and display all their listings
    System.out.print("Enter the SIN of the host: ");
    int sin = scanner.nextInt();
    scanner.nextLine();
    if (!SqlDAO.getInstance().checkUserExists(sin)) {
      System.out.println("INVALID USER SIN");
      return;
    }
    ArrayList<Integer> listings = displayUserListings(sin);

    if (listings.isEmpty()) {
      System.out.println("This user is not hosting any listings");
      return;
    }

    // User selects listing they want to update by id
    System.out.println("Select the listing to update (by id): ");
    int listing = scanner.nextInt();
    scanner.nextLine();
    if (!listings.contains(listing)) {
      System.out.println("This user does not have the listing: id = " + listing);
      return;
    }

    // Can display the availability / price for the current listing (try to merge
    // the range / cost to be easier to display)
    displayListingAvailabilities(listing);

    // User inputs date range and price, if existing date, then update price if not
    // booked
    // if not existing date, then insert new availability
    // If overlapping date range, delete overlap and replace with new range and cost
    boolean done = false;
    while (!done) {
      boolean invalid = false;

      System.out.println();
      System.out.print("Enter the starting date of the range (YYYY-MM-DD): ");
      String start = scanner.next();

      System.out.println();
      System.out.print("Enter the end date of the range (YYYY-MM-DD): ");
      String end = scanner.next();

      System.out.println();
      System.out.print("Enter cost per day: ");
      double cost = scanner.nextDouble();
      LocalDate startDate = null, endDate = null;
      scanner.nextLine();
      try {
        startDate = LocalDate.parse(start, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        endDate = LocalDate.parse(end, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
      } catch (DateTimeParseException e) {
        System.out.println("Wrong date format! Correct format is YYYY-MM-DD!");
        continue;
      }

      if (!isDateRangeValid(start, end)) {
        System.out.println("Invalid date range!");
        continue;
      }

      if (!invalid) {
        // Overlapping dates if start within range or end within range

        ResultSet rs = SqlDAO.getInstance().getRangeOverlapFromDate(listing, start, end);
        Date first_start = null, last_end = null;
        double first_cost = 0, last_cost = 0;
        boolean booked = false;
        boolean atLeastOne = false;

        while (rs.next()) {
          if (rs.isFirst()) {
            first_start = rs.getDate("start");
            first_cost = rs.getDouble("cost");
            // System.out.println("First: " + first_start);
          }
          if (rs.isLast()) {
            last_end = rs.getDate("end");
            last_cost = rs.getDouble("cost");
            // System.out.println("Last: " + last_end);
          }
          if (!rs.getBoolean("availability")) {
            System.out.println("One of the dates in the range is booked");
            booked = true;
            break;
          }
          atLeastOne = true;
        }
        if (booked) {
          rs.close();
          continue;
        }
        rs.close();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        SqlDAO.getInstance().deleteRangeOverlapFromDate(listing, start, end);
        if (atLeastOne) {
          if (Date.valueOf(startDate.format(formatter)).after(first_start))
            SqlDAO.getInstance().insertAvailability(listing,
                new DateCost(first_start, Date.valueOf(startDate.minusDays(1).format(formatter)), first_cost));
          if (Date.valueOf(endDate.format(formatter)).before(last_end))
            SqlDAO.getInstance().insertAvailability(listing,
                new DateCost(Date.valueOf(endDate.plusDays(1).format(formatter)), last_end, last_cost));
        }
        SqlDAO.getInstance().insertAvailability(listing,
            new DateCost(Date.valueOf(startDate.format(formatter)), Date.valueOf(endDate.format(formatter)), cost));

        displayListingAvailabilities(listing);

        // Give chance to stop inserting:
        System.out.println("Do you want to continue entering dates? (c) or quit? (q): ");
        String choice = scanner.nextLine();
        while (!(choice.equals("c") || choice.equals("q"))) {
          System.out.println("Please enter a valid choice (c/q): ");
          choice = scanner.nextLine();
        }
        if (choice.equals("q")) {
          done = true;
        } else {
          System.out.println("Continue entering dates...");
        }
      }
    }
  }

  public static ArrayList<Integer> displayUserListings(int sin) throws ClassNotFoundException, SQLException {
    System.out.println();
    ResultSet rs = SqlDAO.getInstance().getListingsFromUser(sin);
    System.out.println("Listings: ");
    int index = 0;
    ArrayList<Integer> listingIds = new ArrayList<Integer>();
    // Extract data from result set
    while (rs.next()) {
      // Retrieve by column name
      int listing = rs.getInt("listing_id");
      listingIds.add(listing);
      // Display values
      System.out.format("id: %-5s", listing);
      index++;
      if (index % 4 == 0) {
        System.out.println();
      }
    }
    rs.close();
    System.out.println();
    System.out.println();
    return listingIds;
  }

  public static void displayListingAvailabilities(int listing_id) throws ClassNotFoundException, SQLException {
    System.out.println();
    ResultSet rs = SqlDAO.getInstance().getAvailabilitiesFromListing(listing_id);
    System.out.println("Availabilities for listing_id " + listing_id + ": ");
    int index = 0;
    // Extract data from result set
    while (rs.next()) {
      // Retrieve by column name
      String start = rs.getString("start");
      String end = rs.getString("end");
      double cost = rs.getDouble("cost");
      boolean available = rs.getBoolean("availability");
      // Display values
      System.out.format("(%-50s", start + ", " + end + ", $" + cost + ", Booked: " + !available + ")");
      index++;
      if (index % 3 == 0) {
        System.out.println();
      }
    }
    rs.close();
    System.out.println();
  }

}
