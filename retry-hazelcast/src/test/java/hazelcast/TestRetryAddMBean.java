package hazelcast;

public interface TestRetryAddMBean {

	public void addRetry();
	
	public void addRetry(int num);
	
	public boolean query(String id);
	
	public int getBlockPrefix();
	
	public boolean getCallbackSuccess();
	
	public void setCallbackSuccess(boolean success);
	
	public long getDelay();
	
	public void setDelay(long delay);
	
	public void makeSuccessfulNoDelay();
}
