package GridSimM;

import java.io.*;
import java.util.Vector;
import java.util.Random;
import java.util.Enumeration;
import ec.util.*;
import sim.engine.*;
import sim.util.*;
import sim.field.grid.*;

public class GridSim extends SimState {
    
  private boolean first_run_info = true; // indicates current simulation is the first run in GUI mode
  private boolean first_run_data = true; // indicates current simulation is the first run in GUI mode
  int step = 0; // simulation time step (cycle)
  
  DoubleGrid2D dosageGrid = null; // grid for dosage
  DoubleGrid2D dosage2Grid = null; // secondary grid for dosage (e.g., alternate formulation)
  DoubleGrid2D gastroGrid = null; // grid to represent gastrointestinal space
  DoubleGrid2D gastro2Grid = null; // secondary grid for GI space (e.g., stacked GI spaces)
  DoubleGrid2D plasmaGrid = null; // grid to represent plasma space
  DoubleGrid2D reservoirGrid = null; // grid connected to GI space
  DoubleGrid2D dosageGridTmp = null; // grid to facilitate diffusion method
  DoubleGrid2D dosage2GridTmp = null;
  DoubleGrid2D gastroGridTmp = null;
  DoubleGrid2D gastro2GridTmp = null;
  DoubleGrid2D plasmaGridTmp = null;
  DoubleGrid2D reservoirGridTmp = null;
  
  ParameterDatabase paramDB = null; // utility to read parameter files
  static final String outputFolderName = "sim_output"; // folder containing all simulation output
  String diss_data_file_name = ""; // reference dissolution data file
  String obs_data_file_name = ""; // reference obs data file
  String sim_data_file_name = ""; // output file recording fraction values at each time step
  String sim_info_file_name = ""; // output file recording parameter and similarity values
  DataProcessor dataprocessor = null;
  Vector dosageVector, gastroVector, plasmaVector, elimVector, reservoirVector; // dose fraction time series
  double elimTotal = 0.0;
  boolean dosage_on = false; // turn on or off dosage time series plot
  boolean gastro_on = false; // ditto for GI tract
  boolean plasma_on = true; // ditto for plasma
  boolean elim_on = false; // ditto for eliminated fraction
  boolean reservoir_on = false; // ditto for reservoir fraction
  int subject_id = 1; // targeted subject id from input data file
  boolean occ1_on = true;
  boolean occ2_on = true;
  double x_conv = 1.0; // hours per simulation cycle
  double y_conv = 120; // Conversion factor between dissolution and plasma concentrations
  boolean semilog_on = false;
  double var_param = -1; // parameter value changes

  // grid setup variables
  static final boolean TOROIDAL = true;
  int grid_width = 100;
  int grid_height = 100;
  
  // solute movement variables
  double init_dose = 10000; // initial value per dosageGrid site
  double formul_ratio = 1.0; // ratio between alternate formulations
  int dtog_delay = 1; // initial transfer delay (in time steps) from dosageGrid to gastroGrid
  double dtog_fract = 0.1; // fraction of dosageGrid content per site transferred
  double dtog_prob = 0.8; // fraction of dosageGrid sites involved in transfer to gastroGrid
  int d2tog_delay = 1; // secondary formulation dosage
  double d2tog_fract = 0.1;
  double d2tog_prob = 0.8;
  boolean stacked_gi = true; // multiple gastroGrid spaces if true
  double diffg_ratio = 1.0; // ratio between two heterogeneous GI spaces
  int gtop_delay = 0; // initial transfer delay from gastroGrid to plasmaGrid
  double gtop_fract = 0.1; // fraction of gastroGrid content per site transferred
  double gtop_prob = 0.8; // probability of transfer (or fraction involved) from gastroGrid sites to plasmaGrid
  int g2top_delay = 20; // secondary transfer delay from gastroGrid to plasmaGrid
  double g2top_fract = 0.1;
  double g2top_prob = 0.8;
  int gtor_delay = 0; // initial transfer delay from gastroGrid to reservoirGrid
  double gtor_fract = 0.0; // fraction transferred from gastroGrid to reservoirGrid
  double gtor_prob = 0.0; // probability of transfer from gastroGrid to reservoirGrid
  double rtop_fract = 0.0; // fraction transferred from reservoirGrid to gastroGrid 
  double rtop_prob = 0.0; // probability of transfer from reservoirGrid to gastroGrid
  int rtop_delay = 0; // transfer delay from reservoirGrid to gastroGrid
  int ptoe_delay = 0; // initial transfer delay in simulation time steps
  double ptoe_fract = 0.1; // fraction of plasmaGrid content per site eliminated
  double ptoe_prob = 0.8; // fraction of plasmaGrid sites involved in elimination
  
