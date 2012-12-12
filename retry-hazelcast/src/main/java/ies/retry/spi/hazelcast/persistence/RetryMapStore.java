package ies.retry.spi.hazelcast.persistence;


import ies.retry.RetryHolder;
import ies.retry.spi.hazelcast.config.PersistenceConfig;
import ies.retry.spi.hazelcast.persistence.ops.ArchiveOp;
import ies.retry.spi.hazelcast.persistence.ops.DelOp;
import ies.retry.spi.hazelcast.persistence.ops.StoreAllOp;
import ies.retry.spi.hazelcast.persistence.ops.StoreOp;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceException;
import javax.persistence.Query;

import org.hibernate.ScrollableResults;
import org.hibernate.Session;

import provision.services.logging.Logger;


/**
 * Allows for synchronous write behind for all store methods.
 * 
 * All read methods are naturally synchronous - and will use a single entity manager 
 * {@link EntityManager} instance.
 * 
 * Each of the (write) operations are atomic, and will not participate in overall transaction
 * of the application.  
 * As the writes can be configured behind {@link #writeSync} - we can not support read/write/read consistency.
 * 
 * 
 *   
 * @author msimonsen
 *
 */
public class RetryMapStore {//implements MapStore<String, List<RetryHolder>> {

	private String mapName = null;
	private static String CALLER = RetryMapStore.class.getName();
	
	private EntityManagerFactory emf;
	private EntityManager sync_emf;
	
	private boolean hasData = true;
	private ScrollableResults retryCursor;
	
	//Provided	
	ExecutorService execService = null;
	
	private boolean writeSync = false;
	
	
	protected RetryMapStore() {}
	
	public RetryMapStore(String mapName,EntityManagerFactory emf,PersistenceConfig config) {
		this.mapName = mapName;
		this.emf = emf;
		this.sync_emf = emf.createEntityManager();
		this.writeSync = config.isWriteSync();
	}
	
	public RetryEntity getEntity(String key) {
		RetryEntity entity = sync_emf.find(RetryEntity.class, new RetryId(key, mapName));
		return entity;
	}
	
	public List<RetryHolder> load(String key) {
		Logger.info(CALLER, "Retry_Map_Load_Key", "loading  " + key, "Type", mapName);
		
		RetryEntity entity = sync_emf.find(RetryEntity.class, new RetryId(key, mapName));
		
		if(entity != null){
			try {
				entity.setHolderList( entity.fromByte(entity.getRetryData()) );
			}catch(Exception e) {
				entity.setHolderList(new ArrayList<RetryHolder>());
				Logger.error(CALLER, "Retry_Map_Load_Key_Exception", e.getMessage(), "Key", key, "Type", mapName, e);
			}
			
			//To MC: this was throwing nullpointer when calling getRetry() through container only. Please check fix is okay
			return entity.getHolderList();
		}else{ 
			return null;
		}		
	}


	/**
	 * Use a scroll-able cursor, only available in hibernate
	 * specific API.
	 * 
	 * @param batchSize
	 * @return Emtpy Map if there is no data to load
	 */
	public Map<String,List<RetryHolder>> load(int batchSize) {
		Logger.info(CALLER, "Retry_Map_Load_Keys", "loading keys from provided start.", "Size", batchSize, "Type", mapName);
		Map<String,List<RetryHolder>> map = new HashMap<String, List<RetryHolder>>(batchSize);
		
		if (!hasData)
			return map;
		
		try {
			if(retryCursor == null) {
				Logger.info(CALLER, "Retry_Map_Load_Keys", "creating database cursor", "Type", mapName);
				// Get Hibernate session for scroll-able cursor from JPA 1.0
				Session session = (Session)sync_emf.getDelegate();
				org.hibernate.Query query = session.createQuery("SELECT r FROM RetryEntity r where r.id.type= :type");
				query.setParameter("type", mapName);
				retryCursor = query.scroll();
			}
			
			int i=0;
			while(i<batchSize && retryCursor.next()){
				RetryEntity entity = (RetryEntity)retryCursor.get(0); 
				entity.setHolderList(entity.fromByte(entity.getRetryData()));
				map.put(entity.getId().getId(), entity.getHolderList());
			
				i++;
			}
		
			if(i==0){ // no data to read
				closeRetryCursor();
			}

			Logger.debug(CALLER, "Loaded " + map.keySet().size() + " retries from db");
		} catch (Exception e) {
			Logger.error(CALLER, "Retry_Map_Load_Keys", "Exception Message: " + e.getMessage(), "Size", batchSize, "Type", mapName, e);
			closeRetryCursor();
		}
		
		return map;
	}

