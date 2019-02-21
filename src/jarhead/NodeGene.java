package jarhead;

import java.io.*;

//where is the activation function?
/**
 * Node Gene class.
 */
public class NodeGene implements Serializable {
	private static final long serialVersionUID = 149348938L;

	public enum TYPE {
		INPUT, HIDDEN, OUTPUT,;// BIAS (SENSOR is input)
	}

	private TYPE type;
	private int id;

	// added bool activated to allow gate keeping of recurrent (and in
	// the future cyclic) signals during forward prop.
	private boolean activated;
	private int depth = 0;

	/**
	 * Constructs a new Node Gene
	 * 
	 * @param type Input, Hidden or Output.
	 * @param id   id number. Innovation number for node gene but not necessary for
	 *             compatibility distance function.
	 */
	public NodeGene(TYPE type, int id/* , float activation */) {
		this.type = type;
		this.id = id;
		activated = false;
//		this.activation = activation;//*sigmoid activation function*. Just a thought does this need to be in connectiongene?
	}

	/**
	 * Inherits a Node Gene. Redundant with respect to constructor
	 * 
	 * @param gene Node Gene to be inherited
	 */
	public NodeGene(NodeGene gene) {
		this.type = gene.type;
		this.id = gene.id;
	}

	// utility functions.
	public TYPE getType() {
		return type;
	}

	public int getId() {
		return id;
	}

	public boolean getActivated() {
		return activated;
	}

	public int getDepth() {
		return depth;
	}

	public void setActivated(boolean activation) {
		this.activated = activation;
	}

	public void setDepth(int newDepth) {
		this.depth = newDepth;
	}

}
