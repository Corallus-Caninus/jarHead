package jarhead;

import java.io.*;

/**
 * Simple counter class for iterating innovation numbers. Not necessary for Node
 * id(verifiable in k.stanely). May be redundant given key values in hashmap data struct are innovation numbers.
 */
public class Counter implements Serializable {
	private static final long serialVersionUID = 159348938L;
	private int currentInnovation = 0;

	public int getInnovation() {
		return currentInnovation++;
	}
}
