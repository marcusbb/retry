package ies.retry.spi.hazelcast;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;

import com.hazelcast.client.ClientConfig;
import com.hazelcast.client.HazelcastClient;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;

import ies.retry.BackOff;
import ies.retry.BatchConfig;
import ies.retry.Retry;
import ies.retry.RetryCallback;
import ies.retry.RetryConfigManager;
import ies.retry.RetryConfiguration;
import ies.retry.RetryHolder;
import ies.retry.RetryState;
import ies.retry.spi.hazelcast.config.HazelcastConfigManager;
import ies.retry.spi.hazelcast.disttasks.DistCallBackTask;
import ies.retry.spi.hazelcast.util.RetryUtil;



/**
 * Manages the scheduler and the callbacks.
 * 
 * call back logic in {@link #tryDequeue(String)} method
 * 
 * Moving to independent scheduler from state manager {@link StateManager}
 * transitions 
 * 
 * @author msimonsen
 *
 */
public class CallbackManager  {

	//static Logger logger = Logger.getLogger(CallbackManager.class.getName());
	private static Logger logger = org.slf4j.LoggerFactory.getLogger(CallbackManager.class);
	
	private HazelcastConfigManager configMgr;
	
	private Map<String,RetryCallback> callbackMap;
	
	private Map<String, ReentrantLock> typeLockMap;
	
	/**
	 * This indicates the current batch size.
	 */
	private Map<String, Integer> batchSizeMap = new HashMap<String, Integer>();
	
	//possible point of refactor
	private StateManager stateMgr;
	//Scheduler scheduler;
	//local timer instance
	private ScheduledThreadPoolExecutor stpe = null;
	//ExecutorService localCallbackExec = null; 
	
	public static String EXEC_SRV_NAME = "RETRY_DEQUEUE";
	private ExecutorService distCallBackExec = null;
	
	
	private HazelcastInstance h1;
	
	RetryStats stats;
	
	public CallbackManager(HazelcastConfigManager configMgr,StateManager stateMgr,RetryStats stats,HazelcastInstance h1)   {
		this.h1 = h1;
		callbackMap = new Hashtable<String, RetryCallback>();
		this.configMgr = configMgr;
		
		//only ever need 1 thread
		stpe = new ScheduledThreadPoolExecutor(1);
		typeLockMap = new Hashtable<String, ReentrantLock>();
		this.stateMgr = stateMgr;
		
		
		//distCallBackExec = h1.getExecutorService(EXEC_SRV_NAME);
		//TODO: possibly convert this to a thread pool 
		//The throttling mechanism determined by the the batch size
		//should control the maximum bounds of threads
		distCallBackExec = Executors.newCachedThreadPool();
		this.stats = stats;
				
		
	}
	
	public void init() {
		for (String retryType: configMgr.getConfigMap().keySet()) {
			scheduleNextRun(retryType);
		}
	}
	public void init(String retryType) {
		scheduleNextRun(retryType);
	}
	/**
	 * shutdown 
	 */
	public void shutdown() {
		if (stpe != null)
			stpe.shutdownNow();
		
	}
	/**
	 * 
	 * @param callback
	 */
	public void addCallback(RetryCallback callback,String retryType) {
		logger.info( "Add_Callback: putting call back availability to: {}, Type={}" + h1.getCluster().getLocalMember(), retryType);
		
		callbackMap.put(retryType, callback);
		
				
	}
		
	public void registerRemoteClient(ClientConfig clientconfig) {
		HazelcastClient client = HazelcastClient.newHazelcastClient(clientconfig);
		
	}
	