  // diffusion-related variables
  boolean diffusion_on = true;
  int diffusion_step_multiples = 2; // number of diffusion execution per simulation time step
  double evaporation_rate = 0.0; // aka loss rate
  double diffusion_rate = 0.1;
  double max_concentration = init_dose / (grid_width * grid_height); // for visualization (color scale) only
  public static java.text.DecimalFormat df3 = new java.text.DecimalFormat("#.###");
  public static java.text.DecimalFormat df4 = new java.text.DecimalFormat("#.####");
  public static final java.text.DecimalFormat form_00 = new java.text.DecimalFormat("00");
  
  /* ================== PARAMETERIZATION FUNCTIONS ======================== */
  public int getSubjectID() { return subject_id; }
  public void setSubjectID(int id) { subject_id = id; }
  public int getGridWidth() { return grid_width; }
  public void setGridWidth(int width) { if(width > 0 && width%2 == 0) grid_width = width; }
  public int getGridHeight() { return grid_height; }
  public void setGridHeight(int height) { if(height > 0 && height%2 == 0) grid_height = height; }
  public double getXScale() { return x_conv; }
  public void setXScale(double val) { x_conv = val; }
  public double getYScale() { return y_conv; }
  public void setYScale(double val) { y_conv = val; }
  public double getInitDose() { return init_dose; }
  public void setInitDose(double val) { init_dose = val; }
  //public double getFormulRatio() { return formul_ratio; }
  //public void setFormulRatio(double ratio) { if(ratio >= 0 && ratio <= 1) formul_ratio = ratio; }
  public int getDtoGDelay() { return dtog_delay; }
  public void setDtoGDelay(int delay) { dtog_delay = delay; }
  public double getDtoGFract() { return dtog_fract; }
  public void setDtoGFract(double fract) { dtog_fract = fract; }
  public double getDtoGProb() { return dtog_prob; }
  public void setDtoGProb(double prob) { dtog_prob = prob; }
  public double getDiffGRatio() { return diffg_ratio; }
  public void setDiffGRatio(double ratio) { if(ratio >= 0 && ratio <= 1) diffg_ratio = ratio; }
  //public int getD2toGDelay() { return d2tog_delay; }
  //public void setD2toGDelay(int delay) { d2tog_delay = delay; }
  //public double getD2toGFract() { return d2tog_fract; }
  //public void setD2toGFract(double fract) { d2tog_fract = fract; }
  //public double getD2toGProb() { return d2tog_prob; }
  //public void setD2toGProb(double prob) { d2tog_prob = prob; }
  public int getGtoCDelay() { return gtor_delay; }
  public void setGtoCDelay(int delay) { gtor_delay = delay; }
  public double getGtoCFract() { return gtor_fract; }
  public void setGtoCFract(double fract) { gtor_fract = fract; }
  public double getGtoCProb() { return gtor_prob; }
  public void setGtoCProb(double prob) { gtor_prob = prob; }
  public int getGAtoPDelay() { return gtop_delay; }
  public void setGAtoPDelay(int delay) { gtop_delay = delay; }
  public double getGAtoPFract() { return gtop_fract; }
  public void setGAtoPFract(double fract) { gtop_fract = fract; }
  public double getGAtoPProb() { return gtop_prob; }
  public void setGAtoPProb(double prob) { gtop_prob = prob; }
  public int getGBtoPDelay() { return g2top_delay; }
  public void setGBtoPDelay(int delay) { g2top_delay = delay; }
  public double getGBtoPFract() { return g2top_fract; }
  public void setGBtoPFract(double fract) { g2top_fract = fract; }
  public double getGBtoPProb() { return g2top_prob; }
  public void setGBtoPProb(double prob) { g2top_prob = prob; }
  public int getGCtoPDelay() { return rtop_delay; }
  public void setGCtoPDelay(int delay) { rtop_delay = delay; }
  public double getGCtoPFract() { return rtop_fract; }
  public void setGCtoPFract(double fract) { rtop_fract = fract; }
  public double getGCtoPProb() { return rtop_prob; }
  public void setGCtoPProb(double prob) { rtop_prob = prob; }
  public int getPtoEDelay() { return ptoe_delay; }
  public void setPtoEDelay(int delay) { ptoe_delay = delay; }
  public double getPtoEFract() { return ptoe_fract; }
  public void setPtoEFract(double fract) { ptoe_fract = fract; }
  public double getPtoEProb() { return ptoe_prob; }
  public void setPtoEProb(double prob) { ptoe_prob = prob; }
  public double getDisperseRate() { return diffusion_rate; }
  public void setDisperseRate(double rate) { if(rate >= 0.0) diffusion_rate = rate; }
  public double getEvapRate() { return evaporation_rate; }
  public void setEvapRate(double rate) { if(rate >= 0.0) evaporation_rate = rate; }
  public int getDisperseCount() { return diffusion_step_multiples; }
  public void setDisperseCount(int val) { if (val > 0) diffusion_step_multiples = val; }
  public boolean getStackedGI() { return stacked_gi; }
  public void setStackedGI(boolean val) { stacked_gi = val; }
  public boolean getDiffusionOn() { return diffusion_on; }
  public void setDiffusionOn(boolean val) { diffusion_on = val; }
  //public double getVarParam() { return var_param; }
  //public void setVarParam(double val) { if (val >= 0.0 && val <= 1.0) var_param = val; }
  public boolean getDosageOn() { return dosage_on; }
  public void setDosageOn(boolean val) { dosage_on = val; }
  public boolean getElimOn() { return elim_on; }
  public void setElimOn(boolean val) { elim_on = val; }
  public boolean getGastroOn() { return gastro_on; }
  public void setReservoirOn(boolean val) {reservoir_on = val; }
  public boolean getReservoirOn() { return reservoir_on; }
  public void setGastroOn(boolean val) { gastro_on = val; }
  public boolean getPlasmaOn() { return plasma_on; }
  public void setPlasmaOn(boolean val) { plasma_on = val; }
  public boolean getOcc1On() { return occ1_on; }
  public void setOcc1On(boolean val) { occ1_on = val; }
  public boolean getOcc2On() { return occ2_on; }
  public void setOcc2On(boolean val) { occ2_on = val; }
  
