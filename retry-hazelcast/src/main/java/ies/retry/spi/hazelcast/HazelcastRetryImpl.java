package ies.retry.spi.hazelcast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.persistence.PersistenceException;

import org.slf4j.Logger;

import com.hazelcast.core.DistributedTask;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.query.EntryObject;
import com.hazelcast.query.Predicate;
import com.hazelcast.query.PredicateBuilder;

import ies.retry.ConfigException;
import ies.retry.NoCallbackException;
import ies.retry.RetryCallback;
import ies.retry.RetryConfigManager;
import ies.retry.RetryConfiguration;
import ies.retry.RetryHolder;
import ies.retry.RetryManager;
import ies.retry.RetryState;
import ies.retry.RetryTransitionListener;
import ies.retry.spi.hazelcast.config.HazelcastConfigManager;
import ies.retry.spi.hazelcast.config.HazelcastXmlConfFactory;
import ies.retry.spi.hazelcast.config.HazelcastXmlConfig;
import ies.retry.spi.hazelcast.disttasks.AddRetryTask;
import ies.retry.spi.hazelcast.disttasks.PutRetryTask;
import ies.retry.spi.hazelcast.persistence.RetryMapStore;
import ies.retry.spi.hazelcast.persistence.RetryMapStoreFactory;
import ies.retry.spi.hazelcast.remote.RemoteCallback;
import ies.retry.spi.hazelcast.util.HzUtil;
import ies.retry.spi.hazelcast.util.KryoSerializer;
import ies.retry.spi.hazelcast.util.RetryUtil;

/**
 * The implementor for {@link RetryManager}.
 * 
 *  A co-ordination point for 
 *  1. Statemanagement - to understand cluster states
 *  2. Callback management - the manage client call backs
 *  3. Hazelcast 
 *  
 *  The initialization is in the constructor, which implies a singleton
 *  type pattern - which it is, since one of its critical members 
 *  the hazelcast instance is also a singleton. 
 *  
 * @author msimonsen
 *
 */
public class HazelcastRetryImpl implements RetryManager {

	//static logger.logger.= logger.getlogger.HazelcastRetryImpl.class.getName()); 
	private static org.slf4j.Logger logger =  org.slf4j.LoggerFactory.getLogger(HazelcastRetryImpl.class);
	
	private final static StackTraceElement [] EMPTY_STACK_TRACE = new StackTraceElement [0];
	
	protected HazelcastInstance h1 = null;
	public static String EXEC_SRV_NAME = "RETRY_ADD";
	protected HazelcastConfigManager configMgr;
	
		
	protected CallbackManager callbackManager;
	
	protected CallbackRemoteProxy callRemotebackProxy;
	
	protected StateManager stateMgr;
	
	protected RetryStats stats;
		
	protected LocalQueuer localQueuer;
	
	HzStateMachine hzStateMachine;
	
	private String id;
	
