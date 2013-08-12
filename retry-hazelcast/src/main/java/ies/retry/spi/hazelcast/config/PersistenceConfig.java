package ies.retry.spi.hazelcast.config;

import java.io.Serializable;

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
	
	private long timeoutInms = 10 * 1000;
	
	private boolean pagedLoading = true;
	
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

	@Override
	public String toString() {
		return "PersistenceConfig [ON=" + ON + ", writeSync=" + writeSync
				+ ", jpaPU=" + jpaPU + ", loadFetchSize=" + loadFetchSize
				+ ", queuePolicy=" + queuePolicy + ", maxPoolSize="
				+ maxPoolSize + ", coreSize=" + coreSize
				+ ", boundedQueueSize=" + boundedQueueSize + ", timeoutInms="
				+ timeoutInms + ", pagedLoading=" + pagedLoading + "]";
	}

	
		
	
	
}
