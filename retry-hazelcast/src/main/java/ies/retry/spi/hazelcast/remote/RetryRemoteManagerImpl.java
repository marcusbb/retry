package ies.retry.spi.hazelcast.remote;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import javax.xml.bind.JAXBException;

import com.hazelcast.client.ClientConfig;
import com.hazelcast.client.HazelcastClient;
import com.hazelcast.config.GroupConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.Member;

import ies.retry.ConfigException;
import ies.retry.NoCallbackException;
import ies.retry.Retry;
import ies.retry.RetryCallback;
import ies.retry.RetryConfigManager;
import ies.retry.RetryConfiguration;
import ies.retry.RetryHolder;
import ies.retry.RetryManager;
import ies.retry.RetryState;
import ies.retry.RetryTransitionListener;
import ies.retry.spi.hazelcast.config.HazelcastConfigManager;
import ies.retry.spi.hazelcast.remote.RemoteCallback.DefaultRemoteCallback;
import ies.retry.spi.hazelcast.config.HazelcastXmlConfFactory;
import ies.retry.spi.hazelcast.config.HazelcastXmlConfig;


/**
 * This instance of Retrymanager exists purely as a client reference to the cluster.
 * 
 * For the purposes of callback, 
 * 
 * @author msimonsen
 *
 */
public class RetryRemoteManagerImpl extends Remoteable implements RetryManager {

	/**
	 * client to the target cluster
	 */
	HazelcastClient hzClient = null;
	HazelcastInstance cluster = null;
	
	private Map<String,RetryCallback> callbackMap = new HashMap<String, RetryCallback>();
	
	private ClientConfigManager configMgr = null;
	

		
	RemoteConfigManager configManager = null;

	/**
	 * Discovers client configuration information through 
	 * retry configuration. 
	 */
	public RetryRemoteManagerImpl() {
		
		super(null); //must be set later on.
				
		this.configManager = new RemoteConfigManager();
		
		
		HazelcastClient client = HazelcastClient.newHazelcastClient(((RemoteXmlConfig)configManager.getConfig()).getHzClientConfig());
		
		this.hzClient = client;
		configManager.setHzInstance(hzClient);
		
		addConfigurations();
	}

	
	/**
	 * Reference the callback cluster by name
	 * @param clientInstance - the client to the retry server
	 * @param clusterName - the callback cluster (this cluster)
	 */
	protected void init(HazelcastClient clientInstance,String clusterName) {
		
		hzClient = clientInstance;
		cluster = Hazelcast.getHazelcastInstanceByName(clusterName);
		
		GroupConfig gc = cluster.getConfig().getGroupConfig();
		Set<Member> memberSet = cluster.getCluster().getMembers();
		ClientConfig cc = new ClientConfig();
		cc.setGroupConfig(gc);
		for (Member member:memberSet)
			cc.addInetSocketAddress(member.getInetSocketAddress());
		
		
	}
	
	
		

	
	public RetryRemoteManagerImpl(HazelcastInstance clientInstance) {
		super(clientInstance);
		
		this.configManager = new RemoteConfigManager(clientInstance);
	}
	
	/**
	 * Add configurations from local config only if it doesn't exist on 
	 * the server
	 */
	private void addConfigurations() {
		
		Map<String,RetryConfiguration> configs = configManager.getConfigMapLocal();
		for (RetryConfiguration config:configs.values()) {
			if (configManager.getConfiguration(config.getType()) == null) {
				configManager.addConfiguration(config);
			} else {
				//TODO log something
			}
			
		}
	}
	
	@Override
	protected <T> RemoteRPC<T> rpcClass(String method,Object...signature) {
		
		return new RemoteManagerRPC<>(method, signature);
	}
	

	public boolean onCallback(RetryHolder holder) throws Exception {
		
		RetryCallback localCallback = callbackMap.get(holder.getType());
		
		if (localCallback == null) {
			throw new NoCallbackException();
		}
		try {
			return localCallback.onEvent(holder);
		}catch (Exception e) {
			//TODO 
		}
		return false;
	}

	@Override
	public void addRetry(RetryHolder retry) throws NoCallbackException,
			ConfigException {
		
		submitRPC("addRetry", retry);
	
	}

	@Override
	public void archiveRetry(RetryHolder retry) throws NoCallbackException,
			ConfigException {
		submitRPC("archiveRetry", retry);
		
	}

	@Override
	public void putRetry(List<RetryHolder> retryList)
			throws NoCallbackException, ConfigException {
		submitRPC("putRetry", retryList);
		
	}

	@Override
	public List<RetryHolder> getRetry(String retryId, String type) {
		return submitRPC("getRetry", retryId,type);
	}

	@Override
	public List<RetryHolder> getRetry(String retryId, String type,
			String secondaryIndex) {
		
		return submitRPC("getRetry", retryId,type,secondaryIndex);
		
	}

	@Override
	public void removeRetry(String retryId, String type) {
		
		submitRPC("getRetry", retryId,type);
		
	}

	@Override
	public int count(String type) {
		
		return submitRPC("count", type);
	}

	@Override
	public boolean exists(String retryId, String type) {
		return submitRPC("exists", retryId, type);
	}

	@Override
	public void registerCallback(RetryCallback callback, String type) {
		callbackMap.put(type, callback);
		//TODO come up with client config of this 
		DefaultRemoteCallback remoteCallback = new DefaultRemoteCallback(null, callback);
		submitRPC("registerRemoteCallback", remoteCallback);
	}

	@Override
	public RetryCallback registeredCallback(String type) {
		throw new UnsupportedOperationException("");
	}

	@Override
	public void removeCallback(RetryCallback callback, String type) {
		throw new UnsupportedOperationException("");
		
	}

	@Override
	public RetryState getState(String type) {
		return submitRPC("getState", type);
	}

	@Override
	public Map<String, RetryState> getAllStates() {
		return submitRPC("getStateAllStates");
	}

	@Override
	public void registerTransitionListener(
			RetryTransitionListener transitionListener) {
		throw new UnsupportedOperationException("");
		
	}

	@Override
	public void removeTransitionListener(
			RetryTransitionListener transitionListener) {
		throw new UnsupportedOperationException("");
		
	}

	@Override
	public void shutdown() {
		hzClient.getLifecycleService().shutdown();
		
	}

	@Override
	public RetryConfigManager getConfigManager() {
		return configManager;
	}

	@Override
	public int countBySecondaryIndex(String type, String secondaryIndex) {
		return submitRPC("countBySecondaryIndex",type,secondaryIndex);
	}

	@Override
	public Collection<RetryHolder> bySecondaryIndex(String type,
			String secondaryIndex) {
		return submitRPC("bySecondaryIndex",type,secondaryIndex);
	}

}
