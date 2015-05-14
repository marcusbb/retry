package ies.retry.spi.hazelcast.persistence.cassandra;

import ies.retry.RetryHolder;
import ies.retry.spi.hazelcast.persistence.DBMergePolicy;
import ies.retry.spi.hazelcast.util.KryoSerializer;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
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
		
	CassRetryMapStore store = null;
	DefaultEntityManager<CassRetryEntity.Id, CassRetryEntity> em = null;
	
	@Before
	public void before() {
		em = new DefaultEntityManager<>(session, CassRetryEntity.class);
		ReaderConfig readerConfig = new ReaderConfig();
		readerConfig.setCassConfig(config);
		readerConfig.setKeyspace("icrs");
		readerConfig.setTable("retry");
		
		store = new CassRetryMapStore("cass-type1",session,true);
		
		Collection<CassRetryEntity> results = store.loadAll(readerConfig,null);
		
		for (CassRetryEntity entity:results) {
			em.remove(entity.getId());
		}
		store.setCountTo(0);
		
	}
	
	protected List<RetryHolder> generateRetryList(String idPrefix,int num) {
		ArrayList<RetryHolder> holder = new ArrayList<>();
		for (int i=0;i<num;i++) {
			RetryHolder r  = new RetryHolder(idPrefix+i, "cass-type1",null,new String("Useful Serializable data"));
			r.setSystemTs(System.currentTimeMillis());
			holder.add(r);
		}
			
			
		return holder;
	}
	protected List<RetryHolder> generateRetryList(int num) {
		return generateRetryList("cass-id", num);
	}
	
	
	@Test
	public void storeAndRetrieveEntity() {
		
		
		store.store(generateRetryList(1), DBMergePolicy.OVERWRITE);
		
		List<RetryHolder> list = store.load("cass-id0");
		Assert.assertNotNull(list);
		
		
	}
	
	@Test
	public void storeAndDeleteCount() {
		
		store.store(generateRetryList(1), DBMergePolicy.OVERWRITE);
		
		store.delete("cass-id0");
		
		Assert.assertEquals(0,store.count());
		
	}
	
	@Test 
	public void storeArchive() {
		
		DefaultEntityManager<CassArchiveRetryEntity.Id, CassArchiveRetryEntity> arcEM = new DefaultEntityManager<>(session, CassArchiveRetryEntity.class);
		
		List<RetryHolder> archive = generateRetryList("archived",1);
		store.archive(archive,false);
		CassArchiveRetryEntity arcEntity = arcEM.find(new CassArchiveRetryEntity.Id(archive.get(0).getId(), archive.get(0).getType(), new Date(archive.get(0).getSystemTs())));
		Assert.assertNotNull(arcEntity);
		
		archive = generateRetryList("tobe-archive-del",1);
		store.store(archive, DBMergePolicy.OVERWRITE);
		
		store.archive(archive, true);
		
		CassRetryEntity nentity = em.find(new CassRetryEntity.Id(archive.get(0).getId(), archive.get(0).getType()));
		
		Assert.assertNull(nentity);
	}
	
	
	
}
