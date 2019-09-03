package jarhead;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.*;
import java.io.*;

//TODO: bad encapsulation including Evaluator
//
//TODO: store datastructure representation in a file format for
//	reading into d3 and other languages. implement write and read methods
//
//
//TODO: change large streaming methods to Apache Spark. Extract evaluator methods
//	out to here as this should be only class calling 
//	Apache Spark methods (cannot call spark methods from workers)
//		in the iterim, cheap way to perform distribution is from Evaluation
//		loop at swap. swap in until workers saturated then continue, broadcast if
//		any update methods besides default case trigger.
//
//TODO: branch from Ancestral_Speciation to Ancestral_Genomic in git repo
//
//GOAL: Get working then simplify. simple operations are getting complex in detail (DID)
//
//Generalization: across domain and range (environment and objective function):
//	keep innovation genes consistent (initial topology of new domain's input/output nodes get newest innovationGene count)
//		
//		need to differentiate between native topology and imported topology
//		can have many imported PoMs tested. Rivers of PoMs from various domains/ranges
//		creates a many to one test case:
//			imported topology needs to be tested to see which if any PoMs should
//			be imported. (initial cross-range/domain POM evaluation biased to or 
//			selecting exclusively from PoMs with high diversity indicators)
//				diversity indicators are merge count across domain and objective functions and branch count

public class Ancestors {
	public List<PointOfMutation> POMs; //list of nodes in the river-tree graph for faster localized changes
	public Map<Integer, ConnectionGene> innovationMap = new HashMap<Integer, ConnectionGene>();

	// constructor (called in Evaluator constructor)
	public Ancestors(List<Genome> initialGenepool, int stagnation) { // initial genome

		innovationMap = new HashMap<Integer, ConnectionGene>();

		POMs = new ArrayList<PointOfMutation>(); //list of PointOfMutation nodes

		innovationMap.putAll(initialGenepool.get(0).getConnectionGenes());

		/*PointOfMutation initialPOM = new PointOfMutation(0f, initialGenepool.get(0),
					        		     initialGenepool.get(0).getConnectionGenes()
								     .keySet().stream().collect(Collectors.toList()));*/
		PointOfMutation initialPOM = new PointOfMutation(0f, initialGenepool.get(0), initialGenepool); 

		POMs.add(initialPOM); //TODO: anchor initPOM so it doesnt walk CRITICAL FOR CONTINUITY

		for (int i = 0; i < initialGenepool.size(); i++) {
			initialPOM.snapshot.add(initialGenepool.get(i));
		}
		initialPOM.lifetime = stagnation;
		printGraph();
	}
	/**
	 *reads in a RoM file for recovery from file system
	 *
	 *resources and lifetime are not kept between runs as these are assigned by the evaluator
	 *
	 *file is partitioned into thirds of ascending defining attributes
	 *
	 *node data - edge data - attribute data(TODO)
	 *
	 *FORMAT:
	 *	() == token
	 *
	 *	node (tab) highscore 
	 *	(newLine) iterate
	 *	(double newLine) //end of node data
	 *
	 * 	(P)
	 *	parents
	 *	(C)
	 *	children
	 *	(newLine) iterate
	 *	(double newLine) //end of edge data
	 *	//currently this is the end of the file as this is all thats needed for basic visualization
	 *
	 *	(S)
	 *	snapshot
	 *	(M)
	 *	mascot
	 *	(newLine) iterate
	 *	(double newLine) //EOF
	 */
	//NOTE: be careful about using tokens that could appear in object reflection (vitual address thing)
	public void writeRiver(){
		try{
			//initialize the outStream
			FileWriter writeROM = new FileWriter(this.toString()); //this should work as object is constructed in the 'heap'
			//writeout the virtual address of the object as a serialization throwback
		
			//GRAPH/VISUALIZATION SPECIFIC DATASETS
			for(PointOfMutation node : POMs){ //TODO: ensure this is sequential.. yes (the world may never know)
				//writeout all nodes to the front of the file
				writeROM.write(node.toString()); 
				writeROM.write('\t');
				//writeout fitness of respective nodes
				writeROM.write(node.highScore.toString());
				writeROM.write('\n');
			}
			writeROM.write('\n');//END OF NODE INDICATOR
			writeROM.write('\n');//END OF NODE INDICATOR

			for(PointOfMutation node : POMs){
				//writeout all the edge information to the back of the file
				for(PointOfMutation parent : node.parents){
					writeROM.write('P');//PARENT INDICATOR
					writeROM.write(parent.toString());
				}
				for(PointOfMutation child : node.children){
					writeROM.write('C');//CHILD INDICATOR
					writeROM.write(child.toString());
				}
				writeROM.write('\n');
			}
			writeROM.write('\n');//END OF EDGE INDICATOR
			writeROM.write('\n');//END OF EDGE INDICATOR

			//MISC DATASETS
			/*
			for(PointOfMutation node : POMs){
				writeROM.write('S');
				for(Genome genome : node.snapshot){
					writeROM.write(genome); //TODO: how are genomes defined? writeout serialization?
				}
				writeROM.write('M');
				writeROM.write(node.mascot);
				//writeout all attribute data per node (snapshot etc)
				writeROM.write('\n')
			}
			writeROM.write('\n\n');//EOF INDICATOR
			*/
			writeROM.close();
		}catch(IOException e){
			System.out.println("ERROR: failed to write RoM");
			e.printStackTrace();
		}
	}
	public void readRiver(){
		//initialize the readStream
		//read in node information
		//associate with edge information
	}
	
