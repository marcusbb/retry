package ies.retry.spi.hazelcast.config;

import javax.xml.bind.annotation.XmlRootElement;

import ies.retry.spi.hazelcast.CallbackManager;
import ies.retry.xml.XmlRetryConfig;

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
	
	
	
}
