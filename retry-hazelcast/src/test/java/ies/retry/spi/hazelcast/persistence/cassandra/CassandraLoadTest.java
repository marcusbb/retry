package ies.retry.spi.hazelcast.persistence.cassandra;

import static org.junit.Assert.*;

import java.nio.ByteBuffer;
import java.util.Collection;

import ies.retry.Retry;
import ies.retry.RetryCallback;
import ies.retry.RetryConfiguration;
import ies.retry.RetryHolder;
import ies.retry.spi.hazelcast.HazelcastRetryImpl;
import ies.retry.spi.hazelcast.HzIntegrationTestUtil;
import ies.retry.xml.XMLRetryConfigMgr;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import provision.services.logging.Logger;
import reader.ReaderConfig;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;

import driver.em.CUtils;
import driver.em.CassConfig;
import driver.em.DefaultEntityManager;

public class CassandraLoadTest {

	static CassConfig config = new CassConfig();
	static {
		config.setNativePort(9180);
		config.setContactHostsName(new String[] { "marcus-v4.rim.net" });
	}
	static Cluster cluster = CUtils.createCluster(config);
	static Session session = CUtils.createSession(cluster, "icrs");
	
	static HazelcastRetryImpl retry = null;
	
	static DefaultEntityManager<CassRetryEntity.Id, CassRetryEntity> em = null;
	
	
	public static class FailCallback implements RetryCallback {

		@Override
		public boolean onEvent(RetryHolder retry) throws Exception {
			return false;
		}
		
	}
	@Before
	public void before() {
		retry.getH1().getMap("cass-type1").clear();
		
	}
	@After
	public void after() {
		
		CassRetryMapStore store = new CassRetryMapStore("cass-type1",session,true);
		ReaderConfig readerConfig = new ReaderConfig();
		readerConfig.setCassConfig(config);
		readerConfig.setKeyspace("icrs");
		readerConfig.setTable("retry");
		Collection<CassRetryEntity> results = store.loadAll(readerConfig);
		
		for (CassRetryEntity entity:results) {
			em.remove(entity.getId());
		}
	}
	@BeforeClass
	public static void beforeClass() {
		HzIntegrationTestUtil.beforeClass();
		XMLRetryConfigMgr.setXML_FILE("retry_config.xml");
		retry = (HazelcastRetryImpl)Retry.getRetryManager();
		RetryConfiguration config = retry.getConfigManager().cloneConfiguration("POKE");
		config.setType("cass-type1");
		config.getBatchConfig().setBatchHeartBeat(Long.MAX_VALUE);
		
		retry.getConfigManager().addConfiguration(config);
		retry.registerCallback(new CassandraLoadTest.FailCallback(), "cass-type1");
		em = new DefaultEntityManager<CassRetryEntity.Id, CassRetryEntity>(session, CassRetryEntity.class);
	}
	@AfterClass
	public static void afterClass() {
		HzIntegrationTestUtil.afterClass();
	}
	protected void loadRandomData(int rows) throws Exception {
		
		for (int i=0;i<rows;i++) {
			 
			em.persist(	new CassRetryEntity(new RetryHolder("id" +i,"cass-type1",null,"Useful Serializable" + i)) );
		}
	}
	protected void loadBadRow(int rows) throws Exception {
		
		for (int i=0;i<rows;i++) {
			CassRetryEntity entity = new CassRetryEntity();
			entity.setId(new CassRetryEntity.Id("bad"+i, "cass-type1"));
			entity.setRetryData(ByteBuffer.wrap(new byte[32]));
			em.persist(	entity );
		}
	}
	@Test
	public void loadStandard() throws Exception {
		ReaderConfig readerConfig = new ReaderConfig();
		readerConfig.setCassConfig(config);
		readerConfig.setKeyspace("icrs");
		readerConfig.setTable("retry");
		readerConfig.setOtherCols(new String[]{"payload"});
		
		
		CassRetryMapStore store = new CassRetryMapStore("cass-type1",session,true);
		
		loadRandomData(100);
		
		store.loadIntoHZ(readerConfig);
				
		assertLoadedRows(100);
		
		
	}
	@Test
	public void corruptElements() throws Exception {
		
		ReaderConfig readerConfig = new ReaderConfig();
		readerConfig.setCassConfig(config);
		readerConfig.setKeyspace("icrs");
		readerConfig.setTable("retry");
		readerConfig.setOtherCols(new String[]{"payload"});
		
		//add
		loadRandomData(10);
		loadBadRow(10);
		CassRetryMapStore store = new CassRetryMapStore("cass-type1",session,true);
		
		store.loadIntoHZ(readerConfig);
		
		assertLoadedRows(10);
	}
	
	private void assertLoadedRows(int size) throws InterruptedException {
		int tries = 0;
		while (tries <20) {
			Logger.debug(getClass().getName(),"hz map size","map_size","size",retry.getH1().getMap("cass-type1").size());
			if (retry.getH1().getMap("cass-type1").size() >= size)
				break;
			tries++;
			Thread.sleep(1000L);	
		}
		if (tries >= 20)
			Assert.fail("failed to load into HZ");
	}

}
