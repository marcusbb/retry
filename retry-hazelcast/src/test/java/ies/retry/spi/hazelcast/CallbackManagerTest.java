package ies.retry.spi.hazelcast;

import ies.retry.Retry;
import ies.retry.RetryHolder;
import ies.retry.xml.XMLRetryConfigMgr;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class CallbackManagerTest {

	static HazelcastRetryImpl retryManager;

	@BeforeClass
	public static void beforeClass() {

		HzIntegrationTestUtil.beforeClass();
		HazelcastRetryImpl.HZ_CONFIG_FILE = "hazelcast_statemgr.xml";
		XMLRetryConfigMgr.setXML_FILE("retry_config_callback.xml");

		retryManager = (HazelcastRetryImpl) Retry.getRetryManager();
	}

	@AfterClass
	public static void afterClass() {
		retryManager.shutdown();
		HzIntegrationTestUtil.afterClass();
	}

	@Test
	public void dequeueSuccess() throws Exception {

		String id = "CALL_BACK_TEST-1";

		RetryHolder holder = new RetryHolder(id, "POKE");
		CountDownLatch latch = new CountDownLatch(1);
		retryManager.registerCallback(new LatchCallback(latch, true), "POKE");
		retryManager.addRetry(holder);

		System.out.println("TS: " + System.currentTimeMillis());
		List<RetryHolder> holderList = (List<RetryHolder>) HazelcastRetryImpl.getHzInst().getMap("POKE").get(id);

		Assert.assertEquals(1, holderList.size());
		// modify next attempt to now
		System.out.println("Next TS: " + holderList.get(0).getNextAttempt());
		holderList.get(0).setNextAttempt(holderList.get(0).getSystemTs());
		HazelcastRetryImpl.getHzInst().getMap("POKE").put(id, holderList);
		
		// de-queue it
		retryManager.getCallbackManager().tryDequeue("POKE");

		// success
		Assert.assertTrue(latch.await(1, TimeUnit.SECONDS));
		
		holderList = (List<RetryHolder>) HazelcastRetryImpl.getHzInst().getMap("POKE").get(id);
		
		Assert.assertNull(holderList);

	}

	
	@Test
	public void addAndDequeueFail() throws Exception {
		String id = "CALL_BACK_TEST-1";
		String TYPE = "POKE_SLOW";
		
		RetryHolder holder = new RetryHolder(id, TYPE);
		CountDownLatch latch = new CountDownLatch(1);
		retryManager.registerCallback(new LatchCallback(latch, false), TYPE);
		retryManager.addRetry(holder);

		System.out.println("TS: " + System.currentTimeMillis());
		List<RetryHolder> holderList = (List<RetryHolder>) HazelcastRetryImpl.getHzInst().getMap(TYPE).get(id);

		Assert.assertEquals(1, holderList.size());
		// modify next attempt to now
		holderList.get(0).setNextAttempt(holderList.get(0).getSystemTs());
		HazelcastRetryImpl.getHzInst().getMap(TYPE).put(id, holderList);
		
		// de-queue it
		retryManager.getCallbackManager().tryDequeue(TYPE);

		// success
		Assert.assertTrue(latch.await(1, TimeUnit.SECONDS));
		
		holderList = (List<RetryHolder>) HazelcastRetryImpl.getHzInst().getMap("POKE_SLOW").get(id);
		
		Assert.assertNotNull(holderList);
		
		Assert.assertEquals(1, holderList.get(0).getCount());
	}

	// TODO
	@Test
	public void successStat() throws Exception {

	}

	// TODO
	@Test
	public void failStat() throws Exception {

	}

}
