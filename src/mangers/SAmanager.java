package mangers;

import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

import eventLog.ELog;
import logGraphStructure.LGraph;

/***
 * This class contains functions about 1- Energy cost function computing ( invoke model fitness, invoke rule fitness, invoke time measures) 2- acceptance prob 3- promote next iteration individual by applying the multiple level optimization 4- running SA
 **/
public class SAmanager {

	private final static Logger logger = Logger.getLogger(SAmanager.class);

	/*** Acceptance probability [ check the existence of other acceptance prob mechanics] **/

	private double acceptanceProb(double d, double e, double temp) {
		if (e < d) {
			return 1.0;
		}
		return Math.exp(-(e - d) / temp);
	}

	/**** Compare the energy cost between 2 individuals based on multiple level optimization and the acceptance prob */

	/*** Get the most fitting within the population individuals **/
	/*** Compute energy cost function in terms of multiple level objective */
	public LGraph getnIterIndvML(EnergyCostManager ecmanager, LGraph oldX, LGraph newX, double temp) {
		// System.out.println("Print old log");
		// System.out.println(oldX.getCasesEvents().toString());
		// System.out.println("Print new log");
		// System.out.println(newX.getCasesEvents().toString());

		logger.debug("getnIterIndvML-old x  ::  " + oldX.getCasesEvents().toString());
		logger.debug("getnIterIndvML-new x  ::  " + newX.getCasesEvents().toString());

		double oldFitness = Double.MAX_VALUE;
		double newFitness = Double.MAX_VALUE;
		try {
			oldFitness = ecmanager.computeFitness(oldX);

			newFitness = ecmanager.computeFitness(newX);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			logger.info(
					"-------------------------------error triggered in SAManager getnIterIndvML check error fie ----------------------------");
			logger.error("Exection in SSAManager getnIterIndvML  as follows", e);
		}
		// System.out.println("old fitness " + oldFitness);
		// System.out.println("new fitness " + newFitness);
		logger.debug("getnIterIndvML-old x fitness  ::  " + oldFitness);
		logger.debug("getnIterIndvML-new x fitness  ::  " + newFitness);

		if (newFitness > oldFitness) // fitness point of view not raw cost
			return newX;
		else if (newFitness == oldFitness) {
			double oldMSE = ecmanager.computeMSE(oldX);
			double newMSE = ecmanager.computeMSE(newX);
			// System.out.println("old MSE " + oldMSE);
			// System.out.println("new MSE " + newMSE);

			logger.debug("getnIterIndvML-old x MSE  ::  " + oldMSE);
			logger.debug("getnIterIndvML-new x MSE  ::  " + newMSE);
			if (oldMSE > newMSE)
				return newX;
			else if (acceptanceProb(oldMSE, newMSE, temp) >= Math.random())
				return newX;
		} else if (acceptanceProb(oldFitness, newFitness, temp) >= Math.random())
			return newX;

		return oldX;
	}

	public LGraph getBestIndvML(LGraph oldX, LGraph newX) {
		if (newX.getmFittness() > oldX.getmFittness())
			return newX;
		else if (newX.getmFittness() == oldX.getmFittness() && newX.getActMSA() >= oldX.getActMSA())
			return newX;
		return oldX;
	}

	/*** Run the SA and cooling schedule for population size of 1 with Log cooling schedule (multiple level objective ) [ //TODO implement several schedules] */
	public ELog runSAIterations(ELog unlabeledLog, ModelMananger model, ConcurrentHashMap<String, Double> eqRule_,
	 int maxStep, double T0) {
		try {

			System.out.println("start SA");
			CorrelationManager cmanager = new CorrelationManager(model, eqRule_);
			EnergyCostManager ecmanager = new EnergyCostManager(model);
			LGraph initLG = cmanager.buildLGraphBasedOnModel(unlabeledLog.getEvents(), 1, 0);
			LGraph xLG = initLG;
			// System.out.println("xLG init");
			// System.out.println(xLG.getCasesEvents().toString());

			logger.debug("runSAIterations-xLG init  ::  " + xLG.getCasesEvents().toString());

			LGraph bestGLG = initLG;
			Double temp = new Double(T0);

			// unlabeledLog.updateCIds(xLG);
			// unlabeledLog.writeELogCSV("test0");
			// System.out.println("saved initial LG");
			for (int step = 1; step < maxStep; step++) {

				// System.out.println("Building newIndividual" + step);
				logger.debug("xLG.getCases()" + xLG.getCases());
				LGraph newxLG = cmanager.buildNewLGraph(unlabeledLog.getEvents(), xLG, step);
				// System.out.println("newxlg");
				// System.out.println(newxLG.getCasesEvents().toString());
				logger.debug("runSAIterations-newxlg ::  " + newxLG.getCasesEvents().toString());

				// call energy cost function for both of them
				// compute model fitness and MSE
				unlabeledLog.updateCIds(newxLG);
				// unlabeledLog.writeELogCSV("testnew");
				LGraph selected = getnIterIndvML(ecmanager, xLG, newxLG, temp);
				// clone best
				try {
					bestGLG = getBestIndvML(bestGLG, newxLG);
					if (!xLG.equals(selected))
						xLG = selected.clone();
				} catch (CloneNotSupportedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				// cooling schedule
				temp = ((T0) / Math.log(1 + step));
			}
			unlabeledLog.updateCIds(bestGLG);
			System.out.println("finished iterations SA");
			logger.info("finish SA iterations");

		} catch (Exception e) {
			logger.info(
					"-------------------------------error triggered in SAManager runSAIterations check error fie ----------------------------");
			logger.error("Exection in SSAManager runSAIterations  as follows", e);
			throw e;
		}
		return unlabeledLog;
	}

}
