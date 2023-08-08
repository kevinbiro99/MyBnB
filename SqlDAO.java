import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashSet;

/*
 * PreparedStatement statement for queries that relies on input?
 */

/**
 * Handles interaction with the SQL database. Contains the connection and
 * queries that
 * interact with the database.
 */
public class SqlDAO {
  private static final String dbClassName = "com.mysql.cj.jdbc.Driver";
  private static final String CONNECTION = "jdbc:mysql://127.0.0.1/mybnb";

    // Database credentials
    // final String USER = "sqluser"; // Laptop
    final String USER = "root"; // Desktop
    final String PASS = "password";

  // SQL connection session
  public static Connection conn;
  public static Statement stmt;
  private static SqlDAO dao;

  public static SqlDAO getInstance() throws ClassNotFoundException {
    if (dao == null) {
      dao = new SqlDAO();
    }
    return dao;
  }

  public static void deleteInstance() throws SQLException {
    if (dao == null)
      return;
    System.out.println("Closing connection...");
    stmt.close();
    conn.close();
    dao = null;
    System.out.println("Success!");
  }

  private SqlDAO() throws ClassNotFoundException {
    // Register JDBC driver
    Class.forName(dbClassName);

    System.out.println("Connecting to database...");

    try {
      // Establish connection
      conn = DriverManager.getConnection(CONNECTION, USER, PASS);
      stmt = conn.createStatement();
      System.out.println("Successfully connected to MySQL!");
    } catch (SQLException e) {
      System.err.println("Connection error occured!" + e);
    }
  }

  /*
   * Creates all the tables, constraints, and populates them with data so that
   * the app can be used.
   */
  public void createDatabase(String fileName) throws IOException, SQLException {
    BufferedReader reader = new BufferedReader(new FileReader(fileName));
    String line;
    StringBuilder queryBuilder = new StringBuilder();

    while ((line = reader.readLine()) != null) {
      line = line.trim();
      if (line.isEmpty() || line.startsWith("--")) {
        continue; // Skip empty lines and comments
      }

      queryBuilder.append(line);
      if (line.endsWith(";")) {
        String query = queryBuilder.toString();
        stmt.executeUpdate(query);
        System.out.println("Executed query: " + query);
        queryBuilder.setLength(0); // Reset query builder
      }
    }
    reader.close();
    System.out.println("Database initialized successfully.");
  }

  // Validate input
  public void registerUser(int sin, String name, String postal, String city, String country, String dob,
      String occupation) throws SQLException {
    String query = "INSERT INTO Users (sin,name,postalcode,city,country,dob,occupation) VALUES (?,?,?,?,?,?,?);";
    PreparedStatement pstmt = conn.prepareStatement(query);
    pstmt.setInt(1, sin);
    pstmt.setString(2, name);
    pstmt.setString(3, postal);
    pstmt.setString(4, city);
    pstmt.setString(5, country);
    pstmt.setString(6, dob);
    pstmt.setString(7, occupation);
    pstmt.executeUpdate();
    System.out.println("User: " + sin + " added to database");
  }

  /*
   * Deletes a user if they exist in the database
   * Need to delete all associated data in listings, ratings, bookings, etc
   * with the associated sin.
   */
  public void deleteUser(int sin) throws SQLException {
    String query = "DELETE FROM users WHERE sin=?;";
    PreparedStatement pstmt = conn.prepareStatement(query);
    pstmt.setInt(1, sin);
    pstmt.executeUpdate();
    System.out.println("User: " + sin + " deleted");
  }

  public ResultSet getUsers() throws SQLException {
    String query = "select * from Users";
    return stmt.executeQuery(query);
  }

