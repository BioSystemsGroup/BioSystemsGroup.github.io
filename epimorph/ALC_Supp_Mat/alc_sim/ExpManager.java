package alc_sim;

import java.io.*;
import java.util.*;
import java.text.DecimalFormat;
import sim.engine.*;
import sim.display.*;
import sim.util.Properties;
import sim.util.media.PngEncoder;
import ec.util.*;
import jxl.*;
import jxl.write.*;

public class ExpManager {
  
  // 1 = GUI;
  // 2 = BATCH (basic var densities); 3 = BATCH (+ var adhesion); 4 = BATCH (+ var mig mode);
  // 5 = BATCH (+ var mig speed, adhesion, mig mode); 6 = BATCH (var wound size)
  // else = DEFAULT (NO GUI)
  public int experiment_mode = 1;
  public boolean image_capture = false;
  public boolean image_info_out = false;
  public boolean movie_capture = false;
  public boolean movie_info_out = false;
  public boolean morph_level_out = true;
  public boolean diffusion_graphics = true; // true->add diffusion graphics
  
  // Parameters to set up NON-BATCH simulation (GUI, DEFAULT)
  public static int grid_width = 100;
  public static int grid_height = 100;
  public static int initial_cell_count = 1500;
  public static int simulation_steps = 100;
  public static int migration_mode = 2;
  
  // parameters to setup BATCH experiments
  public static int exp_migration_mode = 0; // 1->diffusion; 2->cell-density; else->random
  public static int exp_numMigrationMode = 3;
  public static int exp_repetitions = 3;
  public static int exp_steps = 5;
  public static int exp_gridWidth = 100;
  public static int exp_gridHeight = 100;
  public static int exp_initDensity = 1000;
  public static int exp_finalDensity = 1000;
  public static int exp_densityInterval = 1000;
  public static double exp_cellUnattInit = 0.0;
  public static double exp_cellUnattFin = 1.0;
  public static double exp_cellUnattIntv = .5;
  public static double exp_cellAttInit = 0.0;
  public static double exp_cellAttFin = 1.0;
  public static double exp_cellAttIntv = .5;
  public static double exp_ceMigSpeedInit = 0.0;
  public static double exp_ceMigSpeedFin = 1.0;
  public static double exp_ceMigSpeedIntv = 0.2;
  public static double exp_clMigSpeedInit = 0.0;
  public static double exp_clMigSpeedFin = 1.0;
  public static double exp_clMigSpeedIntv = 0.2;
  public static int exp_initWoundSize = 1;
  public static int exp_finalWoundSize = 5;
  public static int exp_woundSizeInterval = 2;
  public static int exp_totalRuleNum = 34;  /* ***** MUST BE THE SAME AS IN Observer ***** */
  
  public static final String P_WIDTH = "GridWidth";
  public static final String P_HEIGHT = "GridHeight";
  public static final DecimalFormat form_0d00 = new DecimalFormat("0.00");
  public static final DecimalFormat form_00 = new DecimalFormat("00");
  public static final DecimalFormat form_000 = new DecimalFormat("000");
  public static final DecimalFormat form_0000 = new DecimalFormat("0000");
  public static final DecimalFormat form_00000 = new DecimalFormat("00000");

  File outputFile = null;
  String outputFilePath = null;
  ParameterDatabase paramDB;
  String outputFolderName = "sim_output";
  String paramFolderName = "params";  // under outputFolder(sim_output/)
  String rawDataFolderName = "rawdata";  // under outputFolder(sim_output/)
  String summaryFolderName = "summary";  // under outputFolder(sim_output/)
  String imageFolderName = "images";  // under outputFolder(sim_output/)
  String excelFolderName = "excel"; // under outputFolder(sim_output/)
  String srcFolderName = "src"; // under outputFolder(sim_output/)
  
  /* ================= PARAMETERIZATION FUNCTIONS ========================= */
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
  
  /* ================ END PARAMETERIZATION FUNCTIONS ====================== */
  
  public ExpManager() {
  }
  
  public ExpManager(ParameterDatabase params) {
    paramDB = params;
    loadParams();
  }
  
  public void run() {
    System.out.println("ExpManager :: run() called");
    // create folders for simulation outputs
    File outputFolderFile = new File(outputFolderName);
    outputFolderFile.mkdir();
    File paramFolderFile = new File(outputFolderName + File.separator + paramFolderName);
    paramFolderFile.mkdir();
    File rawDataFolderFile = new File(outputFolderName + File.separator + rawDataFolderName);
    rawDataFolderFile.mkdirs();
    File summaryFolderFile = new File(outputFolderName + File.separator + summaryFolderName);
    summaryFolderFile.mkdirs();
    File imageFolderFile = new File(outputFolderName + File.separator + imageFolderName);
    imageFolderFile.mkdirs();
    File srcFolderFile = new File(outputFolderName + File.separator + srcFolderName);
    srcFolderFile.mkdirs();
    
    if (experiment_mode == 1) {
      // control by GUI
      CultureGUI guiCulture = new CultureGUI(initial_cell_count, migration_mode);
      sim.display.Console mainConsole = new sim.display.Console(guiCulture);
      mainConsole.setVisible(true);
    }
    else if (experiment_mode > 1 && experiment_mode <= 6) {
      // culture setups controlled by parameter files

      // this.copySourceCode();
      this.createParameterFiles(experiment_mode);
      this.runParameterizedBatch(experiment_mode);
      this.processRawData(experiment_mode);
      //this.transferClusterSummaryToExcel(experiment_mode);
      System.out.println("ExpManager :: run() done");
      System.exit(0);
    }
    else {
      // default
      CultureGUI modelUI = null;
      sim.display.Console modelConsole = null;
      String imgFileName = null;
      if (image_capture || movie_capture) {
        modelUI = new CultureGUI();
        modelConsole = new sim.display.Console(modelUI);
        modelUI.diffusionGraphics = this.diffusion_graphics;
        modelUI.init(modelConsole);
      }
      alc_sim.Culture model = new alc_sim.Culture(exp_migration_mode);
      model.setGridWidth(grid_width);
      model.setGridHeight(grid_height);
      model.setInitialCellCount(initial_cell_count);
      model.setMigrationMode(migration_mode);
      model.start();
      for (int stepNum = 0; stepNum < simulation_steps; stepNum++) {
        if (stepNum%25 == 0) {
          System.out.println("STEP " + stepNum);
        }
        if (movie_info_out) {
          imgFileName = outputFolderName + File.separator + imageFolderName + File.separator
                        + "img_step" + form_000.format(stepNum) + ".txt";
          model.printGraphicsInfo(imgFileName);
        }
        if (movie_capture) {
          imgFileName = outputFolderName + File.separator + imageFolderName + File.separator
                                     + "img_step" + form_000.format(stepNum) + ".png";
          modelUI.captureImage(model, imgFileName);
        }
        model.schedule.step(model);
      }
      model.finish();
      if (image_info_out || movie_info_out) {
        // print image info after the final step
        imgFileName = outputFolderName + File.separator + imageFolderName + File.separator
                      + "img_step" + form_000.format(simulation_steps) + ".txt";
        model.printGraphicsInfo(imgFileName);
      }
      if (image_capture || movie_capture) {
        imgFileName = outputFolderName + File.separator + imageFolderName + File.separator
                      + "img_step" + form_000.format(simulation_steps) + ".png";
        modelUI.captureImage(model, imgFileName);
      }
      if (modelUI != null) { modelUI.quit(); }
      if (modelConsole != null) { modelConsole.dispose(); }
      modelUI = null;
      modelConsole = null;
      System.out.println("ExpManager :: run() done");
      System.exit(0);
    }
  } // end run()
  
