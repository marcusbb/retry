package ies.retry;

import java.util.Collection;
import java.util.List;
import java.util.Map;


/**
 * Optimistic Retry Manager with cluster state transitions.
 * 
 *  All operations are atomic and ignore client transaction boundaries 
 *  
 *  Q: Do we need to support a suspended state?  Yes - see RetryManagementMBean
 *  
 *  Q: Do we need a shutdown? Or lifecycle hooks?  We'll support a shutdown through
 *  JMX 
 *  
 * @author msimonsen
 *
 */
public interface RetryManager {

	/**
	 * Queue the retry for later, scheduled retry according
	 * to the contract of the {@link RetryConfiguration}.
	 * 
	 * Retry is categorized by type {@link RetryHolder#type}
	 * 
	 * Supporting a "fair share" dequeue policy for now {@link DrainStrategy#UNORDERED}
	 * 
	 * For any retry that exists, the if {@link RetryConfiguration#isListBacked()} is true
	 * then it will be added to a list.
	 * Otherwise a last win approach is in effect. 
	 * 
	 * @param retry
	 */
	public void addRetry(RetryHolder retry) throws NoCallbackException,ConfigException; //throws IOException??
	
	/**
	 * Unlike the add operation above, this has no additive effect.
	 * This puts over-riding any previous retry with the given id.
	 * It will take the first retry as the key for the storage. 
	 * 
	 * @param retryList
	 * @throws NoCallbackException
	 * @throws ConfigException
	 */
	public void putRetry(List<RetryHolder> retryList) throws NoCallbackException,ConfigException;
	
	/**
	 * Gets a retry by id and type 
	 * 
	 * 
	 * 
	 * @param retryId
	 * @return
	 */
	public List<RetryHolder> getRetry(String retryId,String type);
	
	/**
	 * Gets with an optional secondary Index categorization 
	 * 
	 * @param retryId
	 * @param type
	 * @param secondaryIndex
	 * @return
	 */
	public List<RetryHolder> getRetry(String retryId,String type,String secondaryIndex);
	
	
	/**
	 * 
	 * @param retryId
	 * @param type
	 */
	public void removeRetry(String retryId,String type);
	
	
	/**
	 * Get the count of the number of retries by type.
	 * 
	 * The total number of retries in the queue.
	 * 
	 * 
	 * 
	 * @param type
	 * @return
	 */
	public int count(String type);
	
	/**
	 * this is absolutely necessary for anything useful to be made for
	 * retry.
	 * Only one callback listener is supported.
	 * 
	 * If you don't register one, a {@link NoCallbackException} will be raised.
	 * 
	 * It is a good practise that the instance is thread safe. 
	 * Implementations may require it.
	 * 
	 * @param callback
	 * @param type
	 */
	public void registerCallback(RetryCallback callback,String type);
	
	public RetryCallback registeredCallback(String type);
	/**
	 * Remove the above callback.
	 * 
	 * @param callback
	 */
	public void removeCallback(RetryCallback callback,String type);
	
	public RetryState getState(String type);
	
	public Map<String,RetryState> getAllStates();
	
	
	/**
	 * Clients may be want to be aware of changes to the 
	 * global state transitions of retry manager 
	 * 
	 * @param transitionListener
	 */
	public void registerTransitionListener(RetryTransitionListener transitionListener);
	
	public void removeTransitionListener(RetryTransitionListener transitionListener);
	
	/**
	 * Adding this in as a convenience method - as JMX registration may be an
	 * optional operation.
	 * 
	 * TODO: to be removed
	 */
	public void shutdown();
	
	/**
	 * Gets configuration manager.
	 * 
	 * @return
	 */
	public RetryConfigManager getConfigManager();
	
	
	
	/**
	 * Gets a count by the secondary Index.
	 * This will take far longer to execute.
	 * 
	 * @param type
	 * @param secondaryIndex
	 * @return
	 */
	public int countBySecondaryIndex(String type,String secondaryIndex);
	
	/**
	 * Depending on the size of the index this could return a large number 
	 * of results
	 * 
	 * @param type
	 * @param secondaryIndex
	 * @return
	 */
	public Collection<RetryHolder> bySecondaryIndex(String type,String secondaryIndex);
}
