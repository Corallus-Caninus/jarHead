package jarhead.neat;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import jarhead.ConnectionGene;
import jarhead.Counter;
import jarhead.Evaluator;
import jarhead.Genome;
import jarhead.Network;
import jarhead.NodeGene;
import jarhead.NodeGene.TYPE;

import java.io.*;

/**
 * Tests a simple evaluator that runs for 100 generations, and scores fitness
 * based on the amount of connections in the network.
 * 
 */
public class TestNEAT {

	public static void main(String[] args) {
		String filename = "genome.out";

		Counter nodeInnovation = new Counter();
		Counter connectionInnovation = new Counter();

		// build the initial topology.
		Genome parent2 = new Genome(); // make sure to get innovation correct. do missing innovation numbers allow for
										// void crossover/mutations?

		int n1 = nodeInnovation.updateInnovation();
		int n2 = nodeInnovation.updateInnovation();
		int n3 = nodeInnovation.updateInnovation();
		int n4 = nodeInnovation.updateInnovation();
		int n5 = nodeInnovation.updateInnovation();

//		int n6 = nodeInnovation.updateInnovation();
//		int n7 = nodeInnovation.updateInnovation();
//		int n8 = parent2.nodeInnovation.updateInnovation();

		NodeGene node1 = new NodeGene(TYPE.INPUT, n1);
		NodeGene node2 = new NodeGene(TYPE.INPUT, n2);
		NodeGene node3 = new NodeGene(TYPE.OUTPUT, n3);
		NodeGene node4 = new NodeGene(TYPE.OUTPUT, n4);
		NodeGene node5 = new NodeGene(TYPE.OUTPUT, n5);

//		NodeGene node6 = new NodeGene(TYPE.HIDDEN, n6);
//		NodeGene node7 = new NodeGene(TYPE.HIDDEN, n7);
//		NodeGene node8 = new NodeGene(TYPE.HIDDEN, n8);

		parent2.addNodeGene(node1);
		parent2.addNodeGene(node2);
		parent2.addNodeGene(node3);
		parent2.addNodeGene(node4);
		parent2.addNodeGene(node5);

//		parent2.addNodeGene(node6);
//		parent2.addNodeGene(node7);
//		parent2.addNodeGene(node8);

		int c1 = connectionInnovation.updateInnovation();
		int c2 = connectionInnovation.updateInnovation();
		int c3 = connectionInnovation.updateInnovation();
		int c4 = connectionInnovation.updateInnovation();
		int c5 = connectionInnovation.updateInnovation();
		int c6 = connectionInnovation.updateInnovation();

//		int c7 = connectionInnovation.updateInnovation();
//		int c8 = connectionInnovation.updateInnovation();
//		int c9 = connectionInnovation.updateInnovation();

//		int c10 = connectionInnovation.updateInnovation();
//		int c11 = connectionInnovation.updateInnovation();
//		int c12 = connectionInnovation.updateInnovation();
		// node innovation n1 == nodeId 0
		parent2.addConnectionGene(new ConnectionGene(n1, n3, 0.5f, true, c1));
		parent2.addConnectionGene(new ConnectionGene(n1, n4, 0.5f, true, c5));
		parent2.addConnectionGene(new ConnectionGene(n1, n5, 0.5f, true, c2));
		parent2.addConnectionGene(new ConnectionGene(n2, n3, 0.5f, true, c6));
		parent2.addConnectionGene(new ConnectionGene(n2, n4, 0.5f, true, c3));
		parent2.addConnectionGene(new ConnectionGene(n2, n5, 0.5f, true, c4));

//		parent2.addConnectionGene(new ConnectionGene(n2, n6, 0.5f, true, c7));
//		parent2.addConnectionGene(new ConnectionGene(n6, n7, 0.5f, true, c8));

//		parent2.addConnectionGene(new ConnectionGene(n7, n5, 0.5f, true, c9));
//		parent2.addConnectionGene(new ConnectionGene(n7, n6, 0.5f, true, c10));
		// test recurrent

		if (parent2.sortDepth()) { // TODO: Implement, verify and check it in
			System.out.println("Successful sort");
			parent2.getNodeGenes().forEach((i, n) -> {
				System.out.println("Printing Node: " + i);
				System.out.println("Depth: " + n.getDepth());
			});
		} else {
			System.out.println("ERROR: hanging node or circularity!");
			System.exit(0);
		}

//		// mutate genome prior to crossover
//		Random r = new Random();
//		List<Genome> genomes = new ArrayList<>();
//		genomes.add(parent2);
//		for (int i = 0; i < 200; i++) {
//			if (r.nextFloat() > 0.90) {
//				parent2.addNodeMutation(r, connectionInnovation, nodeInnovation, genomes);
//			}
//			parent2.addConnectionMutation(r, connectionInnovation, genomes);
//			parent2.addConnectionMutation(r, connectionInnovation, genomes);
//			System.out.println(i);
//		}
//		
//		if (!parent2.sortDepth()) {
//			System.out.println("FAILURE");
//		} else {
//			NetworkPrinter testing = new NetworkPrinter(parent2);
//			testing.displayGraph();
//		}

		writeGenome(parent2, "startGenome"); // testing serializable object
		writeCounter(connectionInnovation, "lastConnectionInnovation");
		writeCounter(nodeInnovation, "lastNodeInnovation");

		// overrides evaluation function (fitness function) to get highest weight sum
		Evaluator eval = new Evaluator(10, parent2, connectionInnovation, nodeInnovation) {
			@Override
			protected float evaluateGenome(Genome evalGenome) {
				float weightSum = 0f;
				for (ConnectionGene cg : evalGenome.getConnectionGenes().values()) {
					if (cg.isExpressed()) {
						weightSum++;
					}
				}
				return weightSum;
			}
		};

		for (int i = 0; i < 300; i++) {
			eval.evaluate();
			System.out.print("Generation: " + i);
			System.out.print("\tHighest fitness: " + eval.getHighestFitness());
			System.out.print("\tAmount of species: " + eval.getSpeciesAmount());
			System.out.print(
					"\tConnections in best performer: " + eval.getFittestGenome().getConnectionGenes().values().size());
			float weightSum = 0;
			for (ConnectionGene cg : eval.getFittestGenome().getConnectionGenes().values()) {
				if (cg.isExpressed()) {
//					weightSum += Math.abs(cg.getWeight());
					weightSum += cg.getWeight();
				}
			}
			System.out.print("\tWeight sum: " + weightSum);
			System.out.print("\n");
			System.out.println("Highest innovation number: "
					+ eval.getFittestGenome().getConnectionGenes().values().stream().map(c -> c.getInnovation())
							.sorted((a, b) -> b.compareTo(a)).collect(Collectors.toList()).get(0));
		}
		//

		Genome tester = eval.getFittestGenome();
		NetworkPrinter testing = new NetworkPrinter(tester);
		if (tester.sortDepth()) {
			System.out.println("Successful sort");
			tester.getNodeGenes().forEach((i, n) -> {
				System.out.println("Printing Node: " + i);
				System.out.println("Depth: " + n.getDepth());
			});
			testing.displayGraph();
		} else {
			System.out.println("ERROR: hanging node or circularity!");

			testing.displayGraph();
		}

		List<Float> sensors = new ArrayList<Float>();
		sensors.add(1f);
		sensors.add(2f); // now network.run(sensors);

		Network network = new Network(eval.getFittestGenome());

//		network.setup(eval.getFittestGenome());
		System.out.println(sensors);
		network.run(sensors); // TODO: make network immutable. use stream method in network.
		System.out.println("NEXT RUN");

//		network.setup(eval.getFittestGenome()); //setup needs to be constructor..
		sensors.add(1f);
		sensors.add(2f); // now network.run(sensors);
		System.out.println(sensors);
		network.run(sensors);

		sensors.add(1f);
		sensors.add(2f); // now network.run(sensors);
		network.run(sensors);

	} // end of main.

