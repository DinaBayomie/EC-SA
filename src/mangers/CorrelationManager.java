package mangers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.log4j.Logger;
import org.jbpt.petri.Marking;

import eventLog.CaseNotation;
import eventLog.Event;
import logGraphStructure.CaseStatus;
import logGraphStructure.ENode;
import logGraphStructure.LGraph;

/*** This class includes the function that correlate the events based on the model and data as a LGraph and generate the new neighbor individual */
public class CorrelationManager {

	ModelMananger model = null;
	ConcurrentHashMap<String, Double> eqRule = null;
	private final static Logger logger = Logger.getLogger(CorrelationManager.class);

	public CorrelationManager(ModelMananger model_, ConcurrentHashMap<String, Double> eqRule_) {
		super();
		model = model_;
		eqRule = eqRule_;
	}

	/***
	 * This function builds Log graph by invoking the model based correlation. One main assumption in the implementation here that the log or Log-chunk starts with start events
	 */
	public LGraph buildLGraphBasedOnModel(List<Event> uEvents, int caseCounter, int nodeCounter) {
		// List<Event> uEvents = new CopyOnWriteArrayList<>(uEvents_);
		ConcurrentHashMap<Integer, CaseStatus> completedCases = new ConcurrentHashMap<Integer, CaseStatus>();
		// Creating the entry node of LGraph
		Event e1 = uEvents.get(0);
		Marking rootPEMarking = model.fireActivity(e1.getActivity(), null);

		ENode entryNode = new ENode(null, nodeCounter, e1.getEId(), caseCounter);
		CopyOnWriteArrayList<CaseStatus> casesStatus = new CopyOnWriteArrayList<>();
		casesStatus.add(new CaseStatus(entryNode, rootPEMarking, caseCounter));
		entryNode.setCaseStatus(casesStatus);

		LGraph logGraph = new LGraph(entryNode);
		logGraph.setExitNode(entryNode);
		// logGraph.getCasesLastEvent().put(caseCounter, entryNode);
		CaseNotation cn1 = new CaseNotation(caseCounter, new CopyOnWriteArrayList<>());
		cn1.getEvents().add(e1);
		logGraph.getCasesEvents().put(caseCounter, cn1);

		nodeCounter++;
		caseCounter++;

		// rest of the events
		// int i = 0;
		// for (Event cEvent : uEvents) {
		for (int i = 1; i < uEvents.size(); i++) {
			Event cEvent = uEvents.get(i);
			logger.debug("possibleCasesBasedOnModel : 1- event; 2- last node; 3- eventNode");
			logger.debug(cEvent.toString());
			logger.debug(logGraph.getExitNode().toString());
			logger.debug(logGraph.getExitNode().caseStatusToString());

			// for start events
			if (model.isStartE(cEvent.getActivity())) {
				Marking newCaseMark = model.fireActivity(cEvent.getActivity(), null);
				ENode newNode = new ENode(logGraph.getExitNode(), nodeCounter, cEvent.getEId(), caseCounter);

				// System.out.println(newNode.toString());
				// keep track to case constraction

				// logGraph.getCasesLastEvent().put(caseCounter, newNode);
				CaseNotation cn = new CaseNotation(caseCounter, new CopyOnWriteArrayList<>());
				cn.getEvents().add(cEvent);
				logGraph.getCasesEvents().put(caseCounter, cn);

				newNode.setCaseStatus(new CopyOnWriteArrayList<>(logGraph.getExitNode().getCaseStatus()));
				newNode.getCaseStatus().add(new CaseStatus(newNode, newCaseMark, caseCounter));
				logGraph.getExitNode().setCaseStatus(null);
				logGraph.setExitNode(newNode);
				logGraph.getAllNodePointer().add(newNode);

				caseCounter++;
			} else {
				// for general events
				ConcurrentHashMap<CaseStatus, Marking> modelCases = possibleCasesBasedOnModel(cEvent,
						logGraph.getExitNode());
				CaseStatus selected = null;
				Marking caseMark = null;
				boolean selectedOpenCase = true;
				if (modelCases.isEmpty()) {
					logger.debug("deviated Event");
					if (!logGraph.getExitNode().getCaseStatus().isEmpty()) {
						logger.info(" selecting one of the opened cases ");
						selected = logGraph.getExitNode().getCaseStatus()
								.get((int) (Math.random() * logGraph.getExitNode().getCaseStatus().size()));
					} else {
						logger.info(" selecting one of the closed cases " + completedCases);
						selectedOpenCase = false;
						selected = completedCases.get((int) (Math.random() * (caseCounter - 1)));
					}

				} else {
					List<CaseStatus> cIds = Collections.list(modelCases.keys());
					Collections.shuffle(cIds);
					selected = cIds.get(0);
					caseMark = modelCases.get(selected);
				}
				logger.debug("new node " + logGraph.getExitNode() + selected.getLastcaseENode() + nodeCounter
						+ cEvent.getEId() + selected.getCaseId());
				ENode newNode = new ENode(logGraph.getExitNode(), selected.getLastcaseENode(), nodeCounter,
						cEvent.getEId(), selected.getCaseId());

				logger.debug(newNode.toString());

				if (modelCases.size() > 1 || modelCases.isEmpty()) {
					// logGraph.getCheckPointsNode().add(newNode);
					logGraph.getCheckPointsStatus().put(cEvent.getEId(), logGraph.getExitNode().cloneCaseStatus());
				}

				// logGraph.getCasesLastEvent().put(selected.getCaseId(), newNode);
				if (!logGraph.getCasesEvents().get(selected.getCaseId()).getEvents().contains(cEvent))
					logGraph.getCasesEvents().get(selected.getCaseId()).getEvents().add(cEvent);
				newNode.setCaseStatus(new CopyOnWriteArrayList<>(logGraph.getExitNode().getCaseStatus()));
				if (selectedOpenCase) {

					newNode.getCaseStatus().get(newNode.getCaseStatus().indexOf(selected)).setLastcaseENode(newNode);
					if (caseMark != null) {
						newNode.getCaseStatus().get(newNode.getCaseStatus().indexOf(selected)).setCaseMarking(caseMark);
						if (selected.isCompleted()) {
							completedCases.put(selected.getCaseId(), selected);
							newNode.getCaseStatus().remove(selected);
						}
					}
				} else {// update closed cases
					logger.debug("update completed cases by this new added noisy event ");
					selected.setLastcaseENode(newNode);
				}
				logGraph.getExitNode().setCaseStatus(null);
				logGraph.setExitNode(newNode);
				logGraph.getAllNodePointer().add(newNode);

			}
			nodeCounter++;
		}

		return logGraph;
	}