	private void closeRetryCursor() {
		hasData = false;
		if(retryCursor == null)
			return;
		
		try {
			retryCursor.close();
		} catch(Exception e) {
			Logger.error(CALLER, "closeRetryCursor", "Close cursor fail", "HibernateException Message: " + e.getMessage(), "Type", mapName, e);
		}
		finally{
			retryCursor = null;
		}
	}
	/**
	 * There is a serious bug in hibernate that once paging is requested
	 * @return
	 */
	public int count() {
		Query query =sync_emf.createNativeQuery("SELECT count(*) FROM RETRIES WHERE RETRY_TYPE = :type");
		query.setParameter("type", mapName);
		query.setFirstResult(0);
		query.setMaxResults(1);
		BigDecimal count = (BigDecimal)query.getSingleResult();
		
		return count.intValue();
	}
	


	public void store(List<RetryHolder> value,final DBMergePolicy mergePolicy) {
		String key = value.get(0).getId();
		//commented out duplicate info log line since it is first thing logged in method its calling
		//Logger.info(CALLER, "Retry_Map_Store_Key", "store  key " + key, "Type", mapName);
		store(key, value,mergePolicy);
	}
	/**
	 * 
	 * @param key
	 * @param value
	 * @param update - if provided will attempt to attach and update
	 */
	public void store(final String key, final List<RetryHolder> value,final DBMergePolicy mergePolicy) {
		Logger.info(CALLER, "Retry_Map_Store_Key", "store  key " + key, "Type", mapName);
		
		Future<Void> future = execService.submit(
				new StoreOp(emf, value, mergePolicy)
				);
		handleWriteSync(future);
	}

	
	
	public void storeAll(final Map<String, List<RetryHolder>> map) {
		Logger.info(CALLER, "Retry_Map_Store_Keys", "store  all " + map.keySet(), "Type", mapName);
		
		Future<Void> future = execService.submit(
				new StoreAllOp(emf, map)
				);
		handleWriteSync(future);
		
	}

	public void archive(final String key) {
		Logger.info(CALLER, "Retry_Map_Archive_Key", "archive " + key, "Type", mapName);
		
		Future<Void> future = execService.submit(
				new ArchiveOp(emf, mapName, key));
		handleWriteSync(future);
	}	
	
	public void delete(final String key) {
		Logger.info(CALLER, "Retry_Map_Delete_Key", "delete " + key, "Type", mapName);
		
		Future<Void> future = execService.submit(
				new DelOp(emf, mapName, key));
		handleWriteSync(future);
	}
	public void deleteByType() {
		Logger.info(CALLER, "Retry_Map_Delete_By_Type", "delete by type: " + mapName);
		Future<Void> future = execService.submit(new Callable<Void>() {

			@Override
			public Void call() throws Exception {
				EntityManager em = emf.createEntityManager();
				em.getTransaction().begin();
				
				Query q = em.createQuery("delete from RetryEntity r where r.id.type = :type"  );
				q.setParameter("type", mapName);
				q.executeUpdate();
				
				em.getTransaction().commit();
				return null;
			}
		});
		handleWriteSync(future);
	}
	private void handleWriteSync(Future<Void> future) throws RuntimeException {
		if (writeSync) {
			try {
				future.get();
			}catch(ExecutionException e) {
				if (e.getCause() instanceof PersistenceException ) {
					throw (PersistenceException)e.getCause();
				}
			}catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}
	
	public String getMapName() {
		return mapName;
	}


	public void setMapName(String mapName) {
		this.mapName = mapName;
	}

	public ExecutorService getExecService() {
		return execService;
	}

	public void setExecService(ExecutorService execService) {
		this.execService = execService;
	}

	public boolean isWriteSync() {
		return writeSync;
	}


	public void setWriteSync(boolean writeSync) {
		this.writeSync = writeSync;
	}

}
