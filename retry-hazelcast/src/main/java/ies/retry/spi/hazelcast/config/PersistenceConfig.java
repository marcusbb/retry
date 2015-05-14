package ies.retry.spi.hazelcast.config;

import java.io.Serializable;

import driver.em.CassConfig;
import reader.ReaderConfig;

public class PersistenceConfig implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 728933236148227227L;
	
	private boolean ON = false;	
	private boolean writeSync = true;
	private String jpaPU;
	
	private int loadFetchSize = 1000;
	
	private ThreadQueuePolicy queuePolicy = ThreadQueuePolicy.LINKED;
	
	private int maxPoolSize = 50;
	private int coreSize = 50;
	private int boundedQueueSize = Integer.MAX_VALUE;
	//when the bounded queue size gets to this size drop 
	//tasks on the floor.
	private int dropOnQueueSize = 10000;
	
	private long timeoutInms = 5 * 1000;
	
	private boolean pagedLoading = true;
	
	//Cassandra batch loading configuration
	private ReaderConfig cqlReaderConfig;
	
	//Cassandra run time connection options
	//it could borrow from above ReaderConfig
	private CassConfig cassConfig;
	
	public enum ThreadQueuePolicy {
		SYNC,ARRAY,LINKED;
	}

	public boolean isON() {
		return ON;
	}

	public void setON(boolean oN) {
		ON = oN;
	}

	public boolean isWriteSync() {
		return writeSync;
	}

	public void setWriteSync(boolean writeSync) {
		this.writeSync = writeSync;
	}

	

	public String getJpaPU() {
		return jpaPU;
	}

	public void setJpaPU(String jpaPU) {
		this.jpaPU = jpaPU;
	}
	
	public int getLoadFetchSize() {
		return loadFetchSize;
	}

	public void setLoadFetchSize(int loadFetchSize) {
		this.loadFetchSize = loadFetchSize;
	}

	public ThreadQueuePolicy getQueuePolicy() {
		return queuePolicy;
	}

	public void setQueuePolicy(ThreadQueuePolicy queuePolicy) {
		this.queuePolicy = queuePolicy;
	}

	public int getMaxPoolSize() {
		return maxPoolSize;
	}

	public void setMaxPoolSize(int maxPoolSize) {
		this.maxPoolSize = maxPoolSize;
	}

	public int getCoreSize() {
		return coreSize;
	}

	public void setCoreSize(int coreSize) {
		this.coreSize = coreSize;
	}

	public int getBoundedQueueSize() {
		return boundedQueueSize;
	}

	public void setBoundedQueueSize(int boundedQueueSize) {
		this.boundedQueueSize = boundedQueueSize;
	}

	
	public boolean isPagedLoading() {
		return pagedLoading;
	}

	public void setPagedLoading(boolean pagedLoading) {
		this.pagedLoading = pagedLoading;
	}

	
	public long getTimeoutInms() {
		return timeoutInms;
	}

	public void setTimeoutInms(long timeoutInms) {
		this.timeoutInms = timeoutInms;
	}

	
	public int getDropOnQueueSize() {
		return dropOnQueueSize;
	}

	public void setDropOnQueueSize(int dropOnQueueSize) {
		this.dropOnQueueSize = dropOnQueueSize;
	}

	
	public ReaderConfig getCqlReaderConfig() {
		return cqlReaderConfig;
	}

	public void setCqlReaderConfig(ReaderConfig cqlReaderConfig) {
		this.cqlReaderConfig = cqlReaderConfig;
	}

	public CassConfig getCassConfig() {
		return cassConfig;
	}

	public void setCassConfig(CassConfig cassConfig) {
		this.cassConfig = cassConfig;
	}

	public boolean isCassandra() {
		if (cassConfig != null && cqlReaderConfig != null)
			return true;
		return false;
					
	}
	@Override
	public String toString() {
		return "PersistenceConfig [ON=" + ON + ", writeSync=" + writeSync
				+ ", jpaPU=" + jpaPU + ", loadFetchSize=" + loadFetchSize
				+ ", queuePolicy=" + queuePolicy + ", maxPoolSize="
				+ maxPoolSize + ", coreSize=" + coreSize
				+ ", boundedQueueSize=" + boundedQueueSize
				+ ", dropOnQueueSize=" + dropOnQueueSize + ", timeoutInms="
				+ timeoutInms + ", pagedLoading=" + pagedLoading
				+ ", cqlReaderConfig=" + cqlReaderConfig + ", cassConfig="
				+ cassConfig + "]";
	}

	

			
	
	
}
