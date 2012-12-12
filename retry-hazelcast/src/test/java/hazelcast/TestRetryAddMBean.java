package hazelcast;

public interface TestRetryAddMBean {

	public void addRetry();
	
	public void addRetry(int num);
	
	public int getBlockPrefix();
	
	public boolean getCallbackSuccess();
	
	public void setCallbackSuccess(boolean success);
	
	public void makeSuccessfulNoDelay();
}