	/****
	 * return last of possible cases based on the model
	 * 
	 * @param cEvent
	 *            is event under investigation now
	 * @param lNode
	 *            is the last event in LGraph till now
	 */
	private synchronized ConcurrentHashMap<CaseStatus, Marking> possibleCasesBasedOnModel(Event cEvent,
			ENode lastNode) {
		try {
			ConcurrentHashMap<CaseStatus, Marking> possibleCases = new ConcurrentHashMap<CaseStatus, Marking>();
			// lastNode.getCasesCurrentState().values()
			logger.debug("possibleCasesBasedOnModel - investiaged Event :: " + cEvent + " ----"
					+ lastNode.getCaseStatus().toString());
			lastNode.getCaseStatus().parallelStream().forEach(cState -> {
				String act = new String(cEvent.getActivity());
				Marking m = (Marking) this.model.fireActivity(act, cState.getCaseMarking());
				if (m != null) {
					// Marking m = (Marking) newMark.clone();
					logger.debug("possibleCasesBasedOnModel - investiaged CState ::  " + act + " ----"
							+ cState.toString() + " ==> " + m.toString());
					possibleCases.put(cState, m);
					cState.setCompleted(this.model.isFinalMark(m));
				}
			});

			return possibleCases;
		} catch (Exception e) {
			logger.info("----error triggered in CorrelationManager possibleCasesBasedOnModel check error fie ------");
			logger.error(e);
			throw e;
		}
	}

