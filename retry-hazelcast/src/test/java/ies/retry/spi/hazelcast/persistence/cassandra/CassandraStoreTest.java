package ies.retry.spi.hazelcast.persistence.cassandra;

import ies.retry.RetryHolder;
import ies.retry.spi.hazelcast.persistence.DBMergePolicy;
import ies.retry.spi.hazelcast.util.KryoSerializer;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import reader.ReaderConfig;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Session;

import driver.em.CUtils;
import driver.em.CassConfig;
import driver.em.DefaultEntityManager;

public class CassandraStoreTest {

	
	static CassConfig config = new CassConfig();
	static {
		config.setNativePort(9180);
		config.setContactHostsName(new String[] { "marcus-v4.rim.net" });
	}
	static Cluster cluster = CUtils.createCluster(config);
	static Session session = CUtils.createSession(cluster, "icrs");
		
	
	@Before
	public void before() {
		DefaultEntityManager<CassRetryEntity.Id, CassRetryEntity> em = new DefaultEntityManager<>(session, CassRetryEntity.class);
		ReaderConfig readerConfig = new ReaderConfig();
		readerConfig.setCassConfig(config);
		readerConfig.setKeyspace("icrs");
		readerConfig.setTable("retry");
		
		CassRetryMapStore store = new CassRetryMapStore("cass-type1",session);
		
		Collection<CassRetryEntity> results = store.loadAll(readerConfig);
		
		for (CassRetryEntity entity:results) {
			em.remove(entity.getId());
		}
		store.setCountTo(0);
		
	}
		
	protected List<RetryHolder> generateRetryList(int num) {
		ArrayList<RetryHolder> holder = new ArrayList<>();
		for (int i=0;i<num;i++)
			holder.add(new RetryHolder("cass-id"+i, "cass-type1",null,new String("Useful Serializable data")));
		return holder;
	}
	
	
	@Test
	public void storeAndRetrieveEntity() {
		
		
		CassRetryMapStore store = new CassRetryMapStore("cass-type1",session);
		
		store.store(generateRetryList(1), DBMergePolicy.OVERWRITE);
		
		List<RetryHolder> list = store.load("cass-id0");
		Assert.assertNotNull(list);
		
		
	}
	
	@Test
	public void storeAndDeleteCount() {
		CassRetryMapStore store = new CassRetryMapStore("cass-type1",session);
		
		store.store(generateRetryList(1), DBMergePolicy.OVERWRITE);
		
		store.delete("cass-id0");
		
		Assert.assertEquals(0,store.count());
		
	}
	
	
	
}
