package alc_sim;

import java.io.*;
import java.awt.Color;
import java.awt.image.BufferedImage;
import ec.util.*;
import sim.engine.*;
import sim.util.*;
import sim.field.grid.*;

public class Culture extends SimState {
  
  ObjectGrid2D cultureGrid = null; // main culture grid
  DoubleGrid2D diffusionGrid = null; // grid for diffusion
  DoubleGrid2D diffusionGridTmp = null; // grid for diffusion
  Observer dataCollector = null;
  Bag cellBag = new Bag();
  File outputFile = null;
  String outputFileName = null;
  ParameterDatabase paramDB = null;
  
  // culture-related variables
  public int migration_mode = 2; // 1->diffusion; 2->cell-density; else->random
  public int initial_cell_count = 500;
  public int grid_width = 100;
  public int grid_height = 100;
  
  // diffusion-related variables
  public int diffusion_step_multiples = 25;
  public double evaporation_rate = 0.05;
  public double diffusion_rate = 0.9;
  public double max_solute_concentration = 50000;
  public double min_solute_output = 3000;
  public double max_solute_output = 8000;
  
  // wound setup variables
  public int wound_size = 1;
  public boolean wound_type_contig = true;

  // variables shared by cells and clusters in the model
  Bag neighborBag = new Bag();
  DoubleBag neighborDoubleBag = new DoubleBag();
  IntBag neighborX = new IntBag();
  IntBag neighborY = new IntBag();
  
  /* ================== PARAMETERIZATION FUNCTIONS ======================== */
  public int getMigrationMode() { return migration_mode; }
  public void setMigrationMode(int mode) { migration_mode = mode; }
  public int getGridWidth() { return grid_width; }
  public void setGridWidth(int width) { if(width > 0 && width%2 == 0) grid_width = width; }
  public int getGridHeight() { return grid_height; }
  public void setGridHeight(int height) { if(height > 0 && height%2 == 0) grid_height = height; }
  public int getInitialCellCount() { return initial_cell_count; }
  public void setInitialCellCount(int count) { if(count >= 0) initial_cell_count = count; }
  public double getEvaporationRate() { return evaporation_rate; }
  public void setEvaporationRate(double rate) { if(rate >= 0.0) evaporation_rate = rate; }
  public double getDiffusionRate() { return diffusion_rate; }
  public void setDiffusionRate(double rate) { if(rate >= 0.0) diffusion_rate = rate; }
  public double getMaxSoluteConcentration() { return max_solute_concentration; }
  public void setMaxSoluteConcentration(double max) { if(max >= 0.0) max_solute_concentration = max; }
  public double getMinSoluteOutput() { return min_solute_output; }
  public void setMinSoluteOutput(double min) { if(min >= 0.0) min_solute_output = min; }
  public double getMaxSoluteOutput() { return max_solute_output; }
  public void getMaxSoluteOutput(double max) { if(max >= 0.0) max_solute_output = max; }
  
  public void setWoundSize(int size) { wound_size = size; }
  public void setWoundTypeContig(boolean type) { wound_type_contig = type; }
  
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
  
  public Culture() {
    super(new MersenneTwisterFast(System.currentTimeMillis()), new Schedule());
  }
    
  public Culture(int cellCount, int mode) {
    super(new MersenneTwisterFast(System.currentTimeMillis()), new Schedule());
    initial_cell_count = cellCount;
    migration_mode = mode;
  }
  
  public Culture(long seed) {
    super(new MersenneTwisterFast(seed), new Schedule());
  }
  
  public Culture(MersenneTwisterFast random, Schedule schedule) {
    super(random, schedule);
  }
  
  public Culture(MersenneTwisterFast random, Schedule schedule, ParameterDatabase params) {
    super(random, schedule);
    paramDB = params;
    loadParams();
  }
  
  public Culture(MersenneTwisterFast random, Schedule schedule, ParameterDatabase params, String fileName) {
    super(random, schedule);
    paramDB = params;
    outputFileName = fileName;
    loadParams();
  }
  
  public ObjectGrid2D getCultureGrid() {
    return cultureGrid;
  }
  
