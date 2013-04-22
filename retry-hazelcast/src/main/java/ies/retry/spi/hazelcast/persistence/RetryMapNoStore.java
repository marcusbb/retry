package ies.retry.spi.hazelcast.persistence;

import ies.retry.RetryHolder;
import ies.retry.spi.hazelcast.config.PersistenceConfig;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManagerFactory;

/**
 * The no read/no write store.
 * Used for the {@link PersistenceConfig#isON()} == false case
 * 
 * @author msimonsen
 *
 */
public class RetryMapNoStore extends RetryMapStore {

	public RetryMapNoStore(String mapName, EntityManagerFactory emf) {
		//super(mapName, emf);
		
	}

	@Override
	public List<RetryHolder> load(String key) {
		return new ArrayList<RetryHolder>();
	}

	@Override
	public Map<String, List<RetryHolder>> load(int batchSize) {
		return new HashMap<String, List<RetryHolder>>();
	}

	@Override
	public int count() {
		return 0;
	}

	

	@Override
	public void store(List<RetryHolder> value, DBMergePolicy mergePolicy) {
		
	}

	@Override
	public void store(String key, List<RetryHolder> value,
			DBMergePolicy mergePolicy) {
		
	}

	@Override
	public void storeAll(Map<String, List<RetryHolder>> map) {
		
	}

	@Override
	public void delete(String key) {
		
	}

	@Override
	public void deleteByType() {
		
	}

	@Override
	public void archive(final List<RetryHolder> list, boolean removeEntity){
	}

}
