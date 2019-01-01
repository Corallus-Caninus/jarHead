package jarhead.neat;

import java.util.ArrayList;
import java.util.List;

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
 * @author hydrozoa
 */
public class TestNEAT {

	public static void main(String[] args) {
		String filename = "genome.out";

		Counter nodeInnovation = new Counter();
		Counter connectionInnovation = new Counter();

		// build the initial topology.
		Genome parent2 = new Genome(); // make sure to get innovation correct. do missing innovation numbers allow for
										// void crossover/mutations?

		int n1 = nodeInnovation.getInnovation();
		int n2 = nodeInnovation.getInnovation();
		int n3 = nodeInnovation.getInnovation();
		int n4 = nodeInnovation.getInnovation();
		int n5 = nodeInnovation.getInnovation();

//		int n6 = nodeInnovation.getInnovation();
//		int n7 = nodeInnovation.getInnovation();

		NodeGene node1 = new NodeGene(TYPE.INPUT, n1);
		NodeGene node2 = new NodeGene(TYPE.INPUT, n2);
		NodeGene node3 = new NodeGene(TYPE.OUTPUT, n3);
		NodeGene node4 = new NodeGene(TYPE.OUTPUT, n4);
		NodeGene node5 = new NodeGene(TYPE.OUTPUT, n5);

//		NodeGene node6 = new NodeGene(TYPE.HIDDEN, n6);
//		NodeGene node7 = new NodeGene(TYPE.HIDDEN, n7);

		parent2.addNodeGene(node1);
		parent2.addNodeGene(node2);
		parent2.addNodeGene(node3);
		parent2.addNodeGene(node4);
		parent2.addNodeGene(node5);

//		parent2.addNodeGene(node6);
//		parent2.addNodeGene(node7);

		int c1 = connectionInnovation.getInnovation();
		int c2 = connectionInnovation.getInnovation();
		int c3 = connectionInnovation.getInnovation();
		int c4 = connectionInnovation.getInnovation();
		int c5 = connectionInnovation.getInnovation();
		int c6 = connectionInnovation.getInnovation();

		parent2.addConnectionGene(new ConnectionGene(n1, n3, 0.5f, true, c1));
		parent2.addConnectionGene(new ConnectionGene(n1, n4, 0.5f, true, c5));
		parent2.addConnectionGene(new ConnectionGene(n1, n5, 0.5f, true, c2));
		parent2.addConnectionGene(new ConnectionGene(n2, n3, 0.5f, true, c6));
		parent2.addConnectionGene(new ConnectionGene(n2, n4, 0.5f, true, c3));
		parent2.addConnectionGene(new ConnectionGene(n2, n5, 0.5f, true, c4));

//		for debugging:		
//		eval.getFittestGenome().maxConnections();
//		System.exit(0);

		writeGenome(parent2, "startGenome");
		writeCounter(connectionInnovation, "lastConnectionInnovation");
		writeCounter(nodeInnovation, "lastNodeInnovation");

		// overrides evaluation function (fitness function) to get highest weight sum
		// (check hydroneat).
		Evaluator eval = new Evaluator(100, parent2, nodeInnovation, connectionInnovation) {
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

		// ConnectionGene.globalCheck test.
		System.out.println("innovation check against: " + parent2.getConnectionGenes().get(0).getInnovation());
		System.out.println(parent2.getConnectionGenes().get(0).globalCheck(eval.getGenomes()).getInnovation());
		System.out.println("versus: ");
		System.out.println(new ConnectionGene(100, 101, 0.5f, true, 12).globalCheck(eval.getGenomes()).getInnovation());
		System.exit(0);
		// WORKING

		for (int i = 0; i < 1000; i++) {
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
		} // was below

//		Network network = new Network(eval.getFittestGenome());// complete. pass only fittest genome since that SEEMS to be the only one passed over generations per species
		// over generations per species
		List<Float> sensors = new ArrayList<Float>();
		sensors.add(1f);
		sensors.add(2f); // now network.run(sensors);

		Network network = new Network(eval.getFittestGenome());// complete. pass only fittest genome since that SEEMS to
																// be the only one passed
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