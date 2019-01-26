package jarhead;

import java.util.Collection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.Stack;
import java.util.stream.Collectors;

import jarhead.NodeGene.TYPE;

import java.io.*;

//TODO: Refactor, possibly into chromosome class for crossover mechanics. backup old network checking methods and cleanup

/**
 * Main Genome class.
 * 
 * @author Hydrozoa
 * @see https://www.youtube.com/watch?v=1I1eG-WLLrY&t=333s
 * @author Updated by: ElectricIsotope
 */

public class Genome implements Serializable { // serializable allows classes to be written to disk and sent over IP/TCP
												// for distributed systems. too slow. send input and output data over
												// IP/TCP. send outputs after a minimum delay to prevent bias
												// against networked/distributed genomes
	private static final long serialVersionUID = 129348938L;

	private static List<Integer> tmpList1 = new ArrayList<Integer>(); // buffer list
	private static List<Integer> tmpList2 = new ArrayList<Integer>();

	private final float PROBABILITY_PERTURBING = 0.9f;

	private Map<Integer, ConnectionGene> connections; // Integer map key is equivalent to connection innovation
	private Map<Integer, NodeGene> nodes; // Integer map key is equivalent to node innovation aka node ID

	/**
	 * Constructs node and connection HashMap.
	 */
	public Genome() {
		nodes = new HashMap<Integer, NodeGene>();
		connections = new HashMap<Integer, ConnectionGene>(); // why HashMap.
	}

	/**
	 * Inherits node and connection HashMap.
	 * 
	 * @param toBeCopied Initializes Genome as toBeCopied's Genome.
	 */
	public Genome(Genome toBeCopied) {
		nodes = new HashMap<Integer, NodeGene>();
		connections = new HashMap<Integer, ConnectionGene>();

		for (Integer index : toBeCopied.getNodeGenes().keySet()) {
			nodes.put(index, new NodeGene(toBeCopied.getNodeGenes().get(index)));
		}

		for (Integer index : toBeCopied.getConnectionGenes().keySet()) {
			connections.put(index, new ConnectionGene(toBeCopied.getConnectionGenes().get(index)));
		}
	}
	// utility functions

	/**
	 * Adds a node gene to a Genome.
	 * 
	 * @param gene Gene to be added as a node.
	 */
	public void addNodeGene(NodeGene gene) {
		nodes.put(gene.getId(), gene);
	}

	/**
	 * Adds a connection gene to a Genome.
	 * 
	 * @param gene Gene to be added as a connection.
	 */
	public void addConnectionGene(ConnectionGene gene) {
		connections.put(gene.getInnovation(), gene);
	}

	/**
	 * Returns connections of a given Genome.
	 */
	public Map<Integer, ConnectionGene> getConnectionGenes() {
		return connections;
	}

	/**
	 * Returns nodes of a given Genome.
	 */
	public Map<Integer, NodeGene> getNodeGenes() {
		return nodes;
	}

	/**
	 * Implements PROBABILITY_PERTURBING which uniformly perturbes weights and
	 * assigns a new weight randomly.
	 * 
	 * @param r random seed
	 */
	public void mutation(Random r) {
		for (ConnectionGene con : connections.values()) { // iterate through connections
			if (r.nextFloat() < PROBABILITY_PERTURBING) { // uniformly perturbing weights
				con.setWeight(con.getWeight() * (r.nextFloat() * 4f - 2f)); // should this be leave the weight alone?
																			// javadoc is correct
			} else { // assigning new weight
				con.setWeight(r.nextFloat() * 4f - 2f);
			}
		}
	}

