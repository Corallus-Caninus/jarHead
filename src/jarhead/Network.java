package jarhead;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.Stack;

import jarhead.NodeGene.TYPE;
import jarhead.neat.NetworkPrinter;

// NOTE: cyclic connections fully define unrolled recurrent connections. 
// Therefore only need to implement Cyclic connections with activated method.
// implement activated here with cyclic additions.

// TODO: can this entire method be one call to a parallel stream over buffer? (immutable no exposed tmpConnections or buffer)
public class Network {
	// buffer of all connections which is processed during forward propagation
	private Stack<ConnectionGene> tmpConnections = new Stack<ConnectionGene>();
	// connectionSignals which are used during processing from buffer
	private Stack<ConnectionGene> buffer = new Stack<ConnectionGene>();
	// values passed per forward propagation by depth
	private ConcurrentHashMap<Integer, Float> signals;// should be size of
														// gene.getNodeGenes

	private Genome genome;

	public Network(Genome gene) {
		this.genome = gene;
		signals = new ConcurrentHashMap<Integer, Float>(genome.getConnectionGenes().size());
		// size used until proper removal is implemented per depth

	}

	public List<Float> run(List<Float> sensors) {
		// TODO: remove inNode != outNode condition once addConnectionMutation is
		// patched
		// TODO: sort within constructor not each run.
		tmpConnections.addAll(genome.getConnectionGenes().values().stream()
				.filter(c -> c.isExpressed() && c.getInNode() != c.getOutNode()).sorted((c1, c2) -> {
					return genome.getNodeGenes().get(c2.getOutNode()).getDepth()
							- genome.getNodeGenes().get(c1.getOutNode()).getDepth();
				}).collect(Collectors.toList()));

		// setup DataStructures for forward propagation
		buffer.addAll(
				tmpConnections.stream().filter(c -> genome.getNodeGenes().get(c.getInNode()).getType() == TYPE.INPUT)
						.collect(Collectors.toList()));
		buffer.forEach(i -> signals.put(i.getInNode(), activate(sensors.get(i.getInNode()))));

		tmpConnections.removeAll(buffer); // breaks initial topology
		if (sensors.size() != genome.getNodeGenes().values().stream().filter(n -> n.getType() == TYPE.INPUT).count()) {
			System.out.println("ERROR: improper input to Network: " + this + "Expecting: "
					+ genome.getNodeGenes().values().stream().filter(n -> n.getType() == TYPE.INPUT).count()
					+ "recieved: " + sensors.size() + " ");
			System.exit(0);
		}

		// Forward propagate
		for (int i = 1; !buffer.isEmpty(); i++) {
			buffer.parallelStream().forEach(c -> {
				signals.put(c.getOutNode(),
						zeroIfNull(signals.get(c.getOutNode())) + signals.get(c.getInNode()) * c.getWeight());
			});
			buffer.clear();

			while (!tmpConnections.isEmpty()
					&& genome.getNodeGenes().get(tmpConnections.peek().getInNode()).getDepth() == i) {
				buffer.add(tmpConnections.pop());
			}

			// activate all nodes of this depth
			for (int n : signals.keySet()) {
				if (genome.getNodeGenes().get(n).getDepth() == i) {
					signals.put(n, activate(signals.get(n)));
				}
				// TODO: how can old signals be removed from hashTable else hashTable is size of
				// connections and should have connections.size initial capacity
//				else if (genome.getNodeGenes().get(n).getDepth() < i) {
//					System.out.println("removing a signal...");
//					signals.remove(n); // can this be called within iterator?
//				}

				// why do all depth have same value
//				System.out.println("In the loop with depth: " + i);
//				signals.forEach((a, f) -> {
//					System.out.println("SIGNAL: " + a + ": " + f);
//				});
			}
		}
		// this is a patchwork solution
		signals.keySet().stream()
				.filter(n -> !tmpConnections.stream().map(c -> c.getInNode()).collect(Collectors.toList()).contains(n)
						&& !tmpConnections.stream().map(c -> c.getOutNode()).collect(Collectors.toList()).contains(n)
						&& genome.getNodeGenes().get(n).getType() != TYPE.OUTPUT)
				.forEach(n -> signals.remove(n));

		// TESTING RETURN VALUE"
//		sensors.forEach(s -> {
//			System.out.println("INPUT: " + s);
//		});
//		signals.values().forEach(s -> {
//			System.out.println("OUTPUT: " + s);
//		});
//		signals.keySet().forEach(k -> {
//			System.out.print(k);
//			System.out.println(" " + genome.getNodeGenes().get(k).getDepth());
//		});
		// TESTING RETURN VALUE
		return signals.values().stream().collect(Collectors.toList());
	}

	public float activate(float f) { // will this overload for unboxing?
		return (float) (1f / (1f + Math.exp(-4.8f * f)));
	}

	// used for nodes that dont have a value in the hashmap and summing incoming
	// connections
	private static Float zeroIfNull(Float val) {
		return (val == null) ? 0 : val;
	}
}