package ies.retry.spi.hazelcast;

import static org.junit.Assert.*;
import ies.retry.Retry;
import ies.retry.RetryHolder;

import org.junit.Test;

public class LocalQueuerTest {

	@Test
	public void testAdd() {
		HazelcastRetryImpl retryManager = (HazelcastRetryImpl) Retry.getRetryManager();
		LocalQueuer queuer = new LocalQueuerImpl(retryManager.getH1(), retryManager.configMgr);
		
		RetryHolder holder = new RetryHolder("id", "POKE",new Exception(),"Object");
		
		queuer.add(holder);
		//TODO: shutdown
	}

}
