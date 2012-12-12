package ies.retry.spi.hazelcast;

import ies.retry.Retry;



public abstract class HzIntegrationTestUtil {


	
	public static void beforeClass()  {
		
		if (HazelcastRetryImpl.getH1()!= null) {
			HazelcastRetryImpl.getH1().getLifecycleService().shutdown();
			HazelcastRetryImpl.setH1(null);
		}
	}
	
	
	public static void afterClass()  {
		
		if (HazelcastRetryImpl.getH1() != null &&
			HazelcastRetryImpl.getH1().getLifecycleService().isRunning() ) {
			HazelcastRetryImpl.getH1().getLifecycleService().shutdown();
		}
		HazelcastRetryImpl.setH1(null);
		Retry.setRetryManager(null);
	}
}