  boolean getSemilogOn() { return semilog_on; }
  void setSemilogOn(boolean val) { semilog_on = val; }
  double getMaxConcentration() { return max_concentration; }
  void setMaxConcentration(double max) { if(max >= 0.0) max_concentration = max; }
  
  String getSimDataFileName() { return sim_data_file_name; }
  void setSimDataFileName(String name) { sim_data_file_name = name; }
  String getSimInfoFileName() { return sim_info_file_name; }
  void setSimInfoFileName(String name) { sim_info_file_name = name; }
  String getDissDataFileName() { return diss_data_file_name; }
  void setDissDataFileName(String name) { diss_data_file_name = name; }
  String getObsDataFileName() { return obs_data_file_name; }
  void setObsDataFileName(String name) { obs_data_file_name = name; }
  
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
  
  void varyParams(double varFactor) {
    if (varFactor <= 0.0 || varFactor >= 1.0) {
      return;
    }
    Random random = new Random();
    // double transform = (random.nextDouble() - 0.5) * varFactor / 0.5; // [0, 1] --> [-0.5, 0.5] --> [-varFactor, varFactor]

    dtog_fract *= 1.0 + ((random.nextDouble() - 0.5) * varFactor / 0.5);
    if (dtog_fract > 1.0) { dtog_fract = 1.0; }
    dtog_prob *= 1.0 + ((random.nextDouble() - 0.5) * varFactor / 0.5);
    if (dtog_prob > 1.0) { dtog_prob = 1.0; }
    d2tog_fract *= 1.0 + ((random.nextDouble() - 0.5) * varFactor / 0.5);
    if (d2tog_fract > 1.0) { d2tog_fract = 1.0; }
    d2tog_prob *= 1.0 + ((random.nextDouble() - 0.5) * varFactor / 0.5);
    if (d2tog_prob > 1.0) { d2tog_prob = 1.0; }
    gtop_fract *= 1.0 + ((random.nextDouble() - 0.5) * varFactor / 0.5);
    if (gtop_fract > 1.0) { gtop_fract = 1.0; }
    gtop_prob *= 1.0 + ((random.nextDouble() - 0.5) * varFactor / 0.5);
    if (gtop_prob > 1.0) { gtop_prob = 1.0; }
    g2top_fract *= 1.0 + ((random.nextDouble() - 0.5) * varFactor / 0.5);
    if (g2top_fract > 1.0) { g2top_fract = 1.0; }
    g2top_prob *= 1.0 + ((random.nextDouble() - 0.5) * varFactor / 0.5);
    if (g2top_prob > 1.0) { g2top_prob = 1.0; }
    gtor_fract *= 1.0 + ((random.nextDouble() - 0.5) * varFactor / 0.5);
    if (gtor_fract > 1.0) { gtor_fract = 1.0; }
    gtor_prob *= 1.0 + ((random.nextDouble() - 0.5) * varFactor / 0.5);
    if (gtor_prob > 1.0) { gtor_prob = 1.0; }
    rtop_fract *= 1.0 + ((random.nextDouble() - 0.5) * varFactor / 0.5);
    if (rtop_fract > 1.0) { rtop_fract = 1.0; }
    rtop_prob *= 1.0 + ((random.nextDouble() - 0.5) * varFactor / 0.5);
    if (rtop_prob > 1.0) { rtop_prob = 1.0; }
    ptoe_fract *= 1.0 + ((random.nextDouble() - 0.5) * varFactor / 0.5);
    if (ptoe_fract > 1.0) { ptoe_fract = 1.0; }
    ptoe_prob *= 1.0 + ((random.nextDouble() - 0.5) * varFactor / 0.5);
    if (ptoe_prob > 1.0) { ptoe_prob = 1.0; }
    
  }

