package alc_sim;

import sim.engine.*;
import sim.util.*;
import sim.field.grid.*;

public class Diffuser implements Steppable {
  
  DoubleGrid2D update_grid;
  DoubleGrid2D temp_grid;
  double evaporation_rate;
  double diffusion_rate;
  int counter = 0;
  
  /** Creates a new instance of Diffuser */
  public Diffuser( final DoubleGrid2D updateGrid, final DoubleGrid2D tempGrid,
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
    final int gridHeight = v.getHeight();
    final double evaporationRate = evaporation_rate;
    final double diffusionRate = diffusion_rate;
    
    double average;
    for (int x = 0; x < gridWidth; x++) {
      for (int y = 0; y < gridHeight; y++) {
        // hexagonal representation
        average = (updateGridField[x][y] + 
                   updateGridField[v.stx(v.ulx(x,y))][v.sty(v.uly(x,y))] +
                   updateGridField[v.stx(v.urx(x,y))][v.sty(v.ury(x,y))] + 
                   updateGridField[v.stx(v.dlx(x,y))][v.sty(v.dly(x,y))] + 
                   updateGridField[v.stx(v.drx(x,y))][v.sty(v.dry(x,y))] + 
                   updateGridField[v.stx(v.upx(x,y))][v.sty(v.upy(x,y))] + 
                   updateGridField[v.stx(v.downx(x,y))][v.sty(v.downy(x,y))]) / 7.0;  
        tempGridField[x][y] = (1.0 - evaporationRate) * 
                               (updateGridField[x][y] + diffusionRate * 
                               (average - updateGridField[x][y]));
      }
    }
    update_grid.setTo(temp_grid);
  } // end step()
  
}
