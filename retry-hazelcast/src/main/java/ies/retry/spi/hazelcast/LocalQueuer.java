package ies.retry.spi.hazelcast;

import ies.retry.RetryHolder;

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
	
	public boolean add(RetryHolder retryHolder);
	
	public int size(String retryType);
	
	public void shutdown();
	
}
