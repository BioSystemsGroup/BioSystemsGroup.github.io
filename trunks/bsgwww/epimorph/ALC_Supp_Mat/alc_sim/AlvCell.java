package alc_sim;

import java.io.*;
import java.util.*;
import sim.engine.*;
import sim.util.*;
import sim.util.Properties;
import ec.util.*;
import sim.field.grid.*;

public class AlvCell extends Cell {
  
  public static final int MAX_PUSH = 10;
  public static final int MAX_PULL = 5;
  
  public static final boolean repair_sim = true;

  private boolean alive = true; // currently used as a mark for outerCell
  private int age = 0; // cell age in number of simulation steps
  private int distanceMovedTotal = 0; // cumulative distance of migration
  private int distanceMovedSingle = 0; // distance of migration as single cell
  private boolean attached = false; // true if part of a cluster
  private boolean pushToggle = false; // used by pushIterative() to prevent cyclic calls
  private boolean pullToggle = false; // used by pullIterative() to prevent cyclic calls
  private Bag ruleUsageRecords = new Bag();
  private Bag solutes = null; // a collection of solutes inside the cell
  private MersenneTwisterFast random_func = null;
  private ParameterDatabase paramDB = null;
  public boolean selfTypeRecog = false;
  
  public int migration_mode = 2; // 1->diffusion; 2->cell-density; else->random
  public double random_move_prob = 0.0;
  public double adhesion_prob_unatt = .2; // adhesion probability when unattached and single
  public double adhesion_prob_att = .01; // adhesion probability when attached to another cell
  public int density_check_radius = 5;
  public double sig_shift = 12.0;
  public double sig_stretch = 2.0;
  public double solute_output = 0.0; // level of solute secretion
  public double max_solute_concentration = 0.0; // maximum level of solute concentration
  public double migration_speed = 1.0;

  /* ===================== PARAMETERIZATION FUNCTIONS ===================== */
  public int getMigrationMode() { return migration_mode; }
  public void setMigrationMode(int mode) { migration_mode = mode; }
  public double getCellAdhesionUnattached() { return adhesion_prob_unatt; }
  public void setCellAdhesionUnattached (double prob) { if(prob >= 0.0) adhesion_prob_unatt = prob; }
  public double getCellAdhesionAttached() { return adhesion_prob_att; }
  public void setCellAdhesionAttached (double prob) { if(prob >= 0.0) adhesion_prob_att = prob; }
  public int getCellDensityCheckRadius() { return density_check_radius; }
  public void setCellDensityCheckRadius(int radius) { if(radius > 0) density_check_radius = radius; }
  public double getCellSigShift() { return sig_shift; }
  public void setCellSigShift(double shift) { sig_shift = shift; }
  public double getCellSigStretch() { return sig_stretch; }
  public void setCellSigStretch(double stretch) { if(stretch > 0.0) sig_stretch = stretch; }
  public double getCellSoluteOutput() { return solute_output; }
  public void setCellSoluteOutput(double output) { if(output >= 0.0) solute_output = output; }
  public double getCellMaxSoluteConcentration() { return max_solute_concentration; }
  public void setCellMaxSoluteConcentration(double max) { if(max >= 0.0) max_solute_concentration = max; }
  public double getCellMigrationSpeed() { return migration_speed; }
  public void setCellMigrationSpeed(double speed) { if(speed >= 0.0) migration_speed = speed; }
  
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
  
  /* ==================== STARTUP & ACCESS FUNCTIONS ====================== */
  
  public AlvCell(int x, int y, ObjectGrid2D grid) {
    super(x, y, grid);
    random_func = new MersenneTwisterFast(System.currentTimeMillis());
    solutes = new Bag();
  } 
  public AlvCell(int x, int y, ObjectGrid2D grid, int seed) {
    super(x, y, grid);
    random_func = new MersenneTwisterFast(seed);
    solutes = new Bag();
  }
  public AlvCell(int x, int y, ObjectGrid2D grid, ParameterDatabase params) {
    super(x, y, grid);
    random_func = new MersenneTwisterFast(System.currentTimeMillis());
    solutes = new Bag();
    this.paramDB = params;
    loadParams();
  }
  public AlvCell(int x, int y, ObjectGrid2D grid, int seed, ParameterDatabase params) {
    super(x, y, grid);
    random_func = new MersenneTwisterFast(seed);
    solutes = new Bag();
    this.paramDB = params;
    loadParams();
  }
  
  public boolean alive() {
    return alive;
  }
  
  public void setAttached (boolean value) {
    attached = value;
  }
  public boolean isAttached() {
    return attached;
  }
  
  public void resetPulled() {
    this.pullToggle = false;
  }
  public void setPulled(boolean bool) {
    this.pullToggle = bool;
  }
  
  public boolean moved() {
    return this.moved;
  }
  
  public void setSoluteProperties(double output, double max) {
    solute_output = output;
    max_solute_concentration = max;
  }
  
  public Bag getRuleUsageRecords() { return ruleUsageRecords; }
  
  public int getDistanceMovedSingle() { return distanceMovedSingle; }
  public int getDistanceMovedTotal() { return distanceMovedTotal; }
  public void addDistanceMovedTotal(int distance) { distanceMovedTotal += distance; }
  
  /* ====================== END STARTUP & ACCESS FUNCTIONS ================ */

  /* ====================== STEP & SUPPORT FUNCTIONS ====================== */
  
  public void step(SimState state) {
    if (alive) {
      this.moved = false; // resets move status to false (has not moved in current step)
      Culture parentCulture = (Culture) state;
      final Bag neighbors = parentCulture.neighborBag;
      final IntBag xCoord = parentCulture.neighborX;
      final IntBag yCoord = parentCulture.neighborY;
      Bag cellNeighbors = new Bag();
      Bag matrixNeighbors = new Bag();
      Bag lumenNeighbors = new Bag();
      Bag nonCellNeighbors = new Bag();
    
      // assess own environment
      this.grid.getNeighborsHexagonalDistance(this.x, this.y, 1, true, neighbors, xCoord, yCoord);
      neighbors.remove(this);
      for (int i = 0; i < neighbors.numObjs; i++) {
        Agent agent = (Agent) neighbors.get(i);
        if (agent instanceof AlvCell) {
          cellNeighbors.add(agent);
        }
        if (agent instanceof Matrix) {
          matrixNeighbors.add(agent);
          nonCellNeighbors.add(agent);
        }
        if (agent instanceof FreeSpace) {
          lumenNeighbors.add(agent);
          nonCellNeighbors.add(agent);
        }
      }
      // done with assessing own environment
      
      // secrete solutes
      if (migration_mode == 1) {
        secreteSolutes(((Culture)state).diffusionGrid,this.x,this.y, max_solute_concentration);
      }
      // merge with nearby cells or clusters
      if (cellNeighbors.numObjs > 0) {
        this.updateRuleUsage(0);
        double randnum = random_func.nextDouble();
        double threshold = adhesion_prob_unatt; // unattached
        if (this.attached && lumenNeighbors.numObjs > 0) {
          threshold = adhesion_prob_att; // attached & has luminal surface
        }
        if (randnum < threshold) {
          cellNeighbors.shuffle(random_func);
          AlvCell cell = (AlvCell) cellNeighbors.get(0);
          this.mergeWith(cell, state);
          this.updateRuleUsage(1);
        }
      }
      // unattached status
      if (!attached) {
        this.updateRuleUsage(2);
        if (migration_speed > 0.0) {
          boolean randomMove = random_func.nextBoolean(random_move_prob);
          int moveDistance = (int) migration_speed;  // assumes migration_speed <= maximum int val
          double probDist = migration_speed - java.lang.Math.floor(migration_speed);
          if (random_func.nextDouble() < probDist) {
            moveDistance++;
          }
          if (!randomMove && migration_mode == 1 && moveDistance > 0) {
            soluteGradientBasedMove(moveDistance, state);
          }
          else if (!randomMove && migration_mode == 2 && moveDistance > 0) {
            densityMove(density_check_radius, moveDistance, state);
          }
          else if (moveDistance > 0) {
            randomMove(moveDistance);
          }
          distanceMovedSingle += moveDistance;
          distanceMovedTotal += moveDistance;
        }
      }
      // attached to other cells in a cluster
      else {
        boolean connected = false;
        if (lumenNeighbors.numObjs == 6) {
          connected = true;
        }
        else {
          for (int i = 0; i < cellNeighbors.numObjs; i++) {
            AlvCell curCell = (AlvCell) cellNeighbors.get(i);
            if (this.cluster.contains(curCell)) {
              connected = true;
              break;
            }
          }
        }
        if (connected) {
         this.rearrangeClustered(state);
         this.updateRuleUsage(4);
        }
        else {
          this.cluster.removeCell(this);
          this.attached = false;
          this.updateRuleUsage(3);
        }
      }
      // reschedule itself
      ((Culture)state).schedule.scheduleOnce(this);
      this.age++;
    } // if (alive)
  } // end step()

