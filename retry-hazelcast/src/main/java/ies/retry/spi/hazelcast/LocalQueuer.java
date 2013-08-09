package ies.retry.spi.hazelcast;

import ies.retry.RetryHolder;

/**
 * A LocalQueuer that dispatches to HZ map. 
 * 
 * @author msimonsen
 *
 */
public interface LocalQueuer {

	/**
	 * Is queue buffer empty
	 * 
	 * @param retryType
	 * @return
	 */
	public boolean isEmpty(String retryType);
	
	/**
	 * Return true if was queued due to current local queue buffer non-empty.
	 * 
	 * @param retryHolder
	 * @return
	 */
	public boolean addIfNotEmpty(RetryHolder retryHolder);
	/**
	 * current implementation blocks when reached the largest queue size
	 * @param retryHolder
	 * @return
	 */
	public boolean add(RetryHolder retryHolder);
	
	public int size(String retryType);
	
	public void shutdown();
	
}
