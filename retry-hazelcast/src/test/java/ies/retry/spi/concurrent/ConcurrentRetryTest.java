package ies.retry.spi.concurrent;

import ies.retry.Retry;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;



import org.junit.Assert;
import org.junit.Test;

import com.hazelcast.core.Hazelcast;

public class ConcurrentRetryTest {

	@Test
	public void testInitOnceOnly() throws Exception {
		ExecutorService exec = Executors.newCachedThreadPool();
		
		for (int i=0;i<100;i++) {
			exec.submit(new Runnable() {
				
				@Override
				public void run() {
					Retry.getRetryManager();
					
				}
			});
			
		}
		//finally block to ensure we're up
		Retry.getRetryManager();
		
		Assert.assertTrue(Hazelcast.getAllHazelcastInstances().size() ==1);
		
		
	}

}
