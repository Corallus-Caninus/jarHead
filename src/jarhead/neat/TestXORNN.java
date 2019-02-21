package jarhead.neat;

import java.text.DecimalFormat;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import jarhead.ConnectionGene;
import jarhead.Genome;
import jarhead.OldNetwork;
import jarhead.Network;
import jarhead.NodeGene;
import jarhead.NodeGene.TYPE;
import jarhead.Counter;
import jarhead.Evaluator;

/**
 * 
 * @author Hydrozoa
 *
 */
public class TestXORNN {
	public static void main(String[] args) {
		Random r = new Random();

		Counter nodeInnovation = new Counter();
		Counter connectionInnovation = new Counter();
		DecimalFormat df = new DecimalFormat();
		df.setMaximumFractionDigits(9);

		// first column and second column contains operands, the third column is for
		// bias input
		float[][] input = { { 0f, 0f, 1f }, { 0f, 1f, 1f }, { 1f, 0f, 1f }, { 1f, 1f, 1f } };

		float[] correctResult = { 0f, 1f, 1f, 0f };

		Genome parent2 = new Genome(); // make sure to get innovation correct. do missing innovation numbers allow for
		// void crossover/mutations?

		int n1 = nodeInnovation.updateInnovation();
		int n2 = nodeInnovation.updateInnovation();
		int n3 = nodeInnovation.updateInnovation();
		int n4 = nodeInnovation.updateInnovation();
		System.out.println(nodeInnovation.getInnovation());

		NodeGene node1 = new NodeGene(TYPE.INPUT, n1);
		NodeGene node2 = new NodeGene(TYPE.INPUT, n2);
		NodeGene node3 = new NodeGene(TYPE.INPUT, n3);
		NodeGene node4 = new NodeGene(TYPE.OUTPUT, n4);

		parent2.addNodeGene(node1);
		parent2.addNodeGene(node2);
		parent2.addNodeGene(node3);
		parent2.addNodeGene(node4);
		int c1 = connectionInnovation.updateInnovation();
		int c2 = connectionInnovation.updateInnovation();
		int c3 = connectionInnovation.updateInnovation();
		parent2.addConnectionGene(new ConnectionGene(n1, n4, 0.5f, true, c1));
		parent2.addConnectionGene(new ConnectionGene(n2, n4, 0.5f, true, c3));
		parent2.addConnectionGene(new ConnectionGene(n3, n4, 0.5f, true, c2));

		if (parent2.setDepth()) { // TODO: move initial sort to genome constructor
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

		List<Float> newList = new LinkedList<Float>();
		Evaluator eva = new Evaluator(10000, parent2, connectionInnovation, nodeInnovation) {
			@Override
			public float evaluateGenome(Genome g) {
//				NeuralNetwork net = new NeuralNetwork(g);
//				Network net = new Network(g);
				Network net = new Network(g);

				// System.out.println("===========NEW NETWORK=============");

				float totalDistance = 0f;
				for (int i = 0; i < input.length; i++) { 
					float[] inputs = { input[i][0], input[i][1], input[i][2] };
//					int testVal = r.nextInt(input.length); // needs stochastic sampling for trainning
//					float[] inputs = { input[testVal][0], input[testVal][1], input[testVal][2] };

					// System.out.println("Giving input "+Arrays.toString(inputs));
//					float[] outputs = net.calculate(inputs);
					
					for (float a : inputs) {
						newList.add(a);
					}
					List<Float> values = net.run(newList);
					// System.out.println("Received output "+Arrays.toString(outputs));
					float[] outputs = { values.get(0) };
					newList.clear();

					float distance = (float) Math.abs(correctResult[i] - outputs[0]);
//					 System.out.println("Error: "+distance);
//					totalDistance += Math.pow(distance, 2);
					totalDistance+=distance; // broken for output set: [0,0,0,0]
					values.clear();
				}

				if (g.getConnectionGenes().size() > 30) { // try to favor smaller solutions
					totalDistance += 1f * (g.getConnectionGenes().size() - 30);
				}

//				 System.out.println("Total distance: "+totalDistance);
//				return 100f - totalDistance * 5f;

				return 4f - totalDistance;
//				return totalDistance;
			}
		};
		for (int i = 0; i < 200; i++) {
			eva.evaluate();

			System.out.println("Generation: " + i);
			System.out.println("\tHighest fitness: " + df.format(eva.getHighestFitness()));
//			System.out.println("\tAmount of genomes: "+eva.getGenomeAmount());
			System.out.print("Generation: " + i);
			System.out.print("\tHighest fitness: " + eva.getHighestFitness());
			System.out.print("\tAmount of species: " + eva.getSpeciesAmount());
			System.out.print(
					"\tConnections in best performer: " + eva.getFittestGenome().getConnectionGenes().values().size());
			System.out.println("Highest innovation number: "
					+ eva.getFittestGenome().getConnectionGenes().values().stream().map(c -> c.getInnovation())
							.sorted((a, b) -> b.compareTo(a)).collect(Collectors.toList()).get(0));

			if(eva.getHighestFitness() >= 4.0f) {
				System.out.println("BEST RUN: ");
				Network bnet = new Network(eva.getFittestGenome());
				for (int l = 0; l < input.length; l++) {
					float[] inputs = { input[l][0], input[l][1], input[l][2] };

					for (float a : inputs) {
						newList.add(a);
					}
					List<Float> values = bnet.run(newList);
					System.out.println("RETURNED: " + values.get(0));
					newList.clear();
					values.clear();
				}

				NetworkPrinter testing = new NetworkPrinter(eva.getFittestGenome());
				testing.displayGraph();
			}
			System.out.print("\n");
		}
		System.out.println("BEST RUN: ");
		Network bnet = new Network(eva.getFittestGenome());
		for (int i = 0; i < input.length; i++) {
			float[] inputs = { input[i][0], input[i][1], input[i][2] };

			for (float a : inputs) {
				newList.add(a);
			}
			List<Float> values = bnet.run(newList);
			// System.out.println("Received output "+Arrays.toString(outputs));
//			float[] outputs = {values.get(0)};
			System.out.println("RETURNED: " + values.get(0));
			newList.clear();
			values.clear();
		}

		NetworkPrinter testing = new NetworkPrinter(eva.getFittestGenome());
		testing.displayGraph();
	}
}
