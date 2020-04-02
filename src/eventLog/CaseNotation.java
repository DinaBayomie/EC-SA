package eventLog;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class CaseNotation {

	int caseId;
	List<Event> events;
	List<String> acts;

	public CaseNotation(int caseId, List<Event> events) {
		super();
		this.caseId = caseId;
		this.events = events;
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
	 * @return the events
	 */
	public List<Event> getEvents() {
		// HashSet<Event> e= new HashSet<>(events);
		// events=new CopyOnWriteArrayList<Event>(e);

		return events;
	}

	/**
	 * @param events
	 *            the events to set
	 */
	public void setEvents(List<Event> events) {
		this.events = events;
	}

	public int caseSize() {
		// TODO Auto-generated method stub
		return events.size();
	}

	public List<String> getEventNames() {
		// TODO Auto-generated method stub
		if (acts == null) {
			acts = new CopyOnWriteArrayList<>();
			events.stream().forEach(e -> {
				acts.add(e.getActivity());
			});
		}
		return acts;
	}

	@Override
	public String toString() {
		StringBuffer str = new StringBuffer();
		str.append("CID= " + caseId + "  -> {");
		for (Event event : events) {
			str.append(event.toString() + " , ");
		}
		str.append(" } ");
		return str.toString();
	}
}
