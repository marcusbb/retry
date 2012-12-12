package ies.retry.spi.hazelcast.persistence.ops;

import ies.retry.RetryHolder;
import ies.retry.spi.hazelcast.persistence.DBMergePolicy;
import ies.retry.spi.hazelcast.persistence.RetryEntity;
import ies.retry.spi.hazelcast.persistence.RetryId;
import ies.retry.spi.hazelcast.util.TSHolderComparator;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceException;

import provision.services.logging.Logger;

public class StoreOp extends AbstractOp<Void>{

	private final static String CALLER = StoreOp.class.getName();
	private boolean update;
	
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
		else if (mergePolicy == DBMergePolicy.ORDER_TS)
			findAndMergePayload(em, false);
		else if ( mergePolicy == DBMergePolicy.ORDER_TS_DISCARD_DUP_TS)
			findAndMergePayload(em, true);
		
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
			Logger.error(CALLER, "Find_Update_Payload_PersistenceException", "Exception Message: " + e.getMessage(),e);
			
		}catch (Exception e) {
			Logger.error(CALLER, "Find_Update_Payload_Exception", "Exception Message: " + e.getMessage(),e);
		}
	}
	
	
	//merge
	//use traversal, and order by TS
	//strip
	private void findAndMergePayload(EntityManager em,boolean stripDupTs) {
		RetryEntity entity = em.find(RetryEntity.class, new RetryId(storeId, retryType));
		
		try {
			if (entity != null) {
				TreeSet<RetryHolder> mergeSet = new TreeSet<RetryHolder>(new TSHolderComparator());
				List<RetryHolder> dbList = entity.fromByte(entity.getRetryData());
				
				mergeSet.addAll(dbList);
				mergeSet.addAll(getListHolder());
				
				//back to list
				ArrayList<RetryHolder> mergeList = new ArrayList<RetryHolder>(mergeSet.size());
				mergeList.addAll(mergeSet);

				if (stripDupTs) {
					//strip all that had the same create TS
					long lastTs = -1;
					Iterator<RetryHolder> iter = mergeList.iterator();
					while(iter.hasNext()) {
						RetryHolder holder = iter.next();
						if (lastTs == holder.getSystemTs())
							iter.remove();
					}
				}
				entity.populate(mergeList);
				em.persist(entity);
			} else {
				em.persist(new RetryEntity(listHolder));
			}
		}catch (PersistenceException e) {
			//This may have different concerns
			Logger.error(CALLER, "Find_Merge_Payload", "Exception Message: " + e.getMessage(),e);
			
		}catch (Exception e) {
			Logger.error(CALLER, "Find_Merge_Payload", "Exception Message: " + e.getMessage(),e);
		}
	}
	

	@Override
	public boolean allowExceptionHandling() {
		return true;
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
