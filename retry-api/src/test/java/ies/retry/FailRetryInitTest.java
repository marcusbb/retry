package ies.retry;

import ies.retry.xml.XMLRetryConfigMgr;

import java.util.HashSet;

import junit.framework.Assert;

import org.junit.Ignore;
import org.junit.Test;

/**
 * Can't redo the 
 * @author msimonsen
 *
 */
public class FailRetryInitTest {

	static volatile int retryCount = 0;
	static volatile HashSet<RetryManager> retryManagers = new HashSet<RetryManager>();
	
	/**
	 * This test can't be performed with other tests as it requires singleton
	 * re-intiatization - 
	 * TODO: investigate JMockit with separate class loading capabilities. 
	 * 
	 * @throws Exception
	 */
	@Test
	public void concurrentWithFixedThread() throws Exception {
		String lastXML = XMLRetryConfigMgr.getXML_FILE();
		//this file doesn't exist and will throw an exception
		XMLRetryConfigMgr.setXML_FILE("retry_config_error.xml");
		
		try {
			RetryManager retryManager = Retry.getRetryManager();
		}catch (ConfigException e) {
			Assert.assertTrue(e != null);
		}
		
		XMLRetryConfigMgr.setXML_FILE(lastXML);
	}
}
