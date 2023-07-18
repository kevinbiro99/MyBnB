import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;

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
        String query = "select * from users";
        return stmt.executeQuery(query);
    }

    /*
     * Takes in a list of amenities, listing info, list of days available (stored as ranges for more efficieny)
     */
    public void insertListing (String type, double lat, double lon, String postalcode, String city, 
                            String country, ArrayList<String> amenities, ArrayList<DateCost> availabilityList) throws SQLException {
        // insert into listing table
        String query = "INSERT INTO Listings (type, latitude, longitude, postal_code, city, country) VALUES (\'%s\',\'%.6f\',\'%.6f\',\'%s\',\'%s\',\'%s\');";
        query = String.format(query, type, lat, lon, postalcode, city, country);
        System.out.println(query);
        int affectedRows = stmt.executeUpdate(query);

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

    public void insertOffering (String amenity, int listingId) throws SQLException {
        String query = "INSERT INTO Offerings (listing_id, amenity) VALUES (\'%d\',\'%s\');";
        query = String.format(query, listingId, amenity);
        System.out.println(query);
        stmt.executeUpdate(query);
        System.out.println("Amenity: " + amenity + " added to database");
    }
}
