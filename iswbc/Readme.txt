Supplementary Material

for

The Rules of Engagement Enabling Leukocyte Rolling, Activation, and Adhesion

Jon Tang and C. Anthony Hunt (a.hunt@ucsf.edu)


DISCLAIMER:

The following source codes are provided under GNU General Public Licence (www.gnu.org/licencses).  The software can be used and distributed freely, but the authors are not liable for any loss or damage that might arise from its use.


SYSTEM ARCHITECTURE:

Refer to javadoc for detailed implementation descriptions of invididual components.


FILE DESCRIPTIONS:

Leukocyte.java - A class that represents the Leukocyte

FlowChamber.java - A general class that represents a flow chamber that contains a SURFCE for LEUKOCYTE objects to interact

ICAM1Grid.java - A 2d Toroidal Hexagonal Grid for Ind_ICAM1 objects to diffuse on. 

RearForceController.java - A class that controls the RearForce throughout a simulation

Surface.java - A class that represents the surface of the flow chamber or endothelial surface (2D Toroidal Lattice grid)

SurfaceUnit.java - A class that represents the functional unit of the SURFACE

Grp_AdhesMolecules.java - A general class that represents a group of Adhesion Molecules

Grp_ChemokReceptors.java - A general class that represents a group of Chemokine Receptors

Grp_Chemoks.java - A general class that represents a group of chemokine molecules

Grp_CXCR2.java - A specific Grp_ChemokReceptors class that represents a group of CXCR2 chemokine receptors.

Grp_GROA.java - A specific Grp_Chemoks class that represents a group of GroAlpha chemokines

Grp_Integrins.java - A general class that represents a group of integrin molecules

Grp_PSelectin.java - A specific AdhesMolecules class that represents a group of P-selectin molecules

Grp_PSGL1.java - A specific AdhesMolecules class that represents a group of PSGL1 molecules

Grp_VCAM1.java - A specific AdhesMolecules class that represents a group of VCAM-1 molecules

Grp_VLA4.java - A specific AdhesMolecules class that represents a group of VLA-4 molecules

Ind_AdhesMolecule.java - A general class that represents an individual adhesion molecule

Ind_ICAM1.java - A specific Ind_AdhesMolecule class that represents an individual ICAM-1 molecule

Ind_LFA1.java - A specific Ind_AdhesMolecule class that represents an individual LFA-1 molecule

Ligand.java - A general class that represents a ligand (molecule that can form bonds)

LeukMem.java - A specific MEMBRANE class that represents the surface of the leukocyte membrane

LFA1Grid.java - A 2d Toroidal Hexagonal grid for Ind_LFA1 objects to diffuse on

Membrane.java - A 2D Toroidal Lattice Grid that represents the membrane of a cell

MemUnit.java - A class that represents a functional unit of the MEMBRANE

Constants.java - A class of constants used in the ISWBC1/2 model

ISWBC.java - the main class that builds and starts the simulation and records main output data



REQUIRED LIBRARIES:

Repast 3 (http://repast.sourceforge.net)
