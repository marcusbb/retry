package ies.retry.spi.hazelcast.jmx;

import java.util.Date;

import ies.retry.spi.hazelcast.HazelcastRetryImpl;
import ies.retry.spi.hazelcast.RetryStat;
import ies.retry.spi.hazelcast.RetryStats;


/**
 * Only for standard mbean compliance.
 * @author msimonsen
 *
 */
public interface RetryManagementMBean extends ies.retry.jmx.RetryManagementMBean{

	/**
	 * operation that allow initialization from client perspective
	 * 
	 * @param coordinator
	 */
	public void init(HazelcastRetryImpl coordinator);
	
	/**
	 * relies on {@link RetryStat#toString()}
	 * 
	 * @param type
	 * @return
	 */
	public String statByType(String type);
	
	/**
	 * String version - relies on {@link RetryStats#toString()} 
	 * @return
	 */
	public String getAllStats();
	
	
	public Date earliestByType(String type);
	
	public Long[][] getFailuresByType(String type);
	
	/**
	 * Management operation to force a dequeue event for all types
	 */
	public void tryDequeue();
	
	/**
	 * Management operation to force a dequeue event for type
	 * @param type
	 */
	public void tryDequeue(String type);
	
	/**
	 * if a callback is registered for this type
	 * 
	 * @param type
	 * @return
	 */
	public boolean callBackRegistered(String type);
	
	/**
	 * Returns the types of retry that have callbacks registered
	 * @return
	 */
	public String[] getCallbacksRegistered();
	
	/**
	 * Get the current current dequeue block size (number of concurrent callbacks) 
	 * @param type
	 * @return
	 */
	public int getDequeueBlockSize(String type);
}
