package processModel;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.jbpt.petri.Marking;
import org.jbpt.petri.NetSystem;
import org.jbpt.petri.Place;
import org.jbpt.petri.Transition;

public class PetriNetTS extends NetSystem {

	public PetriNetTS() {
		super();
	}

	@Override
	public synchronized Marking getMarking() {
		ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
		try {
			lock.readLock().lock();
			return (Marking) this.marking;
		} finally {
			lock.readLock().unlock();
		}
	}

	@Override
	public Set<Transition> getEnabledTransitions() {
		Set<Transition> result = new HashSet<Transition>();
		ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
		try {
			lock.writeLock().lock();
			for (Transition t : this.getTransitions()) {
				if (this.getMarkedPlaces().containsAll(this.getPreset(t)))
					result.add(t);
			}
			return result;
		} finally {
			lock.writeLock().unlock();
		}
	}

	@Override
	public synchronized void loadMarking(Marking newMarking) {
		ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
		try {
			lock.writeLock().lock();
			if (newMarking.getPetriNet() != this)
				return;
			if (this.marking.equals(newMarking))
				return;

			this.marking.clear();
			for (Map.Entry<Place, Integer> entry : newMarking.entrySet()) {
				this.marking.put(entry.getKey(), entry.getValue());
			}
		} finally {
			lock.writeLock().unlock();
		}

	}

	@Override
	public synchronized boolean fire(Transition transition) {
		ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
		try {
			lock.writeLock().lock();
			return this.marking.fire(transition);
		} finally {
			lock.writeLock().unlock();
		}
	}

}
