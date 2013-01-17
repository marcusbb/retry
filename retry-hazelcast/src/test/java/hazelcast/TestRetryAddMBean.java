package hazelcast;

public interface TestRetryAddMBean {

	public void addRetry();
	
	public void addRetry(int num);
	
	public void addRetry(int num,boolean withException);
	
	public int getBlockPrefix();
	
	public boolean getCallbackSuccess();
	
	public void setCallbackSuccess(boolean success);
	
	public void makeSuccessfulNoDelay();
}
