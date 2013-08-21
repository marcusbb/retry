package ies.retry.spi.hazelcast;

import ies.retry.Retry;
import ies.retry.RetryCallback;
import ies.retry.RetryHolder;
import ies.retry.RetryManager;
import ies.retry.spi.hazelcast.jmx.RetryManagement;
import ies.retry.spi.hazelcast.util.HzUtil;
import ies.retry.xml.XMLRetryConfigMgr;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import junit.framework.Assert;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class ConcurrentOpsIntegrationTest {

	private static RetryManager retryManager;
	private static RetryManagement management;
	private static StateManager stateManager;
	//private static int numRetryTypes = 3;
	private static TestCallBack callBack;
	private static String type = "POKE";
	
	@BeforeClass
	public static void beforeClass() throws Exception {
		HzIntegrationTestUtil.beforeClass();
		XMLRetryConfigMgr.setXML_FILE("retry_config_persistence.xml");
		HzUtil.HZ_CONFIG_FILE = "hz-mgmt-integration.xml";

		retryManager = Retry.getRetryManager();
		stateManager = ((HazelcastRetryImpl)retryManager).getStateMgr();
		//retryManager.registerCallback(new SuccessCallback(), "poke");
		
		management =  new RetryManagement();
		management.init((HazelcastRetryImpl)retryManager);
		
		MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
		mbs.registerMBean(management, new ObjectName( "retry:retry=hazelcast-retry"));
		callBack = new TestCallBack(true);
		retryManager.registerCallback(callBack, "POKE");
		
	}
	@AfterClass
	public static void afterClass() throws Exception {
		
		retryManager.shutdown();
		HzIntegrationTestUtil.afterClass();
		MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
		mbs.unregisterMBean( new ObjectName( "retry:retry=hazelcast-retry"));
	}
	@Before
	public void before() {
		
	}
	
	@Test
	public void synchronousAddSameId() {
			
		
		int numOps = 10;
		Random rand = new Random();
		String id = "" + rand.nextInt();
		
		try {
			for (int i=0;i<numOps;i++) {
				RetryHolder holder = new RetryHolder(id,type,
						new IOException("Houston there is a problem"),
						"Useful Serializable object ");		
				retryManager.addRetry(holder);
			}
			List<RetryHolder> list = retryManager.getRetry(id, type);
			System.out.println("size: " + list.size());
			//Assert.assertEquals(numOps, list.size());
			retryManager.removeRetry(id, type);
			
			//again:
			for (int i=0;i<numOps;i++) {
				RetryHolder holder = new RetryHolder(id,type,
						new IOException("Houston there is a problem"),
						"Useful Serializable object ");		
				retryManager.addRetry(holder);
			}
			list = retryManager.getRetry(id, type);
			System.out.println("size: " + list.size());
			Assert.assertEquals(numOps, list.size());
		}finally {
			retryManager.removeRetry(id, type);
		}
		
	}
	
	
	@Test
	public void concurrentAddDiffIds() throws Exception {
		
		List<RetryHolder> list = testConcurrentAdd(5,null);
		System.out.println("list size: " + list.size());
		retryManager.removeRetry(list.get(0).getId(), type);
		
		list = testConcurrentAdd(5,null);
		System.out.println("list size: " + list.size());
		retryManager.removeRetry(list.get(0).getId(), type);
		
		list = testConcurrentAdd(10,null);
		System.out.println("list size: " + list.size());
		retryManager.removeRetry(list.get(0).getId(), type);
		
		list = testConcurrentAdd(20,null);
		System.out.println("list size: " + list.size());
		retryManager.removeRetry(list.get(0).getId(), type);
		
		list = testConcurrentAdd(20,null);
		System.out.println("list size: " + list.size());
		retryManager.removeRetry(list.get(0).getId(), type);
	}
	
	//@Test
	public void concurrentAddSameIds() throws Exception {
		
		Random rand = new Random();
		String id = "" + rand.nextInt();
		 RetryHolder nHolder = new RetryHolder(id,"POKE",
					new IOException("Houston there is a problem"),
					"Useful Serializable object ");
		 
		List<RetryHolder> list = testConcurrentAdd(5,nHolder);
		System.out.println("list size: " + list.size());
		
		list = testConcurrentAdd(5,nHolder);
		System.out.println("list size: " + list.size());
		
		
		
	}
	
	private List<RetryHolder> testConcurrentAdd(int num,RetryHolder holder) throws Exception {
		int numOps = num;
		Random rand = new Random();
		String id = "" + rand.nextInt();
		RetryHolder nHolder = holder;
		if (nHolder == null) {
			 nHolder = new RetryHolder(id,"POKE",
				new IOException("Houston there is a problem"),
				"Useful Serializable object ");
		}
		final RetryHolder holderRef = nHolder;
		ExecutorService execService = Executors.newCachedThreadPool();
		ArrayList<Future<?>> futures = new ArrayList<Future<?>>();
		for (int i=0;i<numOps;i++) {
			futures.add(execService.submit(
					new Runnable() {
	
						@Override
						public void run() {
							
							retryManager.addRetry(holderRef);
							
						}
				
					}
				));
		}
		for (Future<?> future:futures) {
			future.get();
		}
		//we should have exactly the same number of retries
		List<RetryHolder> retVal = null;
		if (holder == null)
			 retVal = retryManager.getRetry(id, type);
		else
			retVal = retryManager.getRetry(holder.getType(),holder.getType() );
		//System.out.println("ret " + retVal);
		
		return retVal;
		
	}
	
	private static class TestCallBack implements RetryCallback {

		private boolean retVal = false;
		
		public TestCallBack(boolean retVal) {
			this.retVal = retVal;
		}
		@Override
		public boolean onEvent(RetryHolder retry) throws Exception {
			return retVal;
		}
		
	}
		
}
	

	




