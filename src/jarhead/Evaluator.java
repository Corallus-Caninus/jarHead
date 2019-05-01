package jarhead;

//import java.util.ArrayList;
//import java.util.Collections;
//import java.util.Comparator;
//import java.util.HashMap;
//import java.util.Iterator;
//import java.util.LinkedList;
//import java.util.List;
//import java.util.Map;
//import java.util.Optional;
//import java.util.Random;
import java.util.stream.*;
import java.util.*;

/**
 * Evaluator class.
 */

public abstract class Evaluator {

	private Counter connectionInnovation;
	private Counter nodeInnovation;

	private Random random = new Random();

	/* Constants for tuning */
//	private float C1 = 1.0f; // why is probability perturbing not included here?
//	private float C2 = 1.0f;
//	private float C3 = 0.4f;
//	private float DT = 20.0f;
	private final float MUTATION_PERTURBING_RATE = 0.9f;
	private final float MUTATION_RATE = 0.05f;
	private final float ADD_CONNECTION_RATE = 0.01f;
	private final float ADD_NODE_RATE = 0.01f;
	private int SCARCITY_OF_RESOURCES = 0; // temporary. fails with gaps in reward

	private int populationSize;
	private int fertility;

	private List<Genome> genepool;
	private List<Genome> nextGenGenomes;
	private Map<Integer, ConnectionGene> globalConnections; // TODO: make concurrent

	private Ancestors lineage;
	private Integer prevResource, nextResource = null;
	private Integer scoreRate = 0;

	private Map<Genome, Float> scoreMap;
	private float highestScore;
	private Genome fittestGenome;

	/**
	 * Evaluator constructor.
	 * 
	 * @param populationSize       size of population to be used in Gene pool.
	 * @param startingGenome       initial Genome topology. Consider adding a bias
	 *                             input node as per k.Stanley.
	 * @param nodeInnovation       ID number of each node as an iterated counting
	 *                             solution.
	 * @param connectionInnovation Innovation number for connections. Identical to
	 *                             nodeInnovation but used for fitness comparison.
	 * @see https://groups.yahoo.com/neo/groups/neat/conversations/messages/6707
	 */
	public Evaluator(int populationSize, Genome startingGenome, Counter connectionInnovation, Counter nodeInnovation) {
		this.populationSize = populationSize;
		this.fertility = populationSize;
		this.connectionInnovation = connectionInnovation;
		this.nodeInnovation = nodeInnovation;
		this.globalConnections = new HashMap<Integer, ConnectionGene>();

		genepool = new ArrayList<Genome>(populationSize);

		globalConnections.putAll(startingGenome.getConnectionGenes());

		// randomize initial weights as per stanley
		for (int i = 0; i < populationSize; i++) {
			startingGenome.mutation(random, MUTATION_PERTURBING_RATE);
			genepool.add(new Genome(startingGenome));
		}
		lineage = new Ancestors(genepool); // need to put until full.

		nextGenGenomes = new ArrayList<Genome>(populationSize);
		scoreMap = new HashMap<Genome, Float>();
	}

	/**
	 * Runs one generation.
	 * 
	 * 1.Place genomes into species 2.Remove unused species 3.Evaluate genomes and
	 * assign score 4.put best genomes from each species into next generation
	 * 5.Breed the rest of the genomes
	 */
	public void evaluate() {
		// Reset species for next generation

		scoreMap.clear();
		nextGenGenomes.clear();
		highestScore = Float.MIN_VALUE;// TODO: DEPRACATED
		fittestGenome = null;

		// Place genomes into species
		System.out.println("Placing genomes into species..");
		lineage.migrate(genepool);

		System.out.println("Evaluating genomes and assigning score..");
		// Evaluate genomes and assign score
		for (Genome g : genepool) {
			float score = evaluateGenome(g);
			if (highestScore < score)
				highestScore = score;
			scoreMap.put(g, score);
		}
		lineage.speciate(scoreMap);

		System.out.println(genepool.size());
		System.out.println("Respawning lacking genepool... " + (genepool.size() - populationSize));// + respawn

		PointOfMutation respawn = lineage.swap(random, populationSize);
		while (genepool.size() < populationSize && respawn != null) {
//			PointOfMutation nextSwap = lineage.swap(random, populationSize);
//			if(nextSwap!=null) { //need to build a list if using this method else immediately stops last swapin table
//				respawn=nextSwap;
//			}
			Genome representative = new Genome(respawn.mascot);
			respawn.members.add(representative);
			scoreMap.put(representative, respawn.highScore);
			genepool.add(representative);
		}
		System.out.println("swaping complete.." + (genepool.size() - populationSize));

		// Breed the rest of the genomes
		System.out.println("Performing crossover..");

		// TODO: build a species list and crossover in parallel
		while (nextGenGenomes.size() < populationSize) {

			PointOfMutation PoM = getRandomPOMBiasedResources(lineage, random);
			if (PoM == null) { // if no POM is found (all are depleted)
				break; // because POM is biased to highscore this causes early breakage
			}

			Genome p1 = getRandomGenomeBiasedFitness(PoM, scoreMap, random);
			Genome p2 = getRandomGenomeBiasedFitness(PoM, scoreMap, random);
			if (p1 == null || p2 == null) {
				PoM.members.clear(); // scoreMap is trash.
				System.out.println("DUMPING POM");
			} else {
				//bootstrapping hack
				if (lineage.POMs.entrySet().stream().filter(e -> e.getValue() == 1).findFirst().get().getKey() != PoM
						|| lineage.POMs.keySet().size() > 1) {
					PoM.lifetime--; // consume crossover resource. need better name
				}

				Genome child;
				if (scoreMap.get(p1) >= scoreMap.get(p2)) {
					child = Genome.crossover(p1, p2, random, ADD_CONNECTION_RATE); // pass in add connection rate for
																					// reactivation
				} else { // Is this due to innovation number?
					child = Genome.crossover(p2, p1, random, ADD_CONNECTION_RATE);
				} // else they are equal so disjoint and excess genes must be randomized
					// respectively not between parents.
				if (child != null) { // successful crossover
					if (random.nextFloat() < MUTATION_RATE) {
						child.mutation(random, MUTATION_PERTURBING_RATE);
					}
					if (random.nextFloat() < ADD_CONNECTION_RATE) {
						// System.out.println("Adding connection mutation...");
						child.addConnectionMutation(random, connectionInnovation, globalConnections);
					}
					if (random.nextFloat() < ADD_NODE_RATE) {
						// System.out.println("Adding node mutation...");
						child.addNodeMutation(random, connectionInnovation, nodeInnovation, globalConnections);
					}
					//if using resources only on loss of fitness condition 
					//evaluate here, compare and consume if appropriate
					//tolerates steep drops in fitness but also consumes for minor fitness loss
					//how can this be made jitter proof? threshold of rate of loss? too complicated?
					PoM.members.add(child);
					nextGenGenomes.add(child);
				}
			}
		}

		lineage.updateInnovations(nextGenGenomes);

		genepool = nextGenGenomes;
		nextGenGenomes = new ArrayList<Genome>();
	}

