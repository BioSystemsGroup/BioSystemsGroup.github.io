package alc_sim;

import sim.util.media.PngEncoder;
import java.io.*;
import ec.util.*;
import sim.display.*;
import sim.engine.*;
import sim.field.grid.*;
import sim.util.gui.*;
import sim.portrayal.*;
import sim.portrayal.grid.*;
import sim.portrayal.simple.*;


public class CultureGUI extends GUIState {
  
  public boolean diffusionGraphics = true;
  public Display2D cultureDisplay;
  public javax.swing.JFrame cultureFrame;
  public HexaObjectGridPortrayal2D cultureGridPortrayal= new HexaObjectGridPortrayal2D();
  // we have heat as EITHER hexagons OR rectangles.  The current portrayal is always
  // currentHeatPortrayal, which can be changed in the model inspector (see
  // getInspector() below)
  FastHexaValueGridPortrayal2D solutePortrayalSqr= new FastHexaValueGridPortrayal2D("Solutes");
  HexaValueGridPortrayal2D solutePortrayalHex = new HexaValueGridPortrayal2D("Solutes");
  HexaValueGridPortrayal2D currentSolutePortrayal = solutePortrayalHex;

  public CultureGUI() {
    super(new Culture());
  }
  
  public CultureGUI(int cellCount, int mode) {
    super(new Culture(cellCount, mode));
  }
  
  public CultureGUI(SimState state) {
    super(state);
  }
  
  public CultureGUI(ParameterDatabase params) {
    super(new Culture(new MersenneTwisterFast(System.currentTimeMillis()), new Schedule(), params));
  }
  
  public Object getSimulationInspectedObject() {
    return state;
  }
  
  /* Hexagon/Rectangle display code from HexaBugs */
  
  // Another approach to doing the Hexagon/Rectangle code is to create a class which has a single
  // readable, writable integer property called DisplayGridCellsAs.  If the property is set to 0 or 1
  // the object changes the heat portrayal appropriately just like was done above.  We make the
  // popup menu appear by providing a domain in the form of an array (for the values 0 and 1).
  // Then we just make an inspector which encapsulates the previous inspector and a new SimpleInspector
  // on an instance of this object.
  public class HexagonChoice {
    int cells = 0;
    public Object domDisplayGridCellsAs() { return new Object[] { "Hexagons", "Rectangles"}; }
    public int getDisplayGridCellsAs() { return cells; }
    public void setDisplayGridCellsAs(int val) {
      if (val == 0) {
        cells = val;
        currentSolutePortrayal = solutePortrayalHex;
      }
      else if (val == 1) {
        cells = val;
        currentSolutePortrayal = solutePortrayalSqr;
      }
      // reattach the portrayals
      cultureDisplay.detatchAll();
      cultureDisplay.attach(currentSolutePortrayal,"Solutes");
      cultureDisplay.attach(cultureGridPortrayal,"Culture");
      // redisplay
      if (cultureDisplay != null) cultureDisplay.repaint();
    }
  } // end class HexagonChoice
        
  public Inspector getInspector() {
    // we'll make a fancy inspector which has a nicely arranged update button and
    // two subinspectors.  In fact the inspector doesn't need an update button --
    // there's nothing that ever changes on update.  But since the inspector
    // has been declared non-volatile, just to be consistent, we'll add an update
    // button up top to show how it's done.  First we get our two subinspectors,
    // one for the hexagon choice menu and one for the model inspector proper.
        
    final Inspector originalInspector = super.getInspector();
    final SimpleInspector hexInspector = new SimpleInspector(new HexagonChoice(),this);
        
    // The originalInspector is non-volatile.  It's a SimpleInspector, which shows
    // its update-button automagically when non-volatile.  We WANT it to be non-volatile,
    // but not show its update button because that just updates the inspector and nothing
    // else.  So we declare the inspector to be volatile.  It won't matter because it'll
    // NEVER receive updateInspector() calls except via the outer inspector we're 
    // constructing next (which will be NON-volatile).
        
    originalInspector.setVolatile(true);
        
    // our wrapper inspector
    Inspector newInspector = new Inspector() {
      public void updateInspector() { originalInspector.updateInspector(); }  // don't care about updating hexInspector
    };
    newInspector.setVolatile(false);
        
    // NOW we want our outer inspector to be NON-volatile, but show an update button.
    // While SimpleInspectors add their own buttons automagically, plain Inspectors
    // do not.  Instead we have to add it manually.  We grab an update-button from
    // the inspector, put it in a box so it doesn't stretch when the inspector does.
    // And we want to move it in a bit border-wise because that's what the SimpleInspector
    // does.
                
    javax.swing.Box b = new javax.swing.Box(javax.swing.BoxLayout.X_AXIS) {
      public java.awt.Insets getInsets() { return new java.awt.Insets(2,2,2,2); }  // in a bit
    };
    b.add(newInspector.makeUpdateButton());
    b.add(javax.swing.Box.createGlue());

    // okay, great.  But we want the button to be up top, followed by the hex inspector,
    // and then the originalInspector taking up the rest of the room.  Sadly, there's
    // no layout manager to do that.  So we do it thus:

    javax.swing.Box b2 = new javax.swing.Box(javax.swing.BoxLayout.Y_AXIS);
    b2.add(b);
    b2.add(hexInspector);
    b2.add(javax.swing.Box.createGlue());
        
    // all one blob now.  We can add it at NORTH.

    newInspector.setLayout(new java.awt.BorderLayout());
    newInspector.add(b2,java.awt.BorderLayout.NORTH);
    newInspector.add(originalInspector,java.awt.BorderLayout.CENTER);

    return newInspector;
  }