	/**
	 * Mutates a new connection gene with a random weight.
	 * 
	 * @param r          random seed
	 * @param innovation innovation counter for new connection
	 * @param genomes    global gene pool for innovation number
	 */
	public void addConnectionMutation(Random r, Counter innovation, List<Genome> genomes) {
		int tries = 0;
		boolean success = false;

		Integer[] nodeInnovationNumbers = new Integer[nodes.keySet().size()];
		nodes.keySet().toArray(nodeInnovationNumbers);

		// TODO: Need to ensure same connection isnt
		// attempted multiple times (to truly keep mutation consistent wrt
		// maxConnections)
		while (tries < this.maxConnections() && success == false) {
			tries++;

			// TODO: add memoization to this by storing random numbers that have been tried.
			// and
			// reducing length
			Integer keyNode1 = nodeInnovationNumbers[r.nextInt(nodeInnovationNumbers.length)];
			Integer keyNode2 = nodeInnovationNumbers[r.nextInt(nodeInnovationNumbers.length)];

			NodeGene node1 = nodes.get(keyNode1);
			NodeGene node2 = nodes.get(keyNode2);
			float weight = r.nextFloat() * 2f - 1f;

			boolean reversed = false;
			if (node1.getType() == NodeGene.TYPE.HIDDEN && node2.getType() == NodeGene.TYPE.INPUT) {
				reversed = true;
			} else if (node1.getType() == NodeGene.TYPE.OUTPUT && node2.getType() == NodeGene.TYPE.HIDDEN) {
				reversed = true;
			} else if (node1.getType() == NodeGene.TYPE.OUTPUT && node2.getType() == NodeGene.TYPE.INPUT) {
				reversed = true;
			}

			boolean connectionImpossible = false;
			if (node1.getType() == NodeGene.TYPE.INPUT && node2.getType() == NodeGene.TYPE.INPUT) {
				connectionImpossible = true;
			} else if (node1.getType() == NodeGene.TYPE.OUTPUT && node2.getType() == NodeGene.TYPE.OUTPUT) {
				connectionImpossible = true;
			}

			boolean connectionExists = false;
			for (ConnectionGene con : connections.values()) { // check if connection exists already (locally)
				if (con.getInNode() == node1.getId() && con.getOutNode() == node2.getId()) { // existing connection
					connectionExists = true;
					break;
				} else if (con.getInNode() == node2.getId() && con.getOutNode() == node1.getId()) { // existing
																									// connection
					connectionExists = true;
					break;
				}
			}
			// local check
			if (connectionExists || connectionImpossible) {
				continue;
			}

			ConnectionGene newCon = new ConnectionGene(reversed ? node2.getId() : node1.getId(),
					reversed ? node1.getId() : node2.getId(), weight, true, innovation.updateInnovation());

			newCon = newCon.globalCheck(genomes, innovation);
			connections.put(newCon.getInnovation(), newCon);

//			if (!fasTest(connections, nodes)) { // check local circularity
			if (!this.sortDepth()) {
				connections.remove(newCon.getInnovation());
				innovation.resetInnovation();
				this.sortDepth(); // resort
				continue;
			} else {
				// global check
				// call globalCheck here (to prevent checking all circularity
				// conditions against global gene pool).
				success = true;
			}
		}
		if (success == false) {
			System.out.println("DETECTED: maxAttempt reached, genome contains all possible connections!");
		}
	}