  /* ==================== END PARAMETERIZATION FUNCTIONS ================== */
  
  public GridSim() {
    super(new MersenneTwisterFast(System.currentTimeMillis()), new Schedule());
  }
  
  public GridSim(long seed) {
    super(new MersenneTwisterFast(seed), new Schedule());
  }
  
  public GridSim(String fileName) {
    super(new MersenneTwisterFast(System.currentTimeMillis()), new Schedule());
    sim_info_file_name = fileName;
  }
  
  public GridSim(String fileName, ParameterDatabase params) {
    super(new MersenneTwisterFast(System.currentTimeMillis()), new Schedule());
    sim_info_file_name = fileName;
    paramDB = params;
    loadParams();
  }
  
  public GridSim(MersenneTwisterFast random, Schedule schedule) {
    super(random, schedule);
  }
  
  public GridSim(MersenneTwisterFast random, Schedule schedule, ParameterDatabase params) {
    super(random, schedule);
    paramDB = params;
    loadParams();
  }
  
  public GridSim(MersenneTwisterFast random, Schedule schedule, ParameterDatabase params, String fileName) {
    super(random, schedule);
    paramDB = params;
    sim_data_file_name = fileName;
    loadParams();
    varyParams(var_param);
  }
  
  public void start() {
    super.start(); // clear out the schedule
    File outputFolderFile = new File(outputFolderName);
    outputFolderFile.mkdir();
    step = 0;
    elimTotal = 0.0;
    max_concentration = init_dose / (grid_width * grid_height);
    dosageGrid = new DoubleGrid2D(grid_width, grid_height, max_concentration * formul_ratio);
    dosage2Grid = new DoubleGrid2D(grid_width, grid_height, max_concentration * (1.0 - formul_ratio));
    gastroGrid = new DoubleGrid2D(grid_width, grid_height, 0.0);
    gastro2Grid = new DoubleGrid2D(grid_width, grid_height, 0.0);
    plasmaGrid = new DoubleGrid2D(grid_width, grid_height, 0.0);
    reservoirGrid = new DoubleGrid2D(grid_width, grid_height, 0.0);
    
    dosageVector = new Vector(100); // initial capacity of 100 elements
    gastroVector = new Vector(100);
    plasmaVector = new Vector(100);
    elimVector = new Vector(100);
    reservoirVector = new Vector(100);
    
    if (diffusion_on) {
      // set up diffusion-related items
      dosageGridTmp = new DoubleGrid2D(grid_width, grid_height, 0.0);
      DiffuserSq dosageDiffuser = new DiffuserSq(dosageGrid, dosageGridTmp, evaporation_rate, diffusion_rate);
      MultiStep dosageSteps = new MultiStep(dosageDiffuser, diffusion_step_multiples, false);
      schedule.scheduleRepeating(dosageSteps);
      
      dosage2GridTmp = new DoubleGrid2D(grid_width, grid_height, 0.0);
      DiffuserSq dosage2Diffuser = new DiffuserSq(dosage2Grid, dosage2GridTmp, evaporation_rate, diffusion_rate);
      MultiStep dosage2Steps = new MultiStep(dosage2Diffuser, diffusion_step_multiples, false);
      schedule.scheduleRepeating(dosage2Steps);
      
      gastroGridTmp = new DoubleGrid2D(grid_width, grid_height, 0.0);
      DiffuserSq gastroDiffuser = new DiffuserSq(gastroGrid, gastroGridTmp, evaporation_rate, diffusion_rate);
      MultiStep gastroSteps = new MultiStep(gastroDiffuser, diffusion_step_multiples, false);
      schedule.scheduleRepeating(gastroSteps);
      
      gastro2GridTmp = new DoubleGrid2D(grid_width, grid_height, 0.0);
      DiffuserSq gastro2Diffuser = new DiffuserSq(gastro2Grid, gastro2GridTmp, evaporation_rate, diffusion_rate);
      MultiStep gastro2Steps = new MultiStep(gastro2Diffuser, diffusion_step_multiples, false);
      schedule.scheduleRepeating(gastro2Steps);
      
      plasmaGridTmp = new DoubleGrid2D(grid_width, grid_height, 0.0);
      DiffuserSq bodyDiffuser = new DiffuserSq(plasmaGrid, plasmaGridTmp, evaporation_rate, diffusion_rate);
      MultiStep bodySteps = new MultiStep(bodyDiffuser, diffusion_step_multiples, false);
      schedule.scheduleRepeating(bodySteps);
      
      reservoirGridTmp = new DoubleGrid2D(grid_width, grid_height, 0.0);
      DiffuserSq reservDiffuser = new DiffuserSq(reservoirGrid, reservoirGridTmp, evaporation_rate, diffusion_rate);
      MultiStep reservSteps = new MultiStep(reservDiffuser, diffusion_step_multiples, false);
      schedule.scheduleRepeating(reservSteps);
    }
    
    class GridSimStep implements Steppable {
      public void step(SimState state) {
        GridSim curSim = (GridSim) state;
        dosageVector.add(new Double(getDosageGridTotalFract()));
        gastroVector.add(new Double(getGastroGridTotalFract()));
        plasmaVector.add(new Double(getPlasmaGridTotalFract()));
        elimVector.add(new Double(getElimTotalFract()));
        reservoirVector.add(new Double(getReservoirGridTotalFract()));

        // PLASMA -> ELIMINATION
        if (step >= ptoe_delay) {
          // Elimination from the plasma space
          elimTotal += withdrawGridVals(plasmaGrid, ptoe_fract, ptoe_prob);
        }
        
        // GASTRO (SPACE A+B) -> PLASMA
        if (step >= gtop_delay) {
          // Transfer from the GI space to plasma
          transferGridVals(gastroGrid, plasmaGrid, gtop_fract, gtop_prob);
        }
        if (step >= g2top_delay) {
          // Transfer from the GI space to plasma
          if (stacked_gi) {
            transferGridVals(gastro2Grid, plasmaGrid, g2top_fract, g2top_prob);
          }
          else {
            transferGridVals(gastroGrid, plasmaGrid, g2top_fract, g2top_prob);
          }
        }
        
        // RESERVOIR (SPACE C) -> PLASMA
        if (step >= rtop_delay) {
          // Transfer from the reservoir to plasma
          if (stacked_gi) {
            transferGridVals(reservoirGrid, plasmaGrid, rtop_fract * diffg_ratio, rtop_prob);
            transferGridVals(reservoirGrid, plasmaGrid, rtop_fract * (1.0 - diffg_ratio), rtop_prob);
          }
          else {
            transferGridVals(reservoirGrid, plasmaGrid, rtop_fract, rtop_prob);
          }
        }
        // GASTRO -> RESERVOIR (SPACE C)
        if (step >= gtor_delay) {
          transferGridVals(gastroGrid, reservoirGrid, gtor_fract, gtor_prob);
          if (stacked_gi) { transferGridVals(gastro2Grid, reservoirGrid, gtor_fract, gtor_prob); }
        }
        
        // DOSAGE -> GASTRO (SPACE A+B)
        if (step >= dtog_delay) {
          // Transfer from the dosage space to GI space
          if (stacked_gi) {
            transferGridVals(dosageGrid, gastroGrid, dtog_fract * diffg_ratio, dtog_prob);
            transferGridVals(dosageGrid, gastro2Grid, dtog_fract * (1.0 - diffg_ratio), dtog_prob);
          }
          else {
            transferGridVals(dosageGrid, gastroGrid, dtog_fract, dtog_prob);
          }
        }
        if (step >= d2tog_delay) {
          // Transfer from the dosage2 space to GI space
          if (stacked_gi) {
            transferGridVals(dosage2Grid, gastroGrid, d2tog_fract * diffg_ratio, d2tog_prob);
            transferGridVals(dosage2Grid, gastro2Grid, d2tog_fract * (1.0 - diffg_ratio), d2tog_prob);
          }
          else {
            transferGridVals(dosage2Grid, gastroGrid, d2tog_fract, d2tog_prob);
          }
        }
        curSim.step++;
      }
    }
    schedule.scheduleRepeating(new GridSimStep());
    dataprocessor = new DataProcessor(diss_data_file_name, obs_data_file_name);
    dataprocessor.setDissObsConv(y_conv);
    dataprocessor.readRefData(DataProcessor.DISS, subject_id);
    dataprocessor.readRefData(DataProcessor.OBS, subject_id);
    
   // System.out.println("GridSim :: start() done");
  } // end start()
  
