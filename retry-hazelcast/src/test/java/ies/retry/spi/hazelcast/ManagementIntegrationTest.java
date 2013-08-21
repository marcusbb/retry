package ies.retry.spi.hazelcast;

import static org.junit.Assert.fail;
import hazelcast.SuccessCallback;
import ies.retry.ConfigException;
import ies.retry.Retry;
import ies.retry.RetryConfiguration;
import ies.retry.RetryHolder;
import ies.retry.RetryManager;
import ies.retry.RetryState;
import ies.retry.spi.hazelcast.jmx.RetryManagement;
import ies.retry.spi.hazelcast.util.HzUtil;
import ies.retry.xml.XMLRetryConfigMgr;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.Collection;
import java.util.concurrent.CountDownLatch;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import junit.framework.Assert;

import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class ManagementIntegrationTest {

	private static RetryManager retryManager;
	private static RetryManagement management;
	private static StateManager stateManager;
	//private static int numRetryTypes = 3;
	private static int runCount = 0;
	
	
	@BeforeClass
	public static void beforeClass() throws Exception {
		HzIntegrationTestUtil.beforeClass();
		XMLRetryConfigMgr.setXML_FILE("retry_config_single_type.xml");
		HzUtil.HZ_CONFIG_FILE = "hz-mgmt-integration.xml";

		retryManager = Retry.getRetryManager();
		stateManager = ((HazelcastRetryImpl)retryManager).getStateMgr();
		//retryManager.registerCallback(new SuccessCallback(), "poke");
		
		management =  new RetryManagement();
		management.init((HazelcastRetryImpl)retryManager);
		
		MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
		mbs.registerMBean(management, new ObjectName( "retry:retry=hazelcast-retry"));
		
		
	}
	@AfterClass
	public static void afterClass() throws Exception {
		
		MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
		mbs.unregisterMBean( new ObjectName( "retry:retry=hazelcast-retry"));
		retryManager.shutdown();
		//HzIntegrationTestUtil.afterClass();
		
	}
	@Before
	public void before() {
		runCount++;
	}
	
	/**
	 * 
	 * This one is fragile - assumes it got run first.
	 */
	@Test
	public void getState() throws Exception {
		String state = management.getState("POKE");
		Assume.assumeTrue("getState() is not the first executed test. Therefore, it was ignored", runCount ==1);
		Assert.assertEquals("DRAINED", state);
		
	}
	
	@Test
	public void suspendResume() throws Exception {
		String state = management.getState("POKE");
		CountDownLatch latch = new CountDownLatch(1);
		StateListener listener = new StateListener(latch);
		retryManager.registerTransitionListener(listener);
		
		
		Assert.assertEquals(RetryState.DRAINED.toString(), state);
		
		management.suspend();
		
		latch.await();
		state = management.getState("POKE");
		System.out.println("State after suspend: " + state);
		//Assert.assertEquals(RetryState.SUSPENDED.toString(), state);
		
		latch = new CountDownLatch(1);
		listener.setLatch(latch);
		management.resume();
		
		latch.await();
		state = management.getState("POKE");
		System.out.println("State: " + state);
		Assert.assertEquals(RetryState.QUEUED.toString(), state);
		
	}
	@Test
	public void addOneCheckSize() throws Exception {
		retryManager.registerCallback(new SuccessCallback(), "POKE");
		
		RetryHolder holder = new RetryHolder("12334" , "POKE", new IOException(
				"Houston there is a problem"), "Useful Serializable object ");
		retryManager.addRetry(holder);
		
		Assert.assertEquals(new Long(1), management.getGridCount() );
		
		retryManager.removeRetry("12334", "poke");
		stateManager.init();
		
		//artifically put back in DRAINED state
		stateManager.getAllStates().put("POKE", RetryState.DRAINED);
		
	}
	@Test
	public void getTypes () {
		String []types = management.getRetryTypes();
		Assert.assertEquals("POKE", types[0]);
		
	}
	

	@Test
	public void loadTest () {
		String origXml = management.getConfig();
		System.out.println("orig: " + origXml);
		try {
			management.loadConfig("");
			fail("Parsing error");
		}catch (ConfigException e) {
		}
		String xml = "<retry><provider>ies.retry.spi.hazelcast.HazelcastRetryImpl</provider><typeConfig><backOff><interval>5000</interval><intervalMultiplier>1.0</intervalMultiplier><maxAttempts>1</maxAttempts></backOff><batchConfig><batchHeartBeat>1000</batchHeartBeat><batchSize>1000</batchSize><dispatchCoreTPSize>10</dispatchCoreTPSize></batchConfig><listBacked>true</listBacked><queueStrategy>UNORDERED</queueStrategy><syncRetryAdd>true</syncRetryAdd><type>poke</type></typeConfig>"
				+ "<typeConfig><type>poke2</type><backOff><interval>1000</interval><intervalMultiplier>1.0</intervalMultiplier><maxAttempts>1</maxAttempts></backOff><batchConfig><batchHeartBeat>1000</batchHeartBeat><batchSize>1000</batchSize></batchConfig><listBacked>true</listBacked><queueStrategy>UNORDERED</queueStrategy><syncRetryAdd>true</syncRetryAdd></typeConfig>"
				+"<persistenceConfig><jpaPU>retry</jpaPU><loadFetchSize>1000</loadFetchSize><ON>false</ON><poolSize>0</poolSize><tpPolicy>CACHED_THREAD</tpPolicy><writeSync>true</writeSync></persistenceConfig></retry>";
		//Assert.assertEquals("poke", types[0]);
		management.loadConfig(xml);
		RetryConfiguration poke2config = retryManager.getConfigManager().getConfiguration("poke2");
		Assert.assertNotNull(poke2config);
		System.out.println("long: " + poke2config.getBackOff().getInterval());
		
		management.loadConfig(origXml);
	}
	
	@Test
	public void loadAndPublishTest () {
		String origXml = management.getConfig();
		System.out.println("orig: " + origXml);
		try {
			management.loadConfig("");
			fail("Parsing error");
		}catch (ConfigException e) {
		}
		String xml = "<retry><provider>ies.retry.spi.hazelcast.HazelcastRetryImpl</provider><typeConfig><backOff><interval>5000</interval><intervalMultiplier>1.0</intervalMultiplier><maxAttempts>1</maxAttempts></backOff><batchConfig><batchHeartBeat>1000</batchHeartBeat><batchSize>1000</batchSize><dispatchCoreTPSize>10</dispatchCoreTPSize></batchConfig><listBacked>true</listBacked><queueStrategy>UNORDERED</queueStrategy><syncRetryAdd>true</syncRetryAdd><type>poke</type></typeConfig>"
				+ "<typeConfig><type>poke2</type><backOff><interval>1000</interval><intervalMultiplier>1.0</intervalMultiplier><maxAttempts>1</maxAttempts></backOff><batchConfig><batchHeartBeat>1000</batchHeartBeat><batchSize>1000</batchSize></batchConfig><listBacked>true</listBacked><queueStrategy>UNORDERED</queueStrategy><syncRetryAdd>true</syncRetryAdd></typeConfig>"
				+"<persistenceConfig><jpaPU>retry</jpaPU><loadFetchSize>1000</loadFetchSize><ON>false</ON><poolSize>0</poolSize><tpPolicy>CACHED_THREAD</tpPolicy><writeSync>true</writeSync></persistenceConfig></retry>";
		//Assert.assertEquals("poke", types[0]);
		management.loadConfig(xml);
		RetryConfiguration poke2config = retryManager.getConfigManager().getConfiguration("poke2");
		Assert.assertNotNull(poke2config);
		System.out.println("long: " + poke2config.getBackOff().getInterval());
		
		management.loadConfig(origXml);
		Collection<RetryConfiguration> origConfig = retryManager.getConfigManager().getAll();
		
		Assert.assertEquals(1, origConfig.size());
		
	}
}

