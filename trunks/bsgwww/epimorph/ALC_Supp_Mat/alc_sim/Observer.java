package alc_sim;

import java.io.*;
import sim.engine.SimState;
import sim.engine.Steppable;
import sim.field.grid.ObjectGrid2D;
import sim.util.*;
import sim.util.Bag;

public class Observer extends Agent {
  
  public static final int totalInfoNum = 34; /* ***** MUST BE THE SAME AS IN ExpManager ***** */
  
  private int curStep = 0;
  private Culture sourceCulture = null;
  int totalCellNumber = 0;
  String outputFileClusterInfo = null;
  BufferedWriter outWriterClusterInfo = null;
  String outputFileRuleUsageInfo = null;
  BufferedWriter outWriterRuleUsageInfo = null;
  
  Bag clusterNoLumenRecords = new Bag();
  Bag clusterLumenRecords = new Bag();
  Bag cellTotalDistanceMovedRecords = new Bag();
  Bag morphDisruptionRecords = new Bag();
  
  /** Creates a new instance of Observer */
  public Observer(int x, int y, ObjectGrid2D grid, Culture culture, String outFileName) {
    super(x, y, grid);
    sourceCulture = culture;
    outputFileClusterInfo = outFileName + "_cluster.txt";
    outputFileRuleUsageInfo = outFileName + "_rules.txt";
    try {
      outWriterClusterInfo = new BufferedWriter(new FileWriter(outputFileClusterInfo, false));
      outWriterRuleUsageInfo = new BufferedWriter(new FileWriter(outputFileRuleUsageInfo, false));
    }
    catch (IOException e) {
      System.out.println("ERROR 100: can't open outWriter");
    }
  }
  
  public void setTotalCellNumber (int total) {
    totalCellNumber = total;
  }
  
  public void step(SimState state) {
    Culture parentCulture = (Culture)state;
    // make records of total distance moved by cells
    int totalDistanceMoved = 0;
    Bag cellBag = sourceCulture.getCellBag();
    for (int i = 0; i < cellBag.numObjs; i++) {
      totalDistanceMoved += ((AlvCell)cellBag.get(i)).getDistanceMovedTotal();
    }
    if (cellBag.numObjs > 0) {
      cellTotalDistanceMovedRecords.add(new Int2D(curStep, totalDistanceMoved / cellBag.numObjs));
    }
    // make records of the number of clusters with and without lumens
    Bag clusterBag = new Bag();
    for (int i = 0; i < grid.getWidth(); i++) {
      for (int j = 0; j < grid.getHeight(); j++) {
        Agent agent = (Agent) grid.get(i, j);
        if (agent instanceof AlvCell) {
          AlvCell curCell = (AlvCell) agent;
          Cluster curCluster = curCell.getCluster();
          if (curCluster != null && !clusterBag.contains(curCluster)) {
            clusterBag.add(curCluster);
          }
        }
      }
    }
    int numClusterLumen = 0;
    int numClusterNoLumen = 0;
    for (int i = 0; i < clusterBag.numObjs; i++) {
      Cluster curCluster = (Cluster) clusterBag.get(i);
      int lumenSize = curCluster.getLumenSize();
      if (lumenSize > 0) {
        numClusterLumen++;
      }
      else {
        numClusterNoLumen++;
      }
    }
    clusterLumenRecords.add(new Int2D(curStep, numClusterLumen));
    clusterNoLumenRecords.add(new Int2D(curStep, numClusterNoLumen));

    // morphology analysis
    morphDisruptionRecords.add(new Double2D(curStep, parentCulture.determineMorphDisruptionLevel()));
    
    curStep++;
  } // end step()
  
  public void finish() {
    try {
      if (outWriterClusterInfo != null) {
        outWriterClusterInfo.close();
      }
      if (outWriterRuleUsageInfo != null) {
        outWriterRuleUsageInfo.close();
      }
    }
    catch (IOException e) {
      System.out.println("ERROR 101: can't close outWriter");
    }
  }
  
  public int totalCellCount() {
    int cellCount = 0;
    for (int i = 0; i < grid.getWidth(); i++) {
      for (int j = 0; j < grid.getHeight(); j++) {
        Agent agent = (Agent) grid.get(i, j);
	if (agent instanceof AlvCell) {
          cellCount++;
	}
      }
    }
    return cellCount;
  }
  
  /* *** For comparison with 3D version *** */
  public void printSummaryComp() {
    try {
      if (outputFileClusterInfo != null && outWriterClusterInfo != null) {
        // write out the number of clusters and their sizes
        Bag isolatedCellBag = new Bag();
        Bag clusterBag = new Bag();
        for (int i = 0; i < grid.getWidth(); i++) {
          for (int j = 0; j < grid.getHeight(); j++) {
            Agent agent = (Agent) grid.get(i, j);
            if (agent instanceof AlvCell) {
              AlvCell curCell = (AlvCell) agent;
              Cluster curCluster = curCell.getCluster();
              if (curCluster != null && !clusterBag.contains(curCluster)) {
                clusterBag.add(curCluster);
              }
              if (curCluster == null && !isolatedCellBag.contains(curCell)) {
                isolatedCellBag.add(curCell);
              }
            }
          }
        }
        int clusteredCellCount = 0;
        int totalCellCount = isolatedCellBag.numObjs;
        int cellNum;
        for (int i = 0; i < clusterBag.numObjs; i++) {
          Cluster curCluster = (Cluster) clusterBag.get(i);
          cellNum = curCluster.getTotalCellNum();
          clusteredCellCount += cellNum;
          totalCellCount += cellNum;
          if (cellNum > 0) {
            outWriterClusterInfo.write(cellNum + " ");
            outWriterClusterInfo.newLine();
          }
        }
        for (int i = 0; i < isolatedCellBag.numObjs; i++) {
          outWriterClusterInfo.write(1 + " ");
          outWriterClusterInfo.newLine();
        }
        System.out.println("*** Total Cell Count: " + totalCellCount);
        System.out.println("*** Number of Isolated Cells: " + isolatedCellBag.numObjs);
        System.out.println("*** Number of Clustered Cells: " + clusteredCellCount);
        System.out.println("*** Number of Clusters: " + clusterBag.numObjs);
        System.out.println("*** Average Cluster Size: " + (clusteredCellCount / clusterBag.numObjs));
      }
    }
    catch (IOException e) {
      System.out.println("ERROR 102: can't print to output file");
    }
  } // end printSummaryComp()
  