  public void finish() {
    this.writeSimInfoToFile();
    this.writeDetailedDataToFile();
    if (dataprocessor != null) {
      dataprocessor = null;
    }
    super.finish();
    // System.out.println("GridSim :: finish() done");
  } // end finish()
  
  void writeSimInfoToFile() {
    if (sim_info_file_name == null || sim_info_file_name.length() == 0) { return; }
    try {
      BufferedWriter outFileWriter;
      File outFile = new File(sim_info_file_name);
      if (first_run_info) {
        outFileWriter = new BufferedWriter(new FileWriter(outFile, false));
        outFileWriter.write(
          "DtoG_D" + "\t" + "DtoG_F" + "\t" + "DtoG_P" + "\t" +
          "GtP_D" + "\t" + "GtP_D" + "\t" + "GtoP_F" + "\t" + "GtoP_P" + "\t" +
          "PtoE_D" + "\t" + "PtoE_F" + "\t" + "PtoE_P" + "\t" +
          "SM_DS_1" + "\t" + "SM_DS_2" + "\t" + "SM_OB_1" + "\t" + "SM_OB_2" + "\t" + "Cycle");
        outFileWriter.newLine();
        first_run_info = false;
      }
      else {
        outFileWriter = new BufferedWriter(new FileWriter(outFile, true));
      }
      outFileWriter.write( 
        dtog_delay + "\t" + dtog_fract + "\t" + dtog_prob + "\t" +
        gtop_delay + "\t" + gtop_delay + "\t" + gtop_fract + "\t" + gtop_prob + "\t" +
        ptoe_delay + "\t" + ptoe_fract + "\t" + ptoe_prob + "\t" +
        df3.format(computeSimpleSM(DataProcessor.DISS, DataProcessor.OCC1)) + "\t" +
        df3.format(computeSimpleSM(DataProcessor.DISS, DataProcessor.OCC2)) + "\t" +
        df3.format(computeSimpleSM(DataProcessor.OBS, DataProcessor.OCC1)) + "\t" +
        df3.format(computeSimpleSM(DataProcessor.OBS, DataProcessor.OCC2)) + "\t" +
        (int)this.schedule.time()
      );
      outFileWriter.newLine();
      outFileWriter.close();
    }
    catch (IOException e) {
      System.out.println("ERROR: GridSim::writeSimInfoToFile() writing error");
    }
  } // end writeSimInfoToFile()

