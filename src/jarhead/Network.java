package jarhead;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue; //added
import java.util.Random;

/**
 * iterates through network performing forward propogation.
 * 
 * @author ElectricIsotope
 *
 */

//TODO: PRIMARY: Rewrite with streams. everything here should be java 8 immutable method (setup is overrated. sorting to depth will suffice).

//TODO: REFACTOR
public class Network {
	// PERSISTENT DATA STRUCTURES
	private Map<Integer, Float> srecurrentPositions = new HashMap<Integer, Float>(); // a flag for weight and node
																						// position of recurrent
																						// connections
	private List<Integer> singene = new ArrayList<Integer>(); // in connections
	private List<Integer> soutgene = new ArrayList<Integer>(); // out connections
	private List<Float> scheckWeight = new ArrayList<Float>(); // connection weights
	private List<Integer> soutputs = new ArrayList<Integer>(); // output nodes
	private List<Integer> sinputs = new ArrayList<Integer>(); // input nodes

	// PRIORITY: OPTIMIZE SETUP, INTEGRATE RUN METHOD INTO SETUP TO
	// SIMPLIFY/REFACTOR RUN. STORE RUN ALGORITHM AS SORTED CONNECTIONWEIGHT, INGENE
	// AND OUTGENE IN QUEUE STRUCTURE FOR QUICK SINGLY LINKED-LIST BASED ALGORITHM
	// (LOOK AHEAD ONE
	// INDEX TO SEE WHEN WE SHOULD ACCUMULATE AND ACTIVATE)

	// CONSTRUCTOR
	public Network(Genome genome) {
		genome.getConnectionGenes().forEach((x, l) -> { // NOTE: setup constructor is in theory parallel already due to
														// foreach method NOTE: foreach method must be on a concurrent
														// structure i.e.: concurrentHashMap. TODO: log system threads
														// to verify
			if (l.isExpressed()) {
				singene.add(l.getInNode());
				soutgene.add(l.getOutNode());
				scheckWeight.add(l.getWeight());
			}
		});
		// consider constructing flags for singene w.r.t. soutgene to create depth.
		// (concurrent)HashMap for parallel computation given depth keys? yas.

		for (int i = 0; i < singene.size(); i++) { // place recurrent positions.
			if (singene.get(i).equals(soutgene.get(i)) && !srecurrentPositions.containsKey(singene.get(i))) {
//				System.out.println("recurrent exception! @ " + soutgene.get(i) + " : " + singene.get(i));
				srecurrentPositions.put(soutgene.remove(i), scheckWeight.remove(i));
				singene.remove(i);
			}
		}
		genome.getNodeGenes().forEach((k, v) -> { // mark input and output nodes for start/finish of current run()
													// method
			if (v.getType() == NodeGene.TYPE.INPUT) {
				sinputs.add(v.getId());
			} else if (v.getType() == NodeGene.TYPE.OUTPUT) {
				soutputs.add(v.getId());
			}
		});
	}


	// TODO: make all infinite loops finite. fix squisher algorithm. fix static data
	// structure divergence in setup vs. run.

	// TODO: organize into depth layers. matrix multiplication with minimum memory
	// access < data structure manipulation processing time (think recursive
	// solution on
	// the stack time). use stream method to not alter data structure in run method.

	// this may fix the above todo: for(;depth<maxDepth;++) as a finite solution.
	// remember to give parallel access to data structures (synchronized hashmap--
	// how does this differ from stream? either or?) to prevent memory access
	// bottleneck in gpu and multithreading otherwise. would like to write flexible
	// code to fallback to multithread if not gpu supported through JNI (like
	// arapari).

