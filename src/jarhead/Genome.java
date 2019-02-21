package jarhead;

import java.util.Collection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Random;
import java.util.stream.Collectors;

import jarhead.NodeGene.TYPE;
import jarhead.neat.NetworkPrinter;

import java.io.*;

//TODO: Refactor, possibly into chromosome class for crossover mechanics. backup old network checking methods and cleanup
/**
 * Main Genome class.
 * 
 * @author Hydrozoa
 * @see https://www.youtube.com/watch?v=1I1eG-WLLrY&t=333s
 * @author Updated by: ElectricIsotope
 */

public class Genome implements Serializable {
	private static final long serialVersionUID = 129348938L;

	private final float PROBABILITY_PERTURBING = 0.9f; // TODO: move this up to evaluator with the rest of the
														// hyperparameters verify with k.stanley on the importance and
														// variation of this value

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
		if (!this.setDepth()) {
			System.out.println("ERROR: BROKEN TOPOLOGY IN GENOME");
			System.exit(0);
		}
	}

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
				con.setWeight(con.getWeight() * (r.nextFloat() * 4f - 2f));
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

		genomes.remove(this);
		int tries = 0;
		boolean success = false;

		Integer[] nodeInnovationNumbers = new Integer[nodes.keySet().size()];
		nodes.keySet().toArray(nodeInnovationNumbers);

		// used to keep mutation consistent with respect to maxConnections method
		List<ConnectionGene> attempts = new LinkedList<ConnectionGene>();
		attempts.addAll(connections.values());

		while (tries < this.maxConnections() && success == false) {

			Integer keyNode1 = nodeInnovationNumbers[r.nextInt(nodeInnovationNumbers.length)];
			Integer keyNode2 = nodeInnovationNumbers[r.nextInt(nodeInnovationNumbers.length)];

			NodeGene node1 = nodes.get(keyNode1);
			NodeGene node2 = nodes.get(keyNode2);
			float weight = r.nextFloat() * 2f - 1f;

			// TODO: add inNode == outNode to conditions for connectionImpossible as
			// cicularity models unfolded recurrent connections sufficiently
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
			} // connectionImpossible shouldnt count for tries++

			boolean connectionExists = false;

			// only if connection exists in attempts should tries++ (and attempts should
			// remove the respective connection)
			for (ConnectionGene con : connections.values()) { // change this to attempts
				if (con.getInNode() == node1.getId() && con.getOutNode() == node2.getId()) { // existing connection
					if (attempts.contains(con)) {
						connectionExists = true;
						attempts.remove(con);
					}
					break;
				} else if (con.getInNode() == node2.getId() && con.getOutNode() == node1.getId()) { // existing
																									// connection
					if (attempts.contains(con)) {
						connectionExists = true;
						attempts.remove(con);
					}
					break;
				}
			}

			// local ConnectionGene check
			if (connectionImpossible) {
				continue;
			}
			if (connectionExists) {
				tries++; // if connectionExists then try++ once for each existing connection. if
							// connectionImpossible dont increase tries
				continue;
			}

			ConnectionGene newCon = new ConnectionGene(reversed ? node2.getId() : node1.getId(),
					reversed ? node1.getId() : node2.getId(), weight, true);

			// TODO: refactor globalCheck into ConnectionGene constructor and move
			// connections.put. if this is the only time globalCheck is called (should be
			// true given nodeGene's globalCheck) it is appropriate to inline into this
			// method (Law of Dimiter wrt OOP). Also need to refactor addConnectionMutation
			newCon.globalCheck(genomes, innovation);
			connections.put(newCon.getInnovation(), newCon);

			if (!this.setDepth()) {
				connections.remove(newCon.getInnovation());
				this.setDepth();
			} else {
				success = true;
			}
		}
		if (success == false) {
//			System.out.println("DETECTED: maxAttempt reached, genome contains all possible connections!");
		}
		if (!this.setDepth()) {
			System.out.println("BROKEN INSIDE CONNECTION MUTATION");
		}
	}
	public int getMaxDepth() {
		int depth;
		depth = nodes.values().stream().sorted((n1, n0) -> {
			return n0.getDepth() - n1.getDepth();
		}).collect(Collectors.toList()).get(0).getDepth();
		
		return depth;
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
		genomes.remove(this);

		ConnectionGene conSearch = (ConnectionGene) connections.values().toArray()[r.nextInt(connections.size())];
		while (!conSearch.isExpressed() || conSearch.getInNode() == conSearch.getOutNode()) {
			conSearch = (ConnectionGene) connections.values().toArray()[r.nextInt(connections.size())];
		}
		final ConnectionGene con = conSearch;

		NodeGene inNode = nodes.get(conSearch.getInNode());
		NodeGene outNode = nodes.get(conSearch.getOutNode());

		con.disable();

		List<ConnectionGene> ins;
		List<ConnectionGene> outs;

		NodeGene newNode;
		ConnectionGene inToNew = null;
		ConnectionGene newToOut = null;

		// SCAN GENOMES//
		for (Genome a : genomes) { // Both connections must be in same topology
			ins = a.getConnectionGenes().values().parallelStream()
					.filter(c -> c.getInNode() == con.getInNode() && c.isExpressed() && c.getInNode() != c.getOutNode())
					.collect(Collectors.toList());
			outs = a.getConnectionGenes().values().parallelStream().filter(
					c -> c.getOutNode() == con.getOutNode() && c.isExpressed() && c.getInNode() != c.getOutNode())
					.collect(Collectors.toList());

			for (ConnectionGene in : ins) {
				Optional<ConnectionGene> match = outs.parallelStream().filter(o -> o.getInNode() == in.getOutNode())
						.findAny();
				// check local nodes to allow a given connection to be split multiple times
				if (match.isPresent() && !nodes.containsKey(in.getOutNode())) {
					newToOut = new ConnectionGene(match.get());
					newToOut.setWeight(1f);

					inToNew = new ConnectionGene(in);
					inToNew.setWeight(con.getWeight());
					break; // break from genome scan
				}
			}
			if (newToOut != null && inToNew != null) { // Unused code
				break;
			}
		}

		if (inToNew != null) {
			// previous node exists in global gene pool (genomes)
			// the previous method should be sufficient to globalCheck wrt innovation
			// numbers
			newNode = new NodeGene(TYPE.HIDDEN, inToNew.getOutNode());
		} else { // this node is novel
			newNode = new NodeGene(TYPE.HIDDEN, nodeInnovation.updateInnovation());

			inToNew = new ConnectionGene(inNode.getId(), newNode.getId(), 1f, true,
					connectionInnovation.updateInnovation());

			newToOut = new ConnectionGene(newNode.getId(), outNode.getId(), con.getWeight(), true,
					connectionInnovation.updateInnovation());
		}

		nodes.put(newNode.getId(), newNode);
		connections.put(inToNew.getInnovation(), inToNew);
		connections.put(newToOut.getInnovation(), newToOut);

		if (!this.setDepth()) {
			System.out.println("\n\n IMPOSSIBLE \n\n");

			System.out.println("Connections: " + inToNew.getInNode() + "->" + inToNew.getOutNode() + " "
					+ newToOut.getInNode() + "->" + newToOut.getOutNode());
			System.out.println(this);

			NetworkPrinter testing = new NetworkPrinter(this);
			testing.displayGraph();

			System.out.println("\n\n IMPOSSIBLE \n\n");
		}
	}

	/**
	 * Performs crossover using map.containsKey check of innovation number. (very
	 * java friendly translation).
	 * 
	 * Needs to consider case where genomes have same fitness. The more fit parents
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

	// innies and outties can be expected wrt connection disabling in crossover.
	// TODO: add bool parameter for equal fitness (random assignment of disjoint and
	// excess genes) (ablate test this as well)
	public static Genome crossover(Genome parent1, Genome parent2, Random r) {
		Genome child = new Genome();

		for (NodeGene parent1Node : parent1.getNodeGenes().values()) {
			child.addNodeGene(new NodeGene(parent1Node)); // should we only inherit parent1 nodes? what about the more
															// fit genome?
		}
		for (ConnectionGene parent1Connection : parent1.getConnectionGenes().values()) {

			if (parent2.getConnectionGenes().containsKey(parent1Connection.getInnovation())) {
				ConnectionGene childConGene = r.nextBoolean() ? new ConnectionGene(parent1Connection)
						: new ConnectionGene(parent2.getConnectionGenes().get(parent1Connection.getInnovation()));

				if (!childConGene.isExpressed()) {
					if (r.nextBoolean()) {
						childConGene.enable(); // add a random chance to enable as per stanely pg.109.
					} // TODO: add probability variable as a hyperparameter.
				} // "there is a preset chance that an inherited gene is disabled if it is
					// disabled in either parent" Stanley p.109 : also need to disable

				child.addConnectionGene(childConGene);

			} else { // disjoint or excess gene
				ConnectionGene childConGene = new ConnectionGene(parent1Connection); // didn't share innovation number
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
		if (child.setDepth()) {
			return child;
		} else {
			return parent1; // ensure parent1 doesnt exist in genome otherwise two identical objects exist
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
	// TODO: since all of these methods are static consider refactoring into a
	// metrics class.
	public static float compatibilityDistance(Genome genome1, Genome genome2, float c1, float c2, float c3) {
		// TODO: make distance metric functions more efficient
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
		List<Integer> nodeKeys1 = asSortedList(genome1.getNodeGenes().keySet());
		List<Integer> nodeKeys2 = asSortedList(genome2.getNodeGenes().keySet());

		int highestInnovation1 = nodeKeys1.get(nodeKeys1.size() - 1);
		int highestInnovation2 = nodeKeys2.get(nodeKeys2.size() - 1);
		int indices = Math.max(highestInnovation1, highestInnovation2);

		for (int i = 0; i <= indices; i++) { // loop through genes -> i is innovation numbers
			NodeGene node1 = genome1.getNodeGenes().get(i);
			NodeGene node2 = genome2.getNodeGenes().get(i);
			if (node1 != null && node2 != null) {
				// both genomes have the gene w/ this innovation number
				matchingGenes++;
			}
		}

		// count congruent connection genes
		List<Integer> conKeys1 = asSortedList(genome1.getConnectionGenes().keySet());
		List<Integer> conKeys2 = asSortedList(genome2.getConnectionGenes().keySet());

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

		List<Integer> nodeKeys1 = asSortedList(genome1.getNodeGenes().keySet());
		List<Integer> nodeKeys2 = asSortedList(genome2.getNodeGenes().keySet());

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

		List<Integer> conKeys1 = asSortedList(genome1.getConnectionGenes().keySet());
		List<Integer> conKeys2 = asSortedList(genome2.getConnectionGenes().keySet());

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

		List<Integer> nodeKeys1 = asSortedList(genome1.getNodeGenes().keySet());
		List<Integer> nodeKeys2 = asSortedList(genome2.getNodeGenes().keySet());

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

		List<Integer> conKeys1 = asSortedList(genome1.getConnectionGenes().keySet());
		List<Integer> conKeys2 = asSortedList(genome2.getConnectionGenes().keySet());

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

		List<Integer> conKeys1 = asSortedList(genome1.getConnectionGenes().keySet());
		List<Integer> conKeys2 = asSortedList(genome2.getConnectionGenes().keySet());

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
	 * @param c collection of integersQc
	 * @return assorted list
	 */
	private static List<Integer> asSortedList(Collection<Integer> c) {
		List<Integer> list = new ArrayList<Integer>();
		list.clear();
		list.addAll(c);
		java.util.Collections.sort(list);
		return list;
	}

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
		boundaryConnections = inputNodes * (hiddenNodes + outputNodes) + hiddenNodes * outputNodes;

		if (hiddenNodes == 0) {
			return boundaryConnections;
		} else {
			for (int i = 1; i <= hiddenNodes; i++) {
				next = prev + (i - 1);
				prev = next;
			}
			return next + boundaryConnections + hiddenNodes; // connections with self permissible
			
		}
	}

	/**
	 * Assigns depth to a genome. Ensures all nodes have minimum amounts of
	 * connections and no circularity is present
	 * 
	 * @return true if genome passes
	 */
	public boolean setDepth() {
		List<ConnectionGene> buffer = new ArrayList<ConnectionGene>(connections.size());
		Queue<ConnectionGene> connectionSignals = new LinkedList<ConnectionGene>(); // expanding ring buffer

		nodes.forEach((i, n) -> {
			n.setDepth(0);
		});

		// filter recurrent connections
		buffer = connections.values().parallelStream().filter(c -> c.getInNode() != c.getOutNode() && c.isExpressed())
				.collect(Collectors.toList());

		// check hanging inNodes
		if (!buffer.parallelStream().map(c -> c.getInNode()).collect(Collectors.toList())
				.containsAll(nodes.values().parallelStream()
						.filter(n -> n.getType() == NodeGene.TYPE.HIDDEN || n.getType() == NodeGene.TYPE.INPUT)
						.map(n -> n.getId()).collect(Collectors.toList()))) {
			System.out.println("Innie");
			return false;
		}
		// check hanging outNodes
		if (!buffer.parallelStream().map(c -> c.getOutNode()).collect(Collectors.toList())
				.containsAll(nodes.values().parallelStream()
						.filter(n -> n.getType() == NodeGene.TYPE.HIDDEN || n.getType() == NodeGene.TYPE.OUTPUT)
						.map(n -> n.getId()).collect(Collectors.toList()))) {
			System.out.println("Outtie");
			return false;
		}

		// BEGING SORTDEPTH
		// initialize input connectionSignals
		for (int c : nodes.values().parallelStream().filter(n -> n.getType() == NodeGene.TYPE.INPUT).map(n -> n.getId())
				.collect(Collectors.toList())) {
			connectionSignals
					.addAll(buffer.parallelStream().filter(g -> g.getInNode() == c).collect(Collectors.toList()));
		}

		int depth;
		for (depth = 1; !buffer.isEmpty(); depth++) {
			if (connectionSignals.isEmpty()) {
				return false; // circularity
			}

			// remove all connectionSignals from buffer and clear connectionSignals
			buffer.removeAll(connectionSignals);
			connectionSignals.clear();

			// step forward one depth in network
			List<Integer> match = buffer.parallelStream().map(m -> m.getOutNode()).collect(Collectors.toList());
			connectionSignals.addAll(
					buffer.parallelStream().filter(c -> !match.contains(c.getInNode())).collect(Collectors.toList()));

			for (ConnectionGene a : connectionSignals) {
				nodes.get(a.getInNode()).setDepth(depth);
			}
		}
		// last assignment is to output nodes
		for (NodeGene a : nodes.values().stream().filter(n -> n.getType() == TYPE.OUTPUT)
				.collect(Collectors.toList())) {
			a.setDepth(depth - 1); // must be -1 from previous iteration
		}

		return true;
	}

}