  public void createParameterFiles(int expMode) {
    if (expMode == 2) {
      for (int dx = exp_initDensity; dx <= exp_finalDensity; dx += exp_densityInterval) {
        // set file name
        String paramFileName = outputFolderName + File.separator
                    + paramFolderName + File.separator
                    + "density" + form_00000.format(dx) + ".txt";
        try {
          BufferedWriter out = new BufferedWriter(new FileWriter(paramFileName));
          out.write("GridWidth = " + exp_gridWidth);
          out.newLine();
          out.write("GridHeight = " + exp_gridHeight);
          out.newLine();
          out.write("InitialCellCount = " + dx);
          out.newLine();
          out.close();
        }
        catch (IOException e) {
          System.out.println("Couldn't write to parameter file");
        }
      }
    } // end if (expMode == 2)
    else if (expMode == 3) {
      for (int dx = exp_initDensity; dx <= exp_finalDensity; dx += exp_densityInterval) {
        for (double jx = exp_cellUnattInit; jx <= exp_cellUnattFin; jx += exp_cellUnattIntv) {
          for (double kx = exp_cellAttInit; kx <= exp_cellAttFin; kx += exp_cellAttIntv) {
            // set file name
            String paramFileName = outputFolderName + File.separator
                    + paramFolderName + File.separator
                    + "Dns" + form_00000.format(dx)
                    + "_CeAdh" + form_0d00.format(jx)
                    + "_ClAdh" + form_0d00.format(kx) + ".txt";
            try {
              BufferedWriter out = new BufferedWriter(new FileWriter(paramFileName));
              out.write("GridWidth = " + exp_gridWidth);
              out.newLine();
              out.write("GridHeight = " + exp_gridHeight);
              out.newLine();
              out.write("InitialCellCount = " + dx);
              out.newLine();
              out.write("CellAdhesionUnattached = " + jx);
              out.newLine();
              out.write("CellAdhesionAttached = " + kx);
              out.newLine();
              out.close();
            }
            catch (IOException e) {
              System.out.println("Couldn't write to parameter file");
            }
          }
        }
      }
    } // end if (expMode == 3)
    else if (expMode == 4) {
      for (int dx = exp_initDensity; dx <= exp_finalDensity; dx += exp_densityInterval) {
        for (int mig = 0; mig < exp_numMigrationMode; mig++) {
          // set file name
          String paramFileName = outputFolderName + File.separator
                    + paramFolderName + File.separator
                    + "Mig" + form_00.format(mig)
                    + "Dns" + form_00000.format(dx) + ".txt";
          try {
            BufferedWriter out = new BufferedWriter(new FileWriter(paramFileName));
            out.write("MigrationMode = " + mig);
            out.newLine();
            out.write("GridWidth = " + exp_gridWidth);
            out.newLine();
            out.write("GridHeight = " + exp_gridHeight);
            out.newLine();
            out.write("InitialCellCount = " + dx);
            out.newLine();
            out.close();
          }
          catch (IOException e) {
            System.out.println("Couldn't write to parameter file");
          }
        }
      }
    } // end if (expMode == 4)
    else if (expMode == 5) {
      for (int dx = exp_initDensity; dx <= exp_finalDensity; dx += exp_densityInterval) {
        for (int mig = 0; mig < exp_numMigrationMode; mig++) {
          for (double adhUn = exp_cellUnattInit; adhUn <= exp_cellUnattFin; adhUn += exp_cellUnattIntv) {
            for (double adhAt = exp_cellAttInit; adhAt <= exp_cellAttFin; adhAt += exp_cellAttIntv) {
              for (double ceSpd = exp_ceMigSpeedInit; ceSpd <= exp_ceMigSpeedFin; ceSpd += exp_ceMigSpeedIntv) {
                for (double clSpd = exp_clMigSpeedInit; clSpd <= exp_clMigSpeedFin; clSpd += exp_clMigSpeedIntv) {
                  // set file name
                  String paramFileName = outputFolderName + File.separator
                      + paramFolderName + File.separator
                      + "Mig" + form_00.format(mig)
                      + "Dns" + form_00000.format(dx)
                      + "_CeAdh" + form_0d00.format(adhUn)
                      + "_ClAdh" + form_0d00.format(adhAt)
                      + "_CeSpd" + form_0d00.format(ceSpd)
                      + "_ClSpd" + form_0d00.format(clSpd) + ".txt";
                    try {
                    BufferedWriter out = new BufferedWriter(new FileWriter(paramFileName));
                    out.write("MigrationMode = " + mig);
                    out.newLine();
                    out.write("GridWidth = " + exp_gridWidth);
                    out.newLine();
                    out.write("GridHeight = " + exp_gridHeight);
                    out.newLine();
                    out.write("InitialCellCount = " + dx);
                    out.newLine();
                    out.write("CellAdhesionUnattached = " + adhUn);
                    out.newLine();
                    out.write("CellAdhesionAttached = " + adhAt);
                    out.newLine();
                    out.write("CellMigrationSpeed = " + ceSpd);
                    out.newLine();
                    out.write("ClusterMigrationSpeed = " + clSpd);
                    out.newLine();
                    out.close();
                  }
                  catch (IOException e) {
                    System.out.println("Couldn't write to parameter file");
                  }
                }
              }
            }
          }
        }
      }
    } // end if (expMode == 5)
    if (expMode == 6) {
      // NOT USED
    } // end if (expMode == 6)
  } // end createParameterFiles()
  
