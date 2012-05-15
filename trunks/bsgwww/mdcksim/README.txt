* README.txt
* MDCK ISMA Installation and user guide.
* Copyright Jesse Engelberg (2010) and the BioSystems group, UCSF, under the GPL.

(For further information on CompuCell3D, please see http://www.compucell3d.org.)

Files included in ISMA.zip
README.txt: this file
CompuCell3D_3.2.1/: directory containing CompuCell3D and customized plug-in code.
mdck_project/: directory containing bash and python execution scripts and MySQL data analysis scripts.

-----

INTRODUCTION:

The MDCK ISMA is a research project designed for a specific purpose.  As a result a significant number of libraries are required and installation is not simple.  In addition tools for data analysis are customized and many files require editing to utilize the full capabilities of the simulation.


PRE-INSTALLATION:

The MDCK ISMA was implemented using version 3.2.1 of CompuCell3D, included with this project.  The prerequisite libraries required to run CC3D on linux are:
* C++ compiler
* QT4 (http://www.qtsoftware.com)
* Python 2.4 or higher but not 3.x (http://www.python.org)
* Swig 1.3 or higher (http://www.swig.org
* CMake 2.6 or higher (http://www.cmake.org)
In addition, to run the database required for the ISMA data collection the following libraries must be installed:
* MySQL 5.0 or higher (http://www.mysql.com)
* MySQL++ 3.2 or higher (http://tangentsoft.net/mysql++)


INSTALLATION:

Steps to install the code after library installation is complete:
1. Unzip ISMA.zip into your home directory or another location.
2. Within CompuCell3D_3.2.1, run "ccmake ." and edit the line "CMAKE_INSTALL_PREFIX" to the location of the new directory that the files will be installed into.  (This is not CompuCell3D_3.2.1.)
3. Edit other lines to point to the correct directories, especially those referring to sql++.
4. Press [c] to configure.  Scroll to the bottom of the resulting text to view error messages.  Press [e] when done viewing messages.
5. If there are no errors you will be able to press [g] to generate and exit.
6. Enter MySQL and create a user that has database creation privileges with the following commands.  The default user is testuser with password testpassword, but this can be changed:
   a. CREATE USER 'testuser'@'localhost' IDENTIFIED BY 'testpassword';
   b. GRANT ALL ON *.* TO 'testuser'@'localhost'; 
7. Edit the following files to contain the name of the database which the data will be stored in (but does not have to preexist), as well as the user and password you just created:
   a. CompuCell3D_3.2.1/CompuCell3D/plugins/Mdck/DBManager.cpp
   b. CompuCell3D_3.2.1/post_install_tasks/getfoldername.py
8. CD to CompuCell3D_3.2.1 and type "make"
9. From the same directory type "make install"
If there are problems that can't be resolved, type "make clean" and repeat steps 8 and 9.
10. Edit mdck_project/src/mdck2.py so the line "sys.path.append" points to the directory in which mdck2.py resides.
11. Edit mdck_project/src/mdck2.xml and mdck_project/src/mdck2_n1.xml so that the line <PIFName> contains the path to mdck_project/src/mdck2.pif.
12. Edit mdck_project/automdck_exp.sh so it points to the correct locations.
13. To test the code run automdck_full.sh <expnum> <simnumber> where expnum is the number of experiments and simnumber is the number of simulations you wish to run.  automdck_full.sh uses mdck2_n1.xml.
14. To verify that it works check the output of debug.txt and also see if the MySQL database has been created or modified.
15. You may need to remove the mdck_<simnumber> directories if you get an error message.
16. You may also need to clear out the database if errors persist.


RUNNING ISMA

Executing experiments:
1. Edit CompuCell3D/mdck2_n<expnum>.xml and select desired parameters, where <expnum> is the number of different parameter settings desired.  For a single experiment (with multiple runs) use mdck2.xml
2. Run "mdck_project/automdck_full.sh <expnum> <simnum>", where <simnum> is the number of simulations for each experiment.
3. For each <expnum>:
   a.  Within mysql, 
   b. run "call AddtoExperiments(<simnum>,'<comment>');", where <simnum> is the number of simulations in that experiment and <comment> is a description of that experiment.
   c. If AddtoExperiments is not loaded, within mysql, "source <homedir>/mdck_project/src/sql/loadexperiments.sql"
4. Output files will be set to the the following locations:
   a. CompuCell3D/mdck_<simulationnumber>/mdck2.xml: parameter settings for that simulation.
   b. CompuCell3D/mdck_<simulationnumber>/mdck2.xml.<simcycle>.png: image files taken at <simcycle>.
   c. CompuCell3D/debug.txt: current simulation debugging output.  Overwritten by subsequent simulations, but useful to check if an error has occurred.
   d. Data is output to the mysql database.


GENERATING DATA

How to generate data files:
1. Set desired parameters 
2. Execute simulation
3. Output data using MySQL
a. Within MySQL source mdck_project/src/sql/showexperiments.sql to get the current simulation number.
b. Edit mdck_project/src/sql/setexpnumber.sql to set the simulation number.
c. From outside MySQL: mysql mdckdb -u <user> -p  < getnewfullexperimental.sql > [output]
4. Transfer data using SCP
5. Analyze data using R
6. View image files


FILES:

Plugin files.  The following files are used for the MDCK plugin, and reside in CompuCell3D_3.2.1/CompuCell3D/plugins/Mdck:
* DBManager.h/.cpp:  initializes and writes to the MySQL database.
* MdckPlugin.h/.cpp: executes primary actions for Mdck plugin.
* Stock.h: specifies the MySQL database
* MdckParseData.h: connects the XML parameter listing to the plugin.
* MdckPluginProxy.cpp: describes Mdck plugin to plugin reader.
In addition the Connectivity plugin was modified to include tight junction code.  The following files in CompuCell3D_3.2.1/CompuCell3D/plugins/Connectivity were modified:
* ConnectivityEnergy.h/.cpp: defines the method for calculating energy penalties when connectivity or tight junction rules are violated.

The python script files used to run the simulation are located in mdck_project/src:
* mdck2.xml: specifies the parameters for running a single simulation
* mdck2_n<number>.xml: specifies the parameters for executing multiple simulations.
* mdck2.pif: specifies the dimensions of the initial CELL object.
* mdck2.py: initializes plugin
* mdckShotDescriptor.txt: specifies screenshots
* mdck2Steppables.py: defines steppables called through plugin
* utils.py: used for debugging python plugin

SQL Scripts:
* getdeathpercents.sql: Gets percentage of cells dying at each day.
* getmeandyingtimes.sql: Gets the mean amount of time that cells are dying when contacting and not contacting the matrix.
* getmeanpolartime.sql: Gets the mean time in days for the experiment that more cells are polarized than unpolarized.
* getnewcellcystratio.sql: Gets the ratio of cell area to cyst area, as well as lumen area, cell are, and cyst area for each simulation at each day. 
* getnewdisplaydata.sql: Summarizes quantitative mean data for each day.
* getnewfinalvalues.sql: Summarizes quanitative and lumen data for each day.
* getnewfinalvaluesw10.sql: Summarizes quantative and lumen data for each day, as well as an additional data point at 0.25 days.
* getnewfullexperimental.sql: Used to generate data for output.  Outputs all data for individual simulations for each day.
* getnewpropersinglelumens.sql: Generates mean lumen data at each day, including summary of which cells contact lumen and matrix.
* getoldfullexperimental.sql: Used to generate data for GM ISMA.  Works with older simulation results.
* getparamssilent.sql: Gets current simulation number and assigns it to variables, without outputting anything.  Used when outputing data to files.
* getparams.sql: Gets current simulation and assigns it to variables, displaying result.  Used when outputting data to the screen.
* getsimnumber.sql: Returns simulation numbers of all simulations.
* loadexperiments.sql: Loads the convenience function AddtoExperiments, which will add a simulation run to the experiments table.  This is only needed once.  To call AddtoExperiments: "call AddtoExperiments(<number of simulations>,'<comment>');" within MySQL.
* setexpnumber.sql: Edit to set the experiment number returned by getparams.sql and getparamssilent.sql. 
* showexperiments.sql: Used to display experiments and comments.


