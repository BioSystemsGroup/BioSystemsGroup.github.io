Supplementary Material

for

Computational Investigation of Epithelial Cell Dynamic Phenotype In Vitro

Sean H. J. Kim, Sunwoo Park, Keith Mostov, Jayanta Debnath, and C. Anthony Hunt (a.hunt@ucsf.edu)


DISCLAIMER:

The following source codes are provided under GNU General Public Licence (www.gnu.org/licencses).  The software can be used and distributed freely, but the authors are not liable for any loss or damage that might arise from its use.


SYSTEM ARCHITECTURE:

The model is a self-contained experimental system that comprises the core analogue and support components for experimentation and analysis.  Main system components are EXPERIMENT MANAGER, OBSERVER, CULTURE, CELL, MATRIX and FREE SPACE.  CULTURE graphical user interface (GUI) extends CULTURE functionalities.  Refer to javadoc for detailed implementation descriptions of invididual components.


FILE DESCRIPTIONS:

Agent.java - A general agent class, which provides basic access methods for grid position information.

Cell.java - A general CELL class, which represents biological cells of a non-specific type.

Culture.java - A simulation proper, which represents in vitro cell culture as a whole.  Maintains event schedule, and provides start/end methods for setting up intra-culture components.

CultureGUI.java - A graphical user interface module which provides live access to CULTURE during simulation.

EmbeddedGrid.java - A wrapper class, which encapsulates simulated 3D embedded cell culture.

ExpManager.java - A top-level agent class, which executes and manages in silico experiments.

FreeSpace.java - A simple object class to represent an arbitrary sized region of devoid of cells and matrix.

ISEA1_Cell.java - A specific CELL class, which represents unpolarized Madin-Darby canine kidney (MDCK) epithelial cells without convexity drive.

ISEA1_PCell.java - A specific CELL class, which represens polarized MDCK cells without convexity drive.

ISEA2_Cell.java - A specific CELL class, which represents unpolarized MDCK cells with convexity drive.

ISEA2_PCell.java - A specific CELL class, which represents polarized MDCK cells with convexity drive.

Matrix.java - A simple object class to represent an arbitrary sized region of extracellular matrix.

OverlayGrid.java - A wrapper class, which encapsulates simulated overlay cell culture.

SurfaceGrid.java - A wrapper class, which encapsulates simulated 2D surface cell culture.

SuspensionGrid.java - A wrapper class, which encapsulates simulated suspension cell culture.


REQUIRED LIBRARIES:

MASON Version 11 (http://cs.gmu.edu/~eclab/projects/mason)
Java Excel API (http://jexcelapi.sourceforge.net)


DEVELOPMENT PLATFORM:

NetBeans IDE 6.1 (http://www.netbeans.org)
