package GridSimM;

import sim.engine.*;
import sim.display.*;
import ec.util.*;
import java.io.*;
import java.util.*;

public class GridSimMain {
    
  public static final int EXP_MODE = 0; // 0 = GUI, 1 = batch
  public static final int SUBJ_START = 11;
  public static final int SUBJ_END = 16;
  public static final int NUM_REP = 100;
  public static final int NUM_STEPS = 20;
  public static final int DATA_COL_NUM = 6;
  
  
  static final String gridsimFolderName = "gridsim_batch"; // folder containing all simulation output
  static final String paramFolderName = "param";  // under outputFolder(sim_output/)
  static final String rawDataFolderName = "rawdata";  // under outputFolder(sim_output/)
  static final String summaryFolderName = "summary";  // under outputFolder(sim_output/)
  
  static final java.text.DecimalFormat form_0d00 = new java.text.DecimalFormat("0.00");
  static final java.text.DecimalFormat form_00 = new java.text.DecimalFormat("00");
  static final java.text.DecimalFormat form_000 = new java.text.DecimalFormat("000");
  static final java.text.DecimalFormat form_0000 = new java.text.DecimalFormat("0000");
  static final java.text.DecimalFormat form_00000 = new java.text.DecimalFormat("00000");
  
  
  public static void main (String[] args) {
    if (EXP_MODE == 0) {
      // GUI MODE
      GridSimGUI model = new GridSimGUI();
      sim.display.Console c = new sim.display.Console(model);
      c.setVisible(true); 
    }
    else if (EXP_MODE == 1) {
      // BATCH MODE
      System.out.println("batch mode");
      File simDir = new File(gridsimFolderName);
      if(!simDir.exists()){
        simDir.mkdirs();
      }
      simDir = new File(gridsimFolderName + File.separator + paramFolderName);
      if (!simDir.exists()) {
        simDir.mkdir();
      }
      simDir = new File(gridsimFolderName + File.separator + rawDataFolderName);
      if (!simDir.exists()) {
        simDir.mkdir();
      }
      simDir = new File(gridsimFolderName + File.separator + summaryFolderName);
      if (!simDir.exists()) {
        simDir.mkdir();
      }
      runParameterizedBatch();
      System.exit(0);
    }
    else {
      System.out.println("Please select experiment mode");
      System.exit(0);
    }
  } // end main()
    
  public static void runParameterizedBatch() {
    ParameterDatabase parameters = null;
    String paramFileName = null;
    String outFileName = null;
    String imgFileName = null;
    for (int idx = SUBJ_START; idx <= SUBJ_END; idx++) {
      System.out.println("SUBJECT # " + idx);
      paramFileName = gridsimFolderName + File.separator + paramFolderName + File.separator + "Param_Subj_" + form_00.format(idx) + ".txt";
      try {
        parameters = new ParameterDatabase(new File(paramFileName).getAbsoluteFile());
      } 
      catch(Exception e) {
       // e.printStackTrace();
        System.out.println("Error: runParameterizedBatch --> failed to read parameter file: " + paramFileName);
        //continue;
      }
      outFileName = "Output_Subj_" + form_00.format(idx);
      runSimulation(parameters, outFileName);
      processRawData(outFileName);
    }
  } // end runParameterizedBatch()
  
  public static void runSimulation(ParameterDatabase paramDB, String outFileName) {
    GridSimM.GridSim model = null;
    String outputName = null;
    try {
      for (int rep = 1; rep <= NUM_REP; rep++) {
        if (rep == 1 || rep%25 == 0) {
          System.out.println("\tREPETITION " + rep);
        }
        outputName = gridsimFolderName + File.separator + rawDataFolderName + File.separator + outFileName + "_Rep" + form_000.format(rep) + ".txt";
        model = new GridSimM.GridSim(new MersenneTwisterFast(), new Schedule(), paramDB, outputName);
        model.start();
        for (int i = 0; i < NUM_STEPS; i++) {
          model.schedule.step(model);
        }
        model.finish();
      }
    }
    catch (Exception e) { e.printStackTrace(); }
    
  } // end runSimulation()
  
  public static void processRawData(String outFileName) { 
    double[][] dataArray = new double[DATA_COL_NUM][NUM_STEPS];
    double[][] varArray = new double[DATA_COL_NUM][NUM_STEPS];
    for (int i=0; i < DATA_COL_NUM; i++) {
      for (int j=0; j < NUM_STEPS; j++) {
        dataArray[i][j] = 0.0;
        varArray[i][j] = 0.0;
      }
    }
    // for each repetition, extract data and add to placeholder
    for (int rep=1 ;rep <= NUM_REP; rep++) {
      String dataFileName = gridsimFolderName + File.separator + rawDataFolderName + File.separator + outFileName + "_Rep" + form_000.format(rep) + ".txt";
      Scanner dataScan = null;
      try {
        dataScan = new Scanner(new BufferedReader(new FileReader(dataFileName)));
        int i = 0;
        int j = 0;
        while (dataScan.hasNext()) {
          if (j == NUM_STEPS) { break; }
          dataArray[i][j] += dataScan.nextDouble();
          i++;
          if (i == DATA_COL_NUM) { i = 0; j++; }
        }
      }
      catch (IOException e){ e.printStackTrace(); }
      if (dataScan!= null) { dataScan.close(); }
    }
    String avgFileName = gridsimFolderName + File.separator + summaryFolderName + File.separator + outFileName + "_Avg.txt";
    BufferedWriter outFile  = null;
    try {
      outFile = new BufferedWriter(new FileWriter(avgFileName));
      for (int j = 0; j < NUM_STEPS; j++) {
        for (int i = 0; i < DATA_COL_NUM; i++) {
          dataArray[i][j] = dataArray[i][j] / ((double)NUM_REP); // average
          outFile.write(dataArray[i][j] + "\t");
        }
        outFile.newLine();
      }
      if (outFile != null) { outFile.close(); }
    }
    catch (IOException e) { e.printStackTrace(); }
    
    // compute variance
    for (int rep=1 ;rep <= NUM_REP; rep++) {
      String dataFileName = gridsimFolderName + File.separator + rawDataFolderName + File.separator + outFileName + "_Rep" + form_000.format(rep) + ".txt";
      Scanner dataScan = null;
      try {
        dataScan = new Scanner(new BufferedReader(new FileReader(dataFileName)));
        int i = 0;
        int j = 0;
        while (dataScan.hasNext()) {
          if (j == NUM_STEPS) { break; }
          double curVal = dataScan.nextDouble();
          varArray[i][j] += (dataArray[i][j] - curVal) * (dataArray[i][j] - curVal);
          i++;
          if (i == DATA_COL_NUM) { i = 0; j++; }
        }
      }
      catch (IOException e){ e.printStackTrace(); }
      if (dataScan!= null) { dataScan.close(); }
    }
    String varFileName = gridsimFolderName + File.separator + summaryFolderName + File.separator + outFileName + "_Var.txt";
    try {
      outFile = new BufferedWriter(new FileWriter(varFileName));
      for (int j = 0; j < NUM_STEPS; j++) {
        for (int i = 0; i < DATA_COL_NUM; i++) {
          varArray[i][j] = varArray[i][j] / ((double)NUM_REP); // variance
          outFile.write(varArray[i][j] + "\t");
        }
        outFile.newLine();
      }
      if (outFile != null) { outFile.close(); }
    }
    catch (IOException e) { e.printStackTrace(); }
    
  }

} // end class GridSimMain

