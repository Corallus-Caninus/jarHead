package jarhead;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;

public class Chromosome {

	/**
	 * Returns compatibility distance (direct scrape from paper).
	 * 
	 * @param genome1
	 * @param genome2
	 * @param c1      tuneable weight for excess genes.
	 * @param c2      tuneable weight for disjoint genes.
	 * @param c3      tuneable weight for average weight difference.
	 * @return compatibility distance metric.
	 */
	public static float compatibilityDistance(Genome genome1, Genome genome2, float c1, float c2, float c3) {
		// TODO: make distance metric functions more efficient, replace with java 8 methods
		int excessGenes = countExcessGenes(genome1, genome2);
		int disjointGenes = countDisjointGenes(genome1, genome2);
		float avgWeightDiff = averageWeightDiff(genome1, genome2);
		return excessGenes * c1 + disjointGenes * c2 + avgWeightDiff * c3;
	}

	/**
	 * Returns number of matching genes (node and connection/innovation).
	 * 
	 * @param genome1
	 * @param genome2
	 * @return
	 */
	public static int countMatchingGenes(Genome genome1, Genome genome2) {
		int matchingGenes = 0;
		// counts congruent node genes
		List<Integer> nodeKeys1 = asSortedList(genome1.getNodeGenes().keySet());
		List<Integer> nodeKeys2 = asSortedList(genome2.getNodeGenes().keySet());
	
		int highestInnovation1 = nodeKeys1.get(nodeKeys1.size() - 1);
		int highestInnovation2 = nodeKeys2.get(nodeKeys2.size() - 1);
		int indices = Math.max(highestInnovation1, highestInnovation2);
	
		for (int i = 0; i <= indices; i++) { // loop through genes -> i is innovation numbers
			NodeGene node1 = genome1.getNodeGenes().get(i);
			NodeGene node2 = genome2.getNodeGenes().get(i);
			if (node1 != null && node2 != null) {
				// both genomes have the gene w/ this innovation number
				matchingGenes++;
			}
		}
	
		// count congruent connection genes
		List<Integer> conKeys1 = asSortedList(genome1.getConnectionGenes().keySet());
		List<Integer> conKeys2 = asSortedList(genome2.getConnectionGenes().keySet());
	
		highestInnovation1 = conKeys1.get(conKeys1.size() - 1);
		highestInnovation2 = conKeys2.get(conKeys2.size() - 1);
	
		indices = Math.max(highestInnovation1, highestInnovation2);
		for (int i = 0; i <= indices; i++) { // loop through genes -> i is innovation numbers
			ConnectionGene connection1 = genome1.getConnectionGenes().get(i);
			ConnectionGene connection2 = genome2.getConnectionGenes().get(i);
			if (connection1 != null && connection2 != null) {
				// both genomes have the gene w/ this innovation number
				matchingGenes++;
			}
		}
	
		return matchingGenes;
	}

	/**
	 * Used for compatibility distance.
	 * 
	 * @param genome1
	 * @param genome2
	 * @return disjoint gene count
	 */
	public static int countDisjointGenes(Genome genome1, Genome genome2) {
		int disjointGenes = 0;
	
		List<Integer> nodeKeys1 = asSortedList(genome1.getNodeGenes().keySet());
		List<Integer> nodeKeys2 = asSortedList(genome2.getNodeGenes().keySet());
	
		int highestInnovation1 = nodeKeys1.get(nodeKeys1.size() - 1);
		int highestInnovation2 = nodeKeys2.get(nodeKeys2.size() - 1);
		int indices = Math.max(highestInnovation1, highestInnovation2);
	
		for (int i = 0; i <= indices; i++) {
			NodeGene node1 = genome1.getNodeGenes().get(i);
			NodeGene node2 = genome2.getNodeGenes().get(i);
			if (node1 == null && highestInnovation1 > i && node2 != null) {
				// genome 1 lacks gene, genome 2 has gene, genome 1 has more genes w/ higher
				// innovation numbers
				disjointGenes++;
			} else if (node2 == null && highestInnovation2 > i && node1 != null) {
				disjointGenes++;
			}
		}
	
		List<Integer> conKeys1 = asSortedList(genome1.getConnectionGenes().keySet());
		List<Integer> conKeys2 = asSortedList(genome2.getConnectionGenes().keySet());
	
		highestInnovation1 = conKeys1.get(conKeys1.size() - 1);
		highestInnovation2 = conKeys2.get(conKeys2.size() - 1);
	
		indices = Math.max(highestInnovation1, highestInnovation2);
		for (int i = 0; i <= indices; i++) {
			ConnectionGene connection1 = genome1.getConnectionGenes().get(i);
			ConnectionGene connection2 = genome2.getConnectionGenes().get(i);
			if (connection1 == null && highestInnovation1 > i && connection2 != null) {
				disjointGenes++;
			} else if (connection2 == null && highestInnovation2 > i && connection1 != null) {
				disjointGenes++;
			}
		}
	
		return disjointGenes;
	}

