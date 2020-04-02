package logGraphStructure;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import eventLog.CaseNotation;
import eventLog.Event;
import jdk.nashorn.internal.ir.annotations.Ignore;

public class LGraph implements Cloneable {

	// first node in the graph all other nodes have path to it
	ENode EntryNode;
	// last node in the graph where there no other nodes pointing for it
	ENode ExitNode;
	// keep track to the check points within the log path; for returning in the next iteration
	List<ENode> CheckPointsNode;
	ConcurrentHashMap<Integer, List<CaseStatus>> CheckPointsStatus;
	// keep trace to the cases' last events. not need now as caseStatus care this pointer now
	ConcurrentHashMap<Integer, CaseNotation> casesEvents;

	// List<CaseNotation> lCases = null;

	List<ENode> allNodePointer;

	// Enery cost functions measures
	double mFittness = -1;
	double actMSA = -1;
	double rSupport = -1;

	public LGraph() {
		super();
		CheckPointsNode = new CopyOnWriteArrayList<ENode>();
		// casesLastEvent = new ConcurrentHashMap<Integer, ENode>();
		casesEvents = new ConcurrentHashMap<Integer, CaseNotation>();
		allNodePointer = new CopyOnWriteArrayList<>();
		CheckPointsStatus = new ConcurrentHashMap<Integer, List<CaseStatus>>();
	}

	public LGraph(ENode entryNode) {
		super();
		EntryNode = entryNode;
		CheckPointsNode = new CopyOnWriteArrayList<ENode>();
		casesEvents = new ConcurrentHashMap<Integer, CaseNotation>();
		allNodePointer = new CopyOnWriteArrayList<>();
		allNodePointer.add(entryNode);
		CheckPointsStatus = new ConcurrentHashMap<Integer, List<CaseStatus>>();
	}

	public CaseNotation getCase(LGraph logGraph, List<Event> pE, int pnE, int Cid) {

		if (casesEvents.get(Cid) == null) {

			logGraph.getExitNode().getCaseStatus().parallelStream().forEach(cs -> {
				List<Event> events = new CopyOnWriteArrayList<>();
				ENode cn = cs.getLastcaseENode();
				while (cn != null) {
					events.add(pE.get(cn.geteIndex() + pnE));
					cn = cn.getPreviousCaseEvent();
				}
				CaseNotation caseN = new CaseNotation(cs.getCaseId(), events);
				this.casesEvents.put(cs.getCaseId(), caseN);

			});
		}

		return casesEvents.get(Cid);
	}

	public List<CaseNotation> getCases() {
		return new CopyOnWriteArrayList<>(this.casesEvents.values());
	}

	/**
	 * @return the entryNode
	 */
	public ENode getEntryNode() {
		return EntryNode;
	}

	/**
	 * @return the checkPointsNode
	 */
	public List<ENode> getCheckPointsNode() {
		return CheckPointsNode;
	}

	/**
	 * @param checkPointsNode
	 *            the checkPointsNode to set
	 */
	public void setCheckPointsNode(List<ENode> checkPointsNode) {
		CheckPointsNode = checkPointsNode;
	}

	/**
	 * @return the exitNode
	 */
	public ENode getExitNode() {
		return ExitNode;
	}

	/**
	 * @param exitNode
	 *            the last node in the graph to set
	 */
	public void setExitNode(ENode exitNode) {
		ExitNode = exitNode;
	}

	/**
	 * @return the allNodePointer
	 */
	public List<ENode> getAllNodePointer() {
		return allNodePointer;
	}

	/**
	 * @param allNodePointer
	 *            the allNodePointer to set
	 */
	public void setAllNodePointer(List<ENode> allNodePointer) {
		this.allNodePointer = allNodePointer;
	}

	/**
	 * @return the checkPointsStatus
	 */
	public ConcurrentHashMap<Integer, List<CaseStatus>> getCheckPointsStatus() {
		return CheckPointsStatus;
	}

	/**
	 * @param checkPointsStatus
	 *            the checkPointsStatus to set
	 */
	public void setCheckPointsStatus(ConcurrentHashMap<Integer, List<CaseStatus>> checkPointsStatus) {
		CheckPointsStatus = checkPointsStatus;
	}

	public void constructOld(LGraph oldLG, int sEId, List<CaseStatus> cStatus) {
		// TODO Auto-generated method stub

		for (int i = 0; i < sEId; i++) {
			ENode oldN = oldLG.getAllNodePointer().get(i);
			ENode nN = null;
			if (i == 0) {
				nN = new ENode(null, null, oldN.getnId(), oldN.geteIndex(), oldN.getcId());
				EntryNode = nN;
			} else {
				if (oldN.getPreviousCaseEvent() == null)
					nN = new ENode(allNodePointer.get(i - 1), null, oldN.getnId(), oldN.geteIndex(), oldN.getcId());
				else {
					int preECIndex = oldN.getPreviousCaseEvent().getnId();
					nN = new ENode(allNodePointer.get(i - 1), allNodePointer.get(preECIndex), oldN.getnId(),
							oldN.geteIndex(), oldN.getcId());
				}
			}
			this.allNodePointer.add(i, nN);
			// this.casesLastEvent.put(nN.getcId(), nN);
		}
		this.ExitNode = allNodePointer.get(sEId - 1);
		this.ExitNode.setCaseStatus(new CopyOnWriteArrayList<CaseStatus>(cStatus));
		this.casesEvents = this.updateCaseEvents(oldLG.getCasesEvents(), sEId);

	}

