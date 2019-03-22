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
	private float MUTATION_RATE = 0.02f;
	private float ADD_CONNECTION_RATE = 0.2f;
	private float ADD_NODE_RATE = 0.1f;

	private int populationSize;

	private List<Genome> genepool;
	private List<Genome> nextGenGenomes;
	
	private Ancestors lineage;
	private Integer prevResource, nextResource = null;

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
		this.connectionInnovation = connectionInnovation;
		this.nodeInnovation = nodeInnovation;

		genepool = new ArrayList<Genome>(populationSize);
		lineage = new Ancestors(startingGenome);

		// randomize initial weights as per stanley
		for (int i = 0; i < populationSize; i++) {
			startingGenome.mutation(random);
			genepool.add(new Genome(startingGenome));
		}
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
		highestScore = Float.MIN_VALUE;
		fittestGenome = null;

		// Place genomes into species
		System.out.println("Placing genomes into species..");
		lineage.migrate(genepool);//does migrate need to be called? private?
		System.out.println("Total number of species.. " + lineage.POMs);

		System.out.println("Evaluating genomes and assigning score");
		// Evaluate genomes and assign score
		for (Genome g : genepool) {
			float score = evaluateGenome(g);
			if(highestScore < score)
				highestScore = score;
			scoreMap.put(g, score);
		}
		lineage.speciate(scoreMap);

		System.out.println("Placing best genomes into next generation..");
		
		// Breed the rest of the genomes
		System.out.println("Performing crossover..");
		//TODO: build a species list and crossover in parallel
		//TODO: limit per POM based on increase in fitness.
		//	(Epsilon-stagnation of resources/niche)
		while (nextGenGenomes.size() < populationSize) {
			// call getRandomPoMBiasedInnovationDensity
			PointOfMutation PoM = getRandomPOMBiasedFitness(lineage, random);
			// call getRandomGenomeBiasedAdjustedFitness
			// 	-change totalAdjustedFitness to relativeFitness
			Genome p1 = getRandomGenomeBiasedFitness(PoM, scoreMap, random);
			Genome p2 = getRandomGenomeBiasedFitness(PoM, scoreMap, random);

			Genome child;
			if (scoreMap.get(p1) >= scoreMap.get(p2)) {
				child = Genome.crossover(p1, p2, random);
			} else { // Is this due to innovation number?
				child = Genome.crossover(p2, p1, random);
			} // else they are equal so disjoint and excess genes must be randomized
				// respectively not between parents.
			if (random.nextFloat() < MUTATION_RATE) {
				child.mutation(random);
			}
			if (random.nextFloat() < ADD_CONNECTION_RATE) {
				// System.out.println("Adding connection mutation...");
				child.addConnectionMutation(random, connectionInnovation, genepool);
			}
			if (random.nextFloat() < ADD_NODE_RATE) {
				// System.out.println("Adding node mutation...");
				child.addNodeMutation(random, connectionInnovation, nodeInnovation, genepool);
			}
			nextGenGenomes.add(child);
			// TODO: need to "garbage collect" fragmented innovation numbers lost with
			// parents that are crossed over innovationCounter = stream.map(innovation
			// numbers).sort(acompb).get(0)
			// will fix max fragmentation but not gaps within innovation list. innovation
			// list still has historical representation is just not an efficient counting
			// method wrt crossover.

			// This may be a good point to add a global ConnectionGene
			// List and redo all instances of SCAN GENOMES in topology mutation. this will
			// lead naturally into A.S.
		}
		if(nextGenGenomes.size() < populationSize){
			PointOfMutation respawn = lineage.swapIn(random);
			while(nextGenGenomes.size() < populationSize){
				respawn.members.add(respawn.mascot); //TODO: mutate mascot weights
			}
		}
		
		lineage.updateInnovations(nextGenGenomes);

		genepool = nextGenGenomes;
		nextGenGenomes = new ArrayList<Genome>();
	}

	/**
	 * Select a random PointOfMutation from the Ancestors list biased towards
	 * POMs with higher score values.
	 * 
	 * @param random random seed.
	 * @return randomly selected species.
	 */
	private PointOfMutation getRandomPOMBiasedFitness(Ancestors lineage, Random random) {
		double completeWeight = 0.0; // sum of probabilities of selecting each species - selection is more probable
										// for species with higher fitness
		// filter out empty POMs as they arent considered here	
		List<PointOfMutation> filteredPOMs = lineage.POMs.keySet().stream()
							 .filter(p->!p.members.isEmpty())
							 .collect(Collectors.toList());
		completeWeight = filteredPOMs.stream()
					     .map(p->p.highScore)
					     .reduce(0f, (s1,s2)-> s1+s2);
		double r = Math.random() * completeWeight;
		double countWeight = 0.0;
		//POMs sorted by score to bias towards higher scores.
		for (PointOfMutation p : filteredPOMs.stream()
						     .sorted((p1,p2)->p1.highScore.compareTo(p2.highScore))
						     .collect(Collectors.toList())) {
			countWeight += p.highScore;
			if(countWeight >= r){
				return p;
			}
		}
		throw new RuntimeException("Couldn't find a PointOfMutation..." 
				+ ", and the total score is " + completeWeight);
	}
	/**
	 * Select a random Genome from a PointOfMutation biased towards fitness.
	 * 
	 * @param selectFrom species to select Genome from.
	 * @param random     random seed.
	 * @return selected genome.
	 */
	private Genome getRandomGenomeBiasedFitness(PointOfMutation selectFrom, Map<Genome, Float> scoreMap, Random random) {
		double completeWeight = 0.0; // sum of probabilities of selecting each genome - selection is more probable
										// for genomes with higher fitness
		for (Genome g : selectFrom.members) {
			completeWeight += scoreMap.get(g);
		}
		double r = Math.random() * completeWeight;
		double countWeight = 0.0;
		for (Genome g : selectFrom.members) {
			countWeight += scoreMap.get(g);
			if (countWeight >= r) {
				return g;
			}
		}
		throw new RuntimeException("Couldn't find a genome... PointOfMutation is " + selectFrom + 
				 "total adjusted fitness is " + completeWeight);
	}
	// epsilon greedy resource stagnation
	private int ScarcityofResources(int population) {
		Float rate;
		if(prevResource == null){
			prevResource = 1;
		}
		nextResource = (int) Math.ceil(scoreMap.values().stream()
							        .reduce(0f, (v1,v2)-> v1+v2));
		if(nextResource < 0){
			System.out.println("ERROR: genepool scoremap is less than 0..");
			System.exit(0);
		}

		rate = (float) nextResource/prevResource;
		//would rather use derivative to tune population reduction.
		//if(rate < 0){
		if(rate < 1){
			return (int) Math.ceil(population * rate); //reduce population by this amount
		}else{
			prevResource = nextResource;
			return population;
		}
	}

	protected abstract float evaluateGenome(Genome genome); // protected: must be called inside subclass and abstract:
								// implemented with @Override method
	public float getHighestScore(){
		return highestScore;
	}

	public Genome getFittestGenome(){
		Genome alphaGenome = lineage.POMs.keySet().stream().max((p1,p2)->p1.highScore.compareTo(p2.highScore)).get().mascot;
		return alphaGenome;
	}

}