  /*
   * Takes in a list of amenities, listing info, list of days available (stored as
   * ranges for more efficieny)
   */
  public int insertListing(int sin, String type, double lat, double lon, String postalcode, String city,
      String country, HashSet<String> amenities, ArrayList<DateCost> availabilityList) throws SQLException {
    lat = Double.parseDouble(String.format("%.6f", lat));
    lon = Double.parseDouble(String.format("%.6f", lon));

    // insert into listing table
    String query = "INSERT INTO Listings (type, latitude, longitude, postal_code, city, country) VALUES (?,?,?,?,?,?);";
    PreparedStatement pstmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
    pstmt.setString(1, type);
    pstmt.setDouble(2, lat);
    pstmt.setDouble(3, lon);
    pstmt.setString(4, postalcode);
    pstmt.setString(5, city);
    pstmt.setString(6, country);

    int affectedRows = pstmt.executeUpdate();

    // Retrieve the listing id
    int listingId = 0;
    if (affectedRows > 0) {
      // Retrieve the generated keys
      ResultSet generatedKeys = pstmt.getGeneratedKeys();

      if (generatedKeys.next()) {
        listingId = generatedKeys.getInt(1);
        System.out.println("The listing_id of the most recently inserted listing is: " + listingId);
      }
    }

    // insert into offers table (for amenities)
    for (String amenity : amenities) {
      insertOffering(amenity, listingId);
    }

    // insert into availability table
    for (DateCost dc : availabilityList) {
      insertAvailability(listingId, dc);
    }

    // insert into hosts table
    insertHost(sin, listingId);

    return listingId;
  }

  public boolean checkAmenityExists(String amenity) throws SQLException {
    boolean exists = false;
    String query = "SELECT amenity_name FROM Amenities WHERE amenity_name = ?";
    PreparedStatement pstmt = conn.prepareStatement(query);
    pstmt.setString(1, amenity);
    
    ResultSet resultSet = pstmt.executeQuery();

    if (resultSet.next()) {
      exists = true;
    }
    resultSet.close();

    return exists;
  }

  public boolean checkUserExists(int sin) throws SQLException {
    boolean exists = false;
    String query = "SELECT sin FROM Users WHERE sin = ?";

    PreparedStatement pstmt = conn.prepareStatement(query);
    pstmt.setInt(1, sin);

    ResultSet resultSet = pstmt.executeQuery();

    if (resultSet.next()) {
      exists = true;
    }
    resultSet.close();

    return exists;
  }

  public void insertOffering(String amenity, int listingId) throws SQLException {
    String query = "INSERT INTO Offerings (listing_id, amenity) VALUES (?,?);";

    PreparedStatement pstmt = conn.prepareStatement(query);
    pstmt.setInt(1, listingId);
    pstmt.setString(2, amenity);

    System.out.println(query);
    pstmt.executeUpdate();
    System.out.println("Amenity: " + amenity + " added to database");
  }

  public ResultSet getAmenities() throws SQLException {
    String query = "select * from Amenities";
    return stmt.executeQuery(query);
  }

  public void insertAvailability(int listingId, DateCost dc) throws SQLException {
    String query = "INSERT INTO Availabilities (listing_id, start, end, cost) VALUES (?,?,?,?);";

    PreparedStatement pstmt = conn.prepareStatement(query);
    pstmt.setInt(1, listingId);
    pstmt.setString(2, dc.getStartDate().toString());
    pstmt.setString(3, dc.getEndDate().toString());
    pstmt.setString(4, String.format("%.2f", dc.getCost()));

    System.out.println(query);
    pstmt.executeUpdate();
    System.out.println("Availability for: " + listingId + " on range: " + dc.getStartDate() + " - "
        + dc.getEndDate() + " added to database");
  }

  public void insertHost(int sin, int listingId) throws SQLException {
    String query = "INSERT INTO Hosts (listing_id, sin) VALUES (?,?);";

    PreparedStatement pstmt = conn.prepareStatement(query);
    pstmt.setInt(1, listingId);
    pstmt.setInt(2, sin);

    System.out.println(query);
    pstmt.executeUpdate();
    System.out.println("Host for: " + listingId + " on with sin: " + sin + " added to database");
  }

  public ResultSet getListings() throws SQLException {
    String query = "select * from listings";
    return stmt.executeQuery(query);
  }

