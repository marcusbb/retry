package ies.retry.spi.hazelcast;


import ies.retry.Retry;
import ies.retry.RetryConfiguration;
import ies.retry.RetryHolder;
import ies.retry.RetryState;
import ies.retry.RetryTransitionEvent;
import ies.retry.RetryTransitionListener;
import ies.retry.spi.hazelcast.config.HazelcastConfigManager;
import ies.retry.spi.hazelcast.config.HazelcastXmlConfig;
import ies.retry.spi.hazelcast.disttasks.AddRetryCallable;
import ies.retry.spi.hazelcast.disttasks.KeySetSizeTask;
import ies.retry.spi.hazelcast.persistence.RetryMapStore;
import ies.retry.spi.hazelcast.persistence.RetryMapStoreFactory;
import ies.retry.spi.hazelcast.persistence.cassandra.CassRetryMapStore;
import ies.retry.xml.XMLRetryConfigMgr;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import provision.services.logging.Logger;

import com.hazelcast.core.DistributedTask;
import com.hazelcast.core.EntryEvent;
import com.hazelcast.core.EntryListener;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.core.Member;
import com.hazelcast.core.MembershipEvent;
import com.hazelcast.core.MembershipListener;
import com.hazelcast.core.MultiTask;


/**
 * Keeps track of the global state changes,
 * the listeners to dispatch to etc....
 * 
 * Manages cluster master detection and state.
 * 
 * TODO: split off the persistence loading features
 * 
 * @author msimonsen
 *
 */
public class StateManager implements  MembershipListener{

	
	public static String STATE_MAP_NAME = "NEAR--RETRY_STATE_MAP";
	private IMap<String, RetryState> globalStateMap = null;
	
	private static String CALLER = StateManager.class.getName();
	
	private List<RetryTransitionListener> globalListeners;
	
	private HazelcastConfigManager configMgr;
	private HazelcastXmlConfig globalConfig;
	
	//master related state information
	private boolean memberLostEvent = false;
	private Member masterMember = null;
	private boolean master =false;
	
	
	public static final String EXEC_SRV_NAME = "RETRY_INIT";
	
	public static final String DB_LOADING_STATE ="NEAR--RETRY_DB_LOADING_STATE";
	private IMap<String, LoadingState> loadingStateMap = null;
	
	private HazelcastInstance h1 = null;
	//stats
	private RetryStats stats;
	
	ExecutorService publishExec;	
	
	private ScheduledThreadPoolExecutor stpe = null;
	
	StateMapEntryListener stateMapListener;
	
	
	public enum LoadingState implements Serializable {
		LOADING,READY;
	}
	
	public StateManager(HazelcastConfigManager configManager,RetryStats stats,HazelcastInstance h1) {
		this.h1 = h1;
		globalStateMap = h1.getMap(STATE_MAP_NAME);
		loadingStateMap = h1.getMap(DB_LOADING_STATE);
		h1.getCluster().addMembershipListener(this);
		this.configMgr = configManager;
		this.globalConfig = (HazelcastXmlConfig)configManager.getConfig();
		
		this.globalListeners = new ArrayList<RetryTransitionListener>();
		this.stats = stats;
		publishExec = Executors.newCachedThreadPool();
		//init();
		
		stateMapListener = new StateMapEntryListener(this);
		globalStateMap.addEntryListener(stateMapListener, true);
		stpe = new ScheduledThreadPoolExecutor(1);
		
		
	}
	
	public void shutdown() {
		if (publishExec != null)
			publishExec.shutdown();
		if (stpe != null)
			stpe.shutdown();
	}
	public void init() {
		//initialize state null -> drained
		List<String> types = new ArrayList<String>();
		for (RetryConfiguration config:configMgr.getConfigMap().values()) {
			if (globalStateMap.get(config.getType()) == null)
				globalStateMap.put(config.getType(), RetryState.DRAINED);
			types.add(config.getType());
		}
		//
		//set master 
		setMaster();
		//load data is deferred to syncGridAndStore
	}
	
