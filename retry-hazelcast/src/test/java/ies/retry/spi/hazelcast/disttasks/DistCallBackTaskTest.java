package ies.retry.spi.hazelcast.disttasks;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import ies.retry.Retry;
import ies.retry.RetryCallback;
import ies.retry.RetryHolder;
import ies.retry.RetryManager;
import ies.retry.spi.hazelcast.HazelcastRetryImpl;
import ies.retry.spi.hazelcast.HzIntegrationTestUtil;
import ies.retry.spi.hazelcast.config.HazelcastXmlConfig;
import ies.retry.spi.hazelcast.persistence.DBMergePolicy;
import ies.retry.spi.hazelcast.persistence.RetryEntity;
import ies.retry.spi.hazelcast.persistence.RetryId;
import ies.retry.spi.hazelcast.persistence.RetryMapStore;
import ies.retry.spi.hazelcast.persistence.RetryMapStoreFactory;
import ies.retry.spi.hazelcast.persistence.TestUtils;
import ies.retry.xml.XMLRetryConfigMgr;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import static junit.framework.Assert.*;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import provision.services.logging.Logger;

import com.hazelcast.core.IMap;


/*
 * 
 * @author akharchuk
 */
@Ignore /* due to race condition */
@RunWith(Parameterized.class)
public class DistCallBackTaskTest {
	private final static String CALLER = DistCallBackTaskTest.class.getName();

	private static EntityManagerFactory emf;
	private static RetryMapStore onMapStore = null;
	private static RetryMapStore offMapStore = null;

	private final static String XML_CONFIG = "retry_config_persistence_archived.xml";
	private final static String ARCHIVE_ON_CONFIG = "ARCHIVE_ON";
	private final static String ARCHIVE_OFF_CONFIG = "ARCHIVE_OFF";
	private static String ORIG_XML_FILE;

	private static RetryManager retryManager;

	@BeforeClass
	public static void setUpBeforeClass() throws Throwable {
		HzIntegrationTestUtil.beforeClass();

		ORIG_XML_FILE = XMLRetryConfigMgr.XML_FILE;
		XMLRetryConfigMgr.XML_FILE = XML_CONFIG;
		// Retry.setRetryManager(null);
		retryManager = Retry.getRetryManager();
		emf = Persistence.createEntityManagerFactory("retryPool");
		HazelcastXmlConfig config = new HazelcastXmlConfig();
		config.getPersistenceConfig().setON(true);

		RetryMapStoreFactory.getInstance().init(config);
		RetryMapStoreFactory.getInstance().setEMF(emf);
		onMapStore = RetryMapStoreFactory.getInstance().newMapStore(
				ARCHIVE_ON_CONFIG);
		offMapStore = RetryMapStoreFactory.getInstance().newMapStore(
				ARCHIVE_OFF_CONFIG);

	}

	@AfterClass
	public static void afterClass() {
		// onMapStore.deleteByType();
		// offMapStore.deleteByType();
		RetryMapStoreFactory.getInstance().shutdown();
		retryManager.shutdown();
		Retry.setRetryManager(null);
		XMLRetryConfigMgr.setXML_FILE(ORIG_XML_FILE);
		HzIntegrationTestUtil.afterClass();
	}

	@After
	public void after() {
		// brute force method
		EntityManager em = emf.createEntityManager();
		em.getTransaction().begin();
		em.createNativeQuery(
				"delete from RETRIES where retry_type in ('"
						+ ARCHIVE_ON_CONFIG + "','" + ARCHIVE_OFF_CONFIG + "')")
				.executeUpdate();
		em.getTransaction().commit();
	}

	private int count;
	private final boolean archive, single, retryFail, presentInRetry,
			presentInArchive;

	public DistCallBackTaskTest(int count, boolean archive, boolean single,
			final boolean retryFail, boolean presentInRetry,
			boolean presentInArchive) {
		this.count = count;
		this.archive = archive;
		this.single = single;
		this.retryFail = retryFail;
		this.presentInArchive = presentInArchive;
		this.presentInRetry = presentInRetry;
	}

