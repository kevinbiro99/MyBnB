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

    scanner = new Scanner(System.in); // Create a Scanner object
    functions = new ArrayList<>(Arrays.asList("user/create", "user/delete", "user/show",
        "user/review", "listing/search", "listing/book", "listing/cancel", "listing/create", "listing/remove",
        "listing/update",
        "listing/review", "reports", "exit"));

    while (handle(selectFunction()))
      ;

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
        case "listing/create":
          Listing.createListing(scanner);
          break;
        case "listing/update":
          Listing.updateListing(scanner);
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
      System.out.format("%-20s", f);
      index++;
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