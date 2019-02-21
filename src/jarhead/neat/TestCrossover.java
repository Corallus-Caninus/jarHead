package jarhead.neat;

import java.util.LinkedList;
import java.util.Random;

import jarhead.ConnectionGene;
import jarhead.Counter;
import jarhead.Genome;
import jarhead.Network;
import jarhead.NodeGene;
import jarhead.NodeGene.TYPE;

// NOTE: Crossover has passed tests
public class TestCrossover {
	public static void main(String[] args) {
		Random r = new Random();

		Counter nodeInnovation = new Counter();
		Counter connectionInnovation = new Counter();
		Genome parent1 = new Genome(); // make sure to get innovation correct. do missing innovation numbers allow for
		// void crossover/mutations?

		int n1 = nodeInnovation.updateInnovation();
		int n2 = nodeInnovation.updateInnovation();
		int n3 = nodeInnovation.updateInnovation();
		int n4 = nodeInnovation.updateInnovation();
		int n5 = nodeInnovation.updateInnovation();
		int n6 = nodeInnovation.updateInnovation();

		NodeGene node1 = new NodeGene(TYPE.INPUT, n1);
		NodeGene node2 = new NodeGene(TYPE.INPUT, n2);
		NodeGene node3 = new NodeGene(TYPE.INPUT, n3);
		NodeGene node4 = new NodeGene(TYPE.OUTPUT, n4);
		NodeGene node5 = new NodeGene(TYPE.HIDDEN, n5);
		NodeGene node6 = new NodeGene(TYPE.HIDDEN, n6);

		parent1.addNodeGene(node1);
		parent1.addNodeGene(node2);
		parent1.addNodeGene(node3);
		parent1.addNodeGene(node4);
		parent1.addNodeGene(node5);
		
		int c1 = connectionInnovation.updateInnovation();
		int c2 = connectionInnovation.updateInnovation();
		int c3 = connectionInnovation.updateInnovation();
		int c4 = connectionInnovation.updateInnovation();
		int c5 = connectionInnovation.updateInnovation();
		int c6 = connectionInnovation.updateInnovation();
		int c7 = connectionInnovation.updateInnovation();
		int c8 = connectionInnovation.updateInnovation();
		int c9 = connectionInnovation.updateInnovation();
		int c10 = connectionInnovation.updateInnovation();
		parent1.addConnectionGene(new ConnectionGene(n1, n4, 1f, true, c1));
		parent1.addConnectionGene(new ConnectionGene(n2, n4, 1f, false, c3));
		parent1.addConnectionGene(new ConnectionGene(n3, n4, 1f, true, c2));
		
		parent1.addConnectionGene(new ConnectionGene(n2, n5, 1f, true, c4));
		parent1.addConnectionGene(new ConnectionGene(n5, n4, 1f, true, c5));
		parent1.addConnectionGene(new ConnectionGene(n1, n5, 1f, true, c8));

		// NEXT PARENT
		Genome parent2 = new Genome();
		parent1.addNodeGene(node1);
		parent1.addNodeGene(node2);
		parent1.addNodeGene(node3);
		parent1.addNodeGene(node4);
		parent1.addNodeGene(node5);
		parent1.addNodeGene(node6);
		
		parent1.addConnectionGene(new ConnectionGene(n1, n4, 1f, true, c1));
		parent1.addConnectionGene(new ConnectionGene(n2, n4, 1f, false, c3));
		parent1.addConnectionGene(new ConnectionGene(n3, n4, 1f, true, c2));
		
		parent1.addConnectionGene(new ConnectionGene(n2, n5, 1f, true, c4));
		parent1.addConnectionGene(new ConnectionGene(n5, n4, 1f, false, c5));
		parent1.addConnectionGene(new ConnectionGene(n1, n5, 1f, true, c8));

		parent1.addConnectionGene(new ConnectionGene(n5, n6, 1f, true, c6));
		parent1.addConnectionGene(new ConnectionGene(n6, n4, 1f, true, c7));
		parent1.addConnectionGene(new ConnectionGene(n3, n5, 1f, true, c9));
		parent1.addConnectionGene(new ConnectionGene(n1, n6, 1f, true, c10));
		
		
		if (parent1.setDepth()) { // TODO: move sort to genome constructor
			System.out.println("Successful sort");
			parent1.getNodeGenes().forEach((i, n) -> {
				System.out.println("Printing Node: " + i);
				System.out.println("Depth: " + n.getDepth());
				System.out.println(parent1.getMaxDepth());
			});
		} else {
			System.out.println("ERROR: hanging node or circularity!");
			System.exit(0);
		}

		NetworkPrinter testing = new NetworkPrinter(Genome.crossover(parent1, parent2, r));
		testing.displayGraph();
		Network testNew = new Network(parent1);
		LinkedList<Float> sensors = new LinkedList<Float>();
		sensors.add(2f);
		sensors.add(2f);
		sensors.add(2f);

		testNew.run(sensors);
	}
}
