package jarhead;

import java.util.*;
import java.util.stream.*;

// ANCESTRAL SPECIATION DEVELOPMENTS
// 	-need to consider weight optimization
public class Ancestors{
	//NOTE: ConnectionGene objects are not comparable. use keySet or innovation number
	public Map<PointOfMutation, Integer> POMs;
	public HashMap<Integer, ConnectionGene> novelInnovationList;

	//constructor (called in Evaluator constructor)
	public Ancestors(Genome initialGenome){ //initial genome==Species.mascot;
		novelInnovationList = new HashMap<Integer, ConnectionGene>();
		POMs = new HashMap<PointOfMutation, Integer>();//Integer is for AncestryTree lineage
		
		novelInnovationList.putAll(initialGenome.getConnectionGenes());
		POMs.put(new PointOfMutation(0f, initialGenome), 0);//initial PoM
	}
	//1.CALLED AT COMPATABILITYDISTANCE
	public void migrate(List<Genome> genepool){
	//migration and placement of genomes.
		swapOut();//always called anyways
		for(Genome assignGenome : genepool){
			PointOfMutation match = POMs.entrySet().stream().sorted((e1,e2)->e2.getValue().compareTo(e1.getValue()))
				      	.filter(e->assignGenome.getConnectionGenes().keySet()
					      	     .containsAll(e.getKey().innovationGenes.keySet()))
				        .findFirst().get().getKey();
			match.members.add(assignGenome); //defaults to initial POM(initGenome)
		}
	}
	//2.CALLED AFTER EVALUATION 
	public void speciate(Map<Genome, Float> scoreMap){
		boolean branched = false;
		for(PointOfMutation checkPOM : POMs.keySet()){
			Optional<Genome> match = checkPOM.members.stream().filter(g->scoreMap.get(g) > checkPOM.highScore)
						 .filter(g->!g.getConnectionGenes().keySet().containsAll(checkPOM.innovationGenes.keySet()))
						 .max((g1,g2)->scoreMap.get(g1).compareTo(scoreMap.get(g2)));
			if(match.isPresent()){
				PointOfMutation addition = new PointOfMutation(scoreMap.get(match.get()), match.get());
				POMs.put(addition, POMs.get(checkPOM)+1);
				//remove all innovations found in matching genome (HashMap method)
				for(Integer innovation : match.get().getConnectionGenes().keySet()){
					if(novelInnovationList.containsKey(innovation))
						novelInnovationList.remove(innovation);
				}
			        branched = true;
			}
		}
		//consider new POMs for migration
		if(branched){
			migrate(scoreMap.keySet().stream().collect(Collectors.toList()));
		}
	}
	//3.CALLED AFTER CROSSOVER if epsilon-stagnation in crossover s.t. nextGenGenome.size()<population.size()
	public PointOfMutation swapIn(Random r){
		//select biased towards innovationDensity
		Integer selection = POMs.values().stream().collect(Collectors.toList()).get(r.nextInt());
		//select random POM from innovationDensity
		PointOfMutation swap = POMs.entrySet().stream().filter(e->e.getValue().equals(selection))
					   .collect(Collectors.toList()).get(r.nextInt()).getKey();
		return swap;//add mutated mascot till nextGenGenome.size()==population.size()
	}
	//4.CALLED AFTER CROSSOVER (swapin doesnt matter as POMs are persistent)
	public void updateInnovations(List<Genome> nextGenGenomes){
		//add all new ConnectionsGenes
		novelInnovationList.putAll(
				nextGenGenomes.stream().flatMap(g->g.getConnectionGenes().values().stream())
					      //not already in novelInnovationList
					      .filter(c->!novelInnovationList.keySet().contains(c.getInnovation()))
					      //innovation is not consumed in a niche
					      .filter(c->!POMs.keySet().stream()
						      	.map(p->p.innovationGenes)
							.collect(Collectors.toList())
							.contains(c.getInnovation()))
					      .collect(Collectors.toMap(c->c.getInnovation(),c->c))
					  );
	}
	//CALLED IN MIGRATE
	private void swapOut(){
		//TODO: only call when members.isEmpty() to alleviate assignPOM
		for(PointOfMutation swap : POMs.keySet()){
			swap.members.clear();
		}
	}
}
