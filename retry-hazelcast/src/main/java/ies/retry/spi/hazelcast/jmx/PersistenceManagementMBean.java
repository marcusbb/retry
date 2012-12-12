package ies.retry.spi.hazelcast.jmx;

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