	/**
	 *fundamental mapping operation; traces/maps meaningful topologies
	 *acquired through neuroevolution runs for pattern analysis and 
	 *search heuristic
	 */
	//TODO: raviolli
	public PointOfMutation update(Map<Integer, ConnectionGene> innovations, Map<Genome, Float> scoremap, PointOfMutation curPoM, float distance, float C1, float C2, float C3, int resources){
		printGraph();
		writeRiver();
		innovationMap = innovations;

		Map.Entry<Genome, Float> newMascot = scoremap.entrySet().stream().max((x1,x2)-> x1.getValue().compareTo(x2.getValue())).get();
		
		//DYNAMIC CONSIDERATIONS
		//TODO: this could be called in swap.
		if(scoremap.keySet().equals(curPoM.snapshot)){ //distance doesnt need to be calculated
			System.out.println("(RoM-update) Evaluation broke determinism..");
			if(newMascot.getValue() != curPoM.highScore){
				curPoM.highScore = newMascot.getValue();

				updatePotential(curPoM, resources); //alternative to insertPotential could cause greater or lower fitness

				//shouldnt have to swap here as lifetime is preserved (just altered highScore)
				return(curPoM);

				//TODO: this should be dynamically programmed within reattachment and fragmentation
				//	this is currently overly destructive and will heavily slow down optimization
			}
		}
		
		//SEARCH FOR POTENTIAL NODE MERGERS (collaboratively destructive)
		//lots of ways to implement this..
		if(curPoM.highScore < newMascot.getValue()){ 
			//TODO: should be able to redefine other PoMs without considering curPoM given genomic distance
			//	this isnt sample efficient
			List<PointOfMutation> merger = POMs.stream().filter(x-> Chromosome.compatibilityDistance(newMascot.getKey(), x.mascot, C1, C2, C3) < distance)
								    .filter(x-> newMascot.getValue() > x.highScore)
								    .collect(Collectors.toList());
			if(!merger.isEmpty()){	
				System.out.println("(RoM-update) Merging with " + merger.size() + " POMs");

				//PREPARING GENEPOOL SNAPSHOT
				List<Genome> snapshotBuffer = new ArrayList<Genome>();
				snapshotBuffer.addAll(merger.stream().sorted((x1,x2)-> x1.highScore.compareTo(x2.highScore)) //sort by HighScore for clipping
								     .flatMap(x-> x.snapshot.stream())//transform to flatmap (verify operation is sequential, lazy should keep sort
								     .collect(Collectors.toList()));//amoeba

				if(snapshotBuffer.size() < curPoM.snapshot.size()){ 
					snapshotBuffer.addAll(curPoM.snapshot); //append to end of list
					//if needing to pad POM pad with current genepool as it has pertinence via genesis
					//
					//TODO: how can a self merge fill its genepool? same as this.
					//
					//TODO: change to performing emergency crossover of mascots and partial genepool until maxPop
					//	instead of this case? I actually kinda like it..
				}

				snapshotBuffer = snapshotBuffer.subList(0, curPoM.snapshot.size()); 

				//PREPARING POM OBJECT
				PointOfMutation speciated = speciate(newMascot, snapshotBuffer, curPoM);

				//include curPoM as a new parent in merger unless it is included in the blob
				/*if(!merger.contains(curPoM) && !merger.stream().flatMap(x-> x.parents.stream()).anyMatch(x->x.equals(curPoM))
					&& !merger.stream().flatMap(x-> x.children.stream()).anyMatch(x->x.equals(curPoM))){  
					speciated.addParent(curPoM); //TODO: this is wrong. wouldnt this be covered in speciation case
				}*/

				//CLEANUP DEFUNCT CHILD NODES
				insertPotential(speciated, merger);
				
				//RETURN SPECIATED POM TO CONTINUE EVOLUTION/EVALUATION
				return(speciated); 
			}

			//SPECIATE A NOVEL POM NODE (constructive)
			else if(merger.isEmpty() && newMascot.getValue() > curPoM.highScore){
				System.out.println("(RoM-update) Novel POM acquired..");

				PointOfMutation speciated = speciate(newMascot, curPoM.snapshot, curPoM);
				speciated.addParent(curPoM);

				printGraph();
				return(speciated);
			}

		}
		//else{
		//DEFAULT CASE (fallthrough)
			//NO SPECIATION HAS OCCURED: CONTINUE
			System.out.println("Passthrough..");
			return(curPoM);
		//}
	}

