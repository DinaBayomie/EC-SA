package processModel;

import java.util.HashMap;
import java.util.Map;

import org.jbpt.petri.Flow;
import org.jbpt.petri.NetSystem;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.PetrinetGraph;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.graphbased.directed.petrinet.impl.PetrinetFactory;
import org.processmining.models.semantics.petrinet.Marking;

public class PetrinetAdapter {

	private Petrinet pNet;
	private Marking sMarking;
	private Marking fMarking;

	public PetrinetAdapter(NetSystem pModel) {
		pNet = constructNet(pModel);
		sMarking = getInitialMarking(pNet);
		fMarking = getFinalMarking(pNet);
	}

	// used for basic code snippet class in Alignment package
	private Petrinet constructNet(NetSystem pModel) {
		Petrinet pNet = PetrinetFactory.newPetrinet("petrinet");

		// places
		Map<org.jbpt.petri.Place, Place> p2p = new HashMap<org.jbpt.petri.Place, Place>();
		for (org.jbpt.petri.Place p : pModel.getPlaces()) {
			Place pp = pNet.addPlace(p.toString());
			p2p.put(p, pp);
		}

		// transitions
		Map<org.jbpt.petri.Transition, Transition> t2t = new HashMap<org.jbpt.petri.Transition, Transition>();
		for (org.jbpt.petri.Transition t : pModel.getTransitions()) {
			Transition tt = pNet.addTransition(t.getLabel());
			// if (t.isSilent() || t.getLabel().startsWith("tau") || t.getLabel().equals("t2") || t.getLabel().equals("t8")
			// || t.getLabel().equals("complete")) {
			if (t.getLabel().toLowerCase().startsWith("tau") || t.getLabel().toLowerCase().equals("sink")
					|| t.getLabel().toLowerCase().equals("source") || t.getLabel().matches("^t\\d+")) {
				tt.setInvisible(true);
			}
			t2t.put(t, tt);
		}

		// flow
		for (Flow f : pModel.getFlow()) {
			if (f.getSource() instanceof org.jbpt.petri.Place) {
				pNet.addArc(p2p.get(f.getSource()), t2t.get(f.getTarget()));
			} else {
				pNet.addArc(t2t.get(f.getSource()), p2p.get(f.getTarget()));
			}
		}

		// add unique start node
		if (pModel.getSourceNodes().isEmpty()) {
			Place i = pNet.addPlace("START_P");
			Transition t = pNet.addTransition("");
			t.setInvisible(true);
			pNet.addArc(i, t);

			for (org.jbpt.petri.Place p : pModel.getMarkedPlaces()) {
				pNet.addArc(t, p2p.get(p));
			}

		}

		return pNet;
	}

	// used for basic code snippet class in Alignment package
	private Marking getFinalMarking(PetrinetGraph net) {
		Marking finalMarking = new Marking();

		for (Place p : net.getPlaces()) {
			if (net.getOutEdges(p).isEmpty())
				finalMarking.add(p);
		}

		return finalMarking;
	}

	// used for basic code snippet class in Alignment package
	private Marking getInitialMarking(PetrinetGraph net) {
		Marking initMarking = new Marking();

		for (Place p : net.getPlaces()) {
			if (net.getInEdges(p).isEmpty())
				initMarking.add(p);
		}

		return initMarking;
	}

	/**
	 * @return the pNet
	 */
	public Petrinet getpNet() {
		return pNet;
	}

	/**
	 * @param pNet
	 *            the pNet to set
	 */
	public void setpNet(Petrinet pNet) {
		this.pNet = pNet;
	}

	/**
	 * @return the sMarking
	 */
	public Marking getsMarking() {
		return sMarking;
	}

	/**
	 * @param sMarking
	 *            the sMarking to set
	 */
	public void setsMarking(Marking sMarking) {
		this.sMarking = sMarking;
	}

	/**
	 * @return the fMarking
	 */
	public Marking getfMarking() {
		return fMarking;
	}

	/**
	 * @param fMarking
	 *            the fMarking to set
	 */
	public void setfMarking(Marking fMarking) {
		this.fMarking = fMarking;
	}

}
