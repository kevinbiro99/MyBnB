import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Scanner;

/*
 * This class handles all user table interactions
 */
public class User {
    /*
     * Display all users
     */
    private static String dobFormat = "yyyy-MM-dd";
    private static int minAge = 18;

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

    public static void showUserBookings(Scanner scanner) throws ClassNotFoundException, SQLException {
        System.out.println("Enter your SIN: ");
        int sin = scanner.nextInt();
        scanner.nextLine();
        if (!SqlDAO.getInstance().checkUserExists(sin)){
            System.out.println("INVALID USER SIN");
            return;
        }
        
        ResultSet rs = SqlDAO.getInstance().getBookings(sin);

        System.out.println("Bookings: ");
        // Extract data from result set
        while(rs.next()){
            //Retrieve by column name
            int listing_id  = rs.getInt("listing_id");
            Date start = rs.getDate("start");
            Date end = rs.getDate("end");
            int card = rs.getInt("card");
        
            //Display values
            System.out.print("(listing_id: " + listing_id);
            System.out.print(", start: " + start);
            System.out.print(", end: " + end);
            System.out.println(", card: " + card + ")");
        }

        rs.close();
    }

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
        int sin;
        System.out.print("Enter your SIN: ");
        sin = scanner.nextInt();
        scanner.nextLine();
        
        while(SqlDAO.getInstance().checkUserExists(sin)){
            System.out.println("User with SIN already exists!");
            System.out.print("Enter your SIN: ");
            sin = scanner.nextInt();
            scanner.nextLine();
        };

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
            System.out.print("\nEnter your date of birth ("+dobFormat.toUpperCase()+"): ");
            dob = scanner.next();
            try {
                LocalDate formatteddob = LocalDate.parse(dob, DateTimeFormatter.ofPattern(dobFormat));
                if(Period.between(formatteddob, LocalDate.now()).getYears() < minAge){
                    System.out.print("You must be older than "+minAge+" to register!");
                    accepted = false;
                }
            }
            catch (DateTimeParseException  e) {
                System.out.println("Wrong date format! Correct format is: YYYY-MM-DD");
                accepted = false;
            }
        }
        scanner.nextLine();

        //Check age, if too young try again or kcik them out?

        System.out.println();
        System.out.print("Enter your occupation: ");
        String occupation = scanner.nextLine();
        
        // Insert into database
        SqlDAO.getInstance().registerUser(sin, name, postal, city, country, dob, occupation);
    }
}