	public HazelcastRetryImpl() throws ConfigException {
		
		
		
		//Unavoidable double loading - if we're providing customized behavior
		HazelcastConfigManager xmlconfigMgr = new HazelcastConfigManager(this);
		xmlconfigMgr.setFactory(new HazelcastXmlConfFactory());
		xmlconfigMgr.setJaxbConfigClass(HazelcastXmlConfig.class);
		try {
			xmlconfigMgr.load();
			logger.info( "Retry config: config={}" + xmlconfigMgr.marshallXML());
		} catch (Exception e) {
			throw new ConfigException(e);
		} 
		
		this.configMgr = xmlconfigMgr;
		
				
		h1 = HzUtil.loadHzConfiguration();
		id = h1.getName();
		
		logger.info( "Initializing_Persistence");
		RetryMapStoreFactory.getInstance().init(configMgr.getRetryHzConfig());

		configMgr.addListener(RetryMapStoreFactory.getInstance() );
		logger.info( "Initializing_State_Callback");
		//Stats might need to be augmented by state manager as well.
		stats = initStats();
		initIndexes();
		stateMgr = new StateManager(configMgr,stats,h1);
		callbackManager = new CallbackManager(configMgr,stateMgr,stats,h1);
		callbackManager.init();

		//possibly load data from DB
		stateMgr.init();
		
		localQueuer = new LocalQueuerImpl(h1, xmlconfigMgr);
		
		hzStateMachine = new HzStateMachine(null, this, -1);
				
	}
	public HazelcastInstance getHzInst() {
		return h1;
	}
	
	
	/**
	 * Initialization of stats.
	 * Done at start-up or during dynamic configuration addition.
	 * @return
	 */
	public RetryStats initStats() {
		RetryStats stats = new RetryStats();
		for (RetryConfiguration config:configMgr.getAll()) {
			RetryStat stat = new RetryStat(config.getType(),config.getBackOff().getMaxAttempts());
			stats.put(config.getType(), stat);	
		}
		return stats;
	}
	public void initStat(RetryConfiguration config) {
		RetryStat stat = new RetryStat(config.getType(),config.getBackOff().getMaxAttempts());
		stats.put(config.getType(), stat);	
	}
	
	
	public void initIndexes() {
		//TODO
	}
	@Override
	public void shutdown() {
		
		if (callbackManager != null)
			callbackManager.shutdown();
		
		if (stateMgr != null)
			stateMgr.shutdown();
		RetryMapStoreFactory.getInstance().shutdown();
		
		if (h1!=null) {
			h1.getLifecycleService().shutdown();
		}
	}
	/**
	 * 
	 * 
	 * The point of entry for "local" based processing, where callbacks
	 * are coresident on the same JVM as Hz/Retry
	 * 
	 */
	public void addRetry(RetryHolder retry) throws NoCallbackException,
			ConfigException {
		
		addRetry(new HzSerializableRetryHolder(retry, new KryoSerializer(),false));
		
		
	}
	/**
	 * this is intended to be called by clients, that choose deferred serialization
	 * @param serializable
	 */
	public void addRetry(HzSerializableRetryHolder serializable) {
		if (serializable.getHolderList() == null)
			throw new IllegalArgumentException("list must contain one retry element");
		if (serializable.getHolderList().get(0) == null)
			throw new IllegalArgumentException("list must contain one retry element");
		
		RetryHolder retry = serializable.getHolderList().get(0);
		
		if (configMgr.getConfiguration(retry.getType()) == null) {
			throw new ConfigException("No configuration set for type: " + retry.getType());
		}
		//logger.debug( "HZ INST: " + this);
		logger.info( "Add_Retry: ID={}, Type={}",  retry.getId(),  retry.getType() );
					
		
		RetryConfiguration config = configMgr.getConfiguration(retry.getType());
		
		if (null != retry.getException()) truncateStackTrace(retry, config);
		
		if (retry.getRetryData() != null && serializable.isDeferPayloadSerialization()) {
			retry.setPayload(new KryoSerializer().serializeToByte(retry.getRetryData()));
		}
				
				
		//queue locally if we have a local queue buffer
		if (localQueuer.addIfNotEmpty(retry)) {
			
			return;
		}
		try {
			//inform state manager
			stateMgr.retryAddedEvent(retry.getType(),true);
			
			// first determine the partition key, add to owning member
			DistributedTask<Void> distTask = new DistributedTask<Void>(new AddRetryTask(serializable), retry.getId());
			
			h1.getExecutorService(EXEC_SRV_NAME).submit(distTask);
			
			dealSync(distTask,config);
		} 
		
		//Storage Exceptions are only propagated AFTER
		//HZ update has been propagated - hence no need to store to local queue
		//However we will have a delta between storage and HZ
		catch (StoreTimeoutException e) {
			logger.error( "Add_Retry_StorageTimeout_Exception: msg={}, ID={}, Type={}",  e.getMessage(),  retry.getId(),  e);
			
		} catch (PersistenceException e) {
			logger.error( "Add_Retry_Persistence_Exception: msg={}, ID={}, Type={}",  e.getMessage(),  retry.getId(),  e);
		} 
		
		catch (Exception e) {
			//Unable to add retry
			logger.error( "Add_Retry_Exception:  msg={}, ID={}, Type={}",  e.getMessage(),  retry.getId());
			if (configMgr.getRetryHzConfig().isThrowOnAddException())
				throw new RuntimeException("Configured to Throw exception, deal with it");
			else {
				//Add to local queue for later consumption
				localQueuer.add(retry);
			}
		}
		
	}
	private void dealSync(DistributedTask<Void> distTask,RetryConfiguration config) throws ExecutionException, InterruptedException, TimeoutException {
		try {
			//right now its a global configuration, TODO: change by type
			if (config.isSyncRetryAdd()) {
				long timeout = configMgr.getRetryHzConfig().getRetryAddLockTimeout();
				distTask.get(timeout,TimeUnit.MILLISECONDS);
							
			}
		}catch (TimeoutException e) {
			distTask.cancel(true);
			throw e;
		}
	}
	
