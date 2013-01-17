package ies.retry.spi.hazelcast;

import ies.retry.Retry;
import ies.retry.RetryConfiguration;
import ies.retry.RetryHolder;
import ies.retry.RetryState;
import ies.retry.RetryTransitionEvent;
import ies.retry.spi.hazelcast.config.HazelcastXmlConfig;
import ies.retry.spi.hazelcast.disttasks.TryDequeueEvent;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import provision.services.logging.Logger;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.core.ITopic;
import com.hazelcast.core.Member;

/**
 * Makes periodic checks against the grid to "extra" safety 
 * from a de-queueing perspective.
 * 
 * Makes 2 checks:
 * - member lost events -in addition to state manager event processing
 * - overall map counts
 * 
 * Deprecating, as it serves very little practical use.
 * 
 * @author msimonsen
 *
 */
@Deprecated
public class GridHealthCheck {
	public static final String CALLER = GridHealthCheck.class.getName();
	public static final String TOPIC_NAME = "healthCheck";
	private ScheduledThreadPoolExecutor stpe = null;
	
	private StateManager stateManager;
	
	private HazelcastXmlConfig config;
	
	private ITopic<TryDequeueEvent> topic = null;
	
	public GridHealthCheck(StateManager stateManager) {
		this.stateManager = stateManager;
		
		topic = ((HazelcastRetryImpl)Retry.getRetryManager()).getH1().getTopic (TOPIC_NAME);
		this.config = stateManager.getGlobalConfig();
	}
	
	public void init() {
		if (config.getHealthCheckConfig().isEnabled()) {
			stpe = new ScheduledThreadPoolExecutor(1);
			
			int period = config.getHealthCheckConfig().getCheckInterval();
			TimeUnit tu = config.getHealthCheckConfig().getTimeUnit();
			stpe.scheduleAtFixedRate(new GridSizeCheck(stateManager), period, period, tu);
			
			period = config.getHealthCheckConfig().getCheckMemeberInterval();
			stpe.scheduleAtFixedRate(new MemberCheck(((HazelcastRetryImpl)Retry.getRetryManager()).getH1(), stateManager), period, period, TimeUnit.MILLISECONDS);
		}
			
	}
	public void shutdown() {
		if (stpe != null)
			stpe.shutdown();
	}

	
	
	
	
}
class MemberCheck implements Runnable {

	StateManager stateManager = null;
	HazelcastInstance hzInst = null;
	Set<Member> lastMembers = null;
	
	static String CALLER = MemberCheck.class.getName();
	
	public MemberCheck(HazelcastInstance hzInst,StateManager stateMgr) {
		this.stateManager = stateMgr;
		this.hzInst = hzInst;
		this.lastMembers = hzInst.getCluster().getMembers();
	}
	@Override
	public void run() {
		
		Set<Member> curMembers = hzInst.getCluster().getMembers();
		
		if (curMembers.size() < lastMembers.size()) {
			Logger.warn(CALLER, "MemberCheck_Lost_Member");
			stateManager.setMemberLostEvent(true);
			stateManager.setMaster();
		}
		if (curMembers.size() > lastMembers.size()) {
			stateManager.setMaster();
			
		}
		lastMembers = curMembers;
	}
	
}

class GridSizeCheck implements Runnable {
	public static final String CALLER = GridSizeCheck.class.getName();
	private  StateManager stateManager;
	
	
	public GridSizeCheck(final StateManager stateManager) {
		this.stateManager = stateManager;
		
	}
	@Override
	public void run() {
		//stateManager.setMaster();
		
//		if (stateManager.isMaster()) {
			
			Logger.debug(GridHealthCheck.CALLER, "Health_Check", "GRID HEALTH CHECK");
			try {
				HazelcastRetryImpl retryManager = (HazelcastRetryImpl)Retry.getRetryManager();
				Collection<RetryConfiguration> retrySet = retryManager.getConfigManager().getConfigMap().values();
				for (RetryConfiguration retryConfig: retrySet) {
					Logger.debug(CALLER, "Check_Retry_Type: " + retryConfig.getType());
					IMap<String,List<RetryHolder>>  retryMap = ((HazelcastRetryImpl)Retry.getRetryManager()).getH1().getMap(retryConfig.getType());
					if (retryMap.localKeySet().size()>0) {
						Logger.info(GridHealthCheck.CALLER, "Health_Check_Items_Found", "Found items in the grid, broadcasting", "Type", retryConfig.getType());
						//ITopic<TryDequeueEvent> topic = HazelcastRetryImpl.getHzInst().getTopic (GridHealthCheck.TOPIC_NAME);
						//topic.publish(new TryDequeueEvent(retryConfig.getType()));
						//Logger.debug(CALLER,"CHECK_statemgr " + retryManager.getStateMgr());
						Logger.debug(CALLER,"Check_Retry_Type","","type",retryConfig.getType(),"state",retryManager.getStateMgr().getState(retryConfig.getType())  );
						if (retryManager.getStateMgr().getState(retryConfig.getType()) != RetryState.SUSPENDED) {
							Logger.info(GridHealthCheck.CALLER, "Health_check_state", "", "Type", retryConfig.getType());
							
							//retryManager.getStateMgr().publish(new RetryTransitionEvent(RetryState.QUEUED, retryConfig.getType()));
							retryManager.getCallbackManager().tryDequeue(retryConfig.getType());
						} else {
							Logger.info(GridHealthCheck.CALLER, "Health_check_state_suspended", "IS suspended", "Type", retryConfig.getType());
						}
					}
				}
			}catch (Exception e) {
				Logger.error(GridSizeCheck.CALLER, "Health_Check_Exception", "Exception Message: " + e.getMessage(),e);
			}
		}
		
	//}
	
}