	/**
	 * Adds a newly mutated node and connects it to an existing node, splitting the
	 * existing connection and adding two new connections.
	 *
	 * @param r                    random seed
	 * @param connectionInnovation
	 * @param nodeInnovation
	 */
	public void addNodeMutation(Random r, Counter connectionInnovation, Counter nodeInnovation, List<Genome> genomes) {
		ConnectionGene con = (ConnectionGene) connections.values().toArray()[r.nextInt(connections.size())];
		while (!con.isExpressed() || con.getInNode() == con.getOutNode()) {
			System.out.println("DETECTED: DISABLED/RECURRENT CONNECTION IN NODE MUTATION!");
			con = (ConnectionGene) connections.values().toArray()[r.nextInt(connections.size())];
		}

		NodeGene inNode = nodes.get(con.getInNode());
		NodeGene outNode = nodes.get(con.getOutNode());

		con.disable(); // this is the only time innovations are disabled. didn't stanley implement
						// chance to disable connection elsewhere?

		NodeGene newNode = new NodeGene(TYPE.HIDDEN, nodeInnovation.updateInnovation());

		ConnectionGene inToNew = new ConnectionGene(inNode.getId(), newNode.getId(), 1f, true,
				connectionInnovation.updateInnovation());
		ConnectionGene newToOut = new ConnectionGene(newNode.getId(), outNode.getId(), con.getWeight(), true,
				connectionInnovation.updateInnovation());

		// cannot fail therefore no need to check sortDepth, but still need to call.
		inToNew.globalCheck(genomes, connectionInnovation);
		newToOut.globalCheck(genomes, connectionInnovation);

		nodes.put(newNode.getId(), newNode);
		connections.put(inToNew.getInnovation(), inToNew);
		connections.put(newToOut.getInnovation(), newToOut);
		this.sortDepth();
	}

	/**
	 * Performs crossover using map.containsKey check of innovation number. (very
	 * java friendly translation).
	 * 
	 * Needs to consider case where genes have same fitness. The more fit parents
	 * passes on excess and disjoint genes and matching genes are inherited
	 * randomly. In the case of equal fitness, disjoint and excess genes are also
	 * assigned randomly.
	 * 
	 * Uses random boolean to decide which disjoint genes are passed to child.
	 * 
	 * @param parent1 More fit parent.
	 * @param parent2 Less fit parent.
	 * @param r       random seed.
	 */
	public static Genome crossover(Genome parent1, Genome parent2, Random r) { // TODO: add bool parameter for equal
																				// fitness (random assignment of
																				// disjoint and excess genes)
		Genome child = new Genome();
		boolean abort = false;

		for (NodeGene parent1Node : parent1.getNodeGenes().values()) {
			child.addNodeGene(new NodeGene(parent1Node)); // should we only inherit parent1 nodes? what about the more
															// fit genome?
		}

		for (ConnectionGene parent1Node : parent1.getConnectionGenes().values()) {
			if (parent2.getConnectionGenes().containsKey(parent1Node.getInnovation())) {
				ConnectionGene childConGene = r.nextBoolean() ? new ConnectionGene(parent1Node)
						: new ConnectionGene(parent2.getConnectionGenes().get(parent1Node.getInnovation()));
				if (!childConGene.isExpressed()) {
					if (r.nextBoolean()) {
						childConGene.enable(); // add a random chance to enable as per stanely pg.109.
					} // TODO: add probability variable as a hyperparameter.
				} // "there is a preset chance that an inherited gene is disabled if it is
					// disabled in either parent" Stanley p.109 : also need to disable

				child.addConnectionGene(childConGene);

			} else { // disjoint or excess gene
				ConnectionGene childConGene = new ConnectionGene(parent1Node); // didn't share innovation number
																				// therefore excess or disjoint.
				if (!childConGene.isExpressed()) {
					if (r.nextBoolean()) {
						childConGene.enable();
					} // Same as above TODO but no need for disable chance as is disjoint/excess with
						// no comparison
				}

				child.addConnectionGene(childConGene);

			}
		}

//		if (!fasTest(child.connections, child.nodes)) {
		if (!child.sortDepth()) {
			abort = true;
			System.out.println("ERROR IN CROSSOVER: FEEDBACK ARC SET");
		}

		if (!abort) {
			return child;
		} else {
			return parent1; // TODO: fix very bad solution. would prefer to recursively call crossover with
							// new candidates. Possibly call fasTest in child.connection addition. sometimes
							// it is a random assignment; special case: disjoint/excess from p1
//			System.out.println("Running Again");
//			return crossover(parent1, parent2, r); //crossover sometimes never works. :D
		}
	}

