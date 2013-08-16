package ies.retry.spi.hazelcast;

import static org.junit.Assert.*;

import java.io.IOException;

import ies.retry.RetryHolder;
import ies.retry.spi.hazelcast.util.IOUtil;

import org.junit.Before;
import org.junit.Test;

public class LocalQueueLogTests {

	static String dir = ".";
	
	
	@Before
	public void before() throws Exception {
		
	}
	
	@Test
	public void queueAndDequeue() throws Exception {
		LocalQueueLog log = new LocalQueueLog(dir);
		
		RetryHolder holder = new RetryHolder("id", "type",new Exception(),"Object");
				
		log.queue(holder);
		
		log.dequeue();
		
		log.close();
		
	}
	
	@Test
	public void queueMarkerCounts() throws IOException {
		
		LocalQueueLog log = new LocalQueueLog(dir);
		RetryHolder holder1 = new RetryHolder("id", "type",new Exception(),"Object");
		RetryHolder holder2 = new RetryHolder("id2", "type",new Exception(),"Object");
		byte[]b1 = IOUtil.serialize(holder1);
		byte[]b2 = IOUtil.serialize(holder2);
		
		System.out.println("bytes total: " + (b1.length +b2.length));
		log.queue(holder1);
		log.queue(holder2);
		
		log.dequeue();
		log.dequeue();
		
		
		
		
		log.close();
	}

}
