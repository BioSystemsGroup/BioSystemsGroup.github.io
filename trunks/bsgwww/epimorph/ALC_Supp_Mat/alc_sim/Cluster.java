package alc_sim;

import java.util.*;
import sim.engine.*;
import sim.util.*;
import sim.util.Properties;
import ec.util.*;
import sim.field.grid.*;

public class Cluster extends Agent {
  
  private Bag cells = new Bag();
  private Bag lumens = new Bag();
  private Bag outerCells = new Bag();
  private MersenneTwisterFast random_func = null;
  private ParameterDatabase paramDB = null;
  private int distanceMoved = 0;
  
  public int migration_mode = 0; // 1->diffusion; 2->cell-density; else->random
  public double random_move_prob = 0.0;
  public double sig_shift = 5.0;
  public double sig_scale = 2.0;
  public int density_check_radius = 10;
  public double diffusion_move_factor = 0.01;
  public double migration_speed = 0.0;
  
  /* ================== PARAMETERIZATION FUNCTIONS  ======================= */
  public int getMigrationMode() { return migration_mode; }
  public void setMigrationMode(int mode) { migration_mode = mode; }
  public double getClusterSigShift() { return sig_shift; }
  public void setClusterSigShift(double shift) { sig_shift = shift; }
  public double getClusterSigStretch() { return sig_scale; }
  public void setClusterSigStretch(double stretch) { if(stretch > 0.0) sig_scale = stretch; }
  public int getClusterDensityCheckRadius() { return density_check_radius; }
  public void setClusterDensityCheckRadius(int radius) { if(radius > 0) density_check_radius = radius; }
  public double getClusterMigrationSpeed() { return migration_speed; }
  public void setClusterMigrationSpeed(double speed) { if(speed >= 0.0) migration_speed = speed; }
  
  void loadParams() {
    if(paramDB==null)
      return;
    Properties p = Properties.getProperties(this,false,true,false);
    for(int i=0; i< p.numProperties(); i++) {
      if (p.isReadWrite(i) && !p.isComposite(i) && paramDB.exists(new Parameter(p.getName(i)))) {
        Parameter param = new Parameter(p.getName(i));
        String value = paramDB.getString(param,null);
        p.setValue(i,value);
      }
    }
  }
  /* ==================== END PARAMETERIZATION FUNCTIONS ================== */
  
  /* ==================== CONSTRUCTOR FUNCTIONS =========================== */
  public Cluster(ObjectGrid2D grid, SimState state) {
    super(0,0,grid);
    random_func = new MersenneTwisterFast(System.currentTimeMillis());
    state.schedule.scheduleOnce(this);
  }
  public Cluster(ObjectGrid2D grid, SimState state, long seed) {
    super(0,0,grid);
    random_func = new MersenneTwisterFast(seed);
    state.schedule.scheduleOnce(this);
  }
  public Cluster(ObjectGrid2D grid, SimState state, long seed, ParameterDatabase paramDB) {
    super(0,0,grid);
    random_func = new MersenneTwisterFast(seed);
    this.paramDB = paramDB;
    loadParams();
    state.schedule.scheduleOnce(this);
  }

  /* ==================== END CONSTRUCTOR FUNCTIONS ======================= */
  
  /* ==================== ACCESSOR/REMOVAL FUNCTIONS ====================== */
  
  public boolean contains(Agent agent) {
    return cells.contains(agent) || outerCells.contains(agent) || lumens.contains(agent);
  }
  
  public boolean containsCell(Cell cell) {
    return (cells.contains(cell) || outerCells.contains(cell));
  }
  public void addCell(Cell cell) {
    if (!cells.contains(cell)) {
      cells.add(cell);
    }
  }
  public void removeCell(Cell cell) {
    cells.remove(cell);
    outerCells.remove(cell);
  }
  public int getActiveCellNum() {
    return cells.numObjs;
  }
  public int getTotalCellNum() {
    return cells.numObjs + outerCells.numObjs;
  }
  public double getCellRatio() {
    if (cells.numObjs == 0) { return 0.0; }
    else { return ((double)outerCells.numObjs) / ((double)cells.numObjs); }
  }
  
