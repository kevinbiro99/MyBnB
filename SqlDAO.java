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
        String query = "INSERT INTO Users (sin,name,postalcode,city,country,dob,occupation) VALUES (\'%d\',\'%s\',\'%s\',\'%s\',\'%s\',\'%s\',\'%s\');";
        query = String.format(query, sin, name, postal, city, country, dob, occupation);
        System.out.println(query);
        stmt.executeUpdate(query);
        System.out.println("User: " + sin + " added to database");
    }

    /*
     * Deletes a user if they exist in the database
     * Need to delete all associated data in listings, ratings, bookings, etc
     * with the associated sin.
     */
    public void deleteUser(int sin) throws SQLException {
        String query = "DELETE FROM users WHERE sin=%d;";
        query = String.format(query, sin);
        stmt.executeUpdate(query);
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
        // insert into listing table
        String query = "INSERT INTO Listings (type, latitude, longitude, postal_code, city, country) VALUES (\'%s\',\'%.6f\',\'%.6f\',\'%s\',\'%s\',\'%s\');";
        query = String.format(query, type, lat, lon, postalcode, city, country);
        System.out.println(query);
        int affectedRows = stmt.executeUpdate(query, Statement.RETURN_GENERATED_KEYS);

        // Retrieve the listing id
        int listingId = 0;
        if (affectedRows > 0) {
            // Retrieve the generated keys
            ResultSet generatedKeys = stmt.getGeneratedKeys();

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
        String query = "SELECT amenity_name FROM Amenities WHERE amenity_name = \'%s\'";
        query = String.format(query, amenity);

        ResultSet resultSet = stmt.executeQuery(query);

        if (resultSet.next()) {
            exists = true;
        }
        resultSet.close();
        
        return exists;
    }

    public boolean checkUserExists(int sin) throws SQLException {
        boolean exists = false;
        String query = "SELECT sin FROM Users WHERE sin = \'%d\'";
        query = String.format(query, sin);

        ResultSet resultSet = stmt.executeQuery(query);

        if (resultSet.next()) {
            exists = true;
        }
        resultSet.close();
        
        return exists;
    }

    public void insertOffering(String amenity, int listingId) throws SQLException {
        String query = "INSERT INTO Offerings (listing_id, amenity) VALUES (\'%d\',\'%s\');";
        query = String.format(query, listingId, amenity);
        System.out.println(query);
        stmt.executeUpdate(query);
        System.out.println("Amenity: " + amenity + " added to database");
    }

    public ResultSet getAmenities() throws SQLException {
        String query = "select * from Amenities";
        return stmt.executeQuery(query);
    }

    public void insertAvailability(int listingId, DateCost dc) throws SQLException {
        String query = "INSERT INTO Availabilities (listing_id, start, end, cost) VALUES (\'%d\',\'%s\', \'%s\',\'%.2f\');";
        query = String.format(query, listingId, dc.getStartDate(), dc.getEndDate(), dc.getCost());
        System.out.println(query);
        stmt.executeUpdate(query);
        System.out.println("Availability for: " + listingId + " on range: " + dc.getStartDate() + " - "
                + dc.getEndDate() + " added to database");
    }

    public void insertHost(int sin, int listingId) throws SQLException {
        String query = "INSERT INTO Hosts (listing_id, sin) VALUES (\'%d\',\'%d\');";
        query = String.format(query, listingId, sin);
        System.out.println(query);
        stmt.executeUpdate(query);
        System.out.println("Host for: " + listingId + " on with sin: " + sin + " added to database");
    }

    public ResultSet getListings() throws SQLException {
        String query = "select * from listings";
        return stmt.executeQuery(query);
    }

    public ResultSet getListingsFromUser(int sin) throws SQLException {
        String query = "select * from hosts natural join listings where sin = \'%d\'";
        query = String.format(query, sin);
        return stmt.executeQuery(query);
    }

    public ResultSet getAvailabilitiesFromListing(int listing_id) throws SQLException {
        String query = "select * from availabilities where listing_id = \'%d\' order by start";
        query = String.format(query, listing_id);
        return stmt.executeQuery(query);
    }

    public ResultSet getRangeOverlapFromDate(int listing_id, String start, String end) throws SQLException {
        String query = "SELECT * FROM availabilities WHERE listing_id = \'%d\' AND (start BETWEEN \'%s\' AND \'%s\' OR end BETWEEN \'%s\' AND \'%s\' OR start <= \'%s\' AND end >= \'%s\')";
        query = String.format(query, listing_id, start, end, start, end, start, end);
        return stmt.executeQuery(query);
    }

    public ResultSet getRangeContainsDate(int listing_id, String start, String end) throws SQLException {
        String query = "SELECT * FROM availabilities WHERE listing_id = \'%d\' AND (start <= \'%s\' AND end >= \'%s\')";
        query = String.format(query, listing_id, start, end);
        return stmt.executeQuery(query);
    }

    public void deleteRangeOverlapFromDate(int listing_id, String start, String end) throws SQLException {
        String query = "DELETE FROM availabilities WHERE listing_id = \'%d\' AND (start BETWEEN \'%s\' AND \'%s\' OR end BETWEEN \'%s\' AND \'%s\' OR start <= \'%s\' AND end >= \'%s\')";
        query = String.format(query, listing_id, start, end, start, end, start, end);
        stmt.executeUpdate(query);
    }

    public void updateAvailability(int listing_id, String start, String end, int availability) throws SQLException {
        String query = "UPDATE availabilities SET availability = \'%d\' WHERE listing_id = \'%d\' AND start = \'%s\' AND end = \'%s\'";
        query = String.format(query, availability, listing_id, start, end);
        stmt.executeUpdate(query);
    }

    public void bookListing(int listing_id, int sin, String start, String end, long card) throws SQLException {
        String query = "INSERT INTO Bookings (listing_id, sin, start, end, card, host_sin) SELECT \'%d\',\'%d\', \'%s\', \'%s\', \'%d\', sin FROM Hosts WHERE listing_id = \'%d\';";
        query = String.format(query, listing_id, sin, start, end, card, listing_id);
        System.out.println(query);
        stmt.executeUpdate(query);
        System.out.println("Booking for: " + listing_id + " by user with sin: " + sin + " added to database");
    }

    public ResultSet getBookings(int sin) throws SQLException {
        String query = "select * from bookings where sin = \'%d\'";
        query = String.format(query, sin);
        return stmt.executeQuery(query);
    }

    /*
     * Returns the bookings that are made on the user's listings and by the host
     */
    public ResultSet getBookingsFromHost(int sin) throws SQLException {
        String query = "(select * from bookings where listing_id in (select listing_id from hosts where sin = \'%d\')) union (select * from bookings where sin = \'%d\')";
        query = String.format(query, sin, sin);
        return stmt.executeQuery(query);
    }

    public void cancelBooking(int booking_id, int canceller_sin) throws SQLException {
        // Save cancellation and who cancelled
        String query = "INSERT INTO Cancelled (canceller_sin, sin, host_sin, listing_id, start, end, card) SELECT \'%d\' AS canceller_sin, sin, host_sin, listing_id, start, end, card FROM Bookings WHERE booking_id = \'%d\';";
        query = String.format(query, canceller_sin, booking_id);
        stmt.executeUpdate(query);

        // Update availability
        query = "UPDATE availabilities AS a JOIN Bookings AS b ON a.start = b.start AND a.end = b.end AND a.listing_id = b.listing_id SET a.availability = 1 WHERE b.booking_id = \'%d\';";
        query = String.format(query, booking_id);
        stmt.executeUpdate(query);

        // Remove booking
        query = "DELETE FROM Bookings WHERE booking_id = \'%d\';";
        query = String.format(query, booking_id);
        stmt.executeUpdate(query);
    }

    /*
     * Returns whether the listing is booked or the stay is complete
     */
    public boolean isListingBooked(int listing_id) throws SQLException {
        String query = "SELECT EXISTS (SELECT 1 FROM availabilities AS a JOIN Bookings AS b ON a.start = b.start AND a.end = b.end AND a.listing_id = b.listing_id WHERE a.availability = 0 AND a.listing_id = \'%d\' AND b.complete = 0) AS is_booked;";
        query = String.format(query, listing_id);
        ResultSet rs = stmt.executeQuery(query);
        rs.next();
        boolean b = rs.getBoolean("is_booked");
        rs.close();
        return b;
    }

    public void removeListing(int listing_id) throws SQLException {
        String query = "DELETE FROM Availabilities WHERE listing_id = \'%d\';";
        query = String.format(query, listing_id);
        stmt.executeUpdate(query);

        query = "DELETE FROM Hosts WHERE listing_id = \'%d\';";
        query = String.format(query, listing_id);
        stmt.executeUpdate(query);

        /*
         * Does not necessarily have offerings, so check if exists
         */
        query = "SELECT EXISTS (SELECT 1 FROM Offerings WHERE listing_id = \'%d\') as has_offering";
        query = String.format(query, listing_id);
        ResultSet rs = stmt.executeQuery(query);
        rs.next();
        if (rs.getBoolean("has_offering")) {
            query = "DELETE FROM Offering WHERE listing_id = \'%d\';";
            query = String.format(query, listing_id);
            stmt.executeUpdate(query);
        }

        query = "DELETE FROM Listings WHERE listing_id = \'%d\';";
        query = String.format(query, listing_id);
        stmt.executeUpdate(query);
    }

    public void completeStay(int booking_id) throws SQLException {
        String query = "UPDATE Bookings SET complete = 1 WHERE booking_id = \'%d\';";
        query = String.format(query, booking_id);
        stmt.executeUpdate(query);
    }

    public boolean userHasListingsOrBookings(int sin) throws SQLException {
        String query = "SELECT EXISTS (SELECT 1 FROM Hosts WHERE sin = \'%d\') OR EXISTS (SELECT 1 FROM Bookings WHERE sin = \'%d\') AS has_listings_or_bookings;";
        query = String.format(query, sin, sin);
        ResultSet rs = stmt.executeQuery(query);
        rs.next();
        boolean b = rs.getBoolean("has_listings_or_bookings");
        rs.close();
        return b;
    }

    public ResultSet getUsersAndHostsFromCompletedRentals(int sin) throws SQLException {
        String query = "(select distinct host_sin as user from bookings where sin = \'%d\' and complete = \'1\') union (select distinct sin as user from bookings where host_sin = \'%d\' and complete = \'1\')";
        query = String.format(query, sin, sin);
        return stmt.executeQuery(query);
    }

    public void insertUserReview(int poster_sin, int user_sin, String comment, int rating) throws SQLException {
        String query = "insert into user_review (poster_sin, user_sin, comment, rating) values (\'%d\', \'%d\', \'%s\', \'%d\')";
        query = String.format(query, poster_sin, user_sin, comment, rating);
        stmt.executeUpdate(query);
    }

    public ResultSet getUserReviews(int sin) throws SQLException {
        String query = "select * from user_review where user_sin = \'%d\'";
        query = String.format(query, sin);
        return stmt.executeQuery(query);
    }

    public ResultSet getCompletedStays(int sin) throws SQLException {
        String query = "select distinct listing_id from bookings where sin = \'%d\' and complete = \'1\'";
        query = String.format(query, sin);
        return stmt.executeQuery(query);
    }

    public void insertListingReview(int poster_sin, int listing_id, String comment, int rating) throws SQLException {
        String query = "insert into listing_review (poster_sin, listing_id, comment, rating) values (\'%d\', \'%d\', \'%s\', \'%d\')";
        query = String.format(query, poster_sin, listing_id, comment, rating);
        stmt.executeUpdate(query);
    }

    public ResultSet getListingReviews(int listing_id) throws SQLException {
        String query = "select * from listing_review where listing_id = \'%d\'";
        query = String.format(query, listing_id);
        return stmt.executeQuery(query);
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

    public ResultSet countBookingInDateRangeByCity(String start, String end) throws SQLException{
        String filterBookingAttributes = "(SELECT listing_id FROM bookings WHERE (start >= \'%s\' AND end <= \'%s\'))";
        filterBookingAttributes = String.format(filterBookingAttributes, start, end);
        String filterListingAttributes = "(SELECT listing_id, city FROM listings)";
        String query = "SELECT b.city, count(*) as count FROM (%s as a NATURAL JOIN %s as b) GROUP BY b.city";
        query = String.format(query, filterBookingAttributes, filterListingAttributes);
        return stmt.executeQuery(query);
    }

    public ResultSet countBookingInDateRangeByPostal(String start, String end, String city) throws SQLException{
        String filterBookingAttributes = "(SELECT listing_id FROM bookings WHERE (start >= \'%s\' AND end <= \'%s\'))";
        filterBookingAttributes = String.format(filterBookingAttributes, start, end);
        String filterListingAttributes = "(SELECT listing_id, postal_code FROM listings WHERE city = \'%s\')";
        filterListingAttributes = String.format(filterListingAttributes, city);
        String query = "SELECT b.postal_code, count(*) as count FROM (%s as a NATURAL JOIN %s as b) GROUP BY b.postal_code";
        query = String.format(query, filterBookingAttributes, filterListingAttributes);
        return stmt.executeQuery(query);
    }

    public ResultSet rankRenterByBookingInDateRange(String start, String end) throws SQLException{
        String query = "SELECT sin, count(*) as count FROM bookings WHERE (start >= \'%s\' AND end <= \'%s\') GROUP BY sin ORDER BY count DESC";
        query = String.format(query, start, end);
        return stmt.executeQuery(query);
    }

    public ResultSet rankRenterByBookingInDateRangeAndCity(String start, String end, String year) throws SQLException{
        String filterAtLeastTwo = "SELECT sin, count(*) as occurences FROM bookings WHERE start LIKE \'" + year + "%\' OR end LIKE \'"+ year + "%\' GROUP BY sin";
        String filterBookingAttributes = "SELECT sin, listing_id FROM bookings WHERE (start >= \'%s\' AND end <= \'%s\') GROUP BY sin, listing_id";
        filterBookingAttributes = String.format(filterBookingAttributes, start, end);
        String query = "SELECT b.sin, count(*) as count, c.city FROM (("+filterAtLeastTwo+") as a NATURAL JOIN ("+filterBookingAttributes+") as b NATURAL JOIN listings as c) WHERE a.occurences > 1 GROUP BY sin, city ORDER BY count DESC";
        return stmt.executeQuery(query);
    }

    public ResultSet rankHostByCancellations(String year) throws SQLException{
        String query = "SELECT canceller_sin, count(*) as count FROM cancelled WHERE canceller_sin = host_sin AND (start LIKE \'" + year + "%\' OR end LIKE \'"+ year + "%\') GROUP BY canceller_sin ORDER BY count DESC";
        return stmt.executeQuery(query);
    }

    public ResultSet rankRenterByCancellations(String year) throws SQLException{
        String query = "SELECT canceller_sin, count(*) as count FROM cancelled WHERE canceller_sin = sin AND (start LIKE \'" + year + "%\' OR end LIKE \'"+ year + "%\') GROUP BY canceller_sin ORDER BY count DESC";
        return stmt.executeQuery(query);
    }

    public ResultSet getAllListingReview() throws SQLException{
        String query = "SELECT listing_id, comment FROM Listing_Review";
        return stmt.executeQuery(query);
    }

    public ResultSet getListingsInRadius(double radius, double lat, double lon, String type) throws SQLException {
        String query = "SELECT * FROM listings WHERE type = \'%s\' and (6371 * 2 * ASIN(SQRT(POWER(SIN((RADIANS(\'%f\') - RADIANS(latitude)) / 2), 2) + COS(RADIANS(\'%f\')) * COS(RADIANS(latitude)) * POWER(SIN((RADIANS(\'%f\') - RADIANS(longitude)) / 2), 2)))) <= \'%f\';";
        query = String.format(query, type, lat, lat, lon, radius);
        return stmt.executeQuery(query);
    }

    public ResultSet getRecommendedAmenitiesInRangeSortedDesc(double radius, double lat, double lon, String type) throws SQLException {
        String query = "SELECT o.amenity, COUNT(*) AS amenity_count FROM Offerings o INNER JOIN Listings l ON o.listing_id = l.listing_id WHERE type = \'%s\' and (6371 * 2 * ASIN(SQRT(POWER(SIN((RADIANS(\'%f\') - RADIANS(l.latitude)) / 2), 2) + COS(RADIANS(\'%f\')) * COS(RADIANS(l.latitude)) * POWER(SIN((RADIANS(\'%f\') - RADIANS(l.longitude)) / 2), 2)))) <= \'%f\' GROUP BY o.amenity ORDER BY amenity_count DESC;";
        query = String.format(query, type, lat, lat, lon, radius);
        return stmt.executeQuery(query);
    }

    public boolean listingOffersAmenity(int listing_id, String amenity) throws SQLException {
        String query = "SELECT EXISTS (SELECT 1 FROM Offerings WHERE listing_id = \'%d\' and amenity = \'%s\') AS has_amenity;";
        query = String.format(query, listing_id, amenity);
        ResultSet rs = stmt.executeQuery(query);
        rs.next();
        boolean b = rs.getBoolean("has_amenity");
        rs.close();
        return b;
    }

    public double averageListingCost(int listing_id) throws SQLException {
        String query = "SELECT AVG(cost) as avg_cost from availabilities where listing_id = \'%d\';";
        query = String.format(query, listing_id);
        ResultSet rs = stmt.executeQuery(query);
        rs.next();
        double b = rs.getDouble("avg_cost");
        rs.close();
        return b;
    }

    public ResultSet getListingAmenities(int listing_id) throws SQLException {
        String query = "select * from offerings where listing_id = \'%d\'";
        query = String.format(query, listing_id);
        return stmt.executeQuery(query);
    }
}
