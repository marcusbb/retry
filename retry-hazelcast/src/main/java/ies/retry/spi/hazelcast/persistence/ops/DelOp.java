package ies.retry.spi.hazelcast.persistence.ops;


import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Query;

public class DelOp extends AbstractOp<Void>{

	
	
	static String sql = "delete from RetryEntity r where r.id.id = :key and r.id.type = :type";
	
	public DelOp(EntityManagerFactory emf,String retryType,String storeId) {
		setRetryType(retryType);
		setStoreId(storeId);
		setEmf(emf);
	}
	
	@Override
	public Void exec(EntityManager em) throws Exception {
		Query q = em.createQuery(sql);
		q.setParameter("key", storeId);
		q.setParameter("type", retryType);
		q.executeUpdate();
		return null;
	}

}
