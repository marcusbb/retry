package hazelcast;

import ies.retry.RetryHolder;
import ies.retry.RetryManager;

import java.io.IOException;

public class TestRetryAdd implements TestRetryAddMBean{

	RetryManager retryManager;
	//not thread safe
	int blockPrefix = 1;
	String retryType;
	TestCallback callback;
	
	public TestRetryAdd(RetryManager retryManager,String retryType,TestCallback callback) {
		this.retryManager = retryManager;
		this.retryType = retryType;
		this.callback = callback;
	}
	@Override
	public int getBlockPrefix() {
		return blockPrefix;
	}
	
	
	@Override
	public boolean getCallbackSuccess() {
		return callback.isSuccess();
	}
	@Override
	public void setCallbackSuccess(boolean success) {
		callback.setSuccess(success);
		
	}
	
	@Override
	public void makeSuccessfulNoDelay() {
		callback.setSleepOn(false);
		callback.setSuccess(true);
		callback.setRethrow(false);
		
	}
	@Override
	public void addRetry(int num) {
		
		
		Exception e = new IOException("Houston there is a problem");
			
			for (int i = 0; i < num; i++) {
				RetryHolder holder = new RetryHolder(blockPrefix + "12334" + i, retryType,
						e,
						"Useful Serializable object ");
				retryManager.addRetry(holder);
			}
			
			blockPrefix++;
		
			
		
		
	}
	@Override
	public void addRetry(int num,boolean withException) {
		
		
		Exception e = null;
		
		if (withException)
			e = new IOException("Houston there is a problem");
			for (int i = 0; i < num; i++) {
				RetryHolder holder = new RetryHolder(blockPrefix + "12334" + i, retryType,
						e,
						"Useful Serializable object ");
				retryManager.addRetry(holder);
			}
			
			blockPrefix++;
		
		
		
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
