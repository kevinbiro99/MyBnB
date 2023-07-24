import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.time.format.DateTimeParseException;

public class Listing {
  public static ArrayList<String> listingTypes = new ArrayList<String>(Arrays.asList("house", "apartment", "guesthouse", "hotel"));
  private static double defaultDistance = 10;
  
  //TODO: Should change how available dates are stored, takes too much space

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
      System.out.print("Enter the type of listing (house, apartment, guesthouse, hotel): ");
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
        if (isDateInRange(dc.getDate().toString(), start, end)) {
          System.out.println("Date range conflicts with existing date: " + dc.getDate());
          invalid = true;
          break;
        }
      }

      if (!invalid) {
        // Insert a date into the database for each date in the given start-end range
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            String formattedDate = currentDate.format(formatter);
            availabilities.add(new DateCost(Date.valueOf(formattedDate), cost));
            currentDate = currentDate.plusDays(1);
        }
        
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
        } else {
          System.out.println("Sorry, you need to insert at least one valid date range");
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

  public static boolean isDateInRange(String dateString, String startDateString, String endDateString) {
    LocalDate date = LocalDate.parse(dateString);
    LocalDate startDate = LocalDate.parse(startDateString);
    LocalDate endDate = LocalDate.parse(endDateString);

    return date.isEqual(startDate) || date.isEqual(endDate) || (date.isAfter(startDate) && date.isBefore(endDate));
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
}
