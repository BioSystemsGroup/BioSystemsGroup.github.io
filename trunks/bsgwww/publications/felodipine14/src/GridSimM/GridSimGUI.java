package GridSimM;

import java.io.*;
import java.util.Enumeration;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import ec.util.*;
import sim.display.*;
import sim.engine.*;
import sim.util.gui.*;
import sim.util.Double2D;
import sim.portrayal.grid.*;
// JFreeChart imports
import org.jfree.chart.JFreeChart;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.ChartFrame;
import org.jfree.chart.plot.PlotOrientation;

public class GridSimGUI extends GUIState {
  
  int gridWidth = 100;
  int gridHeight = 100;
  boolean semilogPlotOn = false;
  
  Display2D dosageGridDisplay, gastroGridDisplay, plasmaGridDisplay, reservoirGridDisplay;
  JFrame dosageGridFrame, gastroGridFrame, plasmaGridFrame, reservoirGridFrame, fileSelectFrame;
  JFileChooser dFileChooser, oFileChooser, simInfoFileChooser, simDataFileChooser;
  JButton dFileButton, oFileButton, simInfoFileButton, simDataFileButton;
  File dInputFile, oInputFile, simInfoFile, simDataFile;
  Label dissOcc1SMLab, dissOcc2SMLab, obsOcc1SMLab, obsOcc2SMLab;
  JTextField dissOcc1SMText, dissOcc2SMText, obsOcc1SMText, obsOcc2SMText;
  Label dissOcc1OkLab, dissOcc2OkLab, obsOcc1OkLab, obsOcc2OkLab;
  JTextField dissOcc1OkText, dissOcc2OkText, obsOcc1OkText, obsOcc2OkText;
  java.text.DecimalFormat df = new java.text.DecimalFormat("#.###");

  FastValueGridPortrayal2D dosageGridPortrayal = new FastValueGridPortrayal2D("Dosage");
  FastValueGridPortrayal2D gastroGridPortrayal = new FastValueGridPortrayal2D("GI Tract");
  FastValueGridPortrayal2D plasmaGridPortrayal = new FastValueGridPortrayal2D("Plasma");
  FastValueGridPortrayal2D reservoirGridPortrayal = new FastValueGridPortrayal2D("Reservoir");

  // For time-series chart
  XYSeries dosageSeries, gastroSeries, plasmaSeries, elimSeries, reservoirSeries;
  XYSeries dissOcc1Series, dissOcc2Series, obsOcc1Series, obsOcc2Series;
  JFrame dissChartFrame, obsChartFrame;
  JFreeChart dissChart, obsChart;
  
  public GridSimGUI() {
    super(new GridSim());
  }
  
  public GridSimGUI(SimState state) {
    super(state);
  }
  
  public GridSimGUI(int width, int height) {
    super(new GridSim());
    gridWidth = width;
    gridHeight = height;
  }
  
  public GridSimGUI(ParameterDatabase params) {
    super(new GridSim(new MersenneTwisterFast(System.currentTimeMillis()), new Schedule(), params));
  }
  
  public void setGridSize(int width, int height) {
    gridWidth = width;
    gridHeight = height;
  }

  public Object getSimulationInspectedObject() { return state; }
  
  public void setupPortrayals() {
    ColorMap map = new sim.util.gui.SimpleColorMap
          (0,((GridSim)state).getMaxConcentration(),new java.awt.Color(255,0,0,0),new java.awt.Color(255,0,0,255));
    
    if (((GridSim)state).dosageGrid != null) {
      dosageGridPortrayal.setField(((GridSim)state).dosageGrid);
      dosageGridPortrayal.setMap(map);
      dosageGridDisplay.reset();
      dosageGridDisplay.repaint();
    }
    if (((GridSim)state).gastroGrid != null) {
      gastroGridPortrayal.setField(((GridSim)state).gastroGrid);
      gastroGridPortrayal.setMap(map);
      gastroGridDisplay.reset();
      gastroGridDisplay.repaint();
    }
    if (((GridSim)state).plasmaGrid != null) {
      plasmaGridPortrayal.setField(((GridSim)state).plasmaGrid);
      plasmaGridPortrayal.setMap(map);
      plasmaGridDisplay.reset();
      plasmaGridDisplay.repaint();
    }
    if (((GridSim)state).reservoirGrid != null) {
      reservoirGridPortrayal.setField(((GridSim)state).reservoirGrid);
      reservoirGridPortrayal.setMap(map);
      reservoirGridDisplay.reset();
      reservoirGridDisplay.repaint();
    }
    if (dissChartFrame != null) { dissChartFrame.repaint(); }
    if (obsChartFrame != null) { obsChartFrame.repaint(); }
  } // end setupPortrayals()
  
