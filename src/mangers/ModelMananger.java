package mangers;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.jbpt.petri.Marking;
import org.jbpt.petri.NetSystem;
import org.jbpt.petri.Node;
import org.jbpt.petri.Transition;

import processModel.PetriNetTS;

public class ModelMananger {

	private final static Logger logger = Logger.getLogger(ModelMananger.class);

	/*** This list contains the observed transitions, i.e., no silent transition included. */
	CopyOnWriteArrayList<Transition> transitions = null;
	/*** This list contains the silent transitions only. */
	CopyOnWriteArrayList<Transition> silentTrans = null;
	/*** This list contains the silent transitions only. */
	CopyOnWriteArrayList<Transition> startTrans = null;

	// NetSystem pModel;
	PetriNetTS pModel;
	/*** This field contains some of the possible Marking and their enabled transition based on the process model */
	ConcurrentHashMap<Marking, CopyOnWriteArrayList<Transition>> marksEnabledTransition;
	/*** This field contains marking and new mark after firing a transition sort of RG */
	ConcurrentHashMap<Marking, ConcurrentHashMap<Transition, Marking>> marksNextMark;
	/*** This field contains the silent transitions with their observed successors */
	ConcurrentHashMap<Transition, Set<Transition>> silentSuccTransitions;
	Marking naturalMark = null;

	public ModelMananger(PetriNetTS pModel_) {
		super();
		this.pModel = pModel_;
		this.transitions = new CopyOnWriteArrayList<>(this.pModel.getTransitions());
		this.silentTrans = this.getSilentTransitions(this.transitions);
		this.startTrans = new CopyOnWriteArrayList<>(this.pModel.getEnabledTransitions());
		this.silentSuccTransitions = new ConcurrentHashMap<Transition, Set<Transition>>();
		this.marksEnabledTransition = new ConcurrentHashMap<Marking, CopyOnWriteArrayList<Transition>>();
		this.marksNextMark = new ConcurrentHashMap<Marking, ConcurrentHashMap<Transition, Marking>>();
		this.fireSilentTransitions();

		this.pModel.loadNaturalMarking();
		this.naturalMark = (Marking) this.pModel.getMarking().clone();
		// this.marksEnabledTransition.put(naturalMark, updateEnableTransitions(new HashSet<>(startTrans)));
		this.marksEnabledTransition.put(naturalMark, startTrans);
	}

	/// pre-processing functions///
	/*** This function fires all the silent transitions to build up silentSuccTransitions HMap */
	private void fireSilentTransitions() {

		silentTrans.parallelStream().forEach(st -> {
			Collection<Node> succNodes = fireSilentTran(st, new HashSet<Node>(), new HashSet<Node>(),
					new HashSet<Node>(), true);
			Set<Transition> succTrans = new HashSet<>(succNodes.size());
			for (Node node : succNodes) {
				succTrans.add((Transition) node);
			}
			this.silentSuccTransitions.put(st, succTrans);
		});
	}

	/***
	 * This function recursively get the successor of a silent transition by retrieve its enabled observed transitions based on the model structure
	 */
	private Collection<Node> fireSilentTran(Node node, Collection<Node> SuccTransNodes, Collection<Node> RemoveNodes,
			Collection<Node> AlreadyChecked, boolean isTransition) {
		Collection<Node> sucNodes = new HashSet<>();
		// as direct successors include places also
		if (isTransition)
			sucNodes = this.pModel.getDirectSuccessors(this.pModel.getDirectSuccessors(node));
		else
			sucNodes = this.pModel.getDirectSuccessors(node);

		SuccTransNodes.addAll(sucNodes);
		if (AlreadyChecked.contains(node))
			return SuccTransNodes;

		for (Node tran : sucNodes) {
			if (this.silentTrans.contains((Transition) tran)) {
				RemoveNodes.add(tran);
				AlreadyChecked.add(node);
				if (this.silentSuccTransitions.containsKey(tran))
					SuccTransNodes.addAll(this.silentSuccTransitions.get((Transition) tran));
				else
					SuccTransNodes.addAll(fireSilentTran(tran, SuccTransNodes, RemoveNodes, AlreadyChecked, true));
				AlreadyChecked.add(node);
			}
		}
		SuccTransNodes.removeAll(RemoveNodes);
		return SuccTransNodes;
	}

