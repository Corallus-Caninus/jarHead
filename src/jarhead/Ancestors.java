package jarhead;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.*;

//TODO: make serializable to save Ancestry tree as abstract fitness landscape map.
public class Ancestors {
	public ConcurrentMap<PointOfMutation, Integer> POMs;// concurrent for speciation
	// POMs should be a class (lineage?) more data structures than just lineage and
	// would reduce streaming
	public HashMap<Integer, ConnectionGene> novelInnovationMap;

	// constructor (called in Evaluator constructor)
	public Ancestors(List<Genome> initialGenepool) { // initial genome==Species.mascot;
		novelInnovationMap = new HashMap<Integer, ConnectionGene>();
		POMs = new ConcurrentHashMap<PointOfMutation, Integer>();// Integer is for AncestryTree lineage

		novelInnovationMap.putAll(initialGenepool.get(0).getConnectionGenes());
		PointOfMutation initialPOM = new PointOfMutation(0f, initialGenepool.get(0),
				initialGenepool.get(0).getConnectionGenes().keySet().stream().collect(Collectors.toList()));

		POMs.put(initialPOM, 1);// initial PoM TODO: initial score hyperparameter

		for (int i = 0; i < initialGenepool.size(); i++) {
			initialPOM.members.add(initialGenepool.get(i));
		}
		initialPOM.resources = initialGenepool.size() * 100;
		initialPOM.lifetime = initialPOM.resources;
	}

	public void migrate(List<Genome> genepool) {
		// migration and placement of genomes.
		for (Genome assignGenome : genepool) {
			PointOfMutation match = POMs.entrySet().stream().sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
					.filter(e -> assignGenome.getConnectionGenes().keySet().stream()
											 .anyMatch(c -> e.getKey().innovationGenes.keySet().contains(c)))
					.findFirst().get().getKey();

			if (match.members.isEmpty() /* || match.lifetime == 0 */) { // swapped out therefore migrating away from
																		// current POM
				System.out.println("\n\nmigrating" + /* transferPOM + */" to " + match + "..\n");
				match.resources++; // only assign resources if topologies tend to migrate this way
			}
		}

		// remove last generations genomes from POM members
		for (PointOfMutation prevPOM : POMs.keySet()) {
			prevPOM.members.removeAll(POMs.keySet().parallelStream().flatMap(p -> p.members.stream())
					.filter(g -> !genepool.contains(g)).collect(Collectors.toList()));
		}

		// clear members (extinct/swapout)
		for (PointOfMutation extinction : POMs.keySet()) {
			if (extinction.lifetime == 0) {
				genepool.removeAll(extinction.members);
				extinction.members.clear();
			} else if (extinction.members.isEmpty()) { // prepare for swapin
				extinction.lifetime = 0;
			}
		}

		System.out.println("MIGRATION PRINTOUT: ");
		for (PointOfMutation printing : POMs.keySet().parallelStream()
				.sorted((p1, p2) -> p1.highScore.compareTo(p2.highScore)).collect(Collectors.toList())) {
			System.out.println("\t " + printing + " With population " + printing.members.size() + " with lineage "
					+ POMs.get(printing) + " with score " + printing.highScore + " and resources " + printing.resources
					+ " and lifetime " + printing.lifetime + " and mascot: " + printing.mascot + " with genes: "
					+ printing.mascot.getConnectionGenes().size() + " and novelGenes: "
					+ printing.innovationGenes.size() + " and Topology: " + printing.mascot.getNodeGenes().size());
		}
		System.out.println("MIGRATION COMPLETE...");
	}

