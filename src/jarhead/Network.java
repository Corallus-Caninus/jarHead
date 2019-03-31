package jarhead;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.concurrent.*;
import java.util.Stack;
import java.util.*;

import jarhead.NodeGene.TYPE;

// NOTE: cyclic connections fully define unrolled recurrent connections. 
// Therefore only need to implement Cyclic connections with activated method.
// implement activated here with cyclic additions.

// TODO: can this entire method be one call to a parallel stream over buffer? (immutable no tmpConnections or buffer)
public class Network {
	// buffer of all connections which is processed during forward propagation
	private Stack<ConnectionGene> tmpConnections = new Stack<ConnectionGene>();
	// connectionSignals which are used during processing from buffer
	private List<ConnectionGene> buffer = new ArrayList<ConnectionGene>();
	// values passed each depth during forward propagation
	private ConcurrentHashMap<Integer, Float> signals = new ConcurrentHashMap<Integer, Float>();//TODO: what are the defficiencies of ConcurrentHashMap?

	private Genome genome;

	public Network(Genome gene) {
		this.genome = gene;
	}

	public List<Float> run(List<Float> sensors) {
		// TODO: sort within constructor not each run.
		// sort connections by depth

		tmpConnections.addAll(genome.getConnectionGenes().values().stream().filter(c -> c.isExpressed())
				.sorted(connectionsSortByDepth).collect(Collectors.toList()));

		// setup
		buffer.addAll(
				tmpConnections.stream().filter(c -> genome.getNodeGenes().get(c.getInNode()).getType() == TYPE.INPUT)
						.collect(Collectors.toList()));

		buffer.forEach(i -> {
			if(!signals.containsKey(i.getInNode())){
				signals.put(i.getInNode(), activate(sensors.get(i.getInNode())));
			}
		});

		tmpConnections.removeAll(buffer);

		if (sensors.size() != genome.getNodeGenes().values().stream().filter(n -> n.getType() == TYPE.INPUT).count()) {
			System.out.println("ERROR: improper input to Network: " + this + "Expecting: "
					+ genome.getNodeGenes().values().stream().filter(n -> n.getType() == TYPE.INPUT).count()
					+ "recieved: " + sensors.size() + " ");
			System.exit(0);
		}

		// Forward propagate
		for (int i = 1; !buffer.isEmpty(); i++) {
			// collect signals per node (multiply by weights and sum)
			buffer.stream().forEachOrdered(c -> { //ordered to prevent initial value race condition
				signals.put(c.getOutNode(),
						zeroIfNull(signals.get(c.getOutNode())) + signals.get(c.getInNode()) * c.getWeight());
			});
			buffer.clear();

			// activate all nodes of this depth
			for (int n : signals.keySet()) {
				if (genome.getNodeGenes().get(n).getDepth() == i) {
					signals.put(n, activate(signals.get(n)));
				} else if (genome.getNodeGenes().get(n).getDepth() < i) {
					signals.remove(n); // can this be called within iterator?
				}
			}

			// get the next row (depth) of nodes
			while (!tmpConnections.isEmpty()
					&& genome.getNodeGenes().get(tmpConnections.peek().getInNode()).getDepth() == i) {
				buffer.add(tmpConnections.pop());
			}
		}

		return signals.values().stream().collect(Collectors.toList());
	}

	public float activate(float f) {
		return (float) (1f / (1f + Math.exp(-4.8f * f)));
	}

	// used for nodes that dont have a previous value in the hashmap
	private static Float zeroIfNull(Float val) {
		return (val == null) ? 0 : val;
	}

	Comparator<ConnectionGene> connectionsSortByDepth = new Comparator<ConnectionGene>() {
		@Override
		public int compare(ConnectionGene c1, ConnectionGene c2) {
			return genome.getNodeGenes().get(c2.getInNode()).getDepth()
					- genome.getNodeGenes().get(c1.getInNode()).getDepth();
		}
	};
}