  public void printSummary() {
    try {
      if (outputFileClusterInfo != null && outWriterClusterInfo != null) {
        // write out the number of clusters and their sizes
        Bag clusterBag = new Bag();
        for (int i = 0; i < grid.getWidth(); i++) {
          for (int j = 0; j < grid.getHeight(); j++) {
            Agent agent = (Agent) grid.get(i, j);
            if (agent instanceof AlvCell) {
              AlvCell curCell = (AlvCell) agent;
              Cluster curCluster = curCell.getCluster();
              if (curCluster != null && !clusterBag.contains(curCluster)) {
                clusterBag.add(curCluster);
              }
            }
          }
        }
        for (int i = 0; i < clusterBag.numObjs; i++) {
          Cluster curCluster = (Cluster) clusterBag.get(i);
          double lumenSize = (double) curCluster.getLumenSize();
          if (lumenSize > 0) {
            // write out cluster diameter (lumen diameter + 2 for cell layer)
            double diameter = 2.0 + 2.0 * java.lang.Math.sqrt(lumenSize / java.lang.Math.PI);
            outWriterClusterInfo.write(diameter + " ");
            outWriterClusterInfo.newLine();
          }
          else {
            outWriterClusterInfo.write("0");
            outWriterClusterInfo.newLine();
          }
        }
      }
     // outputFileRuleUsageInfo = null;
      if (outputFileRuleUsageInfo != null && outWriterRuleUsageInfo != null) {
        // write out rule usage pattern over time
        // determine the final simulation time step
        int maxTime = 0;
        for (int i = 0; i < morphDisruptionRecords.numObjs; i++) {
          Double2D curDbl = (Double2D) morphDisruptionRecords.get(i);
          if (((int)curDbl.x > maxTime)) {
            maxTime = (int)curDbl.x;
          }
        }
        for (int i = 0; i < clusterLumenRecords.numObjs; i++) {
          Int2D curInt = (Int2D) clusterLumenRecords.get(i);
          if (curInt.x > maxTime) {
            maxTime = curInt.x;
          }
        }
        for (int i = 0; i < clusterNoLumenRecords.numObjs; i++) {
          Int2D curInt = (Int2D) clusterNoLumenRecords.get(i);
          if (curInt.x > maxTime) {
            maxTime = curInt.x;
          }
        }
        Bag cellBag = sourceCulture.getCellBag();
        for (int i = 0; i < cellBag.numObjs; i++) {
          AlvCell curCell = (AlvCell) cellBag.get(i);
          Bag curRecords = curCell.getRuleUsageRecords();
          for (int j = 0; j < curRecords.numObjs; j++) {
            Int2D curInt = (Int2D) curRecords.get(j);
            if (curInt.x > maxTime) {
              maxTime = curInt.x;
            }
          }
        }
        // Initialize usage info array [time step] [# of info]
        double[][] ruleUsage = new double[maxTime+1][totalInfoNum];
        for (int i = 0; i <= maxTime; i++) {
          for (int j = 0; j < totalInfoNum; j++) {
            ruleUsage[i][j] = 0;
          }
        }
        for (int i = 0; i < cellBag.numObjs; i++) {
          AlvCell curCell = (AlvCell) cellBag.get(i);
          Bag curRecords = curCell.getRuleUsageRecords();
          for (int j = 0; j < curRecords.numObjs; j++) {
            Int2D curInt = (Int2D) curRecords.get(j);
            if (curInt.x <= maxTime && curInt.y < totalInfoNum) {
              ruleUsage[curInt.x][curInt.y]++;
            }
          }
        }
        for (int i = 0; i < morphDisruptionRecords.numObjs; i++) {
          Double2D curDbl = (Double2D) morphDisruptionRecords.get(i);
          ruleUsage[(int)curDbl.x][30] = curDbl.y;
        }
        for (int i = 0; i < cellTotalDistanceMovedRecords.numObjs; i++) {
          Int2D curInt = (Int2D) cellTotalDistanceMovedRecords.get(i);
          ruleUsage[curInt.x][31] = curInt.y;
        }
        for (int i = 0; i < clusterNoLumenRecords.numObjs; i++) {
          Int2D curInt = (Int2D) clusterNoLumenRecords.get(i);
          ruleUsage[curInt.x][32] = curInt.y;
        }
        for (int i = 0; i < clusterLumenRecords.numObjs; i++) {
          Int2D curInt = (Int2D) clusterLumenRecords.get(i);
          ruleUsage[curInt.x][33] = curInt.y;
        }
        // Write out data out to the output file
        for (int i = 0; i <= maxTime; i++) {
          for (int j = 0; j < totalInfoNum; j++) {
            outWriterRuleUsageInfo.write(ruleUsage[i][j] + "\t");
          }
          outWriterRuleUsageInfo.newLine();
        }
      }
    }
    catch (IOException e) {
      System.out.println("ERROR 102: can't print to output file");
    }
  } // end printSummary()
  
}