	/*To be called for when dynamic retry types register themselves later */
	public void init(RetryConfiguration config) {
		Logger.info(CALLER, "StateManager_Init","","TYPE",config.getType());
		//does a state already exist, just notify listener
		if(globalStateMap.get(config.getType()) != null){
			notifyStateListeners(null, new RetryTransitionEvent(RetryState.DRAINED, globalStateMap.get(config.getType()), config.getType()));
			Logger.warn(CALLER, "StateManager_Init","State_determined","TYPE",config.getType(),"state",globalStateMap.get(config.getType()));
			//return;
		} else {
			//set all to drained state initially:
			globalStateMap.put(config.getType(), RetryState.DRAINED);
		}
		//load retry data		
		if (master) {
			Logger.info(CALLER, "Init_State","master_loading","TYPE",config.getType());
			List<String> types = new ArrayList<String>();
			types.add(config.getType());
			//this be deferred like above: 
			//loadDataAsync(types);
		}	
	}
	
	
	

	
	/**
	 * The old loading mechanism - using paging, non-scroll mechanism
	 * @param type
	 * @param config
	 */
	protected void loadData(String type,RetryConfiguration config) {
		HazelcastInstance h1 = ((HazelcastRetryImpl)Retry.getRetryManager()).getH1();
		RetryMapStore store = (RetryMapStore)RetryMapStoreFactory.getInstance().newMapStore(config.getType());

		int retSize = globalConfig.getPersistenceConfig().getLoadFetchSize();
		int index = 0;
		ExecutorService exec = h1.getExecutorService(EXEC_SRV_NAME);
		
		int count = store.count();
		Logger.info(CALLER, "Load_Data", "Starting to load  " + count + " " + type + " retries");
		
		while (count>0) {
			long start = System.currentTimeMillis();
			Map<String,List<RetryHolder>> map = new HashMap<String, List<RetryHolder>>();
			count -= retSize;
			try {
				map = store.load(index, retSize);

				boolean nonZeroMap = map.size() >0;
				if (nonZeroMap) {
					Logger.info(CALLER, "Load_Data", "Loading Retry from Store.", "Type", type, "Index", index, "Map_Size", map.size());
					retryAddedEvent(type,false);
				}
			
				for (List<RetryHolder> retries : map.values()) {
					String retryId = retries.get(0).getId();
					DistributedTask<Void> distTask = new DistributedTask<Void>(new AddRetryCallable(retries, config,false), retryId);
				
					exec.submit(distTask);
				}

				index += retSize;

			}catch (Exception e) {
				Logger.error(CALLER, "LOAD_EXCEPTION","",e.getMessage(),e);
			}
			Logger.info(CALLER, "Type", type, "Loaded: " + map.size() + " in " + ( System.currentTimeMillis() - start));
		}
	}

	public void loadDataAsync(final List<String> types) {
		
		if (configMgr.getRetryHzConfig().getPersistenceConfig().isCassandra()) {
			loadingStateMap.put("__any__", LoadingState.LOADING);
			publishExec.submit(new Runnable() {
				
				@Override
				public void run() {
					//As the map store it should
					if(loadingStateMap.get("__any__") != LoadingState.LOADING ) {
						CassRetryMapStore cassStore = (CassRetryMapStore)RetryMapStoreFactory.getInstance().newMapStore("__any__");
						//loadAll with types not specified will load all
						cassStore.loadAll(configMgr.getRetryHzConfig().getPersistenceConfig().getCqlReaderConfig(),null);
					}
					
				}
			});
		}
		else
			publishExec.submit(new Runnable() {
	
				@Override
				public void run() {
					for(String type: types) {
						RetryConfiguration config = configMgr.getConfiguration(type);
						if(config == null)
							continue;
						try {
						
							
							if(loadingStateMap.get(config.getType()) != LoadingState.LOADING) {
								// initialize loading state null -> loading
								loadingStateMap.put(config.getType(), LoadingState.LOADING);
								Logger.info(CALLER, "Load_Data_Async", "Update loading State -> LOADING", "Type", config.getType());
								
								//scrolling or paging loading?
								if (configMgr.getRetryHzConfig().getPersistenceConfig().isPagedLoading())
									loadData(config.getType(), config);
								else
									loadData(config.getType(),config,true);
								
								loadingStateMap.put(config.getType(), LoadingState.READY);
								Logger.info(CALLER, "Load_Data_Async", "Update loading State -> READY", "Type", config.getType());
							}
								else {
									Logger.warn(CALLER, "UnableToLoad","","TYPE",config.getType());
								}
							 
						}finally {
							loadingStateMap.unlock(config.getType());
						}		
					}
				}
			});
	}
	
	
	/**
	 * It's possible that we should be throttling this back, as we bring the cluster
	 * back up, we want to make sure that we don't skew the partition on the master
	 * 
	 * 
	 * @param type
	 * @param config
	 * @param isWait
	 */
	protected void loadData(String type,RetryConfiguration config,boolean isWait) {
		HazelcastInstance h1 = ((HazelcastRetryImpl)Retry.getRetryManager()).getH1();
		RetryMapStore store = (RetryMapStore)RetryMapStoreFactory.getInstance().newMapStore(config.getType());
		boolean hasMore = true;
		int retSize = globalConfig.getPersistenceConfig().getLoadFetchSize();
		int index = 0;
		ExecutorService exec = h1.getExecutorService(EXEC_SRV_NAME);
		while (hasMore) {
			long start = System.currentTimeMillis();
			Map<String,List<RetryHolder>> map = store.load(retSize);
			index = map.size();
			boolean nonZeroMap = map.size() >0;
			if (nonZeroMap) {
				Logger.info(CALLER, "Load_Data", "Loading Retry from Store.", "Type", type, "Index", index, "Map_Size", map.size());
				retryAddedEvent(type,false);
			}

			//List<Future<Void>> futures = new ArrayList<Future<Void>>(map.size());
			List<DistributedTask<Void>> tasks = new ArrayList<DistributedTask<Void>>();
			for (List<RetryHolder> retry:map.values()) {
				String retryId = retry.get(0).getId();
				DistributedTask<Void> distTask = new DistributedTask<Void>(new AddRetryCallable(retry, config,false), retryId);
				
				tasks.add(distTask);
				exec.submit(distTask);
			}
			
			if(isWait) {
				for(Future<Void> future:tasks) {
					try {
						future.get(300, TimeUnit.SECONDS);
					} catch (Exception e) {
						Logger.error(CALLER, "Loading_Exception","","msg",e.getMessage(),e);
					}
				}
			}
			
			index += retSize;
			hasMore = nonZeroMap;
			Logger.info(CALLER,"Loaded : " + map.size() + "in " + ( System.currentTimeMillis() - start));
		}
	}
	
