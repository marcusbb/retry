package ies.retry.spi.hazelcast;

import ies.retry.Retry;
import ies.retry.RetryCallback;
import ies.retry.RetryHolder;
import ies.retry.spi.hazelcast.config.HazelcastConfigManager;
import ies.retry.xml.XMLRetryConfigMgr;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

public class LocalQueuerTest {

	static HazelcastRetryImpl retryManager;
	static String TYPE = "POKE";
	static String ORIG_FILE = XMLRetryConfigMgr.getXML_FILE();
	@BeforeClass
	public static void before() {
		XMLRetryConfigMgr.setXML_FILE("config_local_queue.xml");
		retryManager = (HazelcastRetryImpl) Retry.getRetryManager();
		HazelcastConfigManager confMgr = (HazelcastConfigManager)retryManager.getConfigManager();
		//not sure if this will work, depends on env
		confMgr.getRetryHzConfig().setLocalQueueLogDir(".");
	}
	@AfterClass
	public static void after() {
		
		//set back to original
		XMLRetryConfigMgr.setXML_FILE(ORIG_FILE);
		HzIntegrationTestUtil.afterClass();
	}
	
	@Test
	public void noLocalQueueIntegration() throws Exception {
		
		
		RetryHolder holder = new RetryHolder("id-local", TYPE,new Exception(),"Object");
		
		retryManager.registerCallback(new LatchCallback(new CountDownLatch(1), false), TYPE);
		
		retryManager.addRetry(holder);
		
		org.junit.Assert.assertNotNull(
				retryManager.getH1().getMap(TYPE).get("id-local")
				);
			
		retryManager.removeRetry("id-local", TYPE);
		
	}
	
	@Test
	public void testAddLocalQueueAndProcess() throws Exception {
		
		@SuppressWarnings("static-access")
		LocalQueuerImpl queuer = new LocalQueuerImpl(retryManager.getH1(), retryManager.configMgr);
		CountDownLatch latch = new CountDownLatch(1);
		retryManager.registerCallback(new LatchCallback(latch, true), "FAST_PROCESS");
		
		RetryHolder holder = new RetryHolder("id-local", "FAST_PROCESS",new Exception(),"Object");
		
		queuer.add(holder);
		
		/*org.junit.Assert.assertNotNull(
				retryManager.getH1().getMap("FAST_PROCESS").get("id-local")
				);*/
			
		//It should get de-queued
		Assert.assertTrue(latch.await(5,TimeUnit.SECONDS));
		
		//there is a slight delay upon latch signal and the 
		//removal from hazelcast
		Thread.sleep(100);
		System.out.println(		retryManager.getH1().getMap("FAST_PROCESS").size());
				
		Assert.assertEquals( 0, retryManager.getH1().getMap("FAST_PROCESS").size());
		
		queuer.getQueueLog().close();
		LocalQueueLogTests.deleteFiles();
	}
	
	/**
	 * This doesn't actually test it, but the converse in an integration style test
	 */
	@Test
	public void addNormalConcurrentTP10_1000() throws Exception {
		ExecutorService exec = Executors.newFixedThreadPool(10);
		final AtomicInteger count = new AtomicInteger(0);
		
		retryManager.registerCallback(new RetryCallback() {
			
			@Override
			public boolean onEvent(RetryHolder retry) throws Exception {
				
				return false;
			}
		}, TYPE);
		
		for (int i=0;i<1000;i++) {
			exec.submit(new Runnable() {
				
				@Override
				public void run() {
					RetryHolder holder = new RetryHolder("id-local"+ count.getAndIncrement(), TYPE,new Exception(),"Object");
					
					retryManager.addRetry(holder);
					
				}
			});
		}
		exec.shutdown();
		exec.awaitTermination(10, TimeUnit.SECONDS);
		int localQueueSize = retryManager.getLocalQueuer().size(TYPE);
		Assert.assertEquals(0,localQueueSize);
		Assert.assertEquals(1000, retryManager.getH1().getMap(TYPE).size() );
				
		//synchronous add, check immediately		
		for (int i=0;i<1000;i++  ) {
			Assert.assertNotNull(
					retryManager.getH1().getMap(TYPE).get("id-local"+i)
					);
			retryManager.removeRetry("id-local"+i, TYPE);
		}
		
	}
	
	@Test
	@Ignore
	public void test10TPConcurrentAddFor1000() throws Exception {
		
		@SuppressWarnings("static-access")
		final LocalQueuer queuer = new LocalQueuerImpl(retryManager.getH1(), retryManager.configMgr);
		ExecutorService exec = Executors.newFixedThreadPool(10);
		final AtomicInteger count = new AtomicInteger(0);
		for (int i=0;i<1000;i++  ) {
			exec.submit(new Runnable() {
				
				@Override
				public void run() {
					RetryHolder holder = new RetryHolder("id-local"+ count.getAndIncrement(), TYPE,new Exception(),"Object");
					
					queuer.add(holder);
					
				}
			});
		
		
		}
		//this is fragile, so we're skipping
		Thread.sleep(5000);
		
		for (int i=0;i<1000;i++  ) {
			org.junit.Assert.assertNotNull(
					retryManager.getH1().getMap(TYPE).get("id-local"+i)
					);
		}
		
				
		
	}
	
	//TODO - to complete this test.
	//@Test
	public void integrationHZshutdownTest() {
		
		retryManager.registerCallback(new LatchCallback(new CountDownLatch(1), false), TYPE);
		
		retryManager.getHzInst().getLifecycleService().shutdown();
		
		RetryHolder retry = new RetryHolder("id-local-to-fail", TYPE,new Exception(),"Object");
		
		retryManager.addRetry(retry);
		
				
		
	}
}
