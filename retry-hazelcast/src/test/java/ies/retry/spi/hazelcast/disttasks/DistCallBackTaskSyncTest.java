package ies.retry.spi.hazelcast.disttasks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import ies.retry.Retry;
import ies.retry.RetryCallback;
import ies.retry.RetryHolder;
import ies.retry.RetryManager;
import ies.retry.spi.hazelcast.DataLossUponLoadingFromDBTest;
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;


import com.hazelcast.core.IMap;

/*
 * 
 * @author akharchuk
 */
@Ignore /* due to race condition */ 
public class DistCallBackTaskSyncTest {
	
	private static org.slf4j.Logger logger =  org.slf4j.LoggerFactory.getLogger(DistCallBackTaskSyncTest.class);
	private static EntityManagerFactory emf;
	private static RetryMapStore mapStore = null;

	private final static String XML_CONFIG = "retry_config_persistence.xml";
	private final static String POKE = "POKE";
	private static String ORIG_XML_FILE;

	private static RetryManager retryManager;

	private int count = 0;
	// i've kept this variables in case we need to write more robust test in future
	private final boolean archive = true, single = false, retryFail = true,
			presentInRetry = true, presentInArchive = false;

	@BeforeClass
	public static void setUpBeforeClass() throws Throwable {
		HzIntegrationTestUtil.beforeClass();

		ORIG_XML_FILE = XMLRetryConfigMgr.DEFAULT_XML_FILE;
		XMLRetryConfigMgr.DEFAULT_XML_FILE = XML_CONFIG;
		retryManager = Retry.getRetryManager();
		//emf = PersistenceUtil.getEMFactory("retryPool");
		emf = Persistence.createEntityManagerFactory("retryPool");
		HazelcastXmlConfig config = new HazelcastXmlConfig();
		config.getPersistenceConfig().setON(true);

		RetryMapStoreFactory.getInstance().init(config);
		RetryMapStoreFactory.getInstance().setEMF(emf);
		mapStore = RetryMapStoreFactory.getInstance().newMapStore(POKE);

	}

	@AfterClass
	public static void afterClass() {
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
				"delete from RETRIES where retry_type in ('" + POKE + "')")
				.executeUpdate();
		em.getTransaction().commit();
	}

	@Test
	public void testArchiving() throws Exception {

		RetryCallback callback = new RetryCallback() {

			@Override
			public boolean onEvent(RetryHolder retry) throws Exception {
				logger.info("RetryCallback_onEvent", "Retried");
				if (retryFail)
					throw new Exception("TEST");
				return true;
			}
		};

		retryManager.registerCallback(callback, POKE);

		// first we create new retry list and store it in HZ and DB
		String type = POKE;
		String key = "key_" + archive + "_" + System.currentTimeMillis();

		IMap<String, List<RetryHolder>> retryMap = ((HazelcastRetryImpl)Retry.getRetryManager())
				.getHzInst().getMap(type);

		List<RetryHolder> listHolder = new ArrayList<RetryHolder>();
		RetryHolder holder = new RetryHolder(
				key,
				type,
				new IOException(
						"Better to remain silent and be thought a fool, than to speak and remove all doubt."),
				"HaHaha");
		holder.setNextAttempt(new Long(0));
		holder.setCount(count);
		holder.setSystemTs(System.currentTimeMillis() - 1000 * 60 * 60 * 24 * 7);
		holder.setNextAttempt(0L);
		listHolder.add(holder);
		if (!single) {
			for (int i = 0; i < 3; i++) {
				holder = new RetryHolder(
						key,
						type,
						new IOException(
								"Better to remain silent and be thought a fool, than to speak and remove all doubt."),
						"HaHaha");
				holder.setNextAttempt(new Long(0));
				holder.setCount(count);
				holder.setSystemTs(System.currentTimeMillis() - 1000 * 60 * 60
						* 24 * 7);
				holder.setNextAttempt(0L);
				listHolder.add(holder);
			}
		}

		mapStore.store(listHolder, DBMergePolicy.OVERWRITE);

		retryMap.lock(key);
		retryMap.put(key, listHolder);
		retryMap.unlock(key);

		// Time to de-queue
		DistCallBackTask task = new DistCallBackTask(listHolder, archive);
		task.call();
		
		EntityManager em = emf.createEntityManager();

		// verify that version was not changed as indicator that we haven't updated in DB
		RetryEntity entity = em.find(RetryEntity.class, new RetryId(key, type));

		assertEquals("Archiving failed", (presentInArchive ? 1 : 0),
				TestUtils.getNumberOfRows(em, true, type, key));
		if (!presentInRetry)
			assertNull(entity);
		else
			assertNotNull(entity);
		
		// Make sure retry count has been incremented
		List<RetryHolder> holders = retryMap.get(key);
		assertEquals(0, entity.getVersion().intValue());
		holder = holders.get(0);
		assertTrue(holder.getCount()>0);

		// reset next attempt time so that this retry is de-queued
		holder.setNextAttempt(0);
		retryMap.lock(key);
		retryMap.put(key, holders);
		retryMap.unlock(key);
		
		// starting another de-queue process
		task.call();
		entity = em.find(RetryEntity.class, new RetryId(key, type));
		// version must stay zero
		assertEquals(0, entity.getVersion().intValue());
		holders = retryMap.get(key);
		holder = holders.get(0);
		// none of the items was lost
		assertEquals(4, holders.size());
		// retry count must be incremented
		assertTrue(holder.getCount()>1);
		em.close();

	}

}
