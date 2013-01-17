package ies.retry.spi.hazelcast.disttasks;

import ies.retry.BackOff;
import ies.retry.Retry;
import ies.retry.RetryCallback;
import ies.retry.RetryConfiguration;
import ies.retry.RetryHolder;
import ies.retry.spi.hazelcast.CallbackStat;
import ies.retry.spi.hazelcast.HazelcastRetryImpl;
import ies.retry.spi.hazelcast.NoCallbackRegistered;
import ies.retry.spi.hazelcast.persistence.RetryMapStoreFactory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import provision.services.logging.Logger;

import com.hazelcast.core.IMap;


/**
 * Makes a distributed callback
 * 
 * TODO: more verbose logging.
 * 
 * @author msimonsen
 *
 */
public class DistCallBackTask implements Callable<CallbackStat>,Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private List<RetryHolder> listHolder;
	private static String CALLER = DistCallBackTask.class.getName();
	private boolean archiveRetries;
	
	public DistCallBackTask(List<RetryHolder> listHolder, boolean archiveRetries) {
		this.listHolder = listHolder;
		this.archiveRetries = archiveRetries;
		
	}
	@Override
	public CallbackStat call() throws Exception {
		CallbackStat stat = new CallbackStat(true);
		IMap<String,List<RetryHolder>> retryMap = null;
		String id = listHolder.get(0).getId();
		String type = listHolder.get(0).getType();
		
		if (id ==null && type ==null) {
			Logger.error(CALLER, "Dist_Callback_Missing_Input", "id or type is null", "ID", id, "TYPE", type);
			return stat;
		}
		
		try {
			//debug to remove
			/*for (RetryHolder dh:listHolder) {
				Logger.debug(CALLER, "Callback_Examine_All_Retry: "+dh);
			}*/
			
			HazelcastRetryImpl retryImpl = ((HazelcastRetryImpl)Retry.getRetryManager());
			RetryConfiguration config = retryImpl.getConfigManager().getConfiguration(type);
			RetryCallback callback = retryImpl.getCallbackManager().getCallbackMap().get(type);
			retryMap = retryImpl.getH1().getMap(type);
			if (callback == null){
				Logger.error(CALLER, "Null_Callback","Callback was not set for type " + type,"ID",id);
				throw new NoCallbackRegistered();
			}
			
			BackOff backOff = config.getBackOff();
			
			List<RetryHolder> listHolder = retryMap.get(id);
			//retryMap.lock(id);
			boolean exec = false;
			long curTime = System.currentTimeMillis();
			
			if (listHolder == null) {
				Logger.error(CALLER, "Null_List_Holder","","ID",id);
				stat.setSuccess(false);
			} else {
				RetryHolder firstHolder = listHolder.get(0);
				
				List<RetryHolder> failedHolder = new ArrayList<RetryHolder>();
				
					
					if (firstHolder.getNextAttempt() <= curTime) {
						exec = true;
						
						int i = 0;
						
						//this is to mark the different behaviour between throwing an exception and returning false(MS-please examine if this behaviour difference is desired), while preventing an 
						//exception failure from overwriting all remaining retries from the loop like when the break; was in the exception catch block and the only failed retry 
						//ended up overwriting the rest of the untried items in the list.
						boolean skipCallbackForRemainingItemsDueToException = false;
						for (i=0;i<listHolder.size();i++) {
							RetryHolder holder = listHolder.get(i);
							try {
								//this is the potentially VERY expensive operation
								//the actual callback portion
								if(!skipCallbackForRemainingItemsDueToException){
									//try the next holder in the list only if the callback didn't throw an exceptoin for the previously executed retries in the list. 
									stat.setSuccess(callback.onEvent(holder));
								}
								
								if (!stat.isSuccess())
									failedHolder.add(holder);
								
									
							}catch (Exception e) {	
								skipCallbackForRemainingItemsDueToException = true;
								stat.setSuccess(false);
								failedHolder.add(holder);
								//removing the break so that exception continues the loop to add the remaining items to failedHolder list so the untried items don't get wiped out. The only difference is 
								//I wont allow the callback to be executed as soon as the first exception is caught as the code was doing before this fix. 
								//break;
							}
							
						}
						
						if (failedHolder.size() == 0) {
							Logger.info(CALLER, "Retry_Callback_Sucess: ID=" + id);
							retryMap.lock(id);
							retryMap.remove(id);
							RetryMapStoreFactory.getInstance().newMapStore(type).delete(id);
						} 
						else {
							stat.setSuccess(false);
						}
						
					}
				/*for (RetryHolder fh:failedHolder) {
					Logger.debug(CALLER, "Examine_failed_retry: " + fh);
				}*/
				if (!stat.isSuccess() && exec) {
						retryMap.lock(id);
						
						firstHolder = failedHolder.get(0);
						if (firstHolder.getCount()>= backOff.getMaxAttempts()) {
							stat.setSuccess(false);
							retryMap.remove(id);
							if(!archiveRetries)
								RetryMapStoreFactory.getInstance().newMapStore(type).delete(id);
							else 
								RetryMapStoreFactory.getInstance().newMapStore(type).archive(id);
							Logger.warn(CALLER, "Dist_Callback_Retry_Failed", "Failed to retry: " + firstHolder);
						} else {
							//we separate the timer from the calculation of next
							//set all of them to be safe (and help in query)
							for (RetryHolder fh:failedHolder) {
								fh.incrementCount();
								int nextTs = Math.round( backOff.getMilliInterval()  );
								fh.setNextAttempt(System.currentTimeMillis() + nextTs);
							}
							
							retryMap.put(id, failedHolder);
						}
						stat.setCount(firstHolder.getCount());
						stat.setDateCreated(firstHolder.getSystemTs());
				
				}
			}
		
		}catch(Exception e) {
			//e.printStackTrace();
			stat.setSuccess(false);
			Logger.error(CALLER, "Dist_Callback_Task_Call_Exception", "Exception Message: " + e.getMessage(), "ID", id, "TYPE", type, e);
		} finally {
			if (retryMap !=null) {
				retryMap.unlock(id);
			}
		}
		return stat;
	}

}