  void writeDetailedDataToFile() {
    if (sim_data_file_name == null || sim_data_file_name.length() == 0) {
      // return;
    sim_data_file_name = outputFolderName + File.separator 
                          + "GridSimSubject" + form_00.format(subject_id) + ".txt";
    }
    try {
      BufferedWriter outFileWriter;
      if (first_run_data) {
        outFileWriter = new BufferedWriter(new FileWriter(sim_data_file_name, false));
        /*
        outFileWriter.write(
          "Time" + "\t" + "Dosage" + "\t" + "GI" + "\t" + "Reserv" + "\t" + "Plasma" + "\t" + "Elim" );
        outFileWriter.newLine();
        outFileWriter.newLine();
        */
        first_run_data = false;
      }
      else {
        outFileWriter = new BufferedWriter(new FileWriter(sim_data_file_name, false));
      }
      Object[] dosageArray = dosageVector.toArray();
      Object[] gastroArray = gastroVector.toArray();
      Object[] reservoirArray = reservoirVector.toArray();
      Object[] plasmaArray = plasmaVector.toArray();
      Object[] elimArray = elimVector.toArray();
      for (int i = 0; i < dosageArray.length; i++) {
        outFileWriter.write(
          df4.format(i * x_conv) + "\t" +
          ((Double)dosageArray[i]).doubleValue() + "\t" + 
          (((Double)gastroArray[i]).doubleValue() / y_conv) + "\t" +
          (((Double)reservoirArray[i]).doubleValue() / y_conv) + "\t" +
          (((Double)plasmaArray[i]).doubleValue() / y_conv) + "\t" +
          (((Double)elimArray[i]).doubleValue() / y_conv));
        outFileWriter.newLine();
      }
      outFileWriter.newLine();
      outFileWriter.newLine();
      outFileWriter.close();
    }
    catch (IOException e) {
      System.out.println("ERROR: GridSim::writeDeatiledDataToFile() writing error -- "+ sim_data_file_name);
    }
  } // end writeDetailedDataToFile()
  
