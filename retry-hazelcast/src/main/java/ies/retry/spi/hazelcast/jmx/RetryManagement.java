package ies.retry.spi.hazelcast.jmx;

import ies.retry.ConfigException;
import ies.retry.RetryState;

import ies.retry.spi.hazelcast.HazelcastRetryImpl;
import ies.retry.spi.hazelcast.RetryStat;
import ies.retry.spi.hazelcast.StateManager;
import ies.retry.xml.XMLRetryConfigMgr;

import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import javax.xml.bind.JAXBException;

import provision.services.logging.Logger;

import com.hazelcast.core.ITopic;
import com.hazelcast.core.Message;
import com.hazelcast.core.MessageListener;


public class RetryManagement implements RetryManagementMBean,MessageListener<ConfigBroadcast> {

	private static String CALLER = RetryManagement.class.toString();
	private HazelcastRetryImpl coordinator;
	private StateManager stateMgr;
	
	public static String topic ="jmxTopic";
	private ITopic<ConfigBroadcast> hzTopic = null;
			
	
	public RetryManagement() {
		
	}
	public RetryManagement(HazelcastRetryImpl coordinator,StateManager stateMgr) {
		this.coordinator = coordinator;
		this.stateMgr = stateMgr;
		hzTopic =  HazelcastRetryImpl.getHzInst().getTopic(topic);
		hzTopic.addMessageListener(this);
		
	}
	
	public void init(HazelcastRetryImpl impl) {
		this.coordinator = impl;
		this.stateMgr = impl.getStateMgr();
		hzTopic =  HazelcastRetryImpl.getHzInst().getTopic(topic);
		hzTopic.addMessageListener(this);
	}
	
	public void onMessage(Message<ConfigBroadcast> message) {
		ConfigBroadcast event = message.getMessageObject();
		((XMLRetryConfigMgr)this.coordinator.getConfigManager()).setConfig(event.getXmlConfig());
		
	}


	
	@Override
	public void shutdown() {
		coordinator.shutdown();
		
		
	}
	public void suspend() throws IllegalStateException {
		Collection<String> types = stateMgr.getAllStates().keySet();
		for (String type:types) {
			stateMgr.suspend(type);
		}
		
		
	}
	
	
	
	public String[] getRetryTypes() {
		
		return stateMgr.getAllStates().keySet().toArray(new String[]{});
		
	}
	public void suspend(String type) throws IllegalStateException {
		Logger.info(CALLER, "Suspend_Retries_By_Type", "Suspending retry type=" + type);
		stateMgr.suspend(type);
		
	}

	
	public void resume(String type) throws IllegalStateException {
		Logger.info(CALLER, "Resume_Retries_By_Type", "Resuming retry type=" + type);
		stateMgr.resume(type);
		
	}

	
	public void resume() throws IllegalStateException {
		Collection<String> types = stateMgr.getAllStates().keySet();
		for (String type:types) {
			stateMgr.resume(type);
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
		for (String key:stateMgr.getAllStates().keySet()) {
			count += getGridCount(key);
		}
		return count;
	}

	
	public Long getGridCount(String type) {
		if (stateMgr.getAllStates().get(type)!= null) {
			return (long)HazelcastRetryImpl.getHzInst().getMap(type).size();
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
		RetryState state =  stateMgr.getState(type);
		
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
	
	public HazelcastRetryImpl getOrchestrator() {
		return coordinator;
	}
	public void setOrchestrator(HazelcastRetryImpl orchestrator) {
		this.coordinator = orchestrator;
	}
	public StateManager getStateMgr() {
		return stateMgr;
	}
	public void setStateMgr(StateManager stateMgr) {
		this.stateMgr = stateMgr;
	}
	
	
	
}
