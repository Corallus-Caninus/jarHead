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
	
	//TODO: cleanup these variables!
	
	private Counter connectionInnovation;
	private Counter nodeInnovation;

	private Random random = new Random();

	/* Constants for tuning */
	//POM specific constants:
	
	//TODO: still not happy with 
	//	these hyperparameters and discretization
	
	private float C1 = 1.0f; 
	private float C2 = 1.0f;
	private float C3 = 0.4f;
	private float DIST = 2; // Discretization hyperparameter for progressive novelty mapping
	private final int STAGNATION = 10000; //momentum given pressurized genepool, volume of water to be flowed

	//Genome specific constants:
	private final float MUTATION_PERTURBING_RATE = 0.9f;
	private final float MUTATION_RATE = 0.05f;
	private final float ADD_CONNECTION_RATE = 0.01f;
	private final float ADD_NODE_RATE = 0.01f;

	
	private int populationSize;
	private int fertility;

	private List<Genome> genepool;
	private List<Genome> nextGenGenomes;
	private Map<Integer, ConnectionGene> globalConnections; // TODO: make concurrent

	private Ancestors lineage;
	private Integer prevResource, nextResource = null;
	private Integer scoreRate = 0;

	private PointOfMutation PoM, respawn;
	private Map<Genome, Float> scoreMap; //TODO: why are these mismatched???
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
		lineage = new Ancestors(genepool, STAGNATION); // need to put until full.
		PoM = lineage.POMs.get(0); //only PoM is init POM
	//	respawn = PoM;
		
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
	//TODO: no more syntax errors but state machine broken
	//	trace transition edges
	public void evaluate() {
		// Reset species for next generation

		scoreMap.clear();
		nextGenGenomes.clear();
		highestScore = Float.MIN_VALUE;// TODO: DEPRACATED
		//used in calculating getRandomGenome, should pass in HighScore to prevent overcalculation 
		fittestGenome = null; //TODO: DEPRECATED 

		respawn = PoM;

		System.out.println("(eval) evaluating scoreMap..");
		scoreMap = evaluate(genepool, scoreMap); //TODO: scoremap is dissasociated from genepool

		System.out.println("(RoM) updating fitness landscape graph..");
		PoM = lineage.update(globalConnections, scoreMap, PoM, DIST, C1, C2, C3, STAGNATION); //cycle until passthrough
		if(!PoM.equals(respawn)) //lolwut
			genepool = PoM.snapshot;

		//	swapin -> evaluate -> merge - speciate
		//-------------BEGIN PROTOTYPE WORKFLOW-----------------------
		//swap:
		System.out.println("(RoM) checking dynamics..");
		while(!respawn.equals(PoM)){ //TODO: placeholder for initial loop
			//PoM = respawn; //iterate..
			respawn = PoM;

			System.out.println("(swap) evaluating scoreMap..");

			scoreMap = evaluate(genepool, scoreMap); //TODO: scoremap is dissasociated from genepool

			System.out.println("(RoM) updating fitness landscape graph..");
			PoM = lineage.update(globalConnections, scoreMap, PoM, DIST, C1, C2, C3, STAGNATION); //cycle until passthrough
			if(!PoM.equals(respawn))//lolwut
				genepool = PoM.snapshot;
		}
		System.out.println("(eval) swapping PoM ");
		PoM = lineage.swap(populationSize, PoM, STAGNATION);
		System.out.println("(eval) PointOfMutation selected: " + PoM);
		//-------------GENEPOOL IS NOW READY FOR CROSSOVER--------------
		
		// Breed the rest of the genomes
		System.out.println("Performing crossover..");
		// TODO: build a species list and crossover in parallel
		while (nextGenGenomes.size() < populationSize) {

			Genome p1 = getRandomGenomeBiasedFitness();
			Genome p2 = getRandomGenomeBiasedFitness();
				
			PoM.lifetime--;
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

				//PoM.members.add(child); //TODO: dont use members, nextGenGenome is chopped into snapshots instead
				nextGenGenomes.add(child);
			}
			//}
		}

		//lineage.updateInnovations(nextGenGenomes);//TODO remove automagically included in Ancestors.update

		genepool = nextGenGenomes; //possibly disconnected from mapping state machine here
		nextGenGenomes = new ArrayList<Genome>();
	}

	/**
	 *pass in scoremap, eventually this may be extracted
	 *but currently is implemented in generation loop.
	 */
	private Map<Genome, Float> evaluate(List<Genome> genepool, Map<Genome, Float> scoreMap){
		//Float highScore;
		for(Genome g : genepool){
			float score = evaluateGenome(g);
			//if(highestScore < score)
			//	highScore = score;
			scoreMap.put(g, score);
		}
		return(scoreMap);
	}

	/**
	 * @Depracated
	 * 	no longer using multi swap at evaluator level. this will be reimplemented
	 * 	in distributed algorithm (apache-spark or messaging)
	 * Select a random PointOfMutation from the Ancestors list biased towards POMs
	 * with higher score values. (Fitness of a species/epsilon exploitation)
	 * 
	 * @param random random seed.
	 * @return randomly selected species.
	 */
	// TODO: why is random passed in if this belongs to the same class and not
	// static?
	// was highScore
	/*private PointOfMutation getRandomPOMBiasedResources(Ancestors lineage, Random random) {
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
			countWeight += p.highScore /* * lineage.POMs.get(p) ;
			if (countWeight >= r) {
				return p;
			}
		}
		return null; // and swapin
//		throw new RuntimeException("Couldn't find a PointOfMutation...");
	}*/

	/**
	 * Select a random Genome from a PointOfMutation biased towards fitness.
	 * 
	 * @param selectFrom species to select Genome from.
	 * @param random     random seed.
	 * @return selected genome.
	 */
	//TODO: consider other evaluation methods for sample efficiency (like Chandler was talking about)
	//	rename this to fitnesse proportionate selection
	
	private Genome getRandomGenomeBiasedFitness(){
		// sum of probabilities of selecting each genome - selection is more probable
		// for genomes with higher fitness
		double completeWeight = 0.0; 

		for (Genome g : genepool) {
			completeWeight += scoreMap.get(g);
		}
		double r = Math.random() * completeWeight;
		double countWeight = 0.0;
		
		for (Genome g : genepool) {
			countWeight += scoreMap.get(g);
			if (countWeight >= r) {
				return g;
			}
		}
		System.out.println("Discarding selection scoreSum is: " + completeWeight);
		return null;
	}


	protected abstract float evaluateGenome(Genome genome); // protected: must be called inside subclass and abstract:
	// implemented with @Override method

	public float getHighestScore() {
		/*return lineage.POMs.keySet().parallelStream().max((p1, p2) -> p1.highScore.compareTo(p2.highScore))
				.get().highScore;*/
		return scoreMap.values().stream().max(Float::compare).get();
	}

	public Genome getFittestGenome() {
	//	return lineage.POMs.keySet().parallelStream().max((p1, p2) -> p1.highScore.compareTo(p2.highScore))
	//			.get().mascot;
		return scoreMap.entrySet().stream().max((e1,e2)-> e1.getValue().compareTo(e2.getValue())).get().getKey();
	}

	/*public PointOfMutation getFittestPOM() {
		return lineage.POMs.keySet().parallelStream().max((p1, p2) -> p1.highScore.compareTo(p2.highScore)).get();

	}*/
}
