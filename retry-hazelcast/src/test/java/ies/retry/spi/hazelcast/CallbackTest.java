package ies.retry.spi.hazelcast;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import junit.framework.Assert;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class CallbackTest {

	static HazelcastRetryImpl retryManager;
	
	@BeforeClass
	public static void beforeClass() {
		HzIntegrationTestUtil.beforeClass();
		retryManager = new HazelcastRetryImpl();
	}
	@AfterClass
	public static void afterClass() {
		retryManager.shutdown();
		HzIntegrationTestUtil.afterClass();
	}
	@Test
	public void concurrentLockAccess() {
		
		final CallbackManager callbackManager = retryManager.getCallbackManager();
		
		ExecutorService execService = Executors.newCachedThreadPool();
		
		for (int i=0;i<5;i++) {
			execService.submit(new AttemptCallBack(i == 0, callbackManager) );
		}
		//Should see no concurrent access to the try dequeue
		
	}
	
	
}
class AttemptCallBack implements Runnable {

	boolean expectDequeue;
	CallbackManager callbackMgr;
	
	AttemptCallBack(boolean expect,CallbackManager callBackManager) {
		this.expectDequeue = expect;
		this.callbackMgr = callBackManager;
	}
	@Override
	public void run() {
		boolean dequeued = callbackMgr.tryDequeue("POKE");
		System.out.println("Dequeued: " + dequeued);
		Assert.assertEquals(expectDequeue, dequeued );
		
	}
	
}