	public void speciate(Map<Genome, Float> scoreMap) {
		boolean branched = false;

//		System.out.println(POMs.keySet().parallelStream().flatMap(p->p.members.stream()).filter(g->!scoreMap.keySet().contains(g))
//									  .collect(Collectors.toList()).size());

		for (PointOfMutation checkPOM : POMs.keySet()) {
			Optional<Genome> match = checkPOM.members.parallelStream().filter(g -> scoreMap.get(g) > checkPOM.highScore)
					.filter(g -> checkPOM.innovationGenes.keySet().equals(g.getConnectionGenes().keySet()))
					.max((g1, g2) -> scoreMap.get(g1).compareTo(scoreMap.get(g2)));
			if (match.isPresent()) {
				// TODO: optimize in migration for fuzzy speciation
				System.out.println("OPTIMIZATION..");
				PointOfMutation replacement = new PointOfMutation(scoreMap.get(match.get()), match.get(),
						checkPOM.innovationGenes.keySet().stream().collect(Collectors.toList()));
				replacement.resources = checkPOM.resources; // TODO: copy POM constructor
				replacement.lifetime = checkPOM.lifetime;
				replacement.members.addAll(checkPOM.members);
				POMs.put(replacement, POMs.get(checkPOM));

				POMs.remove(checkPOM); // remove old unoptimized POM. should this keep resources

				// defragment lineage count
				// TODO: last POM doesnt get reduced
				PointOfMutation buffer = POMs.keySet().parallelStream().findFirst().get();
				for (PointOfMutation rearrange : POMs.keySet().parallelStream().skip(1).collect(Collectors.toSet())) {
					if (POMs.get(buffer) < POMs.get(rearrange) - 1) {
						POMs.remove(rearrange);
						POMs.put(rearrange, POMs.get(buffer) + 1);
					}
					buffer = rearrange;
				}
				branched = true;
			}
		}

		for (PointOfMutation checkPOM : POMs.keySet()) {

			Optional<Genome> match = checkPOM.members.parallelStream().filter(g -> scoreMap.get(g) > checkPOM.highScore)
			// filter out genomes that dont have novel innovations. careful as migrate MUST
			// happen first
//						   .filter(g->!checkPOM.innovationGenes.keySet().containsAll(g.getConnectionGenes().keySet())) //redundant with next filter
					.filter(g -> g.getConnectionGenes().keySet().stream()
							.anyMatch(c -> novelInnovationMap.keySet().contains(c)))
					// ensure POM doesnt already exist
//						   .filter(g-> !POMs.keySet().parallelStream().map(p->p.innovationGenes.keySet()).anyMatch(i->i.containsAll(g.getConnectionGenes().keySet())))
					.max((g1, g2) -> scoreMap.get(g1).compareTo(scoreMap.get(g2)));
			if (match.isPresent()) {
				System.out.println("HIGHSCORE..");
				Genome newMascot = match.get();

				List<Integer> novelInnovationGenes = match.get().getConnectionGenes().keySet().parallelStream()
						.filter(c -> !POMs.keySet().parallelStream().flatMap(p -> p.innovationGenes.keySet().stream())
								.anyMatch(i -> i.equals(c)))
						.collect(Collectors.toList());

				PointOfMutation addition = new PointOfMutation(scoreMap.get(newMascot), newMascot,
						novelInnovationGenes);

				POMs.put(addition, POMs.get(checkPOM) + 1);
				consumeInnovations();
				// remove all innovations found in matching genome (HashMap method)
				if (checkPOM.resources > checkPOM.lifetime && POMs.get(checkPOM) != 1) { // why is this condition
																							// necessary? trace resource
																							// manipulation migration
																							// may be flawed
					checkPOM.resources = checkPOM.resources - checkPOM.lifetime; // keep search limited to minima
																					// innovation gap.
					checkPOM.lifetime = checkPOM.resources; // controversal but works
				}

				branched = true;
			}
		}

		// survival of the fittest (remove niches that overconsume and undercompete)
		for (PointOfMutation fittestSurvivor : POMs.keySet()) {
			// BUG: was removing both values
			POMs.entrySet().parallelStream()
					.filter(p -> fittestSurvivor.highScore >= p.getKey().highScore && fittestSurvivor.mascot
							.getConnectionGenes().size() < p.getKey().mascot.getConnectionGenes().size()
							&& !fittestSurvivor.equals(p.getKey()))
					.forEachOrdered(p -> POMs.remove(p.getKey()));
		}

		// consider new POMs for migration
		if (branched) {
			migrate(scoreMap.keySet().parallelStream().filter(g -> POMs.keySet().stream() // only pass current
																							// generation. mine as well
																							// pass POM members.
					.flatMap(p -> p.members.stream()).collect(Collectors.toList()).contains(g))
					.collect(Collectors.toList()));
		}
	}

	/**
	 * used to declare a niche
	 */
	public void consumeInnovations() {
		List<Integer> potentialNiches = POMs.keySet().parallelStream().flatMap(p -> p.members.parallelStream())
				.flatMap(g -> g.getConnectionGenes().keySet().parallelStream())
				.filter(c -> novelInnovationMap.containsKey(c)).collect(Collectors.toList());
		novelInnovationMap.keySet().removeAll(potentialNiches);

	}

	/**
	 * novel innovation produced by genepool.
	 */
	// TODO: use this for global SCAN_GENOMES check
	public void updateInnovations(List<Genome> nextGenGenomes) {
		// add all new ConnectionsGenes
		System.out.println("innovationList count: " + novelInnovationMap.entrySet().parallelStream().count());

		List<ConnectionGene> novelInnovations = nextGenGenomes.parallelStream()
				.flatMap(g -> g.getConnectionGenes().values().parallelStream())
				// not already in novelInnovationMap
				.filter(c -> !novelInnovationMap.keySet().contains(c.getInnovation()))
				// innovation is not already consumed in a niche
				.filter(c -> !POMs.keySet().parallelStream().flatMap(p -> p.innovationGenes.keySet().stream())
						.collect(Collectors.toList()).contains(c.getInnovation()))// autobox rollout
				.collect(Collectors.toList());
		for (ConnectionGene gene : novelInnovations) {
			if (!novelInnovationMap.containsKey(gene.getInnovation()))
				novelInnovationMap.put(gene.getInnovation(), gene);
		}
	}

	public PointOfMutation swap(Random random, Integer populationSize) {
		List<PointOfMutation> tables = POMs.keySet().parallelStream().filter(p -> p.lifetime == 0)
				.filter(p -> p.resources >= populationSize) // limit thrashing and partial generations
				.collect(Collectors.toList());
		if (!tables.isEmpty()) {
			PointOfMutation swap = tables.get(random.nextInt(tables.size()));

			PointOfMutation first = POMs.entrySet().stream().filter(e -> e.getValue() == 1).findFirst().get().getKey();
			if (POMs.size() == 1) {
				first.resources += populationSize;
			}

			if (swap.lifetime == 0) { // redundant
				swap.lifetime = swap.resources;
			}
//			System.out.println("swap: Selected POM: " + swap);
			return swap;
		} else {
			return null;
		}
	}
}