	/***
	 * This function retrieve the silent transitions in the model using different selection criteria based on the transition name
	 */
	private CopyOnWriteArrayList<Transition> getSilentTransitions(CopyOnWriteArrayList<Transition> transitions) {

		CopyOnWriteArrayList<Transition> result = new CopyOnWriteArrayList<Transition>();
		String p = "^t\\d+";
		Pattern r = Pattern.compile(p);
		transitions.parallelStream().forEach(t -> {
			if (t.getLabel().isEmpty() || t.getLabel().toLowerCase().startsWith("tau")
					|| t.getLabel().toLowerCase().equals("sink") || t.getLabel().toLowerCase().equals("source")
					|| r.matcher(t.getLabel()).find() || t.getLabel().toLowerCase().startsWith("xor ")
					|| t.getLabel().toLowerCase().startsWith("and ") || t.getLabel().toLowerCase().startsWith("tree ")
					|| t.getLabel().toLowerCase().startsWith("start_initial")
					|| t.getLabel().toLowerCase().startsWith("t_start")
					|| t.getLabel().toLowerCase().startsWith("t_end") || t.getLabel().toLowerCase().startsWith("end")
					|| t.getLabel().toLowerCase().startsWith("start"))
				result.add(t);
		});
		;
		// for (Transition t : transitions)
		// if (t.getLabel().isEmpty() || t.getLabel().toLowerCase().startsWith("tau")
		// || t.getLabel().toLowerCase().equals("sink") || t.getLabel().toLowerCase().equals("source")
		// || r.matcher(t.getLabel()).find() || t.getLabel().toLowerCase().startsWith("xor ")
		// || t.getLabel().toLowerCase().startsWith("and ") || t.getLabel().toLowerCase().startsWith("tree ")
		// || t.getLabel().toLowerCase().startsWith("start_initial")
		// || t.getLabel().toLowerCase().startsWith("t_start")
		// || t.getLabel().toLowerCase().startsWith("t_end") || t.getLabel().toLowerCase().startsWith("end")
		// || t.getLabel().toLowerCase().startsWith("start"))
		// result.add(t);

		return result;
	}

	/***
	 * This function retrieve the silent transitions from this.silentTrans that contains eTran as it's transition from Mark enabled transitions
	 */
	private CopyOnWriteArrayList<Transition> getSilentTransitions(CopyOnWriteArrayList<Transition> STs,
			Transition eTran) {
		CopyOnWriteArrayList<Transition> result = new CopyOnWriteArrayList<Transition>();
		STs.stream().forEach(st -> {
			if (this.silentTrans.contains(st) && this.silentSuccTransitions.get(st).contains(eTran))
				result.add(st);
		});
		return result;
	}

	private Transition getTransition(String activity) {
		String eventActLC = activity.toLowerCase().trim();
		for (Transition transition : transitions) {
			if (transition.getLabel().toLowerCase().trim().equals(eventActLC.toLowerCase().trim())
					|| transition.getName().toLowerCase().trim().equals(eventActLC.toLowerCase().trim()))
				return transition;
		}
		return null;
	}

	// should be changed to only remove the silent transition only if all its successors are enabled from that mark
	/*** This function update the enabled transition set by replacing the silent transitions with their successors */
	private Set<Transition> updateEnableTransitions(Set<Transition> enabledTrans) {
		Set<Transition> updated = new HashSet<>();
		for (Transition transition : enabledTrans) {
			if (silentTrans.contains(transition)) {
				updated.addAll(this.silentSuccTransitions.get(transition));
			} else
				updated.add(transition);
		}
		return updated;
	}

	// Correlation functions//

	/***
	 * This function try to fire an event's activity using a case marking First, it checks if the marking exist in the marksEnabledTransition HMap, if it there then check if activity belongs to the enabled transitions, if yes then return new marking after firing the event; if not return null. Secondly, if the mark is not in HM, then load the marking on the model and check the enabled
	 */

