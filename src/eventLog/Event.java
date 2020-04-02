package eventLog;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;

import com.opencsv.bean.CsvBindAndJoinByName;
import com.opencsv.bean.CsvBindByName;

public class Event {

	// @CsvBindByName(column = "Activity")
	String activity;

	// @CsvBindByName(column = "Timestamp")
	String timestamp;

	// @CsvBindByName(column = "Event Id")
	String id;

	// @CsvBindByName(column = "CaseId")
	String caseId;

	Date timestampD;

	double timeD = -1;
	// to support other event attributes
	// @CsvBindAndJoinByName(column = "[]", elementType = String.class)
	// @CsvBindByName(column = "Timestamp")
	ConcurrentHashMap<String, String> eventData;

	public Event() {
	}

	public Event(String id, String activity, String timestamp) throws ParseException {
		this.activity = activity;
		this.timestamp = timestamp;
		this.id = id;
		this.caseId = null;
	}

	/*** create unlabeled event with data attributes */
	public Event(String id, String activity, String timestamp, ConcurrentHashMap<String, String> eventData) {
		this.activity = activity;
		this.timestamp = timestamp;
		this.id = id;
		this.caseId = null;
		this.eventData = eventData;
	}

	/*** create labeled event with data attributes */
	public Event(String id, String CaseId, String activity, String timestamp,
			ConcurrentHashMap<String, String> eventData) {
		this.activity = activity;
		this.timestamp = timestamp;
		this.id = id;
		this.caseId = CaseId;
		this.eventData = eventData;
	}

	public Event(String activity, String timestamp) {
		this.activity = activity;
		this.timestamp = timestamp;
		this.caseId = null;
	}

	public Event(String activity2, String timestamp2, ConcurrentHashMap<String, String> others) {
		// TODO Auto-generated constructor stub

		this.activity = activity2;
		this.timestamp = timestamp2;
		this.eventData = others;
	}

	/*** return time as double */
	public double getTime() {
		if (timeD == -1)
			timeD = Double.parseDouble(this.timestamp);
		return timeD;
	}

	public Date getTimestampD() {
		if (timestampD == null)
			this.timestampD = this.formatTimestamp(this.timestamp);
		return timestampD;
	}

	public String getActivity() {
		return activity;
	}

	public void setActivity(String activity) {
		this.activity = activity;
	}

	public String getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(String timestamp) {
		this.timestamp = timestamp;
	}

	public String getCaseId() {
		return caseId;
	}

	public void setCaseId(String caseId) {
		this.caseId = caseId;
	}

	/** Return the event id\index in the input log */
	public String getId() {
		return id;
	}

	/** Return the event id\index in the input log */
	public int getEId() {
		return Integer.parseInt(id);
	}

	/** Set the event id by Its index in the input log */
	public void setId(String id) {
		this.id = id;
	}

	public void printEvent() {
		if (this.caseId == null)
			System.out.println(this.id + ":" + this.timestamp + ":" + this.activity);
		else
			System.out.println(this.caseId + ":" + this.id + ":" + this.timestamp + ":" + this.activity);
	}

	@Override
	public boolean equals(Object e) {
		Event e2 = (Event) e;
		if (this.id.equals(e2.id) && this.timestamp.equals(e2.timestamp) && this.activity.equals(e2.activity))
			return true;
		else if (this.timestamp.equals(e2.timestamp) && this.activity.equals(e2.activity))
			return true;
		return false;
	}

	private Date formatTimestamp(String timestamp) {

		try {
			return new SimpleDateFormat("yyyy/M/d H:m:s").parse(timestamp);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			try {
				return new SimpleDateFormat("M/d/yyyy H:m").parse(timestamp);
			} catch (ParseException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			return null;
		}
	}

	public int compareTo(Event arg0) {
		// TODO Auto-generated method stub
		return this.timestamp.compareTo(arg0.timestamp);
	}

	/**
	 * @return the eventData
	 */
	public ConcurrentHashMap<String, String> getEventData() {
		return eventData;
	}

	/**
	 * @param eventData
	 *            the eventData to set
	 */
	public void setEventData(ConcurrentHashMap<String, String> eventData) {
		this.eventData = eventData;
	}

	public String[] getRecord(String[] headers) {
		// TODO Auto-generated method stub
		String[] record = new String[headers.length];
		for (int i = 0; i < headers.length; i++) {
			if (headers[i].toLowerCase().contains("activity"))
				record[i] = this.activity;
			else if (headers[i].toLowerCase().contains("timestamp"))
				record[i] = this.timestamp;
			else if (headers[i].toLowerCase().contains("case"))
				record[i] = this.caseId;
			else if ((headers[i].toLowerCase().contains("id") || headers[i].toLowerCase().contains("event")))
				record[i] = this.id;
			else {
				if (this.eventData != null && this.eventData.size() > 0) {
					record[i] = this.eventData.get(headers[i]);
				}
			}
		}
		return record;
	}

	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return id +" - "+activity+" - "+timestamp;
	}
}
