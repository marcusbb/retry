package ies.retry.spi.hazelcast.disttasks;

import ies.retry.Retry;
import ies.retry.RetryState;
import ies.retry.RetryTransitionEvent;
import ies.retry.spi.hazelcast.HazelcastRetryImpl;

import java.io.Serializable;
import java.util.concurrent.Callable;

/**
 * Broadcasts state, and returns the nodes current state.
 * 
 * @author msimonsen
 *
 */
public class BroadcastStateTask implements Serializable,Callable<BroadcastState>{

	/**
	 * 
	 */
	private static final long serialVersionUID = -3558953507272310381L;
	private RetryTransitionEvent event;
	
	public BroadcastStateTask(RetryTransitionEvent event) {
		this.event = event;
	}
	@Override
	public BroadcastState call() throws Exception {
		HazelcastRetryImpl retryManager = (HazelcastRetryImpl)Retry.getRetryManager();
		retryManager.getStateMgr().notifyStateListeners(event);
		RetryState curState = retryManager.getStateMgr().getState(event.getRetryType());
		return new BroadcastState(curState,HazelcastRetryImpl.getHzInst().getCluster().getLocalMember());
		
		
		
	}

}

