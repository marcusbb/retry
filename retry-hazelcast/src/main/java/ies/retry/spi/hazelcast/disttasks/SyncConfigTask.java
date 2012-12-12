package ies.retry.spi.hazelcast.disttasks;

import ies.retry.Retry;
import ies.retry.RetryConfiguration;
import ies.retry.RetryState;
import ies.retry.spi.hazelcast.HazelcastRetryImpl;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * An rpc type mechanism to synchronize with master.
 * 
 * Called by a joining member to the master.
 * 
 * @author msimonsen
 *
 */
public class SyncConfigTask implements Callable<Map<String,RetryConfiguration>>,Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3290991539893947840L;
	//private static HazelcastRetryImpl retryImpl  =(HazelcastRetryImpl) Retry.getRetryManager();
	
	public SyncConfigTask() {
		 
	}
	@Override
	public Map<String,RetryConfiguration> call() throws Exception {
		HazelcastRetryImpl retryImpl = (HazelcastRetryImpl) Retry.getRetryManager();
		return retryImpl.getConfigManager().getConfigMap();
	}

	
	
	

}
