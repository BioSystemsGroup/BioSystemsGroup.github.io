package GridSimM;

import java.applet.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import sim.display.*;
import sim.display.GUIState.*;

/**
 *
 * @author huntlab
 */
public class GridSimMApplet extends Applet {
  
  // some random static stuff to force Display2D and Console classes to load, as they have
  // some important properties to set
  
  // private static boolean b;
  private static int i;
  private GridSimGUI model = null;
  static { i = Console.DEFAULT_WIDTH; }

  public static boolean isApplet = false;
  public void GridSimMApplet() { isApplet = true; }

  public String getName() { return "GridSimM"; }

  public void init() {
    final JButton button = new JButton("Start Simulation");
    setLayout(new BorderLayout());
    add(button, BorderLayout.CENTER);
    try {
      button.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent evt) {
          try { setupApplet(); }
          catch (Exception e) { doException(button,e); }
        }});
    }
    catch (Exception e) { doException(button,e); }
  }
        
  public void setupApplet() throws Exception {
    model = new GridSimM.GridSimGUI();
    sim.display.Console c = new sim.display.Console(model);
    c.setVisible(true);
  }
  
  public void doException(JComponent button, Exception e) {
    JTextArea text = new JTextArea();
    text.setText("" + e);
    JScrollPane scroll = new JScrollPane(text);
    if (button != null) remove(button);
    add(scroll,BorderLayout.CENTER);
    e.printStackTrace();        
  }
}