	//NOTE: both Potential methods should initialize the node locally for flowForward nothing more.
	//	TODO: should Potential methods be extracted to PointOfMutation since localized tree operations
	/**
	 *insert newly created POM merge into graph and cleanup any headless nodes/branches
	 *caused by correcting fitness gradient downstream
	 *
	 *also seam any connections that are lost by altering the river graph with 
	 *merge-consolidation (the blob)
	 *
	 * river analogy: creates a change in potential at a specific point by crossing 
	 * the streams, possibly causing a reflow over existing channels (edges)
	 */
	//TODO: do merge of POMs indicate diversity wrt walking POMs? likely not..
	//	this still has some information about the distance covered in evaluating this
	//	PointOfMutation, but shows latent hillClimbing. investigate further
	private void insertPotential(PointOfMutation speciated, List<PointOfMutation> blob){

		//TODO: first development is walk the graph
		List<PointOfMutation> currentNodes = new ArrayList<PointOfMutation>();
		List<PointOfMutation> nextNodes = new ArrayList<PointOfMutation>();

		HashMap<List<PointOfMutation>, List<PointOfMutation>> residues = new HashMap<PointOfMutation, List<PointOfMutation>>();
		//setup:
		currentNodes.add(POMs.stream().findAny(x-> x.parents.isEmpty()).get());

		//loop:
		while(currentNodes.stream().allMatch(x-> x.children.isEmpty())){
			for(PointOfMutation node : currentNodes){
				for(PointOfMutation child : node.children){
					//check for rising edges
					if(blob.contains(child)){
						//add all parents that are outside the blob boundary
						residues.put(child, child.parents.stream().filter(x-> !blob.contains(x)).collect(Collectors.toList()));
						//replace existing residues with their blob children nodes
					}
					//check for falling edges
				}
				//if a rising edge is found add current node parents that arent in the blob to map
				//	entry: non-blob parents value: node discovered
				//if blob node has children outside the blob a seam is formed
				//	add edges from discovered entry to this nodes non-blob children
				//else if blob node has children inside the blob add child node parents
				//	that are outside the blob to blob nodes entry
				//
				//(default case) doesnt need consideration and will be GC'd
				//if no children are left
				//	blob is frontier and remove
				nextNodes.addAll(node.children);
			}
			currentNodes.clear();
			currentNodes.addAll(nextNodes);
			nextNodes.clear();
		}

		//End of Blob-Seaming
		
		//TODO: not as trivial as this. need to place as a distinct speciated child from furthest
		//	child candidates and add all parents
		List<PointOfMutation> boundaryChildren = blob.stream()
							       .flatMap(x-> x.children.stream())
							       .filter(x-> !merger.contains(x))
							       .collect(Collectors.toList());

		List<PointOfMutation> boundaryParents = blob.stream()
							      .flatMap(x-> x.parents.stream())
							      .filter(x-> !merger.contains(x))
							      .collect(Collectors.toList());
		//TODO: easiest merge solution is to walk through the tree, 
		//shorting across merge candidates to reform the graph
		//this would be a special case of flowForward

		speciated.addParents(boundaryParents);
		speciated.addChildren(boundaryChildren);

		//completely remove all now merged POMs
		blob.forEach(x-> x.removeNode(POMs));

		speciated.flowForward(POMs);
		//ready to flowForward, brokenChildren will be filtered in this walk
	}

	/**
	 *reset tree locally if score has changed and resort fitness gradient
	 *called on swapIn evaluation of existing POM genepool representation
	 */
	private void updatePotential(PointOfMutation rescore, int resources){
		if(!rescore.parents.contains(rescore)){  //special case for initPoM
			//gradient case 2
			for(PointOfMutation parent : rescore.parents){
				parent.flowForward(POMs); 
			}

			//gradient case 3 and 1
			rescore.flowForward(POMs);
		}
	}
	