  public void runParameterizedBatch(int expMode) {
    CultureGUI modelUI = null;
    sim.display.Console modelConsole = null;
    String imgFileName = null;
    if (image_capture || movie_capture) {
      modelUI = new CultureGUI();
      modelConsole = new sim.display.Console(modelUI);
      modelUI.diffusionGraphics = this.diffusion_graphics;
      modelUI.init(modelConsole);
    }
    if (expMode == 2) {
      for (int dx = exp_initDensity; dx <= exp_finalDensity; dx+= exp_densityInterval) {
        System.out.println("DENSITY: " + dx);
        for (int rep = 1; rep <= exp_repetitions; rep++) {
          if (rep%25 == 0) { System.out.println("\tREPETITION: " + rep); }
        
          // set file names
          String paramFileName = outputFolderName + File.separator
                    + paramFolderName + File.separator
                    + "density" + form_00000.format(dx) + ".txt";
          String rawOutFileName = outputFolderName + File.separator
                    + rawDataFolderName + File.separator
                    + "density" + form_00000.format(dx)
                    + "_rep" + form_000.format(rep);
          
          ParameterDatabase parameters = null;
          try {
            parameters=new ParameterDatabase(new File(paramFileName).getAbsoluteFile());
          }
          catch(IOException ex) { 
            ex.printStackTrace();
          }  
        
          // run simulation per parameter file
          Culture model = new Culture
                (new MersenneTwisterFast(), new Schedule(2), parameters, rawOutFileName);
          model.setMigrationMode(exp_migration_mode);
          model.start();
          for (int i = 0; i < exp_steps; i++) {
            if (movie_info_out) {
              imgFileName = outputFolderName + File.separator + imageFolderName + File.separator
                        + "img_density" + form_00000.format(dx) + "_rep" + form_000.format(rep)
                        + "_step" + form_000.format(i) + ".txt";
              model.printGraphicsInfo(imgFileName);
            }
            if (movie_capture) {
              imgFileName = outputFolderName + File.separator + imageFolderName + File.separator
                        + "img_density" + form_00000.format(dx) + "_rep" + form_000.format(rep)
                        + "_step" + form_000.format(i) + ".png";
              modelUI.captureImage(model, imgFileName);
            }
            model.schedule.step(model);
          }
          model.finish();
          if (image_info_out || movie_info_out) {
            // print image info after the final step
            imgFileName = outputFolderName + File.separator + imageFolderName + File.separator
                      + "img_density" + form_00000.format(dx) + "_rep" + form_000.format(rep)
                      + "_step" + form_000.format(exp_steps) + ".txt";
            model.printGraphicsInfo(imgFileName);
          }
          if (image_capture || movie_capture) {
            imgFileName = outputFolderName + File.separator + imageFolderName + File.separator
                      + "img_density" + form_00000.format(dx) + "_rep" + form_000.format(rep)
                      + "_step" + form_000.format(exp_steps) + ".png";
            modelUI.captureImage(model, imgFileName);
          }
        } // end for (rep...)
      } // end for (dx...)
    } // end if (expMode == 2)
    else if (expMode == 3) {
      for (int dx = exp_initDensity; dx <= exp_finalDensity; dx+= exp_densityInterval) {
        System.out.println("DENSITY: " + dx);
        for (double jx = exp_cellUnattInit; jx <= exp_cellUnattFin; jx += exp_cellUnattIntv) {
          for (double kx = exp_cellAttInit; kx <= exp_cellAttFin; kx += exp_cellAttIntv) {
            System.out.println("CELL ADHESION: " + jx + " (UNATT), " + kx + " (ATT)");
            for (int rep = 1; rep <= exp_repetitions; rep++) {
              // set file names
              String paramFileName = outputFolderName + File.separator
                    + paramFolderName + File.separator
                    + "Dns" + form_00000.format(dx)
                    + "_CeAdh" + form_0d00.format(jx)
                    + "_ClAdh" + form_0d00.format(kx) + ".txt";
              String rawOutFileName = outputFolderName + File.separator
                    + rawDataFolderName + File.separator
                    + "Dns" + form_00000.format(dx)
                    + "_CeAdh" + form_0d00.format(jx)
                    + "_ClAdh" + form_0d00.format(kx)
                    + "_rep" + form_000.format(rep);
              ParameterDatabase parameters = null;
              try {
                parameters=new ParameterDatabase(new File(paramFileName).getAbsoluteFile());
              }
              catch(IOException ex) { 
                ex.printStackTrace();
              }  
        
              // run simulation per parameter file
              Culture model = new Culture
                    (new MersenneTwisterFast(), new Schedule(2), parameters, rawOutFileName);
              model.setMigrationMode(exp_migration_mode);
              model.start();
              for (int i = 0; i < exp_steps; i++) {
                if (movie_info_out) {
                  imgFileName = outputFolderName + File.separator + imageFolderName + File.separator
                        + "img_dns" + form_00000.format(dx) + "_ce" + form_0d00.format(jx)
                        + "_cl" + form_0d00.format(kx) + "_rep" + form_000.format(rep)
                        + "_step" + form_000.format(i) + ".txt";
                  model.printGraphicsInfo(imgFileName);
                }
                if (movie_capture) {
                  imgFileName = outputFolderName + File.separator + imageFolderName + File.separator
                        + "img_dns" + form_00000.format(dx) + "_ce" + form_0d00.format(jx)
                        + "_cl" + form_0d00.format(kx) + "_rep" + form_000.format(rep)
                        + "_step" + form_000.format(i) + ".png";
                  modelUI.captureImage(model, imgFileName);
                }
                model.schedule.step(model);
              }
              model.finish();
              if (image_info_out || movie_info_out) {
                // print image info after the final step
                imgFileName = outputFolderName + File.separator + imageFolderName + File.separator
                        + "img_dns" + form_00000.format(dx) + "_ce" + form_0d00.format(jx)
                        + "_cl" + form_0d00.format(kx) + "_rep" + form_000.format(rep)
                        + "_step" + form_000.format(exp_steps) + ".txt";
                model.printGraphicsInfo(imgFileName);
              }
              if (image_capture || movie_capture) {
                imgFileName = outputFolderName + File.separator + imageFolderName + File.separator
                        + "img_dns" + form_00000.format(dx) + "_ce" + form_0d00.format(jx)
                        + "_cl" + form_0d00.format(kx) + "_rep" + form_000.format(rep)
                        + "_step" + form_000.format(exp_steps) + ".png";
                modelUI.captureImage(model, imgFileName);
              }
            } // end for (rep...)
          }
        }
      } // end for (dx...)
    } // end if (expMode == 3)
    else if (expMode == 4) {
      for (int mig = 0; mig < exp_numMigrationMode; mig++) {
        for (int dx = exp_initDensity; dx <= exp_finalDensity; dx+= exp_densityInterval) {
          System.out.println("MIGRATION MODE: " + mig + "; DENSITY: " + dx);
          for (int rep = 1; rep <= exp_repetitions; rep++) {
            if (rep%25 == 0) { System.out.println("\tREPETITION: " + rep); }
        
            // set file names
            String paramFileName = outputFolderName + File.separator
                    + paramFolderName + File.separator
                    + "Mig" + form_00.format(mig)
                    + "Dns" + form_00000.format(dx) + ".txt";
            String rawOutFileName = outputFolderName + File.separator
                    + rawDataFolderName + File.separator
                    + "Mig" + form_00.format(mig)
                    + "Dns" + form_00000.format(dx)
                    + "_rep" + form_000.format(rep);
          
            ParameterDatabase parameters = null;
            try {
              parameters=new ParameterDatabase(new File(paramFileName).getAbsoluteFile());
            }
            catch(IOException ex) { 
              ex.printStackTrace();
            }  
        
            // run simulation per parameter file
            Culture model = new Culture
                  (new MersenneTwisterFast(), new Schedule(2), parameters, rawOutFileName);
            model.start();
            for (int i = 0; i < exp_steps; i++) {
              if (movie_info_out) {
                imgFileName = outputFolderName + File.separator + imageFolderName + File.separator
                        + "img_mig" + form_00.format(mig) + "_dns" + form_00000.format(dx)
                        + "_rep" + form_000.format(rep) + "_step" + form_000.format(i) + ".txt";
                model.printGraphicsInfo(imgFileName);
              }
              if (movie_capture) {
                imgFileName = outputFolderName + File.separator + imageFolderName + File.separator
                        + "img_mig" + form_00.format(mig) + "_dns" + form_00000.format(dx)
                        + "_rep" + form_000.format(rep) + "_step" + form_000.format(i) + ".png";
                modelUI.captureImage(model, imgFileName);
              }
              model.schedule.step(model);
            }
            model.finish();
            if (image_info_out || movie_info_out) {
              // print image info after the final step
              imgFileName = outputFolderName + File.separator + imageFolderName + File.separator
                      + "img_mig" + form_00.format(mig) + "_dns" + form_00000.format(dx)
                      + "_rep" + form_000.format(rep) + "_step" + form_000.format(exp_steps) + ".txt";
              model.printGraphicsInfo(imgFileName);
            }
            if (image_capture || movie_capture) {
              imgFileName = outputFolderName + File.separator + imageFolderName + File.separator
                      + "img_mig" + form_00.format(mig) + "_dns" + form_00000.format(dx)
                      + "_rep" + form_000.format(rep) + "_step" + form_000.format(exp_steps) + ".png";
              modelUI.captureImage(model, imgFileName);
            }
          } // end for (rep...)
        } // end for (dx...)
      }
    } // end if (expMode == 4)
    else if (expMode == 5) {
      for (int mig = 0; mig < exp_numMigrationMode; mig++) {
        for (int dx = exp_initDensity; dx <= exp_finalDensity; dx+= exp_densityInterval) {
          for (double adhUn = exp_cellUnattInit; adhUn <= exp_cellUnattFin; adhUn += exp_cellUnattIntv) {
            for (double adhAt = exp_cellAttInit; adhAt <= exp_cellAttFin; adhAt += exp_cellAttIntv) {
              for (double ceSpd = exp_ceMigSpeedInit; ceSpd <= exp_ceMigSpeedFin; ceSpd += exp_ceMigSpeedIntv) {
                for (double clSpd = exp_clMigSpeedInit; clSpd <= exp_clMigSpeedFin; clSpd += exp_clMigSpeedIntv) {
                  // set file name
                  String paramFileName = outputFolderName + File.separator
                      + paramFolderName + File.separator
                      + "Mig" + form_00.format(mig)
                      + "Dns" + form_00000.format(dx)
                      + "_CeAdh" + form_0d00.format(adhUn)
                      + "_ClAdh" + form_0d00.format(adhAt)
                      + "_CeSpd" + form_0d00.format(ceSpd)
                      + "_ClSpd" + form_0d00.format(clSpd) + ".txt";
                  System.out.println("MIGRATION MODE: " + mig + "; DENSITY: " + dx);
                  for (int rep = 1; rep <= exp_repetitions; rep++) {
                    if (rep%25 == 0) { System.out.println("\tREPETITION: " + rep); }
                    // set file names
                    String rawOutFileName = outputFolderName + File.separator
                        + rawDataFolderName + File.separator
                        + "Mig" + form_00.format(mig)
                        + "Dns" + form_00000.format(dx)
                        + "_CeAdh" + form_0d00.format(adhUn)
                        + "_ClAdh" + form_0d00.format(adhAt)
                        + "_CeSpd" + form_0d00.format(ceSpd)
                        + "_ClSpd" + form_0d00.format(clSpd)
                        + "_rep" + form_000.format(rep);
                    ParameterDatabase parameters = null;
                    try {
                      parameters=new ParameterDatabase(new File(paramFileName).getAbsoluteFile());
                    }
                    catch(IOException ex) { 
                      ex.printStackTrace();
                    }  
        
                    // run simulation per parameter file
                    Culture model = new Culture
                      (new MersenneTwisterFast(), new Schedule(2), parameters, rawOutFileName);
                    model.start();
                    for (int i = 0; i < exp_steps; i++) {
                      if (movie_info_out) {
                        imgFileName = outputFolderName + File.separator + imageFolderName + File.separator
                          + "img_mig" + form_00.format(mig) + "_dns" + form_00000.format(dx)
                          + "_ceAd" + form_0d00.format(adhUn) + "_clAd" + form_0d00.format(adhAt)
                          + "_ceSp" + form_0d00.format(ceSpd) + "_clSp" + form_0d00.format(clSpd)
                          + "_rep" + form_000.format(rep) + "_step" + form_000.format(i) + ".txt";
                        model.printGraphicsInfo(imgFileName);
                      }
                      if (movie_capture) {
                        imgFileName = outputFolderName + File.separator + imageFolderName + File.separator
                          + "img_mig" + form_00.format(mig) + "_dns" + form_00000.format(dx)
                          + "_ceAd" + form_0d00.format(adhUn) + "_clAd" + form_0d00.format(adhAt)
                          + "_ceSp" + form_0d00.format(ceSpd) + "_clSp" + form_0d00.format(clSpd)
                          + "_rep" + form_000.format(rep) + "_step" + form_000.format(i) + ".png";
                        modelUI.captureImage(model, imgFileName);
                      }
                      model.schedule.step(model);
                    }
                    model.finish();
                    if (image_info_out || movie_info_out) {
                      // print image info after the final step
                      imgFileName = outputFolderName + File.separator + imageFolderName + File.separator
                        + "img_mig" + form_00.format(mig) + "_dns" + form_00000.format(dx)
                        + "_ceAd" + form_0d00.format(adhUn) + "_clAd" + form_0d00.format(adhAt)
                        + "_ceSp" + form_0d00.format(ceSpd) + "_clSp" + form_0d00.format(clSpd)
                        + "_rep" + form_000.format(rep) + "_step" + form_000.format(exp_steps) + ".txt";
                      model.printGraphicsInfo(imgFileName);
                    }
                    if (image_capture || movie_capture) {
                      imgFileName = outputFolderName + File.separator + imageFolderName + File.separator
                        + "img_mig" + form_00.format(mig) + "_dns" + form_00000.format(dx)
                        + "_ceAd" + form_0d00.format(adhUn) + "_clAd" + form_0d00.format(adhAt)
                        + "_ceSp" + form_0d00.format(ceSpd) + "_clSp" + form_0d00.format(clSpd)
                        + "_rep" + form_000.format(rep) + "_step" + form_000.format(exp_steps) + ".png";
                      modelUI.captureImage(model, imgFileName);
                    }
                  }
                }
              }
            }
          } // end for (rep...)
        } // end for (dx...)
      }
    } // end if (expMode == 5)
    if (expMode == 6) {
      for (int ix = 0; ix < 2; ix++) {
        String woundType = "Sep";
        if (ix ==1 ) { woundType = "Cont"; }
        for (int dx = exp_initWoundSize; dx <= exp_finalWoundSize; dx+= exp_woundSizeInterval) {
          System.out.println("WOUND SIZE (" + woundType + "): " + dx);
          for (int rep = 1; rep <= exp_repetitions; rep++) {
            if (rep%25 == 0) { System.out.println("\tREPETITION: " + rep); }
        
            // set file names
            String paramFileName = outputFolderName + File.separator
                    + paramFolderName + File.separator
                    + "wound" + woundType + form_00.format(dx) + ".txt";
            String rawOutFileName = outputFolderName + File.separator
                    + rawDataFolderName + File.separator
                    + "wound" + woundType + form_00.format(dx)
                    + "_rep" + form_000.format(rep);
          
            ParameterDatabase parameters = null;
            /*
            try {
              parameters=new ParameterDatabase(new File(paramFileName).getAbsoluteFile());
            }
            catch(IOException ex) { 
              ex.printStackTrace();
            }
            */
        
            // run simulation per parameter file
            Culture model = new Culture
                (new MersenneTwisterFast(), new Schedule(2), parameters, rawOutFileName);
            model.setMigrationMode(exp_migration_mode);
            if (ix == 0) { model.setWoundTypeContig(false); }
            else { model.setWoundTypeContig(true); }
            model.setWoundSize(dx);
            model.start();
            for (int i = 0; i < exp_steps; i++) {
              if (movie_info_out) {
                imgFileName = outputFolderName + File.separator + imageFolderName + File.separator
                        + "wound" + woundType + form_00.format(dx) + "_rep" + form_000.format(rep)
                        + "_step" + form_000.format(i) + ".txt";
                model.printGraphicsInfo(imgFileName);
              }
              if (movie_capture) {
                imgFileName = outputFolderName + File.separator + imageFolderName + File.separator
                        + "wound" + woundType + form_00.format(dx) + "_rep" + form_000.format(rep)
                        + "_step" + form_000.format(i) + ".png";
                modelUI.captureImage(model, imgFileName);
              }
              model.schedule.step(model);
            }
            model.finish();
            if (image_info_out || movie_info_out) {
              // print image info after the final step
              imgFileName = outputFolderName + File.separator + imageFolderName + File.separator
                      + "wound" + woundType + form_00.format(dx) + "_rep" + form_000.format(rep)
                      + "_step" + form_000.format(exp_steps) + ".txt";
              model.printGraphicsInfo(imgFileName);
            }
            if (image_capture || movie_capture) {
              imgFileName = outputFolderName + File.separator + imageFolderName + File.separator
                      + "wound" + woundType + form_00.format(dx) + "_rep" + form_000.format(rep)
                      + "_step" + form_000.format(exp_steps) + ".png";
              modelUI.captureImage(model, imgFileName);
            }
          } // end for (rep...)
        } // end for (dx...)
      } // end for (ix...)
    } // end if (expMode == 6)
    
    if (modelUI != null) { modelUI.quit(); }
    if (modelConsole != null) { modelConsole.dispose(); }
    modelUI = null;
    modelConsole = null;
    
  } // end runParameterizedBatch()
  
