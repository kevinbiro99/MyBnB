import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Scanner;

import com.mysql.cj.protocol.Resultset;

public class Report {
  public static void report(Scanner scanner) throws ClassNotFoundException, SQLException {
    String input = "";

    ArrayList<InputKey> options = new ArrayList<InputKey>();
    options.add(new InputKey(1, "d", "distance"));
    options.add(new InputKey(2, "c", "the total number of listings per country"));
    options.add(new InputKey(3, "cc", "the total number of listings per country and city"));
    options.add(new InputKey(4, "ccp", "the total number of listings per country, city, and postal code"));

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
          reportTotalListingByCCP(scanner, 0);
          break;
        case 3:
          reportTotalListingByCCP(scanner, 1);
          break;
        case 4:
          reportTotalListingByCCP(scanner, 2);
          break;
        default:
          System.out.println();
          System.out.println("Invalid operation");
      }
      System.out.println();
    }
  }

  public static void reportDateRangeByCity(Scanner scanner) {
    // Get all city
    // For each city get all booking,
  }

  public static void reportTotalListingByCCP(Scanner scanner, int mode)
      throws SQLException, ClassNotFoundException {
    ResultSet rs = SqlDAO.getInstance().getAllCCP();
    ArrayList<CCPcounter> countries = new ArrayList<>();
    String country, city, postal;
    boolean found;

    while (rs.next()) {
      country = rs.getString("country");
      city = rs.getString("city");
      postal = rs.getString("postal_code");
      found = false;
      for (CCPcounter c : countries) {
        if (c.getCountry().equalsIgnoreCase(country)) {
          if (mode == 0) {
            c.setCount(c.getCount() + 1);
            found = true;
            break;
          }
          if (c.getCity().equalsIgnoreCase(city)) {
            if (mode == 1) {
              c.setCount(c.getCount() + 1);
              found = true;
              break;
            }
            if (c.getPostal().equalsIgnoreCase(postal)) {
              c.setCount(c.getCount() + 1);
              found = true;
              break;
            }
          }
        }
      }
      if (!found) {
        if (mode == 0)
          countries.add(new CCPcounter(country));
        if (mode == 1)
          countries.add(new CCPcounter(country, city));
        if (mode == 2)
          countries.add(new CCPcounter(country, city, postal));
      }
    }

    for (CCPcounter c : countries) {
      System.out.println(c);
    }
  }
}

class CCPcounter {
  private String country, city, postal;
  private int count;

  public CCPcounter(String country, String city, String postal) {
    this.country = country;
    this.city = city;
    this.postal = postal;
    this.count = 1;
  }

  public CCPcounter(String country, String city) {
    this.country = country;
    this.city = city;
    this.postal = "NULL";
    this.count = 1;
  }

  public CCPcounter(String country) {
    this.country = country;
    this.city = "NULL";
    this.postal = "NULL";
    this.count = 1;
  }

  public CCPcounter() {
    this.country = "NULL";
    this.city = "NULL";
    this.postal = "NULL";
    this.count = 1;
  }

  public String getCountry() {
    return country;
  }

  public void setCountry(String country) {
    this.country = country;
  }

  public String getCity() {
    return city;
  }

  public void setCity(String city) {
    this.city = city;
  }

  public String getPostal() {
    return postal;
  }

  public void setPostal(String postal) {
    this.postal = postal;
  }

  public int getCount() {
    return count;
  }

  public void setCount(int count) {
    this.count = count;
  }

  @Override
  public String toString() {
    if (city.equals("NULL") && postal.equals("NULL"))
      return "[" + country + ", " + count + "]";
    if (postal.equals("NULL"))
      return "[" + country + ", " + city + ", " + count + "]";
    return "[" + country + ", " + city + ", " + postal + ", " + count + "]";
  }
}