  // merge with another cell/cluster
  void mergeWith(AlvCell cell, SimState state) {
    // both are unattached cells
    if (!this.attached && !cell.attached) {
      Cluster newCluster = new Cluster(grid, state, random_func.nextInt(), paramDB);
      newCluster.setMigrationMode(migration_mode);
      newCluster.addCell(this);
      newCluster.addCell(cell);
      this.cluster = newCluster;
      this.attached = true;
      cell.cluster = newCluster;
      cell.attached = true;
      ((Culture)state).schedule.scheduleOnce(newCluster);
    }
    // self is unattached but the other cell is part of a cluster
    else if (!this.attached && cell.attached) {
      cell.cluster.addCell(this);
      this.cluster = cell.cluster;
      this.attached = true;
    }
    // self is part of a cluster but the other cell is unattached
    else if (this.attached && !cell.attached) {
      this.cluster.addCell(cell);
      cell.cluster = this.cluster;
      cell.attached = true;
    }
    // both are part of (same or different) cluster
    else {
      if (!this.cluster.containsCell(cell) || !cell.cluster.containsCell(this)) {
        this.cluster.mergeWith(cell.cluster);
      }
    }
  } // end mergeWith()
  
  void randomMove(int moveDistance) {
    int moveDirection = random_func.nextInt(7); // assumes hexagonal grid
    if (moveDirection > 5) {
      // stay in place
      return;
    }
    for (int dist = 0; dist < moveDistance; dist++) {
      Agent targetAgent = getAgentAt(this, moveDirection);
      if (targetAgent instanceof Matrix || targetAgent instanceof FreeSpace) {
        int newX = targetAgent.x;
        int newY = targetAgent.y;
        grid.set(this.x, this.y, targetAgent);
        targetAgent.setCoordinates(this.x, this.y);
        this.moveTo(newX, newY);
      }
      else {
        break;
      }
    }
  } // end randomMove()
  