  public void changeToOuterCell(Cell cell) {
    if (!outerCells.contains(cell)) {
      outerCells.add(cell);
    }
    if (cells.contains(cell)) {
      cells.remove(cell);
    }
  }
  
  public int getLumenSize() {
    return lumens.numObjs;
  }
  public boolean containsLumen(FreeSpace lumen) {
    return lumens.contains(lumen);
  }
  public void addLumen(FreeSpace lumen) {
    if (lumen == null) { return; }
    if (!lumens.contains(lumen)) {
      lumens.add(lumen);
    }
  }
  public void removeLumen(FreeSpace lumen) {
    if (lumen == null) { return; }
    lumens.removeMultiply(lumen);
    lumens.remove(lumen);
    Bag trashBag = new Bag();
    for (int i = 0; i < lumens.numObjs; i++) {
      FreeSpace curLumen = (FreeSpace) lumens.get(i);
      if (curLumen.x == lumen.x && curLumen.y == lumen.y) {
        trashBag.add(curLumen);
      }
    }
    for (int i = 0; i < trashBag.numObjs; i++) {
      lumens.remove(trashBag.get(i));
    }
  }
  
  public double getJumpVal() {
    double jumpval = 0.0;
    double perimDiameter = cells.numObjs / java.lang.Math.PI;
    double surfDiameter = java.lang.Math.sqrt((double)getTotalCellNum() / java.lang.Math.PI);
    if ((lumens.numObjs > 0 || cells.numObjs < 6) && perimDiameter > surfDiameter) {
      jumpval = 1.0;
    }
    return jumpval;
  }
  
  public int getCenterX() {
    int centerX = 0;
    int memberCounter = 0;
    int cumulativeX = 0;
    for (int i = 0; i < cells.numObjs; i++) {
      Cell curCell = (Cell) cells.get(i);
      cumulativeX += curCell.x;
      memberCounter++;
    }
    for (int i = 0; i < lumens.numObjs; i++) {
      FreeSpace curLumen = (FreeSpace) lumens.get(i);
      cumulativeX += curLumen.x;
      memberCounter++;
    }
    if (memberCounter > 0) {
      centerX = cumulativeX / memberCounter;
    }
    return centerX;
  }
  public int getCenterY() {
    int centerY = 0;
    int memberCounter = 0;
    int cumulativeY = 0;
    for (int i = 0; i < cells.numObjs; i++) {
      Cell curCell = (Cell) cells.get(i);
      cumulativeY += curCell.y;
      memberCounter++;
    }
    for (int i = 0; i < lumens.numObjs; i++) {
      FreeSpace curLumen = (FreeSpace) lumens.get(i);
      cumulativeY += curLumen.y;
      memberCounter++;
    }
    if (memberCounter > 0) {
      centerY = cumulativeY / memberCounter;
    }
    return centerY;
  }
  
  /* ==================== END ACCESSOR/REMOVAL FUNCTIONS ================== */
  
  /* ==================== MERGE FUNCTIONS ================================= */
  public void mergeWith(Cluster targetCluster) {
    if (this == targetCluster) {
      return;
    }
    Cell targetCell = null;
    for (int k = 0; k < targetCluster.cells.numObjs; k++) {
      targetCell = (Cell) targetCluster.cells.get(k);
      cells.add(targetCell);
      targetCell.setCluster(this);
    }
    targetCluster.cells.clear();
    for (int k = 0; k < targetCluster.outerCells.numObjs; k++) {
      targetCell = (Cell) targetCluster.outerCells.get(k);
      outerCells.add(targetCell);
      targetCell.setCluster(this);
    }
    targetCluster.outerCells.clear();
    for (int k = 0; k < targetCluster.lumens.numObjs; k++) {
      FreeSpace targetLumen = (FreeSpace) targetCluster.lumens.get(k);
      lumens.add(targetLumen);
    }
    targetCluster.lumens.clear();
  } // end mergeWith()
  /* ====================== END MERGE FUNCTIONS =========================== */
  