  public ResultSet getListingsFromUser(int sin) throws SQLException {
    String query = "select * from hosts natural join listings where sin = ?";

    PreparedStatement pstmt = conn.prepareStatement(query);
    pstmt.setInt(1, sin);
    return pstmt.executeQuery();
  }

  public ResultSet getAvailabilitiesFromListing(int listing_id) throws SQLException {
    String query = "select * from availabilities where listing_id = ? order by start";

    PreparedStatement pstmt = conn.prepareStatement(query);
    pstmt.setInt(1, listing_id);
    return pstmt.executeQuery();
  }

  public ResultSet getRangeOverlapFromDate(int listing_id, String start, String end) throws SQLException {
    String query = "SELECT * FROM availabilities WHERE listing_id = ? AND (start BETWEEN ? AND ? OR end BETWEEN ? AND ? OR start <= ? AND end >= ?)";

    PreparedStatement pstmt = conn.prepareStatement(query);
    pstmt.setInt(1, listing_id);
    pstmt.setString(2, start);
    pstmt.setString(3, end);
    pstmt.setString(4, start);
    pstmt.setString(5, end);
    pstmt.setString(6, start);
    pstmt.setString(7, end);
    return pstmt.executeQuery();
  }

  public ResultSet getRangeContainsDate(int listing_id, String start, String end) throws SQLException {
    String query = "SELECT * FROM availabilities WHERE listing_id = ? AND (start <= ? AND end >= ?)";

    PreparedStatement pstmt = conn.prepareStatement(query);
    pstmt.setInt(1, listing_id);
    pstmt.setString(2, start);
    pstmt.setString(3, end);
    return pstmt.executeQuery();
  }

  public void deleteRangeOverlapFromDate(int listing_id, String start, String end) throws SQLException {
    String query = "DELETE FROM availabilities WHERE listing_id = ? AND (start BETWEEN ? AND ? OR end BETWEEN ? AND ? OR start <= ? AND end >= ?)";

    PreparedStatement pstmt = conn.prepareStatement(query);
    pstmt.setInt(1, listing_id);
    pstmt.setString(2, start);
    pstmt.setString(3, end);
    pstmt.setString(4, start);
    pstmt.setString(5, end);
    pstmt.setString(6, start);
    pstmt.setString(7, end);
    pstmt.executeUpdate();
  }

  public void updateAvailability(int listing_id, String start, String end, int availability) throws SQLException {
    String query = "UPDATE availabilities SET availability = ? WHERE listing_id = ? AND start = ? AND end = ?";

    PreparedStatement pstmt = conn.prepareStatement(query);
    pstmt.setInt(1, availability);
    pstmt.setInt(2, listing_id);
    pstmt.setString(3, start);
    pstmt.setString(4, end);
    pstmt.executeUpdate();
  }

  public void bookListing(int listing_id, int sin, String start, String end, long card) throws SQLException {
    String query = "INSERT INTO Bookings (listing_id, sin, start, end, card, host_sin) SELECT ?, ?, ?, ?, ?, sin FROM Hosts WHERE listing_id = ?;";

    PreparedStatement pstmt = conn.prepareStatement(query);
    pstmt.setInt(1, listing_id);
    pstmt.setInt(2, sin);
    pstmt.setString(3, start);
    pstmt.setString(4, end);
    pstmt.setLong(5, card);
    pstmt.setInt(6, listing_id);

    System.out.println(query);
    pstmt.executeUpdate();
    System.out.println("Booking for: " + listing_id + " by user with sin: " + sin + " added to database");
  }

  public ResultSet getBookings(int sin) throws SQLException {
    String query = "select * from bookings where sin = ?";

    PreparedStatement pstmt = conn.prepareStatement(query);
    pstmt.setInt(1, sin);

    return pstmt.executeQuery();
  }

