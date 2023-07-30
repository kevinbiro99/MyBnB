public class ListingObject {
  private int id;
  private double dist, lat, lon, cost;
  private String type, pos, city, country, startDate, endDate;

  public ListingObject(int id, double dist, double lat, double lon, double cost, String type, String pos, String city,
      String country) {
    this.id = id;
    this.dist = dist;
    this.lat = lat;
    this.lon = lon;
    this.cost = cost;
    this.type = type;
    this.pos = pos;
    this.city = city;
    this.country = country;
  }

  @Override
  public String toString() {
    String distString = Math.round(dist * 10.0) / 10.0 + "km";
    String costString = "$" + cost;
    if (cost < 0)
      costString = "Unknown";
    if (dist < 0)
      distString = "Unknown";
    return String.format("%1$-10s%2$-10s%3$-15s%4$-15s%5$-15s%6$-15s%7$-15s%8$-15s%9$-15s%10$-15s", distString,
        costString, startDate, endDate, type, city, pos, country, lat, lon);
    // return "[dist=" + distString + ", lat=" + lat + ", lon=" + lon + ", cost=" +
    // cost + ", type=" + type
    // + ", pos=" + pos + ", city=" + city + ", country=" + country + ", id=" + id +
    // "]";
  }

  public double getDist() {
    return dist;
  }

  public double getLat() {
    return lat;
  }

  public double getLon() {
    return lon;
  }

  public String getType() {
    return type;
  }

  public String getPos() {
    return pos;
  }

  public String getCity() {
    return city;
  }

  public String getCountry() {
    return country;
  }

  public void setDist(double dist) {
    this.dist = dist;
  }

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public double getCost() {
    return cost;
  }

  public void setCost(double cost) {
    this.cost = cost;
  }

  public String getStartDate() {
    return startDate;
  }

  public void setStartDate(String startDate) {
    this.startDate = startDate;
  }

  public String getEndDate() {
    return endDate;
  }

  public void setEndDate(String endDate) {
    this.endDate = endDate;
  }

}
