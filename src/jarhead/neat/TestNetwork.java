package jarhead.neat;

import java.util.LinkedList;
import java.util.Random;

import jarhead.ConnectionGene;
import jarhead.Counter;
import jarhead.Genome;
import jarhead.Network;
import jarhead.NodeGene;
import jarhead.NodeGene.TYPE;

// TODO: add additional depth and layers and test NewNetwork with simple counting activation function (dont return Float f but +1 or other simple change
public class TestNetwork {
	public static void main(String[] args) {
		Random r = new Random();

		Counter nodeInnovation = new Counter();
		Counter connectionInnovation = new Counter();
		Genome parent2 = new Genome(); // make sure to get innovation correct. do missing innovation numbers allow for
		// void crossover/mutations?

		int n1 = nodeInnovation.updateInnovation();
		int n2 = nodeInnovation.updateInnovation();
		int n3 = nodeInnovation.updateInnovation();
		int n4 = nodeInnovation.updateInnovation();
		int n5 = nodeInnovation.updateInnovation();

		NodeGene node1 = new NodeGene(TYPE.INPUT, n1);
		NodeGene node2 = new NodeGene(TYPE.INPUT, n2);
		NodeGene node3 = new NodeGene(TYPE.INPUT, n3);
		NodeGene node4 = new NodeGene(TYPE.OUTPUT, n4);
		NodeGene node5 = new NodeGene(TYPE.HIDDEN, n5);

		parent2.addNodeGene(node1);
		parent2.addNodeGene(node2);
		parent2.addNodeGene(node3);
		parent2.addNodeGene(node4);
		parent2.addNodeGene(node5);
		int c1 = connectionInnovation.updateInnovation();
		int c2 = connectionInnovation.updateInnovation();
		int c3 = connectionInnovation.updateInnovation();
		int c4 = connectionInnovation.updateInnovation();
		int c5 = connectionInnovation.updateInnovation();
		parent2.addConnectionGene(new ConnectionGene(n1, n4, 0.5f, true, c1));
		parent2.addConnectionGene(new ConnectionGene(n2, n4, 0.5f, true, c3));
		parent2.addConnectionGene(new ConnectionGene(n3, n4, 0.5f, true, c2));
		parent2.addConnectionGene(new ConnectionGene(n5, n4, 0.5f, true, c4));
		parent2.addConnectionGene(new ConnectionGene(n1, n5, 0.5f, true, c5));

		if (parent2.setDepth()) { // TODO: move sort to genome constructor
			System.out.println("Successful sort");
			parent2.getNodeGenes().forEach((i, n) -> {
				System.out.println("Printing Node: " + i);
				System.out.println("Depth: " + n.getDepth());
				System.out.println(parent2.getMaxDepth());
			});
		} else {
			System.out.println("ERROR: hanging node or circularity!");
			System.exit(0);
		}
		Network testNew = new Network(parent2);
		LinkedList<Float> sensors = new LinkedList<Float>();
		sensors.add(2f);
		sensors.add(2f);
		sensors.add(2f);

		testNew.run(sensors);
	}
}
