package ies.retry.spi.hazelcast.persistence;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import ies.retry.Retry;
import ies.retry.RetryCallback;
import ies.retry.RetryConfigManager;
import ies.retry.RetryConfiguration;
import ies.retry.RetryHolder;
import ies.retry.RetryManager;
import ies.retry.spi.hazelcast.HzIntegrationTestUtil;
import ies.retry.spi.hazelcast.config.HazelcastXmlConfig;
import ies.retry.xml.XMLRetryConfigMgr;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Query;

import static junit.framework.Assert.*;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import test.util.PersistenceUtil;

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

		emf = PersistenceUtil.getEMFactory("retryPool");
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

	@After
	public void after() {
		// brute force method
		EntityManager em = emf.createEntityManager();
		em.getTransaction().begin();
		em.createNativeQuery("delete from RETRIES").executeUpdate();
		em.getTransaction().commit();
	}


	@Test
	/* Verifies whether archived records are not loaded from DB */
	public void testDataArchived() throws ClassNotFoundException, IOException {
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

		Query query = em
				.createQuery("SELECT count(r.id.id) FROM RetryEntity r");
		Long countResult = (Long) query.getSingleResult();
		assertEquals(1, countResult.intValue());

		onMapStore.archive(key);
		
		query = em
				.createQuery("SELECT count(r.id.id) FROM RetryEntity r");
		countResult = (Long) query.getSingleResult();
		assertEquals(0, countResult.intValue());
		
		ArchivedRetryEntity archivedEntity = em.find(ArchivedRetryEntity.class, new RetryId(key,ARCHIVE_ON_CONFIG));
		assertNotNull(archivedEntity);
		em.close();
	}

	
}