	/**
	 * Used for compatibility distance.
	 * 
	 * @param genome1
	 * @param genome2
	 * @return excess gene count
	 */
	public static int countExcessGenes(Genome genome1, Genome genome2) {
		int excessGenes = 0;
	
		List<Integer> nodeKeys1 = asSortedList(genome1.getNodeGenes().keySet());
		List<Integer> nodeKeys2 = asSortedList(genome2.getNodeGenes().keySet());
	
		int highestInnovation1 = nodeKeys1.get(nodeKeys1.size() - 1);
		int highestInnovation2 = nodeKeys2.get(nodeKeys2.size() - 1);
		int indices = Math.max(highestInnovation1, highestInnovation2);
	
		for (int i = 0; i <= indices; i++) {
			NodeGene node1 = genome1.getNodeGenes().get(i);
			NodeGene node2 = genome2.getNodeGenes().get(i);
			if (node1 == null && highestInnovation1 < i && node2 != null) {
				excessGenes++;
			} else if (node2 == null && highestInnovation2 < i && node1 != null) {
				excessGenes++;
			}
		}
	
		List<Integer> conKeys1 = asSortedList(genome1.getConnectionGenes().keySet());
		List<Integer> conKeys2 = asSortedList(genome2.getConnectionGenes().keySet());
	
		highestInnovation1 = conKeys1.get(conKeys1.size() - 1);
		highestInnovation2 = conKeys2.get(conKeys2.size() - 1);
	
		indices = Math.max(highestInnovation1, highestInnovation2);
		for (int i = 0; i <= indices; i++) {
			ConnectionGene connection1 = genome1.getConnectionGenes().get(i);
			ConnectionGene connection2 = genome2.getConnectionGenes().get(i);
			if (connection1 == null && highestInnovation1 < i && connection2 != null) {
				excessGenes++;
			} else if (connection2 == null && highestInnovation2 < i && connection1 != null) {
				excessGenes++;
			}
		}
	
		return excessGenes;
	}

	/**
	 * Used for compatibility distance.
	 * 
	 * @param genome1
	 * @param genome2
	 * @return weight difference to matching genes ratio
	 */
	public static float averageWeightDiff(Genome genome1, Genome genome2) {
		int matchingGenes = 0;
		float weightDifference = 0;
	
		List<Integer> conKeys1 = asSortedList(genome1.getConnectionGenes().keySet());
		List<Integer> conKeys2 = asSortedList(genome2.getConnectionGenes().keySet());
	
		int highestInnovation1 = conKeys1.get(conKeys1.size() - 1);
		int highestInnovation2 = conKeys2.get(conKeys2.size() - 1);
	
		int indices = Math.max(highestInnovation1, highestInnovation2);
		for (int i = 0; i <= indices; i++) { // loop through genes -> i is innovation numbers
			ConnectionGene connection1 = genome1.getConnectionGenes().get(i);
			ConnectionGene connection2 = genome2.getConnectionGenes().get(i);
			if (connection1 != null && connection2 != null) {
				// both genomes have the gene w/ this innovation number
				matchingGenes++;
				weightDifference += Math.abs(connection1.getWeight() - connection2.getWeight());
			}
		}
	
		return weightDifference / matchingGenes;
	}

	/**
	 * Utility function for distance equation metrics. clears given list and adds
	 * all of collection c then sorts the list in ascending order.
	 * 
	 * @param c collection of integersQc
	 * @return assorted list
	 */
	private static List<Integer> asSortedList(Collection<Integer> c) {
		List<Integer> list = new ArrayList<Integer>();
		list.clear();
		list.addAll(c);
		java.util.Collections.sort(list);
		return list;
	}
	
	// ANCESTRAL SPECIATION DEVELOPMENTS
	// -branch and check into github
	public class AncestralSpecies{
		//public List<PointOfMutation> POMs;//used for speciation			
		public Map<PointOfMutation, Integer> POMs;
		public List<ConnectionGene> newConnectionGenes;//global innovation list

