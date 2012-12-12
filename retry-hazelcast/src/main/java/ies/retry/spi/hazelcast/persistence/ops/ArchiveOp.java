package ies.retry.spi.hazelcast.persistence.ops;

import ies.retry.RetryHolder;
import ies.retry.spi.hazelcast.persistence.ArchivedRetryEntity;
import ies.retry.spi.hazelcast.persistence.RetryEntity;
import ies.retry.spi.hazelcast.persistence.RetryId;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

public class ArchiveOp extends DelOp{
	
	public ArchiveOp(EntityManagerFactory emf,String retryType,String storeId) {
		super(emf, retryType, storeId);
	}
	
	@Override
	public Void exec(EntityManager em) throws Exception {
		
		RetryEntity original = em.find(RetryEntity.class, new RetryId(storeId, retryType));
		List<RetryHolder> dbList = original.fromByte(original.getRetryData()); // we have to deserialize data to get retry timestamp 

		ArchivedRetryEntity archived = new ArchivedRetryEntity(dbList);
		archived.setVersion(original.getVersion());
		em.persist(archived);

		super.exec(em);
		return null;
	}

}