  /*
   * Returns the bookings that are made on the user's listings and by the host
   */
  public ResultSet getBookingsFromHost(int sin) throws SQLException {
    String query = "(select * from bookings where listing_id in (select listing_id from hosts where sin = ?)) union (select * from bookings where sin = ?)";

    PreparedStatement pstmt = conn.prepareStatement(query);
    pstmt.setInt(1, sin);
    pstmt.setInt(2, sin);

    return pstmt.executeQuery();
  }

  public void cancelBooking(int booking_id, int canceller_sin) throws SQLException {
    // Save cancellation and who cancelled
    String query = "INSERT INTO Cancelled (canceller_sin, sin, host_sin, listing_id, start, end, card) SELECT ? AS canceller_sin, sin, host_sin, listing_id, start, end, card FROM Bookings WHERE booking_id = ?;";

    PreparedStatement pstmt = conn.prepareStatement(query);
    pstmt.setInt(1, canceller_sin);
    pstmt.setInt(2, booking_id);
    pstmt.executeUpdate();

    // Update availability
    query = "UPDATE availabilities AS a JOIN Bookings AS b ON a.start = b.start AND a.end = b.end AND a.listing_id = b.listing_id SET a.availability = 1 WHERE b.booking_id = ?;";

    pstmt = conn.prepareStatement(query);
    pstmt.setInt(1, booking_id);
    pstmt.executeUpdate();

    // Remove booking
    query = "DELETE FROM Bookings WHERE booking_id = ?;";

    pstmt = conn.prepareStatement(query);
    pstmt.setInt(1, booking_id);
    pstmt.executeUpdate();
  }

  /*
   * Returns whether the listing is booked or the stay is complete
   */
  public boolean isListingBooked(int listing_id) throws SQLException {
    String query = "SELECT EXISTS (SELECT 1 FROM availabilities AS a JOIN Bookings AS b ON a.start = b.start AND a.end = b.end AND a.listing_id = b.listing_id WHERE a.availability = 0 AND a.listing_id = ? AND b.complete = 0) AS is_booked;";

    PreparedStatement pstmt = conn.prepareStatement(query);
    pstmt.setInt(1, listing_id);
    ResultSet rs = pstmt.executeQuery();
    rs.next();
    boolean b = rs.getBoolean("is_booked");
    rs.close();
    return b;
  }

  public void removeListing(int listing_id) throws SQLException {
    String query = "DELETE FROM Availabilities WHERE listing_id = ?;";
    PreparedStatement pstmt = conn.prepareStatement(query);
    pstmt.setInt(1, listing_id);
    pstmt.executeUpdate();

    query = "DELETE FROM Hosts WHERE listing_id = ?;";
    pstmt = conn.prepareStatement(query);
    pstmt.setInt(1, listing_id);
    pstmt.executeUpdate();

    /*
     * Does not necessarily have offerings, so check if exists
     */
    query = "SELECT EXISTS (SELECT 1 FROM Offerings WHERE listing_id = ?) as has_offering";
    pstmt = conn.prepareStatement(query);
    pstmt.setInt(1, listing_id);
    ResultSet rs = pstmt.executeQuery();
    rs.next();
    if (rs.getBoolean("has_offering")) {
      query = "DELETE FROM Offerings WHERE listing_id = ?;";
      pstmt = conn.prepareStatement(query);
    pstmt.setInt(1, listing_id);
      pstmt.executeUpdate();
    }

    query = "DELETE FROM Listings WHERE listing_id = ?;";
    pstmt = conn.prepareStatement(query);
    pstmt.setInt(1, listing_id);
    pstmt.executeUpdate();
  }

  public void completeStay(int booking_id) throws SQLException {
    String query = "UPDATE Bookings SET complete = 1 WHERE booking_id = ?;";
    PreparedStatement pstmt = conn.prepareStatement(query);
    pstmt.setInt(1, booking_id);
    pstmt.executeUpdate();
  }