	/**
	 * @author ElectricIsotope
	 */
	public static void writeBestGenome(Evaluator eval, String filename) {

		Genome winner = new Genome();

		winner = eval.getFittestGenome();
		// SAVE/LOAD GENOME

		try {
			FileOutputStream file = new FileOutputStream(filename);
			ObjectOutputStream out = new ObjectOutputStream(file);

			out.writeObject(winner);
			out.close();
			file.close();
			System.out.println("genome has been serialized and saved");

		} catch (IOException ex) {
			System.out.println("IOException caught:" + ex);
		}
	}

	public static void writeGenome(Genome genome, String filename) {

		try {
			FileOutputStream file = new FileOutputStream(filename);
			ObjectOutputStream out = new ObjectOutputStream(file);

			out.writeObject(genome);
			out.close();
			file.close();
			System.out.println("genome has been serialized and saved");

		} catch (IOException ex) {
			System.out.println("IOException caught:" + ex);
		}
	}
	// serialize and save highest fitness genome.

	public static Genome loadGenome(String filename) {
		Genome loadWinner = new Genome();
		try {
			FileInputStream inFile = new FileInputStream(filename);
			ObjectInputStream in = new ObjectInputStream(inFile);
			loadWinner = (Genome) in.readObject();

			System.out.println(loadWinner);
			loadWinner.getConnectionGenes().forEach((k, v) -> System.out.println(k + ", " + v));
			loadWinner.getNodeGenes().forEach((k, v) -> System.out.println(k + "," + v)); // working.

			in.close();
			inFile.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return loadWinner;
	}

	public static void writeCounter(Counter count, String filename) {

		try {
			FileOutputStream file = new FileOutputStream(filename);
			ObjectOutputStream out = new ObjectOutputStream(file);

			out.writeObject(count);
			out.close();
			file.close();
			System.out.println("Counter has been serialized and saved");

		} catch (IOException ex) {
			System.out.println("IOException caught:" + ex);
		}
	}

	public static Counter loadCounter(String filename) {
		Counter load = new Counter();
		try {
			FileInputStream inFile = new FileInputStream(filename);
			ObjectInputStream in = new ObjectInputStream(inFile);
			load = (Counter) in.readObject();

			System.out.println(load);

			in.close();
			inFile.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return load;
	}

}