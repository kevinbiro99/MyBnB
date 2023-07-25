import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Scanner;
import java.time.format.DateTimeParseException;

public class Listing {
  public static ArrayList<String> listingTypes = new ArrayList<String>(Arrays.asList("house", "apartment", "guesthouse", "hotel", "room"));
  private static double defaultDistance = 10;
  
  /*
   * int sin, String type, double lat, double lon, String postalcode, String city, 
                            String country, ArrayList<String> amenities, ArrayList<DateCost> availabilityList
   */
  public static void createListing(Scanner scanner) throws ClassNotFoundException, SQLException {
    System.out.print("Enter the SIN of the host: ");
    int sin = scanner.nextInt();
    scanner.nextLine();
    if (!SqlDAO.getInstance().checkUserExists(sin)){
      System.out.println("INVALID USER SIN");
      return;
    }
    
    System.out.println();
    boolean valid = false;
    String type = "";
    while (!valid) {
      System.out.print("Enter the type of listing "+ listingTypes + " : ");
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
      }
      catch (DateTimeParseException e) {
        System.out.println("Wrong date format! Correct format is YYYY-MM-DD!");
        invalid = true;
      }
      
      // check for overlapping / duplicate date entries
      for (DateCost dc : availabilities) {
        String s = dc.getStartDate().toString();
        String e = dc.getEndDate().toString();
        if (isDateRangeValid(s,e) && isDateInRange(s, e, start, end)) {
          System.out.println("Date range conflicts with existing date range: " + dc.getStartDate() + " - " + dc.getEndDate());
          invalid = true;
          break;
        }
      }

      if (!invalid) {
        // Insert a date into the database for each date in the given start-end range
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        LocalDate rangeStart = startDate;
        LocalDate rangeEnd = endDate;
        
        availabilities.add(new DateCost(Date.valueOf(rangeStart.format(formatter)), Date.valueOf(rangeEnd.format(formatter)), cost));
        
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
    while(rs.next()){
      //Retrieve by column name
      String amenity = rs.getString("amenity_name");
      //Display values
      System.out.format("%-30s", amenity);
      index ++;
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

    return existingStart.isEqual(startDate) || existingStart.isEqual(endDate) || (existingStart.isAfter(startDate) && existingStart.isBefore(endDate))
            || existingEnd.isEqual(startDate) || existingEnd.isEqual(endDate) || (existingEnd.isAfter(startDate) && existingEnd.isBefore(endDate));
  }

  public static boolean isDateRangeValid(String start, String end) {
    LocalDate startDate = LocalDate.parse(start);
    LocalDate endDate = LocalDate.parse(end);
    return startDate.equals(endDate) || startDate.isBefore(endDate);
  }

  public static ArrayList<ListingDist> getListingSet() throws ClassNotFoundException, SQLException{
    ResultSet rs = SqlDAO.getInstance().getListings();
    ArrayList<ListingDist> listings = new ArrayList<ListingDist>();
    while(rs.next()){
      ListingDist listing = new ListingDist(rs.getString("type"), 
                                    rs.getDouble("latitude"),
                                    rs.getDouble("longitude"),
                                    rs.getString("postal_code"),
                                    rs.getString("city"),
                                    rs.getString("country"),
                                    0);
      listings.add(listing);
    }
    return listings;
  }

  /*
   * Given a location and distance by user using scanner, prints all listing within the distance
   */
  public static void showListingsNear(Scanner scanner) throws ClassNotFoundException, SQLException{
    System.out.print("Enter the latitude: ");
    double lat = scanner.nextDouble();
    scanner.nextLine();

    System.out.println();
    System.out.print("Enter the longitude: ");
    double lon = scanner.nextDouble();
    scanner.nextLine();

    System.out.println();
    System.out.print("Use default distaince("+defaultDistance+"km)? Y/N: ");
    String input = scanner.nextLine();
    double distance = defaultDistance;

    if(input.toLowerCase().equals("n")){
      System.out.println();
      System.out.print("Enter the distance(km): ");
      distance = scanner.nextDouble();
      scanner.nextLine();
    }

    ArrayList<ListingDist> listings = getListingSet();
    listings = filterByDist(listings, lat, lon, distance);

    Collections.sort(listings, new Comparator<ListingDist>() {
      @Override
      public int compare(ListingDist l1, ListingDist l2){
        if(l1.getDist() > l2.getDist()) return 1;
        if(l1.getDist() == l2.getDist()) return 0;
        return -1;
      }
    });

    printListingSet(listings);
  }

  public static ArrayList<ListingDist> filterByDist(ArrayList<ListingDist> listings, 
                                                    double lat, double lon, double dist){
    ArrayList<ListingDist> filtered = new ArrayList<ListingDist>();

    for(ListingDist listing : listings){
      double d = distanceBetween(lat, lon, listing.getLat(), listing.getLon());
      if(d <= dist){
        listing.setDist(d);
        filtered.add(listing);
      }
    }
    
    return filtered;
  }

  /*
   * Returns the distance in km between lat1, lon1 and lat2, lon2 using the Haversine formula
   */
  public static double distanceBetween(double lat1, double lon1, double lat2, double lon2){
    double rlat1 = Math.toRadians(lat1);
    double rlat2 = Math.toRadians(lat2);
    double radius = 6371;

    double dlat = rlat2 - rlat1;
    double dlon = Math.toRadians(lon2 - lon1);

    double a = 
      Math.sin(dlat/2) * Math.sin(dlat/2) + Math.cos(rlat1) * Math.cos(rlat2) * 
      Math.sin(dlon/2) * Math.sin(dlon/2);
    double c = Math.asin(Math.sqrt(a));
    return 2 * c * radius;
  }

  public static void printListingSet(ArrayList<ListingDist> listings){
    for (ListingDist listing : listings){
      System.out.println(listing.toString());
    }
  }

  public static void updateListing(Scanner scanner) throws ClassNotFoundException, SQLException {
    // Before any updates, need to check that the given availability is not booked
    // Get the sin of the host and display all their listings
    System.out.print("Enter the SIN of the host: ");
    int sin = scanner.nextInt();
    scanner.nextLine();
    if (!SqlDAO.getInstance().checkUserExists(sin)){
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

    // Can display the availability / price for the current listing (try to merge the range / cost to be easier to display)
    displayListingAvailabilities(listing);
    
    // User inputs date range and price, if existing date, then update price if not booked
    //                             if not existing date, then insert new availability
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
      }
      catch (DateTimeParseException e) {
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
            SqlDAO.getInstance().insertAvailability(listing, new DateCost(first_start, Date.valueOf(startDate.minusDays(1).format(formatter)), first_cost));
          if (Date.valueOf(endDate.format(formatter)).before(last_end))
            SqlDAO.getInstance().insertAvailability(listing, new DateCost(Date.valueOf(endDate.plusDays(1).format(formatter)), last_end, last_cost));
        }
        SqlDAO.getInstance().insertAvailability(listing, new DateCost(Date.valueOf(startDate.format(formatter)), Date.valueOf(endDate.format(formatter)), cost));
        
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
    while(rs.next()){
      //Retrieve by column name
      int listing = rs.getInt("listing_id");
      listingIds.add(listing);
      //Display values
      System.out.format("id: %-5s", listing);
      index ++;
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
    while(rs.next()){
      //Retrieve by column name
      String start = rs.getString("start");
      String end = rs.getString("end");
      double cost = rs.getDouble("cost");
      boolean available = rs.getBoolean("availability");
      //Display values
      System.out.format("(%-50s", start + ", " + end + ", $" + cost + ", Booked: " + !available + ")");
      index ++;
      if (index % 3 == 0) {
        System.out.println();
      }
    }
    rs.close();
    System.out.println();
  }

}