	/**
	 * Returns compatibility distance (direct scrape from paper).
	 * 
	 * @param genome1
	 * @param genome2
	 * @param c1      tuneable weight for excess genes.
	 * @param c2      tuneable weight for disjoint genes.
	 * @param c3      tuneable weight for average weight difference.
	 * @return compatibility distance metric.
	 */
	public static float compatibilityDistance(Genome genome1, Genome genome2, float c1, float c2, float c3) {
		int excessGenes = countExcessGenes(genome1, genome2);
		int disjointGenes = countDisjointGenes(genome1, genome2);
		float avgWeightDiff = averageWeightDiff(genome1, genome2);

		return excessGenes * c1 + disjointGenes * c2 + avgWeightDiff * c3;
	}

	/**
	 * Returns number of matching genes (node and connection/innovation).
	 * 
	 * @param genome1
	 * @param genome2
	 * @return
	 */
	public static int countMatchingGenes(Genome genome1, Genome genome2) {
		int matchingGenes = 0;

		// counts congruent node genes
		List<Integer> nodeKeys1 = asSortedList(genome1.getNodeGenes().keySet(), tmpList1);
		List<Integer> nodeKeys2 = asSortedList(genome2.getNodeGenes().keySet(), tmpList2);

		int highestInnovation1 = nodeKeys1.get(nodeKeys1.size() - 1);
		int highestInnovation2 = nodeKeys2.get(nodeKeys2.size() - 1);
		int indices = Math.max(highestInnovation1, highestInnovation2);

		for (int i = 0; i <= indices; i++) { // loop through genes -> i is innovation numbers
			NodeGene node1 = genome1.getNodeGenes().get(i);
			NodeGene node2 = genome2.getNodeGenes().get(i);
			if (node1 != null && node2 != null) {
				// both genomes has the gene w/ this innovation number
				matchingGenes++;
			}
		}

		// count congruent connection genes
		List<Integer> conKeys1 = asSortedList(genome1.getConnectionGenes().keySet(), tmpList1);
		List<Integer> conKeys2 = asSortedList(genome2.getConnectionGenes().keySet(), tmpList2);

		highestInnovation1 = conKeys1.get(conKeys1.size() - 1);
		highestInnovation2 = conKeys2.get(conKeys2.size() - 1);

		indices = Math.max(highestInnovation1, highestInnovation2);
		for (int i = 0; i <= indices; i++) { // loop through genes -> i is innovation numbers
			ConnectionGene connection1 = genome1.getConnectionGenes().get(i);
			ConnectionGene connection2 = genome2.getConnectionGenes().get(i);
			if (connection1 != null && connection2 != null) {
				// both genomes have the gene w/ this innovation number
				matchingGenes++;
			}
		}

		return matchingGenes;
	}

	// **********************************************************
	// ALL FUNCTIONS BELOW SUPPORT COMPATIBILITY DISTANCE METRICS
	// **********************************************************
	/**
	 * Used for compatibility distance.
	 * 
	 * @param genome1
	 * @param genome2
	 * @return disjoint gene count
	 */
	public static int countDisjointGenes(Genome genome1, Genome genome2) {
		int disjointGenes = 0;

		List<Integer> nodeKeys1 = asSortedList(genome1.getNodeGenes().keySet(), tmpList1);
		List<Integer> nodeKeys2 = asSortedList(genome2.getNodeGenes().keySet(), tmpList2);

		int highestInnovation1 = nodeKeys1.get(nodeKeys1.size() - 1);
		int highestInnovation2 = nodeKeys2.get(nodeKeys2.size() - 1);
		int indices = Math.max(highestInnovation1, highestInnovation2);

		for (int i = 0; i <= indices; i++) {
			NodeGene node1 = genome1.getNodeGenes().get(i);
			NodeGene node2 = genome2.getNodeGenes().get(i);
			if (node1 == null && highestInnovation1 > i && node2 != null) {
				// genome 1 lacks gene, genome 2 has gene, genome 1 has more genes w/ higher
				// innovation numbers
				disjointGenes++;
			} else if (node2 == null && highestInnovation2 > i && node1 != null) {
				disjointGenes++;
			}
		}

		List<Integer> conKeys1 = asSortedList(genome1.getConnectionGenes().keySet(), tmpList1);
		List<Integer> conKeys2 = asSortedList(genome2.getConnectionGenes().keySet(), tmpList2);

		highestInnovation1 = conKeys1.get(conKeys1.size() - 1);
		highestInnovation2 = conKeys2.get(conKeys2.size() - 1);

		indices = Math.max(highestInnovation1, highestInnovation2);
		for (int i = 0; i <= indices; i++) {
			ConnectionGene connection1 = genome1.getConnectionGenes().get(i);
			ConnectionGene connection2 = genome2.getConnectionGenes().get(i);
			if (connection1 == null && highestInnovation1 > i && connection2 != null) {
				disjointGenes++;
			} else if (connection2 == null && highestInnovation2 > i && connection1 != null) {
				disjointGenes++;
			}
		}

		return disjointGenes;
	}

