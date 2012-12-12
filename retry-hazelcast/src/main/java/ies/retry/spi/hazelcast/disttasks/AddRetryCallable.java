package ies.retry.spi.hazelcast.disttasks;

import ies.retry.RetryConfiguration;
import ies.retry.RetryHolder;
import ies.retry.spi.hazelcast.HazelcastRetryImpl;
import ies.retry.spi.hazelcast.StateManager;
import ies.retry.spi.hazelcast.StateManager.LoadingState;
import ies.retry.spi.hazelcast.persistence.DBMergePolicy;
import ies.retry.spi.hazelcast.persistence.RetryMapStoreFactory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import provision.services.logging.Logger;

import com.hazelcast.core.IMap;

/**
 * An optimized retry add operation.
 * 
 * @author msimonsen
 *
 */
public class AddRetryCallable implements Callable<Void>,Serializable {
	private static final String CALLER = AddRetryCallable.class.getName(); 
	private static final long serialVersionUID = -5057315181594224793L;
	
	private RetryHolder retry = null;
	private List<RetryHolder> retryList = null;
	private boolean appendList = true;
	//private long nextTs = 0;
	private boolean persist = true;
	private boolean merge;
	//private RetryConfiguration config;
	private long backOffInterval;
	
	public AddRetryCallable() {}
	
	public AddRetryCallable(RetryHolder holder,RetryConfiguration config) {
		this.retry = holder;
		this.appendList = config.isListBacked();
		//this.nextTs = System.currentTimeMillis() + config.getBackOff().getMilliInterval();
		this.backOffInterval = config.getBackOff().getInterval();
	}
	
	public AddRetryCallable(List<RetryHolder> listHolder,RetryConfiguration config) {
		this.retryList = listHolder;
		this.appendList = config.isListBacked();
		//this.nextTs = System.currentTimeMillis() + config.getBackOff().getMilliInterval();
		this.backOffInterval = config.getBackOff().getInterval();
	}
	public AddRetryCallable(List<RetryHolder> listHolder,RetryConfiguration config,boolean persist) {
		this(listHolder,config);
		this.persist = persist;
	}
	
	public Void call() throws Exception {
		try {
			if (retryList != null)
				return callListPut();
			
			IMap<String,List<RetryHolder>> distMap = HazelcastRetryImpl.getHzInst().getMap(retry.getType());
			distMap.lock(retry.getId());
			
			long curTs = System.currentTimeMillis();
			retry.setSystemTs(curTs);
			long nextTs = curTs + backOffInterval;
					
			List<RetryHolder> listHolder = distMap.get(retry.getId());
			if (listHolder == null) {
				listHolder = new ArrayList<RetryHolder>();
			}
			if (appendList)
				listHolder.add(retry);
			else {
				listHolder.set(0, retry);
			}
			//sync all counts and nextTs date
			for (RetryHolder holder:listHolder) {
				holder.setCount(0);
				holder.setNextAttempt(nextTs);
			}
			distMap.put(retry.getId(), listHolder);
			
			DBMergePolicy mergePolicy = null;
			IMap<String, LoadingState> loadStateMap = HazelcastRetryImpl.getHzInst().getMap(StateManager.DB_LOADING_STATE);
			
			if( (loadStateMap == null) || (loadStateMap.get(retry.getType()) == LoadingState.LOADING) )
				mergePolicy = DBMergePolicy.ORDER_TS_DISCARD_DUP_TS;
			else if(listHolder.size() > 1)
				mergePolicy = DBMergePolicy.FIND_OVERWRITE;
			else
				mergePolicy = DBMergePolicy.OVERWRITE;

			if (persist)
				RetryMapStoreFactory.getInstance().newMapStore(retry.getType()).store(listHolder, mergePolicy);
			
			distMap.unlock(retry.getId());
			/*for (RetryHolder rh:listHolder) {
				Logger.debug(CALLER, "Add_Task: " + rh);
			}*/
		}catch (Exception e) {
			Logger.error(CALLER, "Add_Retry_Task_Call_Exception", "Exception Message: " + e.getMessage(), e);
		}
		return null;
	}

	public Void callListPut() throws Exception {
		
		RetryHolder retry = retryList.get(0);
		IMap<String,List<RetryHolder>> distMap = HazelcastRetryImpl.getHzInst().getMap(retry.getType());
		distMap.lock(retry.getId());
		long curTs = System.currentTimeMillis();
		long nextTs = curTs + backOffInterval;
		//sync all counts and nextTs date
				for (RetryHolder holder:retryList) {
					holder.setSystemTs(curTs);
					holder.setCount(0);
					holder.setNextAttempt(nextTs);
				}
		distMap.put(retry.getId(),retryList);
		
		if (persist)
			RetryMapStoreFactory.getInstance().newMapStore(retry.getType()).store(retryList, DBMergePolicy.OVERWRITE);
		distMap.unlock(retry.getId());
		
		return null;
	}
	
	public String getPartitionKey() {
		return retry.getId();
	}

	public boolean isPersist() {
		return persist;
	}

	public void setPersist(boolean persist) {
		this.persist = persist;
	}
	
	
}
