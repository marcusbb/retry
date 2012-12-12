package ies.retry.spi.hazelcast.persistence;

import ies.retry.RetryHolder;
import ies.retry.spi.hazelcast.config.HazelcastConfigManager;
import ies.retry.spi.hazelcast.config.HazelcastXmlConfig;
import ies.retry.spi.hazelcast.persistence.DBMergePolicy;
import ies.retry.spi.hazelcast.persistence.RetryEntity;
import ies.retry.spi.hazelcast.persistence.RetryId;
import ies.retry.spi.hazelcast.persistence.RetryMapStore;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import junit.framework.Assert;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import test.util.PersistenceUtil;

/**
 * This is really an persistence integration test.
 * 
 * @author msimonsen
 *
 */
public class RetryEntityTest {

	private static EntityManagerFactory emf;
	private static RetryMapStore mapStore = null;
	
	@BeforeClass
    public static void setUpBeforeClass() throws Throwable 
    {
		emf = PersistenceUtil.getEMFactory("retryPool");
		HazelcastXmlConfig config = new HazelcastXmlConfig();
		config.getPersistenceConfig().setON(true);
		RetryMapStoreFactory.getInstance().init(config);
		RetryMapStoreFactory.getInstance().setEMF(emf);
	}
	
	@AfterClass
	public static void afterClass() {
		mapStore.deleteByType();
		RetryMapStoreFactory.getInstance().shutdown();
	}
	@Before
	public void before()
	{
		mapStore = RetryMapStoreFactory.getInstance().newMapStore("POKE_BY_PIN");
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
	public void insertAndUpdate() throws Exception {
		//RetryMapStore mapStore = new RetryMapStore("POKE_BY_PIN", emf);
		
		mapStore.setWriteSync(true);
		String key = "1233412";
		
		mapStore.delete(key);
		RetryHolder holder = new RetryHolder(
				key,
				"POKE_BY_PIN",
				new IOException("Houston there is a problem"),
				"Useful Serializable object");
		
				
		ArrayList<RetryHolder> list = new ArrayList<RetryHolder>();
		list.add(holder);
		mapStore.store(list,DBMergePolicy.OVERWRITE);
		
		EntityManager em = emf.createEntityManager();
		
		System.out.println("version 0: " + mapStore.getEntity(key).getVersion());
		list.get(0).setRetryData("Modified");			
		mapStore.store(list, DBMergePolicy.FIND_OVERWRITE);
		
		System.out.println("version 1: " + mapStore.getEntity(key).getVersion());
		
		
		RetryEntity entity = em.find(RetryEntity.class, new RetryId(key, "POKE_BY_PIN"));
		em.getTransaction().begin();
		//em.lock(entity, LockModeType.READ);
		System.out.println("version: " + entity.getVersion());
		entity.setRetryData("more stuff".getBytes());
		
		em.persist(entity);
		em.getTransaction().commit();
		System.out.println("version: " + entity.getVersion());
		
	}
	
	@Test
	public void insertDup() throws Exception {
		//RetryMapStore mapStore = new RetryMapStore("POKE_BY_PIN", emf);
		mapStore.setWriteSync(true);
		String key = "1233412";
				
		RetryHolder holder = new RetryHolder(
				key,
				"POKE_BY_PIN",
				new IOException("Houston there is a problem"),
				"Useful Serializable object");
		
				
		ArrayList<RetryHolder> list = new ArrayList<RetryHolder>();
		list.add(holder);
		mapStore.store(list,DBMergePolicy.OVERWRITE);
		
		try {
			mapStore.store(list, DBMergePolicy.OVERWRITE);
			Assert.fail("Should not allow duplicate inserts");
		}catch (Exception e) {
			e.printStackTrace();
		}
		
		
	}
	
	@Test
	public void insertAndReadBatch() {
		//RetryMapStore mapStore = new RetryMapStore("POKE_BY_PIN", emf);
		mapStore.setWriteSync(true);
		String key = "1233412lol";
		HashMap<String, List<RetryHolder>> map = new HashMap<String, List<RetryHolder>>();
		
		int totalSize = 100;
		for (int i=0;i<totalSize;i++) {
			String iKey = key + i;
			RetryHolder holder = new RetryHolder(
					iKey,
					"POKE_BY_PIN",
					new IOException("Houston there is a problem"),
					"Useful Serializable object");
			
					
			ArrayList<RetryHolder> list = new ArrayList<RetryHolder>();
			list.add(holder);
			map.put(iKey, list);
		}
		
		mapStore.storeAll(map);
		
		//load mod at a time
		int mod = 30;
		int totalFetch = 0;
		Map<String,List<RetryHolder>> loadMap;
		for (int i=0;i<100;i+=mod) {
			loadMap = mapStore.load(mod);
			
			System.out.println("got results: " + loadMap.size());
			if (i<90)
				Assert.assertEquals(30, loadMap.size());
			totalFetch += loadMap.size();
		}
		Assert.assertEquals(100, totalFetch);
		System.out.println("total: " + totalFetch);
		
		loadMap = mapStore.load(mod);
		Assert.assertEquals(0, loadMap.size());
	}
	

	@Test
	public void insertAndReadBatch2() {
		//RetryMapStore mapStore = new RetryMapStore("POKE_BY_PIN", emf);
		mapStore.setWriteSync(true);
		String key = "1233412";
		HashMap<String, List<RetryHolder>> map = new HashMap<String, List<RetryHolder>>();
		
		int totalSize = 1;
		for (int i=0;i<totalSize;i++) {
			String iKey = key + i;
			RetryHolder holder = new RetryHolder(
					iKey,
					"POKE_BY_PIN",
					new IOException("Houston there is a problem"),
					"Useful Serializable object");
			
					
			ArrayList<RetryHolder> list = new ArrayList<RetryHolder>();
			list.add(holder);
			map.put(iKey, list);
		}
		
		mapStore.storeAll(map);
		
		//load mod at a time
		int mod = 30;
		int totalFetch = 0;
		Map<String,List<RetryHolder>> loadMap = mapStore.load(mod);

		Assert.assertEquals(1, loadMap.size());
		System.out.println("total: " + totalFetch);
		
		loadMap = mapStore.load(mod);
		Assert.assertEquals(0, loadMap.size());
	}	
	
	
	@Test
	public void testCount() {
		//RetryMapStore mapStore = new RetryMapStore("POKE_BY_PIN", emf);
		mapStore.setWriteSync(true);
		String key = "1233412";
		
		HashMap<String, List<RetryHolder>> map = new HashMap<String, List<RetryHolder>>();
		int totalSize = 100;
		for (int i=0;i<totalSize;i++) {
			String iKey = key + i;
			RetryHolder holder = new RetryHolder(
					iKey,
					"POKE_BY_PIN",
					new IOException("Houston there is a problem"),
					"Useful Serializable object");
			
					
			ArrayList<RetryHolder> list = new ArrayList<RetryHolder>();
			list.add(holder);
			map.put(iKey, list);
		}
		
		mapStore.storeAll(map);
		int size = mapStore.count();
		
		Assert.assertTrue(size > 0);
		
	}
}
