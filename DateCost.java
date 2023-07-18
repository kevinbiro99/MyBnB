import java.sql.Date;

public class DateCost {
  
  private Date date;
  private double cost;

  public DateCost(Date date, double cost) {
    this.date = date;
    this.cost = cost;
  }

  public Date getDate() {
    return date;
  }

  public double getCost() {
    return cost;
  }
}
