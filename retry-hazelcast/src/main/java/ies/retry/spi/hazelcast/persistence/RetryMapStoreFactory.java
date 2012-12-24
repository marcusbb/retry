package ies.retry.spi.hazelcast.persistence;

import ies.retry.spi.hazelcast.config.HazelcastXmlConfig;
import ies.retry.spi.hazelcast.config.PersistenceConfig;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import provision.services.logging.Logger;

public class RetryMapStoreFactory {//implements MapStoreFactory<String, List<RetryHolder>>{
	private static final String CALLER = RetryMapStoreFactory.class.getName();
	
	private String pu = "retry";
	private EntityManagerFactory emf = null;
	private PersistenceConfig persistConfig;

	private static RetryMapStoreFactory instance = null;
	
	ExecutorService execService = null;
	
	static {
		instance = new RetryMapStoreFactory();
	}
	protected RetryMapStoreFactory() {
		
	}
	
	public static RetryMapStoreFactory getInstance() {
		return instance;
	}
	//for testing
	public static void setInstance(RetryMapStoreFactory inst) {
		instance = inst;
	}
	/**
	 * TODO: make this configurable
	 * @param mapName
	 * @param properties
	 * @return
	 */
	public RetryMapStore newMapStore(String mapName) {
		//Logger.info(CALLER, "Getting store factory");
		
		
		if (persistConfig.isON()) {
			EntityManagerFactory i_emf = getEMF();
			RetryMapStore store =  new RetryMapStore(mapName,i_emf,persistConfig);
			store.setExecService(execService);
			return store;
		}
		return new RetryMapNoStore(mapName,null);
	}
	/**
	 * This MUST be called before instance is initialized
	 * @param config
	 */
	public void init(HazelcastXmlConfig config) {
		this.pu = config.getPersistenceConfig().getJpaPU();
		this.persistConfig = config.getPersistenceConfig();
		
		Logger.info(CALLER, "Retry_Map_Store_Init", "Loaded persistence configuration: " + persistConfig);
		
		if (persistConfig.getQueuePolicy() == PersistenceConfig.ThreadQueuePolicy.LINKED) {
			execService = new ThreadPoolExecutor(persistConfig.getCoreSize(), persistConfig.getMaxPoolSize(),
				0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>() );
		}else if (persistConfig.getQueuePolicy() == PersistenceConfig.ThreadQueuePolicy.ARRAY) {
			execService = new ThreadPoolExecutor(persistConfig.getCoreSize(), persistConfig.getMaxPoolSize(),
					0L, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<Runnable>(persistConfig.getBoundedQueueSize()) );
		}else if (persistConfig.getQueuePolicy() == PersistenceConfig.ThreadQueuePolicy.SYNC) {
			execService = new ThreadPoolExecutor(persistConfig.getCoreSize(), persistConfig.getMaxPoolSize(),
					0L, TimeUnit.MILLISECONDS, new SynchronousQueue<Runnable>() );
		}
		
		long start = System.currentTimeMillis();
		if (persistConfig.isON()) {
			emf = Persistence.createEntityManagerFactory(pu);
		}

		Logger.info(CALLER, "Retry_Map_Store_Init", "created exec service, EMF creation time:" + (System.currentTimeMillis()-start));
	}
	
	private EntityManagerFactory getEMF() {

		return emf;
	}
	
	public void setEMF(EntityManagerFactory emf) {
		this.emf = emf;
	}
	
	public void shutdown() {
		if (execService != null)
			execService.shutdown();
	}
}
