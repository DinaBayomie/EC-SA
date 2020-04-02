package logGraphStructure;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.jbpt.petri.Marking;

public class ENode {

	// node information
	private int nId;
	private int eIndex;
	private int cId;

	// keep track of the case order
	private int preEvtIndex;
	private ENode previousCaseEvent;

	// preserve the log structure
	private ENode parent;

	// keep the correlation status
	// we use ConcurrentHashMap instead of hashtable because its faster with thread and allow multiple access to the map
	ConcurrentHashMap<Integer, CaseStatus> casesCurrentStateHM;

	// as caseStatus has the case id i can remove the hashmap now
	CopyOnWriteArrayList<CaseStatus> caseStatus;

	/*** use for start events */
	public ENode(ENode parent, int nId, int eIndex, int cId) {
		super();
		this.parent = parent;
		this.nId = nId;
		this.eIndex = eIndex;
		this.cId = cId;
		this.previousCaseEvent = null;
	}

	/*** use for normal event */
	public ENode(ENode parent, ENode preCaseEvent, int nId, int eIndex, int cId) {
		super();
		this.parent = parent;
		this.nId = nId;
		this.eIndex = eIndex;
		this.cId = cId;
		this.previousCaseEvent = preCaseEvent;
	}

	/*** Use for the first event in the log Root event */
	public ENode(int nId, int eIndex, int cId) {
		super();
		this.nId = nId;
		this.eIndex = eIndex;
		this.cId = cId;
		this.parent = null;
		this.previousCaseEvent = null;
	}

	/*** Generate a new Node based on another node **/
	public ENode(ENode eNode) {
		this.parent = eNode.parent;
		this.previousCaseEvent = eNode.getPreviousCaseEvent();
		this.nId = eNode.nId;
		this.eIndex = eNode.eIndex;
		this.cId = eNode.cId;
	}

	/** Clone ENode case status */
	public CopyOnWriteArrayList<CaseStatus> cloneCaseStatus() {
		CopyOnWriteArrayList<CaseStatus> n = new CopyOnWriteArrayList<>();
		this.caseStatus.parallelStream().forEach(c -> {
			n.add(new CaseStatus(c.getLastcaseENode(), c.getCaseMarking(), c.getCaseId()));
		});
		return n;
	}

	/**
	 * @return the parent
	 */
	public ENode getParent() {
		return parent;
	}

	/**
	 * @param parent
	 *            the parent to set
	 */
	public void setParent(ENode parent) {
		this.parent = parent;
	}

	/**
	 * @return the nId
	 */
	public int getnId() {
		return nId;
	}

	/**
	 * @param nId
	 *            the nId to set
	 */
	public void setnId(int nId) {
		this.nId = nId;
	}

	/**
	 * @return the eIndex
	 */
	public int geteIndex() {
		return eIndex;
	}

	/**
	 * @param eIndex
	 *            the eIndex to set
	 */
	public void seteIndex(int eIndex) {
		this.eIndex = eIndex;
	}

	/**
	 * @return the cId
	 */
	public int getcId() {
		return cId;
	}

	/**
	 * @param cId
	 *            the cId to set
	 */
	public void setcId(int cId) {
		this.cId = cId;
	}

	/**
	 * @return the casesCurrentStateHM
	 */
	public ConcurrentHashMap<Integer, CaseStatus> getCasesCurrentState() {
		return casesCurrentStateHM;
	}

	/**
	 * @param casesCurrentStateHM
	 *            the casesCurrentStateHM to set
	 */
	public void setCasesCurrentState(ConcurrentHashMap<Integer, CaseStatus> casesCurrentState) {
		this.casesCurrentStateHM = casesCurrentState;
	}

	/**
	 * remove the cases current state hashtable
	 */
	public void cleanCasesCurrentStateHT() {
		this.casesCurrentStateHM.clear();
	}

	/**
	 * @return clone version of the casesCurrentStateHM
	 */
	public ConcurrentHashMap<Integer, CaseStatus> cloneCasesCurrentState(int expectNumCases) {
		ConcurrentHashMap<Integer, CaseStatus> newCCs = new ConcurrentHashMap<>(expectNumCases);
		this.casesCurrentStateHM.keySet().parallelStream().forEach(key -> {
			newCCs.put(key, casesCurrentStateHM.get(key));
		});
		return newCCs;
	}

	/**
	 * @param final
	 *            marks
	 * @return clone version of the casesCurrentStateHM without the completed cases
	 */
	public ConcurrentHashMap<Integer, CaseStatus> cloneCasesCurrentState(List<Marking> finalMarks, int expectNumCases) {
		ConcurrentHashMap<Integer, CaseStatus> newCCs = new ConcurrentHashMap<>(expectNumCases);
		this.casesCurrentStateHM.keySet().parallelStream().forEach(key -> {
			if (!finalMarks.contains(casesCurrentStateHM.get(key).getCaseMarking()))
				newCCs.put(key, casesCurrentStateHM.get(key));
		});
		return newCCs;
	}

	/**
	 * @param final
	 *            marks
	 * @return clone version of the casesCurrentStateHM without the completed cases
	 */
	public ConcurrentHashMap<Integer, CaseStatus> cloneAndCleanCasesCurrentState(List<Marking> finalMarks,
			int expectNumCases) {
		ConcurrentHashMap<Integer, CaseStatus> newCCs = new ConcurrentHashMap<>(expectNumCases);
		this.casesCurrentStateHM.keySet().parallelStream().forEach(key -> {
			if (!finalMarks.contains(casesCurrentStateHM.get(key).getCaseMarking()))
				newCCs.put(key, casesCurrentStateHM.get(key));
		});
		this.casesCurrentStateHM = null;
		return newCCs;
	}

	/**
	 * @return the preEvtIndex
	 */
	public int getPreEvtIndex() {
		return preEvtIndex;
	}

	/**
	 * @param preEvtIndex
	 *            the preEvtIndex to set
	 */
	public void setPreEvtIndex(int preEvtIndex) {
		this.preEvtIndex = preEvtIndex;
	}

	/**
	 * @return the previousCaseEvent
	 */
	public ENode getPreviousCaseEvent() {
		return previousCaseEvent;
	}

	/**
	 * retrieve case events based on current node
	 * 
	 * @return the previousCaseEvents
	 */
	public List<ENode> getPreviousCaseEvents() {
		List<ENode> preEvents = new ArrayList<ENode>();
		ENode pre = this.previousCaseEvent;
		while (pre != null) {
			preEvents.add(pre);
			pre = pre.getPreviousCaseEvent();
		}
		return preEvents;
	}

	/**
	 * @param previousCaseEvent
	 *            the previousCaseEvent to set
	 */
	public void setPreviousCaseEvent(ENode previousCaseEvent) {
		this.previousCaseEvent = previousCaseEvent;
	}

	/**
	 * @return the caseStatus
	 */
	public CopyOnWriteArrayList<CaseStatus> getCaseStatus() {
		return caseStatus;
	}

	/**
	 * @param caseStatus
	 *            the caseStatus to set
	 */
	public void setCaseStatus(CopyOnWriteArrayList<CaseStatus> caseStatus) {
		this.caseStatus = caseStatus;
	}

	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return "nId= " + nId + " - eID= " + eIndex + "- CID= " + cId;
	}

	public String caseStatusToString() {
		StringBuffer str = new StringBuffer();
		
		this.caseStatus.parallelStream().forEach(c->{
		str.append(	c.toString());
		
		});;
		
		
		return str.toString();
	}
}
