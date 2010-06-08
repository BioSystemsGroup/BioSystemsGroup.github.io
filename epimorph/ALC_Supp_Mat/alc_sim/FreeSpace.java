package alc_sim;

import sim.engine.SimState;
import sim.field.grid.ObjectGrid2D;
import sim.util.*;
import ec.util.*;

public class FreeSpace extends Agent {
  
  private Bag solutes;
  private MersenneTwisterFast random_func;
  
  public FreeSpace(int x, int y, ObjectGrid2D grid) {
    super(x,y,grid);
    solutes = new Bag();
    random_func = new MersenneTwisterFast(System.currentTimeMillis());
  } // end Lumen()
  
  public void intake (int id, double amount) {
    boolean present = false;
    for (int i = 0; i < solutes.numObjs; i++) {
      Solute curSolute = (Solute) solutes.get(i);
      if (id == curSolute.getID()) {
        curSolute.addAmount(amount);
        present = true;
      }
    }
    if (!present) {
      Solute newSolute = new Solute(x,y,grid,id,amount);
      solutes.add(newSolute);
    }
  } // end receive()
  
  public void step(SimState state) {
  } // end step()
  
  public void diffuse() {
    Bag neighbors = new Bag();
    IntBag xCoord = new IntBag();
    IntBag yCoord = new IntBag();
    Bag matrixNeighbors = new Bag();
    Bag freeSpaceNeighbors = new Bag();
    
    this.grid.getNeighborsHexagonalDistance(this.x, this.y, 1, true, neighbors, xCoord, yCoord);
    neighbors.remove(this);
    for (int i = 0; i < neighbors.numObjs; i++) {
      Agent agent = (Agent) neighbors.get(i);
      if (agent instanceof Matrix) {
        matrixNeighbors.add(agent);
      }
      if (agent instanceof FreeSpace) {
        freeSpaceNeighbors.add(agent);
      }
    }
    freeSpaceNeighbors.shuffle(random_func);
    for (int i = 0; i < freeSpaceNeighbors.numObjs; i++) {
      FreeSpace curLumen = (FreeSpace) freeSpaceNeighbors.get(i);
      for (int j = 0; j < solutes.numObjs; j++) {
        Solute curSolute = (Solute) solutes.get(j);
        int curID = curSolute.getID();
        double transportAmount = 0.0;
        curLumen.intake(curID, transportAmount);
      }
    }
    
  } // end diffuse()

 
} // end class Lumen