	/*
	 * Created to support immediate archiving of retry object without de-queueing  
	 */
	public void archiveRetry(RetryHolder retry) throws NoCallbackException,ConfigException{

		if (configMgr.getConfiguration(retry.getType()) == null) {
			throw new ConfigException("No configuration set for type: " + retry.getType());
		}
		 
		logger.info( "Archive_Retry_No_Dequeue: ID={}, Type={}",  retry.getId(), retry.getType());
		retry.setPayload(configMgr.getConfiguration(retry.getType()).getSerializer().serializeToByte(retry.getRetryData()));
		RetryMapStore store = (RetryMapStore)RetryMapStoreFactory.getInstance().newMapStore(retry.getType());
		List<RetryHolder> retries = new ArrayList<RetryHolder>();
		retries.add(retry);
		store.archive(retries, false);
	}
	
	
	/**
	 * Rules of truncating stack trace:
	 * 
	 * 
	 */
	private void truncateStackTrace(RetryHolder retry, RetryConfiguration config) {
		
		if (null == retry.getException()) return;	
								
		final int EXCEPTION_LEVEL = config.getExceptionLevel();
		final int STACK_TRACE_COUNT = config.getStackTraceLinesCount();
		
		Exception exceptionCloned = null;
		Exception exceptionInitial = retry.getException();
			
		if (EXCEPTION_LEVEL<=0) 
			exceptionCloned = createException(exceptionInitial, STACK_TRACE_COUNT);
		else {
			for (int i= 0; i < EXCEPTION_LEVEL; i++)
			{	if (exceptionInitial.getCause()==null || 
						exceptionInitial.getCause() == exceptionInitial) 
							break;
				exceptionInitial = (Exception)exceptionInitial.getCause();
			}
			exceptionCloned = createException(exceptionInitial, STACK_TRACE_COUNT);
		}
		
		retry.setException(exceptionCloned);
			
	}
	
	private Exception createException(Exception exception, int linesCount){
		if (null == exception) return null;
		
		Exception cloned = new Exception(exception.getMessage());
		
		if (linesCount <=0 ) 
			cloned.setStackTrace(new StackTraceElement[]{});
		else if ( linesCount >= exception.getStackTrace().length) 
			cloned.setStackTrace(Arrays.copyOf(exception.getStackTrace(), exception.getStackTrace().length));
		else 
			cloned.setStackTrace(Arrays.copyOf(exception.getStackTrace(), linesCount));
		
		return cloned;
	}
	
