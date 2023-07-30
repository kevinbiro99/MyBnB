import java.io.IOException;
import java.sql.*;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;

/**
 * Handles user interaction with the terminal.
 */
public class MyBnB {

  private static ArrayList<String> functions = new ArrayList<String>();
  private static Scanner scanner;

  public static void main(String[] args) throws ClassNotFoundException, ParseException, SQLException, IOException {

    SqlDAO.getInstance().createDatabase("create_database.txt");

    scanner = new Scanner(System.in);  // Create a Scanner object
    functions = new ArrayList<>(Arrays.asList("user/create", "user/delete", "user/show", 
              "user/review", "user/seereview", "listing/search", "listing/search/dist", "listing/book", "bookings/show", "bookings/completestay", 
              "bookings/cancel", "listing/create", "listing/remove", "listing/update",
              "listing/review", "listing/seereview", "reports", "exit"));
    
    while(handle(selectFunction()));

    scanner.close();
    SqlDAO.deleteInstance();
  }

  public static boolean handle(String userInput) {
    try {
      switch (userInput.toLowerCase()) {
        case "user/create":
          User.registerUser(scanner);
          break;
        case "user/delete":
          User.deleteUser(scanner);
          break;
        case "user/show":
          User.showUsers();
          break;
        case "user/review":
          User.reviewUser(scanner);
          break;
        case "user/seereview":
          User.showUserReviews(scanner);
          break;
        case "listing/create":
          Listing.createListing(scanner);
          break;
        case "listing/update":
          Listing.updateListing(scanner);
          break;
        case "listing/remove":
          Listing.removeListing(scanner);
          break;
        case "listing/book":
          Listing.bookListing(scanner);
          break;
        case "listing/review":
          User.reviewListing(scanner);
          break;
        case "listing/seereview":
          User.showListingReviews(scanner);
          break;
        case "bookings/show":
          User.showBookingsForHost(scanner, -1);
          break;
        case "bookings/cancel":
          User.cancelBooking(scanner);
          break;
        case "bookings/completestay":
          User.completeStay(scanner);
          break;
        case "listing/search":
          Listing.listingSearch(scanner);
          break;
        case "reports":
          Report.report(scanner);
          break;
        case "exit":
          return false;
        default:
          System.out.println("Invalid operation");
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return true;
  }

  public static String selectFunction() {
    System.out.println("\nFunctions supported:");
    int index = 0;
    for (String f : functions) {
      System.out.format("%-25s", f);
      index ++;
      if (index % 4 == 0) {
        System.out.println();
      }
    }
    System.out.println("\n");
    System.out.print("Select a function: ");
    String choice = scanner.nextLine();
    System.out.println();
    return choice;
  }
}