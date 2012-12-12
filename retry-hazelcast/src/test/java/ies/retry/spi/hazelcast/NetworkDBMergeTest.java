package ies.retry.spi.hazelcast;

import ies.retry.Retry;
import ies.retry.RetryHolder;
import ies.retry.spi.hazelcast.config.HazelcastConfigManager;
import ies.retry.spi.hazelcast.config.HazelcastXmlConfig;
import ies.retry.spi.hazelcast.persistence.DBMergePolicy;
import ies.retry.spi.hazelcast.persistence.RetryMapStore;
import ies.retry.spi.hazelcast.persistence.RetryMapStoreFactory;
import ies.retry.xml.XMLRetryConfigMgr;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import junit.framework.Assert;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import test.util.PersistenceUtil;

import com.hazelcast.impl.base.DataRecordEntry;
import com.hazelcast.nio.Data;
import com.hazelcast.nio.IOUtil;

/**
 *  
 * 
 *
 */
public class NetworkDBMergeTest {

	// Original hazelcast instance which is used inside NetworkMerge object
	static HazelcastRetryImpl retryInst = (HazelcastRetryImpl)Retry.getRetryManager();
	
	static HazelcastRetryImpl retryManager;
	private static EntityManagerFactory emf;
	//private static EntityManager em;
	static RetryMapStore mapStore = null;
	
	
	@BeforeClass
	public static void beforeClass() throws Exception{
		HzIntegrationTestUtil.beforeClass();
		
		emf = PersistenceUtil.getEMFactory("retryPool");
		
		XMLRetryConfigMgr.setXML_FILE("retry_config_persistence.xml");
		retryManager = new HazelcastRetryImpl();
		HazelcastXmlConfig config = new HazelcastXmlConfig();
		config.getPersistenceConfig().setON(true);
		RetryMapStoreFactory.getInstance().setEMF(emf);
		RetryMapStoreFactory.getInstance().init(config);
		mapStore = RetryMapStoreFactory.getInstance().newMapStore("POKE_BY_PIN");
		mapStore.setWriteSync(true);
		
		// overwrite configration manager in the original hazelcast instance
		retryInst.setConfigManager((HazelcastConfigManager) retryManager.getConfigManager());
	}
	@AfterClass
	public static void afterClass() {
		retryManager.shutdown();
		HzIntegrationTestUtil.afterClass();
	}
	@After
	public void after() {
		//brute force method
		EntityManager em = emf.createEntityManager();
		em.getTransaction().begin();
		em.createNativeQuery("delete from RETRIES").executeUpdate();
		em.getTransaction().commit();
	}	
	
	@Test
	public void testNoConfig() {
		NetworkMerge merge = new NetworkMerge();
		RetryHolder mergeHolder = new RetryHolder("12345", "POKE",new Exception(),new String("merge"));
		RetryHolder existHolder = new RetryHolder("12346", "POKE",new Exception(),new String("exist"));
		
		DataRecordEntry merging = getEntry(mergeHolder);
		DataRecordEntry existing = getEntry(existHolder);
		
		Object merged = merge.merge("no map", merging, existing);
		
		Assert.assertEquals( existing.getValueData(),merged);
	}
	
	@Test
	public void testDBMerge() {
		
		String key = "1233412";
		
		mapStore.delete(key);
		RetryHolder existing = new RetryHolder(
				key,
				"POKE_BY_PIN",
				new IOException("Houston there is a problem"),
				"DB OBJ");
		
				
		ArrayList<RetryHolder> list = new ArrayList<RetryHolder>();
		list.add(existing);
		mapStore.store(list,DBMergePolicy.OVERWRITE);
		
		
		NetworkMerge merge = new NetworkMerge();
		RetryHolder merging = new RetryHolder(
				key,
				"POKE_BY_PIN",
				new IOException("Houston there is a problem"),
				"NON DB object");
		
		Data data = (Data)merge.merge("POKE_BY_PIN", getEntry(merging), getEntry(existing));
		
		Assert.assertTrue(IOUtil.toObject(data) instanceof List<?>);
		
		List<RetryHolder> listHolder = (List<RetryHolder>)IOUtil.toObject(data);
		
		Assert.assertEquals(1, listHolder.size());
		
		Assert.assertEquals("DB OBJ", listHolder.get(0).getRetryData());
		
	}
	
	
	
	private static DataRecordEntry getEntry(RetryHolder holder) {
		ArrayList<RetryHolder> list = new ArrayList<RetryHolder>(1);
		list.add(holder);
		return getEntry(holder.getId(), list);
	}
	private static DataRecordEntry getEntry(String id,List<RetryHolder> holder) {
		//CMap
		
		DataRecordEntry entry = new MockDataRecordEntry(id,holder);
		
		
		return entry;
	}
}