	protected void setMaster()  {
		
		masterMember = h1.getCluster().getMembers().iterator().next();
		//if I'm the master member then I own the scheduler
		if (h1.getCluster().getLocalMember().equals(masterMember)) {			
			Logger.info(CALLER, "I_Am_Master", "I am the master: "+ masterMember);
			master = true;
			//
			long queueCheckPeriod = configMgr.getRetryHzConfig().getQueueCheckPeriod();
			stpe.scheduleAtFixedRate(
					new SyncGridStorageTask(this), 
					queueCheckPeriod, queueCheckPeriod, TimeUnit.MILLISECONDS);
		} else {
			master = false;
			Logger.info(CALLER, "I_Am_Slave", "I am a slave: master=["+ masterMember + "] slave=" + h1.getCluster().getLocalMember());			
		}		
	}
	
	@Override
	public void memberAdded(MembershipEvent membershipEvent) {
		Logger.info(CALLER, "Member_Added", "Adding new member "+ membershipEvent.getMember());
		setMaster();
		
		
	}

	@Override
	public void memberRemoved(MembershipEvent membershipEvent) {
		Logger.info(CALLER, "Member_Removed", "Member was removed "+ membershipEvent.getMember());
		memberLostEvent = true;
		setMaster();
		
	}

	/**
	 * TODO: synchronize
	 * @param retryState
	 * @param listener
	 */
	public void addTransitionListener(RetryTransitionListener listener) {
		//new Exception().printStackTrace();
		Logger.debug(CALLER, "Transition_listener_Add","listener",listener);
		globalListeners.add(listener);
	}
	/**
	 * TODO: synchronize
	 * @param retryState
	 * @param listener
	 */
	public void removeListener(RetryTransitionListener listener) {
		Logger.debug(CALLER, "Transition_listener_Remove","listener",listener);
		globalListeners.remove(listener);
	}
	/**
	 * synchronization is not required - as we only care about
	 * putting into a final QUEUED state
	 * 
	 * @param type
	 */
	public void retryAddedEvent(String type,boolean syncPush) {
		
		RetryState t = globalStateMap.get(type);
		if ( t == null) {
			publish(new RetryTransitionEvent(t, RetryState.DRAINED,type));
		}
		if (t== RetryState.DRAINED) {
			//Logger.info(CALLER, "Retry_Added_Event", "Publishing message: type=[" + type + "]" + ", state=" + RetryState.QUEUED);
			publish(new RetryTransitionEvent(t, RetryState.QUEUED,type));
					
		}
		//if QUEUED or SUSPENDED there is no need to 
		//modify state or notify interested parties.
	}
	
	protected void publish(RetryTransitionEvent event) {
		//putting async because if other nodes can't be reached then this will pass through
		globalStateMap.putAsync(event.getRetryType(), event.getRetryState());
		
		//notify others - this will happen in the event change listeners below
		//notifyStateListeners(event);
	}
	
