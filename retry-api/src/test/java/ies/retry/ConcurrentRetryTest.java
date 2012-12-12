package ies.retry;

import ies.retry.xml.XMLRetryConfigMgr;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ConcurrentRetryTest {

	
	@Test
	public void concurrentWithCachedThreadsAccess() throws Exception {
		String lastXML = XMLRetryConfigMgr.getXML_FILE();
		XMLRetryConfigMgr.setXML_FILE("retry_config.xml");
		ExecutorService execService = Executors.newCachedThreadPool();
		
		HashSet<RetryManager> retryManagers = new HashSet<RetryManager>();
		
		
		ArrayList<Future<RetryManager>> futures = new ArrayList<Future<RetryManager>>();
		for (int i=0;i<10;i++) {
			futures.add(execService.submit(
				new GetRetryManagerTask()
			) );
		}
		for (Future<RetryManager> future:futures) {
			RetryManager retrymanager = future.get();
			System.out.println("RetryManager: " + retrymanager);
			retryManagers.add(retrymanager);
		}
		execService.shutdown();
		execService.awaitTermination(10, TimeUnit.SECONDS);
		
		Assert.assertEquals(1, retryManagers.size());
		
		XMLRetryConfigMgr.setXML_FILE(lastXML);
	}
	@Test
	public void concurrentWithFixedThread() throws Exception {
		String lastXML = XMLRetryConfigMgr.getXML_FILE();
		XMLRetryConfigMgr.setXML_FILE("retry_config.xml");
		ExecutorService execService = Executors.newFixedThreadPool(5);
		
		HashSet<RetryManager> retryManagers = new HashSet<RetryManager>();
		
		
		ArrayList<Future<RetryManager>> futures = new ArrayList<Future<RetryManager>>();
		for (int i=0;i<10;i++) {
			futures.add(execService.submit(
				new GetRetryManagerTask()
			) );
		}
		for (Future<RetryManager> future:futures) {
			RetryManager retrymanager = future.get();
			System.out.println("RetryManager: " + retrymanager);
			retryManagers.add(retrymanager);
		}
		execService.shutdown();
		execService.awaitTermination(10, TimeUnit.SECONDS);
		
		Assert.assertEquals(1, retryManagers.size());
		
		XMLRetryConfigMgr.setXML_FILE(lastXML);
	}
	
}

