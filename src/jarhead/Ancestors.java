package jarhead;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.*;

// ANCESTRAL SPECIATION DEVELOPMENTS
// 	-need to consider weight optimization
// TODO: set private variables/DataStructures accordingly. Consider gettters and setters
public class Ancestors{
	//NOTE: ConnectionGene objects are not comparable. use keySet or innovation number
	public Map<PointOfMutation, Integer> POMs;
	public HashMap<Integer, ConnectionGene> novelInnovationMap;

	//constructor (called in Evaluator constructor)
	public Ancestors(Genome initialGenome){ //initial genome==Species.mascot;
		novelInnovationMap = new HashMap<Integer, ConnectionGene>();
		POMs = new HashMap<PointOfMutation, Integer>();//Integer is for AncestryTree lineage
		
		novelInnovationMap.putAll(initialGenome.getConnectionGenes());
		POMs.put(new PointOfMutation(0f, initialGenome), 0);//initial PoM TODO: initial score hyperparameter
	}

	public void migrate(List<Genome> genepool){
	//migration and placement of genomes.
	//currently uses immediate migration (to most complex match)
	//TODO?: implement iterative migration (only migrate to lineage thats +1)
		swapOut();//clear members
		for(Genome assignGenome : genepool){
			PointOfMutation match = POMs.entrySet().stream()
					.sorted((e1,e2)->e2.getValue().compareTo(e1.getValue()))//TODO: check this is sorted right. this needs debug/testing
				      	.filter(e->assignGenome.getConnectionGenes().keySet()
					      	     .containsAll(e.getKey().innovationGenes.keySet()))
				        .findFirst().get().getKey();
			match.members.add(assignGenome); //defaults to initial POM(initGenome)
		}
		System.out.println("MIGRATION PRINTOUT: ");
		for(PointOfMutation printing : POMs.keySet()){
			System.out.println("\tPoM " + printing + " With " + printing.members.size() + " Genomes"
						+ " with score " + printing.highScore);
		}
		System.out.println("MIGRATION COMPLETE...");

	}
	public void speciate(Map<Genome, Float> scoreMap){
		boolean branched = false;

		//TODO: need to move scoreMap to concurrentMap getting concurrent modification from eval
		// (bane of laziness)
		for(PointOfMutation checkPOM : POMs.keySet()){
			Optional<Genome> match = checkPOM.members.stream().filter(g->scoreMap.get(g) > checkPOM.highScore)
						   .filter(g->checkPOM.innovationGenes.keySet().containsAll(g.getConnectionGenes().keySet()))
						   .max((g1,g2)->scoreMap.get(g1).compareTo(scoreMap.get(g2)));
			if(match.isPresent()){
				System.out.println("HIGHSCORE");

				PointOfMutation addition = new PointOfMutation(scoreMap.get(match.get()), match.get());
				POMs.put(addition, POMs.get(checkPOM)+1); //broken here?
				//remove all innovations found in matching genome (HashMap method)
				System.out.println("Establishing a new niche..");
				
			novelInnovationMap.keySet().removeAll(match.get().getConnectionGenes().keySet()
							 .stream().filter(c->novelInnovationMap.containsKey(c))
							 .collect(Collectors.toList()));
				
			        branched = true;
			}
		}
		//consider new POMs for migration
		if(branched){
			migrate(scoreMap.keySet().stream().collect(Collectors.toList()));
		}
	}
	public void updateInnovations(List<Genome> nextGenGenomes){
		//add all new ConnectionsGenes
		System.out.println("innovationList count: " + novelInnovationMap.entrySet().stream().count());

		List<ConnectionGene> novelInnovations =	nextGenGenomes.stream().flatMap(g->g.getConnectionGenes().values().stream())
				      			//not already in novelInnovationMap
				      			.filter(c->!novelInnovationMap.keySet().contains(c.getInnovation()))
				      			//innovation is not already consumed in a niche
				      			.filter(c->!POMs.keySet().stream()
					      			.map(p->p.innovationGenes)
								.collect(Collectors.toList())
								.contains(c.getInnovation()))
				      			.collect(Collectors.toList());
		for(ConnectionGene gene : novelInnovations){
			if(!novelInnovationMap.containsKey(gene.getInnovation()))
				novelInnovationMap.put(gene.getInnovation(), gene);
		}
	}
	public PointOfMutation swapIn(Random r){
		//selection biased towards innovationDensity
		List<Integer> swappedOut = POMs.entrySet().stream().filter(e->e.getKey().members.isEmpty())
						   .map(e->e.getValue())
						   .collect(Collectors.toList());

		Integer selection = swappedOut.get(r.nextInt(swappedOut.size()));

		System.out.println("swapIn: selecting from: " + selection);
		//select random POM from innovationDensity	
		PointOfMutation swap = POMs.entrySet().stream().filter(e->e.getValue().equals(selection))
					   .collect(Collectors.toList())
					   .get(r.nextInt((int) POMs.values().stream()
					   .filter(v->v.equals(selection)).count()))
					   .getKey();
		System.out.println("swapIn: Selected POM: " + swap);
		return swap;//add mutated mascot till nextGenGenome.size()==population.size()
	}
	private void swapOut(){
		//TODO: only call when members.isEmpty() to alleviate migrate 
		//as of now this is just members.clear()
		for(PointOfMutation swap : POMs.keySet()){
			swap.members.clear();
		}
	}
}
