package ies.retry.spi.hazelcast.persistence.ops;

import ies.retry.RetryHolder;
import ies.retry.spi.hazelcast.persistence.RetryEntity;
import ies.retry.spi.hazelcast.persistence.RetryId;
import ies.retry.spi.hazelcast.persistence.RetryMapStore;
import ies.retry.spi.hazelcast.persistence.cassandra.CassRetryMapStore;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;


/**
 * 
 * We want the load also to timeout.
 * 
 * This will replace {@link RetryMapStore#load(int)}
 *
 *	TODO to be tested
 */
public class LoadOp extends AbstractOp<List<RetryHolder>>{

	private static org.slf4j.Logger logger =  org.slf4j.LoggerFactory.getLogger(CassRetryMapStore.class);
	
	private String key;
	private String type;
	
			
	public LoadOp(EntityManagerFactory emf,String id,String type) {
		setEmf(emf);
	}
	@Override
	public List<RetryHolder> exec(EntityManager em) throws Exception {
		try {
			RetryEntity entity = em.find(RetryEntity.class, new RetryId(key, type));

			if (entity != null) {
				try {
					entity.setHolderList(entity.fromByte(entity.getRetryData()));
				} catch (Exception e) {
					logger.error( "Retry_Map_Load_Key_Exception: Failed to de-serialize binary data: msg={}, key={}, type={}", e.getMessage(), 
							 key,  type,  e);
					// Remove retry that can't be de-serialized. A new transaction will be started in DeleteOp. 
					// However, we are not supposed to get deadlock as current context is read-only
					new DelOp(getEmf(), type, key).exec(em);
					return null;
				}

				// To MC: this was throwing nullpointer when calling getRetry()
				// through container only. Please check fix is okay
				return entity.getHolderList();
			} else {
				return null;
			}
		} finally {
			if (em != null) {
				em.clear();
				// can't close this one it's stateful
				// sync_emf.close();
			}
		}
	}

	

}
