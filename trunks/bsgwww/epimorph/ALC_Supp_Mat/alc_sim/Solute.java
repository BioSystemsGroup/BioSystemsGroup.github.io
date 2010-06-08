package alc_sim;

import sim.engine.SimState;
import sim.field.grid.ObjectGrid2D;
import sim.util.*;
import ec.util.*;

public class Solute extends Agent {
  
  private int id;
  private double amount;
  
  /** Creates a new instance of Solute */
  public Solute(int x, int y, ObjectGrid2D grid) {
    super(x,y,grid);
    this.id = 0;
    this.amount = 0.0;
  }
  public Solute(int x, int y, ObjectGrid2D grid, int newid, double newamt) {
    super(x,y,grid);
    this.id = newid;
    this.amount = newamt;
  }
  
  public int getID() {
    return id;
  }
  public void setID(int newid) {
    this.id = newid;
  }
  
  public double getAmount() {
    return amount;
  }
  public void setAmount(double newval) {
    amount = newval;
  }
  public void addAmount(double addval) {
    amount += addval;
  }
  public void subtractAmount(double minusval) {
    amount -= minusval;
  }
  
}
