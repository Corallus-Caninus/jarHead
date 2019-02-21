package jarhead;

import java.io.*;
import java.util.List;
import java.util.Optional;

/**
 * Connection Gene class for node connections.
 */
public class ConnectionGene implements Serializable {
	private static final long serialVersionUID = 139348938L;

	private int inNode;
	private int outNode;
	private float weight;
	private boolean expressed;
	private int innovation;

	/**
	 * Constructs a new connection gene.
	 * 
	 * @param inNode     input node
	 * @param outNode    output node
	 * @param weight     weight of the node
	 * @param expressed  boolean whether node is expressed or disabled
	 * @param innovation global innovation number
	 */
	public ConnectionGene(int inNode, int outNode, float weight, boolean expressed, int innovation) {
		this.inNode = inNode;
		this.outNode = outNode;
		this.weight = weight;
		this.expressed = expressed;
		this.innovation = innovation;
	}

	/**
	 * Construct a new connection gene without innovation
	 * 
	 * @param inNode
	 * @param outNode
	 * @param weight
	 * @param expressed
	 */
	public ConnectionGene(int inNode, int outNode, float weight, boolean expressed) {
		this.inNode = inNode;
		this.outNode = outNode;
		this.weight = weight;
		this.expressed = expressed;
		// TODO: default innovation value
	}

	/**
	 * Inherits a connection gene.
	 * 
	 * @param con Connection gene to be copied
	 */
	public ConnectionGene(ConnectionGene con) {
		this.inNode = con.inNode;
		this.outNode = con.outNode;
		this.weight = con.weight;
		this.expressed = con.expressed;
		this.innovation = con.innovation; // innovation is same as map key due to iterative nature of both algorithms
	}

	// utility functions
	public int getInNode() {
		return inNode;
	}

	public int getOutNode() {
		return outNode;
	}

	public float getWeight() {
		return weight;
	}

	public void setWeight(float newWeight) {
		this.weight = newWeight;
	}

	public boolean isExpressed() {
		return expressed;
	}

	public void enable() {
		expressed = true;
	}

	public void disable() {
		expressed = false;
	}

	public int getInnovation() { // bad name. used as counter iterator (getter/setter)
		return innovation;
	}
	
	public void setInnovation(int newInnovation) { // bad name. used as counter iterator (getter/setter)
		innovation = newInnovation;
	}
	

	/**
	 * @return new connectionGene with identical inNode outNode weight expression
	 *         and innovation number.
	 */
	public ConnectionGene copy() {
		return new ConnectionGene(inNode, outNode, weight, expressed, innovation);
	}

}