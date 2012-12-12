package ies.retry.spi.hazelcast.disttasks;

import ies.retry.Retry;
import ies.retry.spi.hazelcast.HazelcastRetryImpl;

import java.io.Serializable;
import java.util.concurrent.Callable;

import provision.services.logging.Logger;

import com.hazelcast.core.Member;

public class CallbackSelectionTask implements Callable<CallbackRegistration>,Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6712682439409108725L;
	String type;
	static String CALLER = CallbackSelectionTask.class.getName();
	
	public CallbackSelectionTask(String type) {
		this.type = type;
	}
	@Override
	public CallbackRegistration call() throws Exception {
		Logger.info(CALLER, "Callback_Selection_Task_Call", "Selecting callback registration");
		Member localMember = HazelcastRetryImpl.getHzInst().getCluster().getLocalMember();
		CallbackRegistration registration = new CallbackRegistration(localMember, false);
		if (Retry.getRetryManager().registeredCallback(type)!= null) {
			registration.setRegistered(true);
			Logger.info(CALLER, "Callback_Selection_Task_Call", "Registered callback on " + localMember);
		}
		return registration;
			
		
	}
	

}