  public void processRawData(int expMode) {
    try {
      String dataTableFileName = outputFolderName + File.separator
                 + summaryFolderName + File.separator
                 + "silico_data_all.txt";
      BufferedWriter dataTableWriter  = new BufferedWriter(new FileWriter(dataTableFileName));
      dataTableWriter.write("density\tdiameter");
      dataTableWriter.newLine();
      
      String morphLevelOutFileName = null;
      BufferedWriter morphLevelWriter = null;
      if (morph_level_out) {
        morphLevelOutFileName = outputFolderName + File.separator + summaryFolderName
                + File.separator + "morph_level_all.txt";
        morphLevelWriter = new BufferedWriter(new FileWriter(morphLevelOutFileName));
      }
      
      if (expMode == 2) {
        for (int dx = exp_initDensity; dx <= exp_finalDensity; dx+= exp_densityInterval) {
          // add up the number of clusters and cluster sizes
          double totalClusterCount = 0.0;
          double totalCystCount = 0.0;
          double totalCystSize = 0.0;
          double[][] totalRuleUsage = new double[exp_steps][exp_totalRuleNum];
          for (int i = 0; i < exp_steps; i++) {
            for (int j = 0; j < exp_totalRuleNum; j++) {
              totalRuleUsage[i][j] = 0;
            }
          }
          for (int rep = 1; rep <= exp_repetitions; rep++) {
            String clusterInfoFileName = outputFolderName + File.separator
                    + rawDataFolderName + File.separator
                    + "density" + form_00000.format(dx)
                    + "_rep" + form_000.format(rep) + "_cluster.txt";
            Scanner clusterInfoScan = null;

            clusterInfoScan = new Scanner(new BufferedReader(new FileReader(clusterInfoFileName)));
            double curCystSize;
            while (clusterInfoScan.hasNext()) {
              curCystSize = clusterInfoScan.nextDouble();
              totalClusterCount++;
              if (curCystSize > 0) {
                totalCystSize += curCystSize;
                totalCystCount++;
                dataTableWriter.write(Integer.toString(dx) + "\t" + Double.toString(curCystSize));
                dataTableWriter.newLine();
              }
            }
            if (clusterInfoScan!= null) { clusterInfoScan.close(); }
          
            // add up the rule usage patterns over time
            String ruleUsageFileName = outputFolderName + File.separator
                    + rawDataFolderName + File.separator
                    + "density" + form_00000.format(dx)
                    + "_rep" + form_000.format(rep) + "_rules.txt";
            Scanner ruleUsageScan = null;

            ruleUsageScan = new Scanner(new BufferedReader(new FileReader(ruleUsageFileName)));
            int xIndex = 0;
            int yIndex = 0;
            while (ruleUsageScan.hasNext()) {
              double curVal = ruleUsageScan.nextDouble();
              totalRuleUsage[xIndex][yIndex] += curVal;
              if (morph_level_out && yIndex == 30) { morphLevelWriter.write(curVal + " "); }
              yIndex++;
              if (yIndex == exp_totalRuleNum) {
                xIndex++;
                yIndex = 0;
              }
            }
            if (morph_level_out) { morphLevelWriter.newLine(); }
            if (ruleUsageScan!= null) { ruleUsageScan.close(); }
          }
        
          // write out the average cluster size and number
          String clusterSumFileName = outputFolderName + File.separator
                 + summaryFolderName + File.separator
                 + "density" + form_00000.format(dx) + "_cluster_summary.txt";
          BufferedWriter clusterSumWriter  = new BufferedWriter(new FileWriter(clusterSumFileName));
          double avgCystSize = 0.0;
          if (totalCystCount > 0) {
            avgCystSize = totalCystSize / totalCystCount;
          }
          clusterSumWriter.write(avgCystSize + " ");
          clusterSumWriter.newLine();
          clusterSumWriter.write((totalCystCount / exp_repetitions) + " ");
          clusterSumWriter.newLine();
          clusterSumWriter.write((totalClusterCount / exp_repetitions) + " ");
          clusterSumWriter.newLine();
          if (clusterSumWriter != null) { clusterSumWriter.close(); }
        
          // write out the average rule usage pattern
          String ruleSumFileName = outputFolderName + File.separator
                 + summaryFolderName + File.separator
                 + "density" + form_00000.format(dx) + "_rules_summary.txt";
          BufferedWriter ruleSumWriter  = new BufferedWriter(new FileWriter(ruleSumFileName));
          for (int j = 0; j < exp_totalRuleNum; j++) {
            ruleSumWriter.write("A_" + (j+1) + "\t");
          }
          ruleSumWriter.newLine();
          for (int i = 0; i < exp_steps; i++) {
            for (int j = 0; j < exp_totalRuleNum; j++) {
              ruleSumWriter.write((totalRuleUsage[i][j] / exp_repetitions) + "\t");
            }
            ruleSumWriter.newLine();
          }
          if (ruleSumWriter != null) { ruleSumWriter.close(); }
        }
      } // end if (expMode == 2)
      else if (expMode == 3) {
        for (int dx = exp_initDensity; dx <= exp_finalDensity; dx+= exp_densityInterval) {
          for (double jx = exp_cellUnattInit; jx <= exp_cellUnattFin; jx += exp_cellUnattIntv) {
             for (double kx = exp_cellAttInit; kx <= exp_cellAttFin; kx += exp_cellAttIntv) {
              // add up the number of clusters and cluster sizes
              double totalClusterCount = 0.0;
              double totalCystCount = 0.0;
              double totalCystSize = 0.0;
              double[][] totalRuleUsage = new double[exp_steps][exp_totalRuleNum];
              for (int i = 0; i < exp_steps; i++) {
                for (int j = 0; j < exp_totalRuleNum; j++) {
                  totalRuleUsage[i][j] = 0;
                }
              }
              for (int rep = 1; rep <= exp_repetitions; rep++) {
                String clusterInfoFileName = outputFolderName + File.separator
                        + rawDataFolderName + File.separator
                        + "Dns" + form_00000.format(dx)
                        + "_CeAdh" + form_0d00.format(jx)
                        + "_ClAdh" + form_0d00.format(kx)
                        + "_rep" + form_000.format(rep)
                        + "_cluster.txt";
                Scanner clusterInfoScan = null;

                clusterInfoScan = new Scanner(new BufferedReader(new FileReader(clusterInfoFileName)));
                double curCystSize;
                while (clusterInfoScan.hasNext()) {
                  curCystSize = clusterInfoScan.nextDouble();
                  totalClusterCount++;
                  if (curCystSize > 0) {
                    totalCystSize += curCystSize;
                    totalCystCount++;
                    dataTableWriter.write(Integer.toString(dx) + "\t" + Double.toString(curCystSize));
                    dataTableWriter.newLine();
                  }
                }
                if (clusterInfoScan!= null) { clusterInfoScan.close(); }
          
                // add up the rule usage patterns over time
                String ruleUsageFileName = outputFolderName + File.separator
                        + rawDataFolderName + File.separator
                        + "Dns" + form_00000.format(dx)
                        + "_CeAdh" + form_0d00.format(jx)
                        + "_ClAdh" + form_0d00.format(kx)
                        + "_rep" + form_000.format(rep)
                        + "_rules.txt";
                Scanner ruleUsageScan = null;

                ruleUsageScan = new Scanner(new BufferedReader(new FileReader(ruleUsageFileName)));
                int xIndex = 0;
                int yIndex = 0;
                while (ruleUsageScan.hasNext()) {
                  totalRuleUsage[xIndex][yIndex] += ruleUsageScan.nextDouble();
                  yIndex++;
                  if (yIndex == exp_totalRuleNum) {
                    xIndex++;
                    yIndex = 0;
                  }
                }
                if (ruleUsageScan!= null) { ruleUsageScan.close(); }
              }
        
              // write out the average cluster size and number
              String clusterSumFileName = outputFolderName + File.separator
                     + summaryFolderName + File.separator
                     + "Dns" + form_00000.format(dx)
                     + "_CeAdh" + form_0d00.format(jx)
                     + "_ClAdh" + form_0d00.format(kx)
                     + "_cluster_sum.txt";
              BufferedWriter clusterSumWriter  = new BufferedWriter(new FileWriter(clusterSumFileName));
              double avgCystSize = 0.0;
              if (totalCystCount > 0) {
                avgCystSize = totalCystSize / totalCystCount;
              }
              clusterSumWriter.write(avgCystSize + " ");
              clusterSumWriter.newLine();
              clusterSumWriter.write((totalCystCount / exp_repetitions) + " ");
              clusterSumWriter.newLine();
              clusterSumWriter.write((totalClusterCount / exp_repetitions) + " ");
              clusterSumWriter.newLine();
              if (clusterSumWriter != null) { clusterSumWriter.close(); }
        
              // write out the average rule usage pattern
              String ruleSumFileName = outputFolderName + File.separator
                     + summaryFolderName + File.separator
                     + "Dns" + form_00000.format(dx)
                     + "_CeAdh" + form_0d00.format(jx)
                     + "_ClAdh" + form_0d00.format(kx)
                     + "_rules_sum.txt";
              BufferedWriter ruleSumWriter  = new BufferedWriter(new FileWriter(ruleSumFileName));
              for (int j = 0; j < exp_totalRuleNum; j++) {
                ruleSumWriter.write("A_" + (j+1) + "\t");
              }
              ruleSumWriter.newLine();
              for (int i = 0; i < exp_steps; i++) {
                for (int j = 0; j < exp_totalRuleNum; j++) {
                  ruleSumWriter.write((totalRuleUsage[i][j] / exp_repetitions) + "\t");
                }
                ruleSumWriter.newLine();
              }
              if (ruleSumWriter != null) { ruleSumWriter.close(); }
            }
          }
        }
      } // end if (expMode == 3)
      else if (expMode == 4) {
        for (int mig = 0; mig < exp_numMigrationMode; mig++) {
          for (int dx = exp_initDensity; dx <= exp_finalDensity; dx+= exp_densityInterval) {
            // add up the number of clusters and cluster sizes
            double totalClusterCount = 0.0;
            double totalCystCount = 0.0;
            double totalCystSize = 0.0;
            double[][] totalRuleUsage = new double[exp_steps][exp_totalRuleNum];
            for (int i = 0; i < exp_steps; i++) {
              for (int j = 0; j < exp_totalRuleNum; j++) {
                totalRuleUsage[i][j] = 0;
              }
            }
            for (int rep = 1; rep <= exp_repetitions; rep++) {
              String clusterInfoFileName = outputFolderName + File.separator
                      + rawDataFolderName + File.separator
                      + "Mig" + form_00.format(mig)
                      + "Dns" + form_00000.format(dx)
                      + "_rep" + form_000.format(rep)
                      + "_cluster.txt";
              Scanner clusterInfoScan = null;

              clusterInfoScan = new Scanner(new BufferedReader(new FileReader(clusterInfoFileName)));
              double curCystSize;
              while (clusterInfoScan.hasNext()) {
                curCystSize = clusterInfoScan.nextDouble();
                totalClusterCount++;
                if (curCystSize > 0) {
                  totalCystSize += curCystSize;
                  totalCystCount++;
                  dataTableWriter.write(Integer.toString(dx) + "\t" + Double.toString(curCystSize));
                  dataTableWriter.newLine();
                }
              }
              if (clusterInfoScan!= null) { clusterInfoScan.close(); }
        
              // add up the rule usage patterns over time
              String ruleUsageFileName = outputFolderName + File.separator
                      + rawDataFolderName + File.separator
                      + "Mig" + form_00.format(mig)
                      + "Dns" + form_00000.format(dx)
                      + "_rep" + form_000.format(rep)
                      + "_rules.txt";
              Scanner ruleUsageScan = null;

              ruleUsageScan = new Scanner(new BufferedReader(new FileReader(ruleUsageFileName)));
              int xIndex = 0;
              int yIndex = 0;
              while (ruleUsageScan.hasNext()) {
                totalRuleUsage[xIndex][yIndex] += ruleUsageScan.nextDouble();
                yIndex++;
                if (yIndex == exp_totalRuleNum) {
                  xIndex++;
                  yIndex = 0;
                }
              }
              if (ruleUsageScan!= null) { ruleUsageScan.close(); }
            }
        
            // write out the average cluster size and number
            String clusterSumFileName = outputFolderName + File.separator
                     + summaryFolderName + File.separator
                     + "Mig" + form_00.format(mig)
                     + "Dns" + form_00000.format(dx)
                     + "_cluster_sum.txt";
            BufferedWriter clusterSumWriter  = new BufferedWriter(new FileWriter(clusterSumFileName));
            double avgCystSize = 0.0;
            if (totalCystCount > 0) {
              avgCystSize = totalCystSize / totalCystCount;
            }
            clusterSumWriter.write(avgCystSize + " ");
            clusterSumWriter.newLine();
            clusterSumWriter.write((totalCystCount / exp_repetitions) + " ");
            clusterSumWriter.newLine();
            clusterSumWriter.write((totalClusterCount / exp_repetitions) + " ");
            clusterSumWriter.newLine();
            if (clusterSumWriter != null) { clusterSumWriter.close(); }
        
            // write out the average rule usage pattern
            String ruleSumFileName = outputFolderName + File.separator
                     + summaryFolderName + File.separator
                     + "Mig" + form_00.format(mig)
                     + "Dns" + form_00000.format(dx)
                     + "_rules_sum.txt";
            BufferedWriter ruleSumWriter  = new BufferedWriter(new FileWriter(ruleSumFileName));
            for (int j = 0; j < exp_totalRuleNum; j++) {
              ruleSumWriter.write("A_" + (j+1) + "\t");
            }
            ruleSumWriter.newLine();
            for (int i = 0; i < exp_steps; i++) {
              for (int j = 0; j < exp_totalRuleNum; j++) {
                ruleSumWriter.write((totalRuleUsage[i][j] / exp_repetitions) + "\t");
              }
              ruleSumWriter.newLine();
            }
            if (ruleSumWriter != null) { ruleSumWriter.close(); }
          }
        }
      } // end if (expMode == 4)
      else if (expMode == 5) {
        for (int mig = 0; mig < exp_numMigrationMode; mig++) {
          for (int dx = exp_initDensity; dx <= exp_finalDensity; dx+= exp_densityInterval) {
            for (double adhUn = exp_cellUnattInit; adhUn <= exp_cellUnattFin; adhUn += exp_cellUnattIntv) {
              for (double adhAt = exp_cellAttInit; adhAt <= exp_cellAttFin; adhAt += exp_cellAttIntv) {
                for (double ceSpd = exp_ceMigSpeedInit; ceSpd <= exp_ceMigSpeedFin; ceSpd += exp_ceMigSpeedIntv) {
                  for (double clSpd = exp_clMigSpeedInit; clSpd <= exp_clMigSpeedFin; clSpd += exp_clMigSpeedIntv) {
                    
                    // add up the number of clusters and cluster sizes
                    double totalClusterCount = 0.0;
                    double totalCystCount = 0.0;
                    double totalCystSize = 0.0;
                    double[][] totalRuleUsage = new double[exp_steps][exp_totalRuleNum];
                    for (int i = 0; i < exp_steps; i++) {
                      for (int j = 0; j < exp_totalRuleNum; j++) {
                        totalRuleUsage[i][j] = 0;
                      }
                    }
                    for (int rep = 1; rep <= exp_repetitions; rep++) {
                      String clusterInfoFileName = outputFolderName + File.separator
                          + rawDataFolderName + File.separator
                          + "Mig" + form_00.format(mig)
                          + "Dns" + form_00000.format(dx)
                          + "_CeAdh" + form_0d00.format(adhUn)
                          + "_ClAdh" + form_0d00.format(adhAt)
                          + "_CeSpd" + form_0d00.format(ceSpd)
                          + "_ClSpd" + form_0d00.format(clSpd)
                          + "_rep" + form_000.format(rep)
                          + "_cluster.txt";
                      Scanner clusterInfoScan = null;

                      clusterInfoScan = new Scanner(new BufferedReader(new FileReader(clusterInfoFileName)));
                      double curCystSize;
                      while (clusterInfoScan.hasNext()) {
                        curCystSize = clusterInfoScan.nextDouble();
                        totalClusterCount++;
                        if (curCystSize > 0) {
                          totalCystSize += curCystSize;
                          totalCystCount++;
                          dataTableWriter.write(Integer.toString(dx) + "\t" + Double.toString(curCystSize));
                          dataTableWriter.newLine();
                        }
                      }
                      if (clusterInfoScan!= null) { clusterInfoScan.close(); }
        
                      // add up the rule usage patterns over time
                      String ruleUsageFileName = outputFolderName + File.separator
                          + rawDataFolderName + File.separator
                          + "Mig" + form_00.format(mig)
                          + "Dns" + form_00000.format(dx)
                          + "_CeAdh" + form_0d00.format(adhUn)
                          + "_ClAdh" + form_0d00.format(adhAt)
                          + "_CeSpd" + form_0d00.format(ceSpd)
                          + "_ClSpd" + form_0d00.format(clSpd)
                          + "_rep" + form_000.format(rep)
                          + "_rules.txt";
                      Scanner ruleUsageScan = null;

                      ruleUsageScan = new Scanner(new BufferedReader(new FileReader(ruleUsageFileName)));
                      int xIndex = 0;
                      int yIndex = 0;
                      while (ruleUsageScan.hasNext()) {
                        totalRuleUsage[xIndex][yIndex] += ruleUsageScan.nextDouble();
                        yIndex++;
                        if (yIndex == exp_totalRuleNum) {
                          xIndex++;
                          yIndex = 0;
                        }
                      }
                      if (ruleUsageScan!= null) { ruleUsageScan.close(); }
                    }
        
                    // write out the average cluster size and number
                    String clusterSumFileName = outputFolderName + File.separator
                        + summaryFolderName + File.separator
                        + "Mig" + form_00.format(mig)
                        + "Dns" + form_00000.format(dx)
                        + "_CeAdh" + form_0d00.format(adhUn)
                        + "_ClAdh" + form_0d00.format(adhAt)
                        + "_CeSpd" + form_0d00.format(ceSpd)
                        + "_ClSpd" + form_0d00.format(clSpd)
                        + "_cluster_sum.txt";
                    BufferedWriter clusterSumWriter  = new BufferedWriter(new FileWriter(clusterSumFileName));
                    double avgCystSize = 0.0;
                    if (totalCystCount > 0) {
                      avgCystSize = totalCystSize / totalCystCount;
                    }
                    clusterSumWriter.write(avgCystSize + " ");
                    clusterSumWriter.newLine();
                    clusterSumWriter.write((totalCystCount / exp_repetitions) + " ");
                    clusterSumWriter.newLine();
                    clusterSumWriter.write((totalClusterCount / exp_repetitions) + " ");
                    clusterSumWriter.newLine();
                    if (clusterSumWriter != null) { clusterSumWriter.close(); }
        
                    // write out the average rule usage pattern
                    String ruleSumFileName = outputFolderName + File.separator
                        + summaryFolderName + File.separator
                        + "Mig" + form_00.format(mig)
                        + "Dns" + form_00000.format(dx)
                        + "_CeAdh" + form_0d00.format(adhUn)
                        + "_ClAdh" + form_0d00.format(adhAt)
                        + "_CeSpd" + form_0d00.format(ceSpd)
                        + "_ClSpd" + form_0d00.format(clSpd)
                        + "_rules_sum.txt";
                    BufferedWriter ruleSumWriter  = new BufferedWriter(new FileWriter(ruleSumFileName));
                    for (int j = 0; j < exp_totalRuleNum; j++) {
                      ruleSumWriter.write("A_" + (j+1) + "\t");
                    }
                    ruleSumWriter.newLine();
                    for (int i = 0; i < exp_steps; i++) {
                      for (int j = 0; j < exp_totalRuleNum; j++) {
                        ruleSumWriter.write((totalRuleUsage[i][j] / exp_repetitions) + "\t");
                      }
                      ruleSumWriter.newLine();
                    }
                    if (ruleSumWriter != null) { ruleSumWriter.close(); }
                  }
                }
              }
            }
          }
        }
      } // end if (expMode == 5)
      if (expMode == 6) {
        for (int ix = 0; ix < 2; ix++) {
          String woundType = "Sep";
          if (ix == 1) { woundType = "Cont"; }
          for (int dx = exp_initWoundSize; dx <= exp_finalWoundSize; dx+= exp_woundSizeInterval) {
            // add up the number of clusters and cluster sizes
            double totalClusterCount = 0.0;
            double totalCystCount = 0.0;
            double totalCystSize = 0.0;
            double[][] totalRuleUsage = new double[exp_steps][exp_totalRuleNum];
            for (int i = 0; i < exp_steps; i++) {
              for (int j = 0; j < exp_totalRuleNum; j++) {
                totalRuleUsage[i][j] = 0;
              }
            }
            for (int rep = 1; rep <= exp_repetitions; rep++) {
              String clusterInfoFileName = outputFolderName + File.separator
                      + rawDataFolderName + File.separator
                      + "wound" + woundType + form_00.format(dx)
                      + "_rep" + form_000.format(rep) + "_cluster.txt";
              Scanner clusterInfoScan = null;

              clusterInfoScan = new Scanner(new BufferedReader(new FileReader(clusterInfoFileName)));
              double curCystSize;
              while (clusterInfoScan.hasNext()) {
                curCystSize = clusterInfoScan.nextDouble();
                totalClusterCount++;
                if (curCystSize > 0) {
                  totalCystSize += curCystSize;
                  totalCystCount++;
                  dataTableWriter.write(Integer.toString(dx) + "\t" + Double.toString(curCystSize));
                  dataTableWriter.newLine();
                }
              }
              if (clusterInfoScan!= null) { clusterInfoScan.close(); }
          
              // add up the rule usage patterns over time
              String ruleUsageFileName = outputFolderName + File.separator
                    + rawDataFolderName + File.separator
                    + "wound" + woundType + form_00.format(dx)
                    + "_rep" + form_000.format(rep) + "_rules.txt";
              Scanner ruleUsageScan = null;

              ruleUsageScan = new Scanner(new BufferedReader(new FileReader(ruleUsageFileName)));
              int xIndex = 0;
              int yIndex = 0;
              while (ruleUsageScan.hasNext()) {
                double curVal = ruleUsageScan.nextDouble();
                totalRuleUsage[xIndex][yIndex] += curVal;
                if (morph_level_out && yIndex == 30) { morphLevelWriter.write(curVal + " "); }
                yIndex++;
                if (yIndex == exp_totalRuleNum) {
                  xIndex++;
                  yIndex = 0;
                }
              }
              if (morph_level_out) { morphLevelWriter.newLine(); }
              if (ruleUsageScan!= null) { ruleUsageScan.close(); }
            }
        
            // write out the average cluster size and number
            String clusterSumFileName = outputFolderName + File.separator
                   + summaryFolderName + File.separator
                   + "wound" + woundType + form_00.format(dx) + "_cluster_summary.txt";
            BufferedWriter clusterSumWriter  = new BufferedWriter(new FileWriter(clusterSumFileName));
            double avgCystSize = 0.0;
            if (totalCystCount > 0) {
              avgCystSize = totalCystSize / totalCystCount;
            }
            clusterSumWriter.write(avgCystSize + " ");
            clusterSumWriter.newLine();
            clusterSumWriter.write((totalCystCount / exp_repetitions) + " ");
            clusterSumWriter.newLine();
            clusterSumWriter.write((totalClusterCount / exp_repetitions) + " ");
            clusterSumWriter.newLine();
            if (clusterSumWriter != null) { clusterSumWriter.close(); }
        
            // write out the average rule usage pattern
            String ruleSumFileName = outputFolderName + File.separator
                   + summaryFolderName + File.separator
                   + "wound" + woundType + form_00.format(dx) + "_rules_summary.txt";
            BufferedWriter ruleSumWriter  = new BufferedWriter(new FileWriter(ruleSumFileName));
            for (int j = 0; j < exp_totalRuleNum; j++) {
              ruleSumWriter.write("A_" + (j+1) + "\t");
            }
            ruleSumWriter.newLine();
            for (int i = 0; i < exp_steps; i++) {
              for (int j = 0; j < exp_totalRuleNum; j++) {
                ruleSumWriter.write((totalRuleUsage[i][j] / exp_repetitions) + "\t");
              }
              ruleSumWriter.newLine();
            }
            if (ruleSumWriter != null) { ruleSumWriter.close(); }
          }
        }
      } // end if (expMode == 6)
      
      if (dataTableWriter != null) { dataTableWriter.close(); }
      if (morph_level_out && morphLevelWriter != null) { morphLevelWriter.close(); }
      // Write out times points when morphology level returns to normal (1.0)
      if (morph_level_out && expMode == 2) {
        String morphLevelTimeFileName = outputFolderName + File.separator + summaryFolderName
                + File.separator + "morph_level_time.txt";
        BufferedWriter morphLevelTimeWriter = null;
        morphLevelTimeWriter = new BufferedWriter(new FileWriter(morphLevelTimeFileName));
        Scanner morphLevelScan = null;
        morphLevelScan = new Scanner(new BufferedReader(new FileReader(morphLevelOutFileName)));
        int xIndex = 0;
        int yIndex = 0;
        boolean prevAbnormal = true;
        while (morphLevelScan.hasNext()) {
          double curVal = morphLevelScan.nextDouble();
          if (prevAbnormal && curVal <= 1.35) {
            morphLevelTimeWriter.write(yIndex + " ");
            morphLevelTimeWriter.newLine();
            prevAbnormal = false;
          }
          yIndex++;
          if (yIndex == exp_steps) {
            if (prevAbnormal) {
              morphLevelTimeWriter.write(yIndex + " ");
              morphLevelTimeWriter.newLine();
            }
            xIndex++;
            yIndex = 0;
            prevAbnormal = true;
          }
        }
        if (morphLevelScan!= null) { morphLevelScan.close(); }
        if (morphLevelTimeWriter != null) { morphLevelTimeWriter.close(); }
      }
    }
    catch (FileNotFoundException ex) {
      ex.printStackTrace();
    }
    catch (IOException ex){
     ex.printStackTrace();
    }
  } // end processRawData()
  