  public void setupPortrayals() {
    HexagonalPortrayal2D alvCellPortrayal =
            new HexagonalPortrayal2D(java.awt.Color.GREEN, true);
    HexagonalPortrayal2D cellPortrayal = 
            new HexagonalPortrayal2D(java.awt.Color.GRAY, true);
    HexagonalPortrayal2D lumenPortrayal = 
            new HexagonalPortrayal2D(java.awt.Color.BLACK, true);
    HexagonalPortrayal2D matrixPortrayal = 
            new HexagonalPortrayal2D(new java.awt.Color((float)1, (float)1, (float)1, (float)0));
    cultureGridPortrayal.setField(((Culture) state).cultureGrid);
    cultureGridPortrayal.setPortrayalForClass(AlvCell.class, alvCellPortrayal);
    cultureGridPortrayal.setPortrayalForClass(Cell.class, cellPortrayal);
    cultureGridPortrayal.setPortrayalForClass(FreeSpace.class, lumenPortrayal);
    cultureGridPortrayal.setPortrayalForClass(Matrix.class, matrixPortrayal);
    
    // solute portrayals
    ColorMap map = new sim.util.gui.SimpleColorMap
                     (0,((Culture)state).max_solute_concentration,java.awt.Color.black,java.awt.Color.BLUE);
    if (((Culture)state).diffusionGrid != null) {
      solutePortrayalSqr.setField(((Culture)state).diffusionGrid);
      solutePortrayalSqr.setMap(map);
      solutePortrayalHex.setField(((Culture)state).diffusionGrid);
      solutePortrayalHex.setMap(map);
    }

  } // end setupPortrayals()
  
  public void start() {
    super.start();
    setupPortrayals();
    cultureDisplay.reset();
    cultureDisplay.repaint();
    System.out.println("CultureGUI :: start() done");
  } // end start()
  
  /** The ratio of the width of a hexagon to its height: 1 / Sin(60 degrees), otherwise known as 2 / Sqrt(3) */
  public static final double HEXAGONAL_RATIO = 2/Math.sqrt(3);

  public void init(Controller c) {
    super.init(c);
    // Culture culture = (Culture)state;
    
    // Make the Display2D.  We'll have it display stuff later.
        
    // Horizontal hexagons are staggered.  This complicates computations.  Thus
    // if  you have a M x N grid scaled to SCALE, then
    // your height is (N + 0.5) * SCALE
    // and your width is ((M - 1) * (3/4) + 1) * HEXAGONAL_RATIO * SCALE
        
    // You might need to adjust by 1 or 2 pixels in each direction to get circles 
    // which usually come out as circles and not as ovals.
        
    final double scale = 4;
    final double m = 100;
    final double n = 100;
    final int height = (int) ( (n + 0.5) * scale );
    final int width = (int) ( ((m - 1) * 3.0 / 4.0 + 1) * HEXAGONAL_RATIO * scale );
    cultureDisplay = new Display2D(width*1.5, height*1.5, this, 1);    
   // cultureDisplay = new Display2D(600,600,this,1);
    cultureFrame = cultureDisplay.createFrame();
    c.registerFrame(cultureFrame);
    cultureFrame.setVisible(true);
    if (diffusionGraphics) {
      cultureDisplay.attach(currentSolutePortrayal, "Solutes");
    }
    cultureDisplay.attach(cultureGridPortrayal, "Culture");
    cultureDisplay.setBackdrop(java.awt.Color.white);
    cultureFrame.setLocation(0,0);
    System.out.println("CultureGUI :: init() done");
  } // end init()

  public void load(SimState state) {
    super.load(state);
    setupPortrayals();
    cultureDisplay.reset();
    cultureDisplay.repaint();
  } // end load()
  
  public void quit() {
    super.quit();
    if(cultureFrame != null) {
      cultureFrame.dispose();
    }
    cultureFrame = null;
    cultureDisplay = null;
  } // end quit()
  
  public void captureImage(Culture model, String outputFileName) {
    if (model == null) {
      System.out.println("ERROR: CultureGUI::captureImage() has NULL model");
      return;
    }
    if (outputFileName == null) {
      System.out.println("ERROR: CultureGUI::captureImage() has NULL outputFileName");
      return;
    }
    try {
      this.load(model);
      java.awt.Graphics g = this.cultureDisplay.insideDisplay.getGraphics();
      this.cultureDisplay.setScale(1.0);
      java.awt.image.BufferedImage img = this.cultureDisplay.insideDisplay.paint(g, true);
      OutputStream stream = 
                          new BufferedOutputStream(new FileOutputStream(new File(outputFileName)));
      PngEncoder tmpEncoder = new PngEncoder(img, false,PngEncoder.FILTER_NONE, 9);
      stream.write(tmpEncoder.pngEncode());
      stream.close();
    }
    catch (IOException e) {
      System.out.println(e);
    }
    catch (IllegalArgumentException e) {
      System.out.println(e);
    }
    
  } // end captureImage()
  
} // end class CultureGUI