  public boolean userHasListingsOrBookings(int sin) throws SQLException {
    String query = "SELECT EXISTS (SELECT 1 FROM Hosts WHERE sin = ?) OR EXISTS (SELECT 1 FROM Bookings WHERE sin = ?) AS has_listings_or_bookings;";
    PreparedStatement pstmt = conn.prepareStatement(query);
    pstmt.setInt(1, sin);
    pstmt.setInt(2, sin);
    ResultSet rs = pstmt.executeQuery();
    rs.next();
    boolean b = rs.getBoolean("has_listings_or_bookings");
    rs.close();
    return b;
  }

  public ResultSet getUsersAndHostsFromCompletedRentals(int sin) throws SQLException {
    String query = "(select distinct host_sin as user from bookings where sin = ? and complete = \'1\') union (select distinct sin as user from bookings where host_sin = ? and complete = \'1\')";
    PreparedStatement pstmt = conn.prepareStatement(query);
    pstmt.setInt(1, sin);
    pstmt.setInt(2, sin);
    return pstmt.executeQuery();
  }

  public void insertUserReview(int poster_sin, int user_sin, String comment, int rating) throws SQLException {
    String query = "insert into user_review (poster_sin, user_sin, comment, rating) values (?, ?, ?, ?)";

    PreparedStatement pstmt = conn.prepareStatement(query);
    pstmt.setInt(1, poster_sin);
    pstmt.setInt(2, user_sin);
    pstmt.setString(3, comment);
    pstmt.setInt(4, rating);
    pstmt.executeUpdate();
  }

  public ResultSet getUserReviews(int sin) throws SQLException {
    String query = "select * from user_review where user_sin = ?";

    PreparedStatement pstmt = conn.prepareStatement(query);
    pstmt.setInt(1, sin);
    return pstmt.executeQuery();
  }

  public ResultSet getCompletedStays(int sin) throws SQLException {
    String query = "select distinct listing_id from bookings where sin = ? and complete = \'1\'";
    PreparedStatement pstmt = conn.prepareStatement(query);
    pstmt.setInt(1, sin);
    return pstmt.executeQuery();
  }

  public void insertListingReview(int poster_sin, int listing_id, String comment, int rating) throws SQLException {
    String query = "insert into listing_review (poster_sin, listing_id, comment, rating) values (?, ?, ?, ?)";

    PreparedStatement pstmt = conn.prepareStatement(query);
    pstmt.setInt(1, poster_sin);
    pstmt.setInt(2, listing_id);
    pstmt.setString(3, comment);
    pstmt.setInt(4, rating);
    pstmt.executeUpdate();
  }

  public ResultSet getListingReviews(int listing_id) throws SQLException {
    String query = "select * from listing_review where listing_id = ?";
    PreparedStatement pstmt = conn.prepareStatement(query);
    pstmt.setInt(1, listing_id);
    return pstmt.executeQuery();
  }

  public ResultSet getAllCCP() throws SQLException {
    String query = "SELECT country, city, postal_code FROM listings";
    return stmt.executeQuery(query);
  }

  public ResultSet countListingInCountries() throws SQLException {
    String query = "SELECT country, count(*) as count FROM listings GROUP BY country ORDER BY count DESC";
    return stmt.executeQuery(query);
  }

  public ResultSet countListingInCities() throws SQLException {
    String query = "SELECT country, city, count(*) as count FROM listings GROUP BY country, city ORDER BY count DESC";
    return stmt.executeQuery(query);
  }

  public ResultSet countListingInPostals() throws SQLException {
    String query = "SELECT country, city, postal_code, count(*) as count FROM listings GROUP BY country, city, postal_code ORDER BY count DESC";
    return stmt.executeQuery(query);
  }

  public ResultSet countListingByHostAndCountry() throws SQLException {
    String query = "SELECT sin, country, count(*) as count FROM hosts NATURAL JOIN listings GROUP BY sin, country ORDER BY country";
    return stmt.executeQuery(query);
  }

  public ResultSet countListingByHostAndCities() throws SQLException {
    String query = "SELECT sin, country, city, count(*) as count FROM hosts NATURAL JOIN listings GROUP BY sin, country, city ORDER BY country, city";
    return stmt.executeQuery(query);
  }

