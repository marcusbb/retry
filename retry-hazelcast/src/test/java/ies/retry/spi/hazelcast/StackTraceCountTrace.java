package ies.retry.spi.hazelcast;

import ies.retry.Retry;
import ies.retry.RetryHolder;
import ies.retry.RetryManager;
import ies.retry.xml.XMLRetryConfigMgr;

import org.junit.Ignore;
import org.junit.Test;
import junit.framework.Assert;

public class StackTraceCountTrace {
	
	
	@Ignore
	@Test
	public void countStackTrace() throws Exception{
		Exception exception = null;
		RetryManager retryManager = null;
		try{
			XMLRetryConfigMgr.setXML_FILE("retry_config_FAKE.xml");
			retryManager = Retry.getRetryManager();
			
		}catch (Exception e) {
			exception = e;	
		}
		
		Assert.assertEquals(exception.getStackTrace().length, 25);
		
		XMLRetryConfigMgr.setXML_FILE("retry_config.xml");
		retryManager = Retry.getRetryManager();
		TestCallback nodelayCallback = new TestCallback(false,-1,false);
		retryManager.registerCallback(nodelayCallback, "POKE");
		
		
		RetryHolder retry = new RetryHolder("1", "POKE", exception);
		retryManager.addRetry(retry);
		
		Assert.assertEquals(exception.getStackTrace().length, 3);
		
		//HzIntegrationTestUtil.afterClass();
		
	}

}
