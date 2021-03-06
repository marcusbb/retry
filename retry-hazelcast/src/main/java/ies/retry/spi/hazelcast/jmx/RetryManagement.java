package ies.retry.spi.hazelcast.jmx;

import ies.retry.ConfigException;
import ies.retry.RetryConfiguration;
import ies.retry.Retry;
import ies.retry.RetryState;
import ies.retry.spi.hazelcast.HazelcastRetryImpl;
import ies.retry.spi.hazelcast.HzState;
import ies.retry.spi.hazelcast.HzStateMachine;
import ies.retry.spi.hazelcast.RetryStat;
import ies.retry.spi.hazelcast.StateManager;
import ies.retry.spi.hazelcast.StateManager.LoadingState;
import ies.retry.spi.hazelcast.config.HazelcastConfigManager;
import ies.retry.spi.hazelcast.persistence.RetryMapStore;
import ies.retry.spi.hazelcast.persistence.RetryMapStoreFactory;
import ies.retry.xml.XMLRetryConfigMgr;
import ies.retry.xml.XmlRetryConfig;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import javax.xml.bind.JAXBException;

import provision.services.logging.Logger;

import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.IMap;
import com.hazelcast.core.ITopic;
import com.hazelcast.core.Message;
import com.hazelcast.core.MessageListener;


public class RetryManagement implements RetryManagementMBean,MessageListener<ConfigBroadcast> {

	private static String CALLER = RetryManagement.class.toString();
	private HazelcastRetryImpl coordinator;
	
	
	public static String topic ="jmxTopic";
	private ITopic<ConfigBroadcast> hzTopic = null;
			
	
	public RetryManagement() {
		
	}
	public RetryManagement(HazelcastRetryImpl coordinator,StateManager stateMgr) {
		this.coordinator = coordinator;
		
		hzTopic =  coordinator.getH1().getTopic(topic);
		hzTopic.addMessageListener(this);
		
	}
	
	public void init(HazelcastRetryImpl impl) {
		this.coordinator = impl;
		
		hzTopic =  impl.getH1().getTopic(topic);
		hzTopic.addMessageListener(this);
	}
	
	public synchronized void onMessage(Message<ConfigBroadcast> message) {
		ConfigBroadcast event = message.getMessageObject();
		HazelcastConfigManager configMgr = ((HazelcastConfigManager)this.coordinator.getConfigManager());
		configMgr.setConfig(event.getXmlConfig());
		configMgr.notifyListeners();
		
	}
	


	
	@Override
	public String getMaster() {
		//will get the hostname of the master node
		return coordinator.getStateMgr().getMasterMember().getInetSocketAddress().getHostName();
	}
	@Override
	public void shutdown() {
		coordinator.shutdown();
		this.coordinator = null;
		
	}
	public void suspend() throws IllegalStateException {
		Collection<String> types = coordinator.getStateMgr().getAllStates().keySet();
		for (String type:types) {
			coordinator.getStateMgr().suspend(type);
		}
		
		
	}
	
	
	
	public String[] getRetryTypes() {
		
		return coordinator.getStateMgr().getAllStates().keySet().toArray(new String[]{});
		
	}
	public void suspend(String type) throws IllegalStateException {
		Logger.info(CALLER, "Suspend_Retries_By_Type", "Suspending retry type=" + type);
		coordinator.getStateMgr().suspend(type);
		
	}

	
	public void resume(String type) throws IllegalStateException {
		Logger.info(CALLER, "Resume_Retries_By_Type", "Resuming retry type=" + type);
		coordinator.getStateMgr().resume(type);
		
	}

	
	public void resume() throws IllegalStateException {
		Collection<String> types = coordinator.getStateMgr().getAllStates().keySet();
		for (String type:types) {
			coordinator.getStateMgr().resume(type);
		}
		
		
	}

	
	public String getConfig() {
		try {
			return ((XMLRetryConfigMgr)coordinator.getConfigManager()).marshallXML();
		}catch (JAXBException e) {
			throw new ConfigException(e.getMessage(),e);
		}
	}

	/**
	 * Distributed -reload operation
	 */
	public void reloadConfig() {
		try {
			((XMLRetryConfigMgr)coordinator.getConfigManager()).load();
			hzTopic.publish( new ConfigBroadcast( ((XMLRetryConfigMgr)coordinator.getConfigManager()).getConfig() ) );
			
		} catch (IOException e) {
			throw new RuntimeException(e.getMessage(),e);
		} catch (JAXBException e) {
			throw new RuntimeException(e.getMessage(),e);
		}
		
		
	}

