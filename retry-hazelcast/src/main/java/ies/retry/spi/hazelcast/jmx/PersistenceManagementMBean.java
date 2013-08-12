package ies.retry.spi.hazelcast.jmx;

/**
 * This isn't used at the moment, but should split persistent monitor ops
 * from {@link RetryManagementMBean}
 * 
 *
 */
public interface PersistenceManagementMBean {


	/**
	 * Orderly shutdown of write behind process - if
	 * configured for write behind
	 * @param graceful
	 */
	public void shutDownLocal(boolean graceful);
	
	
	public int localpersistQueueSize();
	
	/**
	 * Gets the entire clustered persistence queue sizes
	 * @return
	 */
	public int clusterPersistQueueSize();
	
}
