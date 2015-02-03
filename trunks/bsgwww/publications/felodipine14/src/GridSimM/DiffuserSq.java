package GridSimM;

import sim.engine.*;
import sim.util.*;
import sim.field.grid.*;

public class DiffuserSq implements Steppable {
  
  DoubleGrid2D update_grid;
  DoubleGrid2D temp_grid;
  double evaporation_rate;
  double diffusion_rate;
  int counter = 0;
  
  /** Creates a new instance of Diffuser */
  public DiffuserSq( final DoubleGrid2D updateGrid, final DoubleGrid2D tempGrid,
                   final double evaporationRate, final double diffusionRate ) {
    this.update_grid = updateGrid;
    this.temp_grid = tempGrid;
    this.evaporation_rate = evaporationRate;
    this.diffusion_rate = diffusionRate;
  }
  
  public void step(SimState state) {
    // locals are faster than instance variables
    final DoubleGrid2D v = update_grid;  // shorter
    final double[][] updateGridField = update_grid.field;
    final double[][] tempGridField = temp_grid.field;
    final int gridWidth = v.getWidth();
    final int gridWidthLim = gridWidth - 1;
    final int gridHeight = v.getHeight();
    final int gridHeightLim = gridHeight - 1;
    final double evaporationRate = evaporation_rate;
    final double diffusionRate = diffusion_rate;
    
    double average = 0.0;
    for (int x = 0; x < gridWidth; x++) {
      for (int y = 0; y < gridHeight; y++) {
        // hexagonal representation
        if (GridSim.TOROIDAL) {
          average = (updateGridField[x][y] + 
                     updateGridField[v.stx(x-1)][y] +
                     updateGridField[v.stx(x+1)][y] + 
                     updateGridField[x][v.sty(y-1)] + 
                     updateGridField[x][v.sty(y+1)]) / 5.0;
        }
        else {
          average = updateGridField[x][y];
          int divider = 1;
          if (x != 0) { average += updateGridField[x-1][y]; divider++; }
          if (x != gridWidthLim) { average += updateGridField[x+1][y]; divider++; }
          if (y != 0) { average += updateGridField[x][y-1]; divider++; }
          if (y != gridHeightLim) { average += updateGridField[x][y+1]; divider++; }
          average = average / divider;
        }
        tempGridField[x][y] = (1.0 - evaporationRate) * 
                                (updateGridField[x][y] + diffusionRate * 
                                (average - updateGridField[x][y]));
      }
    }
    update_grid.setTo(temp_grid);
  } // end step()
  
}
