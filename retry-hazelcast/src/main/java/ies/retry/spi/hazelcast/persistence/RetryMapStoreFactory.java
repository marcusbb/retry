package ies.retry.spi.hazelcast.persistence;

import ies.retry.spi.hazelcast.config.ConfigListener;
import ies.retry.spi.hazelcast.config.HazelcastXmlConfig;
import ies.retry.spi.hazelcast.config.PersistenceConfig;
import ies.retry.spi.hazelcast.persistence.cassandra.CassRetryMapStore;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;

import driver.em.CUtils;


public class RetryMapStoreFactory implements ConfigListener {//implements MapStoreFactory<String, List<RetryHolder>>{
	
	private static org.slf4j.Logger logger =  org.slf4j.LoggerFactory.getLogger(RetryMapStore.class);
	
	private String pu = "retry";
	private EntityManagerFactory emf = null;
	private PersistenceConfig persistConfig;
	private static String TFactoryName = "retry-mapstore";
	private static String TFactoryPrefix = "rstore";
	
	private static RetryMapStoreFactory instance = null;
	
	ThreadPoolExecutor execService = null;
	
	private Cluster cluster = null;
	private Session session = null;
	
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
	 * 
	 * @param mapName
	 * @param properties
	 * @return
	 */
	public RetryMapStore newMapStore(String mapName) {
		//Logger.info(CALLER, "Getting store factory");
		
		if (persistConfig.isCassandra()) {
			if (cluster == null) {
				synchronized(instance) {
					cluster = CUtils.createCluster(persistConfig.getCassConfig());
					session = cluster.connect(persistConfig.getCqlReaderConfig().getKeyspace()); 
				}
			}
			return new CassRetryMapStore(mapName, session, persistConfig.isWriteSync());
		}
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
		
		logger.info( "Retry_Map_Store_Init: config={}", persistConfig);
		//ThreadFactory tFactory = new TurboThreadFactory(TFactoryName,TFactoryPrefix);
		ThreadFactory tFactory = new ThreadFactory() {
			String prefix = "RetryMapStoreTP_";
			AtomicLong suffix = new AtomicLong(1);
			@Override
			public Thread newThread(Runnable r) {
				Thread t = new Thread(r);
				t.setName(prefix +  suffix.getAndIncrement());
				return t;
			}
		};
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
		
		//Start the entity manager regardless of the configuration
		//allows toggling of persistence on and off
		//and switching implementations at runtime
		//although this is very dangerous from data integrity perspective
		try {
			emf = Persistence.createEntityManagerFactory(pu);
		}catch (Exception e) {
			logger.error( "Retry_Map_Store_Exception: msg={}", e.getMessage(),e);
			//let the client continue to get exceptions on newMapStore
		}

		logger.info( "Retry_Map_Store_Init: time={}",  (System.currentTimeMillis()-start));
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
	
	public ThreadPoolExecutor getTPE() {
		return execService;
	}
	
	@Override
	public void onConfigChange(HazelcastXmlConfig config) {
		//we can't refresh the thread pool, without complications
		//so we'll focus on the ON/OFF flag for now.
		this.persistConfig = config.getPersistenceConfig();
		
	}
	
	public PersistenceConfig getConfig() {
		return this.persistConfig;
	}
	
}
