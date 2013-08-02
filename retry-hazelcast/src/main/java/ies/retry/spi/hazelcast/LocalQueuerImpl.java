package ies.retry.spi.hazelcast;

import ies.retry.Retry;
import ies.retry.RetryHolder;
import ies.retry.spi.hazelcast.config.HazelcastConfigManager;
import ies.retry.spi.hazelcast.config.HazelcastXmlConfig;
import ies.retry.spi.hazelcast.disttasks.AddRetryCallable;

import java.util.HashMap;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.hazelcast.core.DistributedTask;
import com.hazelcast.core.HazelcastInstance;

public class LocalQueuerImpl implements LocalQueuer {

	private HazelcastInstance hz;
	private HazelcastXmlConfig config;
	private HazelcastConfigManager configMgr;
	
	private HashMap<String, Queue<RetryHolder>> queueMap;
	private HashMap<String,ExecutorService> queueExec;
	
	private Object lock = new String();
	
	private long quietPeriod = 10*1000;
			
	public LocalQueuerImpl(HazelcastInstance inst,HazelcastConfigManager configMgr) {
		this.hz = inst;
		this.config = configMgr.getHzConfig();
		queueMap = new HashMap<String, Queue<RetryHolder>>();
		queueExec = new HashMap<String, ExecutorService>();
	}
	
	private Queue<RetryHolder> getQueue(String key) {
		Queue<RetryHolder> queue = queueMap.get(key);
		if (queue == null) {
			synchronized (lock) {
				queue = new ArrayBlockingQueue<RetryHolder>(config.getDefaultLocalQueueSize());
				ExecutorService exec = Executors.newSingleThreadExecutor();
				queueExec.put(key, exec );
				
				initDeque(queue, exec);
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
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean add(RetryHolder retryHolder) {

		Queue<RetryHolder> queue = getQueue(retryHolder.getType());
		boolean ret = queue.add(retryHolder);
		
		
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

	/**
	 * 
	 * @param retryType
	 */
	private void initDeque(final Queue<RetryHolder> queue,final ExecutorService exec) {
		
		
		
		exec.submit(new Runnable() {
			
			@Override
			public void run() {
				while (true) {
					RetryHolder retry = queue.peek();
					
					//may improve this by having a 
					if (retry == null) {
						try {Thread.sleep(quietPeriod); }catch (InterruptedException e) {e.printStackTrace();}
					
					}else {
						
						
						DistributedTask<Void> distTask = new DistributedTask<Void>(new AddRetryCallable(retry, configMgr.getConfiguration(retry.getType())), retry.getId());
						try {
							hz.getExecutorService(HazelcastRetryImpl.EXEC_SRV_NAME).submit(distTask).get();
							queue.poll();
							
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
		});
	}

}
