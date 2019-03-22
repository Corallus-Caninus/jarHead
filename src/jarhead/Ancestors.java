package jarhead;

import java.util.*;
import java.util.stream.*;

// ANCESTRAL SPECIATION DEVELOPMENTS
// 	-need to consider weight optimization
// TODO: set private variables/DataStructures accordingly.
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
		swapOut();//clear members
		for(Genome assignGenome : genepool){
			PointOfMutation match = POMs.entrySet().stream().sorted((e1,e2)->e2.getValue().compareTo(e1.getValue()))
				      	.filter(e->assignGenome.getConnectionGenes().keySet()
					      	     .containsAll(e.getKey().innovationGenes.keySet()))
				        .findFirst().get().getKey();
			match.members.add(assignGenome); //defaults to initial POM(initGenome)
		}
	}
	public void speciate(Map<Genome, Float> scoreMap){
		boolean branched = false;
		for(PointOfMutation checkPOM : POMs.keySet()){
			Optional<Genome> match = checkPOM.members.stream().filter(g->scoreMap.get(g) > checkPOM.highScore)
						   .filter(g->checkPOM.innovationGenes.keySet().containsAll(g.getConnectionGenes().keySet()))
						   .max((g1,g2)->scoreMap.get(g1).compareTo(scoreMap.get(g2)));
			if(match.isPresent()){
				System.out.println("HIGHSCORE");
				PointOfMutation addition = new PointOfMutation(scoreMap.get(match.get()), match.get());
				POMs.put(addition, POMs.get(checkPOM)+1); //how does long term crossover effect this?
				//remove all innovations found in matching genome (HashMap method)
				for(Integer innovation : match.get().getConnectionGenes().keySet()){
					if(novelInnovationMap.containsKey(innovation))
						novelInnovationMap.remove(innovation);
				}
			        branched = true;
			}
		}
		//consider new POMs for migration
		if(branched){
			migrate(scoreMap.keySet().stream().collect(Collectors.toList()));
		}
	}
	public PointOfMutation swapIn(Random r){
		//select biased towards innovationDensity
		Integer selection = POMs.values().stream().collect(Collectors.toList()).get(r.nextInt());
		//select random POM from innovationDensity
		PointOfMutation swap = POMs.entrySet().stream().filter(e->e.getValue().equals(selection))
					   .collect(Collectors.toList()).get(r.nextInt()).getKey();
		return swap;//add mutated mascot till nextGenGenome.size()==population.size()
	}
	public void updateInnovations(List<Genome> nextGenGenomes){
		//add all new ConnectionsGenes
		System.out.println("innovationList: " + novelInnovationMap);

		List<ConnectionGene> novelInnovations =	nextGenGenomes.stream().flatMap(g->g.getConnectionGenes().values().stream())
				      			//not already in novelInnovationMap
				      			.filter(c->!novelInnovationMap.keySet().contains(c.getInnovation()))
				      			//innovation is not consumed in a niche
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
	private void swapOut(){
		//TODO: only call when members.isEmpty() to alleviate assignPOM
		for(PointOfMutation swap : POMs.keySet()){
			swap.members.clear();
		}
	}
}
