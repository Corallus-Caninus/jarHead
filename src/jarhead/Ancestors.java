package jarhead;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.*;

// ANCESTRAL SPECIATION DEVELOPMENTS
// 	-need to consider weight optimization
// TODO: set private variables/DataStructures accordingly. Consider gettters and setters
public class Ancestors{
	//NOTE: ConnectionGene objects are not comparable. use keySet or innovation number
	public ConcurrentMap<PointOfMutation, Integer> POMs;//concurrent for speciation
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
		swapOut();//clear members
		for(Genome assignGenome : genepool){
			PointOfMutation match = POMs.entrySet().parallelStream()
					.sorted((e1,e2)->e2.getValue().compareTo(e1.getValue()))
				      	.filter(e->assignGenome.getConnectionGenes().keySet()
					      	     .containsAll(e.getKey().innovationGenes.keySet()))
				        .findFirst().get().getKey();
			match.members.add(assignGenome); //defaults to initial POM
		}
		System.out.println("MIGRATION PRINTOUT: ");
		for(PointOfMutation printing : POMs.keySet().parallelStream()
							    .sorted((p1,p2)->p1.highScore.compareTo(p2.highScore))
							    .collect(Collectors.toList())){
			System.out.println("\t " + printing + " With " + printing.members.size() + " @ " + POMs.get(printing) + " Genomes"
						+ " with score " + printing.highScore + " and mascot: " + printing.mascot);
		}
		System.out.println("MIGRATION COMPLETE...");

	}
	public void speciate(Map<Genome, Float> scoreMap){
		boolean branched = false;

		for(PointOfMutation checkPOM : POMs.keySet()){
			Optional<Genome> match = checkPOM.members.parallelStream().filter(g->scoreMap.get(g) > checkPOM.highScore)
								  .filter(g->checkPOM.innovationGenes.keySet().containsAll(g.getConnectionGenes().keySet()))
								  .max((g1,g2)->scoreMap.get(g1).compareTo(scoreMap.get(g2)));
			if(match.isPresent()){
				System.out.println("OPTIMIZATION..");
				PointOfMutation replacement = new PointOfMutation(scoreMap.get(match.get()), match.get());
				POMs.put(replacement, POMs.get(checkPOM));
				//remove all POMs that have lower high score than
				//replacement and lineage greater than replacement
				for(PointOfMutation extinct : POMs.keySet()){
					if(POMs.get(extinct) >= POMs.get(replacement) && extinct.highScore < replacement.highScore){
						POMs.remove(extinct);
					}
				}
				POMs.remove(checkPOM);

				//defragment lineage count
				PointOfMutation buffer = POMs.keySet().stream().findFirst().get();
				for(PointOfMutation rearrange : POMs.keySet().stream().skip(1).collect(Collectors.toSet())){
					if(POMs.get(buffer) < POMs.get(rearrange) - 1){
						POMs.remove(rearrange);
						POMs.put(rearrange, POMs.get(buffer) + 1);
					}
					buffer = rearrange;
				}
				branched = true;
			}
		}

		for(PointOfMutation checkPOM : POMs.keySet()){

			Optional<Genome> match = checkPOM.members.parallelStream().filter(g->scoreMap.get(g) > checkPOM.highScore)
						   //filter out genomes that dont have novel innovations. careful as migrate MUST happen first
						   .filter(g->!checkPOM.innovationGenes.keySet().containsAll(g.getConnectionGenes().keySet()))
						   .max((g1,g2)->scoreMap.get(g1).compareTo(scoreMap.get(g2)));
			if(match.isPresent()){
				System.out.println("HIGHSCORE..");
				Genome newMascot = match.get();
				PointOfMutation addition = new PointOfMutation(scoreMap.get(newMascot), newMascot);

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
	//nicheOptimization (backtracking weight optimization):
	//called within (before) speciate
	//if highscore but no innovation genes remove POM and construct same POM topology 
	//with superior weight schema and remove all POMs with lineage count > POMs.get(checkPOM)
	//and score < highscore
	//
	//else{
	/**
	 * used to declare a niche
	 */
	public void consumeInnovations(){
		List<Integer> potentialNiches = POMs.keySet().parallelStream()
							  .flatMap(p->p.members.parallelStream())
							  .flatMap(g->g.getConnectionGenes().keySet().parallelStream())
							  .filter(c->novelInnovationMap.containsKey(c))
							  .collect(Collectors.toList());
		novelInnovationMap.keySet().removeAll(potentialNiches);
		
	}
	/**
	 * novel innovation produced by genepool. 
	 */
	//TODO: use this for global SCAN_GENOMES check
	public void updateInnovations(List<Genome> nextGenGenomes){
		//add all new ConnectionsGenes
		System.out.println("innovationList count: " + novelInnovationMap.entrySet().parallelStream().count());

		List<ConnectionGene> novelInnovations =	nextGenGenomes.parallelStream().flatMap(g->g.getConnectionGenes().values().parallelStream())
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
	// this can be made adaptable with fitness localization 
	// wrt discovered fitness landscape. innovationDensity holds
	// appropriatness information for selection, backtracking 
	// allows for adaptability
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