  double getGridTotal(DoubleGrid2D source) {
    double retval = 0.0;
    if (source == null) { return retval; }
    int width = source.getWidth();
    int height = source.getHeight();
    for (int i = 0; i < width; i++) {
      for (int j = 0; j < height; j++) {
        retval += source.field[i][j];
      }
    }
    return retval;
  } // end getGridTotal()
  
  double getDosageGridTotalFract() {
    return (getGridTotal(dosageGrid) + getGridTotal(dosage2Grid)) / init_dose;
  }
  double getGastroGridTotalFract() {
    return (getGridTotal(gastroGrid) + getGridTotal(gastro2Grid)) / init_dose;
  }
  double getReservoirGridTotalFract() {
    return getGridTotal(reservoirGrid) / init_dose;
  }
  double getPlasmaGridTotalFract() {
    return getGridTotal(plasmaGrid) / init_dose;
  }
  double getElimTotalFract() {
    return elimTotal / init_dose;
  }
  
  Enumeration getRefDataEnum(int categ, int occ) {
    if (dataprocessor == null) { return null; }
    return dataprocessor.getRefDataEnum(categ, occ);
  }
  
  Enumeration getRefDataFractEnum(int categ, int occ) {
    if (dataprocessor == null) { return null; }
    return dataprocessor.getRefDataFractEnum(categ, occ);
  }
  
  void addGridVals(DoubleGrid2D target, double amount) {
    if (target == null || amount <= 0) { return; }
    int width = target.getWidth();
    int height = target.getHeight();
    for (int i = 0; i < width; i++) {
      for (int j = 0; j < height; j++) {
        target.field[i][j] += amount;
      }
    }
  } // end addGridVals()
  
  void transferGridVals(DoubleGrid2D source, DoubleGrid2D target, double fract, double prob) {
    if (source == null || target == null) { return; }
    int width = source.getWidth();
    int height = source.getHeight();
    if (width != target.getWidth() || height != target.getHeight()) {
      System.out.println("ERROR: GridSim::transferGridValues mismatching dimensions");
      return;
    }
    for (int i = 0; i < width; i++) {
      for (int j = 0; j < height; j++) {
        if (random.nextDouble() < prob) {
          target.field[i][j] += source.field[i][j] * fract;
          if (target.field[i][j] > max_concentration) { target.field[i][j] = max_concentration; }
          source.field[i][j] -= source.field[i][j] * fract;
          if (source.field[i][j] < 0) { source.field[i][j] = 0; }
        }
      }
    }
  } // end transferGridVals()
  
  double withdrawGridVals(DoubleGrid2D source, double fract, double prob) {
    if (source == null) { return 0.0;}
    double total = 0.0;
    int width = source.getWidth();
    int height = source.getHeight();
    for (int i = 0; i < width; i++) {
      for (int j = 0; j < height; j++) {
        if (random.nextDouble() < prob) {
          total += source.field[i][j] * fract;
          source.field[i][j] -= source.field[i][j] * fract;
        }
      }
    }
    return total;
  } // end withdrawGridvals()
  