	/**
	 * Select a random PointOfMutation from the Ancestors list biased towards POMs
	 * with higher score values. (Fitness of a species/epsilon exploitation)
	 * 
	 * @param random random seed.
	 * @return randomly selected species.
	 */
	// TODO: why is random passed in if this belongs to the same class and not
	// static?
	// was highScore
	private PointOfMutation getRandomPOMBiasedResources(Ancestors lineage, Random random) {
		double completeWeight = 0.0; // sum of probabilities of selecting each species - selection is more probable
										// for species with higher fitness
		// filter out empty POMs as they arent considered here
		List<PointOfMutation> filteredPOMs = lineage.POMs.keySet().parallelStream().filter(p -> !p.members.isEmpty())
				.filter(p -> p.lifetime != 0).collect(Collectors.toList());
		completeWeight = filteredPOMs.parallelStream().map(p -> p.highScore).reduce(0f, (s1, s2) -> s1 + s2);

		double r = Math.random() * completeWeight;
		double countWeight = 0.0;
		// POMs sorted to bias towards higher scores.
		for (PointOfMutation p : filteredPOMs.parallelStream().sorted((p1, p2) -> p1.highScore.compareTo(p2.highScore))
				.collect(Collectors.toList())) {
			countWeight += p.highScore /* * lineage.POMs.get(p) */;
			if (countWeight >= r) {
				return p;
			}
		}
		return null; // and swapin
//		throw new RuntimeException("Couldn't find a PointOfMutation...");
	}

	/**
	 * Select a random Genome from a PointOfMutation biased towards fitness.
	 * 
	 * @param selectFrom species to select Genome from.
	 * @param random     random seed.
	 * @return selected genome.
	 */
	private Genome getRandomGenomeBiasedFitness(PointOfMutation selectFrom, Map<Genome, Float> scoreMap,
			Random random) {
		double completeWeight = 0.0; // sum of probabilities of selecting each genome - selection is more probable
										// for genomes with higher fitness
		List<Genome> nonChildren = selectFrom.members.parallelStream().filter(g -> scoreMap.containsKey(g))
				.collect(Collectors.toList());
		for (Genome g : nonChildren) {
			completeWeight += scoreMap.get(g);
		}
		double r = Math.random() * completeWeight;
		double countWeight = 0.0;
		for (Genome g : nonChildren) {
			countWeight += scoreMap.get(g);
			if (countWeight >= r) {
				return g;
			}
		}
		System.out.println("Discarding selection of POM: " + selectFrom + " scoreSum is: " + completeWeight);
		return null;
	}

	/**
	 * Reduce crossover of current species based on ability to exploit their
	 * respective niches (Fertillity of species/epsilon exploration) WIP
	 * 
	 * @deprecated
	 */

	private int ScarcityofResources(int population) {
		nextResource = (int) Math.ceil(scoreMap.values().parallelStream().reduce(0f, (v1, v2) -> v1 + v2));

		if (nextResource < 0) {
			System.out.println("ERROR: genepool total score is negative..");
			System.exit(0);
		}
		if (prevResource == null) {
			scoreRate = 0;
			prevResource = 0;
		}
		System.out.println("SoR DEBUG: " + scoreRate + " " + nextResource + " " + prevResource);

		if (nextResource - prevResource < 0) { // lost fitness momentum
			scoreRate = (int) nextResource - prevResource;
			prevResource = nextResource;
			return population - 2; // slow leak
		} else {
			prevResource = nextResource;
			return population;
		}
	}

	protected abstract float evaluateGenome(Genome genome); // protected: must be called inside subclass and abstract:
	// implemented with @Override method

	public float getHighestScore() {
		return lineage.POMs.keySet().parallelStream().max((p1, p2) -> p1.highScore.compareTo(p2.highScore))
				.get().highScore;
	}

	public Genome getFittestGenome() {
		return lineage.POMs.keySet().parallelStream().max((p1, p2) -> p1.highScore.compareTo(p2.highScore))
				.get().mascot;
	}

	public PointOfMutation getFittestPOM() {
		return lineage.POMs.keySet().parallelStream().max((p1, p2) -> p1.highScore.compareTo(p2.highScore)).get();

	}
}