	/**
	 * sync grid and store mechanism
	 */
	public void syncGridAndStore() {
		
		if (!master) {
			Logger.debug(StateManager.class.getName(),"SLAVE_MEMBER","dropping request");
			return;
		}
		if (h1.getCluster().getMembers().size() < configMgr.getRetryHzConfig().getMinMembersToSyncStore()) {
			Logger.warn(CALLER,"MIN_CLUSTER_SIZE_NOT_MET_STORE_SYNC","Not enough members to sync - to force sync use JMX operation");
			return;
			
		}
		HashSet<String> syncTypes = new HashSet<>();
		for (String type:getAllStates().keySet()) {
			RetryState t = globalStateMap.get(type);
			if ( t == null) {
				throw new StateTransitionException();
			}
						
			
			Logger.debug(CALLER, "SYNC_GRID_QUEUED","","TYPE",type);
			int storeCount = ((RetryMapStore)RetryMapStoreFactory.getInstance().newMapStore(type)).count();
			int gridCount = h1.getMap(type).size();
			int maxSize = configMgr.getHzConfiguration().getMapConfig(type).getMaxSizeConfig().getSize();
			int avgSizePerNode = gridCount / h1.getCluster().getMembers().size();
			
			//define over capacity as this average node map size exceeding the max size
			//we don't want to trigger a load if cache has evicted
			boolean overCapacity = avgSizePerNode >= maxSize;
			
			//raw idea of the busy-ness of persistence: don't sync if anything in the queue
			boolean pBusy = RetryMapStoreFactory.getInstance().getTPE().getQueue().size() >= 1;
			
			if ( (gridCount < storeCount) && !pBusy && !overCapacity && master ) {
				
				Logger.warn(CALLER, "SYNC_DB_GRID","","gridCount",gridCount,"storeCount",storeCount);			
				syncTypes.add(type);
			}
			if ( storeCount == 0 && gridCount ==0 ) {
				Logger.info(CALLER, "SYNC_GRID_DB_ZERO","","TYPE",type);
				publish(new RetryTransitionEvent(t, RetryState.DRAINED,type));
				//finally flip the member lost event off,
				//as we're  synchronized persistence
				memberLostEvent = false;
			}
		}
		
			if (configMgr.getRetryHzConfig().getPersistenceConfig().isCassandra()) {
				((CassRetryMapStore)RetryMapStoreFactory.getInstance().newMapStore("__any__"))
					.loadIntoHZ(configMgr.getRetryHzConfig().getPersistenceConfig().getCqlReaderConfig(), syncTypes);
			}else {
				for (String type:syncTypes) {
					loadData(DB_LOADING_STATE, configMgr.getConfiguration(type), false);
				}
			}
		
	}
	
	public boolean gridEmpty(String type) {
		for (Integer size:getLocalKeySetSizes(type) ) {
			if (size > 0) return false;
		}
		return true;
	}
	
	//This method may be completely redundant to an IMap.size function: consider removing it.
	private Collection<Integer> getLocalKeySetSizes(String type) {
		HazelcastInstance h1 = ((HazelcastRetryImpl)Retry.getRetryManager()).getH1();
		
		MultiTask<Integer> sizeTask = new MultiTask<Integer>(new KeySetSizeTask(type),h1.getCluster().getMembers());
		
		h1.getExecutorService().execute(sizeTask);
		try {
			return sizeTask.get();
		}catch (Exception  e) {
			Logger.error(CALLER, "Multitask_execution_failure","","msg",e.getMessage(),e);
			throw new RuntimeException(e);
		}
		
	}
	
	
	
	public void notifyStateListeners(RetryState oldState, RetryTransitionEvent event) {
		
		String retryType = event.getRetryType();
		Logger.info(CALLER, "Notify_State_Listeners", "State Transition [" +retryType +"]" + oldState + "->" + event.getRetryState());
		
		//could have our own thread dispatch policy
		//inform all listeners
		
		for (RetryTransitionListener listener:globalListeners) {
			listener.onEvent(event);
		}
	}
	public RetryState getState(String type) {
		return globalStateMap.get(type);
	}
	public Map<String,RetryState> getAllStates() {
		return globalStateMap;
	}
	/**
	 * Can suspend from any state
	 * 
	 * @param type
	 */
	public void suspend(String type) {
		Logger.warn(CALLER, "StateMgr_suspend","Suspended","Type",type);
		RetryState t = globalStateMap.get(type);
		if ( t == null) {
			throw new StateTransitionException();
		}
		publish(new RetryTransitionEvent(t, RetryState.SUSPENDED,type));
		 
	}
	
