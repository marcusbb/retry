package ies.retry.spi.hazelcast.persistence.cassandra;

import ies.retry.RetryHolder;
import ies.retry.spi.hazelcast.persistence.DBMergePolicy;
import ies.retry.spi.hazelcast.persistence.RetryEntity;
import ies.retry.spi.hazelcast.persistence.RetryMapStore;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

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
	
	
	/**
	 * It's terrible that the retry type is the map name, but keeps things the
	 * way they are for now.
	 * 
	 * @param mapName
	 * @param session
	 */
	public CassRetryMapStore(String mapName,Session session) {
		this.em = new DefaultEntityManager<>(session, CassRetryEntity.class);
		this.archive_em = new DefaultEntityManager<>(session, CassArchiveRetryEntity.class);
		this.mapName = mapName;
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

	public void loadIntoHZ(ReaderConfig config ) {
		
		
		//TODO: move
		BatchLoadJob loadJob = new BatchLoadJob(1);
		
				
		loadJob.bootstrap(config);
		loadJob.initJob(config);
		
		loadJob.runJob();
		
	}
	
	public Collection<CassRetryEntity> loadAll(ReaderConfig config ) {
		Collection<CassRetryEntity> col = new ArrayList<>();
		BatchLoadJob loadJob = new BatchLoadJob(1,col);
		
		
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
	public void store(String key, List<RetryHolder> value,
			DBMergePolicy mergePolicy) {
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
			throw new IllegalArgumentException(e);
		}
	}

	@Override
	public void storeAll(Map<String, List<RetryHolder>> map) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void archive(List<RetryHolder> list, boolean removeEntity) {
		
		try {
			archive_em.persist(new CassArchiveRetryEntity(list));
			if (removeEntity)
				delete(list.get(0).getId());
			
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void delete(String key) {
		em.remove(new CassRetryEntity.Id(key,mapName) , CUtils.getDefaultParams());
		em.getSession().execute("UPDATE retry_counters SET count = count -1 where type = ?",mapName);
	}

	@Override
	public void deleteByType() {
		throw new UnsupportedOperationException();
	}

	

	@Override
	public boolean isWriteSync() {
		return true;
	}

	@Override
	public void setWriteSync(boolean writeSync) {
		throw new UnsupportedOperationException();
	}

	

}
