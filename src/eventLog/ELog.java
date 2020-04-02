package eventLog;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.CSVWriterBuilder;
import com.opencsv.ICSVWriter;
import com.opencsv.bean.ColumnPositionMappingStrategy;
import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;

import logGraphStructure.LGraph;

public class ELog {

	List<Event> events;
	Map<String, List<Event>> actEvents;
	String[] headers;
	int nCases;
	HashSet<String> activities;
	boolean isLabeled = false;

	/*** Constructor for reading uncorrelated log without event id in the given log */
	public ELog(String ELogFilePath) {
		try {
			this.events = readELogCSV(ELogFilePath);
			actEvents = this.events.stream().collect(Collectors.groupingBy(e -> e.getActivity()));
			this.activities = getDActivities();
			// Collections.sort((List<Event>) this.events);
			this.events.sort(Comparator.comparingDouble(Event::getTime));
			updateEventsId(events);

		} catch (IOException ex) {
			System.out.print(ex);
		}
	}

	/***
	 * Create ELog for both correlated and uncorrelated logs with and without the event id in the given data
	 * 
	 * @param labeled
	 *            true if the log is correlated log, otherwise false
	 * @param withEid
	 *            true if the log contains event id as data attribute, otherwise false and event id is assigned based on the timestamp order
	 */
	public ELog(String ELogFilePath, boolean labeled, boolean withEid) {
		try {

			if (withEid) {
				if (labeled)
					this.events = readLabeledELogWiDCSV(ELogFilePath);
				else
					this.events = readELogWiDCSV(ELogFilePath);

				this.events.sort(Comparator.comparing(Event::getId));

			} else {
				if (labeled)
					this.events = this.readLabeledELogCSV(ELogFilePath);
				else
					this.events = this.readELogCSV(ELogFilePath);

				this.events.sort(Comparator.comparingDouble(Event::getTime));
				updateEventsId(events);
			}
			// Collections.sort(this.events);
			this.activities = getDActivities();
			this.isLabeled = labeled;

		} catch (IOException ex) {
			System.out.print(ex);
		}
	}

	private void updateEventsId(List<Event> events) {
		for (int i = 0; i < events.size(); i++) {
			events.get(i).setId(i + "");
		}
	}

	// read the ELog file in the main
	public ELog(List<Event> events) {
		this.events = events;
	}

	public ELog(ELog orglabeledELog) {
		// TODO Auto-generated constructor stub
		// this.events = new ArrayList<>(orglabeledELog.getEvents());
		this.events = new CopyOnWriteArrayList<Event>();
		orglabeledELog.getEvents().parallelStream().forEach(e -> {
			this.events.add(e);
		});
		this.activities = new HashSet<>(orglabeledELog.getActivities());
	}

	public List<Event> getEvents() {
		return events;
	}

	public void setEvents(List<Event> events) {
		this.events = events;
	}

	//// reading functions/////////////////////////////////////

	/****
	 * Read uncorrelated log with event attributes without event id in the given log
	 * 
	 * @param filename
	 * @return unordered unlabeled Events
	 */
	public List<Event> readELogCSV(String filename) throws IOException {
		Reader reader = Files.newBufferedReader(Paths.get(filename));
		// create csv reader
		CSVReader csvReader = new CSVReader(reader);

		// read all records at once
		CopyOnWriteArrayList<String[]> records = new CopyOnWriteArrayList<>(csvReader.readAll());
		headers = records.remove(0);
		CopyOnWriteArrayList<Event> unlabeledEvents = new CopyOnWriteArrayList<>();
		records.parallelStream().forEach(r -> {
			String activity = null;
			String timestamp = null;
			ConcurrentHashMap<String, String> others = new ConcurrentHashMap<>(headers.length);
			for (int i = 0; i < headers.length; i++) {
				if (headers[i].toLowerCase().contains("activity"))
					activity = r[i];
				else if (headers[i].toLowerCase().contains("timestamp"))
					timestamp = r[i];
				else {
					others.put(headers[i], r[i]);
				}
			}
			unlabeledEvents.add(new Event(activity, timestamp, others));
		});

		// List<Event> unLabeledEvents = new CsvToBeanBuilder<Event>(reader).withType(Event.class).build().parse();
		// updateEventsId(unlabeledEvents);
		return unlabeledEvents;
	}

