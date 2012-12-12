package ies.retry.spi.hazelcast;

import java.io.Serializable;

/**
 * The return contract from call back process.
 * 
 * @author msimonsen
 *
 */
public class CallbackStat implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4776921638782787900L;

	private boolean success = true;
	
	//
	//if not successful
	private int count;
	
	private long dateCreated;
	
	public CallbackStat(boolean success) {
		this.success = success;
	}
	
	public CallbackStat(boolean success,int count, long dateCreated) {
		this(success);
		this.count = count;
		this.dateCreated = dateCreated;
	}

	public boolean isSuccess() {
		return success;
	}

	public void setSuccess(boolean success) {
		this.success = success;
	}

	public int getCount() {
		return count;
	}

	public void setCount(int count) {
		this.count = count;
	}

	public long getDateCreated() {
		return dateCreated;
	}

	public void setDateCreated(long dateCreated) {
		this.dateCreated = dateCreated;
	}
	
	
}
