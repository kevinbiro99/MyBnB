import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Scanner;

public class Report {
  public static void report(Scanner scanner) throws ClassNotFoundException, SQLException {
    String input = "";

    /*
     * "This is a query that identifies the possible commercial hosts, something
     * that
     * the system should flag and prohibit"
     * 
     * What do we actually do about this, prevent making more listings? warning? ban
     * user?
     * 
     * Still need:
     * -We would like to run a report that presents for each listing the set
     * of most popular noun phrases associated with the listing
     */


    /*
     * reportDateRangeByCity
     * reportDateRangeByCityAndPostal
     * reportTotalListingByCountry
     * reportTotalListingByCountryCity
     * reportTotalListingByCCP
     * reportRankHostsByListingCountry
     * reportRankHostByListingCountryCity
     * reportMorethan10percent
     * reportRankRenterByBookings
     * reportRankRenterByBookingInDateRangeAndCity
     * reportLargestCancellationsInYear
     */
    ArrayList<InputKey> options = new ArrayList<InputKey>();
    options.add(new InputKey(1, "1", "the total number of bookings in a specific date range by city"));
    options.add(new InputKey(2, "2", "the total number of bookings in a specific date range by city and zip code"));
    options.add(new InputKey(3, "3", "the total number of listings per country"));
    options.add(new InputKey(4, "4", "the total number of listings per country and city"));
    options.add(new InputKey(5, "5", "the total number of listings per country, city, and postal code"));
    options.add(new InputKey(6, "6", "the rank of hosts by the total number of listings they have overall per country"));
    options.add(new InputKey(7, "7", "the rank of hosts by the total number of listings they have overall per country and city"));
    options.add(new InputKey(8, "8", "the hosts that have more than 10% of listings in a country and city"));
    options.add(new InputKey(9, "9", "the rank of renters by the number of bookings in a given time period"));
    options.add(new InputKey(10, "10", "the rank of renters by the number of bookings in a given time period per city"));
    options.add(new InputKey(11, "11", "the hosts and renters with the largest number of cancellations within a year"));

    while (!input.equals("q")) {
      int selected = -1;
      System.out.println("Choose a report, [q] to quit: ");
      for (InputKey option : options) {
        System.out.println("[" + option.getKey() + "] for " + option.getDescription());
      }
      input = scanner.nextLine();
      for (InputKey option : options) {
        if (input.equalsIgnoreCase(option.getKey() + ""))
          selected = option.getId();
      }

      System.out.println();
      switch (selected) {
        case 1:
          reportDateRangeByCity(scanner);
          break;
        case 2:
          reportDateRangeByCityAndPostal(scanner);
          break;
        case 3:
          reportTotalListingByCountry();
          break;
        case 4:
          reportTotalListingByCountryCity();
          break;
        case 5:
          reportTotalListingByCCP();
          break;
        case 6:
          reportRankHostsByListingCountry();
          break;
        case 7:
          reportRankHostByListingCountryCity();
          break;
        case 8:
          reportMorethan10percent();
          break;
        case 9:
          reportRankRenterByBookings(scanner);
          break;
        case 10:
          reportRankRenterByBookingInDateRangeAndCity(scanner);
          break;
        case 11:
          reportLargestCancellationsInYear(scanner);
          break;
        default:
          System.out.println();
          System.out.println("Invalid operation");
      }
      System.out.println();
    }
  }

  public static void myTestFunction(Scanner scanner) throws ClassNotFoundException, SQLException {

  }