	/**
	 * Used for compatibility distance.
	 * 
	 * @param genome1
	 * @param genome2
	 * @return excess gene count
	 */
	public static int countExcessGenes(Genome genome1, Genome genome2) {
		int excessGenes = 0;

		List<Integer> nodeKeys1 = asSortedList(genome1.getNodeGenes().keySet(), tmpList1);
		List<Integer> nodeKeys2 = asSortedList(genome2.getNodeGenes().keySet(), tmpList2);

		int highestInnovation1 = nodeKeys1.get(nodeKeys1.size() - 1);
		int highestInnovation2 = nodeKeys2.get(nodeKeys2.size() - 1);
		int indices = Math.max(highestInnovation1, highestInnovation2);

		for (int i = 0; i <= indices; i++) {
			NodeGene node1 = genome1.getNodeGenes().get(i);
			NodeGene node2 = genome2.getNodeGenes().get(i);
			if (node1 == null && highestInnovation1 < i && node2 != null) {
				excessGenes++;
			} else if (node2 == null && highestInnovation2 < i && node1 != null) {
				excessGenes++;
			}
		}

		List<Integer> conKeys1 = asSortedList(genome1.getConnectionGenes().keySet(), tmpList1);
		List<Integer> conKeys2 = asSortedList(genome2.getConnectionGenes().keySet(), tmpList2);

		highestInnovation1 = conKeys1.get(conKeys1.size() - 1);
		highestInnovation2 = conKeys2.get(conKeys2.size() - 1);

		indices = Math.max(highestInnovation1, highestInnovation2);
		for (int i = 0; i <= indices; i++) {
			ConnectionGene connection1 = genome1.getConnectionGenes().get(i);
			ConnectionGene connection2 = genome2.getConnectionGenes().get(i);
			if (connection1 == null && highestInnovation1 < i && connection2 != null) {
				excessGenes++;
			} else if (connection2 == null && highestInnovation2 < i && connection1 != null) {
				excessGenes++;
			}
		}

		return excessGenes;
	}

	/**
	 * Used for compatibility distance.
	 * 
	 * @param genome1
	 * @param genome2
	 * @return weight difference to matching genes ratio
	 */
	public static float averageWeightDiff(Genome genome1, Genome genome2) {
		int matchingGenes = 0;
		float weightDifference = 0;

		List<Integer> conKeys1 = asSortedList(genome1.getConnectionGenes().keySet(), tmpList1);
		List<Integer> conKeys2 = asSortedList(genome2.getConnectionGenes().keySet(), tmpList2);

		int highestInnovation1 = conKeys1.get(conKeys1.size() - 1);
		int highestInnovation2 = conKeys2.get(conKeys2.size() - 1);

		int indices = Math.max(highestInnovation1, highestInnovation2);
		for (int i = 0; i <= indices; i++) { // loop through genes -> i is innovation numbers
			ConnectionGene connection1 = genome1.getConnectionGenes().get(i);
			ConnectionGene connection2 = genome2.getConnectionGenes().get(i);
			if (connection1 != null && connection2 != null) {
				// both genomes have the gene w/ this innovation number
				matchingGenes++;
				weightDifference += Math.abs(connection1.getWeight() - connection2.getWeight());
			}
		}

		return weightDifference / matchingGenes;
	}

