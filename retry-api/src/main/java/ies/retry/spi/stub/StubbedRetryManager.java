package ies.retry.spi.stub;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import ies.retry.NoCallbackException;
import ies.retry.ConfigException;
import ies.retry.RetryCallback;
import ies.retry.RetryConfigManager;
import ies.retry.RetryConfiguration;
import ies.retry.RetryHolder;
import ies.retry.RetryManager;
import ies.retry.RetryState;
import ies.retry.RetryTransitionListener;

/**
 * 
 * A place holder, empty implementation.
 *
 */
public class StubbedRetryManager implements RetryManager {

	
	public void addRetry(RetryHolder retry) throws NoCallbackException,
			ConfigException {
		// TODO Auto-generated method stub
		
	}
	/*
	 * Created to support immediate archiving of passed retry object without de-queueing  
	 */
	public void archiveRetry(RetryHolder retry) throws NoCallbackException,ConfigException{
		
	}
	
	

	
	public void removeRetry(String retryId, String type) {
		// TODO Auto-generated method stub
		
	}

	

	
	public void putRetry(List<RetryHolder> retryList)
			throws NoCallbackException, ConfigException {
		// TODO Auto-generated method stub
		
	}


	public List<RetryHolder> getListRetry(String retryId, String type) {
		// TODO Auto-generated method stub
		return null;
	}


	public List<RetryHolder> getRetry(String retryId, String type) {
		// TODO Auto-generated method stub
		return null;
	}

	
	public int count(String type) {
		// TODO Auto-generated method stub
		return 0;
	}

	
	public void registerCallback(RetryCallback callback, String type) {
		// TODO Auto-generated method stub
		
	}

	
	public void registerCallback(Class<? extends RetryCallback> callbackClass) {
		// TODO Auto-generated method stub
		
	}


	public void removeCallback(RetryCallback callback,String type) {
		// TODO Auto-generated method stub
		
	}
	
	
	public RetryCallback registeredCallback(String type) {
		// TODO Auto-generated method stub
		return null;
	}


	public void registerTransitionListener(
			RetryTransitionListener transitionListener) {
		// TODO Auto-generated method stub
		
	}

	
	public void removeTransitionListener(
			RetryTransitionListener transitionListener) {
		// TODO Auto-generated method stub
		
	}

	
	public Iterator<String> getRetries(String type) {
		// TODO Auto-generated method stub
		return null;
	}

	
	public RetryConfigManager getConfigManager() {
		return new StubbedConfigManager();
	}


	public void shutdown() {
		// TODO Auto-generated method stub
		
	}


	public List<RetryHolder> getRetry(String retryId, String type,
			String secondaryIndex) {
		// TODO Auto-generated method stub
		return null;
	}


	public int countBySecondaryIndex(String type, String secondaryIndex) {
		// TODO Auto-generated method stub
		return 0;
	}


	public Collection<RetryHolder> bySecondaryIndex(String type,
			String secondaryIndex) {
		// TODO Auto-generated method stub
		return null;
	}


	public RetryState getState(String type) {
		// TODO Auto-generated method stub
		return null;
	}


	public Map<String, RetryState> getAllStates() {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public boolean exists(String retryId, String type) {
		// TODO Auto-generated method stub
		return false;
	}
	@Override
	public String getId() {
		return "";
	}

	
	
}
class StubbedConfigManager implements RetryConfigManager {

	
	
	
	public RetryConfiguration getConfiguration(String type) {
		// TODO Auto-generated method stub
		return null;
	}


	public void addConfiguration(RetryConfiguration config) {
		// TODO Auto-generated method stub
		
	}

	
	public Collection<RetryConfiguration> getAll() {
		// TODO Auto-generated method stub
		return null;
	}


	public Map<String, RetryConfiguration> getConfigMap() {
		// TODO Auto-generated method stub
		return null;
	}


	public RetryConfiguration cloneConfiguration(String type) {
		// TODO Auto-generated method stub
		return null;
	}
	
	
}
