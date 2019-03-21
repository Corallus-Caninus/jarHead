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
		POMs = new HashMap<PointOfMutation, Integer>();
		
		novelInnovationList.putAll(initialGenome.getConnectionGenes());
		POMs.put(new PointOfMutation(0f, initialGenome), 0);//initial PoM
	}
	//1.CALLED AT COMPATABILITYDISTANCE
	public void assignPOM(Genome assignGenome){
	//migration and placement of genomes.
		PointOfMutation match = POMs.entrySet().stream().sorted((p1,p2)->p2.getValue().compareTo(p1.getValue()))
				      .filter(p->assignGenome.getConnectionGenes().keySet()
				      	     .containsAll(p.getKey().innovationGenes.keySet()))
				      .findFirst().get().getKey();
		match.members.add(assignGenome); //defaults to initial POM(initGenome)
	}
	//2.CALLED AFTER EVALUATION AND ASSIGNPOM
	public boolean speciate(Map<Genome, Float> scoreMap){
		boolean branched = false;
		for(PointOfMutation checkPOM : POMs.keySet()){
			Optional<Genome> match = checkPOM.members.stream().filter(g->scoreMap.get(g) > checkPOM.highScore)
						 .filter(g->!g.getConnectionGenes().keySet().containsAll(checkPOM.innovationGenes.keySet()))
						 .max((g1,g2)->scoreMap.get(g1).compareTo(scoreMap.get(g2)));
			if(match.isPresent()){
				PointOfMutation addition = new PointOfMutation(scoreMap.get(match.get()), match.get());
				POMs.put(addition, POMs.get(checkPOM)+1);
				//remove all innovations found in matching genome
				for(Integer innovation : match.get().getConnectionGenes().keySet()){//slow
					if(novelInnovationList.containsKey(innovation))
						novelInnovationList.remove(innovation);
				}
			        branched = true;
			}
		}
		return branched;//used to call assignPOM again for migration to new POM
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
	//4.CALLED AFTER CROSSOVER AND SWAPIN
	public void swapOut(){
		//TODO: only call when members.isEmpty() to alleviate assignPOM
		for(PointOfMutation swap : POMs.keySet()){
			swap.members.clear();
		}
	}
	//5.CALLED second AFTER CROSSOVER
	public void updateInnovations(List<Genome> nextGenGenomes){
		//add all new ConnectionsGenes
		novelInnovationList.putAll(
				nextGenGenomes.stream().flatMap(g->g.getConnectionGenes().values().stream())
					      .filter(c->!novelInnovationList.keySet().contains(c.getInnovation()))
					      .filter(c->!POMs.keySet().stream()
						      	.map(p->p.innovationGenes)
							.collect(Collectors.toList())
							.contains(c.getInnovation()))
					      .collect(Collectors.toMap(c->c.getInnovation(),c->c))
					);
	}
}
