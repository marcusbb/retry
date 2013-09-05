package hazelcast;

public interface TestRetryOpsMBean {

	public void addRetry(String id);
	
	public void addRetry();
	
	public void addRetry(int num);
	
	public boolean query(String id);
	
	public boolean getOp(String id) throws Exception;
	
	public int getBlockPrefix();
	
	public boolean getCallbackSuccess();
	
	public void setCallbackSuccess(boolean success);
	
	public long getDelay();
	
	public void setDelay(long delay);
	
	public void makeSuccessfulNoDelay();
	
	public void lock(String id);
	
	public void unlock(String id);
}
