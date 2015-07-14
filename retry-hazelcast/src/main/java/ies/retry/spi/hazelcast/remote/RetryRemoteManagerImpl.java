package ies.retry.spi.hazelcast.remote;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import provision.services.logging.Logger;

import com.hazelcast.client.ClientConfig;
import com.hazelcast.client.HazelcastClient;
import com.hazelcast.config.GroupConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.Member;

import ies.retry.ConfigException;
import ies.retry.NoCallbackException;
import ies.retry.RetryCallback;
import ies.retry.RetryConfigManager;
import ies.retry.RetryHolder;
import ies.retry.RetryManager;
import ies.retry.RetryState;
import ies.retry.RetryTransitionListener;
import ies.retry.spi.hazelcast.config.HazelcastConfigManager;
import ies.retry.spi.hazelcast.remote.RemoteCallback.DefaultRemoteCallback;

/**
 * This instance of Retrymanager exists purely as a client reference to the cluster.
 * 
 * For the purposes of callback, 
 * 
 * @author msimonsen
 *
 */
public class RetryRemoteManagerImpl implements RetryManager {

	/**
	 * client to the target cluster
	 */
	HazelcastClient hzClient = null;
	HazelcastInstance cluster = null;
	
	private Map<String,RetryCallback> callbackMap = new HashMap<String, RetryCallback>();
	
	private ClientConfigManager configMgr = null;
	
	/**
	 * Discovers client configuration information through 
	 * retry configuration. 
	 */
	public RetryRemoteManagerImpl() {
		//builds a client config from file - meaning a list of hz IPs.
		//this could be bundled as part of retry (client)
		configMgr = new ClientConfigManager();
		try {
			configMgr.load();
			
			HazelcastClient inst = HazelcastClient.newHazelcastClient(configMgr.getConfig().getRemoteClusterConfig().getClientConfig());
			init(inst,configMgr.getConfig().getClusterName());
		} catch (Exception e) {
			throw new ConfigException(e);
		} 
		
	}
	public RetryRemoteManagerImpl(HazelcastClient clientInstance,String clusterName) {
		init(clientInstance, clusterName);
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
	
	
	private <T> T submitRPC(String method,Object...signature) {
		try {
			return (T)hzClient.getExecutorService().submit(new RemoteRPCTask<T>(method, signature )).get();
		}catch (ExecutionException | InterruptedException e) {
			//TODO - wrap in a suitable runtime exception
			throw new RuntimeException(e.getMessage(),e);
		}finally {
		
		}
		
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
		throw new UnsupportedOperationException("");
		
	}

	@Override
	public RetryConfigManager getConfigManager() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int countBySecondaryIndex(String type, String secondaryIndex) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public Collection<RetryHolder> bySecondaryIndex(String type,
			String secondaryIndex) {
		// TODO Auto-generated method stub
		return null;
	}

}
