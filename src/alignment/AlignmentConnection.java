package alignment;

import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import org.deckfour.xes.classification.XEventClass;
import org.deckfour.xes.classification.XEventClasses;
import org.deckfour.xes.classification.XEventClassifier;
import org.deckfour.xes.classification.XEventNameClassifier;
import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.factory.XFactory;
import org.deckfour.xes.factory.XFactoryRegistry;
import org.deckfour.xes.info.XLogInfo;
import org.deckfour.xes.info.XLogInfoFactory;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.jbpt.petri.NetSystem;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.PetrinetGraph;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.plugins.connectionfactories.logpetrinet.TransEvClassMapping;
import org.processmining.plugins.replayer.replayresult.SyncReplayResult;

import eventLog.CaseNotation;
import nl.tue.alignment.Replayer;
import nl.tue.alignment.ReplayerParameters;
import nl.tue.alignment.TraceReplayTask;
import nl.tue.alignment.TraceReplayTask.TraceReplayResult;
import nl.tue.alignment.algorithms.ReplayAlgorithm.Debug;
import processModel.PetrinetAdapter;

public class AlignmentConnection {

	private XFactory factory;
	private XConceptExtension conceptExt = XConceptExtension.instance();

	private PetrinetAdapter pNetAd;

	static XLogInfo summary;
	static TransEvClassMapping mapping;
	static XEventClasses classes;
	// contains all the traces that have been checked by the alignment before and the value is the raw cost of this trace
	// Hashtable<List<String>, Double> checkedTracesRawCost;
	ConcurrentHashMap<List<String>, Double> checkedTracesRawCost;
	// contains all the traces that have been checked by the alignment before and the value is the raw cost of this trace
	Hashtable<List<String>, Double> checkedTracesWorstCost;
	static int modelMoveOfEmptyTrace = 5;

	public AlignmentConnection(NetSystem net) {
		pNetAd = new PetrinetAdapter(net);
		checkedTracesRawCost = new ConcurrentHashMap<>();
		checkedTracesWorstCost = new Hashtable<>();
		// this.computeShorestMPath();
	}

	public XLog createLog(List<CaseNotation> LogCases) {

		factory = XFactoryRegistry.instance().currentDefault();
		XLog log = factory.createLog();
		log.getExtensions().add(conceptExt);

		CopyOnWriteArrayList<XTrace> traces = new CopyOnWriteArrayList<>();
		// creating the log based on the log based on the activities list per each case
		LogCases.parallelStream().forEach(c -> {
			if (c.caseSize() > 0) {
				XTrace trace = factory.createTrace();
				for (String eventA : c.getEventNames()) {
					XEvent event = factory.createEvent();
					conceptExt.assignName(event, eventA);
					trace.add(event);
				}
				traces.add(trace);
				checkedTracesRawCost.put(c.getEventNames(), Double.MAX_VALUE);
				checkedTracesWorstCost.put(c.getEventNames(), Double.MAX_VALUE);
			}
		});
		log.addAll(traces);
		XTrace emptyTrace = factory.createTrace();
		log.add(0, emptyTrace);
		return log;
	}

	public Hashtable<String, Double> doReplayAlignment(List<CaseNotation> LogCases)
			throws InterruptedException, ExecutionException {
		// have no clue why i need all this lets try to run first and then remove what is not needed
		XLog lxlog = createLog(LogCases);

		if (summary == null) {
			// XLog lxlog = createLog(LogCases);
			XEventClassifier eventClassifier = new XEventNameClassifier();
			XEventClass dummyEvClass = new XEventClass("DUMMY", 99999);
			summary = XLogInfoFactory.createLogInfo(lxlog, eventClassifier);
			mapping = constructMappingBasedOnLabelEquality(pNetAd.getpNet(), dummyEvClass, eventClassifier, summary);

			classes = summary.getEventClasses();
		}

		return doReplay(lxlog, pNetAd.getpNet(), pNetAd.getsMarking(), pNetAd.getfMarking(), classes, mapping);
		// return doReplayWithoutRepeating(LogCases, lxlog, pNetAd.getpNet(), pNetAd.getsMarking(), pNetAd.getfMarking(), classes, mapping);
		// return doReplay(lxlog, pNetAd.getpNet(), pNetAd.getsMarking(), pNetAd.getfMarking(), classes,
		// mapping, deviatedCases);
	}

