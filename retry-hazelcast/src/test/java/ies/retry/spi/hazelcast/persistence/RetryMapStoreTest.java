package ies.retry.spi.hazelcast.persistence;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import ies.retry.Retry;
import ies.retry.RetryCallback;
import ies.retry.RetryConfigManager;
import ies.retry.RetryConfiguration;
import ies.retry.RetryHolder;
import ies.retry.RetryManager;
import ies.retry.spi.hazelcast.HzIntegrationTestUtil;
import ies.retry.spi.hazelcast.config.HazelcastXmlConfig;
import ies.retry.xml.XMLRetryConfigMgr;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/* End2End Tests for RDBMS based implementation of RetryMapStore
 * @author akharchuk
 */
public class RetryMapStoreTest {

	private static EntityManagerFactory emf;
	private static RetryMapStore onMapStore = null;
	private static RetryMapStore offMapStore = null;
	private final static String XML_CONFIG = "retry_config_persistence_archived.xml";
	private final static String ARCHIVE_ON_CONFIG = "ARCHIVE_ON";
	private final static String ARCHIVE_OFF_CONFIG = "ARCHIVE_OFF";
	private static String ORIG_XML_FILE;
	
	@BeforeClass
	public static void setUpBeforeClass() throws Throwable {
		HzIntegrationTestUtil.beforeClass();
		ORIG_XML_FILE = XMLRetryConfigMgr.XML_FILE;
		Retry.setRetryManager(null);
		XMLRetryConfigMgr.setXML_FILE(XML_CONFIG);
		
		RetryConfigManager configMgr = Retry.getRetryManager()
				.getConfigManager();
		
		Map<String, RetryConfiguration> configMap = configMgr.getConfigMap();
		assertNotNull("ARCHIVE_ON config was not loaded",
				configMap.get(ARCHIVE_ON_CONFIG));
		assertTrue("ARCHIVE_ON ", configMap.get(ARCHIVE_ON_CONFIG)
				.isArchiveExpired());
		assertNotNull("ARCHIVE_OFF config was not loaded",
				configMap.get(ARCHIVE_OFF_CONFIG));
		assertFalse("ARCHIVE_OFF ", configMap.get(ARCHIVE_OFF_CONFIG)
				.isArchiveExpired());

		emf = Persistence.createEntityManagerFactory("retryPool");
		HazelcastXmlConfig config = new HazelcastXmlConfig();
		config.getPersistenceConfig().setON(true);
		
		RetryMapStoreFactory.getInstance().init(config);
		RetryMapStoreFactory.getInstance().setEMF(emf);
		onMapStore = RetryMapStoreFactory.getInstance().newMapStore(
				ARCHIVE_ON_CONFIG);
		offMapStore = RetryMapStoreFactory.getInstance().newMapStore(
				ARCHIVE_OFF_CONFIG);
		
		
		RetryManager retryManager = Retry.getRetryManager();
		RetryCallback callback = new RetryCallback() {
			
			@Override
			public boolean onEvent(RetryHolder retry) throws Exception {
				//throw new Exception("TEST");
				return true;
			}
		};
		
		retryManager.registerCallback(callback, ARCHIVE_ON_CONFIG);
		retryManager.registerCallback(callback, ARCHIVE_OFF_CONFIG);
	}

	@AfterClass
	public static void afterClass() {
		onMapStore.deleteByType();
		offMapStore.deleteByType();
		RetryMapStoreFactory.getInstance().shutdown();
		Retry.getRetryManager().shutdown();
		Retry.setRetryManager(null);
		XMLRetryConfigMgr.setXML_FILE(ORIG_XML_FILE);
		HzIntegrationTestUtil.afterClass();
	}

	@Before
	public void before() {
		// brute force method
		EntityManager em = emf.createEntityManager();
		em.getTransaction().begin();
		int count = em.createNativeQuery("delete from RETRIES").executeUpdate();
		//System.out.println(count);
		em.getTransaction().commit();
		em.close();
	}

	@Test
	public void testStoreFindOverwriteWithBadRetrtyPresent() throws ClassNotFoundException, IOException {
		testStoreWithBadRetrtyPresent(DBMergePolicy.FIND_OVERWRITE);
	}

	@Test
	public void testStoreMergeWithBadRetrtyPresent() throws ClassNotFoundException, IOException {
		testStoreWithBadRetrtyPresent(DBMergePolicy.ORDER_TS_DISCARD_DUP_TS);
	}

