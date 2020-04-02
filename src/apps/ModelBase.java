package apps;


import eventLog.ELog;
import mangers.ModelMananger;
import mangers.SAmanager;
import processModel.*;

import org.apache.log4j.Logger;

public class ModelBase {
	private final static Logger logger = Logger.getLogger(ModelBase.class);
	public static void main(String[] args) {
	try {
		// TODO Auto-generated method stub
//		BasicConfigurator.configure();
		System.out.println("start");
		logger.info("Start processing input data");
		// read event log
		ELog orglabeledLog = new ELog(args[0]);
		// orglabeledLog.writeELogCSV("test");

		// read model and check silent\
		
		PNMLSerTS ser = new PNMLSerTS();
		PetriNetTS pm = ser.parse(args[1]);
		ModelMananger modelManager = new ModelMananger(pm);

		logger.info("Finish processing input data");
		// String outputFileName = args[2];
		// String folderName = args[3];
		//
		// int popSize = 1;
		// if (args[4] != null && !args[4].isEmpty())
		// popSize = Integer.parseInt(args[4]);
		//
		 int maxStep = 50;
		 if (args[2] != null && !args[2].isEmpty())
		 maxStep = Integer.parseInt(args[2]);
		//
		// double T0 = 1000;
		// if (args[6] != null && !args[6].isEmpty())
		// T0 = Double.parseDouble(args[6]);
		//

		// check correlation

		logger.info("Start SA");
		SAmanager sa = new SAmanager();
		sa.runSAIterations(orglabeledLog, modelManager, null, maxStep,1000);
//		System.gc();
		orglabeledLog.writeELogCSV("testd");
		System.out.println("Done");

		logger.info("End of the correlation process");
	} catch (Exception e) {
	logger.info("-------------------------------error triggered check error fie ----------------------------");
	logger.error("Exection in main class as follows",e);
	
	}
	}

}