  /* ==================== STEP & SUPPORT FUNCTIONS ======================== */
  public void step(SimState state) {
    if (cells.numObjs > 0) {
      // clusterCheck();

      int moveDistance = (int) migration_speed;  // assumes migration_speed <= maximum int val
      double probDist = migration_speed - java.lang.Math.floor(migration_speed);
      if (random_func.nextDouble() < probDist) {
        moveDistance++;
      }
      double sig_threshold = 
              1.0 - 1.0 / (1.0 + java.lang.Math.exp(-(getTotalCellNum() - sig_shift) / sig_scale));
      double randnum = random_func.nextDouble();
      if (migration_mode == 1) {
        if (randnum < sig_threshold) { soluteBasedMove(true, moveDistance, state); }
        else { soluteBasedMove(false, moveDistance, state); }
      }
      else if (migration_mode == 2) {
        if (randnum < sig_threshold) { densityMove(density_check_radius, moveDistance, state); }
      }
      else {
        if (lumens.numObjs == 0 || randnum < sig_threshold) { randomMove(moveDistance); }
      }
      /*
      if (randnum < sig_threshold) {
        boolean randomMove = random_func.nextBoolean(random_move_prob);
        if (!randomMove && migration_mode == 1) {
          soluteBasedMove(state);
        }
        else if (!randomMove && migration_mode == 2) {
          densityMove(density_check_radius, state);
        }
        else {
          glideMove(-1); // -1 = random
        }
      }
       */
      state.schedule.scheduleOnce(this);
    }
  } // end step()
  
  // check whether this cluster has orphan cells
  void clusterCheck() {
    boolean connectStatus = true;
    for (int i = 0; i < cells.numObjs; i++) {
      Cell curCell = (Cell) cells.get(i);
      if (!curCell.alive()) {
        continue;
      }
      boolean neighborStatus = false;
      for (int j = 0; j < cells.numObjs; j++) {
        Cell compCell = (Cell) cells.get(j);
        if (curCell != compCell && curCell.areNeighbors(curCell, compCell)) {
          neighborStatus = true;
          break;
        }
      }
      for (int j = 0; j < lumens.numObjs; j++) {
        FreeSpace curLumen = (FreeSpace) lumens.get(j);
        if (curCell.areNeighbors(curCell, curLumen)) {
          neighborStatus = true;
          break;
        }
      }
      if (!neighborStatus) {
        connectStatus = false;
        break;
      }
    }
    if (!connectStatus) {
      System.out.println("Cluster is not connected: " + cells.numObjs);
    }
  }
  
  // checks whether any cell has moved within the cluster
  boolean changedShape() {
    boolean result = false;
    for (int i = 0; i < cells.numObjs; i++) {
      Cell curCell = (Cell) cells.get(i);
      if (!curCell.alive()) {
        continue;
      }
      if (curCell.moved()) {
        result = true;
        break;
      }
    }
    return result;
  }
  
  void randomMove(int moveDistance) {
    int moveDirection = random_func.nextInt(6);
    for (int dist = 0; dist < moveDistance; dist++) {
      if (!glideMove(moveDirection)) {
        break;
      }
    }
  }
  
