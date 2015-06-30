package ies.retry.spi.hazelcast;

import ies.retry.BackOff;
import ies.retry.Retry;
import ies.retry.RetryCallback;
import ies.retry.RetryConfiguration;
import ies.retry.RetryHolder;
import ies.retry.spi.hazelcast.config.HazelcastConfigManager;
import ies.retry.spi.hazelcast.disttasks.DistCallBackTask;
import ies.retry.spi.hazelcast.persistence.DBMergePolicy;
import ies.retry.spi.hazelcast.persistence.RetryMapStore;
import ies.retry.spi.hazelcast.persistence.RetryMapStoreFactory;
import ies.retry.spi.hazelcast.util.RetryUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import provision.services.logging.Logger;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;

/**
 * Copied mostly from {@link DistCallBackTask}, but this will allow 
 * for remote callback.
 *
 */
public class CallBackExecutor {

	private static String CALLER = DistCallBackTask.class.getName();
	HazelcastRetryImpl retryImpl = ((HazelcastRetryImpl) Retry
			.getRetryManager());
	HazelcastConfigManager configMgr = (HazelcastConfigManager) retryImpl
			.getConfigManager();

	HazelcastInstance remoteInstance;
	
	public CallBackExecutor() {

	}
	
	public CallBackExecutor(HazelcastInstance remoteInstance) {
		this.remoteInstance = remoteInstance;
	}
	

	private long getNextDelayForRetry(BackOff backOff, int retryNum) {

		long nextDelay = 0;

		switch (backOff.getBackoffMode()) {
		case Geometric:
			nextDelay = Math.round(Math.pow(backOff.getIntervalMultiplier(),
					retryNum) * backOff.getMilliInterval());
			break;
		case StaticIntervals:
			if (retryNum < backOff.staticMillis().length - 1)
				nextDelay = backOff.staticMillis()[retryNum];
			else
				nextDelay = backOff.staticMillis()[backOff.staticMillis().length - 1];
			break;
		case Periodic:
			nextDelay = Math.round(backOff.getMilliInterval());
			break;
		}
		return nextDelay;
	}

