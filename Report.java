import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Scanner;

public class Report {
  public static void report(Scanner scanner) throws ClassNotFoundException, SQLException {
    String input = "";

    /*
     * This is a query that identifies the possible commercial hosts, something that
     * the system should flag and prohibit
     * 
     * What do we actually do about this, prevent making more listings? warning? ban
     * user?
     */

    ArrayList<InputKey> options = new ArrayList<InputKey>();
    options.add(new InputKey(1, "d", ""));
    options.add(new InputKey(2, "c", "the total number of listings per country"));
    options.add(new InputKey(3, "cc", "the total number of listings per country and city"));
    options.add(new InputKey(4, "ccp", "the total number of listings per country, city, and postal code"));
    options.add(new InputKey(5, "lc", "the number of listings each host has by country"));
    options.add(new InputKey(6, "lcc", "the number of listings each host has by country and city"));
    options.add(new InputKey(7, "m10", "the hosts that have more than 10% of listings in ac country"));
    options.add(new InputKey(10, "t", ""));

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
          reportDateRangeByCity();
          break;
        case 2:
          reportTotalListingByCountry();
          break;
        case 3:
          reportTotalListingByCountryCity();
          break;
        case 4:
          reportTotalListingByCCP();
          break;
        case 5:
          reportRankHostsByListingCountry();
          break;
        case 6:
          reportRankHostByListingCountryCity();
          break;
        case 7:
          reportMorethan10percent();
          break;
        case 10:
          myTestFunction(scanner);
          break;
        default:
          System.out.println();
          System.out.println("Invalid operation");
      }
      System.out.println();
    }
  }

  public static void myTestFunction(Scanner scanner) throws ClassNotFoundException, SQLException {
    ResultSet rs = SqlDAO.getInstance().morethan10percent();
    while (rs.next()) {
      System.out.println(rs.getString("sin") + ", " + rs.getString("country") + ", " + rs.getString("city") + ", "
          + rs.getInt("count") + ", " + rs.getInt("total"));
    }
  }

  public static void reportDateRangeByCity() throws ClassNotFoundException, SQLException {

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
          + rs.getString("sin") + ", " + rs.getInt("count"));
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