	public void broadcastCurrentConfig() {
		hzTopic.publish( new ConfigBroadcast( ((XMLRetryConfigMgr)coordinator.getConfigManager()).getConfig() ) );
	}
	public void broadCast(XmlRetryConfig config) {
		hzTopic.publish(new ConfigBroadcast(config));
	}
	/**
	 * loads config from xml - distributes configuration
	 */
	public void loadConfig(String xml) throws ConfigException {
		XMLRetryConfigMgr configMgr = ((XMLRetryConfigMgr)coordinator.getConfigManager());
		try {
			configMgr.load(xml);
			hzTopic.publish( new ConfigBroadcast(configMgr.getConfig()));
		}catch (Exception e) {
			throw new ConfigException(e.getMessage(), e);
		}
	}

	/**
	 * 
	 */
	public Long getGridCount() {
		long count = 0;
		for (String key:coordinator.getStateMgr().getAllStates().keySet()) {
			count += getGridCount(key);
		}
		return count;
	}

	
	public Long getGridCount(String type) {
		if (coordinator.getStateMgr().getAllStates().get(type)!= null) {
			return (long)((HazelcastRetryImpl)Retry.getRetryManager()).getH1().getMap(type).size();
		}
		return 0L;
	}

	
	public String statByType(String type) {
		return coordinator.getStats().getAllStats().get(type).toString();
		
	}
	
	public Date earliestByType(String type) {
		Date date = new Date();
		RetryStat stat = coordinator.getStats().getAllStats().get(type);
		if (stat != null) {
			date = new Date(stat.getEarliestTs());
		}
		return date;
	}
	
	public Long[][] getFailuresByType(String type) {
		RetryStat stat = coordinator.getStats().getAllStats().get(type);
		
		Map<Integer,AtomicLong> map = stat.getTotalFailed();
		Long[][] lstat = new Long[map.size()][2];
		int index = 0;
		for (Integer i:map.keySet()) {
			lstat[index][0] = new Long(i);
			lstat[index][1] = map.get(i).longValue();
		}
		return lstat;
	}
	
	public String getAllStats() {
		return coordinator.getStats().toString();
	}
	public String getState(String type) {
		RetryState state =  coordinator.getStateMgr().getState(type);
		
		if (state != null)
			return state.toString();
		return null;
	}
	
	
	
	@Override
	public boolean callBackRegistered(String type) {
		if (coordinator.getCallbacks() != null)
			return coordinator.getCallbacks().get(type) != null;
		return false;
	}
	@Override
	public String[] getCallbacksRegistered() {
		String [] types = null;
		
		if (coordinator.getCallbacks()!=null) {
			types = coordinator.getCallbacks().keySet().toArray(new String[]{});
		}
		
		return types;
	}
	public void tryDequeue() {
		coordinator.tryDequeueAll();
		
	}
	@Override
	public void tryDequeue(String type) {
		coordinator.tryDequeue(type);
		
	}
	
	@Override
	public int getDequeueBlockSize(String type) {
		return coordinator.getCallbackManager().getBatchSize(type);
	}
	
	
	
	@Override
	public void loadFromDB() {
		XMLRetryConfigMgr configMgr = ((XMLRetryConfigMgr)coordinator.getConfigManager());
		ArrayList<String> typeList = new ArrayList<String>();
		for (RetryConfiguration config:configMgr.getConfigMap().values()) {
			Logger.info(CALLER, "Loading from DB: " + config.getType());
			typeList.add(config.getType());
		}
		coordinator.getStateMgr().loadDataAsync(typeList);
		
		
	}
	@Override
	public void loadFromDB(String retryType) {
		
		ArrayList<String> typeList = new ArrayList<String>();
		typeList.add(retryType);
		coordinator.getStateMgr().loadDataAsync(typeList);
		
	}
		
	/**
	 * Gets loading state.
	 */
	@Override
	public String getLoadingState(String retryType) {
		IMap<String, LoadingState> loadingStateMap = coordinator.getH1().getMap(StateManager.DB_LOADING_STATE);
		
		return loadingStateMap.get(retryType).toString();
		
	}
		
