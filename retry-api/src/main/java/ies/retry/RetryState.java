package ies.retry;

import java.io.Serializable;

public enum RetryState implements Serializable {
	
	QUEUED,DRAINED,SUSPENDED; 
}
