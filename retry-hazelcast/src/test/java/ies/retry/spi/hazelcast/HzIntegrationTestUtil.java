package ies.retry.spi.hazelcast;

import com.hazelcast.core.HazelcastInstance;

import ies.retry.Retry;



public abstract class HzIntegrationTestUtil {


	
	public static void beforeClass()  {
		/*HazelcastInstance inst = ((HazelcastRetryImpl)Retry.getRetryManager()).getH1();
		if (inst!= null) {
			inst.getLifecycleService().shutdown();
			((HazelcastRetryImpl)Retry.getRetryManager()).setH1(null);
		}*/
	}
	
	
	public static void afterClass()  {
		HazelcastInstance inst = ((HazelcastRetryImpl)Retry.getRetryManager()).getH1();
		if (inst != null &&
			inst.getLifecycleService().isRunning() ) {
			inst.getLifecycleService().shutdown();
		}
		((HazelcastRetryImpl)Retry.getRetryManager()).setH1(null);
		Retry.setRetryManager(null);
	}
}
