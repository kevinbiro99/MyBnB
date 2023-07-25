import java.sql.Date;

public class DateCost {
  
  private Date start, end;
  private double cost;

  public DateCost(Date start, Date end, double cost) {
    this.start = start;
    this.end = end;
    this.cost = cost;
  }

  public Date getStartDate() {
    return start;
  }

  public Date getEndDate() {
    return end;
  }

  public double getCost() {
    return cost;
  }
}
