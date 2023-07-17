import java.sql.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;

public class MyBnB {

	private static final String dbClassName = "com.mysql.cj.jdbc.Driver";
	private static final String CONNECTION = "jdbc:mysql://127.0.0.1/mybnb";
  private static int choice;
  private static ArrayList<String> functions = new ArrayList<String>();
  private static Scanner scanner;
	
	public static void main(String[] args) throws ClassNotFoundException, ParseException {
		//Register JDBC driver
		Class.forName(dbClassName);
		//Database credentials
		final String USER = "root";
		final String PASS = "password";
		System.out.println("Connecting to database...");
		
		try {
			//Establish connection
			Connection conn = DriverManager.getConnection(CONNECTION,USER,PASS);
			System.out.println("Successfully connected to MySQL!");
			
			
			
      // ***********************************************
      scanner = new Scanner(System.in);  // Create a Scanner object
      functions = new ArrayList<>(Arrays.asList("Create User", "Delete User", 
                "Select User", "Book Listing", "Cancel Booking", "Create Listing", "Remove Listing",
                "Update Listing", "Review Listing", "Review User",
                "Search Listings", "Reports"));
      selectFunction();
      // ***********************************************
      
      while(true) {
        System.out.println(functions.get(choice - 1));
        switch (choice) {
          case 1: // Create User
            try {
              String info = getUserInfo();
              Statement stmt = conn.createStatement();
              String sql = "INSERT INTO Users (sin,name,postalcode,city,country,dob,occupation) VALUES (" + info + ");";
              stmt.executeUpdate(sql);
              System.out.println("User added to database");
            }
            catch (SQLException e) {
              System.out.println("Error inserting user: " + e);
            }
          case 2: // Delete User
            
        }
        break;
      }
      scanner.close();

      //Execute a query
			System.out.println("Preparing a statement...");
			Statement stmt = conn.createStatement();
			String sql = "select * from users";
			ResultSet rs = stmt.executeQuery(sql);

			//STEP 5: Extract data from result set
			while(rs.next()){
				//Retrieve by column name
				int sid  = rs.getInt("sin");
				String sname = rs.getString("name");
				Date rating = rs.getDate("dob");
				int age = rs.getInt("age");
			
				//Display values
				System.out.print("ID: " + sid);
				System.out.print(", Name: " + sname);
				System.out.print(", Rating: " + rating);
				System.out.println(", Age: " + age);
			}
			
			
			System.out.println("Closing connection...");
			rs.close();
			stmt.close();
			conn.close();
			System.out.println("Success!");
		} catch (SQLException e) {
			System.err.println("Connection error occured!" + e);
		}
	}

  public static void selectFunction() {
    System.out.println("Functions supported:");
    int index = 0;
    for (String f : functions) {
      System.out.print((index + 1) + ". " + f + "\t\t");
      index ++;
      if (index % 5 == 0) {
        System.out.println();
      }
    }
    System.out.println();
    System.out.print("Select a function: ");
    choice = scanner.nextInt();
    scanner.nextLine();
    System.out.println();
  }

  public static String getUserInfo() throws ParseException{
    String output = "";
    System.out.print("Enter your SIN: ");
    int num = scanner.nextInt();
    scanner.nextLine();
    output += "'" + num + "',";
    
    System.out.println();
    System.out.print("Enter your name: ");
    String str = scanner.nextLine();
    output += "'" + str + "',";
    
    System.out.println();
    System.out.print("Enter your postal code: ");
    str = scanner.nextLine();
    output += "'" + str + "',";
    
    System.out.println();
    System.out.print("Enter your city: ");
    str = scanner.nextLine();
    output += "'" + str + "',";
    
    System.out.println();
    System.out.print("Enter your country: ");
    str = scanner.nextLine();
    output += "'" + str + "',";
    
    boolean accepted = false;
    while (!accepted) {
      accepted = true;
      System.out.println();
      System.out.print("Enter your date of birth (YYYY-MM-DD): ");
      str = scanner.next();
      try {
        String tmp = new SimpleDateFormat("YYYY-MM-DD").parse(str).toString();
      }
      catch (ParseException e) {
        System.out.println("Wrong date format! Correct format is: YYYY-MM-DD");
        accepted = false;
      }
    }
    output += "'" + str + "',";
    scanner.nextLine();

    System.out.println();
    System.out.print("Enter your occupation: ");
    str = scanner.nextLine();
    output += "'" + str + "'";
    return output;
  }

}