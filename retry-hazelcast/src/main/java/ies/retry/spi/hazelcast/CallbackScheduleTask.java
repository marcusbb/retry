package ies.retry.spi.hazelcast;

/**
 * 
 * 
 * @author msimonsen
 *
 */
public class CallbackScheduleTask implements Runnable {

	CallbackManager callbackMgr;
	String type;
	
	protected CallbackScheduleTask(CallbackManager callbackMgr,String type) {
		this.callbackMgr = callbackMgr;
		this.type = type;
	}
	
	public void run() {
		callbackMgr.tryDequeue(type);
		
	}

}