		//constructor(allocator)
		public AncestralSpecies(Genome initialGenome){
			POMs = new HashMap<PointOfMutation, Integer>();
			POMs.put(new PointOfMutation(0f, initialGenome),  0);
			newConnectionGenes = new ArrayList<ConnectionGene>(initialGenome.getConnectionGenes().values());
			//how exactly does this work...
		}
		//CALLED AT COMPATABILITYDISTANCE AND AFTER SPECIATE
		public PointOfMutation assignPOMs(Genome assignGenome){
		//migration and placement of genomes.
		//PointOfMutation must be associated with
		//a map in Evaluator (Species) for now wrap species
		//onto PointOfMutation
			PointOfMutation match = POMs.entrySet().stream().sorted((p1,p2)->p2.getValue().compareTo(p1.getValue()))
					      .filter(p->assignGenome.getConnectionGenes().values().containsAll(p.getKey().innovationGenes))
					      .findFirst().get().getKey();
			//must return a value as initial Genome is first POM (verify in Constructor)
			match.members.add(assignGenome);
			return match;	
		} 
		//
		//CALLED AFTER CROSSOVER
		//public void updateInnovations(List<Genome> nextGenGenomes){
		//	//add all new ConnectionsGenes
		//	newConnectionGenes.addAll(
		//	nextGenGenomes.stream().map(g->g.getConnectionGenes)
		//			.filter(c->!newConnectionGenes.contains(c.getInnovation()))
		//			.distinct().Collect(Collectors.toList())
		//				);
		//}
		//CALLED AFTER CROSSOVER
		//public PointOfMutation respawn(){
		//	PointOfMutation swapIn = POMs.Stream().sort((p1,p2)-> p1.highScore - p2.highScore)
		//		.collect(Collectors.toList()).get(randomBiasedInitial);
		//	return swapIn;
		//}
		//
		//CALLED AFTER EVALUATION
		public void speciate(Map<Genome, Float> scoreMap){
			for(PointOfMutation checkPOM : POMs.keySet()){
				Optional<Genome> branch = 
					checkPOM.members.stream().filter(g->scoreMap.get(g) > checkPOM.highScore)// check best score
					.filter(g->!Collections.disjoint(newConnectionGenes, g.getConnectionGenes().values()))// check novel innovation
						.max((g1,g2)->scoreMap.get(g1).compareTo(scoreMap.get(g2)));//choose fittest of all candidates
				if(branch.isPresent()){
					PointOfMutation addition = new PointOfMutation(scoreMap.get(branch.get()), branch.get());
					POMs.put(addition, POMs.get(checkPOM)+1);
					checkPOM.branchCount++;

					newConnectionGenes.removeAll(branch.get().getConnectionGenes().values());
				}
			}
		}
	}
	public class PointOfMutation{
		public List<ConnectionGene> innovationGenes; //note public only to Chromosome class?
		public float highScore;
		public List<Genome> members;
		public int branchCount; //number of branches created by a POM 
		
		//Constructor
		public PointOfMutation(float initialScore, Genome newGenome){	
			innovationGenes = new ArrayList<ConnectionGene>(newGenome.getConnectionGenes().values());
			highScore = initialScore;
			members = new ArrayList<Genome>();
			members.add(newGenome);
		}
	}	
	// GOAL: KISS and small/agile iterations/
	//
	// *Can Chromosome.compatabilityDistance 
	// be replaced with an ancestralSpeciation 
	// method to preserve all other methods used
	// in Evaluator? trace Ancestral Speciation
	// algorithm and look for simplest implementation
	// regardless of functionality (genetic alg ANN
	// without speciation is valid so first I shall
	// do no harm).
	//
	// 	1. if Chromosome becomes too large create Chromosome package
	// 	and put all classes within. consider refactoring into 
	// 	other biological classifications to be more pedantic
	// 		-this is looking necessary
	//
	//	2. only change to Evaluator thus far is 
	//		*epsilon greedy population limiting in crossover.
	//		*CALL:   respawn swapIn species to fill population.
	//		*CALL:	 update newInnovation genes
	//		*CALL: 	 change selectBiasedSpecies to selectBiasedPOM and selectGenome for POM
	//		*REMOVE: compatabilityDistance.
	//		*CALL:   speciation after evaluation.
	//	
	//	3. POMs are species. how does this effect 2?
	//		-try to leave selectBiasedSpecies as is. stay agile.
	//
	// 
	// * adding a global list of novel genes (ConnectionGenes as used in SCAN_
	//   GENOMES algorithms to Evaluator is a simpler and more appropriate (LoD) implementation.
	//   push this ^ to master
	//   	-for now only newConnectionGenes as this is a upstream refactor
	//
}
