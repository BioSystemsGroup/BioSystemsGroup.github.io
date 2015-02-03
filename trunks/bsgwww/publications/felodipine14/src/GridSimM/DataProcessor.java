package GridSimM;

import java.io.*;
import java.util.Vector;
import java.util.Enumeration;
import sim.util.Double2D;
import au.com.bytecode.opencsv.CSVReader;

public class DataProcessor {

  public static final int DISS = 0;
  public static final int OBS = 1;
  public static final int OCC1 = 0;
  public static final int OCC2 = 1;

  
  String diss_file_name = null;
  String obs_file_name = null;
  Vector dissOcc1Vector, dissOcc2Vector, obsOcc1Vector, obsOcc2Vector;
  Vector dissOcc1FractVector, dissOcc2FractVector, obsOcc1FractVector, obsOcc2FractVector;
  double total_dose_occ1 = 10000;
  double total_dose_occ2 = 10000;
  double diss_obs_conv = 100;
    
  public DataProcessor(String dissFileName, String obsFileName) {
    diss_file_name = dissFileName;
    obs_file_name = obsFileName;
    dissOcc1Vector = new Vector(100);
    dissOcc2Vector = new Vector(100);
    obsOcc1Vector = new Vector(100);
    obsOcc2Vector = new Vector(100);
    dissOcc1FractVector = new Vector(100);
    dissOcc2FractVector = new Vector(100);
    obsOcc1FractVector = new Vector(100);
    obsOcc2FractVector = new Vector(100);
  }
  
  public double getDissObsConv() { return diss_obs_conv; }
  public void setDissObsConv(double val) { diss_obs_conv = val; }
  
  public Vector getDissDataVector(int subject, int occ) {
    return null;
  }
  
  public void readRefData(int categ, int subjId) {
    File inFile = null;
    CSVReader csvReader = null;
    try {
      if (categ == DataProcessor.DISS) {
        inFile = new File(diss_file_name);
      }
      else if (categ == DataProcessor.OBS) {
        inFile = new File(obs_file_name);
      }
      // check if file exists and is not a directory
      if (!inFile.exists() || !inFile.isFile()) {
        return;
      }
      csvReader =  new CSVReader(new FileReader(inFile));
      if (csvReader == null) { return; }
      
      // Start parsing data
      String[] nextLine = null;
      String curToken = null;
      int subjIndex = 0;
      int concIndex = 0;
      int timeIndex = 0;
      int occIndex = 0;
      if ((nextLine = csvReader.readNext()) != null) {
        // Identify labels and indices
        for (int i = 0; i < nextLine.length; i++) {
          curToken = nextLine[i];
          if (curToken.matches(".*((?i)SUBJECT).*")) {
            // System.out.println("Match to SUBJECT");
            subjIndex = i;
          }
          else if (curToken.matches(".*((?i)CONC).*")) {
            // System.out.println("Match to CONC");
            concIndex = i;
          }
          else if (curToken.matches(".*((?i)TIME).*")) {
            // System.out.println("Match to TIME");
            timeIndex = i;
          }
          else if (curToken.matches("((?i)OCC).*")) {
            // System.out.println("Match to OCC");
            occIndex = i;
          }
        }
      }
      while ((nextLine = csvReader.readNext()) != null) {
        if (Integer.parseInt(nextLine[subjIndex].trim()) == subjId) {
          if (Integer.parseInt(nextLine[occIndex].trim()) == DataProcessor.OCC1) {
            double timeVal = Double.parseDouble(nextLine[timeIndex].trim());
            double concVal = Double.parseDouble(nextLine[concIndex].trim());
            if (categ == DataProcessor.DISS && timeVal == 0 && concVal > 0) {
              total_dose_occ1 = concVal;
            }
            if (categ == DataProcessor.DISS) {
              dissOcc1Vector.add(new Double2D(timeVal, concVal));
              dissOcc1FractVector.add(new Double2D(timeVal, concVal/total_dose_occ1));
            }
            else if (categ == DataProcessor.OBS) {
              obsOcc1Vector.add(new Double2D(timeVal, concVal));
              obsOcc1FractVector.add(new Double2D(timeVal, concVal/total_dose_occ1));
            }
          }
          else if (Integer.parseInt(nextLine[occIndex].trim()) == DataProcessor.OCC2) {
            double timeVal = Double.parseDouble(nextLine[timeIndex].trim());
            double concVal = Double.parseDouble(nextLine[concIndex].trim());
            if (categ == DataProcessor.DISS && timeVal == 0 && concVal > 0) {
              total_dose_occ2 = concVal;
            }
            if (categ == DataProcessor.DISS) {
              dissOcc2Vector.add(new Double2D(timeVal, concVal));
              dissOcc2FractVector.add(new Double2D(timeVal, concVal/total_dose_occ2));
            }
            else if (categ == DataProcessor.OBS) {
              obsOcc2Vector.add(new Double2D(timeVal, concVal));
              obsOcc2FractVector.add(new Double2D(timeVal, concVal/total_dose_occ2));
            }
          }
        }
      }
      csvReader.close();
    }
    catch (IOException e) {
      System.out.println("ERROR: DataProcessor::readRefData() failed to read data");
    }
  } // end readRefData()
  