	/****
	 * Read correlated log with event attributes without event id in the given log
	 * 
	 * @param filename
	 * @return unordered labeled Events
	 */
	public List<Event> readLabeledELogCSV(String filename) throws IOException {
		Reader reader = Files.newBufferedReader(Paths.get(filename));
		// create csv reader
		CSVReader csvReader = new CSVReader(reader);

		// read all records at once
		CopyOnWriteArrayList<String[]> records = new CopyOnWriteArrayList<>(csvReader.readAll());
		headers = records.remove(0);
		CopyOnWriteArrayList<Event> unlabeledEvents = new CopyOnWriteArrayList<>();
		records.parallelStream().forEach(r -> {
			String activity = null;
			String timestamp = null;
			String caseId = null;
			ConcurrentHashMap<String, String> others = new ConcurrentHashMap<>(headers.length);
			for (int i = 0; i < headers.length; i++) {
				if (headers[i].toLowerCase().contains("activity"))
					activity = r[i];
				else if (headers[i].toLowerCase().contains("timestamp"))
					timestamp = r[i];
				else if (headers[i].toLowerCase().contains("case"))
					caseId = r[i];
				else {
					others.put(headers[i], r[i]);
				}
			}
			unlabeledEvents.add(new Event(null, caseId, activity, timestamp, others));
		});

		// List<Event> unLabeledEvents = new CsvToBeanBuilder<Event>(reader).withType(Event.class).build().parse();
		// updateEventsId(unlabeledEvents);
		return unlabeledEvents;
	}

	/****
	 * Read uncorrelated log with event attributes with event id in the given log
	 * 
	 * @param filename
	 * @return unordered unlabeled Events
	 */
	public List<Event> readELogWiDCSV(String filename) throws IOException {
		Reader reader = Files.newBufferedReader(Paths.get(filename));
		// create csv reader
		CSVReader csvReader = new CSVReader(reader);

		// read all records at once
		CopyOnWriteArrayList<String[]> records = new CopyOnWriteArrayList<>(csvReader.readAll());
		headers = records.remove(0);
		CopyOnWriteArrayList<Event> unlabeledEvents = new CopyOnWriteArrayList<>();
		records.parallelStream().forEach(r -> {
			String activity = null;
			String timestamp = null;
			String eventId = null;
			ConcurrentHashMap<String, String> others = new ConcurrentHashMap<>(headers.length);
			for (int i = 0; i < headers.length; i++) {
				if (headers[i].toLowerCase().contains("activity"))
					activity = r[i];
				else if (headers[i].toLowerCase().contains("timestamp"))
					timestamp = r[i];
				else if ((headers[i].toLowerCase().contains("id") || headers[i].toLowerCase().contains("event")))
					eventId = r[i];

				else {
					others.put(headers[i], r[i]);
				}
			}
			unlabeledEvents.add(new Event(eventId, activity, timestamp, others));
		});

		// List<Event> unLabeledEvents = new CsvToBeanBuilder<Event>(reader).withType(Event.class).build().parse();
		// updateEventsId(unlabeledEvents);
		return unlabeledEvents;
	}

