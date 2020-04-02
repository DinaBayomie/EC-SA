package mangers;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

import alignment.AlignmentConnection;
import eventLog.CaseNotation;
import eventLog.Event;
import logGraphStructure.LGraph;

/**** This class contains functions that computes model fitness, rule support, time variance */
public class EnergyCostManager {

	ModelMananger model = null;
	ConcurrentHashMap<String, Double> eqRule = null;
	AlignmentConnection algConnection;

	public EnergyCostManager(ModelMananger model_) {
		this.model = model_;
		algConnection = new AlignmentConnection(model.pModel);
		}

	/**** Model fitness based on Alignment package 
	 * @throws Exception */

	public double getAlignmentCost(List<CaseNotation> LogCases) throws Exception {
		double alignmentCost = Integer.MAX_VALUE;
		try {
			Hashtable<String, Double> results = algConnection.doReplayAlignment(LogCases);
			// alignmentCost = results.get("Raw Fitness Cost");
			alignmentCost = results.get("Fitness");
		} catch (InterruptedException | ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();	throw e;
		}
		// call the alignment output
		// return the raw cost value
		// System.out.println(alignmentCost);
		LogCases = null;
		return alignmentCost;
	}
	public double getAlignmentCostwoRepeating(List<CaseNotation> LogCases) throws Exception {
		double alignmentCost = Integer.MAX_VALUE;
		try {
			Hashtable<String, Double> results = algConnection.doReplayAlignment(LogCases);
			 alignmentCost = results.get("Raw Fitness Cost");
		} catch (InterruptedException | ExecutionException e) {

			throw e;
		}
		LogCases = null;
		return alignmentCost;
	}

	/**** MSE as activity time variance */
	public double calcTimeDeviMSE(List<CaseNotation> LogCases) {
		double MSE = 0;

		int initCap = 50;
		if (model != null)
			initCap = model.countActivities();
		Hashtable<String, double[]> actExec = new Hashtable<String, double[]>(initCap);
		Hashtable<String, ArrayList<Double>> actExecDetails = new Hashtable<String, ArrayList<Double>>(initCap);
		for (CaseNotation ccase : LogCases) {
			// calculate delta with considering the complete time of the new event so the start activity has no execution duration
			for (int i = 1; i < ccase.caseSize(); i++) {
				String eventAct = ccase.getEventNames().get(i);// caseEvents.get(i).getActivity();
				if (actExec.get(eventAct) == null) {
					// 0- total duration, 1= nActEvents, 2= Avg of duration, 3- sum(x - mean)^2 // variance ]
					actExec.put(eventAct, new double[5]);
					actExecDetails.put(eventAct, new ArrayList<Double>());
				}
				// calculate the time elapse as e(i) - e(i-1) so as sequence dont search for the actual parent
				Event Ec = ccase.getEvents().get(i);

				Event Eo = ccase.getEvents().get(i - 1);

				double delta = calculateDelta(Eo, Ec);
				double[] data = actExec.get(eventAct);
				data[0] += delta; // Total time elapse of the activity over the log
				data[1]++; // Number of the events of that executed the activity
				actExecDetails.get(eventAct).add(delta);// actual time elapse of the current event
			}

		}
		// this part can be parmeterize [ to select which central tendency measure, we can use]
		// currently measure the mean
		for (String key : actExec.keySet()) {
			// there is events for this activity
			if (actExec.get(key)[1] > 0) {
				actExec.get(key)[2] = actExec.get(key)[0] / actExec.get(key)[1];
			}
		}
		// for each activity calculate sum((x-avg)^2)
		double MSENom = 0;
		double nEvents = -1;
		for (String key : actExecDetails.keySet()) {
			ArrayList<Double> durations = actExecDetails.get(key);
			double actMean = actExec.get(key)[2];
			nEvents += actExec.get(key)[1];
			for (double long1 : durations) {
				// double delta = long1 - actMean ;
				actExec.get(key)[3] += Math.pow((actMean - long1), 2);
				MSENom += actExec.get(key)[3];

			}
		}

		MSE = MSENom / nEvents;

		return MSE;
	}

	/*** Computes the different in hours */
	private double calculateDelta(Event e1, Event e2) {
		// long diff = e2.getTimestampD().getTime() - e1.getTimestampD().getTime();// as given
		// return diff;// diff in milisecond
		// the difference is in hours
		return (e2.getTime() - e1.getTime()) / 3600000.0;
	}

	public double computeFitness(LGraph lg) throws Exception {
		if (lg.getmFittness() == -1) {
			lg.setmFittness(this.getAlignmentCostwoRepeating(lg.getCases()));
//			lg.setmFittness(this.getAlignmentCostPNetReplayerAstar(lg.getCases()));
			
		}
		return lg.getmFittness();
	}

	public double computeMSE(LGraph lg) {
		if (lg.getActMSA() == -1) {
			lg.setActMSA(this.calcTimeDeviMSE(lg.getCases()));
		}
		return lg.getActMSA();
	}
}