	/// create new neighbor based on old version check points with percentiles and enforcing that the new choice is not the same as the previous one
	/***
	 * This function create new individual by selecting on of the check points based on the percentiles boundaries that depend over the current SA iteration
	 * 
	 * @param uEvents
	 *            are the uncorrelated event list
	 * @param oldLG
	 *            x individual that used to generate new individual x` SA feature
	 * @param cStep
	 *            current iteration number in SA
	 * @param config
	 *            correlation settings: model only , rules only, or both
	 * @return new LGraph new individual x` for the re-run of the correlation process
	 */
	public LGraph buildNewLGraph(List<Event> uEvents_, LGraph oldLG, int cStep) {

		try {
			// select check Points and redo event point

			// to be more the 100 so the percentiles would be effective
			int sEId = 0;
			if (oldLG.getCheckPointsStatus().size() > 100) {
				// select the boundaries of selection interval using the percentiles
				int LBper = (int) (Math.ceil(((cStep - 1) / (double) 100) * uEvents_.size()));
				int UBper = (int) (Math.ceil(((cStep) / (double) 100) * uEvents_.size()));
				List<Integer> eIndices = new ArrayList<>(oldLG.getCheckPointsStatus().keySet());
				Collections.sort(eIndices);
				List<Integer> subeIndices = eIndices.subList(LBper, UBper);
				// select random event to split the log
				sEId = eIndices.get(subeIndices.get((int) (Math.random() * subeIndices.size())));
			} else {
				// Randomly select of all check points
				List<Integer> eIndices = new ArrayList<>(oldLG.getCheckPointsStatus().keySet());
				sEId = eIndices.get((int) (Math.random() * eIndices.size()));
			}
			logger.debug("SEID = " + sEId);
			// create new LG based on old LG till sEId event based on the Correlation config.
			LGraph newLG = new LGraph();
			newLG.constructOld(oldLG, sEId, oldLG.getCheckPointsStatus().get(sEId));
				return buildNLGraphBasedOnModel(newLG, uEvents_, sEId);
		
		} catch (Exception e) {
			logger.info("----error triggered in CorrelationManager buildNewLGraph check error fie ------");
			logger.error(e);
			throw e;
		}
	}