	/**
	 *called within all merge methods with different implementations for
	 *multi-merge, self-merge and generic speciation
	 *
	 * sets highScore, snapshot, mascot (center of distance area/circle) and parent
	 */
	public PointOfMutation speciate(Map.Entry<Genome, Float> newMascot, List<Genome> snapshot, PointOfMutation parent) {
		PointOfMutation speciated = new PointOfMutation(newMascot.getValue(), newMascot.getKey(), snapshot);
		
		//speciated.addParent(parent); 
		POMs.add(speciated);

		return speciated;
	}

	/**
	 *swap out the current PoM for one in the list if the lifetime is 0 
	 */
	public PointOfMutation swap(Integer populationSize, PointOfMutation curPoM, int resources) {
		Random random = new Random();
		if(curPoM.lifetime <= 0){ //allows for swap to be implemented where lifetime % genepool.size() != 0 
			PointOfMutation swapin = POMs.get(random.nextInt(POMs.size()));
			swapin.lifetime = resources;
			System.out.println("(swap) SWAPING OUT " + curPoM + "for: " + swapin);
			return swapin;
		}
		//dont swap out
		else
			return curPoM;
	}		
	
	public void printGraph(){
		//get first node
		List<PointOfMutation> head = POMs.stream().filter(x-> x.parents.isEmpty()).collect(Collectors.toList());//top o the river to ya
		
		System.out.println("(RoM) Printout: ");
		for(PointOfMutation node : POMs){
			System.out.println("Node: " + node + "-" + node.highScore +" has children: ");
			for(PointOfMutation child : node.children){
				System.out.println(child + "-" + child.highScore);
			}
		}
		//works but contains circularity
		System.out.println("Printout with " + head.size() + " fragmentations");
		if(!head.isEmpty()){
			//DEBUG: first frag has is a child with no parent node registered..
			for(PointOfMutation frag : head){
				System.out.println("Top o the river.."); 
				List<PointOfMutation> current = new ArrayList<PointOfMutation>();
				List<PointOfMutation> buffer = new ArrayList<PointOfMutation>();

				//current.add(head.get(0));
				current.add(frag);
				//drill down the river
				while(current.stream().flatMap(x-> x.children.stream()).count() > 0){
						for(PointOfMutation node : current){
							System.out.println("PointOfMutation: " + node + "-" + node.highScore + " contains..");
							for(PointOfMutation child : node.children){
								System.out.println("--" + child + "-" + child.highScore);
							}
						}
					buffer.addAll(current.stream().flatMap(x-> x.children.stream()).collect(Collectors.toSet()));
					current.clear();
					current.addAll(buffer);
					buffer.clear();
				}
			}
		}
		//exit
	}

	//Only pertinent for storing Ancestry structure
	/**
	 * @Deprecated
	 * used to declare a niche
	 * in new method: used to keep innovation genes consistent
	 * across POMs.
	 */
	//TODO: ensure innovationGenes are collected here and not in evaluator/genome 
	//	(SEE BELOW)
	/*public void consumeInnovations() {
		List<Integer> potentialNiches = POMs.keySet().parallelStream().flatMap(p -> p.snapshot.parallelStream())
				.flatMap(g -> g.getConnectionGenes().keySet().parallelStream())
				.filter(c -> innovationMap.containsKey(c)).collect(Collectors.toList());
		innovationMap.keySet().removeAll(potentialNiches);

	}*/

	/**
	 * @Deprecated
	 * novel innovation produced by genepool.
	 */
	// TODO: use this for global SCAN_GENOMES check
	/*public void updateInnovations(List<Genome> nextGenGenomes) {
		// add all new ConnectionsGenes
		System.out.println("innovationList count: " + innovationMap.entrySet().parallelStream().count());

		List<ConnectionGene> novelInnovations = nextGenGenomes.parallelStream()
				.flatMap(g -> g.getConnectionGenes().values().parallelStream())
				// not already in innovationMap
				.filter(c -> !innovationMap.keySet().contains(c.getInnovation()))
				.collect(Collectors.toList());

		for(ConnectionGene entry : novelInnovations){
			innovationMap.put(entry.getInnovation(), innovation);
		}
	}*/

	//return a randomly selected PoM from list (implement randomWalk when biasing selection to lineage)
	//	return a fully swapped in PoM with snapshot if swapped
	/**
	 *potentially return a swapped in PoM, fundamental operation for implementing mapped graph(load from graph for runtime)
	 */
	//TODO: possibly add special case to randomize initPOM topology as per k.stanley
	//TODO: this is broken in implementation.. shouldnt be since this is the only place lifetime is increased
	//	SWAP IS NEVER CALLED!!!!!
}
