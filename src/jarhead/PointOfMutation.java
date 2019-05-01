package jarhead;
import java.util.*;
import java.util.stream.Collectors;

public class PointOfMutation{
	public Map<Integer, ConnectionGene> innovationGenes = new HashMap<Integer,ConnectionGene>();
	public Genome mascot;
	public Float highScore;
	public List<Genome> members;
	public Integer resources, lifetime;
	
	//Constructor
	public PointOfMutation(Float score, Genome newGenome, List<Integer> novelInnovationGenes) {	
		mascot = new Genome(newGenome);//snapshot of genome
//		innovationGenes = newGenome.getConnectionGenes();
		newGenome.getConnectionGenes().entrySet().parallelStream().filter(e->novelInnovationGenes.contains(e.getKey()))
				.forEach(e->innovationGenes.put(e.getKey(), newGenome.getConnectionGenes().get(e.getKey())));

		highScore = score;//not highScore as not resetting scores on branch.
		members = new ArrayList<Genome>();
//		members.add(newGenome); 
		resources = 1;
		lifetime = 1;
	}
	
}