	public void resume(String type) {
		Logger.warn(CALLER, "StateMgr_resume","Resume","Type",type);
		RetryState t = globalStateMap.get(type);
		if ( t == null) {
			throw new StateTransitionException();
		}
		//will move to drained state - if it doesn't have 
		if (t == RetryState.SUSPENDED)
			publish(new RetryTransitionEvent(t, RetryState.QUEUED,type));
		 
	}

	

	

	public HazelcastInstance getH1() {
		return h1;
	}

	public void setH1(HazelcastInstance h1) {
		this.h1 = h1;
	}

	public XMLRetryConfigMgr getConfigMgr() {
		return configMgr;
	}

	public void setConfigMgr(HazelcastConfigManager configMgr) {
		this.configMgr = configMgr;
	}

	public HazelcastXmlConfig getGlobalConfig() {
		return globalConfig;
	}

	public void setGlobalConfig(HazelcastXmlConfig globalConfig) {
		this.globalConfig = globalConfig;
	}

	public Member getMasterMember() {
		return masterMember;
	}

	public void setMasterMember(Member masterMember) {
		this.masterMember = masterMember;
	}

	public boolean isMaster() {
		return master;
	}

	public void setMaster(boolean master) {
		this.master = master;
	}

	public boolean isMemberLostEvent() {
		return memberLostEvent;
	}

	public void setMemberLostEvent(boolean memberLostEvent) {
		this.memberLostEvent = memberLostEvent;
	}

	public RetryStats getStats() {
		return stats;
	}

	public void setStats(RetryStats stats) {
		this.stats = stats;
	}
	
	@Override
	public String toString() {
		
		return "StateManager + " +getClass().getName() + "@" + Integer.toHexString(hashCode())
				+" [globalStateMap=" + globalStateMap
				+ ", globalListeners=" + globalListeners + ", configMgr="
				+ configMgr + ", globalConfig=" + globalConfig + ", memberLostEvent=" + memberLostEvent
				+ ", masterMember=" + masterMember + ", master=" + master
				+ ", stats=" + stats + ", publishExec=" + publishExec + "]";
	}
	
	
}

class StateMapEntryListener implements EntryListener<String,RetryState> {

	private StateManager stateMgr;
	private static String CALLER = StateMapEntryListener.class.getName();
	
	public StateMapEntryListener(StateManager stateMgr) {
		this.stateMgr = stateMgr;
	}
	@Override
	public void entryAdded(EntryEvent<String, RetryState> event) {
		Logger.info(CALLER, "entryAdded","type/STATE: " +event.getKey()+"/"+ event.getOldValue() +"->" + event.getValue());
		
		if (event.getOldValue() == null) {
			Logger.info(CALLER, "entryAdded","Discarding notification");
			return;
		}
		stateMgr.notifyStateListeners( event.getOldValue(), new RetryTransitionEvent(event.getOldValue(),event.getValue(),event.getKey()) );
	}

	@Override
	public void entryRemoved(EntryEvent<String, RetryState> event) {
		Logger.info(CALLER, "entryRemoved","type/STATE: " +event.getKey()+"/"+ event.getOldValue() +"->" + event.getValue());
		
	}

	@Override
	public void entryUpdated(EntryEvent<String, RetryState> event) {
		Logger.info(CALLER, "entryUpdated","type/STATE: " +event.getKey()+"/"+ event.getOldValue() +"->" + event.getValue());
		if (event.getOldValue() == event.getValue()) {
			Logger.info(CALLER, "entryUpdated","Discarding notification");
			return;
		}
		stateMgr.notifyStateListeners(event.getOldValue(), new RetryTransitionEvent(event.getOldValue(),event.getValue(),event.getKey()) );
	}

	@Override
	public void entryEvicted(EntryEvent<String, RetryState> event) {
		Logger.info(CALLER, "entryEvicted","type/STATE: " +event.getKey()+"/"+ event.getOldValue() +"->" + event.getValue());
		
	}
	
}

class SyncGridStorageTask implements Runnable {

	private StateManager stateMgr;
	private static String CALLER = SyncGridStorageTask.class.getName();
	
	
	public SyncGridStorageTask(StateManager stateMgr) {
		this.stateMgr = stateMgr;
		
	}
	
	@Override
	public void run() {
		Logger.debug(CALLER,"Check_state_start");
				
		try {
			if(stateMgr.isMaster())
				stateMgr.syncGridAndStore();
				
			
		}catch (Throwable e) {
			Logger.error(CALLER,"Check_period_fail","","msg",e.getMessage(),e);
		}
		
	}
	
}