  public void setupTSChart() {
    // time series chart frame window and scheduling
    GridSim model = (GridSim)state;
    semilogPlotOn = model.getSemilogOn();
    if (dosageSeries != null) { dosageSeries.clear(); }
    if (gastroSeries != null) { gastroSeries.clear(); }
    if (plasmaSeries != null) { plasmaSeries.clear(); }
    if (elimSeries != null) { elimSeries.clear(); }
    if (reservoirSeries != null) { reservoirSeries.clear(); }
    if (dissOcc1Series != null) { dissOcc1Series.clear(); }
    if (dissOcc2Series != null) { dissOcc2Series.clear(); }
    if (obsOcc1Series != null) { obsOcc1Series.clear(); }
    if (obsOcc2Series != null) { obsOcc2Series.clear(); }
    
    if (model.getDosageOn() && model.getOcc1On() && dissOcc1Series != null) {
      Enumeration e = model.getRefDataFractEnum(DataProcessor.DISS, DataProcessor.OCC1);
      while (e != null && e.hasMoreElements()) {
        Double2D curval = (Double2D)e.nextElement();
        if (semilogPlotOn && curval.y != 0) { dissOcc1Series.add(curval.x, Math.log(curval.y), true); }
        else { dissOcc1Series.add(curval.x, curval.y, true); }
        // System.out.println("Diss Occ1 value: " + curval.x + ", " + curval.y);
      }
    }
    if (model.getDosageOn() && model.getOcc2On() && dissOcc2Series != null) {
      Enumeration e = model.getRefDataFractEnum(DataProcessor.DISS, DataProcessor.OCC2);
      while (e != null && e.hasMoreElements()) {
        Double2D curval = (Double2D)e.nextElement();
        if (semilogPlotOn && curval.y != 0) { dissOcc2Series.add(curval.x, Math.log(curval.y), true); }
        else { dissOcc2Series.add(curval.x, curval.y, true); }
        // System.out.println("Diss Occ2 value: " + curval.x + ", " + curval.y);
      }
    }
    if (model.getPlasmaOn() && model.getOcc1On() && obsOcc1Series != null) {
      Enumeration e = model.getRefDataFractEnum(DataProcessor.OBS, DataProcessor.OCC1);
      while (e != null && e.hasMoreElements()) {
        Double2D curval = (Double2D)e.nextElement();
        if (semilogPlotOn && curval.y != 0) { obsOcc1Series.add(curval.x, Math.log(curval.y), true); }
        else { obsOcc1Series.add(curval.x, curval.y, true); }
        // System.out.println("Obs Occ1 value: " + curval.x + ", " + curval.y);
      }
    }
    if (model.getPlasmaOn() && model.getOcc2On() && obsOcc2Series != null) {
      Enumeration e = model.getRefDataFractEnum(DataProcessor.OBS, DataProcessor.OCC2);
      while (e != null && e.hasMoreElements()) {
        Double2D curval = (Double2D)e.nextElement();
        if (semilogPlotOn && curval.y != 0) { obsOcc2Series.add(curval.x, Math.log(curval.y), true); }
        else { obsOcc2Series.add(curval.x, curval.y, true); }
        // System.out.println("Obs Occ2 value: " + curval.x + ", " + curval.y);
      }
    }     

    scheduleImmediateRepeat(true, new Steppable() { 
      public void step(SimState state) {
        double x = state.schedule.time() * ((GridSim)state).getXScale();
        double y_scale = ((GridSim)state).getYScale();
        double y = 0;
        if (((GridSim)state).getDosageOn() && dosageSeries != null) {
          y = ((GridSim)state).getDosageGridTotalFract();
          if (semilogPlotOn && y != 0) { dosageSeries.add(x, Math.log(y / y_scale), true); }
          else { dosageSeries.add(x, (y / y_scale), true); }
        }
        if (((GridSim)state).getGastroOn() && gastroSeries != null) {
          y = ((GridSim)state).getGastroGridTotalFract();
          if (semilogPlotOn && y != 0) { gastroSeries.add(x, Math.log(y / y_scale), true); }
          else { gastroSeries.add(x, (y / y_scale), true); }
          // System.out.println("GUI Time: " + (int)state.schedule.time() + " Gastro Fraction: " + GridSim.df4.format(y));
        }
        if (((GridSim)state).getPlasmaOn() && plasmaSeries != null) {
          y = ((GridSim)state).getPlasmaGridTotalFract();
          if (semilogPlotOn && y != 0) { plasmaSeries.add(x, Math.log(y / y_scale), true); }
          else { plasmaSeries.add(x, (y / y_scale), true); }
        }
        if (((GridSim)state).getElimOn() && elimSeries != null) {
          y = ((GridSim)state).getElimTotalFract();
          if (semilogPlotOn && y != 0) { elimSeries.add(x, Math.log(y / y_scale), true); }
          else { elimSeries.add(x, (y / y_scale), true); }
        }
        if (((GridSim)state).getReservoirOn() && reservoirSeries != null) {
          y = ((GridSim)state).getReservoirGridTotalFract();
          if (semilogPlotOn && y != 0) { reservoirSeries.add(x, Math.log(y / y_scale), true); }
          else { reservoirSeries.add(x, (y / y_scale), true); }
        }
      }
    });
  } // end setupChart()
  
