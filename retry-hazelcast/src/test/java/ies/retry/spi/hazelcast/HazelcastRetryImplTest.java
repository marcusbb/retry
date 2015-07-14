package ies.retry.spi.hazelcast;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import ies.retry.Retry;
import ies.retry.RetryCallback;
import ies.retry.RetryConfigManager;
import ies.retry.RetryConfiguration;
import ies.retry.RetryHolder;
import ies.retry.RetryManager;
import ies.retry.spi.hazelcast.config.HazelcastXmlConfig;
import ies.retry.spi.hazelcast.persistence.RetryEntity;
import ies.retry.spi.hazelcast.persistence.RetryId;
import ies.retry.spi.hazelcast.persistence.RetryMapStore;
import ies.retry.spi.hazelcast.persistence.RetryMapStoreFactory;
import ies.retry.spi.hazelcast.persistence.TestUtils;
import ies.retry.xml.XMLRetryConfigMgr;

import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;


public class HazelcastRetryImplTest {
	private static EntityManagerFactory emf;
	private final static String XML_CONFIG = "retry_config_persistence_archived.xml";
	private final static String ARCHIVE_ON_CONFIG = "ARCHIVE_ON";
	private final static String ARCHIVE_OFF_CONFIG = "ARCHIVE_OFF";
	private static String ORIG_XML_FILE;
	
	@BeforeClass
	public static void setUpBeforeClass() throws Throwable {
		HzIntegrationTestUtil.beforeClass();
		ORIG_XML_FILE = XMLRetryConfigMgr.DEFAULT_XML_FILE;
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

		//emf = PersistenceUtil.getEMFactory("retryPool");
		emf = Persistence.createEntityManagerFactory("retryPool");
		HazelcastXmlConfig config = new HazelcastXmlConfig();
		config.getPersistenceConfig().setON(true);
		
		RetryMapStoreFactory.getInstance().init(config);
		RetryMapStoreFactory.getInstance().setEMF(emf);
		
		
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
		RetryMapStoreFactory.getInstance().shutdown();
		Retry.getRetryManager().shutdown();
		Retry.setRetryManager(null);
		XMLRetryConfigMgr.setXML_FILE(ORIG_XML_FILE);
		HzIntegrationTestUtil.afterClass();
	}

	@After
	public void after() {
	}
	
	@Test
	public void archiveTest(){
		String id = "OPOSSUM_BITES_" + System.currentTimeMillis();
		RetryManager retryManager = Retry.getRetryManager();
		RetryHolder holder = new RetryHolder(id, ARCHIVE_ON_CONFIG);
		retryManager.archiveRetry(holder);
		
		EntityManager em = emf.createEntityManager();

		Number count = (Number) em.createNativeQuery("SELECT count(1) FROM retries_archive r where natural_identifier='"+id+"'").getSingleResult();
		assertEquals("Archiving failed", count.intValue(), 1);
		count = (Number) em.createNativeQuery("SELECT count(1) FROM retries r where natural_identifier='"+id+"'").getSingleResult();
		assertEquals("Regular item was created", count.intValue(), 0);
		
	}
	
	
}