	private ConcurrentHashMap<Integer, CaseNotation> updateCaseEvents(ConcurrentHashMap<Integer, CaseNotation> old,
			int sEId) {
		ConcurrentHashMap<Integer, CaseNotation> updated = new ConcurrentHashMap<Integer, CaseNotation>();
		final int x = sEId;
		old.forEach( (cId, cn) -> {
			if (!cn.getEvents().isEmpty() && cn.getEvents().get(cn.getEvents().size() - 1).getEId() > x) {
				List<Event> uEs = new CopyOnWriteArrayList<>();
				for (Event event : cn.getEvents()) {
					if (event.getEId() >= sEId)
						break;
					uEs.add(event);
				}
				updated.put(cId, new CaseNotation(cId, uEs));
			} else {
				updated.put(cId, new CaseNotation(cId, cn.getEvents()));
			}
		});
		return updated;
	}

	@Override
	public LGraph clone() throws CloneNotSupportedException {
		// TODO Auto-generated method stub
		// this is oldLG
		LGraph n = new LGraph();
		// copy enery functions for the next iteration
		n.mFittness = this.mFittness;
		n.rSupport = this.rSupport;
		n.actMSA = this.actMSA;

		int i = 0;
		for (; i < this.getAllNodePointer().size(); i++) {
			ENode oldN = this.getAllNodePointer().get(i);
			ENode nN = null;
			if (i == 0) {
				nN = new ENode(null, null, oldN.getnId(), oldN.geteIndex(), oldN.getcId());
				n.EntryNode = nN;
			} else {
				if (oldN.getPreviousCaseEvent() == null)
					nN = new ENode(this.allNodePointer.get(i - 1), null, oldN.getnId(), oldN.geteIndex(),
							oldN.getcId());
				else {
					int preECIndex = oldN.getPreviousCaseEvent().getnId();
					nN = new ENode(this.allNodePointer.get(i - 1), this.allNodePointer.get(preECIndex), oldN.getnId(),
							oldN.geteIndex(), oldN.getcId());
				}
			}
			n.allNodePointer.add(i, nN);
			// n.casesLastEvent.put(nN.getcId(), nN);

		}
		n.ExitNode = allNodePointer.get(i - 1);
		n.ExitNode.caseStatus = this.getExitNode().cloneCaseStatus();

		this.CheckPointsStatus.forEach(this.CheckPointsStatus.size() / 4, (eId, cs) -> {
			CopyOnWriteArrayList<CaseStatus> cs_ = new CopyOnWriteArrayList<>();
			cs.parallelStream().forEach(c -> {
				cs_.add(new CaseStatus(c.getLastcaseENode(), c.getCaseMarking(), c.getCaseId()));
			});
			n.CheckPointsStatus.put(eId, cs_);
		});
		this.casesEvents.forEach(this.casesEvents.size() / 4, (cId, cn) -> {
			n.casesEvents.put(cId, new CaseNotation(cId, new CopyOnWriteArrayList<>(cn.getEvents())));
		});
		return n;
	}

	/**
	 * @return the casesEvents
	 */
	public ConcurrentHashMap<Integer, CaseNotation> getCasesEvents() {
		return casesEvents;
	}

	/**
	 * @param casesEvents
	 *            the casesEvents to set
	 */
	public void setCasesEvents(ConcurrentHashMap<Integer, CaseNotation> casesEvents) {
		this.casesEvents = casesEvents;
	}

	/**
	 * @return the mFittness
	 */
	public double getmFittness() {
		return mFittness;
	}

	/**
	 * @param mFittness
	 *            the mFittness to set
	 */
	public void setmFittness(double mFittness) {
		this.mFittness = mFittness;
	}

	/**
	 * @return the actMSA
	 */
	public double getActMSA() {
		return actMSA;
	}

	/**
	 * @param actMSA
	 *            the actMSA to set
	 */
	public void setActMSA(double actMSA) {
		this.actMSA = actMSA;
	}

	/**
	 * @return the rSupport
	 */
	public double getrSupport() {
		return rSupport;
	}

	/**
	 * @param rSupport
	 *            the rSupport to set
	 */
	public void setrSupport(double rSupport) {
		this.rSupport = rSupport;
	}

	/**
	 * @param entryNode
	 *            the entryNode to set
	 */
	public void setEntryNode(ENode entryNode) {
		EntryNode = entryNode;
	}
}
