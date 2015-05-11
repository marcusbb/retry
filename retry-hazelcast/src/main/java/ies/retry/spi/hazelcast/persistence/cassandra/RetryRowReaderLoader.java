package ies.retry.spi.hazelcast.persistence.cassandra;

import ies.retry.Retry;
import ies.retry.RetryConfigManager;
import ies.retry.RetryHolder;
import ies.retry.spi.hazelcast.HazelcastRetryImpl;
import ies.retry.spi.hazelcast.StateManager;
import ies.retry.spi.hazelcast.disttasks.AddRetryCallable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutorService;

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
	
	public RetryRowReaderLoader(Session session) {
		em = new DefaultEntityManager<>(session, CassRetryEntity.class);
		
		configMgr = ((HazelcastRetryImpl)Retry.getRetryManager()).getConfigManager();
		HazelcastInstance h1 = ((HazelcastRetryImpl)Retry.getRetryManager()).getH1();
		exec = h1.getExecutorService(StateManager.EXEC_SRV_NAME);
		
	}
	/**
	 * No hazelcast loading, simply stores in col
	 * @param col - the collection to store the retry entities
	 * 
	 */
	public RetryRowReaderLoader(Session session,Collection<CassRetryEntity> col) {
		em = new DefaultEntityManager<>(session, CassRetryEntity.class);
		this.results = col;
	}
	
	public class RowTask implements RowReaderTask<Void> {
		
		@Override
		public Void process(Row row,ColumnDefinitions meta, ExecutionInfo execInfo) {
			
			CassRetryEntity entity = em.getEntityConfig().get(row);
			List<RetryHolder> list = entity.convertPayload();
			
			if (list != null && !list.isEmpty()) {
				
				DistributedTask<Void> distTask = new DistributedTask<Void>(
						new AddRetryCallable(list, configMgr.getConfiguration(list.get(0).getType()),false), 
						list.get(0).getId());
				exec.submit(distTask);
			}
			
			
			
			return null;
		}
		
	}
	public class SimpleRowTask implements RowReaderTask<Void> {

		private Collection<CassRetryEntity> results;
		
		public SimpleRowTask(Collection<CassRetryEntity> results) {
			this.results = results;
		}
		@Override
		public Void process(Row row, ColumnDefinitions colDef,
				ExecutionInfo execInfo) {
			CassRetryEntity entity = em.getEntityConfig().get(row);
			results.add(entity);
			
			return null;
		}
		
	}
	
	
	@Override
	public RowReaderTask<Void> newTask() throws Exception {
		if (results != null)
			return new SimpleRowTask(results);
		return new RowTask();
	}

	@Override
	public void processResult(Void result) {
		//Nothing to do		
	}

	@Override
	public void onReadComplete() {
		// Possibly alter state here
		
	}

}