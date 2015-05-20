package ies.retry.spi.hazelcast;

import ies.retry.Retry;
import ies.retry.RetryHolder;
import ies.retry.spi.hazelcast.config.HazelcastConfigManager;
import ies.retry.spi.hazelcast.persistence.DBMergePolicy;
import ies.retry.spi.hazelcast.persistence.RetryMapStore;
import ies.retry.spi.hazelcast.persistence.RetryMapStoreFactory;
import ies.retry.spi.hazelcast.persistence.cassandra.CassRetryEntity;
import ies.retry.spi.hazelcast.persistence.cassandra.CassRetryMapStore;
import ies.retry.spi.hazelcast.util.HzUtil;
import ies.retry.xml.XMLRetryConfigMgr;

import java.util.ArrayList;
import java.util.Collection;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.hazelcast.config.ClasspathXmlConfig;
import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;

import driver.em.DefaultEntityManager;

public class StateManagerSyncTest {

	static HazelcastRetryImpl retry = null;
	
	
	DefaultEntityManager<CassRetryEntity.Id, CassRetryEntity> em = null;
	
	
	protected void loadRandomData(int rows,String type) throws Exception {
		
		RetryMapStore store = RetryMapStoreFactory.getInstance().newMapStore(type);
		
		for (int i=0;i<rows;i++) {
			ArrayList<RetryHolder> list = new ArrayList<>();
			list.add(new RetryHolder("id-"+i, type, null, "useful stuff here"));
			store.store(list, DBMergePolicy.OVERWRITE);
		}
	}
	protected void cleanData(String type) {
		CassRetryMapStore store = (CassRetryMapStore)RetryMapStoreFactory.getInstance().newMapStore(type);
		Collection<CassRetryEntity> results = store.loadAll(RetryMapStoreFactory.getInstance().getConfig().getCqlReaderConfig(),null);
		for (CassRetryEntity entity:results) {
			store.delete(entity.getId().getId());
		}
	}
	@Before
	public void before() throws Exception {
		HzIntegrationTestUtil.beforeClass();
		XMLRetryConfigMgr.setXML_FILE("retry_config_cassandra.xml");
		HzUtil.HZ_CONFIG_FILE = "hazelcast_multilocal.xml";
		
		retry = (HazelcastRetryImpl) Retry.getRetryManager();
		//Load rows
		loadRandomData(1024, "any");
		
	}

	@After
	public void after() {
		HzIntegrationTestUtil.afterClass();
		//clean up
		cleanData("any");
	}

	@Test
	public void testLoaded() throws InterruptedException {
		retry.getStateMgr().syncGridAndStore();
		int tries = 0;
		while (tries > 5) {
			Thread.sleep(2000);
			if (retry.getH1().getMap("any").size() >= 1024)
				break;
			tries++;
		}
		org.junit.Assert.assertTrue(tries <= 5);
		
	}
	
	@Test
	public void failToLoad1HzInstance() throws InterruptedException {
		((HazelcastConfigManager)retry.getConfigManager()).getRetryHzConfig().setMinMembersToSyncStore(2);
				
		
		
		retry.getStateMgr().syncGridAndStore();
		Thread.sleep(1000);
		org.junit.Assert.assertEquals(0, retry.getH1().getMap("any").size());
		Assert.assertTrue(retry.getStateMgr().gridEmpty("any"));
		
		
	}
	
	@Test
	public void loadWith2HzInstances() throws InterruptedException {
		((HazelcastConfigManager)retry.getConfigManager()).getRetryHzConfig().setMinMembersToSyncStore(2);
		Config hzConfig = new ClasspathXmlConfig(HzUtil.HZ_CONFIG_FILE);
		HazelcastInstance hzInst = Hazelcast.newHazelcastInstance(hzConfig);
		
		testLoaded();
		
		hzInst.getLifecycleService().shutdown();
		
	}

	@Test
	public void overcapacity() throws Exception {
		
		loadRandomData(4096, "any");
		
		retry.getStateMgr().syncGridAndStore();
		
		Thread.sleep(1000);
		org.junit.Assert.assertEquals(0, retry.getH1().getMap("any").size());
		Assert.assertTrue(retry.getStateMgr().gridEmpty("any"));
		
		
	}
}
