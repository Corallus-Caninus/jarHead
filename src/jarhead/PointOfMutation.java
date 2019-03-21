package jarhead;
import java.util.*;

public class PointOfMutation{
	public HashMap<Integer, ConnectionGene> innovationGenes; //make this genome instead. too verbose. use both because why not.
       								 //use genome.getConnectionGenes for innovationGenes
	public Genome mascot;
	public Float highScore;
	public List<Genome> members;
	//need mascot for respawn
	
	//Constructor
	public PointOfMutation(float initialScore, Genome newGenome){	
		mascot = new Genome(newGenome);//copy as genomes may be removed. this is historic so must be non-volatile
		innovationGenes = new HashMap<Integer, ConnectionGene>();
		innovationGenes.putAll(newGenome.getConnectionGenes());

		highScore = initialScore;
		members = new ArrayList<Genome>();
		members.add(newGenome);
	}
}

