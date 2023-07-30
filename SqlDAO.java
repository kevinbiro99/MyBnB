import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashSet;

/**
 * Handles interaction with the SQL database. Contains the connection and queries that
 * interact with the database.
 */
public class SqlDAO {
    private static final String dbClassName = "com.mysql.cj.jdbc.Driver";
	private static final String CONNECTION = "jdbc:mysql://127.0.0.1/mybnb";

    //Database credentials
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
        if (dao == null) return;
        System.out.println("Closing connection...");
        stmt.close();
        conn.close();
        dao = null;
        System.out.println("Success!");
    }

    private SqlDAO () throws ClassNotFoundException {
        //Register JDBC driver
		Class.forName(dbClassName);
		
		System.out.println("Connecting to database...");

        try {
			//Establish connection
			conn = DriverManager.getConnection(CONNECTION,USER,PASS);
            stmt = conn.createStatement();
			System.out.println("Successfully connected to MySQL!");
        }
        catch (SQLException e) {
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
    public void registerUser (int sin, String name, String postal, String city, String country, String dob, String occupation) throws SQLException {
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
     * Takes in a list of amenities, listing info, list of days available (stored as ranges for more efficieny)
     */
    public int insertListing (int sin, String type, double lat, double lon, String postalcode, String city, 
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
        
        return exists;
    }

    public void insertOffering (String amenity, int listingId) throws SQLException {
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

    public void insertAvailability (int listingId, DateCost dc) throws SQLException {
        String query = "INSERT INTO Availabilities (listing_id, start, end, cost) VALUES (\'%d\',\'%s\', \'%s\',\'%.2f\');";
        query = String.format(query, listingId, dc.getStartDate(), dc.getEndDate(), dc.getCost());
        System.out.println(query);
        stmt.executeUpdate(query);
        System.out.println("Availability for: " + listingId + " on range: " + dc.getStartDate() + " - " + dc.getEndDate() + " added to database");
    }

    public void insertHost (int sin, int listingId) throws SQLException {
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
        String query = "INSERT INTO Bookings (listing_id, sin, start, end, card) VALUES (\'%d\',\'%d\', \'%s\', \'%s\', \'%d\');";
        query = String.format(query, listing_id, sin, start, end, card);
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
        String query = "INSERT INTO Cancelled (canceller_sin, sin, listing_id, start, end, card) SELECT \'%d\' AS canceller_sin, sin, listing_id, start, end, card FROM Bookings WHERE booking_id = \'%d\';";
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
        return rs.getBoolean("is_booked");
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
        return rs.getBoolean("has_listings_or_bookings");
    }

}
