/*
 * English version follows
 * 
 * Ce fichier fait partie du module QuebecMRNF de la plateforme CAPSIS 4.2.2.
 * Il est prot�g� par la loi sur le droit d'auteur (L.R.C.,cC-42) et par les
 * conventions internationales. Toute reproduction de ce fichier sans l'accord 
 * du minist�re des Ressources naturelles et de la Faune du Gouvernement du 
 * Qu�bec est strictement interdite.
 * 
 * Copyright (C) 2009 Gouvernement du Qu�bec 
 * 	Pour information, contactez Jean-Pierre Saucier, 
 * 			Minist�re des Ressources naturelles et de la Faune du Qu�bec
 * 			jean-pierre.saucier@mrnf.gouv.qc.ca
 *
 * This file is part of the QuebecMRNF module within Capsis 4.2.2 platform. It is 
 * protected by copyright law (L.R.C., cC-42) and by international agreements. 
 * Any reproduction of this file without the agreement of Qu�bec Ministry of 
 * Natural Resources and Wildlife is strictly prohibited.
 *
 * Copyright (C) 2009 Gouvernement du Qu�bec 
 * 	For further information, please contact Jean-Pierre Saucier,
 * 			Minist�re des Ressources naturelles et de la Faune du Qu�bec
 * 			jean-pierre.saucier@mrnf.gouv.qc.ca
 */
package quebecmrnf.weather;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import quebecmrnfutility.biosim.BioSimClient.BioSimVersion;
import quebecmrnfutility.biosim.ClimateVariables;
import quebecmrnfutility.biosim.ClimateVariables.Variable;
import repicea.simulation.covariateproviders.standlevel.GeographicalCoordinatesProvider;
import repicea.util.ObjectUtility;

public final class BioSimWeather {

	private static boolean sbDLLLoaded = false;

	private static final String EXTENSION_PATH = ObjectUtility.getTrueRootPath(BioSimWeather.class) + 
			"ext" + File.separator +
			"windows" + File.separator;

	private static final String BIOSIM_PATH = ObjectUtility.getPackagePath(BioSimWeather.class);
	
	private static Map<BioSimVersion, BioSimWeather> weatherGeneratorMap = new HashMap<BioSimVersion, BioSimWeather>();
	
	

	static {
		if(System.getProperty("os.arch").equals("x86")){
			System.load(EXTENSION_PATH + "SimWeatherWrap.dll");
			sbDLLLoaded = true;
		}
	};
	
	
	/**
	 * Private constructor to implement the singleton strategy
	 */
	private BioSimWeather(String dataWeatherDBFile) throws IOException {
		// first step
		String pathDll = EXTENSION_PATH + "TempGenLib.dll";
		File dllFile = new File(pathDll);
		if (!dllFile.exists()) {
			throw new IOException("File " + dllFile.toString() + "does not exist!");
		}
		setTempGenHelpPath(dllFile.getAbsolutePath());
		if (!getErrorStatus().isEmpty()) {
			return;
		}
			
		String m_StrNormalDBFilePath = BIOSIM_PATH + dataWeatherDBFile;
		File dbfFile = new File(m_StrNormalDBFilePath);
		if (!dbfFile.exists()) {
			throw new IOException("File " + dbfFile.toString() + " does not exist!");
		}
		setNormalDBFilePath(dbfFile.getAbsolutePath());
		if (!getErrorStatus().isEmpty()) {
			return;
		}
		
	}

	/**
	 * This method implements a singleton strategy. It is synchronized in case two threads would instantiate the singleton
	 * at the same time
	 * @return the instance
	 * @throws Exception
	 */
	private synchronized static BioSimWeather getInstance(BioSimVersion version) throws Exception {
		if (weatherGeneratorMap.get(version) == null) {
			String filename = null;
			switch(version) {
			case VERSION_1971_2000:
				filename = "Canada-USA 1971-2000.Normals";
			break;
			case VERSION_1981_2010:
				filename = "Canada-USA 1981-2010.Normals";
			break;
			}
			weatherGeneratorMap.put(version, new BioSimWeather(filename));
			if (!weatherGeneratorMap.get(version).getErrorStatus().isEmpty()) {
				Exception e = new Exception(weatherGeneratorMap.get(version).getErrorStatus());
				throw e;
			}
		}
//		if (weatherGenerator == null) {
//			weatherGenerator = new BioSimWeather("Canada-USA 1971-2000.Normals");
//			if (!weatherGenerator.getErrorStatus().isEmpty()) {
//				Exception e = new Exception(weatherGenerator.getErrorStatus());
//				throw e;
//			}
//		}
		return weatherGeneratorMap.get(version);
	}
	
//	/**
//	 * This method implements a singleton strategy. It is synchronized in case two threads would instanciate the singleton
//	 * at the same time
//	 * @return the instance
//	 * @throws Exception
//	 */
//	protected synchronized static BioSimWeather getInstance1981_2010() throws Exception {
//		if (weatherGenerator == null) {
//			if(!sbDLLLoaded){
//				throw new Exception("Can't load 32 bit BioSim dll with a 64 bit java jre");
//			}
//			weatherGenerator = new BioSimWeather("Canada-USA 1981-2010.Normals");
//			if (!weatherGenerator.getErrorStatus().isEmpty()) {
//				Exception e = new Exception(weatherGenerator.getErrorStatus());
//				throw e;
//			}
//		}
//		return weatherGenerator;
//	}
	
