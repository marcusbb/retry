package ies.retry.spi.hazelcast;


import ies.retry.RetryConfiguration;
import ies.retry.RetryHolder;
import ies.retry.RetryState;
import ies.retry.RetryTransitionEvent;
import ies.retry.RetryTransitionListener;
import ies.retry.spi.hazelcast.config.HazelcastConfigManager;
import ies.retry.spi.hazelcast.config.HazelcastXmlConfig;
import ies.retry.spi.hazelcast.disttasks.AddRetryCallable;
import ies.retry.spi.hazelcast.disttasks.SyncConfigTask;
import ies.retry.spi.hazelcast.disttasks.SyncToMasterTask;
import ies.retry.spi.hazelcast.persistence.RetryMapStore;
import ies.retry.spi.hazelcast.persistence.RetryMapStoreFactory;
import ies.retry.xml.XMLRetryConfigMgr;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import provision.services.logging.Logger;

import com.hazelcast.core.DistributedTask;
import com.hazelcast.core.EntryEvent;
import com.hazelcast.core.EntryListener;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.core.Member;
import com.hazelcast.core.MembershipEvent;
import com.hazelcast.core.MembershipListener;
import com.hazelcast.core.Message;


/**
 * Keeps track of the global state changes,
 * the listeners to dispatch to etc....
 * 
 * Manages cluster master detection and state.
 * 
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
	
	public static final String DB_LOADING_STATE ="RETRY_DB_LOADING_STATE";
	private IMap<String, LoadingState> loadingStateMap = null;
	
	//stats
	private RetryStats stats;
	
	ExecutorService publishExec;	
	
	StateMapEntryListener stateMapListener;
	
	public enum LoadingState implements Serializable {
		LOADING,READY;
	}
	
	public StateManager(HazelcastConfigManager configManager,RetryStats stats) {
						
		globalStateMap = HazelcastRetryImpl.getHzInst().getMap(STATE_MAP_NAME);
		loadingStateMap = HazelcastRetryImpl.getHzInst().getMap(DB_LOADING_STATE);
		HazelcastRetryImpl.getHzInst().getCluster().addMembershipListener(this);
		this.configMgr = configManager;
		this.globalConfig = (HazelcastXmlConfig)configManager.getConfig();
		
		this.globalListeners = new ArrayList<RetryTransitionListener>();
		this.stats = stats;
		publishExec = Executors.newCachedThreadPool();
		//init();
		
		stateMapListener = new StateMapEntryListener(this);
		globalStateMap.addEntryListener(stateMapListener, true);
	}
	
	public void shutdown() {
		if (publishExec != null)
			publishExec.shutdown();
	}
	public void init() {
		//initialize state null -> drained
		List<String> types = new ArrayList<String>();
		for (RetryConfiguration config:configMgr.getConfigMap().values()) {
			if (globalStateMap.get(config.getType()) == null)
				globalStateMap.put(config.getType(), RetryState.DRAINED);
			types.add(config.getType());
		}
		//set master and load retry data
		setMaster();
		if (master) {
			loadDataAsync(types);
			//even if we have slaves coming on line, we should get informed of
			//state changes via loading
		}
		
			//publish current state to listener (callback manager)
			//this is to make sure that QUEUED states are picked up by callback manager
			for (RetryConfiguration config:configMgr.getConfigMap().values()) {
				notifyStateListeners(new RetryTransitionEvent(globalStateMap.get(config.getType()), config.getType()));
			}
		
		
	}
	
	/*To be called for when dynamic retry types register themselves later */
	public void init(RetryConfiguration config) {
		//does a state already exist, just notify listener
		if(globalStateMap.get(config.getType()) != null){
			notifyStateListeners(new RetryTransitionEvent(globalStateMap.get(config.getType()), config.getType()));
			return;
		}
		//set all to drained state initially:
		globalStateMap.put(config.getType(), RetryState.DRAINED);
		
		//load retry data		
		if (master) {
			List<String> types = new ArrayList<String>();
			types.add(config.getType());
			loadDataAsync(types);
		}	
	}
	
	
	
