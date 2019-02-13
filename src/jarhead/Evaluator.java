package jarhead;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Evaluator class.
 */
public abstract class Evaluator {

	private FitnessGenomeComparator fitComp = new FitnessGenomeComparator();

	private Counter connectionInnovation;
	private Counter nodeInnovation;

	private Random random = new Random();

	/* Constants for tuning */
	private float C1 = 1.0f; // why is probability perturbing not included here?
	private float C2 = 1.0f;
	private float C3 = 0.4f;
	private float DT = 10.0f;
	private float MUTATION_RATE = 0.5f;
	private float ADD_CONNECTION_RATE = 0.7f;
//	private float ADD_CONNECTION_RATE = 0.05f;
	private float ADD_NODE_RATE = 0.01f;

	private int populationSize;

	private List<Genome> genomes;
	private List<Genome> nextGenGenomes;

	private List<Species> species;

	private Map<Genome, Species> mappedSpecies;
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
		this.connectionInnovation = connectionInnovation; // TODO: this should be its own object to prevent lost genomes
															// from exploding innovation (not critical)
		this.nodeInnovation = nodeInnovation;

		genomes = new ArrayList<Genome>(populationSize);
		for (int i = 0; i < populationSize; i++) {
			genomes.add(new Genome(startingGenome));
		} // serialized genome injection: i < populationSize-1; then add stored genome
			// after this loop.
		nextGenGenomes = new ArrayList<Genome>(populationSize);
		mappedSpecies = new HashMap<Genome, Species>();
		scoreMap = new HashMap<Genome, Float>();
		species = new ArrayList<Species>();
	}

	/**
	 * Runs one generation.
	 * 
	 * 1.Place genomes into species 2.Remove unused species 3.Evaluate genomes and
	 * assign score 4.put best genomes from each species into next generation
	 * 5.Breed the rest of the genomes
	 */
	public void evaluate() { // TODO: multi-thread these methods to speed up crossover/mutation.
		// Reset species for next generation
		for (Species s : species) {
			s.reset(random);
		}
		scoreMap.clear();
		mappedSpecies.clear();
		nextGenGenomes.clear();
		highestScore = Float.MIN_VALUE;
		fittestGenome = null;

		// Place genomes into species
		System.out.println("Placing genomes into species..");
		for (Genome g : genomes) {
			boolean foundSpecies = false;
			for (Species s : species) {
				if (Genome.compatibilityDistance(g, s.mascot, C1, C2, C3) < DT) { // compatibility distance is less than
																					// DT, so genome belongs to this
																					// species
					s.members.add(g);
					mappedSpecies.put(g, s);
					foundSpecies = true;
					break;
				}
			}
			if (!foundSpecies) { // if there is no appropriate species for genome, make a new one
				Species newSpecies = new Species(g);
				species.add(newSpecies);
				mappedSpecies.put(g, newSpecies);
			}
		}
		System.out.println("Clearing unused species..");
		// Remove unused species
		Iterator<Species> iter = species.iterator();
		while (iter.hasNext()) {
			Species s = iter.next();
			if (s.members.isEmpty()) {
				iter.remove();
			}
		}
		System.out.println("Evaluating genomes and assigning score");
		// Evaluate genomes and assign score
		for (Genome g : genomes) {
			Species s = mappedSpecies.get(g); // Get species of the genome

			float score = evaluateGenome(g);
			float adjustedScore = score / mappedSpecies.get(g).members.size(); // explicit fitness sharing?

			s.addAdjustedFitness(adjustedScore);
			s.fitnessPop.add(new FitnessGenome(g, adjustedScore));
			scoreMap.put(g, adjustedScore);
			if (score > highestScore) {
				highestScore = score;
				fittestGenome = g;
			}
		}

		System.out.println("Placing best genomes into next generation..");
		// put best genomes from each species directly into next generation
		// is this (fittestInSpecies) explicit fitness sharing from k.stanely?
		for (Species s : species) {
			Collections.sort(s.fitnessPop, fitComp);
			Collections.reverse(s.fitnessPop);
			FitnessGenome fittestInSpecies = s.fitnessPop.get(0);
			nextGenGenomes.add(fittestInSpecies.genome);
		}

		// Breed the rest of the genomes
		while (nextGenGenomes.size() < populationSize) { // replace removed genomes by randomly breeding
			Species s = getRandomSpeciesBiasedAjdustedFitness(random);

			Genome p1 = getRandomGenomeBiasedAdjustedFitness(s, random);
			Genome p2 = getRandomGenomeBiasedAdjustedFitness(s, random);

			Genome child;
			if (scoreMap.get(p1) >= scoreMap.get(p2)) {
				child = Genome.crossover(p1, p2, random); // TODO: need to handle when child is not possible.
			} else { // Is this due to innovation number?
				child = Genome.crossover(p2, p1, random);
			} // else they are equal so disjoint and excess genes must be randomized
				// respectively not between parents.
			if (random.nextFloat() < MUTATION_RATE) {
				child.mutation(random);
			}
			if (random.nextFloat() < ADD_CONNECTION_RATE) {
				// System.out.println("Adding connection mutation...");
				child.addConnectionMutation(random, connectionInnovation, genomes);
			}
			if (random.nextFloat() < ADD_NODE_RATE) {
				// System.out.println("Adding node mutation...");
				child.addNodeMutation(random, connectionInnovation, nodeInnovation, genomes);
			}
			nextGenGenomes.add(child);
			// TODO: need to "garbage collect" fragmented innovation numbers lost with
			// parents that are crossed over innovatoinCounter = stream.map(innovation
			// numbers).sort(acompb).get(0)
			// will fix max fragmentation but not gaps within innovation list. innovation
			// list still has historical representation is just not an efficient counting
			// method wrt crossover
		}

		genomes = nextGenGenomes;
		nextGenGenomes = new ArrayList<Genome>();
	}

	/**
	 * Selects a random species from the species list, where species with a higher
	 * total adjusted fitness have a higher chance of being selected
	 * 
	 * @param random random seed.
	 * @return randomly selected species.
	 */
	private Species getRandomSpeciesBiasedAjdustedFitness(Random random) {
		double completeWeight = 0.0; // sum of probabilities of selecting each species - selection is more probable
										// for species with higher fitness
		for (Species s : species) {
			completeWeight += s.totalAdjustedFitness;
		}
		double r = Math.random() * completeWeight;
		double countWeight = 0.0;
		for (Species s : species) {
			countWeight += s.totalAdjustedFitness;
			if (countWeight >= r) {
				return s;
			}
		}
		throw new RuntimeException("Couldn't find a species... Number is species in total is " + species.size()
				+ ", and the total adjusted fitness is " + completeWeight);
	}

	/**
	 * Selects a random genome from the species chosen, where genomes with a higher
	 * adjusted fitness have a higher chance of being selected
	 * 
	 * @param selectFrom species to select Genome from.
	 * @param random     random seed.
	 * @return selected genome.
	 */
	private Genome getRandomGenomeBiasedAdjustedFitness(Species selectFrom, Random random) {
		double completeWeight = 0.0; // sum of probabilities of selecting each genome - selection is more probable
										// for
										// genomes with higher fitness
		for (FitnessGenome fg : selectFrom.fitnessPop) {
			completeWeight += fg.fitness;
		}
		double r = Math.random() * completeWeight;
		double countWeight = 0.0;
		for (FitnessGenome fg : selectFrom.fitnessPop) {
			countWeight += fg.fitness;
			if (countWeight >= r) {
				return fg.genome;
			}
		}
		throw new RuntimeException("Couldn't find a genome... Number is genomes in selected species is "
				+ selectFrom.fitnessPop.size() + ", and the total adjusted fitness is " + completeWeight);
	}

	/**
	 * @return size of species.
	 */
	public int getSpeciesAmount() {
		return species.size();
	}

	/**
	 * @return highest score.
	 */
	public float getHighestFitness() {
		return highestScore;
	}

	/**
	 * @return genome with highest fitness.
	 */
	public Genome getFittestGenome() {
		return fittestGenome;
	}

	/**
	 * @return list of genomes.
	 */
	public List<Genome> getGenomes() {
		return genomes;
	}

	/**
	 * Uses @Override method to instantiate a fitness function.
	 * 
	 * @param genome genome to be evaluated.
	 */
	protected abstract float evaluateGenome(Genome genome); // protected: must be called inside subclass and abstract:
															// implemented with @Override method

	/**
	 * assigns a fitness to a given Genome.
	 */
	public class FitnessGenome {

		float fitness;
		Genome genome;

		/**
		 * @param genome  genome to be assigned.
		 * @param fitness fitness to assign.
		 */
		public FitnessGenome(Genome genome, float fitness) {
			this.genome = genome;
			this.fitness = fitness;
		}
	}

	/**
	 * Species Constructor class.
	 */
	public class Species {

		public Genome mascot;
		public List<Genome> members;
		public List<FitnessGenome> fitnessPop;
		public float totalAdjustedFitness = 0f;

		public Species(Genome mascot) {
			this.mascot = mascot;
			this.members = new LinkedList<Genome>();
			this.members.add(mascot);
			this.fitnessPop = new ArrayList<FitnessGenome>();
		}

		public void addAdjustedFitness(float adjustedFitness) {
			this.totalAdjustedFitness += adjustedFitness;
		}

		/*
		 * Selects new random mascot + clear members + set totaladjustedfitness to 0f
		 */
		/**
		 * Selects new random mascot + clear members + set totaladjustedfitness to 0f
		 * 
		 * @param r random seed.
		 */
		public void reset(Random r) {
			int newMascotIndex = r.nextInt(members.size());
			this.mascot = members.get(newMascotIndex);
			members.clear();
			fitnessPop.clear();
			totalAdjustedFitness = 0f;
		}
	}

	/**
	 * returns comparison of genomes implementing javaUtil.Comparator method.
	 */
	public class FitnessGenomeComparator implements Comparator<FitnessGenome> {

		@Override
		public int compare(FitnessGenome one, FitnessGenome two) {
			if (one.fitness > two.fitness) {
				return 1;
			} else if (one.fitness < two.fitness) {
				return -1;
			}
			return 0;
		}

	}

	/**
	 * returns species mapped to genomes .
	 * 
	 * @return mappedSpecies.
	 */
	public Map<Genome, Species> getSpecies() {
		return mappedSpecies;
	}

}