	public void putRetry(List<RetryHolder> retryList)
			throws NoCallbackException, ConfigException {
		
		putRetry(retryList, true);
	}
	protected void putRetry(List<RetryHolder> retryList, boolean persist) {
		
		if(retryList.isEmpty()){
			//TODO MS - is this behaviour you expect? I'm trying to find if a retryId exists by calling getRetry(id, type) and this was throwing an indexoutofbound when there were no retries found.
			logger.warn( "Put_Retry_RetryList_Empty");
			return;
		}
		
		RetryHolder retry = retryList.get(0);
		RetryConfiguration config = configMgr.getConfiguration(retry.getType());
		
		
		if (configMgr.getConfiguration(retry.getType()) == null) {
			throw new ConfigException("No configuration set for type: " + retry.getType());
		}

		try {
			//queue locally if we have a local queue buffer
			if (localQueuer.addIfNotEmpty(retry)) {
				
				return;
			}
			for (RetryHolder rh:retryList) {
				if (rh.getRetryData() != null && rh.getPayload() == null)
					rh.setPayload(config.getSerializer().serializeToByte(retry.getRetryData()));
			}
			DistributedTask<Void> distTask = new DistributedTask<Void>(new PutRetryTask(retryList,true), retry.getId());
			
			h1.getExecutorService(EXEC_SRV_NAME).submit(distTask);
			dealSync(distTask, config);
		}catch (Exception e) {
			//Unable to add retry
			logger.error( "Put_Retry_Exception: msg={}, Type={}",  e.getMessage(), (retry!=null)?retry.getType():null, e);
			if (configMgr.getRetryHzConfig().isThrowOnAddException())
				throw new RuntimeException("Configured to Throw exception, deal with it");
			else {
				//Add to local queue for later consumption
				localQueuer.add(retry);
			}
		}
		//inform state manager
		stateMgr.retryAddedEvent(retry.getType(),true);
		
	}
	/**
	 * We can't fullfil this contract for FIFO based ordering.
	 */
	public void removeRetry(String retryId, String type) {
		
		IMap<String,List<RetryHolder>> distMap = h1.getMap(type);
		
		try {
			distMap.lock(retryId);
			distMap.remove(retryId);
			RetryMapStore store = (RetryMapStore)RetryMapStoreFactory.getInstance().newMapStore(type);
			store.delete(retryId);
			//distMap.unlock(retryId);
		}finally {
			if (distMap != null && retryId != null)
				distMap.unlock(retryId);
		}
		
	}
	public List<RetryHolder> getRetry(String retryId, String type) {
		
		List<RetryHolder> list = null;
		if (isActive(type)) {
			Map<String,List<RetryHolder>> distMap = h1.getMap(type);
			list = distMap.get(retryId);
				
			//resort to Db fetch
			if (list == null) {
				list = RetryMapStoreFactory.getInstance().newMapStore(type).load(retryId);
				//only call putRetry if the list is not empty, putRetry will log error otherwise (before it was throwing an index out of bound but I changed it to log an error)
				if(list != null && !list.isEmpty()){
					logger.info( "Put_Retry_RetryList", "Loaded retry list from DB", "Type", type, "Id", retryId, "Size", list.size());
					putRetry(list, false);
				}else{
					//TODO - MS - I think the mapstorefactory sets this to empty list but probably should be null??
					list = null;
				}
			}			
		}
		return list;
		
	}
	/**
	 * This could indicate we're in DEQUEUE mode
	 * or that we've lost some data (members lost)
	 * 
	 * @param type
	 * @return
	 */
	@Deprecated
	private boolean isActive(String type) {
//		RetryState transitionType = stateMgr.getState(type);
//		if (stateMgr.isMemberLostEvent())
//			return true;
//		return  (! (transitionType == RetryState.DRAINED) );
		return true;
	}
	@Override
	public List<RetryHolder> getRetry(String retryId, String type,
			String secondaryIndex) {
		List<RetryHolder> list = null;
		if (isActive(type)) {
			List<RetryHolder> qlist = getRetry(retryId,type);
			list = new ArrayList<RetryHolder>();
			for (RetryHolder holder:qlist) {
				if (secondaryIndex.equals(holder.getSecondaryIndex())) {
					list.add(holder);
				}
			}
			
		}
		return list;
	}
	
	
	
	
	@Override
	public int countBySecondaryIndex(String type,String secondaryIndex) {
		if (isActive(type)) {
			EntryObject e = new PredicateBuilder().getEntryObject();
			Predicate predicate = e.is(secondaryIndex);
					
			IMap<String,List<RetryHolder>> map = h1.getMap(type);
			Collection<List<RetryHolder>> values = map.values(predicate);
			
			return values.size();
		}
		return 0;
	}
	@Override
	public Collection<RetryHolder> bySecondaryIndex(String type,String secondaryIndex) {

		if (isActive(type)) {
		EntryObject e = new PredicateBuilder().getEntryObject();
		Predicate predicate = e.is(secondaryIndex);
				
		IMap<String,List<RetryHolder>> map = h1.getMap(type);
		Collection<List<RetryHolder>> values = map.values(predicate);
		ArrayList<RetryHolder> retList = new ArrayList<RetryHolder>(values.size());
		for (List<RetryHolder> list: values){
			retList.addAll(list);
		}
		return retList;
		}
		return null;
		
	}
	

	/**
	 * TODO: we need a consistent method for checking that we go to storage or remain
	 * purely a HZ operation.
	 * 
	 */
	public boolean exists(String retryId, String type) {
		Map<String, List<RetryHolder>> distMap = h1.getMap(type);
		boolean exists = distMap.containsKey(retryId);

		// resort to Db fetch if data has not been loaded yet from DB
		if (!exists && !RetryUtil.hasLoaded(type)) {
			exists = RetryMapStoreFactory.getInstance().newMapStore(type).load(retryId) != null;
		}
		return exists;
	}
	
