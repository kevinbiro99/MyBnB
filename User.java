import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Scanner;

/*
 * This class handles all user table interactions
 */
public class User {
    /*
     * Display all users
     */
    public static void showUsers() throws ClassNotFoundException, SQLException {
        ResultSet rs = SqlDAO.getInstance().getUsers();

        // Extract data from result set
        while(rs.next()){
            //Retrieve by column name
            int sid  = rs.getInt("sin");
            String sname = rs.getString("name");
            Date dob = rs.getDate("dob");
            int age = rs.getInt("age");
        
            //Display values
            System.out.print("ID: " + sid);
            System.out.print(", Name: " + sname);
            System.out.print(", DOB: " + dob);
            System.out.println(", Age: " + age);
        }

        rs.close();
    }

    // Deletes the user and all associated data (listings, bookings, reviews)
    public static void deleteUser(Scanner scanner) throws ClassNotFoundException, SQLException {
        System.out.println("Enter the SIN of the user you want to delete: ");
        int sin = scanner.nextInt();
        scanner.nextLine();
        SqlDAO.getInstance().deleteUser(sin);
    }

    /*
     * Register a new user with valid parameters
     */
    public static void registerUser(Scanner scanner) throws ParseException, ClassNotFoundException, SQLException{
        System.out.print("Enter your SIN: ");
        int sin = scanner.nextInt();
        scanner.nextLine();
        
        System.out.println();
        System.out.print("Enter your name: ");
        String name = scanner.nextLine();
        
        System.out.println();
        System.out.print("Enter your postal code: ");
        String postal = scanner.nextLine();
        
        System.out.println();
        System.out.print("Enter your city: ");
        String city = scanner.nextLine();
        
        System.out.println();
        System.out.print("Enter your country: ");
        String country = scanner.nextLine();
        
        boolean accepted = false;
        String dob = "";
        while (!accepted) {
            accepted = true;
            System.out.println();
            System.out.print("Enter your date of birth (YYYY-MM-DD): ");
            dob = scanner.next();
            try {
                new SimpleDateFormat("YYYY-MM-DD").parse(dob).toString();
            }
            catch (ParseException e) {
                System.out.println("Wrong date format! Correct format is: YYYY-MM-DD");
                accepted = false;
            }
        }
        scanner.nextLine();

        System.out.println();
        System.out.print("Enter your occupation: ");
        String occupation = scanner.nextLine();
        
        // Insert into database
        SqlDAO.getInstance().registerUser(sin, name, postal, city, country, dob, occupation);
    }
}