	public Marking fireActivity(String activity, Marking caseMark) {
		try {
			Transition eTran = getTransition(activity);
			Marking currentPEMarking = null;
			ReadWriteLock lock = new ReentrantReadWriteLock();

			// in case of start event
			if (caseMark == null || this.startTrans.contains(eTran)) {
				if (this.marksNextMark.containsKey(this.naturalMark)
						&& this.marksNextMark.get(this.naturalMark).containsKey(eTran)) {
					currentPEMarking = (Marking) this.marksNextMark.get(this.naturalMark).get(eTran).clone();
				} else {
					CopyOnWriteArrayList<Transition> enabledTrans = null;
					// ReadWriteLock lock = new ReentrantReadWriteLock();

					try {
						lock.writeLock().lock();
						this.pModel.loadNaturalMarking();
						this.naturalMark = (Marking) this.pModel.getMarking().clone();

						this.pModel.fire(eTran);
						currentPEMarking = (Marking) pModel.getMarking().clone();
						if (!marksEnabledTransition.containsKey(currentPEMarking)) {
							enabledTrans = new CopyOnWriteArrayList<>(this.pModel.getEnabledTransitions());
							// check if enabledTrans contain silent to substitute it with the enabled Successors
							// marksEnabledTransition.put(currentPEMarking, updateEnableTransitions(enabledTrans));
							// i will remove the silent transition only if all its successors can be reached by this mark
							marksEnabledTransition.put(currentPEMarking, enabledTrans);
							if (!this.marksNextMark.containsKey(this.naturalMark)) {
								ConcurrentHashMap<Transition, Marking> temp = new ConcurrentHashMap<Transition, Marking>(
										1);
								temp.put(eTran, currentPEMarking);
								marksNextMark.put(this.naturalMark, temp);
							} else {
								marksNextMark.get(this.naturalMark).put(eTran, currentPEMarking);
							}
						}
					} finally {
						lock.writeLock().unlock();
					}
					// return currentPEMarking;//fireETran(this.naturalMark, eTran);
				}

			}// in case of other events
			else if (this.marksEnabledTransition.containsKey(caseMark)) {
				// the event is enabled directly fired before
				if (this.marksNextMark.containsKey(caseMark) && this.marksNextMark.get(caseMark).containsKey(eTran))
					return (Marking) this.marksNextMark.get(caseMark).get(eTran).clone();
				// enabled by current mark
				if (this.marksEnabledTransition.get(caseMark).contains(eTran))
					return fireETran(caseMark, eTran);
				else {
					/* In case of enabled silent transition; filtered the enabled ST based on eTran */
					CopyOnWriteArrayList<Transition> silentTran = getSilentTransitions(
							new CopyOnWriteArrayList<>(this.marksEnabledTransition.get(caseMark)), eTran);
					if (!silentTran.isEmpty()) {
						currentPEMarking = (Marking) fireSilentTran(caseMark, null, eTran, silentTran).clone();
					}
				}

			}

			return currentPEMarking;
		} catch (Exception e) {
			logger.info("----error triggered in CorrelationManager possibleCasesBasedOnModel check error fie ------");
			logger.error(e);
			throw e;
		}
	}

	private Marking fireETran(Marking caseMark, Transition eTran) {
		Marking currentPEMarking = null;
		ReadWriteLock lock = new ReentrantReadWriteLock();
		CopyOnWriteArrayList<Transition> enabledTrans = null;

		try {
			lock.writeLock().lock();
			this.pModel.loadMarking(caseMark);
			this.pModel.fire(eTran);
			currentPEMarking = (Marking) pModel.getMarking().clone();
			if (!marksEnabledTransition.containsKey(currentPEMarking)) {
				enabledTrans = new CopyOnWriteArrayList<Transition>(this.pModel.getEnabledTransitions());
				// check if enabledTrans contain silent to substitute it with the enabled Successors
				marksEnabledTransition.put(currentPEMarking, enabledTrans);

			}
		} finally {
			lock.writeLock().unlock();
		}
		if (!this.marksNextMark.containsKey(caseMark)) {
			ConcurrentHashMap<Transition, Marking> temp = new ConcurrentHashMap<Transition, Marking>(1);
			temp.put(eTran, currentPEMarking);
			marksNextMark.put(caseMark, temp);
		} else {
			marksNextMark.get(caseMark).put(eTran, currentPEMarking);
		}

		return currentPEMarking;
	}