  public void transferClusterSummaryToExcel(int expMode) {
    File excelFolderFile = new File(outputFolderName + File.separator + excelFolderName);
    excelFolderFile.mkdirs();
    try {
      // transfer simulation output to Excel spreadsheet
      String outFileName = outputFolderName + File.separator + excelFolderName + File.separator
                                 + "ClusterSummary.xls";
      WritableWorkbook resultWorkbook = Workbook.createWorkbook(new File(outFileName));
      if (expMode == 2) {
        for (int index = 1; index < 2; index++) {
          WritableSheet resultSheet = resultWorkbook.createSheet("silico_data", index);
          resultSheet.addCell(new Label(0, 2, "*converted"));
          resultSheet.addCell(new Label(0, 4, "Density"));
          resultSheet.addCell(new Label(1, 4, "Density*"));
          resultSheet.addCell(new Label(3, 4, "ALC Size"));
          resultSheet.addCell(new Label(4, 4, "ALC Size*"));
          resultSheet.addCell(new Label(5, 4, "ALC Count"));
          resultSheet.addCell(new Label(6, 4, "Cluster Count"));
          int row = 5;
          for (int dx = exp_initDensity; dx <= exp_finalDensity; dx+= exp_densityInterval) {
            resultSheet.addCell(new jxl.write.Number(0, row, dx)); // write initial cell count
            String outputFileName = outputFolderName + File.separator
                                     + summaryFolderName + File.separator
                                     + "density" + form_00000.format(dx) + "_cluster_summary.txt";

            Scanner outputScan = null;
            outputScan = new Scanner(new BufferedReader(new FileReader(outputFileName)));
            resultSheet.addCell(new jxl.write.Number(3, row, outputScan.nextDouble()));
            resultSheet.addCell(new jxl.write.Number(5, row, outputScan.nextDouble()));
            resultSheet.addCell(new jxl.write.Number(6, row, outputScan.nextDouble()));
            if (outputScan != null) {
              outputScan.close();
            }
            row++;
          }
        }
      } // end if (expMode == 2)
      else if (expMode == 3) {
        int idx, row, col;
        idx = 0;
        WritableSheet resultSheet;
        for (double jx = exp_cellUnattInit; jx <= exp_cellUnattFin; jx += exp_cellUnattIntv) {
          String sheetName = "UnattPrb_" + form_0d00.format(jx);
          resultSheet = resultWorkbook.createSheet(sheetName, idx++);
          resultSheet.addCell(new Label(0, 2, "*converted"));
          resultSheet.addCell(new Label(0, 4, "Density"));
          resultSheet.addCell(new Label(1, 4, "Density*"));
          row = 5;
          for (int dx = exp_initDensity; dx <= exp_finalDensity; dx += exp_densityInterval) {
            resultSheet.addCell(new jxl.write.Number(0, row, dx));
            row++;
          }
          col = 3;
          for (double kx = exp_cellAttInit; kx <= exp_cellAttFin; kx += exp_cellAttIntv) {
            String entryName = "Unatt=" + form_0d00.format(jx) + ", Att=" + form_0d00.format(kx);
            resultSheet.addCell(new Label(col, 3, entryName));
            resultSheet.addCell(new Label(col, 4, "ALC Size"));
            resultSheet.addCell(new Label(col+1, 4, "ALC Size*"));
            resultSheet.addCell(new Label(col+2, 4, "ALC Count"));
            resultSheet.addCell(new Label(col+3, 4, "Cluster Count"));
            row = 5;
            for (int dx = exp_initDensity; dx <= exp_finalDensity; dx+= exp_densityInterval) {
              String outputFileName = outputFolderName + File.separator
                                 + summaryFolderName + File.separator
                                 + "Dns" + form_00000.format(dx)
                                 + "_CeAdh" + form_0d00.format(jx)
                                 + "_ClAdh" + form_0d00.format(kx)
                                 + "_cluster_sum.txt";
              Scanner outputScan = null;
              outputScan = new Scanner(new BufferedReader(new FileReader(outputFileName)));
              resultSheet.addCell(new jxl.write.Number(col, row, outputScan.nextDouble()));
              resultSheet.addCell(new jxl.write.Number(col+2, row, outputScan.nextDouble()));
              resultSheet.addCell(new jxl.write.Number(col+3, row, outputScan.nextDouble()));
              if (outputScan != null) {
                outputScan.close();
              }
              row++;
            }
            col += 5;
          }
        }
      } // end if (expMode == 3)
      else if (expMode == 4) {
        int row, col;
        WritableSheet resultSheet = resultWorkbook.createSheet("silico_data", 1);
        resultSheet.addCell(new Label(0, 2, "*converted"));
        resultSheet.addCell(new Label(0, 4, "Density"));
        resultSheet.addCell(new Label(1, 4, "Density*"));

        row = 5;
        for (int dx = exp_initDensity; dx <= exp_finalDensity; dx += exp_densityInterval) {
          resultSheet.addCell(new jxl.write.Number(0, row, dx));
          row++;
        }
        col = 3;
        for (int mig = 0; mig < exp_numMigrationMode; mig++) {
          String entryName = "Mig.Mode=" + form_00.format(mig);
          resultSheet.addCell(new Label(col, 3, entryName));
          resultSheet.addCell(new Label(col, 4, "ALC Size"));
          resultSheet.addCell(new Label(col+1, 4, "ALC Size*"));
          resultSheet.addCell(new Label(col+2, 4, "ALC Count"));
          resultSheet.addCell(new Label(col+3, 4, "Cluster Count"));
          row = 5;
          for (int dx = exp_initDensity; dx <= exp_finalDensity; dx += exp_densityInterval) {
            String outputFileName = outputFolderName + File.separator
                                 + summaryFolderName + File.separator
                                 + "Mig" + form_00.format(mig)
                                 + "Dns" + form_00000.format(dx)
                                 + "_cluster_sum.txt";
            Scanner outputScan = null;
            outputScan = new Scanner(new BufferedReader(new FileReader(outputFileName)));
            resultSheet.addCell(new jxl.write.Number(col, row, outputScan.nextDouble()));
            resultSheet.addCell(new jxl.write.Number(col+2, row, outputScan.nextDouble()));
            resultSheet.addCell(new jxl.write.Number(col+3, row, outputScan.nextDouble()));
            if (outputScan != null) {
              outputScan.close();
            }
            row++;
          }
          col += 5;
        }
      } // end if (expMode == 4)
      /*
      else if (expMode == 5) {
        int row, col;
        WritableSheet resultSheet = resultWorkbook.createSheet("silico_data", 1);
        resultSheet.addCell(new Label(0, 2, "*converted"));
        resultSheet.addCell(new Label(0, 4, "Density"));
        resultSheet.addCell(new Label(1, 4, "Density*"));

        row = 5;
        for (int dx = exp_initDensity; dx <= exp_finalDensity; dx += exp_densityInterval) {
          resultSheet.addCell(new jxl.write.Number(0, row, dx));
          row++;
        }
        col = 3;
        for (int mig = 0; mig < exp_numMigrationMode; mig++) {
          String entryName = "Mig.Mode=" + form_00.format(mig);
          resultSheet.addCell(new Label(col, 3, entryName));
          resultSheet.addCell(new Label(col, 4, "ALC Size"));
          resultSheet.addCell(new Label(col+1, 4, "ALC Size*"));
          resultSheet.addCell(new Label(col+2, 4, "ALC Count"));
          resultSheet.addCell(new Label(col+3, 4, "Cluster Count"));
          row = 5;
          for (int dx = exp_initDensity; dx <= exp_finalDensity; dx += exp_densityInterval) {
            String outputFileName = outputFolderName + File.separator
                                 + summaryFolderName + File.separator
                                 + "Mig" + form_00.format(mig)
                                 + "Dns" + form_00000.format(dx)
                                 + "_cluster_sum.txt";
            Scanner outputScan = null;
            outputScan = new Scanner(new BufferedReader(new FileReader(outputFileName)));
            resultSheet.addCell(new jxl.write.Number(col, row, outputScan.nextDouble()));
            resultSheet.addCell(new jxl.write.Number(col+2, row, outputScan.nextDouble()));
            resultSheet.addCell(new jxl.write.Number(col+3, row, outputScan.nextDouble()));
            if (outputScan != null) {
              outputScan.close();
            }
            row++;
          }
          col += 5;
        }
      } // end if (expMode == 5)
      */
      
      resultWorkbook.write();
      resultWorkbook.close();
    }
    catch (IOException ex) {
      ex.printStackTrace();
    }
    catch (jxl.write.WriteException wx) {
      wx.printStackTrace();
    }
  } // end transferClusterSummaryToExcel()
  
public void transferRuleSummaryToExcel() {
    File excelFolderFile = new File(outputFolderName + File.separator + excelFolderName);
    excelFolderFile.mkdirs();
    try {
      // transfer simulation output to Excel spreadsheet
      String outFileName = outputFolderName + File.separator + excelFolderName + File.separator
                                 + "RuleSummary.xls";
      WritableWorkbook resultWorkbook = Workbook.createWorkbook(new File(outFileName));
      for (int index = 1; index < 2; index++) {
        WritableSheet resultSheet = resultWorkbook.createSheet("rule_usage", index);
        resultSheet.addCell(new Label(0, 2, "*converted"));
        resultSheet.addCell(new Label(0, 4, "Density"));
        resultSheet.addCell(new Label(1, 4, "Density*"));
        resultSheet.addCell(new Label(3, 4, "ALC Size"));
        resultSheet.addCell(new Label(4, 4, "ALC Size*"));
        resultSheet.addCell(new Label(5, 4, "ALC Count"));
        int row = 5;
        for (int dx = exp_initDensity; dx <= exp_finalDensity; dx+= exp_densityInterval) {
          resultSheet.addCell(new jxl.write.Number(0, row, dx)); // write initial cell count
          String outputFileName = outputFolderName + File.separator
                                     + summaryFolderName + File.separator
                                     + "density" + form_00000.format(dx) + "_cluster_summary.txt";

          Scanner outputScan = null;
          outputScan = new Scanner(new BufferedReader(new FileReader(outputFileName)));
          resultSheet.addCell(new jxl.write.Number(3, row, outputScan.nextDouble()));
          resultSheet.addCell(new jxl.write.Number(5, row, outputScan.nextDouble()));
          if (outputScan != null) {
            outputScan.close();
          }
          row++;
        }
        resultWorkbook.write();
        resultWorkbook.close();
      }
    }
    catch (IOException ex) {
      ex.printStackTrace();
    }
    catch (jxl.write.WriteException wx) {
      wx.printStackTrace();
    }
  } // end transferRuleSummaryToExcel()

