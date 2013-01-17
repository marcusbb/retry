package hazelcast;

import ies.retry.RetryHolder;
import ies.retry.RetryManager;
import ies.retry.spi.hazelcast.HazelcastRetryImpl;

import java.io.IOException;

import com.hazelcast.core.ITopic;
import com.hazelcast.core.Message;
import com.hazelcast.core.MessageListener;

public class TestRetryAdd implements TestRetryAddMBean,  MessageListener<TestBroadcastMsg>{

	HazelcastRetryImpl retryManager;
	//not thread safe
	int blockPrefix = 1;
	String retryType;
	TestCallback callback;
	String topicName = "TEST_TOPIC";
	
	public TestRetryAdd(RetryManager retryManager,String retryType,TestCallback callback) {
		this.retryManager = (HazelcastRetryImpl)retryManager;
		this.retryType = retryType;
		this.callback = callback;
		ITopic<TestBroadcastMsg> topic = this.retryManager.getHzInst().getTopic(topicName);
		topic.addMessageListener(this);
	}
	@Override
	public int getBlockPrefix() {
		return blockPrefix;
	}
	
	
	@Override
	public void onMessage(Message<TestBroadcastMsg> message) {
		System.out.println("Processing : msg");
		TestBroadcastMsg msg = message.getMessageObject();
		callback.setRethrow(msg.rethrow);
		callback.setSleep(msg.sleep);
		callback.setSleepOn(msg.sleepOn);
		callback.setSuccess(msg.success);
		System.out.println("Call back set to: " +  callback);
		
	}
	@Override
	public boolean getCallbackSuccess() {
		return callback.isSuccess();
	}
	@Override
	public void setCallbackSuccess(boolean success) {
		TestCallback newCallback = new TestCallback(callback);
		newCallback.setSuccess(success);
		retryManager.getHzInst().getTopic(topicName).publish(new TestBroadcastMsg(newCallback));
		
	}
	
	
	@Override
	public long getDelay() {
		return callback.getSleep();
	}
	@Override
	public void setDelay(long delay) {
		TestCallback newCallback = new TestCallback(callback);
		newCallback.setSleep(delay);
		retryManager.getHzInst().getTopic(topicName).publish(new TestBroadcastMsg(newCallback));
		
	}
	private void doMakeSuccess(TestBroadcastMsg msg) {
		callback.setSleepOn(msg.sleepOn);
		callback.setSuccess(msg.success);
		callback.setRethrow(msg.rethrow);
	}
	@Override
	public void makeSuccessfulNoDelay() {
		TestCallback successCallback = new TestCallback(true, -1, false);
		retryManager.getHzInst().getTopic(topicName).publish(new TestBroadcastMsg(successCallback));
		
	}
	@Override
	public void addRetry(int num) {
		
		
		
			
			for (int i = 0; i < num; i++) {
				RetryHolder holder = new RetryHolder(blockPrefix + "12334" + i, retryType,
						new IOException("Houston there is a problem"),
						"Useful Serializable object ");
				
				retryManager.addRetry(holder);
			}
			
			blockPrefix++;
		
		
		
	}
	@Override
	public boolean query(String id) {
		
		
				
		return retryManager.getRetry(id,retryType) != null;
			
		
		
	}
	@Override
	public void addRetry() {
		RetryHolder holder = new RetryHolder(blockPrefix + "12334", retryType,
				new IOException("Houston there is a problem"),
				"Useful Serializable object ");
		retryManager.addRetry(holder);
		blockPrefix++;
	}

}
