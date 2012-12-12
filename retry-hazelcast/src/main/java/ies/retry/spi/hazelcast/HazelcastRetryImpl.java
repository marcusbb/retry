package ies.retry.spi.hazelcast;

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
import ies.retry.spi.hazelcast.disttasks.AddRetryCallable;
import ies.retry.spi.hazelcast.persistence.RetryMapStore;
import ies.retry.spi.hazelcast.persistence.RetryMapStoreFactory;
import ies.retry.spi.hazelcast.util.IOUtil;
import ies.retry.spi.hazelcast.util.StringUtil;
import ies.retry.xml.XMLRetryConfigMgr;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import provision.services.logging.Logger;

import com.hazelcast.config.ClasspathXmlConfig;
import com.hazelcast.config.Config;
import com.hazelcast.config.InMemoryXmlConfig;
import com.hazelcast.core.DistributedTask;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.query.EntryObject;
import com.hazelcast.query.Predicate;
import com.hazelcast.query.PredicateBuilder;

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

	//static Logger logger = Logger.getLogger(HazelcastRetryImpl.class.getName()); 
	private static String CALLER = HazelcastRetryImpl.class.getName();
	public static String HZ_CONFIG_FILE = "hazelcast.xml";
	
	protected static HazelcastInstance h1 = null;
	public static String EXEC_SRV_NAME = "RETRY_ADD";
	protected HazelcastConfigManager configMgr;
	
		
	protected CallbackManager callbackManager;
	
	protected StateManager stateMgr;
	
	protected RetryStats stats;
	protected GridHealthCheck gridCheck;
	
	public HazelcastRetryImpl() throws ConfigException {
		
		
		Logger.info(CALLER, "Constructor", "Hazelcast Retry co-ordinator initiating");
		//Unavoidable double loading - if we're providing customized behavior
		HazelcastConfigManager xmlconfigMgr = new HazelcastConfigManager(this);
		xmlconfigMgr.setFactory(new HazelcastXmlConfFactory());
		xmlconfigMgr.setJaxbConfigClass(HazelcastXmlConfig.class);
		try {
			xmlconfigMgr.load();
			Logger.info(CALLER, "Constructor", "Retry config: " + xmlconfigMgr.marshallXML());
		} catch (Exception e) {
			throw new ConfigException(e);
		} 
		
		this.configMgr = xmlconfigMgr;
		
		Logger.info(CALLER, "Constructor", "Loading HazelCast config from classpath");
		
		if (h1 == null) {
			synchronized(HazelcastRetryImpl.class) {
				
				loadHzConfiguration();
			
			}
		}
		
		Logger.info(CALLER, "Constructor", "Initializing Persistence");
		RetryMapStoreFactory.getInstance().init(((HazelcastXmlConfig)xmlconfigMgr.getConfig()));
		
		Logger.info(CALLER, "Constructor", "Initializing State and Callback");
		//Stats might need to be augmented by state manager as well.
		stats = initStats();
		initIndexes();
		stateMgr = new StateManager(configMgr,stats);
		callbackManager = new CallbackManager(configMgr,stateMgr,stats);
		stateMgr.addTransitionListener(callbackManager);
		//possibly load data from DB
		stateMgr.init();
		
		//Grid Health check
		gridCheck = new GridHealthCheck(stateMgr);
		gridCheck.init();
		
				
	}
	public static HazelcastInstance getHzInst() {
		return h1;
	}
	
	public void loadHzConfiguration() {
		//XMLRetryConfigMgr xmlconfigMgr = (XMLRetryConfigMgr)configMgr;
		try {
			Config config = null;
			String dir = XMLRetryConfigMgr.getCONFIG_DIR();
			if (!"".equals(dir)) {
				String fileName = XMLRetryConfigMgr.getCONFIG_DIR() + System.getProperty("file.separator") + HZ_CONFIG_FILE;
				//config = new FileSystemXmlConfig(fileName);
				String xml = IOUtil.load(fileName);
				xml = StringUtil.replace(xml, System.getProperties());
				config = new InMemoryXmlConfig(xml); 
			}else {
				config = new ClasspathXmlConfig(HZ_CONFIG_FILE);
			}
			
			Logger.info(CALLER, "Load_Hazelcast_Configuration", "Loaded Hazelcast: " + config.toString());
			
								
			h1 = Hazelcast.newHazelcastInstance(config);
		}catch (Exception e) {
			Logger.warn(CALLER, "Load_Hazelcast_Configuration", "NO HAZELCAST CONFIGURATION FOUND: " + e.getMessage(), e);
			h1 = Hazelcast.getDefaultInstance();
			Logger.info(CALLER, "Load_Hazelcast_Configuration", "Using default config");
		}	
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
		if (h1!=null) {
			h1.getLifecycleService().shutdown();
		}
		if (callbackManager != null)
			callbackManager.shutdown();
		if (gridCheck !=null)
			gridCheck.shutdown();
		if (stateMgr != null)
			stateMgr.shutdown();
		RetryMapStoreFactory.getInstance().shutdown();
		
	}
	/**
	 * 
	 * 
	 * Only supported unordered dequeue.
	 * 
	 */
	public void addRetry(RetryHolder retry) throws NoCallbackException,
			ConfigException {
		
		if (callbackManager.getCallbackMap().get(retry.getType()) == null) {
			throw new NoCallbackException("No Callback defined for type: " + retry.getType());
		}
		if (configMgr.getConfiguration(retry.getType()) == null) {
			throw new ConfigException("No configuration set for type: " + retry.getType());
		}
		//Logger.debug(CALLER, "HZ INST: " + this);
		Logger.info(CALLER, "Add_Retry", "Adding Retry.", "ID", retry.getId(), "Type", retry.getType() );
					
		
		RetryConfiguration config = configMgr.getConfiguration(retry.getType());
		//inform state manager
		stateMgr.retryAddedEvent(retry.getType(),true);
				
		try {
			//TODO: first determine the partition key and optimize add
			DistributedTask<Void> distTask = new DistributedTask<Void>(new AddRetryCallable(retry, config), retry.getId());
			//use this distributed task
			 h1.getExecutorService(EXEC_SRV_NAME).submit(distTask);
			if (config.isSyncRetryAdd())
				distTask.get();
		}catch (Exception e) {
			//Unable to add retry
			Logger.error(CALLER, "Add_Retry_Exception", "Exception Message: " + e.getMessage(), "ID", retry.getId(), "Type", e);
		}
		
		
		
		
		
	}
	
	
	
	public void putRetry(List<RetryHolder> retryList)
			throws NoCallbackException, ConfigException {
		
		putRetry(retryList, true);
	}
	protected void putRetry(List<RetryHolder> retryList, boolean persist) {
		
		if(retryList.isEmpty()){
			//TODO MS - is this behaviour you expect? I'm trying to find if a retryId exists by calling getRetry(id, type) and this was throwing an indexoutofbound when there were no retries found.
			Logger.warn(CALLER, "Put_Retry_RetryList_Empty", "putRetry will be skipped since the RetryList is empty");
			return;
		}
		
		RetryHolder retry = retryList.get(0);
		RetryConfiguration config = configMgr.getConfiguration(retry.getType());
		
		
		if (configMgr.getConfiguration(retry.getType()) == null) {
			throw new ConfigException("No configuration set for type: " + retry.getType());
		}
		
		for (RetryHolder rh: retryList) {
			rh.setSystemTs(System.currentTimeMillis());
			
		}
		try {
			Future<Void> future = h1.getExecutorService().submit(new AddRetryCallable(retryList, config,persist ));
			if (config.isSyncRetryAdd())
				future.get();
		}catch (Exception e) {
			//Unable to add retry
			Logger.error(CALLER, "Put_Retry_Exception", "Exception Message: " + e.getMessage(), "Type", (retry!=null)?retry.getType():null, e);
		}
		//inform state manager
		stateMgr.retryAddedEvent(retry.getType(),true);
		
	}
	/**
	 * We can't fullfil this contract for FIFO based ordering.
	 */
	public void removeRetry(String retryId, String type) {
		
		IMap<String,List<RetryHolder>> distMap = h1.getMap(type);
		
		//TODO: make sure to remove from DB
		//could be one network op
		distMap.lock(retryId);
		distMap.remove(retryId);
		RetryMapStore store = (RetryMapStore)RetryMapStoreFactory.getInstance().newMapStore(type);
		store.delete(retryId);
		distMap.unlock(retryId);
		
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
	 * or that we've lost some data
	 * 
	 * @param type
	 * @return
	 */
	private boolean isActive(String type) {
		RetryState transitionType = stateMgr.getState(type);
		if (stateMgr.isMemberLostEvent())
			return true;
		return  (! (transitionType == RetryState.DRAINED) );
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
	public static HazelcastInstance getH1() {
		return h1;
	}
	public static void setH1(HazelcastInstance h1) {
		HazelcastRetryImpl.h1 = h1;
	}
	@Override
	public RetryState getState(String type) {
		return stateMgr.getState(type);
	}
	@Override
	public Map<String, RetryState> getAllStates() {
		return stateMgr.getAllStates();
	}
	
	
	
}