  // move in a direction dependent on surrounding density
  void densityMove(int radius, int moveDistance, SimState state) {
    final Culture parentCulture = (Culture)state;
    final Bag neighbors = parentCulture.neighborBag;
    final IntBag xPositions = parentCulture.neighborX;
    final IntBag yPositions = parentCulture.neighborY;

    int centerX = this.getCenterX();
    int centerY = this.getCenterY();
    double[] surroundingDensities = new double[6]; // 0=UP, 1=UR, 2=DR, 3=DN, 4=DL, 5=UL
    for (int i = 0; i < 6; i++) {
      surroundingDensities[i] = 0.0;
    }
    
    this.grid.getNeighborsHexagonalDistance
            (centerX, centerY, radius, true, neighbors, xPositions, yPositions);
    
    /*
    for (int i = 0; i < cells.numObjs; i++) {
      Cell curCell = (Cell) cells.get(i);
      Bag curBag = new Bag();
      xPositions.clear();
      yPositions.clear();
      this.grid.getNeighborsHexagonalDistance
              (curCell.x, curCell.y, radius, true, curBag, xPositions, yPositions);
      for (int j = 0; j < curBag.numObjs; j++) {
        Agent curAgent = (Agent) curBag.get(j);
        if (curAgent instanceof Cell && !neighborsToroidal.contains(curAgent)) {
          neighborsToroidal.add(curAgent);
        }
      }
    }
    */
    
    // determine cell densities of the surrounding regions
    for (int i = 0; i < neighbors.numObjs; i++) {
      Agent curAgent = (Agent) neighbors.get(i);
      if (curAgent instanceof Cell && !this.contains(curAgent)) {
        // determine which neighboring region it belongs to
        int curX = curAgent.x;
        int curY = curAgent.y;
        
        if (curX > centerX + radius) {
          curX = curX - grid.getWidth();
        }
        if (curX < centerX - radius) {
          curX = curX + grid.getWidth();
        }
        if (curY > centerY + radius) {
          curY = curY - grid.getHeight();
        }
        if (curY < centerY - radius) {
          curY = curY + grid.getHeight();
        }
        
        int xdif = curX - centerX; // negative if curAgent is located left
        int xdifAbs = java.lang.Math.abs(xdif);
        int upUrY = centerY;
        int urDrY = centerY;
        int drDnY = centerY;
        if (centerX%2 == 1) {
          upUrY = centerY - (xdifAbs + xdifAbs / 2);
          drDnY = centerY + (xdifAbs + (xdifAbs + 1) / 2);
        }
        else {
          upUrY = centerY - (xdifAbs + (xdifAbs + 1) / 2);
          drDnY = centerY + (xdifAbs + xdifAbs / 2);
        }
    
        if (curY < upUrY) {
          surroundingDensities[0] += 1.0; // UP
        }
        else if (curY == upUrY) {
          if (xdif > 0) {
            surroundingDensities[0] += 0.5; // 1/2 UP
            surroundingDensities[1] += 0.5; // 1/2 UR
          }
          else {
            surroundingDensities[0] += 0.5; // 1/2 UP
            surroundingDensities[5] += 0.5; // 1/2 UL
          }
        }
        else if (curY > upUrY && curY < urDrY) {
          if (xdif > 0) {
            surroundingDensities[1] += 1.0; // UR
          }
          else {
            surroundingDensities[5] += 1.0; // UL
          }
        }
        else if (curY == urDrY) {
          if (xdif > 0) {
            surroundingDensities[1] += 0.5; // 1/2 UR
            surroundingDensities[2] += 0.5; // 1/2 DR
          }
          else {
            surroundingDensities[4] += 0.5; // 1/2 DL
            surroundingDensities[5] += 0.5; // 1/2 UL
          }
        }
        else if (curY > urDrY && curY < drDnY) {
          if (xdif > 0) {
            surroundingDensities[2] += 1.0; // DR
          }
          else {
            surroundingDensities[4] += 1.0; // DL
          }
        }
        else if (curY == drDnY) {
          if (xdif > 0) {
            surroundingDensities[2] += 0.5; // 1/2 DR
            surroundingDensities[3] += 0.5; // 1/2 DN
          }
          else {
            surroundingDensities[3] += 0.5; // 1/2 DN
            surroundingDensities[4] += 0.5; // 1/2 DL
          }
        }
        else if (curY > drDnY) {
          surroundingDensities[3] += 1.0; // DN
        }
        else {
          System.out.println("Cell::densityMove() has invalid curY comparison");
        }
      }
    }
    // identify the most desired regions
    IntBag targetDirections = new IntBag();
    double maxDensity = 0.0;
    for (int i = 0; i < 6; i++) {
      if (surroundingDensities[i] >= maxDensity) {
        maxDensity = surroundingDensities[i];
      }
    }
    if (maxDensity == 0.0) {
      return;
    }
    for (int i = 0; i < 6; i++) {
      if (surroundingDensities[i] == maxDensity) {
        targetDirections.add(i);
      }
    }
    // now move to the most desired region (randomly selected if more than one)
    int direction = targetDirections.get(random_func.nextInt(targetDirections.numObjs));
    for (int dist = 0; dist < moveDistance; dist++) {
      if (!glideMove(direction)) {
        break;
      }
    }
  } // end densityMove()
  
