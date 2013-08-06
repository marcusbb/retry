package ies.retry.spi.hazelcast;

import ies.retry.RetryConfiguration;
import ies.retry.RetryHolder;
import ies.retry.spi.hazelcast.config.HazelcastConfigManager;
import ies.retry.spi.hazelcast.config.HazelcastXmlConfig;
import ies.retry.spi.hazelcast.disttasks.AddRetryCallable;

import java.util.HashMap;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;

import com.hazelcast.core.DistributedTask;
import com.hazelcast.core.HazelcastInstance;

public class LocalQueuerImpl implements LocalQueuer {

	private HazelcastInstance hz;
	private HazelcastXmlConfig config;
	private HazelcastConfigManager configMgr;
	
	private HashMap<String, Queue<RetryHolder>> queueMap;
	private HashMap<String,ExecutorService> queueExec;
	private HashMap<String,PollQueue> pollQueueMap;
	
	private Object lock = new String();
	
	private long quietPeriod = 1000;
			
	public LocalQueuerImpl(HazelcastInstance inst,HazelcastConfigManager configMgr) {
		this.hz = inst;
		this.configMgr = configMgr;
		this.config = configMgr.getHzConfig();
		queueMap = new HashMap<String, Queue<RetryHolder>>();
		queueExec = new HashMap<String, ExecutorService>();
		pollQueueMap = new HashMap<String, PollQueue>();
	}
	
	private Queue<RetryHolder> getQueue(String key) {
		Queue<RetryHolder> queue = queueMap.get(key);
		if (queue == null) {
			synchronized (lock) {
				queue = new ArrayBlockingQueue<RetryHolder>(config.getDefaultLocalQueueSize());
				ExecutorService exec = new ThreadPoolExecutor(1,1,1L,TimeUnit.SECONDS,new SynchronousQueue<Runnable>());
				queueExec.put(key, exec );
				//polling queue implementation
				PollQueue poller = new PollQueue( queue, configMgr.getConfiguration(key), hz);
				exec.submit(poller);
				pollQueueMap.put(key, poller);
				//initPoll(queue, exec,key,pollQueueMap.get(key));
			}
		}
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

	

	private static class PollQueue implements Runnable {

		
		//final Condition condition;
		final Queue<RetryHolder> queue;
		final RetryConfiguration config;
		final HazelcastInstance hz;
		
		//TODO the synchronization could be smarter.
		CountDownLatch latch = new CountDownLatch(1);
		boolean done = false;
		
		PollQueue(Queue<RetryHolder> queue,RetryConfiguration config,HazelcastInstance hz) {
			
			//this.condition = condition;
			this.queue = queue;
			this.config = config;
			this.hz = hz;
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
						latch.await();
						latch = new CountDownLatch(1);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} 
				}else {
					
					
					DistributedTask<Void> distTask = new DistributedTask<Void>(new AddRetryCallable(retry, config), retry.getId());
					try {
						hz.getExecutorService(HazelcastRetryImpl.EXEC_SRV_NAME).submit(distTask).get();
						queue.remove();
						
					}catch (ExecutionException e) {
						//TODO 
						e.printStackTrace();
					}catch (InterruptedException e) {
						//TODO
						e.printStackTrace();
					}
					
					
				}
				
				
			}
		
			
		}
		
	}
}
