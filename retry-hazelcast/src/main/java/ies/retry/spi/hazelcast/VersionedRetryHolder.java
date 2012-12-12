package ies.retry.spi.hazelcast;

import ies.retry.RetryHolder;

import java.io.Serializable;
import java.util.List;

/**
 * Wrap all retry in this structure.
 * 
 * THIS IS CURRENTLY NOT USED, BUT SHOULD BE USED INSTEAD OF 
 * USING List<RetryHolder>
 * 
 * @author msimonsen
 *
 */
public class VersionedRetryHolder implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1786798062485437377L;

	/**
	 * this is a storage version (could be a string too)
	 */
	private int version;
	
	private List<RetryHolder> listHolder;

	private String secondaryIndex;
	
	public VersionedRetryHolder() {}
	
	public VersionedRetryHolder(int version,List<RetryHolder> listHolder) {
		this.version = version;
		this.listHolder = listHolder;
	}
	
	public int getVersion() {
		return version;
	}

	public void setVersion(int version) {
		this.version = version;
	}

	public List<RetryHolder> getListHolder() {
		return listHolder;
	}

	public void setListHolder(List<RetryHolder> listHolder) {
		this.listHolder = listHolder;
	}

	public String getSecondaryIndex() {
		return secondaryIndex;
	}

	public void setSecondaryIndex(String secondaryIndex) {
		this.secondaryIndex = secondaryIndex;
	}
	
	
	
}
