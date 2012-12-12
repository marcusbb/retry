package ies.retry;

import java.io.Serializable;

/**
 * the main classification of major global state transitions in 
 * for the retry manager.
 * 
 * Allowed state changes:
 * DRAINED -> QUEUED
 * DRAINED -> SUSPENDED
 * QUEUED -> SUSPENDED
 * QUEUED -> DRAINED
 * SUSPENDED -> QUEUED
 * SUSPENDED -> DRAINED
 * 
 * @author msimonsen
 *
 */
public class RetryTransitionEvent implements Serializable {

	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1226377412695765614L;
	
	
	public RetryTransitionEvent(){
		
	}
	public RetryTransitionEvent(RetryState type,String retryType) {
		this.retryState = type;
		this.retryType = retryType;
	}
	
	RetryState retryState = RetryState.DRAINED;
	
	String retryType;


	
	public String getRetryType() {
		return retryType;
	}
	public void setRetryType(String retryType) {
		this.retryType = retryType;
	}
	public RetryState getRetryState() {
		return retryState;
	}
	public void setRetryState(RetryState retryState) {
		this.retryState = retryState;
	}
	
	
	
	
}
