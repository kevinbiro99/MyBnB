import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Scanner;
import java.util.ArrayList;

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

    /*
     * Only shows the bookings that the user made
     */
    public static ArrayList<Integer> showUserBookings(Scanner scanner, int sin) throws ClassNotFoundException, SQLException {
        ResultSet rs = SqlDAO.getInstance().getBookings(sin);

        System.out.println("Bookings: ");
        ArrayList<Integer> bookings = new ArrayList<Integer>();
        // Extract data from result set
        while(rs.next()){
            //Retrieve by column name
            int listing_id  = rs.getInt("listing_id");
            Date start = rs.getDate("start");
            Date end = rs.getDate("end");
            long card = rs.getLong("card");
            int booking_id = rs.getInt("booking_id");
            boolean complete = rs.getBoolean("complete");
            bookings.add(booking_id);
            
            //Display values
            System.out.print("booking_id: " + booking_id + ", (listing_id: " + listing_id);
            System.out.print(", start: " + start);
            System.out.print(", end: " + end);
            System.out.print(", complete: " + complete);
            System.out.println(", card: " + card + ")");
        }

        rs.close();
        return bookings;
    }

    /*
     * You can cancel a booking if you made it (in bookings table linked to your sin)
     * OR if you are the host of the listing in the bookings table.
     */
    public static void cancelBooking(Scanner scanner) throws ClassNotFoundException, SQLException {
        // show bookings the user made 
        System.out.println("Enter your SIN: ");
        int sin = scanner.nextInt();
        scanner.nextLine();
        if (!SqlDAO.getInstance().checkUserExists(sin)){
            System.out.println("INVALID USER SIN");
            return;
        }

        // show bookings made for listings the user is hosting
        ArrayList<Integer> bookings = showBookingsForHost(scanner, sin);

        if (bookings.isEmpty()) {
            System.out.println("This user has no bookings that are not complete");
            return;
        }

        // can cancel any of the above bookings
        System.out.println("Select a booking to cancel (booking_id): ");
        int booking = scanner.nextInt();
        scanner.nextLine();
        if (!bookings.contains(booking)) {
            System.out.println("This booking id: " + booking + " for this user does not exist or the stay is complete");
            return;
        }

        // update availability, insert into cancelled table, remove from booking table
        SqlDAO.getInstance().cancelBooking(booking, sin);
    }

    /*
     * Need to make sure all bookings are cancelled, and all listings have no bookings,
     * all listings are removed.
     * Keep bookings (cancellations), comments and ratings for reporting purposes
     */
    public static void deleteUser(Scanner scanner) throws ClassNotFoundException, SQLException {
        System.out.println("Enter the SIN of the user you want to delete: ");
        int sin = scanner.nextInt();
        scanner.nextLine();

        // Check if user has any bookings, listings, 
        if(SqlDAO.getInstance().userHasListingsOrBookings(sin)) {
            System.out.println("This user has listings and/or bookings that are not removed/cancelled");
            System.out.println("Cancel and remove all bookings and listings to delete your account");
            return;
        }

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

    /*
     * Will not show completed bookings because you cannot cancel those.
     */
    public static ArrayList<Integer> showBookingsForHost(Scanner scanner, int sin) throws ClassNotFoundException, SQLException {
        if (sin < 0) {
            System.out.println("Enter your SIN: ");
            sin = scanner.nextInt();
            scanner.nextLine();
            if (!SqlDAO.getInstance().checkUserExists(sin)){
                System.out.println("INVALID USER SIN");
                return null;
            }
        }
        
        ResultSet rs = SqlDAO.getInstance().getBookingsFromHost(sin);

        System.out.println("Bookings made by the user " + sin + ": ");
        ArrayList<Integer> bookings = new ArrayList<Integer>();

        boolean diffUsr = false;

        // Extract data from result set
        while(rs.next()){
            //Retrieve by column name
            int booking_id = rs.getInt("booking_id");
            int listing_id  = rs.getInt("listing_id");
            Date start = rs.getDate("start");
            Date end = rs.getDate("end");
            long card = rs.getLong("card");
            int booking_sin = rs.getInt("sin");
            boolean complete = rs.getBoolean("complete");

            if (!diffUsr && booking_sin != sin) {
                System.out.println("\nBookings made on your listings: ");
                diffUsr = true;
            }
        
            if (!complete) bookings.add(booking_id);

            //Display values
            System.out.print("booking_id: " + booking_id + ", (listing_id: " + listing_id);
            System.out.print(", start: " + start);
            System.out.print(", end: " + end);
            System.out.print(", booking_sin: " + booking_sin);
            System.out.print(", complete: " + complete);
            System.out.println(", card: " + card + ")");
        }

        rs.close();
        return bookings;
    }

    /*
     * Need to have completed a stay in order to comment or give a rating, same for hosts they can only
     * rate a renter if they completed a stay.
     */
    public static void completeStay(Scanner scanner) throws ClassNotFoundException, SQLException {
        System.out.println("Enter your SIN: ");
        int sin = scanner.nextInt();
        scanner.nextLine();
        if (!SqlDAO.getInstance().checkUserExists(sin)){
            System.out.println("INVALID USER SIN");
            return;
        }

        ArrayList<Integer> bookings = showUserBookings(scanner, sin);

        if (bookings.isEmpty()) {
            System.out.println("This user has no bookings");
            return;
        }

        System.out.println("Select a booking to complete (booking_id): ");
        int booking = scanner.nextInt();
        scanner.nextLine();
        if (!bookings.contains(booking)) {
            System.out.println("This booking id: " + booking + " for this user does not exist");
            return;
        }

        // update availability, insert into cancelled table, remove from booking table
        SqlDAO.getInstance().completeStay(booking);

    }

    /*
     * Can only review a user if you have rented from this user or have hosted a listing
     * that this user has rented. This information is ONLY in Bookings since you 
     * need to have completed the booking to comment and rate.
     */
    public static void reviewUser(Scanner scanner) throws ClassNotFoundException, SQLException {
        System.out.println("Enter your SIN: ");
        int sin = scanner.nextInt();
        scanner.nextLine();
        if (!SqlDAO.getInstance().checkUserExists(sin)){
            System.out.println("INVALID USER SIN");
            return;
        }

        // show all users in database that this user has completed a stay with
        ArrayList<Integer> users = showAllUsersAndHostsFromCompletedRentals(sin);

        if (users.isEmpty()) {
            System.out.println("You have not completed any stays or do not have renters who completed stays at your listings to make a user review");
            return;
        }

        System.out.println("Select a user to review (user_sin): ");
        int user = scanner.nextInt();
        scanner.nextLine();
        if (!users.contains(user)) {
            System.out.println("This user sin: " + user + " does not exist");
            return;
        }

        System.out.println("Enter a comment for this user (1000 chars max): ");
        String comment = scanner.nextLine();
        System.out.println("Enter a rating for this user (1-5): ");
        int rating = scanner.nextInt();
        scanner.nextLine();
        while(rating < 1 || rating > 5) {
            System.out.println("Please enter a valid rating in the range 1-5: ");
            rating = scanner.nextInt();
            scanner.nextLine();
        }

        SqlDAO.getInstance().insertUserReview(sin, user, comment, rating);
    }

    public static ArrayList<Integer> showAllUsersAndHostsFromCompletedRentals(int renter_sin) throws ClassNotFoundException, SQLException {
        ResultSet rs = SqlDAO.getInstance().getUsersAndHostsFromCompletedRentals(renter_sin);

        System.out.println("Hosts you can review: ");
        ArrayList<Integer> users = new ArrayList<Integer>();
        // Extract data from result set
        while(rs.next()){
            //Retrieve by column name
            int sin  = rs.getInt("user");
            users.add(sin);
            
            //Display values
            System.out.println("User sin: " + sin);
        }

        rs.close();
        return users;
    }

    public static void showUserReviews(Scanner scanner) throws ClassNotFoundException, SQLException {
        System.out.println("Enter the SIN of the user to see their reviews: ");
        int sin = scanner.nextInt();
        scanner.nextLine();
        if (!SqlDAO.getInstance().checkUserExists(sin)){
            System.out.println("INVALID USER SIN");
            return;
        }

        ResultSet rs = SqlDAO.getInstance().getUserReviews(sin);

        System.out.println("Reviews for this user: ");
        // Extract data from result set
        while(rs.next()){
            //Retrieve by column name
            int poster = rs.getInt("poster_sin");
            String comment = rs.getString("comment");
            int rating = rs.getInt("rating");
            
            //Display values
            System.out.println("Comment from " + poster + ": ");
            printFormattedText(comment, 50);
            System.out.println("Rating: " + rating);
            System.out.println();
        }

        rs.close();
    }

    public static void printFormattedText(String text, int lineWidth) {
        if (text == null || text.isEmpty()) {
            System.out.println("The input text is empty.");
            return;
        }

        StringBuilder formattedText = new StringBuilder();
        StringBuilder wordBuffer = new StringBuilder();
        int currentLineWidth = 0;

        for (char c : text.toCharArray()) {
            if (Character.isWhitespace(c)) {
                if (currentLineWidth + wordBuffer.length() + 1 > lineWidth) {
                    formattedText.append(System.lineSeparator());
                    currentLineWidth = 0;
                }

                formattedText.append(wordBuffer).append(c);
                currentLineWidth += wordBuffer.length() + 1;
                wordBuffer.setLength(0);
            } else {
                wordBuffer.append(c);
            }
        }

        if (currentLineWidth + wordBuffer.length() > lineWidth) {
            formattedText.append(System.lineSeparator());
        }

        formattedText.append(wordBuffer);

        System.out.println(formattedText.toString());
    }

    /*
     * Can only review a listing if you have completed a stay recently
     */
    public static void reviewListing(Scanner scanner) throws ClassNotFoundException, SQLException {
        System.out.println("Enter your SIN: ");
        int sin = scanner.nextInt();
        scanner.nextLine();
        if (!SqlDAO.getInstance().checkUserExists(sin)){
            System.out.println("INVALID USER SIN");
            return;
        }

        // show all listings in database that this user has rented and completed a stay
        ArrayList<Integer> listings = showAllCompletedStays(sin);

        if (listings.isEmpty()) {
            System.out.println("You have not completed any stays to make a listing review");
            return;
        }

        System.out.println("Select a listing to review (listing_id): ");
        int listing = scanner.nextInt();
        scanner.nextLine();
        if (!listings.contains(listing)) {
            System.out.println("This listing id: " + listing + " does not exist");
            return;
        }

        System.out.println("Enter a comment for this listing (1000 chars max): ");
        String comment = scanner.nextLine();
        System.out.println("Enter a rating for this listing (1-5): ");
        int rating = scanner.nextInt();
        scanner.nextLine();
        while(rating < 1 || rating > 5) {
            System.out.println("Please enter a valid rating in the range 1-5: ");
            rating = scanner.nextInt();
            scanner.nextLine();
        }

        SqlDAO.getInstance().insertListingReview(sin, listing, comment, rating);
    }

    public static ArrayList<Integer> showAllCompletedStays(int sin) throws ClassNotFoundException, SQLException {
        ResultSet rs = SqlDAO.getInstance().getCompletedStays(sin);

        System.out.println("Listings you can review: ");
        ArrayList<Integer> listings = new ArrayList<Integer>();
        // Extract data from result set
        while(rs.next()){
            //Retrieve by column name
            int listing_id  = rs.getInt("listing_id");
            listings.add(listing_id);
            
            //Display values
            System.out.println("Listing: " + listing_id);
        }

        rs.close();
        return listings;
    }

    public static void showListingReviews(Scanner scanner) throws ClassNotFoundException, SQLException {
        ArrayList<Integer> listings = Listing.displayAllListings();
        System.out.println("Enter the listing id to see their reviews: ");
        int listing = scanner.nextInt();
        scanner.nextLine();
        if (!listings.contains(listing)){
            System.out.println("INVALID LISTING ID");
            return;
        }

        ResultSet rs = SqlDAO.getInstance().getListingReviews(listing);

        System.out.println("Reviews for this listing: ");
        // Extract data from result set
        while(rs.next()){
            //Retrieve by column name
            int poster = rs.getInt("poster_sin");
            String comment = rs.getString("comment");
            int rating = rs.getInt("rating");
            
            //Display values
            System.out.println("Comment from " + poster + ": ");
            printFormattedText(comment, 50);
            System.out.println("Rating: " + rating);
            System.out.println();
        }

        rs.close();
    }
}
