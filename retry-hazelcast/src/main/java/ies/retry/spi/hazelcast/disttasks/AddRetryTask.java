package ies.retry.spi.hazelcast.disttasks;

import ies.retry.Retry;
import ies.retry.RetryConfiguration;
import ies.retry.RetryHolder;
import ies.retry.spi.hazelcast.HazelcastRetryImpl;
import ies.retry.spi.hazelcast.HzSerializableRetryHolder;
import ies.retry.spi.hazelcast.config.HazelcastConfigManager;
import ies.retry.spi.hazelcast.persistence.DBMergePolicy;
import ies.retry.spi.hazelcast.persistence.RetryMapStoreFactory;
import ies.retry.spi.hazelcast.util.KryoSerializer;
import ies.retry.spi.hazelcast.util.RetryUtil;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import provision.services.logging.Logger;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.nio.DataSerializable;

/**
 * 
 * 
 *
 */
public class AddRetryTask implements Callable<Void>, DataSerializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2759049204058496674L;
	private static String CALLER = AddRetryTask.class.getName();
	private HzSerializableRetryHolder serialiableHolder = null;
	private RetryHolder retry = null;
	private boolean persist = true;
	
	public AddRetryTask() {
		
	}
	//Constructor for prep over the wire
	public AddRetryTask(RetryHolder holder) {
		this.serialiableHolder = new HzSerializableRetryHolder(holder, new KryoSerializer());
	}
	public AddRetryTask(RetryHolder holder,boolean persist) {
		this.serialiableHolder = new HzSerializableRetryHolder(holder, new KryoSerializer());
		this.persist = persist;
	}
	
	@Override
	public void writeData(DataOutput out) throws IOException {
		this.serialiableHolder.writeData(out);
		
	}

	@Override
	public void readData(DataInput in) throws IOException {
		this.serialiableHolder = new HzSerializableRetryHolder();
		this.serialiableHolder.readData(in);
		this.retry = serialiableHolder.getHolderList().get(0);
		
	}

	@Override
	public Void call() throws Exception {
		
		IMap<String,List<RetryHolder>> distMap = null;
		boolean lockAquired = false;
		try {
			
						
			HazelcastConfigManager configMgr = (HazelcastConfigManager)((HazelcastRetryImpl)Retry.getRetryManager()).getConfigManager();
			
			RetryConfiguration curConfig = configMgr.getConfiguration(retry.getType());
			HazelcastInstance h1 = ((HazelcastRetryImpl)Retry.getRetryManager()).getH1();
			long backOffInterval = curConfig.getBackOff().getInterval();
			boolean appendList = curConfig.isListBacked();
			distMap = h1.getMap(retry.getType());
			//distMap.lock(retry.getId());
			lockAquired = distMap.tryLock(retry.getId(), configMgr.getRetryHzConfig().getRetryAddLockTimeout(), TimeUnit.MILLISECONDS);
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
			
			distMap.put(retry.getId(), serialiableHolder);
			
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

	
}