	// use this method to set the DLL path (ex: "c:\directory\TempGenHelp.dll")
	private native void setTempGenHelpPath(String strPath);
	
	// use this after each method call to know the status of the lib. 
	// Empty string means ok.
	private native String getErrorStatus();
	
	//****************************************************************************
	// Summary:     Initialize normals file path 
	//
	// Description:  set the normal file path (.normals)
	//
	// Input:      filePath : the file path of the normals file
	//
	// Output:     ERMsg : error msg
	//
	// Note:
	//****************************************************************************
	private native String setNormalDBFilePath(String filePath);
	
	//****************************************************************************
	// Summary: Initialize daily file path 
	//
	// Description: set the daily file path. 
	//				This is optionnal if you use only normals generation (ie year==0)
	//
	// Input:   filePath : the file path of the daily file (.dailyStations)
	//
	// Output:  ERMsg : error msg.
	//
	// Note:    To use daily simulation, year must be different from zero in the SetTGInput function.
	//****************************************************************************
	private native String setDailyDBFilePath(String filePath);
	
	//****************************************************************************
	// Summary:     Initialize target localisation(simulation point )
	//
	// Description:  set the target location. lat,lonelev,slope and orientation
	//
	// Input:   name: name of the simulation point 
	//	        lat: latitude in decimal degree of the point. negative lat for southern hemisphere.
	//	        lon: longitude in decimal degree of the point. negative lon for western hemisphere.
	//	        elev: elevation in meters.
	//	        slope: slope in %. 100% equals a slope of 45 degres.
	//	        orientation: orientation in degrees. 0 = north, 90 = east, 180 = south, 270 = west.
	//
	// Output:     
	//
	// Note:       
	//****************************************************************************
	private native void setTarget(String name, double lat, double lon, float elev, float slope, float orientation);
	
	/**
	 * This method sets the location of the point of interest before generating the climate variables.
	 * @param latitude in degree.decimal
	 * @param longitude in degree.decimal (don't forget the negative sign)
	 * @param elevation above see level in m
	 * @throws Exception
	 */
	protected void setLocation(double latitude, double longitude, float elevation) throws Exception {
		setTarget("none", latitude, longitude, elevation, 0f, 0f);
		
		setReplication((short) 10, true); // true : fixed seed
		if (!getErrorStatus().isEmpty()) {
			Exception e = new Exception(getErrorStatus());
			throw e;
//			return;
		}
		
		setTGInput((short) 0, (short) 0, (short) 8, (short) 8, (short) 0, "T P");
		if (!getErrorStatus().isEmpty()) {
			Exception e = new Exception(getErrorStatus());
			throw e;
//			return;
		}


		generate();
		if (!getErrorStatus().isEmpty()) {
			Exception e = new Exception(getErrorStatus());
			throw e;
		}
	}
	
	
	
	//****************************************************************************
	// Summary: Set replication
	//
	// Description: Set the number of replication.
	//
	// Input:   nbRep:  The number of replications. For normals simulations, this should be > 10.
	//			        See BioSIM documentation for more details.
	// Output:  
	//
	// Note:    After simulation( method Generate ), one result is create 
	//	          for each replication.
	//****************************************************************************
	private native void setReplication(short nbRep, boolean bFixedSeed);
	
	//****************************************************************************
	// Summary: Set parameters
	//
	// Description:  Set temporal and simulation parameters.
	//
	// Input:   year: the last year of the simulation. For normals simulations use 0.
	//	          nbYear: The number of years to simulate. 
	//	          nbNormalStation: The number of normal stations to find around a simulation point. 
	//	          nbDailyStation: The number of daily stations to find around a simulation point. 
	//	          albedoType:     Exposition correction. Only used if slope and orientation are not NULL.
	//	          bSimPpt: true to simulate precipitation, false otherwise.
	//
	// Output:  ERMsg : error msg
	//
	// Note:    To use daily simulation, year and daily file path must be supplied 
	//	          variable            min value   max value		default     Note
	//	          firstYear:          -998        2100			0			for normals simulation, firstYear must be <= 0
	//	          lastYear:           -998        2100			0			for normals simulation, lastYear must be <= 0
	//	          nbNormalStation:    1           20				8           
	//	          nbDailyStation:     1           20				8           Not used if lastYear = 0
	//	          AlbedoType:       NONE(0)  CONIFER_CANOPY(1) CONIFER_CANOPY(1)
	//	          category:           one or combinaison of "T P H WS"		T=temperatrue, P=precipitation, H=humidity and WS = windSpeed
	//****************************************************************************
	private native void setTGInput(short year, short nbYear, short nbNormalStation, short nbDailyStation, short albedoType, String cat);
	
