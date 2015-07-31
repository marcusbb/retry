package ies.retry.spi.hazelcast.persistence.cassandra;

import ies.retry.Retry;
import ies.retry.RetryConfigManager;
import ies.retry.RetryHolder;
import ies.retry.spi.hazelcast.HazelcastRetryImpl;
import ies.retry.spi.hazelcast.StateManager;
import ies.retry.spi.hazelcast.disttasks.PutRetryTask;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

import provision.services.logging.Logger;
import reader.ReaderJob;
import reader.RowReaderTask;

import com.datastax.driver.core.ColumnDefinitions;
import com.datastax.driver.core.ExecutionInfo;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.hazelcast.core.DistributedTask;
import com.hazelcast.core.HazelcastInstance;

import driver.em.DefaultEntityManager;

/**
 * Reads from Cassandra, loads into HZ
 * 
 *
 */
public class RetryRowReaderLoader extends ReaderJob<Void> {

	
	private static final long serialVersionUID = -4855716640224367110L;

	private DefaultEntityManager<CassRetryEntity.Id, CassRetryEntity> em = null;
	
	private Collection<CassRetryEntity> results = null;
	
	private ExecutorService exec = null;
	
	private RetryConfigManager configMgr = null;
	
	private AtomicInteger count = new AtomicInteger();

	private HashSet<String> types;
	
	public RetryRowReaderLoader(Session session,HashSet<String> types) {
		em = new DefaultEntityManager<>(session, CassRetryEntity.class);
		
		configMgr = ((HazelcastRetryImpl)Retry.getRetryManager()).getConfigManager();
		HazelcastInstance h1 = ((HazelcastRetryImpl)Retry.getRetryManager()).getH1();
		exec = h1.getExecutorService(StateManager.EXEC_SRV_NAME);
		this.types = types;
		
	}
	/**
	 * No hazelcast loading, simply stores in col
	 * @param col - the collection to store the retry entities
	 * 
	 */
	public RetryRowReaderLoader(Session session,Collection<CassRetryEntity> col,HashSet<String> type) {
		em = new DefaultEntityManager<>(session, CassRetryEntity.class);
		this.results = col;
		this.types = type;
	}
	
	public class RowTask implements RowReaderTask<Void> {
		
		HashSet<String> types;
		
		public RowTask(HashSet<String> types) {
			this.types = types;
		}
		@Override
		public Void process(Row row,ColumnDefinitions meta, ExecutionInfo execInfo) {
			
			try {
				CassRetryEntity entity = em.getEntityConfig().get(row);
				if (types != null && !types.contains(entity.getId().getType()))
					return null;
				
				List<RetryHolder> list = entity.convertPayload();
				if (list != null && !list.isEmpty()) {
					
					DistributedTask<Void> distTask = new DistributedTask<Void>(
							new PutRetryTask(list, false), 
							list.get(0).getId());
					exec.submit(distTask);
				}
			}catch (Exception e) {
				Logger.error(getClass().getName(), "Add_Retry_StorageTimeout_Exception", "Exception Message: " + e.getMessage(), "ex", e);
			}
			count.incrementAndGet();
			
			
			return null;
		}
		
	}
	public class SimpleRowTask implements RowReaderTask<Void> {

		private Collection<CassRetryEntity> results;
		private HashSet<String> types;
		public SimpleRowTask(HashSet<String> types) {
			this.types = types;
		}
		public SimpleRowTask(Collection<CassRetryEntity> results) {
			this.results = results;
		}
		@Override
		public Void process(Row row, ColumnDefinitions colDef,
				ExecutionInfo execInfo) {
			CassRetryEntity entity = em.getEntityConfig().get(row);
			if (types != null && !types.contains(entity.getId().getType()))
				return null;
			
			results.add(entity);
			
			return null;
		}
		
	}
	
	
	@Override
	public RowReaderTask<Void> newTask() throws Exception {
		if (results != null)
			return new SimpleRowTask(results);
		return new RowTask(types);
	}

	@Override
	public void processResult(Void result) {
		//Nothing to do		
	}

	@Override
	public void onReadComplete() {
		Logger.info(getClass().getName(), "Complete_read","complete_read","size",getCount());
		
	}
	public int getCount() {
		return count.get();
	}

}
