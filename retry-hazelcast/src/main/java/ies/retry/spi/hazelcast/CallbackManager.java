package ies.retry.spi.hazelcast;

import ies.retry.BackOff;
import ies.retry.BatchConfig;
import ies.retry.Retry;
import ies.retry.RetryCallback;
import ies.retry.RetryConfigManager;
import ies.retry.RetryConfiguration;
import ies.retry.RetryHolder;
import ies.retry.RetryState;
import ies.retry.spi.hazelcast.config.HazelcastConfigManager;
import ies.retry.spi.hazelcast.config.HazelcastXmlConfig;
import ies.retry.spi.hazelcast.disttasks.CallbackRegistration;
import ies.retry.spi.hazelcast.disttasks.CallbackSelectionTask;
import ies.retry.spi.hazelcast.disttasks.DistCallBackTask;
import ies.retry.spi.hazelcast.util.RetryUtil;

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
import java.util.concurrent.FutureTask;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.ReentrantLock;

import provision.services.logging.Logger;

import com.hazelcast.client.ClientConfig;
import com.hazelcast.client.HazelcastClient;
import com.hazelcast.core.DistributedTask;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.core.Member;
import com.hazelcast.core.MultiTask;

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
	static String CALLER = CallbackManager.class.getName();
	
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
	
	/*
	 * All information related to member distribution
	 * is a possible points to refactor 
	 */
	private volatile int memberCursor = 0;
	private Member distMember = null;
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
		
		//
		distMember = h1.getCluster().getLocalMember();
		distCallBackExec = h1.getExecutorService(EXEC_SRV_NAME);
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
		Logger.info(CALLER, "Add_Callback", "putting call back availability to: " + h1.getCluster().getLocalMember(), "Type", retryType);
		
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
		Logger.debug(CALLER, "Schedule_Next_Run", "Scheduled Next Run " + retryType + " in " + batchConfig.getBatchHeartBeat() + " ms");
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
				Logger.error(CALLER, "tryLock","msg",e.getMessage(),e);
				return false;
			}
			
		//}
	}
	private void releaseLock(String type) {
		ReentrantLock lock = typeLockMap.get(type);
		Logger.debug(CALLER, "Release_Lock", "Unlocking");
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
			Logger.debug(CALLER, "Try_Dequeue_Try_Locked","Type",type,"Locked",locked);
			if (!locked) {
				Logger.warn(CALLER, "Try_Dequeue_Lock_Unavailable","","Type",type);
				return false;
			}
			
			
			IMap<String,List<RetryHolder>>  retryMap = h1.getMap(type);
			RetryStat stat = stats.getAllStats().get(type);
			
						
			
			if (retryMap.localKeySet().size() <1) {
				Logger.debug(CALLER, "Try_Dequeue_zero_size");
				return false;
			}
			Iterator<String> keyIter = retryMap.localKeySet().iterator();
			
			Member execMember = pickMember(type);
			

			Logger.info(CALLER, "Try_Dequeue", "Dequeueing local set size: " + retryMap.localKeySet().size() + ". BLOCK size: " + getBatchSize(type), "Type", type);
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
					task = new DistributedTask<CallbackStat>(callbackTask, execMember);
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
							Logger.warn(CALLER, "Try_Dequeue_NoCallbackRegistered", "Exception Message: " + e.getMessage(), "Type", type);
							//next round will pick another member
						} else {
							Logger.info(CALLER, "Try_Dequeue_ExecutionException", "Exception Message: " + e.getCause().getMessage(), "Type", type);
						}
					}catch(TimeoutException te) {
						Logger.warn(CALLER, "Try_Dequeue_TimeoutException", "msg: " + te.getMessage(), "Type", type,"id",fw.getId());
						ret = false;
						fw.getFutureTask().cancel(true);
						callBackTimeOut(fw.getType(), fw.getId());
					}
					catch (Exception e) {
						Logger.warn(CALLER, "Try_Dequeue_Exception", "Exception Message: " + e.getMessage(), "Type", type);
						
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
					Logger.info(CALLER, "Try_Dequeue_Suspended", "Suspended, no dequeuing for type.", "Type", type);
					break;
				}
				
			}
			Logger.info(CALLER, "Try_Dequeue_Iteration_Completed", "Completed iteration.", "Type", type, "SUCCESS", successCount, "FAILED", failCount,"CUR_BATCH_SIZE",getBatchSize(type));			
			
			
			if (isDrained(type)) {
				Logger.info(CALLER, "Try_Dequeue_Queue_Drained", "Retry is drained from memory", "Type", type);
				
				/*boolean allDrained = stateMgr.isStorageDrained(type);
				if (allDrained)
					Logger.info(CALLER, "Try_Dequeue_Queue_Drained", "Retry is completely drained", "Type", type);
				*/
				//Reset the stats
				stat.resetFailed();
			} 
		
		}catch (NoCallbackMember e) {
			Logger.warn(CALLER, "Try_Dequeue_NoCallbackMember", "No member to call back registered anywhere in the grid", "Type", type);
			
			return false;
		}
		catch (Throwable t) {
			
			Logger.error(CALLER, "Try_Dequeue_Throwable", "Exception Message: " + t.getMessage(), "Type", type, t);
			
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
	
	
	private Member pickLocalMember() {
		return h1.getCluster().getLocalMember();
	}
	
	private Member pickMember(String type) throws NoCallbackMember {
		HazelcastXmlConfig config = configMgr.getRetryHzConfig();
		if (config.isPickLocalCallback())
			return pickLocalMember();
		//if a local registered listener is available, callbackMap will 
		//populated
		Logger.debug(CALLER, "Pick_Member", "Picking exection member for type.", "Type", type);
		
		if (callbackMap.get(type) != null) {
			Logger.debug(CALLER, "Pick_Member", "Picked local member", "Type", type);
			return pickLocalMember();
		//
		//	else synchronous RPC type task is 
		}else {
			Logger.info(CALLER, "Pick_Member", "Picking another member to execute callback ", "Type", type);			
			MultiTask<CallbackRegistration> task = new MultiTask<CallbackRegistration>(new CallbackSelectionTask(type), h1.getCluster().getMembers());
			distCallBackExec.submit(task);
			try {
				Iterator<CallbackRegistration> iter = task.get().iterator();
				ArrayList<Member> callbackMembers = new ArrayList<Member>();
				//pick first one: this won't be optimal
				while(iter.hasNext()) {
					CallbackRegistration registation = iter.next();
					if (registation.isRegistered()) {
						callbackMembers.add( registation.getMember() );
						
					}
				}
				if (callbackMembers.size() == 0)
					throw new NoCallbackMember();
				distMember = callbackMembers.get(memberCursor); 
				memberCursor = memberCursor++ %callbackMembers.size();
				
					
			}catch (Exception e) {
				//unable to choose member
				Logger.error(CALLER, "Pick_Member_Exception", "Exception Message: " + e.getMessage(), "Type", type, e);
				distMember = h1.getCluster().getLocalMember();
			}
		}
		return distMember;
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