	/**
	 * Utility function for distance equation metrics. clears given list and adds
	 * all of collection c then sorts the list in ascending order.
	 * 
	 * @param c    collection of integersQc
	 * @param list integer list
	 * @return assorted list
	 */
	private static List<Integer> asSortedList(Collection<Integer> c, List<Integer> list) {
		list.clear();
		list.addAll(c);
		java.util.Collections.sort(list);
		return list;
	}

	// should be able to find all Feedback Arc Set cases from
	// output node(s). test speed for this method vs FASTest.

	// Need to check every nodegene has input and output in
	// nodeGeneMutation (can we do that here? instead? is it faster to check in the
	// loop since we are performing DFS
	// anyways? hanging nodes only exist due to crossover of disabled genes
	// (fundamental to NEAT algorithm per stanely)

	// Is this still viable: Review this.
	/**
	 * @deprecated
	 * 
	 * 			Tests Connections for feedback arc sets and hanging nodes (nodes
	 *             with no connection to input which would break the current forward
	 *             propagation algorithm). uses depth first search algorithm. not
	 *             implemented as crossover/mutation isnt a bottleneck yet. (no need
	 *             but here for future cases). Look to implement in
	 *             connectionMutation.
	 * 
	 * @param connections current connection topology to be passed through
	 * @param nodes       current nodes to find input nodes (hanging node
	 *                    condition).
	 * @param node1       currently N/A
	 * @param node2       the outnode to backwards propagate from in DFS, looking
	 *                    for inputs and checking for hanging node or feedback arc
	 *                    set failure conditions.
	 * @return true if the connection is possible (input reached and no hanging
	 *         input nodes nor feedback arc sets) else false.
	 */
	public static boolean fasDFS(Map<Integer, ConnectionGene> connections, Map<Integer, NodeGene> nodes,
			int checkNode) {

		List<Integer> tmpInConnection = new ArrayList<Integer>();
		List<Integer> tmpOutConnection = new ArrayList<Integer>();

		Stack<Integer> searchStack = new Stack<Integer>();

		connections.forEach((l, p) -> {
			if (p.isExpressed()) {
				if (!(p.getInNode() == p.getOutNode())) { // removes recurrent genes anyways.
					tmpInConnection.add(p.getInNode());
					tmpOutConnection.add(p.getOutNode());
				}
			}
		});

		searchStack.push(checkNode);

		while (!(!tmpOutConnection.contains(checkNode) && searchStack.size() == 1)) {

			if (!tmpOutConnection.contains(searchStack.peek())) {
				searchStack.pop(); // exhausted all outConnections at given node and ready to go up.

			} else {
				int index = tmpOutConnection.indexOf(searchStack.peek());

				if (searchStack.contains(tmpInConnection.get(index))) {
					System.out.println("IN DFS ALG: FEEDBACK ARC CYCLE DETECTED");
					return false;
				}

				searchStack.push(tmpInConnection.get(index));

				tmpInConnection.remove(index);
				tmpOutConnection.remove(index);
			}
		} // EOW
		return true;
	}

