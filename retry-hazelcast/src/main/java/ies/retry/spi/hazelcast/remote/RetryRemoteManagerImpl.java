package ies.retry.spi.hazelcast.remote;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import com.hazelcast.client.ClientConfig;
import com.hazelcast.core.HazelcastInstance;

import ies.retry.ConfigException;
import ies.retry.NoCallbackException;
import ies.retry.RetryCallback;
import ies.retry.RetryConfigManager;
import ies.retry.RetryHolder;
import ies.retry.RetryManager;
import ies.retry.RetryState;
import ies.retry.RetryTransitionListener;

/**
 * Does everything a local RetryManager would do but with a remote hazelcast instance/cluster.
 * 
 * @author msimonsen
 *
 */
public class RetryRemoteManagerImpl implements RetryManager {

	HazelcastInstance hzClient = null;
	
	/**
	 * Discovers client configuration information through 
	 * retry configuration. 
	 */
	public RetryRemoteManagerImpl() {
		//builds a client config from file - meaning a list of hz IPs.
		//this could be bundled as part of retry (client)
		
	}
	public RetryRemoteManagerImpl(HazelcastInstance clientInstance) {
		hzClient = clientInstance;
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
		if (! (callback instanceof RemoteCallback) )
			throw new UnsupportedOperationException("");
		
		RemoteCallback rcb = (RemoteCallback)callback;
		//this will register into the CallbackManager directly or through the server RPC
		
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
