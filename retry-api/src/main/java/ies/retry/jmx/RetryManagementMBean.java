package ies.retry.jmx;

import ies.retry.RetryManager;

public interface RetryManagementMBean {

	public void shutdown();
	
	public String[] getRetryTypes();
	/**
	 * Suspends all.
	 * 
	 * Allows adds to {@link RetryManager#addRetry(ies.retry.RetryHolder)}
	 */
	public void suspend() throws IllegalStateException;
	
	
	public void suspend(String type)throws IllegalStateException;
	
	public void resume(String type)throws IllegalStateException;
	
	public void resume()throws IllegalStateException;
	
	/*
	 * Operations related to reloading configuration.
	 * 
	 */
	public String getConfig();
	
	public void reloadConfig();
	/**
	 * This likely will not have the implication of restart
	 * 
	 * @param xml
	 */
	public void loadConfig(String xml);
	
	
	
	//MONITORING
	
	public Long getGridCount();
	
	
	public Long getGridCount(String type);
	
	
	/**
	 * {@link RetryTransitionEvent.Type}
	 * 
	 * @param type
	 * @return
	 */
	public String getState(String type);
}