	public CallbackStat execute(List<RetryHolder> listHolder) throws Exception {
		CallbackStat stat = new CallbackStat(true);
		IMap<String, List<RetryHolder>> retryMap = null;
		String id = listHolder.get(0).getId();
		String type = listHolder.get(0).getType();
		boolean lockAquired = false;

		if (id == null && type == null) {
			Logger.error(CALLER, "Dist_Callback_Missing_Input",
					"id or type is null", "ID", id, "TYPE", type);
			return stat;
		}

		try {

			RetryConfiguration config = configMgr.getConfiguration(type);
			boolean archive = config.isArchiveExpired();
			RetryCallback callback = retryImpl.getCallbackManager().getCallbackMap().get(type);
			retryMap = retryImpl.getH1().getMap(type);
			if (callback == null) {
				Logger.error(CALLER, "Null_Callback","Callback was not set for type " + type, "ID", id);
				throw new NoCallbackRegistered();
			}

			BackOff backOff = config.getBackOff();

			// retryMap.lock(id); // WE do lock below
			boolean exec = false;
			long curTime = System.currentTimeMillis();

			if (listHolder == null) {
				Logger.error(CALLER, "Null_List_Holder", "", "ID", id);
				stat.setSuccess(false);
			} else {
				RetryHolder firstHolder = listHolder.get(0);

				List<RetryHolder> failedHolder = new ArrayList<RetryHolder>();

				if (firstHolder.getNextAttempt() <= curTime) {
					exec = true;

					int i = 0;

				
					boolean skipCallbackForRemainingItemsDueToException = false;
					for (i = 0; i < listHolder.size(); i++) {
						RetryHolder holder = listHolder.get(i);
						try {
							// this is the potentially VERY expensive operation
							// the actual callback portion
							if (!skipCallbackForRemainingItemsDueToException) {
								
								stat.setSuccess(callback.onEvent(holder));
							}

							if (!stat.isSuccess())
								failedHolder.add(holder);

						} catch (Exception e) {
							skipCallbackForRemainingItemsDueToException = true;
							stat.setSuccess(false);
							failedHolder.add(holder);
							
						}

					}

					if (failedHolder.size() == 0) {
						Logger.info(CALLER, "Retry_Callback_Sucess: ID=" + id);
						// potential that this locks for a much briefer time
						lockAquired = retryMap.tryLock(id, configMgr
								.getRetryHzConfig().getRetryAddLockTimeout(),
								TimeUnit.MILLISECONDS);
						if (!lockAquired) {
							Logger.warn(CALLER, "DistCallBackTask_LockTimeout",
									"Lock timeout", "retry", id);
							throw new RuntimeException(
									"Unable to Aquire Lock: " + id.toString());
						}
						// retryMap.lock(id);
						List<RetryHolder> latest = retryMap.get(id);
						List<RetryHolder> mergedList = RetryUtil.merge(CALLER,
								listHolder, failedHolder, latest);

						RetryMapStore mapStore = RetryMapStoreFactory
								.getInstance().newMapStore(type);

						if (mergedList.size() == 0) {
							retryMap.remove(id);
							mapStore.delete(id);
						} else {
							retryMap.put(id, mergedList);
							mapStore.store(mergedList, 	DBMergePolicy.FIND_OVERWRITE); 
						}
					} else {
						stat.setSuccess(false);
					}

				}
				
				if (!stat.isSuccess() && exec) {
					lockAquired = retryMap.tryLock(id, configMgr.getRetryHzConfig().getRetryAddLockTimeout(), TimeUnit.MILLISECONDS);
					if (!lockAquired) {
						Logger.warn(CALLER, "DistCallBackTask_LockTimeout", "Lock timeout", "retry", id);
						throw new RuntimeException("Unable to Aquire Lock: " + id.toString());
					}
					List<RetryHolder> latest = retryMap.get(id);
					RetryMapStore mapStore = RetryMapStoreFactory.getInstance()
							.newMapStore(type);

					firstHolder = failedHolder.get(0);
					if (firstHolder.getCount() >= backOff.getMaxAttempts()) {
						failedHolder.remove(0);
						List<RetryHolder> mergedList = RetryUtil.merge(CALLER,
								listHolder, failedHolder, latest);

						if (mergedList.size() > 0) {
							// only one item in the beginning of the
							// failedHolder list can expire
							// so we archive only first entry
							if (archive) {
								List<RetryHolder> expired = new ArrayList<RetryHolder>();
								expired.add(firstHolder);
								mapStore.archive(expired, false);
							}
							mapStore.store(mergedList,
									DBMergePolicy.FIND_OVERWRITE); // first item
																	// has been
																	// removed
																	// from the
																	// list, so
																	// we have
																	// to
																	// synchronize
																	// with DB
							retryMap.put(id, mergedList);
						} else {
							retryMap.remove(id);
							if (!archive)
								mapStore.delete(id);
							else
								mapStore.archive(listHolder, true);
						}
						Logger.warn(CALLER, "Dist_Callback_Retry_Failed",
								"Failed to retry: " + firstHolder);

					} else {
						// we separate the timer from the calculation of next
						// set all of them to be safe (and help in query)
						for (RetryHolder fh : failedHolder) {

							long nextDelay = getNextDelayForRetry(backOff,
									fh.getCount());

							fh.setNextAttempt(System.currentTimeMillis()
									+ nextDelay);
							fh.incrementCount();
						}
						List<RetryHolder> mergedList = RetryUtil.merge(CALLER,
								listHolder, failedHolder, latest);
						// nothing expired so only new item could have been
						// potentially added to the list
						boolean updateDB = failedHolder.size() != listHolder
								.size() ? true : // some successfully processed
													// items are removed from
													// the list
								latest.size() != listHolder.size(); // some new
																	// item(s)
																	// has been
																	// added

						if (updateDB) // we synchronize only if there were
										// changes to number of items in the
										// list
							mapStore.store(mergedList,
									DBMergePolicy.FIND_OVERWRITE);
						retryMap.put(id, mergedList);
					}
					stat.setCount(firstHolder.getCount());
					stat.setDateCreated(firstHolder.getSystemTs());

				}
			}

		} catch (Exception e) {
			// e.printStackTrace();
			stat.setSuccess(false);
			Logger.error(CALLER, "Dist_Callback_Task_Call_Exception",
					"Exception Message: " + e.getMessage(), "ID", id, "TYPE",
					type, e);
		} finally {
			if (retryMap != null && lockAquired) {
				retryMap.unlock(id);
			}
		}
		return stat;
	}
}