	public Hashtable<String, Double> doReplay(XLog log, Petrinet net, Marking initialMarking, Marking finalMarking,
			XEventClasses classes, TransEvClassMapping mapping) {

		int nThreads = 2;
		int costUpperBound = Integer.MAX_VALUE;
		// timeout per trace in milliseconds
		int timeoutMilliseconds = 10 * 1000;

		ReplayerParameters parameters = 
//				new ReplayerParameters.IncrementalAStar(false, nThreads, false, Debug.DOT,
//				timeoutMilliseconds, Integer.MAX_VALUE, Integer.MAX_VALUE, false, false, 0,3);

		 new ReplayerParameters.Dijkstra(false, false, nThreads, Debug.NONE,
					timeoutMilliseconds, Integer.MAX_VALUE, Integer.MAX_VALUE, false, 2, true);

		// = new ReplayerParameters.Default();// (nThreads, costUpperBound, Debug.NONE);
		Replayer replayer = new Replayer(parameters, net, initialMarking, finalMarking, classes, mapping, false);

		// preprocessing time to be added to the statistics if necessary
		long preProcessTimeNanoseconds = 0;

		Double total_cost = new Double(0);
		int numerator = 0;
		int denominator = 0;

		ExecutorService service = Executors.newCachedThreadPool();// newFixedThreadPool(parameters.nThreads);

		@SuppressWarnings("unchecked")
		Future<TraceReplayTask>[] futures = new Future[log.size()];

		for (int i = 0; i < log.size(); i++) {
			// Setup the trace replay task
			TraceReplayTask task = new TraceReplayTask(replayer, parameters, log.get(i), i, timeoutMilliseconds,
					parameters.maximumNumberOfStates, preProcessTimeNanoseconds);

			// submit for execution
			futures[i] = service.submit(task);
		}
		// initiate shutdown and wait for termination of all submitted tasks.
		service.shutdown();

		// obtain the results one by one.

		for (int i = 0; i < log.size(); i++) {

			TraceReplayTask result;
			try {
				result = futures[i].get();
			} catch (Exception e) {
				// execution os the service has terminated.
				assert false;
				throw new RuntimeException("Error while executing replayer in ExecutorService. Interrupted maybe?", e);
			}
			switch (result.getResult()) {
				case DUPLICATE:
					assert false; // cannot happen in this setting
					throw new RuntimeException("Result cannot be a duplicate in per-trace computations.");
				case FAILED:
					// internal error in the construction of synchronous product or other error.
					throw new RuntimeException("Error in alignment computations");
				case SUCCESS:
					if (result.getResult() == TraceReplayResult.SUCCESS) {
						SyncReplayResult replayResult = result.getSuccesfulResult();
						if (i == 0) {
							this.modelMoveOfEmptyTrace = (int) Math.round(replayResult.getInfo().get("Raw Fitness Cost"));
						} else {
							total_cost += replayResult.getInfo().get("Raw Fitness Cost")
									* replayResult.getTraceIndex().size();
							if (this.modelMoveOfEmptyTrace  != -1) {
								int worseCost = (int) Math
										.round(replayResult.getInfo().get("Trace Length") + this.modelMoveOfEmptyTrace);
								denominator += (worseCost * replayResult.getTraceIndex().size());
							}
						}
					}
			}
		}

		Double Fitness = total_cost / denominator;
		Fitness = 1 - Fitness;
		Hashtable<String, Double> results = new Hashtable<>(3);
		results.put("Raw Fitness Cost", total_cost);
		results.put("Fitness", Fitness);
		return results;
	}

