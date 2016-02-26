package hazelcast;

import ies.retry.RetryCallback;
import ies.retry.RetryHolder;

import java.util.Random;

public class TestCallback implements RetryCallback {

	private boolean sleepOn = false;
	private long sleep = -1;
	private boolean success = true;
	
	private boolean rethrow = false;
	
	private Random rand;
	
	public TestCallback() {}
	
	public TestCallback(TestCallback callback) {
		this.sleep = callback.sleep;
		this.sleepOn = callback.sleepOn;
		this.success = callback.success;
		this.rethrow = callback.rethrow;
	}
	
	public TestCallback(boolean success, long sleep) {
		if (sleep > 0) {
			this.sleepOn = true;
			this.sleep = sleep;
		}
		this.success = success;
	}
	
	public TestCallback(boolean success, long sleep,boolean rethrow) {
		this(success,sleep);
		this.rethrow = rethrow;
	}
	
	public TestCallback(boolean success, int minSleep,int maxSleep) {
		this.sleepOn = true;
	
		this.rand = new Random();
		this.sleep = minSleep + rand.nextInt(maxSleep + minSleep);
		this.success = success;
	}
	
	public boolean onEvent(RetryHolder holder) throws Exception {
		
		//Logger.debug(getClass().getName(),"SleepON: " + sleepOn + "Sleeping for: " + sleep);
		if (sleepOn)
			Thread.sleep(sleep);
		
		if (rethrow)
			throw holder.getException();
		
		return success;
	}

	public boolean isSleepOn() {
		return sleepOn;
	}

	public void setSleepOn(boolean sleepOn) {
		this.sleepOn = sleepOn;
	}

	public long getSleep() {
		return sleep;
	}

	public void setSleep(long sleep) {
		this.sleep = sleep;
	}

	public boolean isSuccess() {
		return success;
	}

	public void setSuccess(boolean success) {
		this.success = success;
	}

	public boolean isRethrow() {
		return rethrow;
	}

	public void setRethrow(boolean rethrow) {
		this.rethrow = rethrow;
	}

	public Random getRand() {
		return rand;
	}

	public void setRand(Random rand) {
		this.rand = rand;
	}

	@Override
	public String toString() {
		return "TestCallback [sleepOn=" + sleepOn + ", sleep=" + sleep
				+ ", success=" + success + ", rethrow=" + rethrow + ", rand="
				+ rand + "]";
	}
	
	
}