//	protected void loadAllData() {
//		publishExec.submit(new Runnable() {
//			
//			@Override
//			public void run() {
//				for (RetryConfiguration config: configMgr.getConfigMap().values()) {
//					if(loadingStateMap.tryLock(config.getType())) {
//						if(loadingStateMap.get(config.getType()) == null) {
//							// initialize loading state null -> loading
//							loadingStateMap.put(config.getType(), LoadingState.LOADING);
//							Logger.info(CALLER, "Load_All_Data", "Update loading State -> LOADING", "Type", config.getType());
//							loadData(config.getType(),config,true);
//							
//							try {
//								Thread.sleep((int)(Math.random()*120000));
//							} catch (InterruptedException e) {
//							}
//							
//							loadingStateMap.put(config.getType(), LoadingState.READY);
//							Logger.info(CALLER, "Load_All_Data", "Update loading State -> READY", "Type", config.getType());
//						}
//						
//						loadingStateMap.unlock(config.getType());
//					}
//				}
//			}
//		});
//		
//	}
	
	
	protected void loadDataAsync(final List<String> types) {
		publishExec.submit(new Runnable() {

			@Override
			public void run() {
				for(String type: types) {
					RetryConfiguration config = configMgr.getConfiguration(type);
					if(config == null)
						continue;
					
					if(loadingStateMap.tryLock(config.getType())) {
						if(loadingStateMap.get(config.getType()) == null) {
							// initialize loading state null -> loading
							loadingStateMap.put(config.getType(), LoadingState.LOADING);
							Logger.info(CALLER, "Load_Data_Async", "Update loading State -> LOADING", "Type", config.getType());
							loadData(config.getType(),config,true);
							
							loadingStateMap.put(config.getType(), LoadingState.READY);
							Logger.info(CALLER, "Load_Data_Async", "Update loading State -> READY", "Type", config.getType());
						}
						
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
		HazelcastInstance h1 = HazelcastRetryImpl.getHzInst();
		RetryMapStore store = (RetryMapStore)RetryMapStoreFactory.getInstance().newMapStore(config.getType());
		boolean hasMore = true;
		int retSize = globalConfig.getPersistenceConfig().getLoadFetchSize();
		int index = 0;
		while (hasMore) {
			Map<String,List<RetryHolder>> map = store.load(retSize);
			index = map.size();
			boolean nonZeroMap = map.size() >0;
			if (nonZeroMap) {
				Logger.info(CALLER, "Load_Data", "Loading Retry from Store.", "Type", type, "Index", index, "Map_Size", map.size());
				retryAddedEvent(type,false);
			}

			List<Future<Void>> futures = new ArrayList<Future<Void>>(map.size());
			for (List<RetryHolder> listHolder:map.values()) {
				futures.add(h1.getExecutorService().submit(new AddRetryCallable(listHolder,config,false)));
			}
			
			if(isWait) {
				for(Future<Void> future:futures) {
					try {
						future.get(300, TimeUnit.SECONDS);
					} catch (InterruptedException e) {
					} catch (ExecutionException e) {
					} catch (TimeoutException e) {
					}
				}
			}
			
			index += retSize;
			hasMore = nonZeroMap;
				 
		}
	}
	
	protected void setMaster()  {
		HazelcastInstance h1 = HazelcastRetryImpl.getHzInst();
		masterMember = h1.getCluster().getMembers().iterator().next();
		//if I'm the master member then I own the scheduler
		if (h1.getCluster().getLocalMember().equals(masterMember)) {			
			Logger.debug(CALLER, "I_Am_Master", "I am the master: "+ masterMember);
			master = true;
		} else {
			Logger.debug(CALLER, "I_Am_Slave", "I am a slave: master=["+ masterMember + "] slave=" + h1.getCluster().getLocalMember());			
		}		
	}
	
	@Override
	public void memberAdded(MembershipEvent membershipEvent) {
		Logger.info(CALLER, "Member_Added", "Adding new member "+ membershipEvent.getMember());
		setMaster();
		
		
	}

	@Override
	public void memberRemoved(MembershipEvent membershipEvent) {
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
			t= RetryState.DRAINED;
			globalStateMap.put(type, t);
		}
		if (t== RetryState.DRAINED) {
			//Logger.info(CALLER, "Retry_Added_Event", "Publishing message: type=[" + type + "]" + ", state=" + RetryState.QUEUED);
			publish(new RetryTransitionEvent(RetryState.QUEUED,type));
					
		}
		//if QUEUED or SUSPENDED there is no need to 
		//modify state or notify interested parties.
	}
	
	protected void publish(RetryTransitionEvent event) {
		
		globalStateMap.put(event.getRetryType(), event.getRetryState());
		
		//notify others - this will happen in the event change listeners below
		//notifyStateListeners(event);
	}
	
	/**
	 * Local event, called from callback (or de-queue) event
	 * Will also inform transition listeners of this  
	 * 
	 * @param type
	 */
	public boolean isStorageDrained(String type) {
		
		RetryState t = globalStateMap.get(type);
		if ( t == null) {
			throw new StateTransitionException();
		}
		//this doesn't ensure
		//that the cluster + storage is drained - let's make sure
		boolean storedRetry = false;
		
		if (t == RetryState.QUEUED) {
			HazelcastInstance h1 = HazelcastRetryImpl.getHzInst();
			
			storedRetry =storedRetry(type);
			if ( !storedRetry ) {
				publish(new RetryTransitionEvent(RetryState.DRAINED,type));
				//finally flip the member lost event off,
				//as we're  synchronized persistence
				memberLostEvent = false;
			} else if(master) {
				//actively load
				Logger.warn(CALLER, "Queue_Drained", "Found retries in store, loading...", "Type", type);
				loadData(type, configMgr.getConfiguration(type),false);
			}
		} 
		return storedRetry;
	}
	
	private boolean storedRetry(String type) {
		RetryMapStore store = (RetryMapStore)RetryMapStoreFactory.getInstance().newMapStore(type);
		return store.count() > 0;
		
	}
	
	public void notifyStateListeners(RetryTransitionEvent event) {
		
		String retryType = event.getRetryType();
		Logger.info(CALLER, "Notify_State_Listeners", "State Transition [" +retryType +"]" + globalStateMap.get(retryType)+ "->" + event.getRetryState());
		
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
		publish(new RetryTransitionEvent(RetryState.SUSPENDED,type));
		 
	}
	
	public void resume(String type) {
		Logger.warn(CALLER, "StateMgr_resume","Resume","Type",type);
		RetryState t = globalStateMap.get(type);
		if ( t == null) {
			throw new StateTransitionException();
		}
		//will move to drained state - if it doesn't have 
		if (t == RetryState.SUSPENDED)
			publish(new RetryTransitionEvent(RetryState.QUEUED,type));
		 
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
		stateMgr.notifyStateListeners(new RetryTransitionEvent(event.getValue(),event.getKey()) );
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
		stateMgr.notifyStateListeners(new RetryTransitionEvent(event.getValue(),event.getKey()) );
	}

	@Override
	public void entryEvicted(EntryEvent<String, RetryState> event) {
		Logger.info(CALLER, "entryEvicted","type/STATE: " +event.getKey()+"/"+ event.getOldValue() +"->" + event.getValue());
		
	}
	
}


