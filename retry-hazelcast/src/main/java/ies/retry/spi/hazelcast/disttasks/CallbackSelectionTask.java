package ies.retry.spi.hazelcast.disttasks;

import ies.retry.Retry;
import ies.retry.spi.hazelcast.HazelcastRetryImpl;

import java.io.Serializable;
import java.util.concurrent.Callable;


import com.hazelcast.core.Member;

public class CallbackSelectionTask implements Callable<CallbackRegistration>,Serializable {

	private static org.slf4j.Logger logger =  org.slf4j.LoggerFactory.getLogger(HazelcastRetryImpl.class);
	
	private static final long serialVersionUID = 6712682439409108725L;
	String type;
	
	public CallbackSelectionTask(String type) {
		this.type = type;
	}
	@Override
	public CallbackRegistration call() throws Exception {
		logger.info( "Callback_Selection_Task_Call");
		Member localMember = ((HazelcastRetryImpl)Retry.getRetryManager()).getH1().getCluster().getLocalMember();
		CallbackRegistration registration = new CallbackRegistration(localMember, false);
		if (Retry.getRetryManager().registeredCallback(type)!= null) {
			registration.setRegistered(true);
			logger.info( "Callback_Selection_Task_Call: member={}",  localMember);
		}
		return registration;
			
		
	}
	

}
