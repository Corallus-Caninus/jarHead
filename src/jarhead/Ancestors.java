package jarhead;

import java.util.*;
import java.util.stream.*;

// ANCESTRAL SPECIATION DEVELOPMENTS
public class Ancestors{
	//NOTE: ConnectionGene objects are not comparable. use keySet or innovation number
	public Map<PointOfMutation, Integer> POMs;
	public HashMap<Integer, ConnectionGene> novelInnovationList;

	//constructor (called in Evaluator constructor)
	public Ancestors(Genome initialGenome){ //initial genome==Species.mascot;
		//newConnectionGenes = new ArrayList<ConnectionGene>(initialGenome.getConnectionGenes().values());
		novelInnovationList = new HashMap<Integer, ConnectionGene>();
		POMs = new HashMap<PointOfMutation, Integer>();
		
		POMs.put(new PointOfMutation(0f, initialGenome),  0);//initial PoM is unique
		novelInnovationList.putAll(initialGenome.getConnectionGenes());
	}
	//1.CALLED AT COMPATABILITYDISTANCE
	public void assignPOM(Genome assignGenome){
	//migration and placement of genomes. prevents speciate from having to migrate
		PointOfMutation match = POMs.entrySet().stream().sorted((p1,p2)->p2.getValue().compareTo(p1.getValue()))
				      .filter(p->assignGenome.getConnectionGenes().keySet()
				      .containsAll(p.getKey().innovationGenes.keySet()))
				      .findFirst().get().getKey(); //TODO: fix POM stream key/values
		//find first should always return given initial POM (special condition)	
		match.members.add(assignGenome); //defaults to initial POM(initGenome)
	}
	//2.CALLED second AFTER CROSSOVER
	public void updateInnovations(List<Genome> nextGenGenomes){
	//add all new ConnectionsGenes
		novelInnovationList.putAll(
				nextGenGenomes.stream().flatMap(g->g.getConnectionGenes().values().stream())
					.filter(c->!novelInnovationList.keySet().contains(c.getInnovation()))
					.collect(Collectors.toMap(c->c.getInnovation(),c->c))
				);
	}
	//3.CALLED AFTER CROSSOVER if epsilon-stagnation s.t. nextGenGenome.size()<population.size()
	public PointOfMutation swapIn(Random r){
		//can bias to innovationDensity by considering all POM values in a List(not Set) 
		//only sort by score to bias towards initial POM
		Integer selection = POMs.values().stream().collect(Collectors.toList()).get(r.nextInt());

		PointOfMutation swap = POMs.entrySet().stream().filter(e->e.getValue().equals(selection))
				.collect(Collectors.toList()).get(r.nextInt()).getKey();
		//PointOfMutation swap = POMs.keySet().stream().sorted((p1,p2)-> p1.highScore.compareTo(p2.highScore))
		//				.filter(p->p.members.isEmpty())
		//				.collect(Collectors.toList())
		//				.get(r.nextInt());//TODO: bias towards innovation density
		return swap;
	}
	//CALLED AFTER CROSSOVER AND SWAPIN
	public void swapOut(){//only call when members.isEmpty()?
		for(PointOfMutation swap : POMs.keySet()){
			swap.members.clear();
		}
	}
	//4.CALLED AFTER EVALUATION AND ASSIGNPOM
	public boolean speciate(Map<Genome, Float> scoreMap){
		boolean branched = false;
		//List<Genome> newPOMs = new ArrayList<Genome>();
		for(PointOfMutation checkPOM : POMs.keySet()){
			//stream over checkPOM and looks for scoremap that breaks highScore
			//filter found genomes for novelInnovationList
			//select maxScore genome
			//
			//branch to new PointOfMutation and add to POMs
			Optional<Genome> match = checkPOM.members.stream().filter(g->scoreMap.get(g) > checkPOM.highScore)
						 .filter(g->!Collections.disjoint(g.getConnectionGenes().keySet(), checkPOM.innovationGenes.keySet()))
						 .max((g1,g2)->scoreMap.get(g1).compareTo(scoreMap.get(g2)));
			if(match.isPresent()){
				PointOfMutation addition = new PointOfMutation(scoreMap.get(match.get()), match.get());
				POMs.put(addition, POMs.get(checkPOM)+1);
			        branched = true; //call assignGenomes again (remigrate)
			}
		}
		
		return branched;//used to call ASSIGNPOM again if necessary
		//need to assignPOM after each call otherwise overmigration
	}
}