	//****************************************************************************
	// Summary: Create results
	//
	// Description:  this function runs the simulator and creates results.
	//
	// Input:   
	//
	// Output:  ERMsg : error msg
	//
	// Note:    For each replication defined, Generate creates a results
	//****************************************************************************
	private native String generate();
	
	//****************************************************************************
	// Summary: Save results to output file
	//
	// Description:  this function save the results create in Generate to output files.
	//
	// Input:   outputFilePathVector: one file per replication.
	//
	// Output:  ERMsg : error msg
	//
	// Note:    The size of outputFilePathVector must be the same as the number of replication
	//****************************************************************************
	private native String save(String[] outputFilePathArray);
	
	//****************************************************************************
	// Summary: Get one value in the results.
	//
	// Description:  this function return one value from the results.
	//
	// Input:   r:	The replication. From 0 to nbReplication-1.
	//			y:	The yearIndex. From 0 to nbYear-1.
	//			jd:	The julianDay. From 0 to 365. 365 can be missing (ie -999)
	//			v:	the variable. See TVariable in the the header file.
	//
	// Output:  float : the value. Can be misiing value(ie -999)
	//
	// Note:    Generate must be call before
	//****************************************************************************
	private native float getValue(short r, short y, short jd, short v);
	
	//****************************************************************************
	// Summary: Get The mean of all years for a variable statitstic
	//
	// Description:  this function return a statistc from the results.
	//
	// Input:   var: the variable. See TVariable in the the header file.
	//			dailyStatType: the type of the daily statistic(ie MEAN, SUM). See TStat in the header file.
	//			annualStatType: the type of the daily statistic(ie MEAN, SUM). See TStat in the header file.
	//
	// Output:  double: the statistic
	//
	// Note:    Generate must be call before
	//			Example of statistic:
	//			The annual mean of minimum mean temperature: GetAllYearsStat(TMIN, MEAN, MEAN);
	//			The annual standard deviation of maximum mean temperature: GetAllYearsStat(TMAX, MEAN, STD_DEV);
	//			The annual mean of total precipitation: GetAllYearsStat(PRCP, SUM, MEAN);
	//			The annual coeficiant of variation of total precipitation: GetAllYearsStat(PRCP, SUM, COEF_VAR);
	//****************************************************************************
	private native double getAllYearsStat(short var, short dailyStatType, short annualStatType);

	/**
	 * This method is the entry point for biosim. For minimum annual temperature, the call should be </br>
	 * </br>
	 * {@code BioSimWeather.getInstance().getStatistic((short) 0, (short) 1, (short) 1);} </br>
	 * </br>
	 * For maximum annual temperature, it should be </br>
	 * </br>
	 * {@code BioSimWeather.getInstance().getStatistic((short) 1, (short) 1, (short) 1);} </br>
	 * </br>
	 * For mean annual precipitation, </br>
	 * </br>
	 * {@code BioSimWeather.getInstance().getStatistic((short) 2, (short) 2, (short) 1);}
	 * </br>
	 * 
	 * @param variable 0 is the minimum temperature, 1 is the maximum temperature, 2 is the precipitation
	 * @param dailyStatType 1 for the mean, 2 for the sum
	 * @param annualStatType 1 for the mean, 2 for the sum
	 * @return the value of the climate variable
	 * @throws Exception
	 */
	protected double getStatistic(short variable, short dailyStatType, short annualStatType) throws Exception {
		double result = getAllYearsStat(variable, dailyStatType, annualStatType);
		if (!getErrorStatus().isEmpty()) {
			Exception e = new Exception(getErrorStatus());
			throw e;
		}
		if (result == -9999999d) {
			throw new Exception("No climate variable found for this location!");
		}
		return result;
	}
	
	// no documentation was provided for this method.
	private native double getGrowingSeasonStat(short var, short dailyStatType, short annualStatType);
	

	/**
	 * This method returns the mean annual temperature and the mean annual precipitation using BIOSIM generator.
	 * @param latitudeDeg
	 * @param longitudeDeg
	 * @param elevationM
	 * @return a ClimateVariables instance
	 */
	public static synchronized ClimateVariables getClimateVariables(BioSimVersion version, String plotId, GeographicalCoordinatesProvider geoProvider) {
		ClimateVariables output = new ClimateVariables(plotId);
		
		try {
			BioSimWeather.getInstance(version).setLocation(geoProvider.getLatitudeDeg(), geoProvider.getLongitudeDeg(), (float) geoProvider.getElevationM());
			double maxTemp = BioSimWeather.getInstance(version).getStatistic((short) 1, (short) 1, (short) 1);
			double minTemp = BioSimWeather.getInstance(version).getStatistic((short) 0, (short) 1, (short) 1);
			double meanPrec = BioSimWeather.getInstance(version).getStatistic((short) 2, (short) 2, (short) 1);
			output.setVariable(Variable.MeanAnnualTempC, 0.5 * (maxTemp + minTemp));
			output.setVariable(Variable.MeanAnnualPrecMm,meanPrec);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return output;
	}
}