  public static void reportLargestCancellationsInYear(Scanner scanner) throws ClassNotFoundException, SQLException {
    System.out.print("Enter the year (YYYY): ");
    String year = scanner.next();
    scanner.nextLine();

    try {
      LocalDate.parse(year + "-01-01", DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    } catch (DateTimeParseException e) {
      System.out.println("Wrong date format! Correct format is YYYY!");
      return;
    }

    ResultSet rs = SqlDAO.getInstance().rankHostByCancellations(year);
    int temp = 0;
    int highest = 0;
    System.out.println("Hosts: ");
    while (rs.next()) {
      temp = rs.getInt("count");
      if (temp >= highest) {
        highest = temp;
        System.out.println(rs.getString("canceller_sin") + ", " + temp);
      } else {
        break;
      }
    }

    rs = SqlDAO.getInstance().rankRenterByCancellations(year);
    temp = 0;
    highest = 0;
    System.out.println("\nRenters: ");

    while (rs.next()) {
      temp = rs.getInt("count");
      if (temp >= highest) {
        highest = temp;
        System.out.println(rs.getString("canceller_sin") + ", " + temp);
      } else {
        break;
      }
    }
  }

  public static void reportRankRenterByBookingInDateRangeAndCity(Scanner scanner)
      throws ClassNotFoundException, SQLException {
    System.out.print("Enter the starting date of the range (YYYY-MM-DD): ");
    String start = scanner.next();
    System.out.print("Enter the end date of the range (YYYY-MM-DD): ");
    String end = scanner.next();
    scanner.nextLine();

    try {
      LocalDate.parse(start, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
      LocalDate.parse(end, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    } catch (DateTimeParseException e) {
      System.out.println("Wrong date format! Correct format is YYYY-MM-DD!");
    }

    ResultSet rs = SqlDAO.getInstance().rankRenterByBookingInDateRangeAndCity(start, end, start.substring(0, 4));

    while (rs.next()) {
      System.out.println(rs.getString("sin") + ", " + rs.getString("city") + ", " + rs.getInt("count"));
    }
  }

  public static void reportRankRenterByBookings(Scanner scanner) throws ClassNotFoundException, SQLException {
    System.out.print("Enter the starting date of the range (YYYY-MM-DD): ");
    String start = scanner.next();
    System.out.print("Enter the end date of the range (YYYY-MM-DD): ");
    String end = scanner.next();
    scanner.nextLine();

    try {
      LocalDate.parse(start, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
      LocalDate.parse(end, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    } catch (DateTimeParseException e) {
      System.out.println("Wrong date format! Correct format is YYYY-MM-DD!");
    }

    ResultSet rs = SqlDAO.getInstance().rankRenterByBookingInDateRange(start, end);

    while (rs.next()) {
      System.out.println(rs.getString("sin") + ", " + rs.getInt("count"));
    }
  }

  public static void reportDateRangeByCityAndPostal(Scanner scanner) throws ClassNotFoundException, SQLException {
    System.out.print("Enter the starting date of the range (YYYY-MM-DD): ");
    String start = scanner.next();
    System.out.print("Enter the end date of the range (YYYY-MM-DD): ");
    String end = scanner.next();
    scanner.nextLine();
    System.out.print("Enter city: ");
    String city = scanner.nextLine();

    try {
      LocalDate.parse(start, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
      LocalDate.parse(end, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    } catch (DateTimeParseException e) {
      System.out.println("Wrong date format! Correct format is YYYY-MM-DD!");
    }

    ResultSet rs = SqlDAO.getInstance().countBookingInDateRangeByPostal(start, end, city);

    System.out.println("Postal code: [total bookings]");
    while (rs.next()) {
      System.out.println(rs.getString("postal_code") + ": " + rs.getInt("count"));
    }
  }

  public static void reportDateRangeByCity(Scanner scanner) throws ClassNotFoundException, SQLException {
    System.out.print("Enter the starting date of the range (YYYY-MM-DD): ");
    String start = scanner.next();
    System.out.print("Enter the end date of the range (YYYY-MM-DD): ");
    String end = scanner.next();
    scanner.nextLine();

    try {
      LocalDate.parse(start, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
      LocalDate.parse(end, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    } catch (DateTimeParseException e) {
      System.out.println("Wrong date format! Correct format is YYYY-MM-DD!");
    }

    ResultSet rs = SqlDAO.getInstance().countBookingInDateRangeByCity(start, end);

    System.out.println("City: [total bookings]");
    while (rs.next()) {
      System.out.println(rs.getString("city") + ": " + rs.getInt("count"));
    }
  }

  public static void reportMorethan10percent() throws ClassNotFoundException, SQLException {
    ResultSet rs = SqlDAO.getInstance().morethan10percent();
    System.out.println("Sin, country, city");
    while (rs.next()) {
      System.out.println(rs.getString("sin") + ", " + rs.getString("country") + ", " + rs.getString("city"));
    }
  }

  public static void reportRankHostByListingCountryCity() throws ClassNotFoundException, SQLException {
    ResultSet rs = SqlDAO.getInstance().countListingByHostAndCities();
    System.out.println("Country, city, sin: [total listings]");
    while (rs.next()) {
      System.out.println(rs.getString("country") + ", " + rs.getString("city") + ", "
          + rs.getString("sin") + ": " + rs.getInt("count"));
    }
  }

  public static void reportRankHostsByListingCountry() throws ClassNotFoundException, SQLException {
    ResultSet rs = SqlDAO.getInstance().countListingByHostAndCountry();
    System.out.println("Country, sin: [total listings]");
    while (rs.next()) {
      System.out.println(rs.getString("country") + ", " + rs.getString("sin") + ": " + rs.getInt("count"));
    }
  }

  public static void reportTotalListingByCCP() throws ClassNotFoundException, SQLException {
    ResultSet rs = SqlDAO.getInstance().countListingInPostals();
    System.out.println("Country, city, postal: [total listings]");
    while (rs.next()) {
      System.out.println(rs.getString("country") + ", " + rs.getString("city") + ", " + rs.getString("postal_code")
          + ": " + rs.getInt("count"));
    }
  }

  public static void reportTotalListingByCountryCity() throws SQLException, ClassNotFoundException {
    ResultSet rs = SqlDAO.getInstance().countListingInCities();
    System.out.println("Country, city: [total listings]");
    while (rs.next()) {
      System.out.println(rs.getString("country") + ", " + rs.getString("city") + ": " + rs.getInt("count"));
    }
  }

  public static void reportTotalListingByCountry()
      throws SQLException, ClassNotFoundException {
    ResultSet rs = SqlDAO.getInstance().countListingInCountries();
    System.out.println("Country: [total listings]");
    while (rs.next()) {
      System.out.println(rs.getString("country") + ": " + rs.getInt("count"));
    }
  }
}