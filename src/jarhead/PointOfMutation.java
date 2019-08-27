package jarhead;
import java.util.*;
import java.util.stream.Collectors;

public class PointOfMutation{
	//public Map<Integer, ConnectionGene> innovationGenes = new HashMap<Integer,ConnectionGene>();
	//no more innovationGenes as defined by distance
	public Genome mascot; //represents highscore
	public Float highScore;
	public List<Genome> snapshot;
	
	//digital-river tree structures 
	public List<PointOfMutation> parents, children;
	public Integer resources, lifetime; //a momentum metric given fitness biased crossover pressure
	
	//Constructor
	//public PointOfMutation(Float score, Genome newGenome, List<Integer> novelInnovationGenes) {	
	public PointOfMutation(Float score, Genome newGenome, List<Genome> record) {	
		mascot = new Genome(newGenome);//snapshot of genome
		highScore = score;//not highScore as not resetting scores on branch.

		
		children = new ArrayList<PointOfMutation>();
		parents = new ArrayList<PointOfMutation>();

		snapshot = new ArrayList<Genome>();
		snapshot.addAll(record);

		//resources = 1;
		lifetime = 1;
	}

	//-------------------------TREE OPERATIONS-----------------------------
	//-----------------------RIVER OF MUTATIONS----------------------------
	//river is special case of tree datastructure: depth-ordered by fitness gradient
	//TODO: make functional and elegant

	/**
	 *remove this node from the tree graph and node list
	 *completely (signal Garbage Collection)
	 */
	public void removeNode(List<PointOfMutation> POMs){
		POMs.remove(this);//remove from nodeList
	
		//remove references to this node in tree
		for(PointOfMutation parent : this.parents){
			parent.children.remove(this);
		}
		for(PointOfMutation child : this.children){
			child.parents.remove(this);
		}
		//remove references from this node in tree
		this.parents.clear();
		this.children.clear();
		this.lifetime=0; //its dead jim..
	}

	/**
	 *remove an edge reference between this and a given POM
	 */
	public void removeEdge(PointOfMutation edge){
		//upstream edge
		if(parents.contains(edge)){
			this.parents.remove(edge);
			edge.children.remove(this);

		}
		//downstream edge
		else if(children.contains(edge)){
			this.children.remove(edge);
			edge.parents.remove(this);
		}
	}
	public void removeEdges(List<PointOfMutation> edges){
		//TODO: this may still be concurently modifying..
		//sort list of edges to be removed
		List<PointOfMutation> parentEdges = edges.stream().filter(x-> this.parents.contains(x)).collect(Collectors.toList());
		List<PointOfMutation> childEdges = edges.stream().filter(x-> this.children.contains(x)).collect(Collectors.toList());

		//remove edges respectively
		for(PointOfMutation parent : parentEdges){
			this.parents.remove(parent);
			parent.children.remove(this);
		}
		for(PointOfMutation child : childEdges){
			this.children.remove(child);
			child.parents.remove(this);
		}
	}

	//Getters and setters for edges make sense due to more than 1
	//operation on more than 1 objects
	
	/**
	 *add a parent node edge to and from this POM
	 */
	public void addParent(PointOfMutation edge){
		this.parents.add(edge);
		edge.children.add(this);
	}
	public void addParents(List<PointOfMutation> edges){
		this.parents.addAll(edges.stream().collect(Collectors.toSet()));
		for(PointOfMutation edge : edges.stream().collect(Collectors.toSet())){
			edge.children.add(this);
		}
	}

	/**
	 *add a child node edge to and from this POM
	 */
	public void addChild(PointOfMutation edge){
		this.children.add(edge);
		edge.children.add(this);
	}
	public void addChildren(List<PointOfMutation> edges){
		this.children.addAll(edges.stream().collect(Collectors.toSet()));
		for(PointOfMutation edge : edges.stream().collect(Collectors.toSet())){
			edge.children.add(this);
		}
	}


