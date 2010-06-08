package alc_sim;

import sim.engine.SimState;
import sim.engine.Steppable;
import sim.field.grid.ObjectGrid2D;

public abstract class Agent implements Steppable {
  int x,y;
  final ObjectGrid2D grid;
  
  public Agent(int x, int y, ObjectGrid2D grid) {
    this.x = x;
    this.y = y;
    this.grid = grid;
  } // end Agent()
  
  public void setCoordinates(int newX, int newY) {
    this.x = newX;
    this.y = newY;
  }
  
  public void moveTo(int newX, int newY) {
    this.x = newX;
    this.y = newY;
    this.grid.set(newX, newY, this);
  }
  
  public void step(SimState state) {
  
  } // end step()

} // end abstract class Agent
