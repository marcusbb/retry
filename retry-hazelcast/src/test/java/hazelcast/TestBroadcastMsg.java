package hazelcast;

import java.io.Serializable;

public class TestBroadcastMsg implements Serializable {

	
	public boolean sleepOn = false;
	public long sleep = -1;
	public boolean success = true;
	
	public boolean rethrow = false;

	public TestBroadcastMsg(TestCallback callback) {
		this.sleep = callback.getSleep();
		this.sleepOn = callback.isSleepOn();
		this.success = callback.isSuccess();
		this.rethrow = callback.isRethrow();
	}
	
	
	
	
}
