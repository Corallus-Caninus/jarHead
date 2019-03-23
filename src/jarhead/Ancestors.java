package jarhead;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.*;

// ANCESTRAL SPECIATION DEVELOPMENTS
// 	-need to consider weight optimization
// TODO: set private variables/DataStructures accordingly. Consider gettters and setters
public class Ancestors{
	//NOTE: ConnectionGene objects are not comparable. use keySet or innovation number
	public ConcurrentMap<PointOfMutation, Integer> POMs;
	public HashMap<Integer, ConnectionGene> novelInnovationMap;

	//constructor (called in Evaluator constructor)
	public Ancestors(Genome initialGenome){ //initial genome==Species.mascot;
		novelInnovationMap = new HashMap<Integer, ConnectionGene>();
		POMs = new ConcurrentHashMap<PointOfMutation, Integer>();//Integer is for AncestryTree lineage
		
		novelInnovationMap.putAll(initialGenome.getConnectionGenes());
		POMs.put(new PointOfMutation(0f, initialGenome), 1);//initial PoM TODO: initial score hyperparameter
	}

	public void migrate(List<Genome> genepool){
	//migration and placement of genomes.
	//TODO?: implement iterative migration (only migrate to lineage thats +1)
		swapOut();//clear members
		for(Genome assignGenome : genepool){
			PointOfMutation match = POMs.entrySet().parallelStream()
					.sorted((e1,e2)->e2.getValue().compareTo(e1.getValue()))
				      	.filter(e->assignGenome.getConnectionGenes().keySet()
					      	     .containsAll(e.getKey().innovationGenes.keySet()))
				        .findFirst().get().getKey();
			match.members.add(assignGenome); //defaults to initial POM(initGenome)
		}
		System.out.println("MIGRATION PRINTOUT: ");
		for(PointOfMutation printing : POMs.keySet().parallelStream()
							    .sorted((p1,p2)->p1.highScore.compareTo(p2.highScore))
							    .collect(Collectors.toList())){
			System.out.println("\tPoM " + printing + " With " + printing.members.size() + " Genomes"
						+ " with score " + printing.highScore);
		}
		System.out.println("MIGRATION COMPLETE...");

	}
	public void speciate(Map<Genome, Float> scoreMap){
		boolean branched = false;

		for(PointOfMutation checkPOM : POMs.keySet()){
			Optional<Genome> match = checkPOM.members.parallelStream().filter(g->scoreMap.get(g) > checkPOM.highScore)
						   //filter out genomes that dont have novel innovations. careful as migrate MUST happen first
						   .filter(g->!checkPOM.innovationGenes.keySet().containsAll(g.getConnectionGenes().keySet()))//wut
						   .max((g1,g2)->scoreMap.get(g1).compareTo(scoreMap.get(g2)));
			if(match.isPresent()){
				System.out.println("HIGHSCORE");
				
				PointOfMutation addition = new PointOfMutation(scoreMap.get(match.get()), match.get());
				System.out.println("Establishing a new niche..");
				POMs.put(addition, POMs.get(checkPOM)+1);
				consumeInnovations();
				//remove all innovations found in matching genome (HashMap method)
				
			        branched = true;
			}
		}
		//consider new POMs for migration
		if(branched){
			migrate(scoreMap.keySet().parallelStream().collect(Collectors.toList()));
		}
	}
	//used to declare a niche
	public void consumeInnovations(){
		List<Integer> potentialNiches = POMs.keySet().parallelStream()
							  .flatMap(p->p.members.parallelStream())
							  .flatMap(g->g.getConnectionGenes().keySet().parallelStream())
							  .filter(c->novelInnovationMap.containsKey(c))
							  .collect(Collectors.toList());
		novelInnovationMap.keySet().removeAll(potentialNiches);
		
	}
	//novel innovation produced by genepool. TODO: use this for global SCAN_GENOMES check
	public void updateInnovations(List<Genome> nextGenGenomes){
		//add all new ConnectionsGenes
		System.out.println("innovationList count: " + novelInnovationMap.entrySet().parallelStream().count());

		List<ConnectionGene> novelInnovations =	nextGenGenomes.parallelStream().flatMap(g->g.getConnectionGenes().values().stream())
				      			//not already in novelInnovationMap
				      			.filter(c->!novelInnovationMap.keySet().contains(c.getInnovation()))
				      			//innovation is not already consumed in a niche
				      			.filter(c->!POMs.keySet().parallelStream()
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
		List<Integer> swappedOut = POMs.entrySet().parallelStream().filter(e->e.getKey().members.isEmpty())
						   .map(e->e.getValue())
						   .collect(Collectors.toList());
		Integer selection = swappedOut.get(r.nextInt(swappedOut.size()));

		System.out.println("swapIn: selecting from: " + selection);
		//select random POM from innovationDensity	
		PointOfMutation swap = POMs.entrySet().parallelStream().filter(e->e.getValue().equals(selection))
					   .collect(Collectors.toList())
					   .get(r.nextInt((int) POMs.values().parallelStream()
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