	/**
	 * 
	 * @param callback
	 */
	public void removeCallback(RetryCallback callback,String retryType) {
		callbackMap.remove(callback);
	}
	
	
	
	
	
	
	private void scheduleNextRun(String retryType) {
		BatchConfig batchConfig = configMgr.getConfiguration(retryType).getBatchConfig();
		Runnable r = new CallbackScheduleTask(this,retryType);
		stpe.schedule(r, 
				batchConfig.getBatchHeartBeat(), 
				TimeUnit.MILLISECONDS);
		//Include interval multiplier here as well.
		logger.debug("Schedule_Next_Run: Scheduled Next Run {} in {} ms ",  retryType , batchConfig.getBatchHeartBeat());
	}
	/**
	 * Try to dequeue for all - it's called from {@link CallbackManager}
	 * and may require feedback.
	 * 
	 */
	protected void tryDequeue() {
		Collection<RetryConfiguration> col = configMgr.getAll();
		Iterator<RetryConfiguration> typeIter = col.iterator();
		while (typeIter.hasNext()) {
			tryDequeue(typeIter.next().getType());
		}
	}
	/**
	 * Now that de-queuing is allowed from external interfaces,
	 * we must guard that we're not doing the double loop.
	 * 
	 * the synchronization is heavy handed, but we dont
	 * have heavy throughput on this method.
	 * 
	 * @param type
	 * @return
	 */
	private synchronized boolean tryLock(String type) {
		//synchronized(CallbackManager.class) {
		//Logger.debug(CALLER, "callermanager obj: " + this );
			
			ReentrantLock lock = typeLockMap.get(type);
			if ( lock ==null) {
				lock = new ReentrantLock();
				typeLockMap.put(type,lock);
			}try {
				return lock.tryLock(0, TimeUnit.SECONDS);
			}catch (InterruptedException e) {
				logger.error(e.getMessage(),e);
				return false;
			}
			
		//}
	}
	private void releaseLock(String type) {
		ReentrantLock lock = typeLockMap.get(type);
		logger.debug("Release_Lock Unlocking type {}", type);
		if (lock != null && lock.isLocked())
			lock.unlock();
	}
	protected void callBackTimeOut(String type, String id) {
		IMap<String,List<RetryHolder>>  retryMap = h1.getMap(type);
		boolean lockAquired = retryMap.tryLock(id,configMgr.getRetryHzConfig().getRetryAddLockTimeout(),TimeUnit.MILLISECONDS);
		BackOff backOff = configMgr.getConfiguration(type).getBackOff();
		try {
			long ts = System.currentTimeMillis();
			List<RetryHolder> listHolder = retryMap.get(id);
			for (RetryHolder fh:listHolder) {
				
				long nextDelay = RetryUtil.getNextDelayForRetry(backOff, fh.getCount());																
				
				fh.setNextAttempt(ts + nextDelay);
				fh.incrementCount();									
			}
			retryMap.put(id, listHolder);
		}finally {
			if (lockAquired)
				retryMap.unlock(id);
		}
		
		
	}
	/**
	 * Completely in-memory retrieval. 
	 *  The meat of the call back logic resides here.
	 *  
	 *  Will swallow all exceptions as to not delay processing.
	 *  
	 *  TODO: in future, abstract this class
	 * @param type
	 * @return an indication that dequeuing did happen or that lock
	 * acquisition failed
	 * 
	 */
	
	public boolean tryDequeue(String type) {
		
		boolean dequeued = true;
		
		boolean locked =false;
		try {
			//is released in finally
			locked = tryLock(type);
			logger.debug("Try_Dequeue_Try_Locked: Type={}, Locked={}",type,locked);
			if (!locked) {
				logger.warn("Try_Dequeue_Lock_Unavailable: Type={}",type);
				return false;
			}
			
			
			IMap<String,List<RetryHolder>>  retryMap = h1.getMap(type);
			RetryStat stat = stats.getAllStats().get(type);
			
						
			
			if (retryMap.localKeySet().size() <1) {
				logger.debug( "Try_Dequeue_zero_size");
				return false;
			}
			Iterator<String> keyIter = retryMap.localKeySet().iterator();
			
			//Member execMember = pickMember(type);
			

			logger.info("Try_Dequeue_local: set size={}, block_size={}, type={}" + retryMap.localKeySet().size(), getBatchSize(type), type);
			long successCount = 0;
			long failCount = 0;
			while(keyIter.hasNext()) {
				Integer batchSize = getBatchSize(type);
				ArrayList<FutureTaskWrapper> futureList = new ArrayList<FutureTaskWrapper>(batchSize);
								
				
				for (int i=0;keyIter.hasNext()&& i<batchSize;i++ ) {
					
					String id = keyIter.next();
					List<RetryHolder> listHolder = retryMap.get(id);
					
					//Can happen if removed from map while reading
					if (listHolder == null)
						continue;
					//bail if we are too early
					long nextTs = listHolder.get(0).getNextAttempt();
					if ( nextTs > System.currentTimeMillis())
						continue;
						
					//otherwise process
					FutureTask<CallbackStat> task = null;
					
					Callable<CallbackStat> callbackTask = new DistCallBackTask(listHolder, isArchiveExpired(type));			
					task = new FutureTask<CallbackStat>(callbackTask);
					distCallBackExec.submit(task);
					futureList.add(new FutureTaskWrapper(task,type,id));
					
					
				}
				int successForBatch = 0;
				for (FutureTaskWrapper fw:futureList) {
					boolean ret = false;
					CallbackStat cbs = null;
					try {
						//set timeout to the batch size heart-beat
						//does this make sense?
						long timeout = configMgr.getConfiguration(type).getCallbackTimeoutMs();
						cbs = fw.getFutureTask().get(timeout,TimeUnit.MILLISECONDS);
						ret = cbs.isSuccess();
					}catch (ExecutionException e) {
						if (e.getCause() instanceof NoCallbackRegistered){
							logger.warn("Try_Dequeue_NoCallbackRegistered: msg={}, Type={}", e.getMessage(), type);
							//next round will pick another member
						} else {
							logger.info( "Try_Dequeue_ExecutionException: msg={}, Type={}", e.getCause().getMessage(), type);
						}
					}catch(TimeoutException te) {
						logger.warn("Try_Dequeue_TimeoutException: msg={}, Type={}, id={}",  te.getMessage(),  type, fw.getId());
						ret = false;
						fw.getFutureTask().cancel(true);
						callBackTimeOut(fw.getType(), fw.getId());
					}
					catch (Exception e) {
						logger.warn("Try_Dequeue_Exception: msg={}, Type={}",  e.getMessage(),  type,e);
						
					}
					if (ret) {
						successCount++;successForBatch++;
						stat.getTotalSuccess().incrementAndGet();			
						
					}else {
						failCount++;successForBatch--;
						stat.incrementFailed(cbs.getCount());
						stat.setIfEarlier(cbs.getDateCreated());
						
					}
					setBatchSize(type,successForBatch);
				}
				if (stateMgr.getState(type) == RetryState.SUSPENDED) {
					logger.info( "Try_Dequeue_Suspended: Type={}", type);
					break;
				}
				
			}
			logger.info("Try_Dequeue_Iteration_Completed: Type={}, SUCCESS={}, FAILED={}, CUR_BATCH_SIZE={}",  type, successCount,  failCount, getBatchSize(type));			
			
			
			if (isDrained(type)) {
				logger.info( "Try_Dequeue_Queue_Drained: Type={}", type);
				
				/*boolean allDrained = stateMgr.isStorageDrained(type);
				if (allDrained)
					Logger.info(CALLER, "Try_Dequeue_Queue_Drained", "Retry is completely drained", "Type", type);
				*/
				//Reset the stats
				stat.resetFailed();
			} 
		
		}
		catch (Throwable t) {
			
			logger.error( "Try_Dequeue_Throwable: ",  t);
			
		} finally {
			if (locked) 
				releaseLock(type);
			
			scheduleNextRun(type);
			
		}
		return dequeued;
		
	}
		
