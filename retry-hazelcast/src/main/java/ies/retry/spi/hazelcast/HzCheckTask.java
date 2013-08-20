package ies.retry.spi.hazelcast;

import java.util.concurrent.Callable;

import com.hazelcast.core.HazelcastInstance;

public class HzCheckTask implements Callable<HzState> {

	HazelcastInstance hzInst;
	
	@Override
	public HzState call() throws Exception {
		hzInst.getLifecycleService().isRunning();
		
		return null;
	}
	
	
	

}
