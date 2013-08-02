package ies.retry.spi.hazelcast.disttasks;

import ies.retry.Retry;
import ies.retry.RetryConfiguration;
import ies.retry.RetryHolder;
import ies.retry.spi.hazelcast.HazelcastRetryImpl;
import ies.retry.spi.hazelcast.StateManager;
import ies.retry.spi.hazelcast.StateManager.LoadingState;
import ies.retry.spi.hazelcast.config.HazelcastConfigManager;
import ies.retry.spi.hazelcast.config.HazelcastXmlConfig;
import ies.retry.spi.hazelcast.persistence.DBMergePolicy;
import ies.retry.spi.hazelcast.persistence.RetryMapStoreFactory;
import ies.retry.spi.hazelcast.util.RetryUtil;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import provision.services.logging.Logger;

import com.hazelcast.core.HazelcastInstance;
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

	//private RetryConfiguration config;
	private long backOffInterval;
	
	public AddRetryCallable() {}
	
	public AddRetryCallable(RetryHolder holder,RetryConfiguration config) {
		//this.config = config;
		this.retry = holder;
		this.appendList = config.isListBacked();
		//this.nextTs = System.currentTimeMillis() + config.getBackOff().getMilliInterval();
		this.backOffInterval = config.getBackOff().getInterval();
	}
	
	public AddRetryCallable(List<RetryHolder> listHolder,RetryConfiguration config) {
		//this.config = config;
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
		IMap<String,List<RetryHolder>> distMap = null;
		boolean lockAquired = false;
		try {
			if (retryList != null)
				return callListPut();
			

			
			HazelcastConfigManager configMgr = (HazelcastConfigManager)((HazelcastRetryImpl)Retry.getRetryManager()).getConfigManager();
			
			RetryConfiguration curConfig = configMgr.getConfiguration(retry.getType());
			HazelcastInstance h1 = ((HazelcastRetryImpl)Retry.getRetryManager()).getH1();
			
			distMap = h1.getMap(retry.getType());
			//distMap.lock(retry.getId());
			lockAquired = distMap.tryLock(retry.getId(), configMgr.getHzConfig().getRetryAddLockTimeout(), TimeUnit.MILLISECONDS);
			if ( ! lockAquired ) {
				Logger.warn(CALLER, "Add_Retry_Task_Call_LockTimeout","Lock timeout","retry",retry);
				throw new RuntimeException("Unable to Aquire Lock: " + retry.toString());
			}
			long curTs = System.currentTimeMillis();
			retry.setSystemTs(curTs);
			retry.setNextAttempt(curTs + backOffInterval);
					
			List<RetryHolder> listHolder = distMap.get(retry.getId());
			if (listHolder == null) {
				listHolder = new ArrayList<RetryHolder>();
			}
			if (appendList || listHolder.size()==0)
				listHolder.add(retry);
			else {
				listHolder.set(0, retry);
			}

			 //  apply max list size policy
			int maxListSize = curConfig.getMaxListSize(); // max allowed number of items in the list
			while(maxListSize<listHolder.size()){
				RetryHolder retryHolder = listHolder.remove(0);
				if(curConfig.isArchiveExpired()){
					List<RetryHolder> list = new ArrayList<RetryHolder>();
					list.add(retryHolder);
					RetryMapStoreFactory.getInstance().newMapStore(retry.getType()).archive(list, false);
				}
				Logger.warn(CALLER, "Add_Retry_Task_Max_List_Size_Reached", "Retry holder was removed: " + retryHolder.getId(), "Type",	retryHolder.getType());
			}
			
			distMap.put(retry.getId(), listHolder);
			
			DBMergePolicy mergePolicy = null;
		
			if(!RetryUtil.hasLoaded(retry.getType()))
				mergePolicy = DBMergePolicy.ORDER_TS_DISCARD_DUP_TS;
			else if(listHolder.size() > 1)
				mergePolicy = DBMergePolicy.FIND_OVERWRITE;
			else
				mergePolicy = DBMergePolicy.OVERWRITE;

			if (persist)
				RetryMapStoreFactory.getInstance().newMapStore(retry.getType()).store(listHolder, mergePolicy);
			
		}catch (Exception e) {
			Logger.error(CALLER, "Add_Retry_Task_Call_Exception", "Exception Message: " + e.getMessage(), e);
		}finally {
			if (distMap != null && retry != null && lockAquired)
				distMap.unlock(retry.getId());
		}
		return null;
	}

	public Void callListPut() throws Exception {
		
		RetryHolder retry = retryList.get(0);
		HazelcastInstance h1 = ((HazelcastRetryImpl)Retry.getRetryManager()).getH1();
		IMap<String,List<RetryHolder>> distMap = h1.getMap(retry.getType());
		try {
			distMap.lock(retry.getId());
			long nextTs = System.currentTimeMillis() + backOffInterval;

			//sync nextTs date
			for (RetryHolder holder : retryList) {
				holder.setNextAttempt(nextTs); // we reset timestamp for all events loaded from database
			}
					
			List<RetryHolder> inMemoryList = distMap.get(retry.getId());
			int hzSize = retryList.size();
			if (inMemoryList != null) { // Merge two lists (from DB and from HZ)
				retryList = RetryUtil.merge(inMemoryList, retryList);
				Logger.info(CALLER, "Add_Retry_Task_Call_callListPut", "Merged data from DB and HZ", "Type", retry.getType(), "Id", retry.getId(), 
						"InMemory", inMemoryList.size(), "HZ", hzSize, "Result", retryList!=null ? retryList.size() : 0);
			}
					
			distMap.put(retry.getId(), retryList);
			
			
			if (persist)
				RetryMapStoreFactory.getInstance().newMapStore(retry.getType()).store(retryList, DBMergePolicy.OVERWRITE);
		}finally {
			if (distMap != null && retry != null)
				distMap.unlock(retry.getId());
		}
		
		
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