	/**
	 * Backward propagate network to look for feedback arc sets (because nodeSignals
	 * wont grow as fast if outputs < inputs (typical ANN architecture)). Does an
	 * initial topology check for hanging nodes (nodes that do not have an expressed
	 * input or output connection) and disregards recurrent connections.
	 * 
	 * @param connections
	 * @param nodes
	 * @return true if no Feedback Arc Set, hanging input node or hanging output
	 *         node is found.
	 */
	// this should not be static connections and nodes are inherited values
	// deprecated after sortDepth
	public static boolean fasTest(Map<Integer, ConnectionGene> connections, Map<Integer, NodeGene> nodes) {
		List<Integer> tmpInConnection = new ArrayList<Integer>();
		List<Integer> tmpOutConnection = new ArrayList<Integer>();

		int x = 0;
		int val;
		List<Integer> nodeSignals = new ArrayList<Integer>();

		// SKIP UNEXPRESSED GENES
		connections.forEach((l, p) -> {
			if (p.isExpressed()) {
				if (p.getInNode() != p.getOutNode()) { // remove recurrent connections.
					tmpInConnection.add(p.getInNode());
					tmpOutConnection.add(p.getOutNode());
				}
			}
		});

		nodes.forEach((k, o) -> {
			if (o.getType() == NodeGene.TYPE.OUTPUT) {
				nodeSignals.add(o.getId());
			}
		});
		// TEST FOR HANGING NODES
		for (NodeGene node : nodes.values()) {
			if (node.getType() == NodeGene.TYPE.HIDDEN) {
				if (!tmpInConnection.contains(node.getId()) || !tmpOutConnection.contains(node.getId())) {
					System.out.println("DETECTED: HANGING INPUT/OUTPUT IN FASTEST");
					return false;
				}
			}
		}

		// CHECK FOR FEEDBACK ARC SETS
		while (!tmpInConnection.isEmpty() && !tmpOutConnection.isEmpty()) {
			x++; // fix bad solution

			if (x > nodeSignals.size()) {
				System.out.println("DETECTED: FEEDBACK ARC IN FASTEST");
				return false;
			}
			for (int i = 0; i < nodeSignals.size(); i++) {
				if (tmpInConnection.contains(nodeSignals.get(i))) {
					continue;
				} else {
					x = 0;
					val = nodeSignals.remove(i);

					while (tmpOutConnection.contains(val)) {
						int j = tmpOutConnection.indexOf(val);

						nodeSignals.add(tmpInConnection.remove(j));
						tmpOutConnection.remove(j);
					}

					for (int k = 0; k < nodeSignals.size(); k++) {
						Integer vals = nodeSignals.get(k);
						for (int m = k + 1; m < nodeSignals.size(); m++) {
							if (vals.equals(nodeSignals.get(m))) {
								nodeSignals.remove(m);
								k = 0; // TODO: fix initial condition so this can be k--
							}
						}
					}
					// end of squisherLite
				}
			}

		}
		return true;
	}

	// TODO: ONLY WORKS IF UNIQUE CONNECTIONGENES ARE ATTEMPTED IN
	// ADDCONNECTIONMUTATION add some form of memoization for mutation attempts
	/**
	 * Calculates the maximum number of connections possible for a given topology
	 * (NodeGenes).
	 * 
	 * @return integer number of connections.
	 */
	public int maxConnections() {
		int hiddenNodes, inputNodes, outputNodes, boundaryConnections;
		int prev = 0, next = 0;

		hiddenNodes = (int) nodes.entrySet().parallelStream()
				.filter(p -> p.getValue().getType() == NodeGene.TYPE.HIDDEN).count();
		outputNodes = (int) nodes.entrySet().parallelStream()
				.filter(p -> p.getValue().getType() == NodeGene.TYPE.OUTPUT).count();
		inputNodes = (int) nodes.entrySet().parallelStream().filter(p -> p.getValue().getType() == NodeGene.TYPE.INPUT)
				.count();
		// first calculate possible connections with input and output (boundary) nodes
		// as they can only be connected once for any respective pair.
		boundaryConnections = inputNodes * (hiddenNodes + outputNodes) + hiddenNodes * outputNodes;
		// all possible connections from boundary nodes

//		System.out.println("Hidden Nodes: " + hiddenNodes);
//		System.out.println("Potential boundary connections: " + boundaryConnections);
		if (hiddenNodes == 0) {
			return boundaryConnections;
		} else {
			for (int i = 1; i <= hiddenNodes; i++) {
				next = prev + (i - 1);
				prev = next;
			}
			return next + boundaryConnections + hiddenNodes; // recurrent connections permissible
			// for recursive but non circular connections:
			// return next + boundaryConnections + hiddenNodes;
			// for circular and recurrent connections:
			// return hiddenNodes*hiddenNodes + hiddenNodes + boundaryConnections
		}
	}