  public ResultSet morethan10percent() throws SQLException {
    String countSinListingsInCity = "(SELECT sin, country, city, count(*) as count FROM hosts NATURAL JOIN listings GROUP BY sin, country, city)";
    String countListingOverallInCity = "(SELECT country, city, count(*) as total FROM listings GROUP BY country, city)";
    String query = "SELECT a.sin, a.country, a.city, a.count, b.total FROM (%s as a NATURAL JOIN %s as b) WHERE (a.count * 10 > b.total)";
    query = String.format(query, countSinListingsInCity, countListingOverallInCity);
    return stmt.executeQuery(query);
  }

  public ResultSet countBookingInDateRangeByCity(String start, String end) throws SQLException {
    String filterBookingAttributes = "(SELECT listing_id FROM bookings WHERE (start >= ? AND end <= ?))";
    filterBookingAttributes = String.format(filterBookingAttributes, start, end);
    String filterListingAttributes = "(SELECT listing_id, city FROM listings)";
    String query = "SELECT b.city, count(*) as count FROM (%s as a NATURAL JOIN %s as b) GROUP BY b.city ORDER BY count DESC";
    query = String.format(query, filterBookingAttributes, filterListingAttributes);
    PreparedStatement pstmt = conn.prepareStatement(query);
    pstmt.setString(1, start);
    pstmt.setString(2, end);
    return pstmt.executeQuery();
  }

  public ResultSet countBookingInDateRangeByPostal(String start, String end, String city) throws SQLException {
    String filterBookingAttributes = "(SELECT listing_id FROM bookings WHERE (start >= ? AND end <= ?))";
    filterBookingAttributes = String.format(filterBookingAttributes, start, end);
    String filterListingAttributes = "(SELECT listing_id, postal_code FROM listings WHERE city = ?)";
    filterListingAttributes = String.format(filterListingAttributes, city);
    String query = "SELECT b.postal_code, count(*) as count FROM (%s as a NATURAL JOIN %s as b) GROUP BY b.postal_code ORDER BY count DESC";
    query = String.format(query, filterBookingAttributes, filterListingAttributes);
    PreparedStatement pstmt = conn.prepareStatement(query);
    pstmt.setString(1, start);
    pstmt.setString(2, end);
    pstmt.setString(3, city);
    return pstmt.executeQuery();
  }

  public ResultSet rankRenterByBookingInDateRange(String start, String end) throws SQLException {
    String query = "SELECT sin, count(*) as count FROM bookings WHERE (start >= ? AND end <= ?) GROUP BY sin ORDER BY count DESC";

    PreparedStatement pstmt = conn.prepareStatement(query);
    pstmt.setString(1, start);
    pstmt.setString(2, end);
    return pstmt.executeQuery();
  }

  public ResultSet rankRenterByBookingInDateRangeAndCity(String start, String end, String year) throws SQLException {
    String filterAtLeastTwo = "SELECT sin, count(*) as occurences FROM bookings WHERE start LIKE ? OR end LIKE ? GROUP BY sin";
    String filterBookingAttributes = "SELECT sin, listing_id FROM bookings WHERE (start >= ? AND end <= ?) GROUP BY sin, listing_id";
    String query = "SELECT b.sin, count(*) as count, c.city FROM ((" + filterAtLeastTwo + ") as a NATURAL JOIN ("
        + filterBookingAttributes
        + ") as b NATURAL JOIN listings as c) WHERE a.occurences > 1 GROUP BY sin, city ORDER BY count DESC";
    PreparedStatement pstmt = conn.prepareStatement(query);
    pstmt.setString(1, year+"%");
    pstmt.setString(2, year+"%");
    pstmt.setString(3, start);
    pstmt.setString(4, end);
    return pstmt.executeQuery();
  }

