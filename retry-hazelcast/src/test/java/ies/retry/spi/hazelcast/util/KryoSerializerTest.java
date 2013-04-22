package ies.retry.spi.hazelcast.util;

import ies.retry.Retry;
import ies.retry.RetryHolder;
import ies.retry.RetryManager;
import ies.retry.RetryMarshaller;
import ies.retry.xml.XMLRetryConfigMgr;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

import org.junit.Test;

public class KryoSerializerTest {
	
	@Test
	public void testSerial() throws Exception{
		
		/**
		 * let's create fake exception
		 * 
		 */
		Exception exception = null;
		try{
			XMLRetryConfigMgr.setXML_FILE("retry_config_FAKE.xml");
			Retry.getRetryManager();
			
		}catch (Exception e) {
			exception = e;	
		}
		
		System.out.println("stack trace count of fake exception being serialized :" + exception.getStackTrace().length);
		
//		exception.setStackTrace(Arrays.copyOf(exception.getStackTrace(), 3));
//		
//		System.out.println("stack trace count " + exception.getStackTrace().length);
	
		List<RetryHolder> listOfRetryData = new ArrayList<RetryHolder>();
		
		RetryHolder holder = null;
		
		for(int i =0; i<10000;i++){
			 holder = new RetryHolder(Integer.toString(i), "POKE", exception, "Houston we have a problem with " + i);
			 listOfRetryData.add(holder);
			 
		}
		
		/**
		 * java
		 */
		
		long startTime = System.currentTimeMillis();
		
		holder = new RetryHolder("MAIN PROBLEM", "POKE", exception, IOUtil.serialize(listOfRetryData));
	
		byte [] resultSerialized = IOUtil.serialize(holder);
		
		long endTime =  System.currentTimeMillis() - startTime;
		
		System.out.println("Java byte amount of data serialized :" + resultSerialized.length);
		
		System.out.println("Java time to serialize " + endTime);
		
		
		holder = (RetryHolder) IOUtil.deserialize(resultSerialized);
		
		List<RetryHolder> listOfRetryData2 = (List<RetryHolder>) IOUtil.deserialize((byte[]) holder.getRetryData());
		
		Assert.assertEquals(listOfRetryData2.size(), 10000);
		
		System.out.println("Java deserialized size : " + listOfRetryData2.size());
		
		String testMessage = (String) listOfRetryData2.get(99).getRetryData();
		
		Assert.assertEquals("Houston we have a problem with 99", testMessage);
		
		System.out.println("Java deserialized element : " + testMessage);
		
		endTime =  System.currentTimeMillis() - startTime;
		
		System.out.println("Java time to serialize & deserialize : " + endTime);
		
		
		/**
		 * kryo
		 */
		
		
		RetryMarshaller marshaller = new KryoSerializer();
		
		startTime = System.currentTimeMillis();
		
		holder = new RetryHolder("MAIN PROBLEM", "POKE", exception, marshaller.marshallToByte((Serializable) listOfRetryData));
		
		resultSerialized = marshaller.marshallToByte(holder);
		
		endTime =  System.currentTimeMillis() - startTime;
		
		System.out.println("Kryo byte amount of data serialized :" + resultSerialized.length);
		
		System.out.println("Kryo time to serialize " + endTime);
		
		holder = (RetryHolder)marshaller.marshallToObject(resultSerialized) ;
		
	    listOfRetryData2 = (List<RetryHolder>) marshaller.marshallToObject((byte[]) holder.getRetryData());
	    
	    Assert.assertEquals(listOfRetryData2.size(), 10000);
		
		System.out.println("Kryo deserialized size : " + listOfRetryData2.size());
		
		testMessage = (String) listOfRetryData2.get(99).getRetryData();
		
		Assert.assertEquals("Houston we have a problem with 99", testMessage);
		
		System.out.println("Kryo deserialized element : " + testMessage);
		
		endTime =  System.currentTimeMillis() - startTime;
		
		System.out.println("Kryo time to serialize & deserialize : " + endTime);
		
	}

}