	/****
	 * Read correlated log with event attributes without event id in the given log
	 * 
	 * @param filename
	 * @return unordered labeled Events
	 */
	public List<Event> readLabeledELogWiDCSV(String filename) throws IOException {
		Reader reader = Files.newBufferedReader(Paths.get(filename));
		// create csv reader
		CSVReader csvReader = new CSVReader(reader);

		// read all records at once
		CopyOnWriteArrayList<String[]> records = new CopyOnWriteArrayList<>(csvReader.readAll());
		headers = records.remove(0);
		CopyOnWriteArrayList<Event> unlabeledEvents = new CopyOnWriteArrayList<>();
		records.parallelStream().forEach(r -> {
			String activity = null;
			String timestamp = null;
			String caseId = null;
			String eventId = null;
			ConcurrentHashMap<String, String> others = new ConcurrentHashMap<>(headers.length);
			for (int i = 0; i < headers.length; i++) {
				if (headers[i].toLowerCase().contains("activity"))
					activity = r[i];
				else if (headers[i].toLowerCase().contains("timestamp"))
					timestamp = r[i];
				else if (headers[i].toLowerCase().contains("case"))
					caseId = r[i];
				else if ((headers[i].toLowerCase().contains("id") || headers[i].toLowerCase().contains("event")))
					eventId = r[i];
				else {
					others.put(headers[i], r[i]);
				}
			}
			unlabeledEvents.add(new Event(eventId, caseId, activity, timestamp, others));
		});

		// List<Event> unLabeledEvents = new CsvToBeanBuilder<Event>(reader).withType(Event.class).build().parse();
		// updateEventsId(unlabeledEvents);
		return unlabeledEvents;
	}

	///// writing log ///
	String outPutName;
	String outputFolderName;

	public void setOutputFileName(String fName, String folderName) {
		outputFolderName = folderName;
		outPutName = fName;
	}

	public Callable<Boolean> writeELogCSVO = () -> {
		Writer csvWriter = null;
		try {
			String filePath = "./resources/output/" + this.outputFolderName + "/LabeledELog" + this.outPutName + ".csv";
			csvWriter = new FileWriter(filePath);

			ColumnPositionMappingStrategy<Event> strategy = new ColumnPositionMappingStrategy<Event>();
			strategy.setType(Event.class);
			String[] memberFieldsToBindTo = { "caseId", "activity", "timestamp" };
			strategy.setColumnMapping(memberFieldsToBindTo);

			// worst solution ever
			Event e = new Event("activity", "timestamp");
			e.setCaseId("caseId");

			StatefulBeanToCsvBuilder<Event> builder = new StatefulBeanToCsvBuilder<Event>(csvWriter);

			StatefulBeanToCsv<Event> beanToCsv = builder.withMappingStrategy(strategy).build();

			beanToCsv.write(e);
			for (Event event : this.events) {
				beanToCsv.write(event);
			}

		} catch (Exception ee) {
			ee.printStackTrace();
			return false;
		} finally {
			csvWriter.flush();
			csvWriter.close();
		}
		return true;
	};

	public Callable<Boolean> writeELogCSV = () -> {
		Writer Writer = null;
		try {
			String filePath = "./resources/output/" + this.outputFolderName + "/LabeledELog" + this.outPutName + ".csv";
			Writer = new FileWriter(filePath);

			ICSVWriter csvWriter = new CSVWriterBuilder(Writer).withSeparator(CSVWriter.DEFAULT_SEPARATOR)
					.withQuoteChar(CSVWriter.NO_QUOTE_CHARACTER).withEscapeChar(CSVWriter.DEFAULT_ESCAPE_CHARACTER)
					.withLineEnd(CSVWriter.DEFAULT_LINE_END).build();
			String[] updatedHeaders = new String[headers.length + 1];
			updatedHeaders[0] = "Case Id";
			for (int i = 1; i < updatedHeaders.length; i++) {
				updatedHeaders[i] = headers[i - 1];
			}
			csvWriter.writeNext(headers);

			// write data records
			csvWriter.writeNext(new String[] { "1", "Emma Watson", "emma.watson@example.com", "UK" });
			for (Event event : this.events) {
				csvWriter.writeNext(event.getRecord(headers));
			}

			csvWriter.flush();
			csvWriter.close();
		} catch (Exception ee) {
			ee.printStackTrace();
			return false;
		} finally {

			Writer.flush();
			Writer.close();
		}
		return true;
	};

