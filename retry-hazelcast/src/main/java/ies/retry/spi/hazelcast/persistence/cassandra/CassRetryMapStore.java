package ies.retry.spi.hazelcast.persistence.cassandra;

import ies.retry.RetryHolder;
import ies.retry.spi.hazelcast.persistence.DBMergePolicy;
import ies.retry.spi.hazelcast.persistence.RetryEntity;
import ies.retry.spi.hazelcast.persistence.RetryMapStore;
import ies.retry.spi.hazelcast.persistence.ops.OpResult;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import provision.services.logging.Logger;
import reader.ReaderConfig;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;

import driver.em.CUtils;
import driver.em.DefaultEntityManager;

/**
 * Instead of extending {@link RetryMapStore} we should be implementing an interface
 * to the store.
 * 
 */
public class CassRetryMapStore extends RetryMapStore {

	
	private DefaultEntityManager<CassRetryEntity.Id, CassRetryEntity> em = null;
	private DefaultEntityManager<CassArchiveRetryEntity.Id, CassArchiveRetryEntity> archive_em = null;
	
	private String mapName = null;
	
	private int dropThreshold = Integer.MAX_VALUE;
	
	/**
	 * 
	 * 
	 * @param mapName - alias for retry type
	 * @param session
	 */
	public CassRetryMapStore(String mapName,Session session,boolean writeSync) {
		this.em = new DefaultEntityManager<>(session, CassRetryEntity.class);
		this.archive_em = new DefaultEntityManager<>(session, CassArchiveRetryEntity.class);
		this.mapName = mapName;
		this.setExecService((ThreadPoolExecutor)Executors.newCachedThreadPool());
		this.setWriteSync(writeSync);
	}
	public CassRetryMapStore(String mapName,Session session,ThreadPoolExecutor tpe,int dropThreshold) {
		this.setExecService(tpe);
		this.dropThreshold = dropThreshold;
	}
	
	@Override
	public RetryEntity getEntity(String key) {
		throw new UnsupportedOperationException();
	}

	@Override
	public List<RetryHolder> load(String key) {
		
		CassRetryEntity entity = em.find(new CassRetryEntity.Id(key,mapName),CUtils.getDefaultParams());
		
		if (entity != null) {
			return entity.convertPayload();
		}	
		
		return null;
	}

	//Until we move the number of threads for MTJobBootstrap
	public void loadIntoHZ(ReaderConfig config,HashSet<String> types ) {
		
		
		BatchLoadJob loadJob = new BatchLoadJob(types);
		
				
		loadJob.bootstrap(config);
		loadJob.initJob(config);
		
		loadJob.runJob();
		
	}
	
	public Collection<CassRetryEntity> loadAll(ReaderConfig config,HashSet<String> types ) {
		Collection<CassRetryEntity> col = new ArrayList<>();
		BatchLoadJob loadJob = new BatchLoadJob(col,types);
		
		
		loadJob.bootstrap(config);
		loadJob.initJob(config);
		
		loadJob.runJob();
		
		return col;
	}
	
	@Override
	public Map<String, List<RetryHolder>> load(int start, int size) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Map<String, List<RetryHolder>> load(int batchSize) {
		throw new UnsupportedOperationException();
	}

	@Override
	public int count() {
		return (int)longCount();
	}
	public long longCount() {
		ResultSet rs = em.getSession().execute("select count from retry_counters where type = ?", mapName);
		Row row = rs.one();
		if (row !=null)
			return (int)row.getLong(0);
		return 0;
	}
	
	
	public void setCountTo(long count) {
		long targetCount = longCount() - count;
		
		em.getSession().execute("update retry_counters set count = count - ? where type = ?", targetCount, mapName );
		
	}
	@Override
	public void store(List<RetryHolder> value, DBMergePolicy mergePolicy) {
		
		store(value.get(0).getId(),value,mergePolicy);
	}

	@Override
	public void store(final String key, final List<RetryHolder> value,
			DBMergePolicy mergePolicy) {
		if (dropThreshold < execService.getQueue().size() ){
			Logger.error(CassRetryMapStore.class.getName(), "DB_QUEUE_MAX_REACHED","Reached queue size: " + execService.getQueue().size());
			return;
		}
		handleWriteSync(
			execService.submit(new Callable<OpResult<Void>>() {
	
				@Override
				public OpResult<Void> call() throws Exception {
					try {
						CassRetryEntity entity = new CassRetryEntity(value);
						//always read just an id
						Collection<CassRetryEntity> col = em.findBy("select id from retry where id = ?",new Object[]{key},CUtils.getDefaultParams());
						if (col != null && col.isEmpty()) {
							//bump counter
							em.getSession().execute("UPDATE retry_counters SET count = count +1 where type = ?",mapName);
						}
						//TODO make parameters configurable
						em.persist(entity, CUtils.getDefaultParams() );
					}catch (ClassNotFoundException | IOException e) {
						Logger.error(CassRetryMapStore.class.getName(), "ERR_DESERIZALATION_CASS",e.getMessage(),e);
						
					}
					return null;
				}
			})
		);
		
		
	}

	@Override
	public void storeAll(Map<String, List<RetryHolder>> map) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void archive(final List<RetryHolder> list, final boolean removeEntity) {
		
		handleWriteSync(
			execService.submit(new Callable<OpResult<Void>>() {
				
				@Override
				public OpResult<Void> call() throws Exception {
					try {
						archive_em.persist(new CassArchiveRetryEntity(list));
						if (removeEntity)
							delete(list.get(0).getId());
						
					} catch (ClassNotFoundException| IOException e) {
					}
					return null;
				}
			}
		));
	}

	@Override
	public void delete(final String key) {
		handleWriteSync(execService.submit(new Callable<OpResult<Void>>() {

			@Override
			public OpResult<Void> call() throws Exception {
				em.remove(new CassRetryEntity.Id(key, mapName),
						CUtils.getDefaultParams());
				em.getSession().execute("UPDATE retry_counters SET count = count -1 where type = ?",mapName);
				return null;
			}
		}));

	}

	@Override
	public void deleteByType() {
		throw new UnsupportedOperationException();
	}

	

	

	

}
