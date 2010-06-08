Supplementary Material

for

A Computational Approach to Understand In Vitro Alveolar Morphogenesis

Sean H. J. Kim, Wei Yu, Keith Mostov, Michael A. Matthay, and C. Anthony Hunt (a.hunt@ucsf.edu)


DISCLAIMER:

The following source codes are provided under GNU General Public Licence (www.gnu.org/licencses).  The software can be used and distributed freely but we are not liable for any loss or damage that might arise from its use.


SYSTEM ARCHITECTURE:

The model is a self-contained experimental system that comprises the core analogue and support components for experimentation and analysis.  Main system components are EXPERIMENT MANAGER, OBSERVER, CULTURE, CELL, CLUSTER, MATRIX and FREE SPACE.  Additional components that extended functionality include CULTURE graphical user interface (GUI) and DIFFUSER.  Refer to javadoc for detailed implementation descriptions of invididual components.


FILE DESCRIPTIONS:

Agent.java - A general agent class, which provides basic access methods for grid position information.

AlsMain.java - A wrapper class for main method

AlvCell.java - Alveolar type II (AT II) cell analogue, which implements all logic governing AT II behavior

Cell.java - A general CELL class, which represents biological cells of a non-specific type

Cluster.java - An agent class, which represents an aggregate of CELLS.  Provides methods for collective movement.

Culture.java - A simulation proper, which represents in vitro cell culture as a whole.  Maintains event schedule, and provides start/end methods for setting up intra-culture components.

CultureGUI.java - A graphical user interface module which provides live access to CULTURE during simulation

Diffuser.java - An optional module that simulates diffusion of an arbitrary number of factors in CULTURE

ExpManager.java - A top-level agent class, which executes and manages in silico experiments.

FreeSpace.java - A simple object class to represent an arbitrary sized region of devoid of cells and matrix

Matrix.java - A simple object class to represent an arbitrary sized region of extracellular matrix

Observer.java - An agent class which is used to take CULTURE measurements during simulation


REQUIRED LIBRARIES:

MASON Version 11 (http://cs.gmu.edu/~eclab/projects/mason)
Java Excel API (http://jexcelapi.sourceforge.net)