	@Test
	public void testArchiving() throws Exception {

		RetryCallback callback = new RetryCallback() {

			@Override
			public boolean onEvent(RetryHolder retry) throws Exception {
				Logger.info(CALLER, "RetryCallback_onEvent", "Retried");
				if (retryFail)
					throw new Exception("TEST");
				return true;
			}
		};

		retryManager.registerCallback(callback, ARCHIVE_ON_CONFIG);
		retryManager.registerCallback(callback, ARCHIVE_OFF_CONFIG);

		String type = !archive ? "ARCHIVE_OFF" : "ARCHIVE_ON";
		String key = "key_" + archive + "_" + System.currentTimeMillis();
		
		IMap<String,List<RetryHolder>> retryMap = ((HazelcastRetryImpl)Retry.getRetryManager()).getH1().getMap(type);
		 
		List<RetryHolder> listHolder = new ArrayList<RetryHolder>();
		RetryHolder holder = new RetryHolder(
				key,
				type,
				new IOException(
						"Better to remain silent and be thought a fool, than to speak and remove all doubt."),
				"HaHaha");
		holder.setNextAttempt(0);
		holder.setCount(count);
		holder.setSystemTs(System.currentTimeMillis() - 1000 * 60 * 60 * 24 * 7);
		listHolder.add(holder);
		if (!single) {
			for (int i = 0; i < 3; i++) {
				holder = new RetryHolder(
						key,
						type,
						new IOException(
								"Better to remain silent and be thought a fool, than to speak and remove all doubt."),
						"HaHaha");
				holder.setNextAttempt(0);
				holder.setCount(count - 1);
				holder.setSystemTs(System.currentTimeMillis() - 1000 * 60 * 60
						* 24 * 7);
				listHolder.add(holder);
			}
		}


		RetryMapStore mapStore = archive ? onMapStore : offMapStore;
		mapStore.store(listHolder, DBMergePolicy.OVERWRITE);
		
		retryMap.lock(key);
		retryMap.put(key, listHolder);
		retryMap.unlock(key);
		
		DistCallBackTask task = new DistCallBackTask(listHolder, archive);

		task.call();
		// listHolder = mapStore.load(key);
		// assertNull("Item was not archived (still in RETRIES table)",
		// listHolder);
		EntityManager em = emf.createEntityManager();

		RetryEntity entity = em.find(RetryEntity.class, new RetryId(key, type));

		assertEquals("Archiving failed", (presentInArchive ? 1 : 0),
				TestUtils.getNumberOfRows(em, true, type, key));
		if (!presentInRetry)
			assertNull(entity);
		else
			assertNotNull(entity);
		em.close();

	}

	public static Collection<Object[]> data() {
		Object[][] data = new Object[][] { { 1 }, { 2 }, { 3 }, { 4 } };
		return Arrays.asList(data);
	}	

	@Parameters(name="processRetries {index}: retry_count={0}, archiving enabled={1}, single item in the list={2}, retry fails={3}")
	public static Collection<Object[]> testDequeue() throws Exception {
		// two last params in array are presentInRetry, presentInArchive
		Object[][] data = { { 2, true, true, true, false, true },
				{ 2, true, true, false, false, false },
				{ 2, true, false, true, true, true },
				{ 2, true, false, false, false, false },

				{ 2, false, true, true, false, false },
				{ 2, false, true, false, false, false },
				{ 2, false, false, true, true, false },
				{ 2, false, true, false, false, false },
				{ 2, false, false, false, false, false },

				{ 0, true, true, true, true, false },
				{ 0, true, true, false, false, false },
				{ 0, true, false, true, true, false },
				{ 0, true, false, false, false, false },

				{ 0, false, true, true, true, false },
				{ 0, false, true, false, false, false },
				{ 0, false, false, true, true, false },
				{ 0, false, true, false, false, false },
				{ 0, false, false, false, false, false } };
		return Arrays.asList(data);
	}

}