  public Bag getCellBag() { return cellBag; }
  
  public void printGraphicsInfo(String outputFileName) {
    try {
      BufferedWriter out = new BufferedWriter(new FileWriter(outputFileName, false));  
      int gridWidth = cultureGrid.getWidth();
      int gridHeight = cultureGrid.getHeight();
      for (int i = 0; i < gridWidth; i++) {
        for (int j = 0; j < gridHeight; j++) {
          Agent agent = (Agent) cultureGrid.get(i, j);
          if (agent instanceof AlvCell) {
            out.write(i + "," + j + "," + 1);
            out.newLine();
          }
          else if (agent instanceof Matrix) {
            out.write(i + "," + j + "," + 2);
            out.newLine();
          }
          else if (agent instanceof FreeSpace) {
            out.write(i + "," + j + "," + 3);
            out.newLine();
          }
        }
      }
      out.close();
    }
    catch (IOException e) { e.printStackTrace(); }
  } // end printGraphicsInfo()
  
  public void start() {
    super.start(); // clear out the schedule
    // set up the main culture grid
    /*
    System.out.println("*** Grid Size: " + grid_width + " x " + grid_height);
    System.out.println("*** AlvCell Count: " + initial_cell_count);
    */
    cultureGrid = new ObjectGrid2D(grid_width, grid_height);
    for (int i = 0; i < grid_width; i++) {
      for (int j = 0; j < grid_height; j++) {
        cultureGrid.set(i, j, new Matrix (i, j, cultureGrid));
      }
    }
    cellBag = new Bag();
    
    // add cells to culture grid
    
    // RANDOM PLACEMENT OF CELLS
    
    for (int i = 0; i < initial_cell_count; i++) {
      int xLoc = random.nextInt(grid_width);
      int yLoc = random.nextInt(grid_height);
      // make sure the location is not occupied by another cell
      Agent agent = (Agent) cultureGrid.get(xLoc, yLoc);
      while ( !(agent == null) && (agent instanceof Cell || agent instanceof FreeSpace)) {
        xLoc = random.nextInt(grid_width);
        yLoc = random.nextInt(grid_height);
        agent = (Agent) cultureGrid.get(xLoc, yLoc);
      }
      AlvCell alvCell = new AlvCell(xLoc, yLoc, cultureGrid, random.nextInt(), paramDB);
      cultureGrid.set(xLoc, yLoc, alvCell);
      cellBag.add(alvCell);
      alvCell.setMigrationMode(migration_mode);
      if (migration_mode == 1) {
        double soluteOutput = min_solute_output + random.nextDouble() * (max_solute_output - min_solute_output);
        alvCell.setSoluteProperties(soluteOutput, max_solute_concentration);
      }
      schedule.scheduleOnce(alvCell);
    }
    
    
    if (migration_mode == 1) {
      // set up diffusion-related items
      diffusionGrid = new DoubleGrid2D(grid_width, grid_height, 0.0);
      diffusionGridTmp = new DoubleGrid2D(grid_width, grid_height, 0.0);
      Diffuser diffuser = new Diffuser(diffusionGrid,diffusionGridTmp,evaporation_rate,diffusion_rate);
      MultiStep multipleSteps = new MultiStep(diffuser, diffusion_step_multiples, false);
      schedule.scheduleRepeating(multipleSteps);
    }
    
    dataCollector = new Observer(0,0,cultureGrid, this, outputFileName);
    dataCollector.setTotalCellNumber(initial_cell_count);
    schedule.scheduleRepeating(dataCollector);
    
    // System.out.println("Culture :: start() done");
  } // end start()
  
  public void finish() {
    dataCollector.printSummary();
    dataCollector.finish();
    super.finish();
    // System.out.println("Culture :: finish() done");
  } // end finish()
    