  /* *** CALL ONLY IN SOLUTE DIFFUSION-BASED MIGRATION MODE *** */
  void soluteBasedMove(boolean migrate, int moveDistance, SimState state) {
    if (migration_mode != 1) {
      System.out.println("Cluster::soluteBasedMove() called in non-diffusion mode");
      return;
    }
    final Culture parentCulture = (Culture)state;
    final DoubleBag neighborsDouble = parentCulture.neighborDoubleBag;
    final IntBag xCoord = parentCulture.neighborX;
    final IntBag yCoord = parentCulture.neighborY;
    final DoubleGrid2D diffusionGrid = parentCulture.diffusionGrid;
    final DoubleGrid2D g = parentCulture.diffusionGrid; // shorter abbrev for stx and sty toroidal operations
    
    // evaluate surrounding solute concentrations in all six hexagonal directions
    double[] totalSoluteConc = new double[6]; // UP = 0; UR = 1; DR = 2; DN = 3; DL = 4; UL = 5;
    double[] adjustedSoluteConc = new double[6];
    for (int i = 0; i < 6; i++) {
      totalSoluteConc[i] = 0.0;
      adjustedSoluteConc[i] = 0.0;
    }
    int curX, curY;
    for (int i = 0; i < cells.numObjs; i++) {
      Cell curCell = (Cell) cells.get(i);
      curX = curCell.x;
      curY = curCell.y;
      totalSoluteConc[0] += diffusionGrid.get(g.stx(g.upx(curX, curY)), g.sty(g.upy(curX, curY)));
      totalSoluteConc[1] += diffusionGrid.get(g.stx(g.urx(curX, curY)), g.sty(g.ury(curX, curY)));
      totalSoluteConc[2] += diffusionGrid.get(g.stx(g.drx(curX, curY)), g.sty(g.dry(curX, curY)));
      totalSoluteConc[3] += diffusionGrid.get(g.stx(g.downx(curX, curY)), g.sty(g.downy(curX, curY)));
      totalSoluteConc[4] += diffusionGrid.get(g.stx(g.dlx(curX, curY)), g.sty(g.dly(curX, curY)));
      totalSoluteConc[5] += diffusionGrid.get(g.stx(g.ulx(curX, curY)), g.sty(g.uly(curX, curY)));
    }
    int curIndex;
    double adjustedVal;
    for (int i = 0; i < 6; i++) {
      adjustedVal = totalSoluteConc[i];
      adjustedSoluteConc[i] += adjustedVal * 0.5;
      curIndex = i+1;
      if (curIndex > 5) { curIndex -= 6; }
      adjustedSoluteConc[curIndex] += adjustedVal * 0.25;
      curIndex = i+2;
      if (curIndex > 5) { curIndex -= 6; }
      adjustedSoluteConc[curIndex] -= adjustedVal * 0.25;
      curIndex = i+3;
      if (curIndex > 5) { curIndex -= 6; }
      adjustedSoluteConc[curIndex] -= adjustedVal * 0.5;
      curIndex = i+4;
      if (curIndex > 5) { curIndex -= 6; }
      adjustedSoluteConc[curIndex] -= adjustedVal * 0.25;
      curIndex = i+5;
      if (curIndex > 5) { curIndex -= 6; }
      adjustedSoluteConc[curIndex] += adjustedVal * 0.25;
    }
    // determine the direction with the highest solute concentration
    IntBag bestDirections = new IntBag();
    double highestConc = adjustedSoluteConc[0];
    double curConc = 0.0;
    for (int i = 0; i < 6; i++) {
      curConc = adjustedSoluteConc[i];
      if (curConc >= highestConc) {
        highestConc = curConc;
      }
    }
    for (int i = 0; i < 6; i++) {
      curConc = adjustedSoluteConc[i];
      if (curConc >= highestConc) {
        bestDirections.add(i);
      }
    }
    if (migrate || highestConc < diffusion_move_factor * (((Culture)state).getMaxSoluteConcentration())) {
      int moveDirection = random_func.nextInt(6);
      if (bestDirections.numObjs > 0) {
        moveDirection = bestDirections.get(random_func.nextInt(bestDirections.numObjs));
      }
      for (int dist = 0; dist < moveDistance; dist++) {
        if (!glideMove(moveDirection)) {
          break;
        }
      }
    }
    /*
    if (bestDirections.numObjs > 0 && bestDirections.numObjs < 6) {
      glideMove(bestDirections.get(random_func.nextInt(bestDirections.numObjs)));
    }
     */
  } // end soluteBasedMove
  
