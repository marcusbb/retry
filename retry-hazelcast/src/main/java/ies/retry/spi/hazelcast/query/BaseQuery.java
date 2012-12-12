package ies.retry.spi.hazelcast.query;

import java.io.Serializable;

/**
 * An interface for now.
 * This may turn out to be an abstract class.
 * 
 * @author msimonsen
 *
 */
public interface BaseQuery extends Serializable {

	

	public abstract QueryResults eval();
	
	public String getMapName();
	
	public Integer maxResultPerNode();
}