	/* Verifies whether archived records are not loaded from DB */
	private void testStoreWithBadRetrtyPresent(DBMergePolicy policy) throws ClassNotFoundException, IOException {
		EntityManager em = emf.createEntityManager();
		String key = policy.name()+"_" + System.currentTimeMillis();

		// create corrupted retry first
		em.getTransaction().begin();
		RetryEntity entity = new RetryEntity();
		entity.setId(new RetryId(key, ARCHIVE_ON_CONFIG));
		byte arr[] = new byte[]{1,2,3,3,4,}; // non de-serializable
		entity.setRetryData(arr);
		em.persist(entity);
		em.flush();
		em.getTransaction().commit();
		em.close();

		// now we try to store good retry
		List<RetryHolder> list = new ArrayList<RetryHolder>();
		RetryHolder holder = new RetryHolder(key, ARCHIVE_ON_CONFIG,
				new IOException("Houston there is a problem"), "TESTTEST");
		holder.setSystemTs(System.currentTimeMillis()-1000*60*60*24*7);
		list.add(holder);
		holder = new RetryHolder(key, ARCHIVE_ON_CONFIG,
				new IOException("Got killed by gamma rays"), "HOHOHOHOHO");
		holder.setSystemTs(System.currentTimeMillis()-1000*60*60*24*14);
		list.add(holder);
		onMapStore.store(key, list, policy);
		em = emf.createEntityManager();
	
		assertEquals(1,  TestUtils.getNumberOfRows(em, false, ARCHIVE_ON_CONFIG, key));
		em.close();

		// now we try to load retry
		List<RetryHolder> savedList = onMapStore.load(key);
		assertEquals(2, list.size());
		assertEquals(savedList.size(), list.size());
	}
	
	
	@Test
	/* Verifies whether archived records are not loaded from DB */
	public void testLoadBadRetrty() throws ClassNotFoundException, IOException {
		EntityManager em = emf.createEntityManager();
		String key = "bad_" + System.currentTimeMillis();

		// create corrupted retry first
		em.getTransaction().begin();
		RetryEntity entity = new RetryEntity();
		entity.setId(new RetryId(key, ARCHIVE_ON_CONFIG));
		byte arr[] = new byte[]{1,2,3,3,4,}; // non de-serializable
		entity.setRetryData(arr);
		em.persist(entity);
		em.flush();
		em.getTransaction().commit();
		em.close();

		// now we try to load retry
		List<RetryHolder> list = onMapStore.load(key);
		assertNull(list);
		em = emf.createEntityManager();

		assertEquals(0,  TestUtils.getNumberOfRows(em, false, ARCHIVE_ON_CONFIG, key));
		em.close();
	}
	
	
	@Test
	/* Verifies whether archived records are not loaded from DB */
	public void testWholeListArchived() throws ClassNotFoundException, IOException {
		String key = "key_" + System.currentTimeMillis();

		List<RetryHolder> list = new ArrayList<RetryHolder>();
		RetryHolder holder = new RetryHolder(key, ARCHIVE_ON_CONFIG,
				new IOException("Houston there is a problem"), "TESTTEST");
		holder.setSystemTs(System.currentTimeMillis()-1000*60*60*24*7);
		list.add(holder);
		holder = new RetryHolder(key, ARCHIVE_ON_CONFIG,
				new IOException("Got killed by gamma rays"), "HOHOHOHOHO");
		holder.setSystemTs(System.currentTimeMillis()-1000*60*60*24*14);
		list.add(holder);
		onMapStore.store(key, list, DBMergePolicy.OVERWRITE);
		EntityManager em = emf.createEntityManager();

		assertEquals(1,  TestUtils.getNumberOfRows(em, false, ARCHIVE_ON_CONFIG, key));

		onMapStore.archive(list, true);
		
		assertEquals(0,  TestUtils.getNumberOfRows(em, false, ARCHIVE_ON_CONFIG, key));
		assertEquals(1, TestUtils.getNumberOfRows(em, true, ARCHIVE_ON_CONFIG, key));
		em.close();
	}
	
	@Test
	/* Verifies whether part of the list is archived*/
	public void testPartOfListArchived() throws ClassNotFoundException, IOException {
		String key = "key_" + System.currentTimeMillis();

		List<RetryHolder> list = new ArrayList<RetryHolder>();
		RetryHolder holder = new RetryHolder(key, ARCHIVE_ON_CONFIG,
				new IOException("Houston there is a problem"), "TESTTEST");
		holder.setSystemTs(System.currentTimeMillis()-1000*60*60*24*7);
		list.add(holder);
		holder = new RetryHolder(key, ARCHIVE_ON_CONFIG,
				new IOException("Got killed by gamma rays"), "HOHOHOHOHO");
		holder.setSystemTs(System.currentTimeMillis()-1000*60*60*24*14);
		list.add(holder);
		onMapStore.store(key, list, DBMergePolicy.OVERWRITE);
		EntityManager em = emf.createEntityManager();
		
		assertEquals(1,  TestUtils.getNumberOfRows(em, false, ARCHIVE_ON_CONFIG, key));

		list.remove(1);
		onMapStore.archive(list, false);
		
		
		assertEquals(1,  TestUtils.getNumberOfRows(em, false, ARCHIVE_ON_CONFIG, key));
		assertEquals(1, TestUtils.getNumberOfRows(em, true, ARCHIVE_ON_CONFIG, key));
		em.close();
	}
	
}
