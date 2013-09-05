package ies.retry.spi.hazelcast.persistence.ops;

import javax.persistence.EntityManager;

public interface OpResult<T> {

	public EntityManager getEM();
}
