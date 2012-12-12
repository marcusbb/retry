package ies.retry.spi.hazelcast.disttasks;

import java.io.Serializable;

public class TryDequeueEvent implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3199549389618989249L;
	
	private String type;

	public TryDequeueEvent(String type) {
		
		this.type = type;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}
	
	
	
}
