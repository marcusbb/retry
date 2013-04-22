package ies.retry.spi.hazelcast;

import ies.retry.Retry;
import ies.retry.RetryCallback;
import ies.retry.RetryConfigManager;
import ies.retry.RetryConfiguration;
import ies.retry.RetryHolder;
import ies.retry.spi.hazelcast.config.HazelcastXmlConfig;
import ies.retry.spi.hazelcast.persistence.RetryMapStoreFactory;
import ies.retry.spi.hazelcast.util.IOUtil;
import ies.retry.xml.XMLRetryConfigMgr;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import provision.services.logging.Logger;
import test.util.PersistenceUtil;

public class DataLossUponLoadingFromDBTest {

	private final static String type = "DT5254787";
	private final static String id = "JB_Believe";
	private final static String XML_CONFIG = "retry_config_persistence_archived.xml";
	private static String ORIG_XML_FILE;
	private static int count = 50;

	static HazelcastRetryImpl retryManager;

	@BeforeClass
	public static void beforeClass() throws IOException {

		EntityManagerFactory emf = PersistenceUtil.getEMFactory("retryPool");
		EntityManager em = emf.createEntityManager();
		em.getTransaction().begin();
		em.createNativeQuery(
				"delete from RETRIES where retry_type in ('" + type + "')")
				.executeUpdate();

		// create 50retries
		for (int j = 0; j < count; j++) {
			List<RetryHolder> list = new ArrayList<RetryHolder>();
			RetryHolder r = new RetryHolder(id + "_" + j, type);
			r.setSystemTs(1000000);

			r.setRetryData(j
					+ "_0_"
					+ "The Incy Wincy Spider climbed up the water spout.  Down came the rain, and washed the spider out.   Out came the sun, and dried up all the rain  And the Incy Wincy Spider climbed up the spout again. ");
			r = new RetryHolder(id + "_" + j, type);
			r.setSystemTs(2000000);
			r.setRetryData(j
					+ "_1_"
					+ "The Incy Wincy Spider climbed up the water spout.  Down came the rain, and washed the spider out.   Out came the sun, and dried up all the rain  And the Incy Wincy Spider climbed up the spout again. ");
			list.add(r);
			int i = 1;
			em.createNativeQuery(
					"INSERT INTO RETRIES (RETRY_TYPE, NATURAL_IDENTIFIER, RETRY_VER, PAYLOAD_DATA) "
							+ "       VALUES(?, ?, ?, ?)")
					.setParameter(i++, r.getType())
					.setParameter(i++, r.getId()).setParameter(i++, 0)
					.setParameter(i++, IOUtil.serialize(list)).executeUpdate();

		}
		em.getTransaction().commit();
		em.close();

		HzIntegrationTestUtil.beforeClass();

		ORIG_XML_FILE = XMLRetryConfigMgr.XML_FILE;
		XMLRetryConfigMgr.XML_FILE = XML_CONFIG;
		retryManager = (HazelcastRetryImpl) Retry.getRetryManager();
		// Retry.setRetryManager(null);
		emf = PersistenceUtil.getEMFactory("retryPool");
		HazelcastXmlConfig config = new HazelcastXmlConfig();
		config.getPersistenceConfig().setON(true);

		RetryMapStoreFactory.getInstance().init(config);
		RetryMapStoreFactory.getInstance().setEMF(emf);

		RetryConfigManager configManager = retryManager.getConfigManager();
		RetryConfiguration cloned = configManager
				.cloneConfiguration("ARCHIVE_ON");
		cloned.setType(type);
		retryManager.getConfigManager().addConfiguration(cloned);

		RetryCallback callback = new RetryCallback() {

			@Override
			public boolean onEvent(RetryHolder retry) throws Exception {
				Logger.info("DT5254787Test", "RetryCallback_onEvent", "Retried");
				Thread.sleep(1000000000); // never completes
				return true;
			}
		};

		retryManager.registerCallback(callback, type);

	}

	@AfterClass
	public static void afterClass() {
		retryManager.shutdown();
		HzIntegrationTestUtil.afterClass();
	}

	/*
	 * This test is trying to make in-memory retries overwritten by ones loaded
	 * from DB
	 */
	@Test
	public void inMemoryLossTest() throws Exception {

		boolean triggerLoad = true;
		for (int i = 0; i < count; i++) {

			final RetryHolder r = new RetryHolder(id + "_" + i, type);
			r.setRetryData(i
					+ "_2_"
					+ "The Incy Wincy Spider climbed up the water spout.  Down came the rain, and washed the spider out.   Out came the sun, and dried up all the rain  And the Incy Wincy Spider climbed up the spout again. ");
			retryManager.addRetry(r);

			if (triggerLoad) {
				Thread t = new Thread() {
					public void run() {
						retryManager.getStateMgr().init();
					}
				};
				t.start();
				triggerLoad = false;
			}
		}
		retryManager.getStateMgr().init();
		Thread.sleep(5000); // allow data to be loaded

		for (int i = 0; i < count; i++) {
			List<RetryHolder> holderList = (List<RetryHolder>) HazelcastRetryImpl
					.getHzInst().getMap(type).get(id + "_" + i);
			System.out.println("holderList=" + holderList);
			if (holderList != null && holderList.size() > 0) {

				boolean hasSecondInsert = false;
				for (RetryHolder r : holderList) {
					String flag = (String) r.getRetryData(); // indicates whether retry is originating from DB or memory
					String temp[] = flag.split("_");
					if ("2".equals(temp[1]))
						hasSecondInsert = true;
				}
				Assert.assertTrue(
						"A new retry added after start-up seems to be overriden",
						hasSecondInsert);
			}

		}

	}

}
