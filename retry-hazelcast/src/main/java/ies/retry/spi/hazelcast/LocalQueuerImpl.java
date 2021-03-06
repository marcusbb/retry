package ies.retry.spi.hazelcast;

import ies.retry.RetryConfiguration;
import ies.retry.RetryHolder;
import ies.retry.spi.hazelcast.config.HazelcastConfigManager;
import ies.retry.spi.hazelcast.config.HazelcastXmlConfig;
import ies.retry.spi.hazelcast.disttasks.AddRetryCallable;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import provision.services.logging.Logger;

import com.hazelcast.core.DistributedTask;
import com.hazelcast.core.HazelcastInstance;

/**
 * An in memory local queue, storing a queue per retry type
 * {@link #queueMap}.
 * 
 *  Each queue will add to the head of the queue and subsequent operations
 *  will not remove from the queue unless a successful operation
 *  to take (or poll) from the queue has occurred see 
 *  {@link PollQueue#run()} 
 * 
 * @author msimonsen
 *
 */
public class LocalQueuerImpl implements LocalQueuer {

	private HazelcastInstance hz;
	private HazelcastXmlConfig config;
	private HazelcastConfigManager configMgr;
	
	private ConcurrentHashMap<String, Queue<RetryHolder>> queueMap;
	//Can turn this into a single thread, single it's a single thread pool
	private HashMap<String,ExecutorService> queueExec;
	private HashMap<String,PollQueue> pollQueueMap;
	
	static long awaitPollPeriod = 10; //seconds
	
	private LocalQueueLog queueLog;
	
	public LocalQueuerImpl(HazelcastInstance inst,HazelcastConfigManager configMgr) {
		this.hz = inst;
		this.configMgr = configMgr;
		this.config = configMgr.getRetryHzConfig();
		queueMap = new ConcurrentHashMap<String, Queue<RetryHolder>>();
		queueExec = new HashMap<String, ExecutorService>();
		pollQueueMap = new HashMap<String, PollQueue>();
		//may end up putting this in another method
		//so that it can be replayed
		try {
			queueLog = new LocalQueueLog(configMgr.getRetryHzConfig().getLocalQueueLogDir());
			replayLogAndQueue();
		}catch (IOException e) {
			Logger.error(getClass().getName(), "LocalQueuerImpl_init","No_local_log","msg",e.getMessage(),e);
		}
	}
	protected void replayLogAndQueue() throws IOException {
		Collection<RetryHolder> col = queueLog.replay();
		
		for (RetryHolder holder:col) {
			Queue<RetryHolder> queue = getQueue(holder.getType());
			queue.add(holder);
			
			pollQueueMap.get(holder.getType()).signal();
		}
		
	}
	private Queue<RetryHolder> getQueue(String key) {
		
		
		Queue<RetryHolder> queue = queueMap.get(key);
		
		if (queue == null) {
			queue = initPollingQueue(key);
				//initPoll(queue, exec,key,pollQueueMap.get(key));
		}
		
		return queue;
	}
	//May improve this but it's a one time synchronization (per key)
	private synchronized Queue<RetryHolder> initPollingQueue(String key) {
		//check to the map, since it could have blocked/queued on synchronized:
		if (queueMap.get(key) != null) {
			return queueMap.get(key);
		}
		//Consider moving this to the a concurrent queue implementation
		Queue<RetryHolder> queue = new ArrayBlockingQueue<RetryHolder>(config.getDefaultLocalQueueSize());
		queueMap.put(key, queue);
		ExecutorService exec = new ThreadPoolExecutor(1,1,1L,TimeUnit.SECONDS,new SynchronousQueue<Runnable>());
		queueExec.put(key, exec );
		//polling queue implementation
		PollQueue poller = new PollQueue( queue, configMgr.getConfiguration(key), hz,awaitPollPeriod,queueLog);
		exec.submit(poller);
		pollQueueMap.put(key, poller);
		
		return queue;
	}
	@Override
	public boolean isEmpty(String retryType) {
		
		boolean isEmpty = getQueue(retryType).isEmpty();
		
		return isEmpty;
		
	}

	@Override
	public boolean addIfNotEmpty(RetryHolder retryHolder) {
		if (isEmpty(retryHolder.getType())) {
			return false;
		}
		else {
			return add(retryHolder);
		}
	}

	@Override
	public boolean add(RetryHolder retryHolder) {

		Queue<RetryHolder> queue = getQueue(retryHolder.getType());
		boolean ret = queue.add(retryHolder);
		//persist to log
		try {
			if (queueLog != null)
				queueLog.queue(retryHolder);
		}catch (IOException e) {
			e.printStackTrace();
		}
		pollQueueMap.get(retryHolder.getType()).signal();
		
		return ret;
	}

	@Override
	public int size(String retryType) {
		return getQueue(retryType).size();
	}

	
	@Override
	public void shutdown() {
		for (ExecutorService exec:queueExec.values()) {
			exec.shutdown();
		}
		
	}
		

	public LocalQueueLog getQueueLog() {
		return queueLog;
	}
	




	private static class PollQueue implements Runnable {

		final Queue<RetryHolder> queue;
		final RetryConfiguration config;
		final HazelcastInstance hz;
		final LocalQueueLog queueLog;
		
		//the synchronization could be smarter.
		CountDownLatch latch = new CountDownLatch(1);
		boolean done = false;
		//paranonia to make we don't wait for ever
		long await;
		
		PollQueue(Queue<RetryHolder> queue,RetryConfiguration config,HazelcastInstance hz,long await,LocalQueueLog queueLog) {
			
			//this.condition = condition;
			this.queue = queue;
			this.config = config;
			this.hz = hz;
			this.await = await;
			this.queueLog = queueLog;
		}
		
		protected void signal() {
			
				latch.countDown();
			
		}
		protected void stop() {
			this.done = true;
		}
		@Override
		public void run() {
			
			while (!done) {
				RetryHolder retry = queue.peek();
				
				//await at the barrier
				if (retry == null) {
					try {
						latch.await(await,TimeUnit.SECONDS);
						latch = new CountDownLatch(1);
					} catch (InterruptedException e) {
						Logger.warn(getClass().getName(), "INTERUPTED_EX","ex_msg",e.getMessage(),e);
						stop();
					} 
				}else {
					
					
					DistributedTask<Void> distTask = new DistributedTask<Void>(new AddRetryCallable(retry, config), retry.getId());
					try {
						//By future.get()  we ensure a FIFO processing
						hz.getExecutorService(HazelcastRetryImpl.EXEC_SRV_NAME).submit(distTask).get(await,TimeUnit.SECONDS);
						//Don't remove if it failed/timedout.
						queue.remove();
						
						if (queueLog != null)
							queueLog.moveTakeMarker();
						
					} catch (TimeoutException e) {
						Logger.warn(getClass().getName(), "TimeoutException","ex_msg",e.getMessage(),e);
					}
					catch (ExecutionException e) {
						Logger.warn(getClass().getName(), "ExecutionException","ex_msg",e.getMessage(),e);
					}catch (InterruptedException e) {
						Logger.warn(getClass().getName(), "INTERUPTED_EX","ex_msg",e.getMessage(),e);
						stop();
					} catch (IOException e) {
						Logger.warn(getClass().getName(), "IOException","ex_msg",e.getMessage(),e);
					}
					
					
				}
				
				
			}
		
			
		}
		
	}
}