  public Enumeration getRefDataEnum(int categ, int occ) {
    Vector targetVector = null;
    Enumeration e = null;
    if (categ == DataProcessor.DISS && occ == DataProcessor.OCC1) {
      targetVector = dissOcc1Vector;
    }
    else if (categ == DataProcessor.DISS && occ == DataProcessor.OCC2) {
      targetVector = dissOcc2Vector;
    }
    else if (categ == DataProcessor.OBS && occ == DataProcessor.OCC1) {
      targetVector = obsOcc1Vector;
    }
    else if (categ == DataProcessor.OBS && occ == DataProcessor.OCC2) {
      targetVector = obsOcc2Vector;
    }
    if (targetVector != null) { e = targetVector.elements(); }
    return e;
  } // end getRefDataEnum()
  
  public Enumeration getRefDataFractEnum(int categ, int occ) {
    Vector targetVector = null;
    Enumeration e = null;
    if (categ == DataProcessor.DISS && occ == DataProcessor.OCC1) {
      targetVector = dissOcc1FractVector;
    }
    else if (categ == DataProcessor.DISS && occ == DataProcessor.OCC2) {
      targetVector = dissOcc2FractVector;
    }
    else if (categ == DataProcessor.OBS && occ == DataProcessor.OCC1) {
      targetVector = obsOcc1FractVector;
    }
    else if (categ == DataProcessor.OBS && occ == DataProcessor.OCC2) {
      targetVector = obsOcc2FractVector;
    }
    if (targetVector != null) { e = targetVector.elements(); }
    return e;
  } // end getRefDataFractEnum()
  
  public double getRefDataValFractAt(int categ, int occ, double x) {
    double val = 0.0;
    Vector targetVector = null;
    Enumeration e = null;
    if (categ == DataProcessor.DISS && occ == DataProcessor.OCC1) {
      targetVector = dissOcc1Vector;
    }
    else if (categ == DataProcessor.DISS && occ == DataProcessor.OCC2) {
      targetVector = dissOcc2Vector;
    }
    else if (categ == DataProcessor.OBS && occ == DataProcessor.OCC1) {
      targetVector = obsOcc1Vector;
    }
    else if (categ == DataProcessor.OBS && occ == DataProcessor.OCC2) {
      targetVector = obsOcc2Vector;
    }
    if (targetVector != null) { e = targetVector.elements(); }
    while (e.hasMoreElements()) {
      Double2D curVal = (Double2D) e.nextElement();
      if (curVal.x == x) {
        val = curVal.y;
        break;
      }
    }
    // System.out.println("Ref data val: (" + x + ", " + val + ")");
    return val;
  } // end getRefDataValAt()
  
}