	public boolean sortDepth() {
		// how can previously established depth be used in
		// addConnectionMutation? crossover?
		List<ConnectionGene> buffer = new ArrayList<ConnectionGene>(connections.size());
		Queue<ConnectionGene> connectionSignals = new LinkedList<ConnectionGene>();
		LinkedList<Integer> nodeSignals = new LinkedList<>(); // set?
		int depth = 0, process = 0;
		ConnectionGene head;

		// filter recurrent connections
		buffer = connections.values().stream().filter(c -> c.getInNode() != c.getOutNode() && c.isExpressed())
				.collect(Collectors.toList());

		// check hanging inNodes
		if (!buffer.stream().map(c -> c.getInNode()).collect(Collectors.toList())
				.containsAll(nodes.values().stream()
						.filter(n -> n.getType() == NodeGene.TYPE.HIDDEN || n.getType() == NodeGene.TYPE.INPUT)
						.map(n -> n.getId()).collect(Collectors.toList()))) {
			System.out.println("Innie");
			return false;
		}
		// check hanging outNodes
		if (!buffer.stream().map(c -> c.getOutNode()).collect(Collectors.toList())
				.containsAll(nodes.values().stream()
						.filter(n -> n.getType() == NodeGene.TYPE.HIDDEN || n.getType() == NodeGene.TYPE.OUTPUT)
						.map(n -> n.getId()).collect(Collectors.toList()))) {
			System.out.println("Outtie");
			return false;
		}

		// initialize input nodeSignals and input connectionSignals
		for (Integer a : nodes.values().stream().filter(n -> n.getType() == NodeGene.TYPE.INPUT).map(n -> n.getId())
				.collect(Collectors.toList())) {
			nodeSignals.push(a);
		}
		while (!nodeSignals.isEmpty()) {
			int val = nodeSignals.pop();
			connectionSignals
					.addAll(buffer.stream().filter(c -> c.getInNode() == val).collect(Collectors.toList()));
		}

		// forward propagate the network
		while (!buffer.isEmpty() && !connectionSignals.isEmpty()) {
			while (!nodeSignals.isEmpty()) {
				int val = nodeSignals.pop();
				connectionSignals
						.addAll(buffer.stream().filter(c -> c.getInNode() == val).collect(Collectors.toList()));
			}
			buffer.removeAll(connectionSignals);

			// remove output signals.
			connectionSignals.removeIf(c -> nodes.get(c.getOutNode()).getType() == NodeGene.TYPE.OUTPUT);

			depth++;
			process = connectionSignals.size();
			head = connectionSignals.peek();
			// get connectionSignals and process nodeSignals
			for (int i = 0; i < process; i++) {

				if (buffer.stream().anyMatch(c -> c.getOutNode() == connectionSignals.peek().getOutNode())) {
					connectionSignals.add(connectionSignals.poll());
					if (connectionSignals.peek().equals(head))
						return false;
				} else if (!nodeSignals.contains(connectionSignals.peek().getOutNode())) { // keep nodeSignals unique.
					nodes.get(connectionSignals.peek().getOutNode()).setDepth(depth); // assign depth
					nodeSignals.push(connectionSignals.poll().getOutNode()); // forward propagate
				} else {
					connectionSignals.poll(); // get rid of duplicates
				}
			}
		}
		return true;
	}
}