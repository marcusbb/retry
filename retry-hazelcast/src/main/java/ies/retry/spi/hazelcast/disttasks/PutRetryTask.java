package ies.retry.spi.hazelcast.disttasks;

import ies.retry.Retry;
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
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;


import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.nio.DataSerializable;

public class PutRetryTask implements Callable<Void>, DataSerializable {

	private static org.slf4j.Logger logger =  org.slf4j.LoggerFactory.getLogger(PutRetryTask.class);
	
	private static final long serialVersionUID = -2695422922759193498L;
	
	private List<RetryHolder> retryList = null;
	private HzSerializableRetryHolder serializableHolder = null;
	private boolean persist = true;
	public PutRetryTask() {}
	
	//ready for over the wire
	public PutRetryTask(List<RetryHolder> retryList,boolean persist) {
		this.serializableHolder = new HzSerializableRetryHolder(retryList, new KryoSerializer());
		this.retryList = retryList;
		this.persist = persist;
	}
	public PutRetryTask(HzSerializableRetryHolder holder,boolean persist) {
		this.serializableHolder = holder;
		this.retryList = holder.getHolderList();
		this.persist = persist;
	}
	@Override
	public void writeData(DataOutput out) throws IOException {
		
		this.serializableHolder.writeData(out);
		out.writeBoolean(persist);
	}

	@Override
	public void readData(DataInput in) throws IOException {
		
		this.serializableHolder = new HzSerializableRetryHolder();
		this.serializableHolder.readData(in);
		this.retryList = this.serializableHolder.getHolderList();
		persist = in.readBoolean();
		
	}

	@Override
	public Void call() throws Exception {
		RetryHolder retry = retryList.get(0);
		HazelcastInstance h1 = ((HazelcastRetryImpl)Retry.getRetryManager()).getH1();
		IMap<String,List<RetryHolder>> distMap = h1.getMap(retry.getType());
		boolean lockAquired = false;
		try {
			HazelcastConfigManager configMgr = (HazelcastConfigManager)((HazelcastRetryImpl)Retry.getRetryManager()).getConfigManager();
			long backOffInterval = configMgr.getConfiguration(retry.getType()).getBackOff().getInterval();
			lockAquired = distMap.tryLock(retry.getId(), configMgr.getRetryHzConfig().getRetryAddLockTimeout(), TimeUnit.MILLISECONDS);
			if ( ! lockAquired ) {
				logger.warn( "Add_Retry_Task_Call_LockTimeout: retry={}",retry);
				throw new RuntimeException("Unable to Aquire Lock: " + retry.toString());
			}
			long nextTs = System.currentTimeMillis() + backOffInterval;

			//sync nextTs date
			for (RetryHolder holder : retryList) {
				holder.setNextAttempt(nextTs); // we reset timestamp for all events loaded from database
			}
					
			List<RetryHolder> inMemoryList = distMap.get(retry.getId());
			int hzSize = retryList.size();
			if (inMemoryList != null) { // Merge two lists (from DB and from HZ)
				retryList = RetryUtil.merge(inMemoryList, retryList);
				logger.info( "Add_Retry_Task_Call_callListPut: type={},id={},InMemory={},hzSize={},result={}" , retry.getType(), retry.getId(), 
						 inMemoryList.size(),  hzSize,  retryList!=null ? retryList.size() : 0);
			}
					
			distMap.put(retry.getId(), new HzSerializableRetryHolder(retryList, new KryoSerializer()));
			
			
			if (persist)
				RetryMapStoreFactory.getInstance().newMapStore(retry.getType()).store(retryList, DBMergePolicy.OVERWRITE);
		}finally {
			if (distMap != null && retry != null && lockAquired)
				distMap.unlock(retry.getId());
		}
		
		
		return null;
	}

}