  public double determineMorphDisruptionLevel() {
    double maxCellPercent = 0.15;
    double maxLumenPercent = 0.15;
    double cellLevelMax = 1.5;
    double lumenLevelMax = 1.5;
    double shapeLevelMax = 1.0;
    
    /* ======= Local Analysis ======= */
    Bag cellBag = new Bag();
    Bag lumenBag = new Bag();
    Bag matrixBag = new Bag();
    Agent curAgent = null;
    double localLevel = 0.0;
    for (int i = 0; i < grid_width; i++) {
      for (int j = 0; j < grid_height; j++) {
        curAgent = (Agent) cultureGrid.get(i, j);
        if (curAgent instanceof AlvCell) {
          cellBag.add(curAgent);
          localLevel += ((AlvCell)curAgent).determineSurroundingMorphology();
        }
        else if (curAgent instanceof FreeSpace) {
          lumenBag.add(curAgent);
        }
        else if (curAgent instanceof Matrix) {
          matrixBag.add(curAgent);
        }
      }
    }
    if (cellBag.numObjs != 0) {
      localLevel = localLevel / (double)cellBag.numObjs;  // average the total (should be <= 3.0)
    }
    
    /* ======= Global Analysis ======= */
    
    Bag neighbors = new Bag();
    IntBag xPositions = new IntBag();
    IntBag yPositions = new IntBag();
    
    // ANALYSIS OF CELLULAR CONTINUITY
    int numCellGroups = 0;
    Object[] cellBagArray = cellBag.toArray();
    java.util.LinkedList adjacentCellList[] = new java.util.LinkedList[cellBagArray.length];
    boolean[] visitedCells = new boolean[cellBagArray.length];
    
    // Create an adjacency list for cells
    for (int curCellId = 0; curCellId < cellBagArray.length; curCellId++) {
      adjacentCellList[curCellId] = new java.util.LinkedList();
      Cell curCell = (Cell) cellBagArray[curCellId];
      cultureGrid.getNeighborsHexagonalDistance
              (curCell.x, curCell.y, 1, true, neighbors, xPositions, yPositions);
      neighbors.remove(curCell);
      for (int ndx = 0; ndx < neighbors.numObjs; ndx++) {
        Agent adjacentAgent = (Agent) neighbors.get(ndx);
        if (adjacentAgent instanceof Cell) {
          for (int adjCellId = 0; adjCellId < cellBagArray.length; adjCellId++) {
            if (adjacentAgent == cellBagArray[adjCellId]) {
              adjacentCellList[curCellId].add(new Integer(adjCellId));
              break;
            }
          }
        }
      }
    }
    // Run depth-first search to identify the number of strongly connected groups of cells
    for (int i = 0; i < cellBagArray.length; i++) {
      visitedCells[i] = false;
    }
    for (int i = 0; i < cellBagArray.length; i++) {
      if (visitedCells[i] == false) {
        numCellGroups++; // beginning iteration of a strongly connected group
        dfsRun(i, visitedCells, adjacentCellList);
      }
    }
    // Evaluate morphological disruption of cells at global level
    double maxCellGrpNum = cellBagArray.length * maxCellPercent;
    double minCellGrpNum = 1.0; // number of strongly connected groups when normal (2 for normal overlay)
    if (minCellGrpNum + 2.0 > maxCellGrpNum) {
      maxCellGrpNum = minCellGrpNum + 2.0;
    }
    double cellLevel = 0.0;
    if (numCellGroups > maxCellGrpNum) {
      cellLevel = cellLevelMax;
    }
    else if (numCellGroups > minCellGrpNum) {
      cellLevel = (cellLevelMax / (maxCellGrpNum - minCellGrpNum)) * (numCellGroups - minCellGrpNum);
    }
    
    // ANALYSIS OF LUMINAL CONTINUITY
    int numLumenGroups = 0;
    Object[] lumenBagArray = lumenBag.toArray();
    java.util.LinkedList adjacentLumenList[] = new java.util.LinkedList[lumenBagArray.length];
    boolean[] visitedLumens = new boolean[lumenBagArray.length];
    
    // Create adjacent lumen list
    for (int curLumenId = 0; curLumenId < lumenBagArray.length; curLumenId++) {
      adjacentLumenList[curLumenId] = new java.util.LinkedList();
      FreeSpace curLumen = (FreeSpace) lumenBagArray[curLumenId];
      cultureGrid.getNeighborsHexagonalDistance
              (curLumen.x, curLumen.y, 1, true, neighbors, xPositions, yPositions);
      neighbors.remove(curLumen);
      for (int ndx = 0; ndx < neighbors.numObjs; ndx++) {
        Agent adjacentAgent = (Agent) neighbors.get(ndx);
        if (adjacentAgent instanceof FreeSpace) {
          for (int adjLumenId = 0; adjLumenId < lumenBagArray.length; adjLumenId++) {
            if (adjacentAgent == lumenBagArray[adjLumenId]) {
              adjacentLumenList[curLumenId].add(new Integer(adjLumenId));
              break;
            }
          }
        }
      }
    }
    // Run depth-first search to identify the number of strongly connected groups of lumen
    for (int i = 0; i < lumenBagArray.length; i++) {
      visitedLumens[i] = false;
    }
    for (int i = 0; i < lumenBagArray.length; i++) {
      if (visitedLumens[i] == false) {
        numLumenGroups++; // beginning iteration of a strongly connected group
        dfsRun(i, visitedLumens, adjacentLumenList);
      }
    }
    // Evaluate global morphological disruption of lumens
    double maxLumenGrpNum = lumenBagArray.length * maxLumenPercent;
    double minLumenGrpNum = 1.0; // number of strongly connected groups when normal
    if (minLumenGrpNum + 2.0 > maxLumenGrpNum) {
      maxLumenGrpNum = minLumenGrpNum + 2.0;
    }
    double lumenLevel = 0.0;
    if (numLumenGroups > maxLumenGrpNum) {
      lumenLevel = lumenLevelMax;
    }
    else if (numLumenGroups > minLumenGrpNum) {
      lumenLevel = (lumenLevelMax / (maxLumenGrpNum - minLumenGrpNum)) * (numLumenGroups - minLumenGrpNum);
    }
    
    // SHAPE ANALYSIS
    double shapeLevel = 0.0;
    double structureArea = 0.0;
    double enclosingArea = 0.0;
    int minCoordX = grid_width;
    int maxCoordX = 0;
    int minCoordY = grid_height;
    int maxCoordY = 0;
    for (int i = 0; i < cellBag.numObjs; i++) {
      curAgent = (Agent) cellBag.get(i);
      if (curAgent.x < minCoordX) { minCoordX = curAgent.x; }
      if (curAgent.x > maxCoordX) { maxCoordX = curAgent.x; }
      if (curAgent.y < minCoordY) { minCoordY = curAgent.y; }
      if (curAgent.y > maxCoordY) { maxCoordY = curAgent.y; }
    }
    double lengthX = maxCoordX - minCoordX + 1.0;
    double lengthY = maxCoordX - minCoordY + 1.0;
    double hexTocir = 2.0 * java.lang.Math.sqrt(3.0) / java.lang.Math.PI;
    if (lengthX > 0 && lengthY > 0) {
      structureArea = cellBag.numObjs + lumenBag.numObjs;
      enclosingArea = java.lang.Math.PI * (lengthX / 2.0) * (lengthY / 2.0) * hexTocir;
      shapeLevel = java.lang.Math.abs(1.0 - (structureArea / enclosingArea));
      if (shapeLevel > shapeLevelMax) { shapeLevel = shapeLevelMax; }
    }
    
    // Return the overall level of morphological disruption
    return localLevel + cellLevel + lumenLevel + shapeLevel;
    
  } // end determineMorphDisruptionLevel()
  
  // recursive depth-first search
  public static void dfsRun(int nodeId, boolean[] visited, java.util.LinkedList[] adjacencyList) {
    visited[nodeId] = true;
    java.util.ListIterator adjacencyIterator = adjacencyList[nodeId].listIterator(0);
    while (adjacencyIterator.hasNext()) {
      int adjacentNodeId = ((Integer)adjacencyIterator.next()).intValue();
      if (! visited[adjacentNodeId]) {
        dfsRun(adjacentNodeId, visited, adjacencyList);
      }
    }
  } // end dfsRun()
  
} // end class Culture