	/*** This function fires the silent transition that has eTran as its successor recursively till its enabled to fire eTran or its not allowed at all */

	private synchronized Marking fireSilentTran(Marking caseMark, Marking afterSTmark, Transition eTran,
			CopyOnWriteArrayList<Transition> silentTran_) {
		Marking newMark = null;
		ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

		CopyOnWriteArrayList<Transition> enabledTrans = null;

		try {
			lock.writeLock().lock();
			if (afterSTmark == null) {
				this.pModel.loadMarking(caseMark);
				// System.out.println("1-Fire Activity" + eTran.getLabel() + " -- caseMark = " + caseMark.toString());

			} else {
				this.pModel.loadMarking(afterSTmark);
				// System.out.println("r1-Fire Activity" + eTran.getLabel() + " -- afterSTmark = " + afterSTmark.toString());

			}
			for (Transition st : silentTran_) {
				this.pModel.fire(st);
				// System.out.println("FOR Fire silent Activity" + st.getLabel());

			}

			afterSTmark = (Marking) this.pModel.getMarking().clone();
			// System.out.println("AFTER Fire SILENTS -- afterSTmark = " + afterSTmark.toString());

			enabledTrans = new CopyOnWriteArrayList<>(this.pModel.getEnabledTransitions());
		} finally {
			lock.writeLock().unlock();
		}
		if (enabledTrans.contains(eTran)) {

			// try {
			// lock.writeLock().lock();
			// this.pModel.fire(eTran);
			// newMark = (Marking) pModel.getMarking().clone();
			// enabledTrans = new CopyOnWriteArrayList<>(this.pModel.getEnabledTransitions());
			//// System.out.println("aFTER Fire Activity" + eTran.getLabel() + " -- newMark = " + newMark.toString());
			//
			// } finally {
			// lock.writeLock().unlock();
			// }
			// if (!marksEnabledTransition.containsKey(newMark)) {
			// // enabledTrans = this.pModel.getEnabledTransitions();
			// // check if enabledTrans contain silent to substitute it with the enabled Successors
			// marksEnabledTransition.put(newMark, enabledTrans);
			// this.marksEnabledTransition.get(caseMark).add(eTran);
			// if (!this.marksNextMark.containsKey(caseMark)) {
			// ConcurrentHashMap<Transition, Marking> temp = new ConcurrentHashMap<Transition, Marking>(1);
			// temp.put(eTran, newMark);
			// marksNextMark.put(caseMark, temp);
			// } else {
			// marksNextMark.get(caseMark).put(eTran, newMark);
			// }
			// }
			// marksEnabledTransition.get(caseMark).add(eTran);

			// return newMark;
			return fireETran(afterSTmark, eTran);
		} else {
			CopyOnWriteArrayList<Transition> silentTran = getSilentTransitions(new CopyOnWriteArrayList<>(enabledTrans),
					eTran);
			if (silentTran != null) {
				// System.out.println("RECURSIVE CALL Fire SILENTS -- afterSTmark = " + afterSTmark.toString());

				return fireSilentTran(caseMark, afterSTmark, eTran, silentTran);
			} else
				return null;

		}

		// return newMark;

	}

	public boolean isFinalMark(Marking m) {
		if (this.marksEnabledTransition.get(m).isEmpty())
			return true;
		return false;
	}

	public int countActivities() {
		// TODO Auto-generated method stub
		return this.transitions.size();
	}

	/*** Return true if the event executed a start activity */
	public boolean isStartE(String activity) {
		for (Transition transition : startTrans) {
			if (transition.getLabel().toLowerCase().trim().equals(activity.toLowerCase().trim())
					|| transition.getName().toLowerCase().trim().equals(activity.toLowerCase().trim()))
				return true;
		}
		return false;
	}
}
