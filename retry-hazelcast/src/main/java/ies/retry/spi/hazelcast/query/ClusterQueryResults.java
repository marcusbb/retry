package ies.retry.spi.hazelcast.query;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;

public class ClusterQueryResults implements Serializable  {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1827102007799784142L;
	public Collection<QueryResults> queryResults;

	public ClusterQueryResults() {
		this.queryResults = new ArrayList<QueryResults>();
	}
	public Collection<QueryResults> getQueryResults() {
		return queryResults;
	}

	public void setQueryResults(Collection<QueryResults> queryResults) {
		this.queryResults = queryResults;
	}
	
	
}