	@Override
	public String[] getLoadingState() {
		IMap<String, LoadingState> loadingStateMap = coordinator.getH1().getMap(StateManager.DB_LOADING_STATE);
		String []retArr = new String[loadingStateMap.size()];
		int i=0;
		for (String key:loadingStateMap.keySet()) {
			String val = loadingStateMap.get(key).toString();
			retArr[i] = key + ":" +val;
			i++;
		}
		return retArr;
		
	}
	
	/*
	 * Only Master node requests count from RETRY table
	 */
	@Override
	public long getStoreCount(String type) {
		if(coordinator.getStateMgr().isMaster()) 
			return RetryMapStoreFactory.getInstance().newMapStore(type).count();
		else 
			return Long.MIN_VALUE;
	}
	
	/*
	 * Only Master node requests count from RETRY table
	 */
	@Override
	public long getStoreCount() {
		if (coordinator.getStateMgr().isMaster()) {
			XMLRetryConfigMgr configMgr = ((XMLRetryConfigMgr)coordinator.getConfigManager());
			long count = 0;
			for (String type:configMgr.getConfigMap().keySet()) {
				count += getStoreCount(type);
			}
			return count;
			}
		else 
			return  Long.MIN_VALUE;
	}
	
	@Override
	public int getLocalQueueCount(String type) {
		return coordinator.getLocalQueuer().size(type);
	}
	
	@Override
	public int[] getLocalQueueCounts() {
		
		Map<String,RetryConfiguration> configMap = coordinator.getConfigManager().getConfigMap();
		int []ret = new int[configMap.size()];
		int i = 0;
		for (String type:configMap.keySet()) {
			ret[i++] = coordinator.getLocalQueuer().size(type);
		}
		
		return ret;
	}
	
	
	@Override
	public int getStoreQueueCount() {
		if ( RetryMapStoreFactory.getInstance() !=null) {
			return RetryMapStoreFactory.getInstance().getTPE().getQueue().size();
		}
		throw new IllegalStateException("RetryMapStoreFactory is not initialized");
	}
	@Override
	public int getStoreActiveThread() {
		if ( RetryMapStoreFactory.getInstance() !=null) {
			return RetryMapStoreFactory.getInstance().getTPE().getActiveCount();
		}
		throw new IllegalStateException("RetryMapStoreFactory is not initialized");
		
	}
	
	
	@Override
	public boolean isPersistenceOn() {
		return ((HazelcastConfigManager)coordinator.getConfigManager()).getRetryHzConfig().getPersistenceConfig().isON();
		
	}
	@Override
	public void setPersistenceOn(boolean on) {
		((HazelcastConfigManager)coordinator.getConfigManager()).getRetryHzConfig().getPersistenceConfig().setON(on);
		broadcastCurrentConfig();
		
	}
	
	
	@Override
	public int getHzRunningInt() {
		if (isHzRunning())
			return 1;
		return 0;
	}
	@Override
	public boolean isHzRunning() {
		return HzState.RUNNING.equals( coordinator.getHzStateMachine().getHzState() );
	}
	
	@Override
	public int getNumHzInstances() {
		return Hazelcast.getAllHazelcastInstances().size();
	}
	@Override
	public String getHzState() {
		return coordinator.getHzStateMachine().getHzState().toString();
	}
	//@Override
	public boolean startHz() throws IllegalStateException {
		coordinator.getHzStateMachine().startHz();
		return true;
	}
	//@Override
	public void shutdownHz() {
		coordinator.getHzStateMachine().stopHz();
		
	}
	//I can't restart retry because all the dynamic callbacks registrations will have been lost
	//@Override
	public void startup() {
		if (coordinator != null)
			throw new IllegalStateException("coordinator is not null: shut it down first");
		
		this.coordinator = new HazelcastRetryImpl();
		Retry.setRetryManager(coordinator);
		
		
	}
	public HazelcastRetryImpl getOrchestrator() {
		return coordinator;
	}
	public void setOrchestrator(HazelcastRetryImpl orchestrator) {
		this.coordinator = orchestrator;
	}
	public StateManager getStateMgr() {
		return coordinator.getStateMgr();
	}
	public void setStateMgr(StateManager stateMgr) {
		coordinator.setStateMgr(stateMgr);
	}
	
	
	
	
}
