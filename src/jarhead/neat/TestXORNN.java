package jarhead.neat;

import java.text.DecimalFormat;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import jarhead.ConnectionGene;
import jarhead.Genome;
import jarhead.Network;
import jarhead.NodeGene;
import jarhead.NodeGene.TYPE;
import jarhead.Counter;
import jarhead.Evaluator;
import jarhead.Chromosome;
import jarhead.Ancestors;
import jarhead.PointOfMutation;

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
		//NOTE: last input is not technically bias as it is considered before activation.

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
		Evaluator eva = new Evaluator(5, parent2, connectionInnovation, nodeInnovation) {
			@Override
			public float evaluateGenome(Genome g) {
				float totalDistance = 0f;
				Network net = new Network(g);

				for (int i = 0; i < input.length; i++) { 
					float[] inputs = { input[i][0], input[i][1], input[i][2] };
					
					for (float a : inputs) {
						newList.add(a);
					}
					List<Float> values = net.run(newList);
					newList.clear();

					float distance = (float) Math.sqrt(Math.pow(correctResult[i] - values.get(0), 2));

//					 for(float input : inputs){
//						 System.out.println("given input: " + input);
//					 }
//					 System.out.println("With output: " + values.get(0));
//					 System.out.println("Error: "+ Math.sqrt(Math.pow(distance, 2)));

					totalDistance += distance;
					values.clear();
				}

				if (g.getConnectionGenes().size() > 30) { // try to favor smaller solutions
					totalDistance += 1f * (g.getConnectionGenes().size() - 30);
				}

				return 100f - totalDistance; // just make value ridiculously high until debuged
			}
		};

		for (int i = 0; ; i++) {
			eva.evaluate();

			System.out.println("Generation: " + i);
			System.out.println("Generation: " + i);
			System.out.println("Highest Score: " + eva.getHighestScore());

			if(eva.getHighestScore() >= 100.0f) {
				System.out.println("BEST RUN: ");
				System.out.println(eva.getFittestGenome());
				//System.out.println(eva.getFittestPOM());

				for(int x = 0; x < 2; x++){
				Network net = new Network(eva.getFittestGenome());
				float totalDistance = 0f;
				for (int l = 0; l < input.length; l++) { 
					float[] inputs = { input[l][0], input[l][1], input[l][2] };
					for (float a : inputs) {
						newList.add(a);
					}
					List<Float> values = net.run(newList);
					newList.clear();

					float distance = (float) Math.sqrt(Math.pow(correctResult[l] - values.get(0), 2));
					System.out.println("Value: " + values.get(0) + " Result: " + correctResult[l] + " error: " + distance);

					totalDistance += distance;
					System.out.println("Value thus far: " + totalDistance);
					values.clear();
				}

				System.out.println("Final value: " + (100f - totalDistance)); // just make value ridiculously high until debuged
				}

			}
			System.out.print("\n");
		}
/*		System.out.println("BEST RUN: " + eva.getHighestScore());
		System.out.println(eva.getFittestGenome());
		System.out.println(eva.getFittestPOM());
		Network bnet = new Network(eva.getFittestGenome());
		for (int i = 0; i < input.length; i++) {
			float[] inputs = { input[i][0], input[i][1], input[i][2] };

			for (float a : inputs) {
				newList.add(a);
			}
			List<Float> values = bnet.run(newList);
			System.out.println("RETURNED: " + values.get(0) + " for input: " 
				+ inputs[0] + " , " + inputs[1] + " with expected value: " + correctResult[i]);

			newList.clear();
			values.clear();
		}*/
	}
}