  // density dependent move
  void densityMove(int radius, int moveDistance, SimState state) {
    Culture parentCulture = (Culture)state;
    Bag neighbors = parentCulture.neighborBag;
    IntBag xPositions = parentCulture.neighborX;
    IntBag yPositions = parentCulture.neighborY;
    
    double[] surroundingDensities = new double[6]; // 0=UP, 1=UR, 2=DR, 3=DN, 4=DL, 5=UL
    for (int i = 0; i < 6; i++) {
      surroundingDensities[i] = 0.0;
    }

    this.grid.getNeighborsHexagonalDistance
            (this.x, this.y, radius, true, neighbors, xPositions, yPositions);
    neighbors.remove(this);
    
    // determine cell densities of the surrounding regions
    for (int i = 0; i < neighbors.numObjs; i++) {
      Agent curAgent = (Agent) neighbors.get(i);
      if (curAgent instanceof AlvCell) {
        // determine which neighboring region it belongs to
        int curX = curAgent.x;
        int curY = curAgent.y;
        
        if (curX > this.x + radius) {
          curX = curX - grid.getWidth();
        }
        if (curX < this.x - radius) {
          curX = curX + grid.getWidth();
        }
        if (curY > this.y + radius) {
          curY = curY - grid.getHeight();
        }
        if (curY < this.y - radius) {
          curY = curY + grid.getHeight();
        }
        
        int xdif = curX - this.x; // negative if curAgent is located left
        int xdifAbs = java.lang.Math.abs(xdif);
        int upUrY = this.y;
        int urDrY = this.y;
        int drDnY = this.y;
        if (this.x%2 == 1) {
          upUrY = this.y - (xdifAbs + xdifAbs / 2);
          drDnY = this.y + (xdifAbs + (xdifAbs + 1) / 2);
        }
        else {
          upUrY = this.y - (xdifAbs + (xdifAbs + 1) / 2);
          drDnY = this.y + (xdifAbs + xdifAbs / 2);
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
    // identify the most cell-dense regions
    IntBag targetDirections = new IntBag();
    double maxDensity = 0.0;
    for (int i = 0; i < 6; i++) {
      if (surroundingDensities[i] >= maxDensity) {
        maxDensity = surroundingDensities[i];
      }
    }
    for (int i = 0; i < 6; i++) {
      if (surroundingDensities[i] == maxDensity) {
        targetDirections.add(i);
      }
    }
    // now move to the most cell-dense region (randomly selected if more than one)
    int direction = targetDirections.get(random_func.nextInt(targetDirections.numObjs));
    for (int dist = 0; dist < moveDistance; dist++) {
      Agent targetAgent = getAgentAt(this, direction);
      if (targetAgent instanceof Matrix || targetAgent instanceof FreeSpace) {
        int newX = targetAgent.x;
        int newY = targetAgent.y;
        // swap coordinates
        grid.set(this.x, this.y, targetAgent);
        targetAgent.setCoordinates(this.x, this.y);
        this.moveTo(newX, newY);
      }
      else {
        break;
      }
    }
  } // end densityMove()
  
  /* *** CALL ONLY IN SOLUTE DIFFUSION-BASED MIGRATION MODE *** */
  void soluteGradientBasedMove(int moveDistance, SimState state) {
    if (migration_mode != 1) {
      System.out.println("Cell::soluteGradientBasedMove() called in non-diffusion mode");
      return;
    }
    Culture parentCulture = (Culture)state;
    final DoubleBag neighborsDouble = parentCulture.neighborDoubleBag;
    final IntBag xCoord = parentCulture.neighborX;
    final IntBag yCoord = parentCulture.neighborY;
    
    Bag bestLocs = new Bag();
    double highestConc = 0.0;
    double curConc = 0.0;
    int curX, curY;
    ((Culture)state).diffusionGrid.getNeighborsHexagonalDistance(this.x, this.y, 1, true, neighborsDouble, xCoord, yCoord);
    for( int i = 0 ; i < neighborsDouble.numObjs ; i++ ) {
      curX = xCoord.objs[i];
      curY = yCoord.objs[i];
      curConc = neighborsDouble.objs[i];
      if (curConc > highestConc) {
        highestConc = curConc;
      }
      /*
      if( curX != this.x || curY != this.y ) {
        if ( curConc > 0.0 && curConc >= highestConc) {
          System.out.println(curConc);
          highestConc = curConc;
          bestLocs.add(new Int2D(curX, curY));
        }
      }
       */
    }
    for (int i = 0; i < neighborsDouble.numObjs; i++) {
      curX = xCoord.objs[i];
      curY = yCoord.objs[i];
      curConc = neighborsDouble.objs[i];
      if (curConc >= highestConc) {
        bestLocs.add(new Int2D(curX, curY));
      }
    }
    // move only if gradients exist
    if (bestLocs.numObjs > 0 && bestLocs.numObjs < 6) {
      Int2D bestCoord = (Int2D) bestLocs.get(random_func.nextInt(bestLocs.numObjs));
      Agent targetDir = (Agent) grid.get(bestCoord.x, bestCoord.y);
      int moveDirection = this.determineMoveDirection(this, targetDir);
      for (int dist = 0; dist < moveDistance; dist++) {
        Agent targetAgent = getAgentAt(this, moveDirection);
        if (targetAgent instanceof Matrix || targetAgent instanceof FreeSpace) {
          int newX = targetAgent.x;
          int newY = targetAgent.y;
          // swap coordinates
          grid.set(this.x, this.y, targetAgent);
          targetAgent.setCoordinates(this.x, this.y);
          this.moveTo(newX, newY);
        }
        else {
          break;
        }
      }
    }
  } // end soluteGradientBasedMove()
  
  void initializeSolutes() {
    if (solutes == null) {
      solutes = new Bag();
    }
    Solute solute01 = new Solute(x,y,grid,0,0.0);
    solutes.add(solute01);
  } // end initializeSolutes()
  
  void updateSolutes() {
    Solute solute01 = (Solute) solutes.get(0);
    double changeVal = 1.0;
    solute01.addAmount(changeVal);
  } // end updateSolutes()
  
  void transport() {
    Bag neighbors = new Bag();
    IntBag xCoord = new IntBag();
    IntBag yCoord = new IntBag();
    Bag cellNeighbors = new Bag();
    Bag matrixNeighbors = new Bag();
    Bag lumenNeighbors = new Bag();
    
    this.grid.getNeighborsHexagonalDistance(this.x, this.y, 1, true, neighbors, xCoord, yCoord);
    neighbors.remove(this);
    for (int i = 0; i < neighbors.numObjs; i++) {
      Agent agent = (Agent) neighbors.get(i);
      if (agent instanceof AlvCell) {
        cellNeighbors.add(agent);
        if (this.cluster.contains(agent)) {
        }
      }
      if (agent instanceof Matrix) {
        matrixNeighbors.add(agent);
      }
      if (agent instanceof FreeSpace) {
        lumenNeighbors.add(agent);
      }
    }
    lumenNeighbors.shuffle(random_func);
    for (int i = 0; i < lumenNeighbors.numObjs; i++) {
      FreeSpace curLumen = (FreeSpace) lumenNeighbors.get(i);
      int soluteID = 0;
      double transportAmount = 1.0;
      curLumen.intake(soluteID, transportAmount);
    }
    
  } // end transport()
  
  /* *** CALL ONLY IN SOLUTE DIFFUSION-BASED MIGRATION MODE *** */
  public void secreteSolutes (final DoubleGrid2D solGrid, final int x, final int y, double maxConc) {
    if (migration_mode != 1) {
      System.out.println("Cell::secreteSolutes() called in non-diffusion mode");
      return;
    }
    int curX, curY;
    double finalOutput = solute_output;
    if (attached && cluster != null) {
      finalOutput = finalOutput * (1.0 + cluster.getCellRatio());
    }
    finalOutput = finalOutput / 7.0;
    // secrete CENTER
    solGrid.field[x][y] += finalOutput;
    if (solGrid.field[x][y] > maxConc) solGrid.field[x][y] = maxConc;
    // secrete UPPER LEFT
    curX = solGrid.stx(solGrid.ulx(x,y));
    curY = solGrid.sty(solGrid.uly(x,y));
    solGrid.field[curX][curY] += finalOutput;
    if (solGrid.field[curX][curY] > maxConc) solGrid.field[curX][curY] = maxConc;
    // secrete UPPER RIGHT
    curX = solGrid.stx(solGrid.urx(x,y));
    curY = solGrid.sty(solGrid.ury(x,y));
    solGrid.field[curX][curY] += finalOutput;
    if (solGrid.field[curX][curY] > maxConc) solGrid.field[curX][curY] = maxConc;
    // secrete DOWN LEFT
    curX = solGrid.stx(solGrid.dlx(x,y));
    curY = solGrid.sty(solGrid.dly(x,y));
    solGrid.field[curX][curY] += finalOutput;
    if (solGrid.field[curX][curY] > maxConc) solGrid.field[curX][curY] = maxConc;
    // secrete DOWN RIGHT
    curX = solGrid.stx(solGrid.drx(x,y));
    curY = solGrid.sty(solGrid.dry(x,y));
    solGrid.field[curX][curY] += finalOutput;
    if (solGrid.field[curX][curY] > maxConc) solGrid.field[curX][curY] = maxConc;
    // secrete UP
    curX = solGrid.stx(solGrid.upx(x,y));
    curY = solGrid.sty(solGrid.upy(x,y));
    solGrid.field[curX][curY] += finalOutput;
    if (solGrid.field[curX][curY] > maxConc) solGrid.field[curX][curY] = maxConc;
    // secrete DOWN
    curX = solGrid.stx(solGrid.downx(x,y));
    curY = solGrid.sty(solGrid.downy(x,y));
    solGrid.field[curX][curY] += finalOutput;
    if (solGrid.field[curX][curY] > maxConc) solGrid.field[curX][curY] = maxConc;
  }
  
  
  // rearrange itself within its cluster
  public void rearrangeClustered(SimState state) {
    Bag neighbors = new Bag();
    IntBag xCoord = new IntBag();
    IntBag yCoord = new IntBag();
    Bag cellNeighbors = new Bag();
    Bag memberCells = new Bag(); // neighboring cells that belong to the same cluster
    Bag nonMemberCells = new Bag(); // neighboring cells but not part of the same cluster
    Bag matrixNeighbors = new Bag();
    Bag lumenNeighbors = new Bag();
    
    // BEGIN COLLECTING INFORMATION ABOUT ITS SURROUNDINGS
    this.grid.getNeighborsHexagonalDistance(this.x, this.y, 1, true, neighbors, xCoord, yCoord);
    neighbors.remove(this);
    for (int i = 0; i < neighbors.numObjs; i++) {
      Agent agent = (Agent) neighbors.get(i);
      if (agent instanceof AlvCell) {
        cellNeighbors.add(agent);
        if (this.cluster.contains(agent)) {
          memberCells.add(agent);
        }
        else {
          nonMemberCells.add(agent);
        }
      }
      if (agent instanceof Matrix) {
        matrixNeighbors.add(agent);
      }
      if (agent instanceof FreeSpace) {
        lumenNeighbors.add(agent);
      }
    }
    // DONE COLLECTING INFORMATION ABOUT ITS SURROUNDINGS

    // BEGIN: CLASSIFICATION OF ITS SURROUNDINGS
    int envConfig = 0;
    int cellNum = cellNeighbors.numObjs;
    int memberCellNum = memberCells.numObjs;
    int nonMemberCellNum = nonMemberCells.numObjs;
    int matrixNum = matrixNeighbors.numObjs;
    int lumenNum = lumenNeighbors.numObjs;
    
    if (cellNum == 6) {
      // ALL CELLS
      envConfig = 1;
      this.updateRuleUsage(5);
    }
    else if (matrixNum == 6) {
      // ALL MATRIX
      envConfig = 2;
      this.updateRuleUsage(8);
    }
    else if (lumenNum == 6) {
      // ALL LUMEN
      envConfig = 3;
      this.updateRuleUsage(9);
    }
    else if (cellNum > 0 && matrixNum > 0 && lumenNum == 0) {
      // CELLS + MATRIX but NO LUMEN
      envConfig = 4;
      this.updateRuleUsage(12);
      switch (memberCellNum) {
        case 1:
          envConfig = 4150;
        break;
        case 2:
          envConfig = 4240;
        break;
        case 3:
          envConfig = 4330;
        break;
        case 4:
          envConfig = 4420;
        break;
        case 5:
          envConfig = 4510;
        break;
      }
    }
    else if (cellNum > 0 && matrixNum == 0 && lumenNum > 0) {
      // CELLS + LUMEN but NO MATRIX
      envConfig = 5;
      this.updateRuleUsage(22);
    }
    else if (cellNum == 0 && matrixNum > 0 && lumenNum > 0) {
      // MATRIX + LUMEN but NO CELLS
      envConfig = 6;
      this.updateRuleUsage(26);
    }
    else if (cellNum > 0 && matrixNum > 0 && lumenNum > 0) {
      // CELLS + MATRIX + LUMEN
      envConfig = 7;
      this.updateRuleUsage(27);
      switch (matrixNum) {
        case 1: envConfig = 7919;
        break;
        case 2: envConfig = 7929;
        break;
      }
    }
    else {
      // DEFAULT
      envConfig = 0;
    }
    // DONE: CLASSIFICATION OF ITS SURROUNDINGS
    
    // START: ACTUAL REARRANGEMENT
    int randnumInt = 0;
    int oldX = this.x;
    int oldY = this.y;
    // calculate the probability of moving out of the image plane
    double totalCellNum = (double) cluster.getTotalCellNum();
    double jumpdraw = random_func.nextDouble();
    double threshold = 0.0;
    // sigmoidal function
    
    threshold = 1.0 / (1.0 + java.lang.Math.exp(-(totalCellNum - sig_shift) / sig_stretch));
    if (this.cluster != null && this.cluster.getActiveCellNum() < 6) { threshold = 0.0; }
    
    threshold = 0.0;
    
    // threshold = 1.0 - java.lang.Math.sqrt(java.lang.Math.PI / totalCellNum);
    
    // if (this.cluster != null) { threshold = this.cluster.getJumpVal(); }

    if (envConfig == 1 && nonMemberCellNum == 0) {
      // ALL CELLS
      if (jumpdraw < threshold) {
        Matrix newMatrix = new Matrix(oldX, oldY, grid);
        grid.set(oldX, oldY, newMatrix);
        this.cluster.changeToOuterCell(this);
        this.alive = false;
        this.updateRuleUsage(6);
      }
      else {
        FreeSpace newLumen = new FreeSpace(oldX, oldY, grid);
        grid.set(oldX, oldY, newLumen);
        this.cluster.addLumen(newLumen);
        this.cluster.changeToOuterCell(this);
        this.alive = false;
        this.updateRuleUsage(7);
      }
    } // end if(envConfig == 1)
    else if (envConfig == 2) {
      // ALL MATRIX
      /* do nothing */
      /*
      // DISRUPTION (divide)
      Matrix tgtMatrix = (Matrix) matrixNeighbors.get(random_func.nextInt(matrixNeighbors.numObjs));
      AlvCell alvCell = new AlvCell(tgtMatrix.x, tgtMatrix.y, this.grid, random_func.nextInt(), paramDB);
      this.grid.set(tgtMatrix.x, tgtMatrix.y, alvCell);
      ((Culture)state).cellBag.add(alvCell);
      alvCell.setMigrationMode(migration_mode);
      ((Culture)state).schedule.scheduleOnce(alvCell);
       */
    } // end if(envConfig == 2)
    else if (envConfig == 3) {
      // ALL LUMEN
      if (jumpdraw < threshold) {
        FreeSpace newLumen = new FreeSpace(oldX, oldY, grid);
        grid.set(oldX, oldY, newLumen);
        this.cluster.addLumen(newLumen);
        this.cluster.changeToOuterCell(this);
        this.alive = false;
        this.updateRuleUsage(10);
      }
      else {
        densityMove(3, 1, state);
        this.updateRuleUsage(11);
      }
    } // end if(envConfig == 3)
    else if (envConfig == 4) {
      // CELLS + MATRIX (NO LUMEN)

      // see below
      
    } // end if(envConfig == 4)
    else if (envConfig == 4150) {
      // 1 MEMBER CELL + NON MEMBER CELLS + MATRIX
      if (jumpdraw < threshold) {
        this.updateRuleUsage(13);
        Matrix newMatrix = new Matrix(oldX, oldY, grid);
        grid.set(oldX, oldY, newMatrix);
        this.cluster.changeToOuterCell(this);
        this.alive = false;
      }
      else {
        this.updateRuleUsage(14);
        AlvCell memberCell = (AlvCell) memberCells.get(0);
        Bag suitableMatrix = new Bag();
        for (int i = 0; i < matrixNeighbors.numObjs; i++) {
          Matrix curMatrix = (Matrix) matrixNeighbors.get(i);
          if (areNeighbors(memberCell, curMatrix)) {
            suitableMatrix.add(curMatrix);
          }
        }
        if (suitableMatrix.numObjs > 0) {
          randnumInt = random_func.nextInt(suitableMatrix.numObjs);
          Matrix targetMatrix = (Matrix) suitableMatrix.get(randnumInt);
          this.moveTo(targetMatrix.x, targetMatrix.y);
          targetMatrix.moveTo(oldX, oldY);
        }
      }
    } // end if(envConfig == 4150)
    else if (envConfig == 4240) {
      // 2 MEMBER CELLS + NON-MEMBER CELLS + MATRIX
      memberCells.shuffle(random_func);
      AlvCell memberCellOne = (AlvCell) memberCells.get(0);
      AlvCell memberCellTwo = (AlvCell) memberCells.get(1);
      Bag suitableMatrixBag = new Bag();
      if (areNeighbors(memberCellOne, memberCellTwo)) {
        if (jumpdraw < threshold) {
          this.updateRuleUsage(15);
          Matrix newMatrix = new Matrix(oldX, oldY, grid);
          grid.set(oldX, oldY, newMatrix);
          this.cluster.changeToOuterCell(this);
          this.alive = false;
        }
        else {
          this.updateRuleUsage(16);
          for (int i = 0; i < matrixNeighbors.numObjs; i++) {
            Matrix curMatrix = (Matrix) matrixNeighbors.get(i);
            if (areNeighbors(curMatrix, memberCellOne) || areNeighbors(curMatrix, memberCellTwo)) {
              suitableMatrixBag.add(curMatrix);
            }
          }
          if (suitableMatrixBag.numObjs > 0) {
            randnumInt = random_func.nextInt(suitableMatrixBag.numObjs);
            Matrix targetMatrix = (Matrix) suitableMatrixBag.get(randnumInt);
            this.moveTo(targetMatrix.x, targetMatrix.y);
            targetMatrix.moveTo(oldX, oldY);
          }
        }
      }
      else {
        this.updateRuleUsage(17);
        for (int i = 0; i < matrixNeighbors.numObjs; i++) {
          Matrix curMatrix = (Matrix) matrixNeighbors.get(i);
          if (areNeighbors(memberCellOne, curMatrix) && !areNeighbors(memberCellTwo, curMatrix)) {
            suitableMatrixBag.add(curMatrix);
          }
        }
        if (suitableMatrixBag.numObjs > 0) {
          randnumInt = random_func.nextInt(suitableMatrixBag.numObjs);
          Matrix targetMatrix = (Matrix) suitableMatrixBag.get(randnumInt);
          int direction = determineMoveDirection(memberCellTwo, this);
          this.moveTo(targetMatrix.x, targetMatrix.y);
          targetMatrix.moveTo(oldX, oldY);
          memberCellTwo.pullCells(this, direction);
        }
      }
    } // end if(envConfig == 4240)
    else if (envConfig == 4330) {
      this.updateRuleUsage(18);
      /* do nothing */
      /*
      // ALTERATION (divide to matrix with maximal number of cell neighbors)
      Bag bestMatrixNeighbors = new Bag();
      int bestCellNeighborCount = 0;
      for (int k = 0; k < matrixNeighbors.numObjs; k++) {
        Matrix matrixAgent = (Matrix) matrixNeighbors.get(k);
        int cellNeighborCount = 0;
        for (int j = 0; j < cellNeighbors.numObjs; j++) {
          AlvCell cellNeighbor = (AlvCell) cellNeighbors.get(j);
          if (areNeighbors(cellNeighbor, matrixAgent)) {
            cellNeighborCount += 1;
          }
        }
        if (cellNeighborCount > bestCellNeighborCount) {
          bestCellNeighborCount = cellNeighborCount;
          bestMatrixNeighbors = new Bag();
          bestMatrixNeighbors.add(matrixAgent);
        }
        else if (cellNeighborCount == bestCellNeighborCount) {
          bestMatrixNeighbors.add(matrixAgent);
        }
      }
      //replace the best matrix found with the daughter cell
      Matrix tgtMat = (Matrix) bestMatrixNeighbors.get(random_func.nextInt(bestMatrixNeighbors.numObjs));
      AlvCell alvCell = new AlvCell(tgtMat.x, tgtMat.y, this.grid, random_func.nextInt(), paramDB);
      this.grid.set(tgtMat.x, tgtMat.y, alvCell);
      ((Culture)state).cellBag.add(alvCell);
      alvCell.setMigrationMode(migration_mode);
      ((Culture)state).schedule.scheduleOnce(alvCell);
      */
    } // end if (envConfig == 4330)
    else if (envConfig == 4420) {
      this.updateRuleUsage(19);
      randnumInt = random_func.nextInt(matrixNeighbors.numObjs);
      Matrix targetMatrix = (Matrix) matrixNeighbors.get(randnumInt);
      this.moveTo(targetMatrix.x, targetMatrix.y);
      targetMatrix.moveTo(oldX, oldY);
    } // end if(envConfig == 4420)
    else if (envConfig == 4510) {
      // 5 CELLS + 1 MATRIX
      if (jumpdraw < threshold) {
        this.updateRuleUsage(20);
        Matrix newMatrix = new Matrix(oldX, oldY, grid);
        grid.set(oldX, oldY, newMatrix);
        this.cluster.changeToOuterCell(this);
        this.alive = false;
      }
      else {
        this.updateRuleUsage(21);
        Matrix targetMatrix = (Matrix) matrixNeighbors.get(0);
        this.moveTo(targetMatrix.x, targetMatrix.y);
        FreeSpace newLumen = new FreeSpace(oldX, oldY, grid);
        grid.set(oldX, oldY, newLumen);
        this.cluster.addLumen(newLumen);
      }
    } // end if (envConfig == 4510)
    else if (envConfig == 5 && nonMemberCellNum == 0) {
      // CELLS + LUMEN (NO MATRIX)
      if (jumpdraw < threshold) {
        this.updateRuleUsage(23);
        FreeSpace newLumen = new FreeSpace(oldX, oldY, grid);
        grid.set(oldX, oldY, newLumen);
        this.cluster.addLumen(newLumen);
        this.cluster.changeToOuterCell(this);
        this.alive = false;
      }
      else {
        randnumInt = random_func.nextInt(cellNeighbors.numObjs);
        AlvCell targetCell = (AlvCell) cellNeighbors.get(randnumInt);
        int newX = targetCell.x;
        int newY = targetCell.y;
        int moveDir = determineMoveDirection(this, targetCell);
        if (targetCell.pushIterative(this, moveDir, 1)) {
          this.moveTo(newX, newY);
          FreeSpace newLumen = new FreeSpace(oldX, oldY, grid);
          grid.set(oldX, oldY, newLumen);
          this.cluster.addLumen(newLumen);
          this.updateRuleUsage(24);
        }
        else {
          randnumInt = random_func.nextInt(lumenNeighbors.numObjs);
          FreeSpace targetLumen = (FreeSpace) lumenNeighbors.get(randnumInt);
          this.moveTo(targetLumen.x, targetLumen.y);
          targetLumen.moveTo(oldX, oldY);
          this.updateRuleUsage(25);
        }
      }
    } // end if(envConfig == 5)
    else if (envConfig == 6) {
      // MATRIX + LUMEN (NO CELLS)
      /* do nothing */
      /*
      // DISRUPTION (divide to lumen adjacent to matrix)
      Bag acceptableLumens = new Bag();
      for (int m = 0; m < lumenNeighbors.numObjs; m++) {
        FreeSpace lum = (FreeSpace) lumenNeighbors.get(m);
        //find out if this Lumen has a matrix neighbor
        for (int n = 0; n < matrixNeighbors.numObjs; n++) {
          Matrix mat = (Matrix) matrixNeighbors.get(n);
          if (areNeighbors(lum, mat)) {
            acceptableLumens.add(lum);
          }
        }
      }
      if (acceptableLumens.numObjs > 0) {
        FreeSpace tgtLum = (FreeSpace) acceptableLumens.get(random_func.nextInt(acceptableLumens.numObjs));
        AlvCell alvCell = new AlvCell(tgtLum.x, tgtLum.y, this.grid, random_func.nextInt(), paramDB);
        this.grid.set(tgtLum.x, tgtLum.y, alvCell);
        ((Culture)state).cellBag.add(alvCell);
        alvCell.setMigrationMode(migration_mode);
        ((Culture)state).schedule.scheduleOnce(alvCell);
      }
      */
    } // end if(envConfig == 6)
    else if (envConfig == 7) {
      // CELLS + MATRIX + LUMEN
      if (repair_sim && depolarizingEnvironment()) {
        // move into lumen adjacent to matrix
        Bag nonconfLumens = new Bag();
        for (int i = 0; i < lumenNeighbors.numObjs; i++) {
          FreeSpace curLumen = (FreeSpace) lumenNeighbors.get(i);
          for (int j = 0; j < matrixNeighbors.numObjs; j++) {
            if (areNeighbors(curLumen, (Matrix)matrixNeighbors.get(j))) {
              nonconfLumens.add(curLumen);
              break;
            }
          }
        }
        if (nonconfLumens.numObjs > 0) {
          FreeSpace targetLum = (FreeSpace) nonconfLumens.get(random_func.nextInt(nonconfLumens.numObjs));
          AlvCell pulledCell = (AlvCell) memberCells.get(random_func.nextInt(memberCells.numObjs));
          int pullDirection = determineMoveDirection(pulledCell, this);
          if (random_func.nextBoolean(0.5)) { pullDirection = determineMoveDirection(this, targetLum); }
          this.moveTo(targetLum.x, targetLum.y);
          this.cluster.removeLumen(targetLum);
          Matrix newMatrix = new Matrix(oldX, oldY, grid);
          grid.set(oldX, oldY, newMatrix);
          // pulledCell.pullCells(this, pullDirection);
        }
      }
      
    } // end if(envConfig == 7)
    else if (envConfig == 7919 && nonMemberCellNum == 0) {
      this.updateRuleUsage(28);
      if (repair_sim && depolarizingEnvironment()) {
        // move into lumen adjacent to matrix
        Bag nonconfLumens = new Bag();
        for (int i = 0; i < lumenNeighbors.numObjs; i++) {
          FreeSpace curLumen = (FreeSpace) lumenNeighbors.get(i);
          for (int j = 0; j < matrixNeighbors.numObjs; j++) {
            if (areNeighbors(curLumen, (Matrix)matrixNeighbors.get(j))) {
              nonconfLumens.add(curLumen);
              break;
            }
          }
        }
        if (nonconfLumens.numObjs > 0) {
          FreeSpace targetLum = (FreeSpace) nonconfLumens.get(random_func.nextInt(nonconfLumens.numObjs));
          AlvCell pulledCell = (AlvCell) memberCells.get(random_func.nextInt(memberCells.numObjs));
          int pullDirection = determineMoveDirection(pulledCell, this);
          this.moveTo(targetLum.x, targetLum.y);
          this.cluster.removeLumen(targetLum);
          Matrix newMatrix = new Matrix(oldX, oldY, grid);
          grid.set(oldX, oldY, newMatrix);
          pulledCell.pullCells(this, pullDirection);
        }
      }
      else {
        Matrix targetMatrix = (Matrix) matrixNeighbors.get(0);
        this.moveTo(targetMatrix.x, targetMatrix.y);
        FreeSpace newLumen = new FreeSpace(oldX, oldY, grid);
        grid.set(oldX, oldY, newLumen);
        this.cluster.addLumen(newLumen);
      }
    } // end if(envConfig == 7919)
    else if (envConfig == 7929 && nonMemberCellNum == 0) {
      Matrix matrixOne = (Matrix) matrixNeighbors.get(0);
      Matrix matrixTwo = (Matrix) matrixNeighbors.get(1);
      if (!areNeighbors(matrixOne, matrixTwo)) {
        for (int i = 0; i < cellNeighbors.numObjs; i++) {
          AlvCell curCell = (AlvCell) cellNeighbors.get(i);
          if (areNeighbors(curCell, matrixOne) && areNeighbors(curCell, matrixTwo)) {
            int pullDirection = determineMoveDirection(curCell, this);
            randnumInt = random_func.nextInt(lumenNeighbors.numObjs);
            FreeSpace targetLumen = (FreeSpace) lumenNeighbors.get(randnumInt);
            this.moveTo(targetLumen.x, targetLumen.y);
            this.cluster.removeLumen(targetLumen);
            Matrix newMatrix = new Matrix(oldX, oldY, grid);
            grid.set(oldX, oldY, newMatrix);
            curCell.pullCells(this, pullDirection);
            this.updateRuleUsage(29);
            break;
          }
        }
      }
      else if (repair_sim && depolarizingEnvironment()) {
        // move into lumen adjacent to matrix
        Bag nonconfLumens = new Bag();
        for (int i = 0; i < lumenNeighbors.numObjs; i++) {
          FreeSpace curLumen = (FreeSpace) lumenNeighbors.get(i);
          for (int j = 0; j < matrixNeighbors.numObjs; j++) {
            if (areNeighbors(curLumen, (Matrix)matrixNeighbors.get(j))) {
              nonconfLumens.add(curLumen);
              break;
            }
          }
        }
        if (nonconfLumens.numObjs > 0) {
          FreeSpace targetLum = (FreeSpace) nonconfLumens.get(random_func.nextInt(nonconfLumens.numObjs));
          AlvCell pulledCell = (AlvCell) memberCells.get(random_func.nextInt(memberCells.numObjs));
          int pullDirection = determineMoveDirection(pulledCell, this);
          if (random_func.nextBoolean(0.5)) { pullDirection = determineMoveDirection(this, targetLum); }
          this.moveTo(targetLum.x, targetLum.y);
          this.cluster.removeLumen(targetLum);
          Matrix newMatrix = new Matrix(oldX, oldY, grid);
          grid.set(oldX, oldY, newMatrix);
          pulledCell.pullCells(this, pullDirection);
        }
      }
      
    } // end if(envConfig == 7929)
    else {
      // DUMMY CASE
    } // end else(envConfig)
    
  } // end rearrangeClustered()
  
  public boolean depolarizingEnvironment() {
    boolean depolarizing = true;

    //polarization-preserving environments
    String pattern1 = "cfcmmm";  //one free space
    String pattern2 = "cffcmm";  //two free spaces
    String pattern3 = "cfffcm";  //three free spaces

    //contiguous cell and matrix spaces
    String pattern4 = "ccmmmm";
    String pattern5 = "cccmmm";
    String pattern6 = "ccccmm";
    String pattern7 = "cccccm";

    //contiguous cell and matrix spaced in which there is some free space
    //three cells, one free space
    String pattern8 = "cfccmm";
    String pattern9 = "ccfcmm";
    //three cells, two free space
    String pattern10 = "ccffcm";
    String pattern11 = "cffccm";
    String pattern12 = "cfcfcm";

    //create the neighbor array;
    Agent[] agents = new Agent[6];
    agents[5] = (Agent)grid.get(grid.stx(grid.dlx(this.x,this.y)), grid.sty(grid.dly(this.x,this.y)));
    agents[4] = (Agent)grid.get(grid.stx(grid.downx(this.x,this.y)), grid.sty(grid.downy(this.x,this.y)));
    agents[3] = (Agent)grid.get(grid.stx(grid.drx(this.x,this.y)), grid.sty(grid.dry(this.x,this.y)));
    agents[0] = (Agent)grid.get(grid.stx(grid.ulx(this.x,this.y)), grid.sty(grid.uly(this.x,this.y)));
    agents[2] = (Agent)grid.get(grid.stx(grid.urx(this.x,this.y)), grid.sty(grid.ury(this.x,this.y)));
    agents[1] = (Agent)grid.get(grid.stx(grid.upx(this.x,this.y)), grid.sty(grid.upy(this.x,this.y)));
    
    for(int j = 0; j < 6; j++) {
      String neighborsAsString = "";
      for(int i = j; i < 6+j; i++) {
        Agent a = agents[i%6];
        if (a instanceof Cell) { neighborsAsString += "c"; }
        else if (a instanceof Matrix) { neighborsAsString += "m"; }
        else if (a instanceof FreeSpace) { neighborsAsString += "f"; }
      }
      if (pattern1.equals(neighborsAsString) ||
          pattern2.equals(neighborsAsString) ||
          pattern3.equals(neighborsAsString) ||
          pattern4.equals(neighborsAsString) ||
          pattern5.equals(neighborsAsString) ||
          pattern6.equals(neighborsAsString) ||
          pattern7.equals(neighborsAsString) ||
          pattern8.equals(neighborsAsString) ||
          pattern9.equals(neighborsAsString) ||
          pattern10.equals(neighborsAsString) ||
          pattern11.equals(neighborsAsString) ||
          pattern12.equals(neighborsAsString)) {
        depolarizing = false;
      }
    }
    return depolarizing; 
  } // end depolarizingEnvironment();
  
  void updateRuleUsage(int rule) {
    int ruleID = rule;
    /*
    switch (rule) {
      case 1:
        ruleID = 1;
      break;
      case 2:
        ruleID = 2;
      break;
    }
     */
    ruleUsageRecords.add(new Int2D(this.age, ruleID));
  }
  
  // push an agent in iterative manner
  boolean pushIterative(Agent sourceAgent, int pushDirection, int pushCount) {
    boolean pushStatus = false; // return value
    
    // terminal case and to prevent cyclic calling
    if (pushToggle || pushCount > MAX_PUSH) {
      this.pushToggle = false;
      return pushStatus;
    }
    this.pushToggle = true;
    
    int newX = this.x;
    int newY = this.y;
    switch (pushDirection) {
      case UP: 
        newX = grid.stx(grid.upx(this.x, this.y));
        newY = grid.sty(grid.upy(this.x, this.y));
        break;
      case UR:
        newX = grid.stx(grid.urx(this.x, this.y));
        newY = grid.sty(grid.ury(this.x, this.y));
        break;
      case DR:
        newX = grid.stx(grid.drx(this.x, this.y));
        newY = grid.sty(grid.dry(this.x, this.y));
        break;
      case DN:
        newX = grid.stx(grid.downx(this.x, this.y));
        newY = grid.sty(grid.downy(this.x, this.y));
        break;
      case DL:
        newX = grid.stx(grid.dlx(this.x, this.y));
        newY = grid.sty(grid.dly(this.x, this.y));
        break;
      case UL:
        newX = grid.stx(grid.ulx(this.x, this.y));
        newY = grid.sty(grid.uly(this.x, this.y));
        break;
      default:
        break;
    }
    Agent target = (Agent) grid.get(newX, newY);
    
    // push into matrix
    if (target != null && target instanceof Matrix) {
      target.moveTo(this.x, this.y);
      this.moveTo(newX, newY);
      pushStatus = true;
    }
    // push another cell
    if (target != null && target instanceof AlvCell) {
      boolean pushSuccess = ((AlvCell)target).pushIterative(this, pushDirection, pushCount+1);
      if (pushSuccess) {
        Matrix newMatrix = new Matrix(this.x, this.y, grid);
        grid.set(this.x, this.y, newMatrix);
        this.moveTo(newX, newY);
        pushStatus = true;
      }
    }
    // push lumen
    /*
    if (target != null && target instanceof Lumen) {
      boolean pushSuccess = ((Lumen)target).pushIterative(this, pushDirection, pushCount);
      if (pushSuccess) {
        Matrix newMatrix = new Matrix(this.x, this.y, grid);
        grid.set(this.x, this.y, newMatrix);
        grid.set(newX, newY, this);
        this.setCoordinates(newX, newY);
        pushStatus = true;
      }
    }
     */
    // reset cyclic pushing sign
    this.pushToggle = false;
    return pushStatus;
  } // end pushIterative()
  
  // pull itself to the indicated direction in an iterative manner (pulling its neighbors too)
  public void pullIterative(AlvCell sourceCell, int pullDirection) {
    
    // to prevent cyclic pulling
    if (this.pullToggle) {
      return;
    }
    if (cluster == null) {
      System.out.println("Cell::pullIterative has null cluster");
      return;
    }
    
    // get object at the target location
    int newX = this.x;
    int newY = this.y;
    switch (pullDirection) {
        case UP: 
          newX = grid.stx(grid.upx(this.x, this.y));
          newY = grid.sty(grid.upy(this.x, this.y));
          break;
        case UR:
          newX = grid.stx(grid.urx(this.x, this.y));
          newY = grid.sty(grid.ury(this.x, this.y));
          break;
        case DR:
          newX = grid.stx(grid.drx(this.x, this.y));
          newY = grid.sty(grid.dry(this.x, this.y));
          break;
        case DN:
          newX = grid.stx(grid.downx(this.x, this.y));
          newY = grid.sty(grid.downy(this.x, this.y));
          break;
        case DL:
          newX = grid.stx(grid.dlx(this.x, this.y));
          newY = grid.sty(grid.dly(this.x, this.y));
          break;
        case UL:
          newX = grid.stx(grid.ulx(this.x, this.y));
          newY = grid.sty(grid.uly(this.x, this.y));
          break;
        default:
          break;
      }
    Agent target = (Agent) grid.get(newX, newY);
    if (target == null) {
      System.out.println("Cell::pullIteravite() has NULL target");
      return;
    }
    if (target instanceof AlvCell) {
      return;
    }
    if (target instanceof FreeSpace) {
      cluster.removeLumen((FreeSpace)target);
    }
    
    // now pull neighboring objects
    Bag neighbors = new Bag();
    IntBag xCoord = new IntBag();
    IntBag yCoord = new IntBag();
    Bag cellNeighbors = new Bag();
    Bag lumenNeighbors = new Bag();
    Bag movedNeighbors = new Bag();
    
    // assess the neighboring environment
    this.grid.getNeighborsHexagonalDistance(this.x, this.y, 1, true, neighbors, xCoord, yCoord);
    neighbors.remove(this);
    if (sourceCell != null) {
      neighbors.remove(sourceCell);
    }
    for (int i = 0; i < neighbors.numObjs; i++) {
      Agent agent = (Agent) neighbors.get(i);
      if (agent instanceof AlvCell) {
        cellNeighbors.add(agent);
      }
      if (agent instanceof FreeSpace) {
        lumenNeighbors.add(agent);
      }
    }
    // done with assessing own environment

    // move to a new location
    int oppositeDirection = getOppositeDirection(pullDirection);
    Agent oppositeAgent = (Agent) getAgentAt(this, oppositeDirection);
    if (oppositeAgent instanceof FreeSpace) {
      FreeSpace newLumen = new FreeSpace(this.x, this.y, grid);
      grid.set(newLumen.x, newLumen.y, newLumen);
      cluster.addLumen(newLumen);
    }
    else {
      Matrix newMatrix = new Matrix(this.x, this.y, grid);
      grid.set(newMatrix.x, newMatrix.y, newMatrix);
    }
    this.moveTo(newX, newY);
    
    // now pull neighboring objects
    Bag newNeighbors = new Bag();
    IntBag newXCoord = new IntBag();
    IntBag newYCoord = new IntBag();
    Bag newCellNeighbors = new Bag();
    Bag newLumenNeighbors = new Bag();
    
    // assess the neighboring environment
    this.grid.getNeighborsHexagonalDistance(this.x, this.y, 1, true, newNeighbors, newXCoord, newYCoord);
    newNeighbors.remove(this);
    if (sourceCell != null) {
      newNeighbors.remove(sourceCell);
    }

    // pull adjacent cells within the cluster
    // added the 2nd condition to prevent pulling nonclustered cells (have null cluster)
    for (int i = 0; i < cellNeighbors.numObjs; i++) {
      AlvCell pulledCell = (AlvCell) cellNeighbors.get(i);
      if (!newNeighbors.contains(pulledCell) && this.cluster.containsCell(pulledCell)) {
        pulledCell.pullIterative(this, pullDirection);
      }
    }
    
  } // end pullIterative()
  
  public void pullCells(AlvCell sourceCell, int pullDirection) {
    
    // collect cells to move
    boolean canPull = true;
    int oppositeDirection = getOppositeDirection(pullDirection);
    Bag cellBag = new Bag();
    collectIterative(sourceCell, cellBag);

    // fill (replace) current cell locations with matrix or lumen
    for (int i = 0; i < cellBag.numObjs; i++) {
      AlvCell curCell = (AlvCell) cellBag.get(i);
      Agent oppositeAgent = (Agent) getAgentAt(curCell, oppositeDirection);
      if (oppositeAgent instanceof FreeSpace) {
        FreeSpace newLumen = new FreeSpace(curCell.x, curCell.y, grid);
        grid.set(newLumen.x, newLumen.y, newLumen);
        this.cluster.addLumen(newLumen);
      }
      else {
        Matrix newMatrix = new Matrix(curCell.x, curCell.y, grid);
        grid.set(newMatrix.x, newMatrix.y, newMatrix);
      }
    }
    
    // determine wheather the cells can be moved
    for (int i = 0; i < cellBag.numObjs; i++) {
      AlvCell curCell = (AlvCell) cellBag.get(i);
      int newX = curCell.x;
      int newY = curCell.y;
      switch (pullDirection) {
        case UP: 
          newX = grid.stx(grid.upx(curCell.x, curCell.y));
          newY = grid.sty(grid.upy(curCell.x, curCell.y));
          break;
        case UR:
          newX = grid.stx(grid.urx(curCell.x, curCell.y));
          newY = grid.sty(grid.ury(curCell.x, curCell.y));
          break;
        case DR:
          newX = grid.stx(grid.drx(curCell.x, curCell.y));
          newY = grid.sty(grid.dry(curCell.x, curCell.y));
          break;
        case DN:
          newX = grid.stx(grid.downx(curCell.x, curCell.y));
          newY = grid.sty(grid.downy(curCell.x, curCell.y));
          break;
        case DL:
          newX = grid.stx(grid.dlx(curCell.x, curCell.y));
          newY = grid.sty(grid.dly(curCell.x, curCell.y));
          break;
        case UL:
          newX = grid.stx(grid.ulx(curCell.x, curCell.y));
          newY = grid.sty(grid.uly(curCell.x, curCell.y));
          break;
        default:
          break;
      }
      Agent targetAgent = (Agent) grid.get(newX, newY);
      if (targetAgent == null || targetAgent instanceof AlvCell) {
        canPull = false;
        break;
      }
    }
    
    // move the cells
    if (canPull) { 
      for (int i = 0; i < cellBag.numObjs; i++) {
        AlvCell curCell = (AlvCell) cellBag.get(i);
        int newX = curCell.x;
        int newY = curCell.y;
        switch (pullDirection) {
          case UP: 
            newX = grid.stx(grid.upx(curCell.x, curCell.y));
            newY = grid.sty(grid.upy(curCell.x, curCell.y));
            break;
          case UR:
            newX = grid.stx(grid.urx(curCell.x, curCell.y));
            newY = grid.sty(grid.ury(curCell.x, curCell.y));
            break;
          case DR:
            newX = grid.stx(grid.drx(curCell.x, curCell.y));
            newY = grid.sty(grid.dry(curCell.x, curCell.y));
            break;
          case DN:
            newX = grid.stx(grid.downx(curCell.x, curCell.y));
            newY = grid.sty(grid.downy(curCell.x, curCell.y));
            break;
          case DL:
            newX = grid.stx(grid.dlx(curCell.x, curCell.y));
            newY = grid.sty(grid.dly(curCell.x, curCell.y));
            break;
          case UL:
            newX = grid.stx(grid.ulx(curCell.x, curCell.y));
            newY = grid.sty(grid.uly(curCell.x, curCell.y));
            break;
          default:
            break;
        }
        Agent targetAgent = (Agent) grid.get(newX, newY);
        if (targetAgent == null) {
          System.out.println("Cell::pullCell() has NULL target");
        }
        else if (targetAgent instanceof FreeSpace) {
          this.cluster.removeLumen((FreeSpace)targetAgent);
        }
        grid.set(newX, newY, curCell);
        curCell.setCoordinates(newX, newY);
      }
    }
    else {
      for (int i = 0; i < cellBag.numObjs; i++) {
        AlvCell curCell = (AlvCell) cellBag.get(i);
        Agent curAgent = (Agent) grid.get(curCell.x, curCell.y);
        if (curAgent instanceof FreeSpace) {
          this.cluster.removeLumen((FreeSpace)curAgent);
        }
        grid.set(curCell.x, curCell.y, curCell);
      }
    }
    
  } // end pullCells()
  
  void collectIterative(AlvCell sourceCell, Bag collectBag) {
    
    if (collectBag.contains(this)) { 
      return; 
    }
    else { 
      collectBag.add(this); 
    }
    Bag neighbors = new Bag();
    IntBag xCoord = new IntBag();
    IntBag yCoord = new IntBag();
    
    // assess the neighboring environment
    grid.getNeighborsHexagonalDistance(this.x, this.y, 1, true, neighbors, xCoord, yCoord);
    neighbors.remove(this);
    if (sourceCell != null) {
      neighbors.remove(sourceCell);
    }
    for (int i = 0; i < neighbors.numObjs; i++) {
      Agent agent = (Agent) neighbors.get(i);
      if (agent instanceof AlvCell && cluster.containsCell((AlvCell)agent) && !collectBag.contains(agent)) {
        ((AlvCell)agent).collectIterative(sourceCell, collectBag);
      }
    }
  } // end collectIterative()
  
  public int determineSurroundingMorphology() {
    int level01 = 1;
    int level02 = 2;
    int level03 = 3;
    int morphologyLevel = 0;
    IntBag xPositions = new IntBag();
    IntBag yPositions = new IntBag();
    Bag neighbors = new Bag();
    Bag matrixNeighbors = new Bag();
    Bag lumenNeighbors = new Bag();
    Bag cellNeighbors = new Bag();
    grid.getNeighborsHexagonalDistance
              (this.x, this.y, 1, true, neighbors, xPositions, yPositions);
    neighbors.remove(this);
    for (int i = 0; i < neighbors.numObjs; i++) {
      Agent neighbor = (Agent) neighbors.get(i);
      if (neighbor instanceof Cell) {
        cellNeighbors.add(neighbor);
      }
      else if (neighbor instanceof Matrix) {
        matrixNeighbors.add(neighbor);
      }
      else if (neighbor instanceof FreeSpace) {
        lumenNeighbors.add(neighbor);
      }
    }
    // Now determine morphology type and disruption level
    if (cellNeighbors.numObjs != 0 && matrixNeighbors.numObjs != 0 && lumenNeighbors.numObjs != 0) {
      // ALL THREE TYPES (CELL + MATRIX + LUMEN)
      boolean separatedSurfaces = true;
      for (int matId = 0; matId < matrixNeighbors.numObjs; matId++) {
        Matrix curMatrix = (Matrix) matrixNeighbors.get(matId);
        for (int lumId = 0; lumId < lumenNeighbors.numObjs; lumId++) {
          FreeSpace curLumen = (FreeSpace) lumenNeighbors.get(lumId);
          if (areNeighbors(curMatrix, curLumen)) {
            separatedSurfaces = false;
            break;
          }
        }
      }
      if (!separatedSurfaces) {
        morphologyLevel = level03;
      }
      else {
        if (cellNeighbors.numObjs == 2) {
          morphologyLevel = level01; // NORMAL
        }
        else if (cellNeighbors.numObjs == 3) {
          boolean connected = false;
          for (int i = 0; i < cellNeighbors.numObjs; i++) {
            for (int j = i+1; j < cellNeighbors.numObjs; j++) {
              if (areNeighbors((Cell)cellNeighbors.get(i), (Cell)cellNeighbors.get(j))) {
                connected = true;
                break;
              }
            }
            if (connected) { break; }
          }
          if (connected) {
            morphologyLevel = level02;
          }
          else {
            morphologyLevel = level03;
          }
        }
        else {
          morphologyLevel = level02;
        }
      }
    }
    else if (cellNeighbors.numObjs != 0 && matrixNeighbors.numObjs != 0) {
      // CELL + MATRIX only
      if (cellNeighbors.numObjs == 1 || cellNeighbors.numObjs == 5) {
        morphologyLevel = level02;
      }
      else if (cellNeighbors.numObjs == 4) {
        if (areNeighbors((Matrix)matrixNeighbors.get(0), (Matrix)matrixNeighbors.get(1))) {
          morphologyLevel = level02;
        }
        else {
          morphologyLevel = level03;
        }
      }
      else {
        boolean separatedSurfaces = true;
        for (int i = 0; i < cellNeighbors.numObjs; i++) {
          Cell curCell = (Cell) cellNeighbors.get(i);
          boolean connected = false;
          for (int j = i+1; j < cellNeighbors.numObjs; j++) {
            if (areNeighbors(curCell, (Cell)cellNeighbors.get(j))) {
              connected = true;
              break;
            }
          }
          if (!connected) {
            separatedSurfaces = false;
            break;
          }
        }
        if (separatedSurfaces) {
          morphologyLevel = level02;
        }
        else {
          morphologyLevel = level03;
        }
      }
    }
    else if (cellNeighbors.numObjs != 0 && lumenNeighbors.numObjs != 0) {
      // CELL + LUMEN only
      if (cellNeighbors.numObjs == 1 || cellNeighbors.numObjs == 5) {
        morphologyLevel = level02;
      }
      else if (cellNeighbors.numObjs == 4) {
        if (areNeighbors((FreeSpace)lumenNeighbors.get(0), (FreeSpace)lumenNeighbors.get(1))) {
          morphologyLevel = level02;
        }
        else {
          morphologyLevel = level03;
        }
      }
      else {
        boolean separatedSurfaces = true;
        for (int i = 0; i < cellNeighbors.numObjs; i++) {
          Cell curCell = (Cell) cellNeighbors.get(i);
          boolean connected = false;
          for (int j = i+1; j < cellNeighbors.numObjs; j++) {
            if (areNeighbors(curCell, (Cell)cellNeighbors.get(j))) {
              connected = true;
              break;
            }
          }
          if (!connected) {
            separatedSurfaces = false;
            break;
          }
        }
        if (separatedSurfaces) {
          morphologyLevel = level02;
        }
        else {
          morphologyLevel = level03;
        }
      }
    }
    else if (matrixNeighbors.numObjs != 0 && lumenNeighbors.numObjs != 0) {
      // MATRIX + LUMEN only
      if (matrixNeighbors.numObjs == 1 || matrixNeighbors.numObjs == 5) {
        morphologyLevel = level02;
      }
      else if (matrixNeighbors.numObjs == 4) {
        if (areNeighbors((FreeSpace)lumenNeighbors.get(0), (FreeSpace)lumenNeighbors.get(1))) {
          morphologyLevel = level02;
        }
        else {
          morphologyLevel = level03;
        }
      }
      else {
        boolean separatedSurfaces = true;
        for (int i = 0; i < matrixNeighbors.numObjs; i++) {
          Matrix curMatrix = (Matrix) matrixNeighbors.get(i);
          boolean connected = false;
          for (int j = i+1; j < matrixNeighbors.numObjs; j++) {
            if (areNeighbors(curMatrix, (Matrix)matrixNeighbors.get(j))) {
              connected = true;
              break;
            }
          }
          if (!connected) {
            separatedSurfaces = false;
            break;
          }
        }
        if (separatedSurfaces) {
          morphologyLevel = level02;
        }
        else {
          morphologyLevel = level03;
        }
      }
    }
    else {
      // ONE TYPE ONLY
      morphologyLevel = level02;
    }
    
    return morphologyLevel;
  } // end determineSurroundingMorphology()
  
  
  /* ====================== END STEP & SUPPORT FUNCTIONS ================== */
  
  /* ====================== MOVE & SUPPORT FUNCTIONS ====================== */
  
  // overwrites Agent::moveTo() function [sets moved flag to true]
  public void moveTo(int newX, int newY) {
    this.moved = true;
    this.grid.set(newX, newY, this);
    this.setCoordinates(newX,newY);
  }
  
  // returns numerical representation of the direction 'from'=>'to'
  public int determineMoveDirection(Agent from, Agent to) {
    int toX = to.x;
    int toY = to.y;
    int upX = grid.stx(grid.upx(from.x, from.y));
    int upY = grid.sty(grid.upy(from.x, from.y));
    int urX = grid.stx(grid.urx(from.x, from.y));
    int urY = grid.sty(grid.ury(from.x, from.y));
    int drX = grid.stx(grid.drx(from.x, from.y));
    int drY = grid.sty(grid.dry(from.x, from.y));
    int dnX = grid.stx(grid.downx(from.x, from.y));
    int dnY = grid.sty(grid.downy(from.x, from.y));
    int dlX = grid.stx(grid.dlx(from.x, from.y));
    int dlY = grid.sty(grid.dly(from.x, from.y));
    int ulX = grid.stx(grid.ulx(from.x, from.y));
    int ulY = grid.sty(grid.uly(from.x, from.y));
    if (toX == upX && toY == upY) {
      return UP;
    }
    if (toX == ulX && toY == ulY) {
      return UL;
    }
    if (toX == urX && toY == urY) {
      return UR;
    }
    if (toX == drX && toY == drY) {
      return DR;
    }
    if (toX == dnX && toY == dnY) {
      return DN;
    }
    if (toX == dlX && toY == dlY) {
      return DL;
    }
    return 0;
  } // end determineMoveDirection()
  
  // returns numerical representation of the opposite direction
  public static int getOppositeDirection(int direction) {
    int opposite = 0;
    switch (direction) {
      case UP: opposite = DN;
      break;
      case UR: opposite = DL;
      break;
      case UL: opposite = DR;
      break;
      case DR: opposite = UL;
      break;
      case DN: opposite = UP;
      break;
      case DL: opposite = UR;
      break;
      default: System.out.println("Cell:getOppositeDirection() - illegal arg" + direction);
      break;
    }
    return opposite;
  } // end getOppositeDirection()
  
  // retrieves neighboring agent at the indicated direction from the source
  public Agent getAgentAt(Agent sourceAgent, int direction) {
    int newX = sourceAgent.x;
    int newY = sourceAgent.y;
    switch (direction) {
        case UP: 
          newX = grid.stx(grid.upx(sourceAgent.x, sourceAgent.y));
          newY = grid.sty(grid.upy(sourceAgent.x, sourceAgent.y));
          break;
        case UR:
          newX = grid.stx(grid.urx(sourceAgent.x, sourceAgent.y));
          newY = grid.sty(grid.ury(sourceAgent.x, sourceAgent.y));
          break;
        case DR:
          newX = grid.stx(grid.drx(sourceAgent.x, sourceAgent.y));
          newY = grid.sty(grid.dry(sourceAgent.x, sourceAgent.y));
          break;
        case DN:
          newX = grid.stx(grid.downx(sourceAgent.x, sourceAgent.y));
          newY = grid.sty(grid.downy(sourceAgent.x, sourceAgent.y));
          break;
        case DL:
          newX = grid.stx(grid.dlx(sourceAgent.x, sourceAgent.y));
          newY = grid.sty(grid.dly(sourceAgent.x, sourceAgent.y));
          break;
        case UL:
          newX = grid.stx(grid.ulx(sourceAgent.x, sourceAgent.y));
          newY = grid.sty(grid.uly(sourceAgent.x, sourceAgent.y));
          break;
        default:
          break;
      }
    Agent targetAgent = (Agent) grid.get(newX, newY);
    return targetAgent;
  } // end getAgentAt()
  
  /* ======================= END MOVE & SUPPORT FUNCTIONS ================= */

}// end class AlvCell