  public ResultSet rankHostByCancellations(String year) throws SQLException {
    String query = "SELECT canceller_sin, count(*) as count FROM cancelled WHERE canceller_sin = host_sin AND (start LIKE ? OR end LIKE ?) GROUP BY canceller_sin ORDER BY count DESC";
    PreparedStatement pstmt = conn.prepareStatement(query);
    pstmt.setString(1, year+"%");
    pstmt.setString(2, year+"%");
    return pstmt.executeQuery();
  }

  public ResultSet rankRenterByCancellations(String year) throws SQLException {
    String query = "SELECT canceller_sin, count(*) as count FROM cancelled WHERE canceller_sin = sin AND (start LIKE ? OR end LIKE ?) GROUP BY canceller_sin ORDER BY count DESC";
    PreparedStatement pstmt = conn.prepareStatement(query);
    pstmt.setString(1, year+"%");
    pstmt.setString(2, year+"%");
    return pstmt.executeQuery();
  }

  public ResultSet getAllListingReview() throws SQLException {
    String query = "SELECT listing_id, comment FROM Listing_Review";
    return stmt.executeQuery(query);
  }

  public ResultSet getListingsInRadius(double radius, double lat, double lon, String type) throws SQLException {
    String query = "SELECT * FROM listings WHERE type = ? and (6371 * 2 * ASIN(SQRT(POWER(SIN((RADIANS(?) - RADIANS(latitude)) / 2), 2) + COS(RADIANS(?)) * COS(RADIANS(latitude)) * POWER(SIN((RADIANS(?) - RADIANS(longitude)) / 2), 2)))) <= ?;";

    PreparedStatement pstmt = conn.prepareStatement(query);
    pstmt.setString(1, type);
    pstmt.setDouble(2, lat);
    pstmt.setDouble(3, lat);
    pstmt.setDouble(4, lon);
    pstmt.setDouble(5, radius);
    return pstmt.executeQuery();
  }

  public ResultSet getRecommendedAmenitiesInRangeSortedDesc(double radius, double lat, double lon, String type)
      throws SQLException {
    String query = "SELECT o.amenity, COUNT(*) AS amenity_count FROM Offerings o INNER JOIN Listings l ON o.listing_id = l.listing_id WHERE type = ? and (6371 * 2 * ASIN(SQRT(POWER(SIN((RADIANS(?) - RADIANS(l.latitude)) / 2), 2) + COS(RADIANS(?)) * COS(RADIANS(l.latitude)) * POWER(SIN((RADIANS(?) - RADIANS(l.longitude)) / 2), 2)))) <= ? GROUP BY o.amenity ORDER BY amenity_count DESC;";

    PreparedStatement pstmt = conn.prepareStatement(query);
    pstmt.setString(1, type);
    pstmt.setDouble(2, lat);
    pstmt.setDouble(3, lat);
    pstmt.setDouble(4, lon);
    pstmt.setDouble(5, radius);
    return pstmt.executeQuery();
  }

  public boolean listingOffersAmenity(int listing_id, String amenity) throws SQLException {
    String query = "SELECT EXISTS (SELECT 1 FROM Offerings WHERE listing_id = ? and amenity = ?) AS has_amenity;";
    PreparedStatement pstmt = conn.prepareStatement(query);
    pstmt.setInt(1, listing_id);
    pstmt.setString(2, amenity);
    ResultSet rs = pstmt.executeQuery();
    rs.next();
    boolean b = rs.getBoolean("has_amenity");
    rs.close();
    return b;
  }

  public double averageListingCost(int listing_id) throws SQLException {
    String query = "SELECT AVG(cost) as avg_cost from availabilities where listing_id = ?;";
    PreparedStatement pstmt = conn.prepareStatement(query);
    pstmt.setInt(1, listing_id);
    ResultSet rs = pstmt.executeQuery();
    rs.next();
    double b = rs.getDouble("avg_cost");
    rs.close();
    return b;
  }

  public ResultSet getListingAmenities(int listing_id) throws SQLException {
    String query = "select * from offerings where listing_id = ?";
    PreparedStatement pstmt = conn.prepareStatement(query);
    pstmt.setInt(1, listing_id);
    return pstmt.executeQuery();
  }
}
