package ies.retry.spi.hazelcast;

import ies.retry.RetryConfiguration;

import ies.retry.RetryState;
import ies.retry.xml.XMLRetryConfigMgr;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;

import org.junit.Test;

import com.hazelcast.core.HazelcastInstance;

public class StateManagerTest {

	private static HazelcastRetryImpl retryManager;
	private static StateManager stateManager;
	//private static int numRetryTypes = 3;
	
	
	@Before
	public void beforeClass() throws Exception {
		HzIntegrationTestUtil.beforeClass();
		HazelcastRetryImpl.HZ_CONFIG_FILE = "hazelcast_statemgr.xml";
		XMLRetryConfigMgr.setXML_FILE("retry_config.xml");
		
//		XMLRetryConfigMgr.setCONFIG_DIR
		retryManager = new HazelcastRetryImpl();
		stateManager = retryManager.getStateMgr();
		//retryManager.registerCallback(new SuccessCallback(), "poke");
		
		
		
		
	}
	@After
	public  void afterClass() throws Exception {
		
		retryManager.shutdown();
		HzIntegrationTestUtil.afterClass();
	}
	
	/**
	 * 
	 * 
	 */
	@Test
	public void getState() throws Exception {
		RetryState state = stateManager.getState("POKE");
		
		Assert.assertEquals(RetryState.DRAINED, state);
		
	}
	
	@Test
	@Ignore
	public void getStateOnQueued() throws Exception {
		
				
		
		HazelcastInstance hzinst = HazelcastRetryImpl.getHzInst();
		hzinst.getMap(StateManager.STATE_MAP_NAME).put("POKE", RetryState.QUEUED);
		
		//miss the initial notification
		Thread.sleep(200);
		
		RetryStats stats = retryManager.initStats();
		stateManager = new StateManager(retryManager.configMgr, stats);
		
		CountDownLatch latch = new CountDownLatch(1);
		stateManager.addTransitionListener(new StateListener(latch));
		
		stateManager.init();
		//second notification should be received.
		Assert.assertTrue("Failed to release latch",latch.await(10,TimeUnit.MILLISECONDS)) ;
		
		
		Assert.assertEquals(RetryState.QUEUED,stateManager.getState("POKE"));
		
		
		
	
		
	}
	@Test
	@Ignore
	public void getStateDynamicTypeQueued() throws Exception {
		
				
		
		HazelcastInstance hzinst = HazelcastRetryImpl.getHzInst();
		hzinst.getMap(StateManager.STATE_MAP_NAME).put("POKE_CLONED", RetryState.QUEUED);
		//miss the initial notification
		Thread.sleep(200);
				
		RetryStats stats = retryManager.initStats();
		stateManager = new StateManager(retryManager.configMgr, stats);
		
		CountDownLatch latch = new CountDownLatch(1);
		stateManager.addTransitionListener(new StateListener(latch));
		
		RetryConfiguration clonedConfig = retryManager.getConfigManager().cloneConfiguration("POKE");
		clonedConfig.setType("POKE_CLONED");
		
		stateManager.init(clonedConfig);
		
		Assert.assertTrue("Failed to release latch",latch.await(1,TimeUnit.SECONDS)) ;
		
		
		Assert.assertEquals(RetryState.QUEUED,stateManager.getState("POKE_CLONED"));
		
		
		
	
		
	}
	
	
	
	
}