	/*public int count(String retryId, String type) {
		Map<String, List<RetryHolder>> distMap = h1.getMap(type);
		List<RetryHolder> list = distMap.get(retryId);

		// resort to Db fetch if data has not been loaded yet from DB
		if (list == null && !RetryUtil.hasLoadedFromDB(type)) {
			list = RetryMapStoreFactory.getInstance().newMapStore(type)
					.load(retryId);
		}

		return list!=null ? list.size() : 0;
	}*/

	public int count(String type) {
		RetryState transitionType = stateMgr.getState(type);
		int count = 0;
		if (! (transitionType == RetryState.DRAINED) ){
			Map<String,List<RetryHolder>> distMap = h1.getMap(type);
			count = distMap.size();
		}
		return count;
	}
	
	public void registerCallback(RetryCallback callback, String type) {
		callbackManager.addCallback(callback, type);
	}
	
	//configure the remote cluster
	//The side effect of Remote RPC is that it needs the explicit class, not interface name :(
	public void registerRemoteCallback(RemoteCallback.DefaultRemoteCallback remoteCallback) {
		logger.info( "Register_remotecallback","","callback",remoteCallback);
		
		callbackManager.addCallback(getCallbackRemoteProxy(remoteCallback), remoteCallback.getType());
		
	}
	private synchronized CallbackRemoteProxy getCallbackRemoteProxy(RemoteCallback.DefaultRemoteCallback remoteCallback) {
		if (this.callRemotebackProxy == null) {
			this.callRemotebackProxy = new CallbackRemoteProxy(remoteCallback.getClientConfig());
		}
		return this.callRemotebackProxy;
	}
	
	
	public RetryCallback registeredCallback(String type) {
		return callbackManager.getCallbackMap().get(type);
	}
	
	public void removeCallback(RetryCallback callback,String retryType) {
		callbackManager.removeCallback(callback, retryType);
		
	}

	public Map<String,RetryCallback> getCallbacks() {
		return callbackManager.getCallbackMap();
	}
	
	public void registerTransitionListener(
			RetryTransitionListener transitionListener) {

		stateMgr.addTransitionListener(transitionListener);
		
	}

	public void removeTransitionListener(
			RetryTransitionListener transitionListener) {
		stateMgr.removeListener(transitionListener);
		
	}
	/**
	 * An iterator of the ids.
	 */
	public Iterator<String> getRetries(String type) {
		throw new UnsupportedOperationException();
	}

	public void tryDequeueAll() {
		callbackManager.tryDequeue();
	}
	public void tryDequeue(String type) {
		callbackManager.tryDequeue(type);
	}
	public RetryConfigManager getConfigManager() {
		return configMgr;
	}
	public void setConfigManager(HazelcastConfigManager configMgr) {
		this.configMgr = configMgr;
	}
	public CallbackManager getCallbackManager() {
		return callbackManager;
	}
	public void setCallbackManager(CallbackManager callbackManager) {
		this.callbackManager = callbackManager;
	}
	public StateManager getStateMgr() {
		return stateMgr;
	}
	public void setStateMgr(StateManager stateMgr) {
		this.stateMgr = stateMgr;
	}
	public RetryStats getStats() {
		return stats;
	}
	public void setStats(RetryStats stats) {
		this.stats = stats;
	}
	public HazelcastInstance getH1() {
		return h1;
	}
	public void setH1(HazelcastInstance h1) {
		this.h1 = h1;
	}
	//This does NOT work, as state is initialized on
	//constructors - this is a larger TODO
	public void setH1AndInit(HazelcastInstance h1) {
		this.h1 = h1;
		this.callbackManager.setH1(h1);
		this.stateMgr.setH1(h1);
		this.stateMgr.init();
		
	}
	@Override
	public RetryState getState(String type) {
		return stateMgr.getState(type);
	}
	@Override
	public Map<String, RetryState> getAllStates() {
		return stateMgr.getAllStates();
	}
	public LocalQueuer getLocalQueuer() {
		return localQueuer;
	}
	public void setLocalQueuer(LocalQueuer localQueuer) {
		this.localQueuer = localQueuer;
	}
	
	public HzStateMachine getHzStateMachine() {
		return hzStateMachine;
	}
	@Override
	public String getId() {
		return id;
	}
	
	
}
