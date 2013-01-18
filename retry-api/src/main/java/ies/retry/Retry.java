package ies.retry;

import ies.retry.xml.XMLRetryConfigMgr;
import ies.retry.xml.XmlRetryConfig;


/**
 * Entry point to Retry.
 * 
 * Wiki: <a href="http://wikis.rim.net/display/bbprv/Retry" />
 *  
 * Singleton strategy is described below.
 * 
 * @author msimonsen
 *
 */
public class Retry {

	private static RetryManager retryManager;
	
	private static Object lock = new Object();
	private static boolean init = false;
	
	private Retry() {
				
	}
	
	
	
	public static RetryManager getRetryManager() throws ConfigException { 

		if (retryManager != null)
			return retryManager;
		
		//to prevent multiple instance creation
		//employ a lock and check
		//the check is a boolean flag to indicate initialization
		
		synchronized(lock) {
			if (!init  ) {
				retryManager = init();
				
			}
		}
		
		
		
		return retryManager;
		
	}
	private static RetryManager init() {
		try {
						
			System.out.println("Instantiating Retry " + Thread.currentThread());
			XMLRetryConfigMgr configLoader = new XMLRetryConfigMgr();
			XmlRetryConfig config = configLoader.load();
			if (config.getProvider() == null)
				throw new ConfigException("No Provider");
			RetryManager inst = (RetryManager) Class.forName(config.getProvider()).newInstance();
			init = true;
			return inst;

		} catch (Exception e) {
			e.printStackTrace();
			throw new ConfigException(e);	
		}
	}
	
	
	/**
	 * For testing purposes only.
	 */
	public static void setRetryManager(RetryManager retryManager) {
		Retry.retryManager = retryManager;
		if(retryManager==null)
			init = false;
	}
	
}
