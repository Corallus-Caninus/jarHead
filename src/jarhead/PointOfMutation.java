package jarhead;
import java.util.*;

public class PointOfMutation{
	public Map<Integer, ConnectionGene> innovationGenes;
	public Genome mascot;
	public Float highScore;
	public List<Genome> members;
	
	//Constructor
	public PointOfMutation(Float score, Genome newGenome){	
		mascot = new Genome(newGenome);//snapshot of genome
		innovationGenes = newGenome.getConnectionGenes();

		highScore = score;//not highScore as not resetting scores on branch.
		members = new ArrayList<Genome>();
		members.add(newGenome); //TODO: getter and setters?
	}
}