	private LGraph buildNLGraphBasedOnModel(LGraph newLG, List<Event> uEvents, int sEId) {
		try {

			ConcurrentHashMap<Integer, CaseStatus> completedCases = new ConcurrentHashMap<Integer, CaseStatus>();

			int nodeCounter = newLG.getExitNode().getnId() + 1;
			int caseCounter = newLG.getCasesEvents().size();

			for (int i = sEId; i < uEvents.size(); i++) {

				Event cEvent = uEvents.get(i);

				logger.debug("possibleCasesBasedOnModel : 1- event; 2- last node; 3- eventNode");
				logger.debug(cEvent.toString());
				logger.debug(newLG.getExitNode().toString());
				logger.debug(newLG.getExitNode().caseStatusToString());

				if (model.isStartE(cEvent.getActivity())) {

					Marking newCaseMark = model.fireActivity(cEvent.getActivity(), null);
					ENode newNode = new ENode(newLG.getExitNode(), nodeCounter, cEvent.getEId(), caseCounter);

					// keep track to cases
					// newLG.getCasesLastEvent().put(caseCounter, newNode);
					CaseNotation cn = new CaseNotation(caseCounter, new CopyOnWriteArrayList<>());
					cn.getEvents().add(cEvent);
					newLG.getCasesEvents().put(caseCounter, cn);

					newNode.setCaseStatus(new CopyOnWriteArrayList<>(newLG.getExitNode().getCaseStatus()));
					newNode.getCaseStatus().add(new CaseStatus(newNode, newCaseMark, caseCounter));
					newLG.getExitNode().setCaseStatus(null);
					newLG.setExitNode(newNode);
					newLG.getAllNodePointer().add(newNode);
					caseCounter++;
				} else {
					// for general events
					ConcurrentHashMap<CaseStatus, Marking> modelCases = possibleCasesBasedOnModel(cEvent,
							newLG.getExitNode());
					CaseStatus selected = null;
					Marking caseMark = null;
					boolean selectedOpenCase = true;
					if (modelCases.isEmpty()) {
						logger.debug("deviated Event");
						if (!newLG.getExitNode().getCaseStatus().isEmpty()) {
							logger.info(" selecting one of the opened cases ");
							selected = newLG.getExitNode().getCaseStatus()
									.get((int) (Math.random() * newLG.getExitNode().getCaseStatus().size()));
						} else {
							logger.info(" selecting one of the closed cases " + completedCases);
							selectedOpenCase = false;
							List<Integer> temp = new ArrayList(completedCases.keySet());
							Collections.sort(temp);
							int lower = temp.get(0);
							selected = completedCases.get((int)  (Math.random() * (caseCounter - lower))+lower );
						if(selected ==null)
							selected = completedCases.get(temp.get(temp.size()-1));
						}

					} else {
						List<CaseStatus> cIds = Collections.list(modelCases.keys());
						// Collections.shuffle(cIds);
						selected = cIds.get((int) (Math.random() * cIds.size()));

						caseMark = modelCases.get(selected);
					}
					
					logger.debug("new node " + newLG.getExitNode() + selected.getLastcaseENode() + nodeCounter
							+ cEvent.getEId() + selected.getCaseId());
			
					ENode newNode = new ENode(newLG.getExitNode(), selected.getLastcaseENode(), nodeCounter,
							cEvent.getEId(), selected.getCaseId());

					logger.debug(newNode.toString());

					if (modelCases.size() > 1) {
						// newLG.getCheckPointsNode().add(newNode);
						newLG.getCheckPointsStatus().put(cEvent.getEId(), newLG.getExitNode().cloneCaseStatus());
					}
					// keep track to cases
					// newLG.getCasesLastEvent().put(selected.getCaseId(), newNode);
					if (!newLG.getCasesEvents().get(selected.getCaseId()).getEvents().contains(cEvent))
						newLG.getCasesEvents().get(selected.getCaseId()).getEvents().add(cEvent);
					newLG.getCasesEvents().get(selected.getCaseId()).toString();
					newNode.setCaseStatus(new CopyOnWriteArrayList<>(newLG.getExitNode().getCaseStatus()));

					if (selectedOpenCase) {
						newNode.getCaseStatus().get(newNode.getCaseStatus().indexOf(selected))
								.setLastcaseENode(newNode);
						if (caseMark != null) {
							newNode.getCaseStatus().get(newNode.getCaseStatus().indexOf(selected))
									.setCaseMarking(caseMark);
							if (selected.isCompleted()) {

								completedCases.put(selected.getCaseId(), selected);
								newNode.getCaseStatus().remove(selected);

							}
						}
					} else {// update closed cases
						logger.debug("update completed cases by this new added noisy event ");
						selected.setLastcaseENode(newNode);
					}
					newLG.getExitNode().setCaseStatus(null);
					newLG.setExitNode(newNode);
					newLG.getAllNodePointer().add(newNode);

				}
				nodeCounter++;
			}
		} catch (Exception e) {
			logger.info("----error triggered in CorrelationManager buildNLGraphBasedOnModel check error fie ------");
			logger.error(e);
			throw e;
		}
		return newLG;
	}

}
