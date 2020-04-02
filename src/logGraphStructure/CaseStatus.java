package logGraphStructure;

import org.jbpt.petri.Marking;

public class CaseStatus {

	// private int lastEventIndex;
	// keep trace as use this and parent point, i can build the case
	private ENode lastcaseENode;

	private Marking currentCaseMarking;
	private double score;
	private int caseId;

	private boolean isCompleted;

	public CaseStatus(double score, int caseId) {
		super();
		this.score = score;
		this.caseId = caseId;
	}

	public CaseStatus(ENode lastcaseENode, Marking currentCaseMarking) {
		super();
		this.lastcaseENode = lastcaseENode;
		this.currentCaseMarking = currentCaseMarking;
	}

	public CaseStatus(ENode lastcaseENode, Marking currentCaseMarking, int caseId) {
		super();
		this.lastcaseENode = lastcaseENode;
		this.currentCaseMarking = currentCaseMarking;
		this.caseId = caseId;

	}

	public CaseStatus(ENode lastcaseENode, Marking currentCaseMarking, int caseId_, double score_) {
		super();
		this.lastcaseENode = lastcaseENode;
		this.currentCaseMarking = currentCaseMarking;
		this.caseId = caseId_;
		this.score = score_;
	}
	// public CaseStatus(int lastEventIndex, Marking currentCaseMarking) {
	// super();
	//// this.lastEventIndex = lastEventIndex;
	// this.currentCaseMarking = currentCaseMarking;
	// }
	//
	// /**
	// * @return the lastEventIndex
	// */
	// public int getLastEventIndex() {
	// return lastEventIndex;
	// }
	//
	// /**
	// * @param lastEventIndex
	// * the lastEventIndex to set
	// */
	// public void setLastEventIndex(int lastEventIndex) {
	// this.lastEventIndex = lastEventIndex;
	// }

	/**
	 * @return the currentCaseMarking
	 */
	public Marking getCaseMarking() {
		return currentCaseMarking;
	}

	/**
	 * @param currentCaseMarking
	 *            the currentCaseMarking to set
	 */
	public void setCaseMarking(Marking currentCaseMarking) {
		this.currentCaseMarking = currentCaseMarking;
	}

	/**
	 * @return the lastcaseENode
	 */
	public ENode getLastcaseENode() {
		return lastcaseENode;
	}

	/**
	 * @param lastcaseENode
	 *            the lastcaseENode to set
	 */
	public void setLastcaseENode(ENode lastcaseENode) {
		this.lastcaseENode = lastcaseENode;
	}

	/**
	 * @return the score
	 */
	public double getScore() {
		return score;
	}

	/**
	 * @param score
	 *            the score to set
	 */
	public void setScore(double score) {
		this.score = score;
	}

	/**
	 * @return the caseId
	 */
	public int getCaseId() {
		return caseId;
	}

	/**
	 * @param caseId
	 *            the caseId to set
	 */
	public void setCaseId(int caseId) {
		this.caseId = caseId;
	}

	/**
	 * @return the isCompleted
	 */
	public boolean isCompleted() {
		return isCompleted;
	}

	/**
	 * @param isCompleted
	 *            the isCompleted to set
	 */
	public void setCompleted(boolean isCompleted) {
		this.isCompleted = isCompleted;
	}

	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return "/////CId = " + caseId + " : " + currentCaseMarking.toString();
	}
}
