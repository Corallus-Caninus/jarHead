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
	private float activation;

	/**
	 * Constructs a new Node Gene
	 * 
	 * @param type Input, Hidden or Output.
	 * @param id   id number. equivalent to innovation number for node gene but not
	 *             necessary for compatibility distance function.
	 */
	public NodeGene(TYPE type, int id/* , float activation */) {
		this.type = type;
		this.id = id;
//		this.activation = activation;//*sigmoid activation function*. how does this affect crossover? does this need to be in connectiongene?
	}

	/**
	 * Inherits a Node Gene
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
	// TODO: add public bool signalPassed to allow gate keeping of recurrent (and in
	// the future cyclic) signals.

}
