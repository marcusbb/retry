package ies.retry.spi.hazelcast;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import ies.retry.Retry;
import ies.retry.RetryHolder;
import ies.retry.spi.hazelcast.config.HazelcastXmlConfig;
import ies.retry.spi.hazelcast.persistence.RetryMapStoreFactory;
import ies.retry.xml.XMLRetryConfigMgr;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import org.junit.Assert;

public class StackTraceCountTest {
	
	static HazelcastRetryImpl retryManager;
	private static EntityManagerFactory emf;
	
	@BeforeClass
	public static void beforeClass() throws Exception{
		HzIntegrationTestUtil.beforeClass();
		
		emf = Persistence.createEntityManagerFactory("retryPool");
		
		XMLRetryConfigMgr.setXML_FILE("retry_config.xml");
		retryManager = new HazelcastRetryImpl();
		HazelcastXmlConfig config = new HazelcastXmlConfig();
		config.getPersistenceConfig().setON(true);
		RetryMapStoreFactory.getInstance().setEMF(emf);
		RetryMapStoreFactory.getInstance().init(config);
		
		TestCallback pokeCallback = new TestCallback(false,-1,false);
		retryManager.registerCallback(pokeCallback, "POKE");
		
		TestCallback archiveOnCallback = new TestCallback(false,-1,false);
		retryManager.registerCallback(archiveOnCallback, "ARCHIVE_ON");
		
		TestCallback archiveOffCallback = new TestCallback(false,-1,false);
		retryManager.registerCallback(archiveOffCallback, "ARCHIVE_OFF");

	}
	@AfterClass
	public static void afterClass() {
		retryManager.shutdown();
		HzIntegrationTestUtil.afterClass();
	}
	
	@Test
	public void countStackTrace() throws Exception{


		try{
			throw new Exception();
			
		}catch (Exception e) {
				
		
	//  number of stack trace lines appears unpredictable in different execution environment and isn't really significant as 
	//  the only important thing is the correspondence of stack trace lines number to the value configured in
	//	<stackTraceLinesEnd>3</stackTraceLinesEnd>  parameter of retry_config.xml	
	//	Assert.assertEquals(31, exception.getStackTrace().length);

		RetryHolder retry1 = new RetryHolder("1", "POKE", e);
		retryManager.addRetry(retry1);
		
		Assert.assertEquals(3, retry1.getException().getStackTrace().length);
		Assert.assertNotEquals(e, retry1.getException());
//		System.out.println("#######Start######");
//		e.printStackTrace(System.out);
//		System.out.println("#######End######");
		

		
		
		}
	}
	
	@Test
	public void startAndCountStackTrace() throws Exception{

		try{
			throw new Exception();
			
		}catch (Exception e) {
			RetryHolder retry2 = new RetryHolder("1", "ARCHIVE_ON", e);
			retryManager.addRetry(retry2);
			
			Assert.assertEquals(3, retry2.getException().getStackTrace().length);
			Assert.assertNotEquals(e, retry2.getException());
//			System.out.println("#######Start######");
//			e.printStackTrace(System.out);
//			System.out.println("#######End######");
			
	
		}
		
	}
	@Test
	public void startAndCountStackTraceAndCause() throws Exception{
		try{
			throw new Exception();
			
		}catch (Exception e) {
			RetryHolder retry3 = new RetryHolder("1", "ARCHIVE_OFF", e);
						
			retryManager.addRetry(retry3);

			Assert.assertEquals(0, retry3.getException().getStackTrace().length);	
			Assert.assertNotEquals(e, retry3.getException());
//			System.out.println("#######Start######");
//			e.printStackTrace(System.out);
//			System.out.println("#######End######");
		}
		
	}

}