  public void copySourceCode() {
    
    String myHome = System.getProperty("user.home");
    File srcDir = new File(myHome + File.separator + "ALS_SIM" 
            + File.separator + "src" + File.separator + "als_sim2d");
    File tgtDir = new File(outputFolderName + File.separator + srcFolderName);
    if (srcDir.exists() && srcDir.isDirectory()) {
      try {
        File list[] = srcDir.listFiles();
        for (int i = 0; i < list.length; i++) {
          File curFile = list[i];
          if (!curFile.isDirectory()) {
            File targetFile = new File(tgtDir, curFile.getName());
            FileInputStream fis  = new FileInputStream(curFile);
            FileOutputStream fos = new FileOutputStream(targetFile);
            byte[] buf = new byte[1024];
            while(fis.read(buf) != -1) {
              fos.write(buf);
            }
            fis.close();
            fos.close();
          }
        }
      }
      catch (Exception e) {
        System.out.println("Error in writing out the source code");
      }
    }
  } // end copySourceCode()
  
  public void convertDataForR(int expMode) {
    try {
      
      String dataTableFileName = outputFolderName + File.separator
                 + summaryFolderName + File.separator
                 + "silico_data_all.txt";
      BufferedWriter dataTableWriter  = new BufferedWriter(new FileWriter(dataTableFileName));
      // dataTableWriter.write("density\tdiameter");
      // dataTableWriter.newLine();
      
      if (expMode == 2) {
        for (int dx = exp_initDensity; dx <= exp_finalDensity; dx+= exp_densityInterval) {
          // add up the number of clusters and cluster sizes
          double totalClusterCount = 0.0;
          double totalCystCount = 0.0;
          double totalCystSize = 0.0;
          int[][] totalRuleUsage = new int[exp_steps][exp_totalRuleNum];
          for (int i = 0; i < exp_steps; i++) {
            for (int j = 0; j < exp_totalRuleNum; j++) {
              totalRuleUsage[i][j] = 0;
            }
          }
          for (int rep = 1; rep <= exp_repetitions; rep++) {
            String clusterInfoFileName = outputFolderName + File.separator
                    + rawDataFolderName + File.separator
                    + "density" + form_00000.format(dx)
                    + "_rep" + form_000.format(rep) + "_cluster.txt";
            Scanner clusterInfoScan = null;

            clusterInfoScan = new Scanner(new BufferedReader(new FileReader(clusterInfoFileName)));
            double curCystSize;
            while (clusterInfoScan.hasNext()) {
              curCystSize = clusterInfoScan.nextDouble();
              totalClusterCount++;
              if (curCystSize > 0) {
                dataTableWriter.write(Integer.toString(dx) + "\t" + Double.toString(curCystSize));
                dataTableWriter.newLine();
              }
            }
            if (clusterInfoScan!= null) { clusterInfoScan.close(); }
          }
        }
      } // end if (expMode == 2)
      else if (expMode == 3) {
        for (int dx = exp_initDensity; dx <= exp_finalDensity; dx+= exp_densityInterval) {
          for (double jx = exp_cellUnattInit; jx <= exp_cellUnattFin; jx += exp_cellUnattIntv) {
             for (double kx = exp_cellAttInit; kx <= exp_cellAttFin; kx += exp_cellAttIntv) {
              for (int rep = 1; rep <= exp_repetitions; rep++) {
                String clusterInfoFileName = outputFolderName + File.separator
                        + rawDataFolderName + File.separator
                        + "Dns" + form_00000.format(dx)
                        + "_CeAdh" + form_0d00.format(jx)
                        + "_ClAdh" + form_0d00.format(kx)
                        + "_rep" + form_000.format(rep)
                        + "_cluster.txt";
                Scanner clusterInfoScan = null;

                clusterInfoScan = new Scanner(new BufferedReader(new FileReader(clusterInfoFileName)));
                double curCystSize;
                while (clusterInfoScan.hasNext()) {
                  curCystSize = clusterInfoScan.nextDouble();
                  if (curCystSize > 0) {
                    dataTableWriter.write(Integer.toString(dx) + "\t" + Double.toString(curCystSize));
                    dataTableWriter.newLine();
                  }
                }
                if (clusterInfoScan!= null) { clusterInfoScan.close(); }
              }
            }
          }
        }
      } // end if (expMode == 3)
      else if (expMode == 4) {
        dataTableWriter.write("size\tmode\tdensity");
        dataTableWriter.newLine();
        for (int mig = 0; mig < exp_numMigrationMode; mig++) {
          for (int dx = exp_initDensity; dx <= exp_finalDensity; dx+= exp_densityInterval) {
            if (dx != 200 && dx != 500 && dx != 1000 && dx != 2000 && dx != 5000) { continue; }
            for (int rep = 1; rep <= exp_repetitions; rep++) {
              String clusterInfoFileName = outputFolderName + File.separator
                      + rawDataFolderName + File.separator
                      + "Mig" + form_00.format(mig)
                      + "Dns" + form_00000.format(dx)
                      + "_rep" + form_000.format(rep)
                      + "_cluster.txt";
              Scanner clusterInfoScan = null;

              clusterInfoScan = new Scanner(new BufferedReader(new FileReader(clusterInfoFileName)));
              double curCystSize;
              while (clusterInfoScan.hasNext()) {
                curCystSize = clusterInfoScan.nextDouble();
                if (curCystSize > 0) {
                  dataTableWriter.write (Double.toString(curCystSize)
                                           + "\t" + Integer.toString(mig)
                                           + "\t" + Integer.toString(dx));
                  dataTableWriter.newLine();
                }
              }
              if (clusterInfoScan!= null) { clusterInfoScan.close(); }
            }
          }
        }
      } // end if (expMode == 4)
      if (dataTableWriter != null) { dataTableWriter.close(); }
    }
    catch (FileNotFoundException ex) {
      ex.printStackTrace();
    }
    catch (IOException ex){
     ex.printStackTrace();
    }
  }
  
} // end class ExpManager
