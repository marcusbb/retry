package ies.retry.spi.hazelcast;

import ies.retry.RetryCallback;
import ies.retry.RetryHolder;

import java.util.concurrent.CountDownLatch;

public class LatchCallback implements RetryCallback {

	private CountDownLatch latch = null;
	private boolean success;
	
	public LatchCallback(CountDownLatch latch,boolean success) {
		this.latch = latch;
		this.success = success;
	}
	public boolean onEvent(RetryHolder retry) throws Exception {
		
		latch.countDown();
		return success;
	}

}