  // move this cluster in its entirety (cells + lumens)
  boolean glideMove(int direction) {
    int moveDirection = random_func.nextInt(6);
    if (direction >= 0 && direction <= 5) {
      moveDirection = direction;
    }
    boolean canMove = true;
    
    // fill (replace) current lumen locations with matrix
    for (int i = 0; i < lumens.numObjs; i++) {
      FreeSpace curLumen = (FreeSpace) lumens.get(i);
      grid.set(curLumen.x, curLumen.y, (new Matrix(curLumen.x, curLumen.y, grid)));
    }
    // fill (replace) current cell locations with matrix
    for (int i = 0; i < cells.numObjs; i++) {
      Cell curCell = (Cell) cells.get(i);
      grid.set(curCell.x, curCell.y, (new Matrix(curCell.x, curCell.y, grid)));
    }
    // test whether the clustered cells can move without bumping into other cells nearby
    for (int i = 0; i < cells.numObjs; i++) {
      Cell curCell = (Cell) cells.get(i);
      Agent targetLoc = null;
      switch (moveDirection) {
        case 0: 
          targetLoc = (Agent)grid.get(grid.stx(grid.upx(curCell.x, curCell.y)),
                                      grid.sty(grid.upy(curCell.x, curCell.y)));
          break;
        case 1:
          targetLoc = (Agent)grid.get(grid.stx(grid.urx(curCell.x, curCell.y)),
                                      grid.sty(grid.ury(curCell.x, curCell.y)));
          break;
        case 2:
          targetLoc = (Agent)grid.get(grid.stx(grid.drx(curCell.x, curCell.y)),
                                      grid.sty(grid.dry(curCell.x, curCell.y)));
          break;
        case 3:
          targetLoc = (Agent)grid.get(grid.stx(grid.downx(curCell.x, curCell.y)),
                                      grid.sty(grid.downy(curCell.x, curCell.y)));
          break;
        case 4:
          targetLoc = (Agent)grid.get(grid.stx(grid.dlx(curCell.x, curCell.y)),
                                      grid.sty(grid.dly(curCell.x, curCell.y)));
          break;
        case 5:
          targetLoc = (Agent)grid.get(grid.stx(grid.ulx(curCell.x, curCell.y)),
                                      grid.sty(grid.uly(curCell.x, curCell.y)));
          break;
        default:
          break;
      }
      if (targetLoc == null || targetLoc instanceof Cell) {
        canMove = false;
        break;
      }
    }
    
    // if all the cells can move, then go ahead and move the cells + lumens
    if (canMove) {
      for (int i = 0; i < cells.numObjs; i++) {
        Cell curCell = (Cell) cells.get(i);
        curCell.addDistanceMovedTotal(1);
        Agent targetLoc = null;
        switch (moveDirection) {
          case 0: 
            targetLoc = (Agent)grid.get(grid.stx(grid.upx(curCell.x, curCell.y)),
                                        grid.sty(grid.upy(curCell.x, curCell.y)));
            break;
          case 1:
            targetLoc = (Agent)grid.get(grid.stx(grid.urx(curCell.x, curCell.y)),
                                        grid.sty(grid.ury(curCell.x, curCell.y)));
            break;
          case 2:
            targetLoc = (Agent)grid.get(grid.stx(grid.drx(curCell.x, curCell.y)),
                                        grid.sty(grid.dry(curCell.x, curCell.y)));
            break;
          case 3:
            targetLoc = (Agent)grid.get(grid.stx(grid.downx(curCell.x, curCell.y)),
                                        grid.sty(grid.downy(curCell.x, curCell.y)));
            break;
          case 4:
            targetLoc = (Agent)grid.get(grid.stx(grid.dlx(curCell.x, curCell.y)),
                                        grid.sty(grid.dly(curCell.x, curCell.y)));
            break;
          case 5:
            targetLoc = (Agent)grid.get(grid.stx(grid.ulx(curCell.x, curCell.y)),
                                        grid.sty(grid.uly(curCell.x, curCell.y)));
            break;
          default:
            break;
        }
        if (targetLoc == null) {
          System.out.println("Cluster:Move targetLoc is NULL");
          return false;
        }
        if (targetLoc instanceof Cell) {
          System.out.println("Cluster:Move overwriting CELL");
        }
        int newX = targetLoc.x;
        int newY = targetLoc.y;
        grid.set(newX, newY, curCell);
        curCell.setCoordinates(newX, newY);
      }
      for (int i = 0; i < lumens.numObjs; i++) {
        FreeSpace curLumen = (FreeSpace) lumens.get(i);
        Agent targetLoc = null;
        switch (moveDirection) {
          case 0: 
            targetLoc = (Agent)grid.get(grid.stx(grid.upx(curLumen.x, curLumen.y)),
                                        grid.sty(grid.upy(curLumen.x, curLumen.y)));
            break;
          case 1:
            targetLoc = (Agent)grid.get(grid.stx(grid.urx(curLumen.x, curLumen.y)),
                                        grid.sty(grid.ury(curLumen.x, curLumen.y)));
            break;
          case 2:
            targetLoc = (Agent)grid.get(grid.stx(grid.drx(curLumen.x, curLumen.y)),
                                        grid.sty(grid.dry(curLumen.x, curLumen.y)));
            break;
          case 3:
            targetLoc = (Agent)grid.get(grid.stx(grid.downx(curLumen.x, curLumen.y)),
                                        grid.sty(grid.downy(curLumen.x, curLumen.y)));
            break;
          case 4:
            targetLoc = (Agent)grid.get(grid.stx(grid.dlx(curLumen.x, curLumen.y)),
                                        grid.sty(grid.dly(curLumen.x, curLumen.y)));
            break;
          case 5:
            targetLoc = (Agent)grid.get(grid.stx(grid.ulx(curLumen.x, curLumen.y)),
                                        grid.sty(grid.uly(curLumen.x, curLumen.y)));
            break;
          default:
            break;
        }
        int newX = targetLoc.x;
        int newY = targetLoc.y;
        grid.set(newX, newY, curLumen);
        curLumen.setCoordinates(newX, newY);
      }
    }
    // if cannot be moved then reset the cells + lumens to their original locations
    else {
      for (int i = 0; i < cells.numObjs; i++) {
        Cell curCell = (Cell) cells.get(i);
        grid.set(curCell.x, curCell.y, curCell);
      }
      for (int i = 0; i < lumens.numObjs; i++) {
        FreeSpace curLumen = (FreeSpace) lumens.get(i);
        grid.set(curLumen.x, curLumen.y, curLumen);
      }
    }
    return canMove;
  } // end glideMove()
  
  /* ==================== END STEP & SUPPORT FUNCTIONS ==================== */
 
  /* ==================== AUXILLARY FUNCTIONS ============================= */
  
  public double diameter() {
    double diameter = 1.0;
    if (cells.numObjs < 2) {
      return diameter;
    }
    for (int i = 0; i < cells.numObjs; i++) {
      Cell curCell = (Cell) cells.get(i);
      for (int k = 0; k < cells.numObjs; k++) {
        if (i == k) { continue; }
        Cell compCell = (Cell) cells.get(k);
        double xDif = curCell.x - compCell.x;
        if (xDif > cells.numObjs) {
          xDif = grid.getWidth() - java.lang.Math.abs(curCell.x - compCell.x);
        }
        double yDif = curCell.y - compCell.y;
        if (yDif > cells.numObjs) {
          yDif = grid.getHeight() - java.lang.Math.abs(curCell.y - compCell.y);
        }
        double euclidian = xDif * xDif + yDif * yDif;
        double compDistance = java.lang.Math.sqrt(euclidian);
        if (compDistance > diameter) {
          diameter = compDistance;
        }
      }
    }
    return diameter;
  }
  
  /* ==================== END AUXILLARY FUNCTIONS ========================= */
  
} // end class cluster
