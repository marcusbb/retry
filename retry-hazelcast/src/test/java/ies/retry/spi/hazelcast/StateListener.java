package ies.retry.spi.hazelcast;

import ies.retry.RetryTransitionEvent;
import ies.retry.RetryTransitionListener;

import java.util.concurrent.CountDownLatch;

public class StateListener implements RetryTransitionListener {

	CountDownLatch latch = null;
	
	public StateListener(CountDownLatch latch) {
		this.latch = latch;
	}
	@Override
	public void onEvent(RetryTransitionEvent event) {
		// TODO Auto-generated method stub
		
		latch.countDown();
	}
	public CountDownLatch getLatch() {
		return latch;
	}
	public void setLatch(CountDownLatch latch) {
		this.latch = latch;
	}

	
}
