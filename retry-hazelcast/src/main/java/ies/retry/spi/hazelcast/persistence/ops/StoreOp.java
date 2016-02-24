package ies.retry.spi.hazelcast.persistence.ops;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceException;

import org.slf4j.Logger;

import ies.retry.RetryHolder;
import ies.retry.spi.hazelcast.persistence.DBMergePolicy;
import ies.retry.spi.hazelcast.persistence.RetryEntity;
import ies.retry.spi.hazelcast.persistence.RetryId;
import ies.retry.spi.hazelcast.util.RetryUtil;

public class StoreOp extends AbstractOp<Void>{

	private static org.slf4j.Logger logger =  org.slf4j.LoggerFactory.getLogger(StoreOp.class);
	
	private DBMergePolicy mergePolicy = DBMergePolicy.FIND_OVERWRITE;
	
	public StoreOp(EntityManagerFactory emf,List<RetryHolder> list) {
		RetryHolder holder = list.get(0);
				
		setRetryType(holder.getType());
		setStoreId(holder.getId());
		setEmf(emf);
		setListHolder(list);
		
	}
	
	public StoreOp(EntityManagerFactory emf,List<RetryHolder> list, DBMergePolicy mergePolicy) {
		this(emf, list);
		this.mergePolicy = mergePolicy;
	}
	
	@Override
	public Void exec(EntityManager em) throws Exception {
		
		if (mergePolicy == DBMergePolicy.FIND_OVERWRITE ) {
			findUpdatePayload(em);
		}
		else if (mergePolicy == DBMergePolicy.OVERWRITE) {
			em.persist(new RetryEntity(listHolder));
		}
		else if (mergePolicy == DBMergePolicy.ORDER_TS || mergePolicy == DBMergePolicy.ORDER_TS_DISCARD_DUP_TS)
			findAndMergePayload(em);

		return null;
	}
	
	
	//cache wins policy - over-ride what's in DB
	private void findUpdatePayload(EntityManager em) {
		RetryEntity entity = em.find(RetryEntity.class, new RetryId(storeId, retryType));
		try {
			if (entity != null) {
				entity.populate(listHolder);
				em.persist(entity);
			} else {
				em.persist(new RetryEntity(listHolder));
			}
		}catch (PersistenceException e) {
			//This may have different concerns
			logger.error( "Find_Update_Payload_PersistenceException: {}",  e.getMessage(),e);
			
		}catch (Exception e) {
			logger.error( "Find_Update_Payload_Exception: {}", e.getMessage(),e);
		}
	}
	
	
	//merge
	//use traversal, and order by TS
	//strip
	private void findAndMergePayload(EntityManager em) {
		RetryEntity entity = em.find(RetryEntity.class, new RetryId(storeId, retryType));
		
		try {
			if (entity != null) {
				List<RetryHolder> dbList = null;
				
				try {
					dbList = entity.fromByte(entity.getRetryData());
				} catch (Exception e) {
					logger.error("Find_Merge_Payload: Failed to de-serialize binary data: key={}, type={},version={},msg={}" , 
							 storeId,  retryType,  entity.getVersion(), e.getMessage(), e);
				}
				
				@SuppressWarnings("unchecked")
				List<RetryHolder> mergeList = dbList==null ? 
						getListHolder() : RetryUtil.merge(dbList, getListHolder());
		
				entity.populate(mergeList);
				em.persist(entity);
			} else {
				em.persist(new RetryEntity(listHolder));
			}
		}catch (PersistenceException e) {
			//This may have different concerns
			logger.error( "Find_Merge_Payload: {}",  e.getMessage(),e);
			
		}catch (Exception e) {
			logger.error( "Find_Merge_Payload: {}",  e.getMessage(),e);
		}
	}
	
	/*@Override
	public void handleException(PersistenceException e) throws PersistenceException {
		Logger.warn(CALLER, "Handle_Exception", e.getMessage(),e);
		if (e.getCause() instanceof SQLException) {
			SQLException sqle = (SQLException)e.getCause();
			System.out.println("sqle: " + sqle.getErrorCode());
		}
		//
		throw e;
	}*/
	
	
}
