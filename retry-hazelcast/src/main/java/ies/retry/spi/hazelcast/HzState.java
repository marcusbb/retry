package ies.retry.spi.hazelcast;

public enum HzState {

	
	
	RUNNING, //it's started normally and 
	INACTIVE_UNGRACEFUL, //it's inactive but not due to graceful shutdown
	INACTIVE_GRACEFUL,
	INACTIVE_STARTING;
	
	
	
}
