package ies.retry.spi.hazelcast.disttasks;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import ies.retry.Retry;
import ies.retry.RetryCallback;
import ies.retry.RetryHolder;
import ies.retry.RetryManager;
import ies.retry.spi.hazelcast.HazelcastRetryImpl;
import ies.retry.spi.hazelcast.HzIntegrationTestUtil;
import ies.retry.spi.hazelcast.config.HazelcastXmlConfig;
import ies.retry.spi.hazelcast.persistence.ArchivedRetryEntity;
import ies.retry.spi.hazelcast.persistence.DBMergePolicy;
import ies.retry.spi.hazelcast.persistence.RetryEntity;
import ies.retry.spi.hazelcast.persistence.RetryId;
import ies.retry.spi.hazelcast.persistence.RetryMapStore;
import ies.retry.spi.hazelcast.persistence.RetryMapStoreFactory;
import ies.retry.xml.XMLRetryConfigMgr;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import static junit.framework.Assert.*;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.hazelcast.core.IMap;

import test.util.PersistenceUtil;

/*
 * 
 * @author akharchuk
 */
@Ignore
public class DistCallBackTaskTest {
	
	private static EntityManagerFactory emf;
	private static RetryMapStore onMapStore = null;
	private static RetryMapStore offMapStore = null;
	private EntityManager em = emf.createEntityManager();
	
	private final static String XML_CONFIG = "retry_config_persistence_archived.xml";
	private final static String ARCHIVE_ON_CONFIG = "ARCHIVE_ON";
	private final static String ARCHIVE_OFF_CONFIG = "ARCHIVE_OFF";
	private static String ORIG_XML_FILE;
	
	private static RetryManager retryManager;

	@BeforeClass
	public static void setUpBeforeClass() throws Throwable {
		ORIG_XML_FILE = XMLRetryConfigMgr.XML_FILE;
		XMLRetryConfigMgr.XML_FILE = XML_CONFIG;
		Retry.setRetryManager(null);
		
		emf = PersistenceUtil.getEMFactory("retryPool");
		HzIntegrationTestUtil.beforeClass();
		HazelcastXmlConfig config = new HazelcastXmlConfig();
		config.getPersistenceConfig().setON(true);

		RetryMapStoreFactory.getInstance().init(config);
		RetryMapStoreFactory.getInstance().setEMF(emf);
		onMapStore = RetryMapStoreFactory.getInstance().newMapStore(
				ARCHIVE_ON_CONFIG);
		offMapStore = RetryMapStoreFactory.getInstance().newMapStore(
				ARCHIVE_OFF_CONFIG);
		
		retryManager = Retry.getRetryManager();
		RetryCallback callback = new RetryCallback() {
			
			@Override
			public boolean onEvent(RetryHolder retry) throws Exception {
				throw new Exception("TEST");
			}
		};
		
		retryManager.registerCallback(callback, ARCHIVE_ON_CONFIG);
		retryManager.registerCallback(callback, ARCHIVE_OFF_CONFIG);

	}

	@AfterClass
	public static void afterClass() {
		//onMapStore.deleteByType();
		//offMapStore.deleteByType();
		RetryMapStoreFactory.getInstance().shutdown();
		retryManager.shutdown();
		Retry.setRetryManager(null);
		XMLRetryConfigMgr.setXML_FILE(ORIG_XML_FILE);
		HzIntegrationTestUtil.afterClass();
	}

	@After
	public void after() {
		// brute force method
		em.getTransaction().begin();
		em.createNativeQuery("delete from RETRIES").executeUpdate();
		em.getTransaction().commit();
	}

	private void testArchiving(boolean archive) throws Exception{
		String type = !archive ? "ARCHIVE_OFF" : "ARCHIVE_ON";
		String key = "key_" + archive + "_" + System.currentTimeMillis();
		
		IMap<String,List<RetryHolder>> retryMap = HazelcastRetryImpl.getHzInst().getMap(type);
		 
		List<RetryHolder> listHolder = new ArrayList<RetryHolder>();
		RetryHolder holder = new RetryHolder(key, type,
				new IOException("Better to remain silent and be thought a fool, than to speak and remove all doubt."), "HaHaha");
		holder.setNextAttempt(new Long(0));
		holder.setCount(1000000);
		holder.setSystemTs(System.currentTimeMillis()-1000*60*60*24*7);
		holder.setNextAttempt(0L);
		listHolder.add(holder);
		retryMap.lock(key);
		retryMap.put(key, listHolder);
		
		RetryMapStore mapStore = archive ? onMapStore : offMapStore;
		mapStore.store(listHolder, DBMergePolicy.OVERWRITE);

		DistCallBackTask task = new DistCallBackTask(listHolder, archive);
		task.call();
		listHolder = mapStore.load(key);
		assertNull("Item was not archived (still in RETRIES table)", listHolder);
		
		
		RetryEntity entity = em.find(RetryEntity.class, new RetryId(key, type));
		ArchivedRetryEntity archEntity = em.find(ArchivedRetryEntity.class, new RetryId(key, type));

		if(archive){
			assertNotNull("Arhived Entity doesn't exist in ARCHIVED_RETRY table", archEntity);
			assertNull(entity);
		//	assertNull("Retry Holder was not removed for archived=" + archive, retryMap.get(key));
		}
		else{
			assertNull(entity);
			assertNull("Arhived Entity still exists in RETRY table", archEntity);
		//	assertNull("Retry Holder was not removed for archived=" + archive, retryMap.get(key));
		}
		retryMap.unlock(key);
	}
	

	@Test
	public void testArchivingOff() throws Exception{
		testArchiving(false);
	}

	
	@Test
	public void testArchivingOn() throws Exception{
		testArchiving(true);
	}
	
}

