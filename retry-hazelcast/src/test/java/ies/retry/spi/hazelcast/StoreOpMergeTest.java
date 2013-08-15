package ies.retry.spi.hazelcast;

import ies.retry.Retry;
import ies.retry.RetryCallback;
import ies.retry.RetryConfigManager;
import ies.retry.RetryConfiguration;
import ies.retry.RetryHolder;
import ies.retry.spi.hazelcast.config.HazelcastXmlConfig;
import ies.retry.spi.hazelcast.persistence.DBMergePolicy;
import ies.retry.spi.hazelcast.persistence.RetryMapStore;
import ies.retry.spi.hazelcast.persistence.RetryMapStoreFactory;
import ies.retry.spi.hazelcast.util.IOUtil;
import ies.retry.xml.XMLRetryConfigMgr;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import provision.services.logging.Logger;
import test.util.PersistenceUtil;
@Ignore
public class StoreOpMergeTest {

	private final static String CALLER = StoreOpMergeTest.class.getName();

	private static EntityManagerFactory emf;
	private static RetryMapStore onMapStore = null;

	private final static String XML_CONFIG = "retry_config_persistence_archived.xml";
	private final static String type = "DT5254787";
	private final static String id = "JB_Believe";
	private static String ORIG_XML_FILE;

	private static HazelcastRetryImpl retryManager;

	@Before
	public void setUpBefore() throws Throwable {
		HzIntegrationTestUtil.beforeClass();

		ORIG_XML_FILE = XMLRetryConfigMgr.XML_FILE;
		XMLRetryConfigMgr.XML_FILE = XML_CONFIG;
		// Retry.setRetryManager(null);
		retryManager = (HazelcastRetryImpl) Retry.getRetryManager();
		emf = PersistenceUtil.getEMFactory("retryPool");
		HazelcastXmlConfig config = new HazelcastXmlConfig();
		config.getPersistenceConfig().setON(true);

		RetryMapStoreFactory.getInstance().init(config);
		RetryMapStoreFactory.getInstance().setEMF(emf);

		EntityManagerFactory emf = PersistenceUtil.getEMFactory("retryPool");
		EntityManager em = emf.createEntityManager();
		em.getTransaction().begin();
		em.createNativeQuery("delete from retries where type='" + type + "'");
		em.getTransaction().commit();
		em.close();

		RetryConfigManager configManager = retryManager.getConfigManager();
		RetryConfiguration cloned = configManager
				.cloneConfiguration("ARCHIVE_ON");
		cloned.setType(type);
		retryManager.getConfigManager().addConfiguration(cloned);

		RetryCallback callback = new RetryCallback() {

			@Override
			public boolean onEvent(RetryHolder retry) throws Exception {
				Logger.info(CALLER, "RetryCallback_onEvent", "Retried");
				Thread.sleep(10000);
				return true;
			}
		};

		retryManager.registerCallback(callback, type);
	}

	@After
	public void after() {

		RetryMapStoreFactory.getInstance().shutdown();
		retryManager.shutdown();
		Retry.setRetryManager(null);
		XMLRetryConfigMgr.setXML_FILE(ORIG_XML_FILE);
		//HzIntegrationTestUtil.afterClass();
	}

	@After
	public void before() {
		// brute force method
		EntityManager em = emf.createEntityManager();
		em.getTransaction().begin();
		em.createNativeQuery(
				"delete from RETRIES where retry_type in ('" + type + "')")
				.executeUpdate();
		em.getTransaction().commit();
	}

	/*
	 * Verify merge results done by StoreOp
	 */
	@Test
	public void testDuplicatesAndNotOrdered() {
		try {
			int size = 4;
			List<RetryHolder> list = new ArrayList<RetryHolder>();
			for (int i = 0; i < size; i++) {
				RetryHolder r = new RetryHolder(id, type);
				r.setSystemTs(10000 * i);
				r.setRetryData(String.valueOf(i));
				list.add(r);
			}
			onMapStore = RetryMapStoreFactory.getInstance().newMapStore(type);
			onMapStore.store(list, DBMergePolicy.FIND_OVERWRITE); // store 4 retries

			list.remove(0);
			onMapStore = RetryMapStoreFactory.getInstance().newMapStore(type);
			onMapStore.store(list, DBMergePolicy.ORDER_TS_DISCARD_DUP_TS); // store 3 retries out of original 4 again
			
			onMapStore = RetryMapStoreFactory.getInstance().newMapStore(type);
			list = onMapStore.load(id);
			
			Assert.assertEquals(size, list.size()); // we are supposed to get 4 retries as merge result
			for (int i = 0; i < size; i++) {
				if (i > 0) {
					Assert.assertTrue(list.get(i - 1).getSystemTs() < list.get(
							i).getSystemTs()); // make sure retries are ordered
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail("Failed due to exception: " + e.getMessage());
		}

	}

	// Test whether retry count and ts are not reset when loaded from DB
	@Test
	public void testCounterAndTSResetWhenLoadingFromDB() {
		try {
			EntityManager em = emf.createEntityManager();
			em.getTransaction().begin();
			List<RetryHolder> list = new ArrayList<RetryHolder>();
			String key = id + "_0";
			RetryHolder r = new RetryHolder(key, type);
			r.setSystemTs(1000000);
			r.setCount(10);
			r.setRetryData("0_"
					+ "The Incy Wincy Spider climbed up the water spout.  Down came the rain, and washed the spider out.   Out came the sun, and dried up all the rain  And the Incy Wincy Spider climbed up the spout again. ");
			list.add(r);
			int i = 1;
			em.createNativeQuery(
					"INSERT INTO RETRIES (RETRY_TYPE, NATURAL_IDENTIFIER, RETRY_VER, PAYLOAD_DATA) "
							+ "       VALUES(?, ?, ?, ?)")
					.setParameter(i++, r.getType())
					.setParameter(i++, r.getId()).setParameter(i++, 0)
					.setParameter(i++, IOUtil.serialize(list)).executeUpdate();

			em.getTransaction().commit();
			em.close();

			retryManager.getStateMgr().init();

			int maxWait = 10;
			int cycle = 0;
			while ((list = retryManager.getRetry(key, type)) == null
					&& cycle < maxWait) {
				Thread.sleep(1000);
				retryManager.getStateMgr().init();
				cycle++;
			}

			Assert.assertNotNull(list);
			Assert.assertEquals(1, list.size());
			Assert.assertEquals(10, list.get(0).getCount());
			Assert.assertEquals(1000000, list.get(0).getSystemTs());

		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail("Failed due to exception: " + e.getMessage());
		}

	}
}