	public List<Float> run(List<Float> sensors) { // REFACTOR AS SOON AS OPERATIONAL
		// TEMPORARY DATA STRUCTURES

		Map<Integer, Float> recurrentPositions = new HashMap<Integer, Float>(srecurrentPositions); // TODO: replace
																									// static
																									// assignments with
																									// stream methods
		List<Integer> ingene = new ArrayList<Integer>(singene);
		List<Integer> outgene = new ArrayList<Integer>(soutgene);
		List<Float> checkWeight = new ArrayList<Float>(scheckWeight);

		List<Integer> outputs = new ArrayList<Integer>(soutputs);
		List<Integer> inputs = new ArrayList<Integer>(sinputs); // not clearing old lists...

		List<Float> connectionSignals = new ArrayList<Float>(); // runtime data structures
		List<Integer> nodeSignals = new ArrayList<Integer>();
		Map<Integer, Float> recurrentSignals = new HashMap<Integer, Float>();

		if (sensors.size() != inputs.size()) {
			System.out.println("FATAL ERROR: INPUTS AND SENSORS DO NOT ALIGN" + sensors + inputs);
			System.exit(0);
		}

		// INITIALIZE
		// too many loops for this procedure. while(!inputs.isEmpty) is only needed due
		// to for() index not being adjusted after remove operation
		while (!inputs.isEmpty()) { // prepare the network for forward propagation (inputs I.E.: row 0).
			for (int i = 0; i < inputs.size(); i++) {
				int inputVal = inputs.remove(i);
				float sensorVal = sensors.remove(i);
				while (ingene.contains(inputVal)) {
					for (int g = 0; g < ingene.size(); g++) {
						if (ingene.get(g).equals(inputVal)) {
							nodeSignals.add(outgene.remove(g));
							connectionSignals.add(checkWeight.remove(g) * sensorVal); // multiply weight by input value
							ingene.remove(g);
						}
					}
				}
			}
		}

		final long startTime = System.currentTimeMillis();

		// GR8 Squisher 2.0
		for (int n = 0; n < nodeSignals.size(); n++) { // sum initial nodeSignals then propagating through hidden nodes
			Integer val = nodeSignals.get(n);
			for (int m = n + 1; m < nodeSignals.size(); m++) {
				if (val.equals(nodeSignals.get(m))) {
					connectionSignals.set(n, connectionSignals.get(n) + connectionSignals.remove(m));
					nodeSignals.remove(m);
					n = 0;// TODO: fix initial condition so this can be k--
				}
			}
		}

		// EXECUTE
		while (!ingene.isEmpty() && !outgene.isEmpty()) { // too many loops for this procedure
			for (int i = 0; i < nodeSignals.size(); i++) {
				if (outgene.contains(nodeSignals.get(i)) || outputs.contains(nodeSignals.get(i))) {
					continue;
				}

				// check for recurrent positions and initialize signals or process signals.
				if (recurrentPositions.containsKey(nodeSignals.get(i))) { // this needs to see squisher in time...
					int val = nodeSignals.get(i); // this condition must be before default condition so recurrent
													// connections are handled marginally prior to forward propagation
					if (recurrentSignals.containsKey(val)) {
						connectionSignals.add(recurrentSignals.remove(val)); // send activated connectionSignals
						nodeSignals.add(val);

						recurrentSignals.put(val, recurrentPositions.remove(val) * connectionSignals.get(i));
						recurrentPositions.remove(val);
					} else { // initialize recurrent signals hashmap for that value.
						recurrentSignals.put(val, recurrentPositions.remove(val) * connectionSignals.get(i));
						recurrentPositions.remove(val); // ERROR: removed but not resolved below
					}
				} else {

					int val = nodeSignals.get(i);

					if (outputs.contains(val)) {
						System.out.println("FATAL ERROR IN THE LOOP: DESTROYING OUTNODE IN NODESIGNALS: " + val);
						System.out.println(ingene);
						System.out.println(outgene);
						System.out.println(nodeSignals);
					}

					float outSignal = (float) (1f / (1f + Math.exp((-4.8f * connectionSignals.remove(i)))));
					nodeSignals.remove(i);

					while (ingene.contains(val)) {
						int j = ingene.indexOf(val);

						// process non-recurrent signals which have summed all incoming
						// connectionSignals
						connectionSignals.add((checkWeight.remove(j)) * outSignal);
						nodeSignals.add(outgene.remove(j));
						ingene.remove(j);
					}
				}
				// GR8 squisher 2.0
				for (int k = 0; k < nodeSignals.size(); k++) {
					Integer vals = nodeSignals.get(k);
					for (int m = k + 1; m < nodeSignals.size(); m++) {
						if (vals.equals(nodeSignals.get(m))) {
							connectionSignals.set(k, connectionSignals.get(k) + connectionSignals.remove(m));
							nodeSignals.remove(m);
							k = 0; // TODO: fix initial condition so this can be k--
						}
					}
				}
			} // EOF
		} // EOW

		final long endTime = System.currentTimeMillis();

		for (int i = 0; i < nodeSignals.size(); i++) {
			for (int j = i + 1; j < nodeSignals.size(); j++) {
				if (nodeSignals.get(i).compareTo(nodeSignals.get(j)) >= 0) { // should never have equal to zero. can
																				// we
																				// leave this to infinite loop in
																				// error
																				// case?
					nodeSignals.add(i, nodeSignals.remove(j));
					connectionSignals.add(i, connectionSignals.remove(j));
					i = 0; // does this work.
				}
			}
		}

		// TODO: move activation up a condition
		for (int i = 0; i < connectionSignals.size(); i++) { // final activation
			connectionSignals.set(i, (float) (1f / (1f + Math.exp(-4.8f * connectionSignals.get(i))))); // move this up
																										// a condition
		}

		return (connectionSignals); // ensure that connectionSignals keep index values of nodes consistent.
									// since connections can arrive at different times based on topologies, sort the
									// outputs by incrementing nodeSignal values (output 1 comes before 2 etc.)

	}
}