  public void setupFileSelect() {
    
    if (simInfoFileButton != null) { simInfoFileButton = null; }
    if (simInfoFileChooser != null) { simInfoFileChooser = null; }
    if (simDataFileButton != null) { simDataFileButton = null; }
    if (simDataFileChooser != null) { simDataFileChooser = null; }
    if (dFileButton != null) { dFileButton = null; }
    if (dFileChooser != null) { dFileChooser = null; }
    if (oFileButton != null) { oFileButton = null; }
    if (oFileChooser != null) { oFileChooser = null; }
    
    // Simulation info output file
    simInfoFileButton = new JButton("Select sim info file");
    simInfoFileChooser = new JFileChooser();
    ActionListener siListener = new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        simInfoFileChooser.showSaveDialog(simInfoFileButton);
        simInfoFile = simInfoFileChooser.getSelectedFile();
        if (simInfoFile != null) {
          ((GridSim)state).setSimInfoFileName(simInfoFile.getAbsolutePath());
        }
      }
    };
    simInfoFileButton.addActionListener(siListener);
    simInfoFileButton.setEnabled(true);
    
    // Simulation data output file
    simDataFileButton = new JButton("Select sim data file");
    simDataFileChooser = new JFileChooser();
    ActionListener sdListener = new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        simDataFileChooser.showSaveDialog(simDataFileButton);
        simDataFile = simDataFileChooser.getSelectedFile();
        if (simDataFile != null) {
          ((GridSim)state).setSimDataFileName(simDataFile.getAbsolutePath());
        }
      }
    };
    simDataFileButton.addActionListener(sdListener);
    simDataFileButton.setEnabled(true);
    
    // Dissolution data file
    dFileButton = new JButton("Select dis data file");
    dFileChooser = new JFileChooser();
    ActionListener dListener = new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        dFileChooser.showOpenDialog(dFileButton);
        dInputFile = dFileChooser.getSelectedFile();
        if (dInputFile != null) {
          ((GridSim)state).setDissDataFileName(dInputFile.getAbsolutePath());
        }
      }
    };
    dFileButton.addActionListener(dListener);
    dFileButton.setEnabled(true);
    
    // Obs (plasma) data file
    oFileButton = new JButton("Select plas data file");
    oFileChooser = new JFileChooser();
    ActionListener oListener = new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        oFileChooser.showOpenDialog(oFileButton);
        oInputFile = oFileChooser.getSelectedFile();
        if (oInputFile != null) {
          ((GridSim)state).setObsDataFileName(oInputFile.getAbsolutePath());
        }
      }
    };
    oFileButton.addActionListener(oListener);
    oFileButton.setEnabled(true);
    
    // Print Similarity Measure values
    /*
    dissOcc1SMLab = new Label("Diss OCC1 SM:");
    dissOcc1SMText = new JTextField(df.format(0), 4);
    dissOcc1SMText.setEditable(false);
    dissOcc2SMLab = new Label("Diss OCC2 SM:");
    dissOcc2SMText = new JTextField(df.format(0), 4);
    dissOcc2SMText.setEditable(false);
    */
            
    obsOcc1SMLab = new Label("Obs OCC1 SM:");
    obsOcc1SMText = new JTextField(df.format(0), 4);
    obsOcc1SMText.setEditable(false);
    obsOcc2SMLab = new Label("Obs OCC2 SM: ");
    obsOcc2SMText = new JTextField(df.format(0), 4);
    obsOcc2SMText.setEditable(false);

    Container content = fileSelectFrame.getContentPane();
    content.removeAll();
    content.setBackground(Color.white);
    content.setLayout(new FlowLayout());
    content.add(simInfoFileButton); content.add(simDataFileButton);
    content.add(dFileButton); content.add(oFileButton);
    //content.add(dissOcc1SMLab); content.add(dissOcc1SMText);
    //content.add(dissOcc2SMLab); content.add(dissOcc2SMText);
    content.add(obsOcc1SMLab); content.add(obsOcc1SMText);
    content.add(obsOcc2SMLab); content.add(obsOcc2SMText);
    
  } // end setupFileSelect()
  
  public void start() {
    super.start();
    setupPortrayals();
    setupTSChart();
    scheduleImmediateRepeat(true, new Steppable() { 
      public void step(SimState state) {
        //dissOcc1SMText.setText(df.format(((GridSim)state).computeSimpleSM(DataProcessor.DISS, DataProcessor.OCC1)));
        //dissOcc2SMText.setText(df.format(((GridSim)state).computeSimpleSM(DataProcessor.DISS, DataProcessor.OCC2)));
        obsOcc1SMText.setText(df.format(((GridSim)state).computeSimpleSM(DataProcessor.OBS, DataProcessor.OCC1)));
        obsOcc2SMText.setText(df.format(((GridSim)state).computeSimpleSM(DataProcessor.OBS, DataProcessor.OCC2)));
      }
    });
    // System.out.println("GridSimGUI :: start() done");
  } // end start()
  
  public void init(Controller c) {
    super.init(c);
    ((sim.display.Console)c).setTitle("GridSimM");
    ((sim.display.Console)c).setLocation(700,0);
    
    setupPortrayals();
    dosageGridDisplay = new Display2D(300, 300, this, 1);
    gastroGridDisplay = new Display2D(300, 300, this, 1);
    reservoirGridDisplay = new Display2D(300, 300, this, 1);
    plasmaGridDisplay = new Display2D(300, 300, this, 1);
    
    dosageGridFrame = dosageGridDisplay.createFrame();
    dosageGridFrame.setTitle("Dosage");
    gastroGridFrame = gastroGridDisplay.createFrame();
    gastroGridFrame.setTitle("GI/Tissue Space A+B");
    reservoirGridFrame = reservoirGridDisplay.createFrame();
    reservoirGridFrame.setTitle("GI/Tissue Space C");
    plasmaGridFrame = plasmaGridDisplay.createFrame();
    plasmaGridFrame.setTitle("Plasma");
    fileSelectFrame = new JFrame("Simulation Info");
    
    c.registerFrame(dosageGridFrame);
    c.registerFrame(gastroGridFrame);
    c.registerFrame(reservoirGridFrame);
    c.registerFrame(plasmaGridFrame);
    c.registerFrame(fileSelectFrame);
    
    /*
    dosageGridFrame.setVisible(true);
    gastroGridFrame.setVisible(true);
    reservoirGridFrame.setVisible(true);
    plasmaGridFrame.setVisible(true);
    */
    
    dosageGridDisplay.attach(dosageGridPortrayal, "Dosage");
    gastroGridDisplay.attach(gastroGridPortrayal, "GI/Tissue Space A+B");
    reservoirGridDisplay.attach(reservoirGridPortrayal, "GI/Tissue Space C");
    plasmaGridDisplay.attach(plasmaGridPortrayal, "Plasma");

    dosageGridDisplay.setBackdrop(java.awt.Color.WHITE);
    gastroGridDisplay.setBackdrop(java.awt.Color.WHITE);
    reservoirGridDisplay.setBackdrop(java.awt.Color.WHITE);
    plasmaGridDisplay.setBackdrop(java.awt.Color.WHITE);
    
    dosageGridFrame.setLocation(10,0);
    gastroGridFrame.setLocation(10, 200);
    reservoirGridFrame.setLocation(10, 400);
    plasmaGridFrame.setLocation(10, 600);
    
    fileSelectFrame.setSize(340,140);
    fileSelectFrame.setLocation(700, 420);
    setupFileSelect();
    fileSelectFrame.setVisible(true);
    
    dissChart = createTSChart(DataProcessor.DISS);
    dissChartFrame = new ChartFrame("Dosage Chart", dissChart);
    c.registerFrame(dissChartFrame);
    dissChartFrame.setVisible(false);
    dissChartFrame.setSize(340,360);
    dissChartFrame.setLocation(330,400);
    
    obsChart = createTSChart(DataProcessor.OBS);
    obsChartFrame = new ChartFrame("Plasma Chart", obsChart);
    c.registerFrame(obsChartFrame);
    obsChartFrame.setVisible(true);
    obsChartFrame.setSize(340,360);
    obsChartFrame.setLocation(330,0);
    
    
    // System.out.println("GridSimGUI :: init() done");
  } // end init()

  public void load(SimState state) {
    super.load(state);
    setupPortrayals();
  } // end load()
  
  public void quit() {
    if (dosageGridFrame != null) {
      dosageGridFrame.dispose();
      dosageGridFrame = null;
    }
    if (gastroGridFrame != null) {
      gastroGridFrame.dispose();
      gastroGridFrame = null;
    }
    if (plasmaGridFrame != null) {
      plasmaGridFrame.dispose();
      plasmaGridFrame = null;
    }
    if (reservoirGridFrame != null) {
      reservoirGridFrame.dispose();
      reservoirGridFrame = null;
    }
    if (dissChartFrame != null) {
      dissChartFrame.dispose();
      dissChartFrame = null;
    }
    if (obsChartFrame != null) {
      obsChartFrame.dispose();
      obsChartFrame = null;
    }
    if (fileSelectFrame != null) {
      fileSelectFrame.dispose();
      fileSelectFrame = null;
    }
    dosageGridDisplay = null;
    gastroGridDisplay = null;
    plasmaGridDisplay = null;
    reservoirGridDisplay = null;
    if (dissChart != null) { dissChart = null; }
    if (obsChart != null) { obsChart = null; }
    super.quit();
  } // end quit()
  
  JFreeChart createTSChart(int categ) {
    // For time series chart
    XYSeriesCollection seriesDataset = new XYSeriesCollection();
    if (categ == DataProcessor.DISS) {
      dosageSeries = new XYSeries("Dosage");
      elimSeries = new XYSeries("Total Eliminated");
      dissOcc1Series = new XYSeries("Diss OCC1");
      dissOcc2Series = new XYSeries("Diss OCC2");
      seriesDataset.addSeries(dosageSeries);
      seriesDataset.addSeries(elimSeries);
      seriesDataset.addSeries(dissOcc1Series);
      seriesDataset.addSeries(dissOcc2Series);
    }
    else if (categ == DataProcessor.OBS) {
      gastroSeries = new XYSeries("GI/Tissue Space A+B");
      plasmaSeries = new XYSeries("Plasma");
      reservoirSeries  = new XYSeries("GI/Tissue Space C");
      obsOcc1Series = new XYSeries("Obs OCC1");
      obsOcc2Series = new XYSeries("Obs OCC2");
      seriesDataset.addSeries(plasmaSeries);
      seriesDataset.addSeries(gastroSeries);
      seriesDataset.addSeries(reservoirSeries);
      seriesDataset.addSeries(obsOcc1Series);
      seriesDataset.addSeries(obsOcc2Series);
    }

    JFreeChart tschart = ChartFactory.createXYLineChart(
        "Dose Fraction", "Time", "Fraction",
        seriesDataset, PlotOrientation.VERTICAL, true, true, false);
    XYPlot tsplot = (XYPlot)tschart.getPlot();
    XYItemRenderer tsrend = (XYItemRenderer)tsplot.getRenderer();
    if (categ == DataProcessor.DISS) {
      tsrend.setSeriesPaint(0, java.awt.Color.BLACK);
      tsrend.setSeriesPaint(1, java.awt.Color.GREEN);
      tsrend.setSeriesPaint(2, java.awt.Color.BLUE);
      tsrend.setSeriesPaint(3, java.awt.Color.RED);
    }
    else if (categ == DataProcessor.OBS) {
      tsrend.setSeriesPaint(0, java.awt.Color.BLACK);
      tsrend.setSeriesPaint(1, java.awt.Color.GREEN);
      tsrend.setSeriesPaint(2, java.awt.Color.CYAN);
      tsrend.setSeriesPaint(3, java.awt.Color.BLUE);
      tsrend.setSeriesPaint(4, java.awt.Color.RED);
    }
    
    return tschart;
  }
  
  
} // end class GridSimGUI
