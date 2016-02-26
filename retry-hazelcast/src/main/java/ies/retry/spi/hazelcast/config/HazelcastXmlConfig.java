package ies.retry.spi.hazelcast.config;

import ies.retry.spi.hazelcast.CallbackManager;
import ies.retry.xml.XmlRetryConfig;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="retry")
public class HazelcastXmlConfig extends XmlRetryConfig {

	/**
	 * 
	 */
	private static final long serialVersionUID = -6421588048535064147L;

	
	private PersistenceConfig persistenceConfig = new PersistenceConfig();

	private NetworkMergePolicy mergePolicy = NetworkMergePolicy.DB_OVERWRITE;
	
	private HealthCheckConfig healthCheckConfig = new HealthCheckConfig();
	
	/**
	 * this is to remove the feature to auto-magically pick another calling member
	 * of the cluster see {@link CallbackManager} for details
	 */
	private boolean pickLocalCallback = true;
	
	private PubConfig pubConfig = new PubConfig();
	
	private long queueCheckPeriod = 10 * 1000;
	/**
	 * The time to wait for a lock to be tried on adding retry to the grid (and storage)
	 */
	private long retryAddLockTimeout = 10 * 1000;
	
	private int defaultLocalQueueSize = 64000;
	
	private boolean throwOnAddException = true;
	
	private String localQueueLogDir = "/var/log/retry";
			
	
	public PersistenceConfig getPersistenceConfig() {
		return persistenceConfig;
	}


	public void setPersistenceConfig(PersistenceConfig persistenceConfig) {
		this.persistenceConfig = persistenceConfig;
	}


	public NetworkMergePolicy getMergePolicy() {
		return mergePolicy;
	}


	public void setMergePolicy(NetworkMergePolicy mergePolicy) {
		this.mergePolicy = mergePolicy;
	}


	public HealthCheckConfig getHealthCheckConfig() {
		return healthCheckConfig;
	}


	public void setHealthCheckConfig(HealthCheckConfig healthCheckConfig) {
		this.healthCheckConfig = healthCheckConfig;
	}


	public boolean isPickLocalCallback() {
		return pickLocalCallback;
	}


	public void setPickLocalCallback(boolean pickLocalCallback) {
		this.pickLocalCallback = pickLocalCallback;
	}


	public PubConfig getPubConfig() {
		return pubConfig;
	}


	public void setPubConfig(PubConfig pubConfig) {
		this.pubConfig = pubConfig;
	}


	public long getQueueCheckPeriod() {
		return queueCheckPeriod;
	}


	public void setQueueCheckPeriod(long queueCheckPeriod) {
		this.queueCheckPeriod = queueCheckPeriod;
	}


	public long getRetryAddLockTimeout() {
		return retryAddLockTimeout;
	}


	public void setRetryAddLockTimeout(long retryAddLockTimeout) {
		this.retryAddLockTimeout = retryAddLockTimeout;
	}


	public int getDefaultLocalQueueSize() {
		return defaultLocalQueueSize;
	}


	public void setDefaultLocalQueueSize(int defaultLocalQueueSize) {
		this.defaultLocalQueueSize = defaultLocalQueueSize;
	}


	public boolean isThrowOnAddException() {
		return throwOnAddException;
	}


	public void setThrowOnAddException(boolean throwOnAddException) {
		this.throwOnAddException = throwOnAddException;
	}


	public String getLocalQueueLogDir() {
		return localQueueLogDir;
	}


	public void setLocalQueueLogDir(String localQueueLogDir) {
		this.localQueueLogDir = localQueueLogDir;
	}
	
	
	
}