  // Medium stringency check
  // Plasma: for 0-10h, all points <= 50% ref and 4+ values within 20% ref
  // Plasma: for 10-48h, all points <= 100% ref and 3+ values within 30% ref
  // Dissolution: up to 2 values away more than 50% ref
  String checkSimpleSM(int categ, int occ) {
    String finval = "n/a";
    Vector targetVector = null;
    if (dataprocessor == null) { return finval; }
    if (categ == DataProcessor.DISS) { targetVector = dosageVector; }
    else if (categ == DataProcessor.OBS) { targetVector = plasmaVector; }
    else { return finval; }
    Enumeration refEnum = dataprocessor.getRefDataFractEnum(categ, occ);
    if (refEnum == null) { return finval; }
    
    double bandOutA = 0.0; // Wide band (count ones outside the band)
    double bandInA = 0.0; // Narrow band (count ones inside the band)
    double bandOutB = 0.0;
    double bandInB = 0.0;
    
    if (categ == DataProcessor.DISS) {
      bandOutA = 0.5;
      bandOutB = 0.5;
    }
    if (categ == DataProcessor.OBS) {
	  /*
      bandOutA = 0.5; // more stringent for early phase
      bandInA = 0.2;
	   */
	  bandOutA = 1.0;
	  bandInA = 0.3;
      bandOutB = 1.0; // less stringent for later phase
      bandInB = 0.3;
    }
    int counterOutA = 0;
    int counterInA = 0;
    int counterOutB = 0;
    int counterInB = 0;
    int pointCountA = 0;
    int pointCountB = 0;
    int timeThresh = 10;
    while (refEnum.hasMoreElements()) {
      Double2D curPt = (Double2D)refEnum.nextElement();
      double refX = curPt.x;
      double refY = curPt.y;
      // compare values up to the current time step (converted to referent time unit h)
      if (refX > x_conv * (double)step) { break; }
      
      // Find index that best matches the target time point
      double minDiff = 1000; // Assume the max time value is 1000
      int simX = 0; // the vector index corresponding to the target time point
      for (int idx = 0; idx < targetVector.size(); idx++) {
        if (minDiff > Math.abs(x_conv * (double)idx - refX)) {
          minDiff = Math.abs(x_conv * (double)idx - refX);
          simX = idx;
        }
      }
      // Count points that lie outside or inside specified bands around the referent values
      double simY = ((Double)targetVector.get(simX)).doubleValue() / y_conv;
      if (refY != 0 && simY != 0) {
        // early phase
        if (refX <= timeThresh) {
          if (simY < refY * (1.0 - bandOutA) || simY > refY * (1.0 + bandOutA)) { counterOutA++; }
          if (simY >= refY * (1.0 - bandInA) && simY <= refY * (1.0 + bandInA)) { counterInA++; }
          pointCountA++;
        }
        // later phase
        else {
          if (simY < refY * (1.0 - bandOutB) || simY > refY * (1.0 + bandOutB)) { counterOutB++; }
          if (simY >= refY * (1.0 - bandInB) && simY <= refY * (1.0 + bandInB)) { counterInB++; }
          pointCountB++;
        }
      }
    }
    if (pointCountA + pointCountB > 0 && categ == DataProcessor.DISS) {
      if (counterOutA + counterOutB <= Math.min(2, (pointCountA + pointCountB))) { finval = "Yes"; }
      else { finval = "No"; }
    }
    else if (pointCountA + pointCountB > 0 && categ == DataProcessor.OBS) {
      //if (occ == DataProcessor.OCC1) System.out.println("CounterOutA: " + counterOutA + ", CounterOutB: " + counterOutB);
      /*
	  if (counterOutA == 0 && counterInA >= Math.min(4, pointCountA)
          && counterOutB == 0 && counterInB >= Math.min(3, pointCountB)) { finval = "Yes"; }
      else { finval = "No"; }
	   */
	  if (counterOutA == 0 && counterOutB == 0 && (counterInA + counterInB) >= Math.min(5, (pointCountA + pointCountB))) {
		finval = "Yes"; }
	  else {finval= "No"; }
    }
    return finval;
  } // end checkSimpleSM()
  
  double computeSimpleSM(int categ, int occ) {
    // Computes normalized, average % deviation referent data points
    double sm = 0.0;
    int points = 0;
    Vector targetVector = null;
    if (dataprocessor == null) { return sm; }
    if (categ == DataProcessor.DISS) { targetVector = dosageVector; }
    else if (categ == DataProcessor.OBS) { targetVector = plasmaVector; }
    else { return sm; }
    Enumeration refEnum = dataprocessor.getRefDataFractEnum(categ, occ);
    if (refEnum == null) { return sm; }
    while (refEnum.hasMoreElements()) {
      Double2D curPt = (Double2D)refEnum.nextElement();
      double refX = curPt.x;
      double refY = curPt.y;
      // Find index that best matches the target time point
      double minDiff = 1000; // Assume the max time value is 1000
      int simX = 0; // the vector index corresponding to the target time point
      for (int idx = 0; idx < targetVector.size(); idx++) {
        if (minDiff > Math.abs(x_conv * (double)idx - (double)refX)) {
          minDiff = Math.abs(x_conv * (double)idx - (double)refX);
          simX = idx;
        }
      }
      double simY = ((Double)targetVector.get(simX)).doubleValue() / y_conv;
      if (refY != 0) { sm = sm + Math.exp(-Math.abs((refY - simY)/refY)); }
      points++;
    }
    if (points != 0) { sm = sm / points; }
    // if (sm < 0) { return 0; }
    return sm;
  } // end computeSimpleSM()
  
} // end class GridSim