	public boolean writeELogCSV(String fileName) {
		Writer Writer = null;
		try {
			String filePath = "./resources/" + fileName + ".csv";
			Writer = new FileWriter(filePath);

			ICSVWriter csvWriter = new CSVWriterBuilder(Writer).withSeparator(CSVWriter.DEFAULT_SEPARATOR)
					.withQuoteChar(CSVWriter.NO_QUOTE_CHARACTER).withEscapeChar(CSVWriter.DEFAULT_ESCAPE_CHARACTER)
					.withLineEnd(CSVWriter.DEFAULT_LINE_END).build();
			String[] updatedHeaders = new String[headers.length + 2];
			updatedHeaders[0] = "Case";
			updatedHeaders[1] = "Event";
			
			for (int i = 2; i < updatedHeaders.length; i++) {
				updatedHeaders[i] = headers[i - 2];
			}
			csvWriter.writeNext(updatedHeaders);

			// write data records
//			csvWriter.writeNext(new String[] { "1", "Emma Watson", "emma.watson@example.com", "UK" });
			for (Event event : this.events) {
				csvWriter.writeNext(event.getRecord(updatedHeaders));
			}

			csvWriter.flush();
			csvWriter.close();
			Writer.close();
		} catch (Exception ee) {
			ee.printStackTrace();
			return false;
		}
		// finally {
		//
		// try {
		// Writer.flush();
		// Writer.close();
		// } catch (IOException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// }
		//
		// }
		return true;
	}

	public Event findEvent(Event e) {

		if (e != null) {
			List<Event> eventsToCheck = this.events;
			if (actEvents != null && e.getActivity() != null)
				eventsToCheck = actEvents.get(e.getActivity());
			if (eventsToCheck == null)
				eventsToCheck = this.events;
			for (Event event : eventsToCheck) {
				if (event.equals(e))
					return event;
			}
		} else {
			System.out.println("null event");
		}
		return null;
	}

	public Event findEventByIndex(Event e, int eindex) {
		if (eindex < this.events.size()) {
			Event e2 = this.events.get(eindex);
			if (e2.equals(e)) {
				return e2;
			} else
				return findEvent(e);
		} else
			return findEvent(e);
	}

	public int getEIndex(Event e) {
		return this.events.indexOf(e);
	}

	public Event findEventInCase(List<Event> caseEvents, Event e) {
		for (Event event : caseEvents) {
			if (event.equals(e))
				return event;
		}
		return null;
	}

		/*** Update the log based on the log graph **/
	public void updateCIds(LGraph logGraph) {

		logGraph.getAllNodePointer().parallelStream().forEach(en -> {
			this.events.get(en.geteIndex()).setCaseId(en.getcId() + "");
		});
	}

	//// parameters

	/**
	 * @return the nCases
	 */
	public int getnCases() {
		return nCases;
	}

	/**
	 * @return the number of events
	 */
	public int getnEvents() {
		return this.events.size();
	}

	/**
	 * @param nCases
	 *            the nCases to set
	 */
	public void setnCases(int nCases) {
		this.nCases = nCases;
	}

	public HashSet<String> getDActivities() {
		List<String> names = new CopyOnWriteArrayList<>();
		// for (Event e : this.events) {
		// activities.add(e.getActivity());
		// }

		this.events.parallelStream().forEach(e -> {
			names.add(e.getActivity());
		});
		HashSet<String> activities = new HashSet<>(names);
		return activities;
	}

	/**
	 * @return the activities
	 */
	public HashSet<String> getActivities() {
		return activities;
	}

	/**
	 * @param activities
	 *            the activities to set
	 */
	public void setActivities(HashSet<String> activities) {
		this.activities = activities;
	}

	/**
	 * @return the isLabeled
	 */
	public boolean isLabeled() {
		return isLabeled;
	}

	/**
	 * @param isLabeled
	 *            the isLabeled to set
	 */
	public void setLabeled(boolean isLabeled) {
		this.isLabeled = isLabeled;
	}

}