	/**** Compute total alignment cost w/o computing a trace multiple times within all the iterations **/
	public Hashtable<String, Double> doReplayWithoutRepeating(List<CaseNotation> LogCases, XLog log, Petrinet net,
			Marking initialMarking, Marking finalMarking, XEventClasses classes, TransEvClassMapping mapping) {

		ReplayerParameters parameters = new ReplayerParameters.Default();// (nThreads, costUpperBound, Debug.NONE);
		Replayer replayer = new Replayer(parameters, net, initialMarking, finalMarking, classes, mapping, false);
		// timeout per trace in milliseconds
		int timeoutMilliseconds = 10 * 1000;
		// preprocessing time to be added to the statistics if necessary
		long preProcessTimeNanoseconds = 0;

		ExecutorService service = Executors.newFixedThreadPool(parameters.nThreads);

		@SuppressWarnings("unchecked")
		Future<TraceReplayTask>[] futures = new Future[log.size()];
		CopyOnWriteArrayList<Future<TraceReplayTask>> futuresCAList = new CopyOnWriteArrayList<>();

		CopyOnWriteArrayList<Integer> checkedBeforeCaseId = new CopyOnWriteArrayList<>();

		// for (int i = 0; i < LogCases.size(); i++) {
		// AtomicInteger j = new AtomicInteger(0);
		LogCases.parallelStream().forEach(c -> {
			// Case c = LogCases.get(i);
			if (!this.checkedTracesRawCost.containsKey(c.getEventNames())
					|| this.checkedTracesRawCost.get(c.getEventNames()) == Double.MAX_VALUE) {
				XTrace trace = factory.createTrace();
				for (String eventA : c.getEventNames()) {
					XEvent event = factory.createEvent();
					conceptExt.assignName(event, eventA);
					trace.add(event);
				}

				// Setup the trace replay task
				TraceReplayTask task = new TraceReplayTask(replayer, parameters, trace, c.getCaseId(),
						timeoutMilliseconds, parameters.maximumNumberOfStates, preProcessTimeNanoseconds);

				// submit for execution
				// futures[i.get()] = service.submit(task);
				futuresCAList.add(service.submit(task));
			} else {
				checkedBeforeCaseId.add(c.getCaseId());
			}
		});
		// initiate shutdown and wait for termination of all submitted tasks.
		service.shutdown();
		AtomicInteger total_cost = new AtomicInteger(0);
		AtomicInteger numerator = new AtomicInteger(0);
		AtomicInteger denominator = new AtomicInteger(0);

		if (futuresCAList.size() > 0) {
			futuresCAList.stream().forEach(future -> {
				TraceReplayTask result;
				try {
					result = future.get();// futures[i].get();
				} catch (Exception e) {
					// execution os the service has terminated.
					assert false;
					throw new RuntimeException("Error while executing replayer in ExecutorService. Interrupted maybe?",
							e);
				}

				if (result.getResult() == TraceReplayResult.SUCCESS) {
					SyncReplayResult replayResult = result.getSuccesfulResult();

					total_cost.addAndGet((int) (replayResult.getInfo().get("Raw Fitness Cost")
							* replayResult.getTraceIndex().size()));

					CaseNotation currentCase = getCase(LogCases, replayResult.getTraceIndex().first());

					if (!this.checkedTracesRawCost.containsKey(currentCase.getEventNames()))
						this.checkedTracesRawCost.put(currentCase.getEventNames(),
								replayResult.getInfo().get("Raw Fitness Cost"));

				}
			});

		}
		if (checkedBeforeCaseId.size() > 0)
			checkedBeforeCaseId.parallelStream().forEach(i -> {

				// List<String> trace = LogCasesParallel.get(i).getEventNames();
				List<String> trace = getCase(LogCases, i).getEventNames();
				if (trace != null) {
					if (this.checkedTracesRawCost.get(trace) != null) {
						double x = this.checkedTracesRawCost.get(trace);
						total_cost.addAndGet((int) x);
					}
				}
			});
		Hashtable<String, Double> results = new Hashtable<>(3);
		results.put("Raw Fitness Cost", (double) total_cost.get());
		return results;
	}

	private CaseNotation getCase(List<CaseNotation> logCases, Integer cId) {
		return logCases.parallelStream().filter(c -> cId.equals(c.getCaseId())).findAny().get();

	}

	public static TransEvClassMapping constructMappingBasedOnLabelEquality(PetrinetGraph net, XEventClass dummyEvClass,
			XEventClassifier eventClassifier, XLogInfo summary) {
		TransEvClassMapping mapping = new TransEvClassMapping(eventClassifier, dummyEvClass);

		// XLogInfo summary = XLogInfoFactory.createLogInfo(log, eventClassifier);

		for (Transition t : net.getTransitions()) {
			boolean mapped = false;
			for (XEventClass evClass : summary.getEventClasses().getClasses()) {
				String id = evClass.getId();

				if (t.getLabel().equals(id)) {
					mapping.put(t, evClass);
					mapped = true;
					break;
				}
			}

			if (!mapped && !t.isInvisible()) {
				mapping.put(t, dummyEvClass);
			}

		}

		return mapping;
	}

}
