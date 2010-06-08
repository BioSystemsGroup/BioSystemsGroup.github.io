package alc_sim;

import sim.field.grid.ObjectGrid2D;
import sim.util.*;

public class Cell extends Agent {
  
  public static final int UP = 0;
  public static final int UR = 1;
  public static final int DR = 2;
  public static final int DN = 3;
  public static final int DL = 4;
  public static final int UL = 5;
  
  boolean alive = true;
  boolean moved = false;
  Cluster cluster = null;
  
  /** Creates a new instance of Cell */
  public Cell(int x, int y, ObjectGrid2D grid) {
    super(x, y, grid);
  }
  
  public boolean alive() {
    return alive;
  }
  
  public void setCluster (Cluster newCluster) {
    cluster = newCluster;
  }
  public Cluster getCluster() {
    return cluster;
  }
  
  public boolean moved() {
    return moved;
  }
  
  public void addDistanceMovedTotal(int dist) {
    return;
  }
  
  // determines whether the two agents are neighbors
  public boolean areNeighbors(Agent a, Agent b) {
    IntBag xPositions = new IntBag();
    IntBag yPositions = new IntBag();
    Bag neighbors = new Bag();
    Bag matrixNeighbors = new Bag();
    Bag lumenNeighbors = new Bag();
    Bag cellNeighbors = new Bag();
    Bag mediaNeighbors = new Bag();
    boolean toroidal = true;
    int radius = 1;
    
    this.grid.getNeighborsHexagonalDistance(a.x, a.y, 1, toroidal, neighbors, xPositions, yPositions);
    neighbors.remove(this);
    for (int i = 0; i < neighbors.numObjs; i++) {
      Agent agent = (Agent) neighbors.get(i);
      if (b == agent) {
        return true;
      }
    }
    return false;
  } // end areNeighbors()
  
  
  
}
