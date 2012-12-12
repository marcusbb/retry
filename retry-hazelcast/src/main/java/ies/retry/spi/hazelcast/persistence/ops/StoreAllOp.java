package ies.retry.spi.hazelcast.persistence.ops;

import ies.retry.RetryHolder;
import ies.retry.spi.hazelcast.persistence.RetryEntity;

import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

public class StoreAllOp extends AbstractOp<Void> {

	private Map<String,List<RetryHolder>> map;
	
	public StoreAllOp(EntityManagerFactory emf,Map<String,List<RetryHolder>> map) {
		super(emf);
		this.map = map;
	}
	/**
	 * Possibly delete/update??
	 */
	@Override
	public Void exec(EntityManager em)throws Exception {

		for (List<RetryHolder> list:map.values()) {
			em.persist(new RetryEntity(list));
		}
		return null;
	}

}