	/**
	 *walk tree downstream from this Point and check for any branches 
	 *without parents and remove from graph, search depth first as to 
	 *cascade breakdown of headless branches, remove POMs that break
	 *gradient
	 *
	 *implemented on merge and rescore to preserve fitness gradient in 
	 *forward propagation using localized tree walk
	 */
	public void flowForward(List<PointOfMutation> POMs){
		System.out.println("Flowing river..");
		List<PointOfMutation> verify = new ArrayList<PointOfMutation>();
		List<PointOfMutation> buffer = new ArrayList<PointOfMutation>();
		
		//initialize from this node
		for(PointOfMutation node : this.children){
			//start with localized gradient
			if(node.highScore < this.highScore){
				verify.add(node);
				//verify is first layer of children
				node.removeEdge(this);
				//TODO: concurrent with this.children 
				//	and node.parents list modification
			}
		}
		
		//currently deletes all headless branches completely. might want to fragment
		//or reattach headless branches but changes diversity measurements
		
		if(!verify.isEmpty()){
			//loop
			while(verify.stream().anyMatch(x-> x.children.size() > 0)){ //until are frontier nodes in buffer..
				//user iteration for readability..
				for(PointOfMutation node : verify){ 
					List<PointOfMutation> childNodes = node.children;
					node.removeEdges(node.children);//thats pretty weird..
					buffer.addAll(childNodes.stream().filter(x-> x.parents.isEmpty()).collect(Collectors.toSet())); 
				}
				//remove nodes that are headless and have been propagated
				verify.stream().filter(x-> x.parents.isEmpty())
					       .forEach(x-> x.removeNode(POMs));
				//forward propagate
				verify = buffer;
			}
		}
	}

	//-----------------TODO: too many features. test---------------------
	/**
	 *placeholder function to change diversity metric for
	 *swap selection or implementation across environment
	 *or objective function (domain and range)
	 */
	public int diversityCount(){
		//WATER ANALOGY for acquiring generalization through pressurized genomic operators:
			//if genomic pressure is fitness biased selection
			//diversity count(merge and branch count) is flow measure (lots of potential branching
			//streams). The larger the data river the more likely generalization
			//is occuring, rivers that merge across domains 
			//or fork to multiple environments also have very significant
			//generalization indicators.
			//
			//DYNAMIC CONSIDERATIONS (resampling on swapin):
				//thrashing in dynamic environments (walking PoMs, cycling between node states, etc) eventually 
				//errodes (settles down and creates a steady state between runs). this needs to be improved but 
				//implement with thrashing for simplicity

		//Objective: lookin for niles...
				
		return children.size() + parents.size();
		//parents only works when not reattaching
		//TODO: also need parent/children across domain metric
	}

	//------------------------storage---------------------------
	//TODO should these be in Ancestors? 
	//LoD:
	//	RoM = Ancestors
	//	PoM = PointOfMutations
	/**
	 *writeout the river to filesystem for
	 *preserving datastructure and cross language usage
	 */
	public void writeRiver(){
	}
	/**
	 * read in a previous river for implementation
	 * across domain/range or run
	 */
	public void readRiver(){
	}
	//---------------------------------------------------------

        /**
	 * @Deprecated
	 *tree method
         *search upstream if nodes share a parent
	 *not static as must be constructed with a parent object
         */
        //TODO: can this be a comparator with 2 dimension signature?
        /*public boolean isDescendant(PointOfMutation parent){
                //declare local scope to prevent overwrite of passed in object
		
		List<PointOfMutation> previous = new ArrayList<PointOfMutation>();
		List<PointOfMutation> verify = new ArrayList<PointOfMutation>(); //LinkedList probably faster
		//setup:
		previous.add(this);//searching up from this node up

		//loop:
		while(true){
			//all search branches are terminated at the many-to-one initPoM node
			if(previous.contains(null) && previous.stream().distinct().count() <= 1)
				return false;

			for(PointOfMutation ancestor : previous.parents){
				verify.add(ancestor.parent);
				//evaluate in the loop
				if(ancestor.equals(parent)){
					return(true); 
				}
			}
		}
		//return(false);//unreachable 
        }*/

}