	private void setBatchSize(String type,int success) {
		int curSize = getBatchSize(type);
		RetryConfiguration retryConfig = configMgr.getConfiguration(type);
		curSize+=success;
		if (curSize < retryConfig.getBatchConfig().getMinBatchSize())
			curSize = retryConfig.getBatchConfig().getMinBatchSize();
		else if (curSize > retryConfig.getBatchConfig().getBatchSize())
			curSize = retryConfig.getBatchConfig().getBatchSize();
			
		batchSizeMap.put(type, curSize);
		//else batch size remains
	}

	private boolean isArchiveExpired(String type) {
		RetryConfiguration retryConfig = configMgr.getConfiguration(type);
		boolean archiveExpired = retryConfig.isArchiveExpired();
		return archiveExpired;
	}
	
	public int getBatchSize(String type) {
		RetryConfiguration retryConfig = configMgr.getConfiguration(type);
		Integer batchSize = batchSizeMap.get(type);
		if (batchSize == null) {
			batchSize =	retryConfig.getBatchConfig().getBatchSize();		
			batchSizeMap.put(type,batchSize);
		}
		return batchSize;
	}
	
	
	
	/**
	 * cluster wide isDrained function.
	 * 
	 * @param type
	 * @return
	 */
	protected boolean isDrained(String type) {
		IMap<String,List<RetryHolder>>  retryMap = ((HazelcastRetryImpl)Retry.getRetryManager()).getH1().getMap(type);
		//evaluate both the local size and the cluster size
		return (retryMap.localKeySet().size() == 0 && retryMap.size() ==0);
		
		
	}
	public RetryConfigManager getConfigMgr() {
		return configMgr;
	}
	public void setConfigMgr(HazelcastConfigManager configMgr) {
		this.configMgr = configMgr;
	}
	public Map<String, RetryCallback> getCallbackMap() {
		return callbackMap;
	}
	public void setCallbackMap(Map<String, RetryCallback> callbackMap) {
		this.callbackMap = callbackMap;
	}

	public HazelcastInstance getH1() {
		return h1;
	}

	public void setH1(HazelcastInstance h1) {
		this.h1 = h1;
	}
	
	
	
	
	
}
/**
 * 
 * Wrapping future task with additional
 *
 */
class FutureTaskWrapper {
	private FutureTask<CallbackStat> futureTask;
	private String type;
	private String id;
	
	public FutureTaskWrapper(FutureTask<CallbackStat> futureTask,String type,String id) {
		this.futureTask = futureTask;
		this.type = type;
		this.id = id;
	}

	public FutureTask<CallbackStat> getFutureTask() {
		return futureTask;
	}

	public String getType() {
		return type;
	}

	public String getId() {
		return id;
	}
	
}
class NoCallbackMember extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5776742729455339360L;
	
}
