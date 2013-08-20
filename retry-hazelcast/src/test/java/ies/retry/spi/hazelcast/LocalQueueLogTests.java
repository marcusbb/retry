package ies.retry.spi.hazelcast;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Random;


import ies.retry.RetryHolder;
import ies.retry.spi.hazelcast.util.IOUtil;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class LocalQueueLogTests {

	static String dir = ".";
	
	
	@Before
	public void before() throws Exception {
		
	}
	
	
	public void deleteFiles() throws IOException {
		File commitLog = new File(dir, LocalQueueLog.curFile);
		commitLog.delete();
		
		File takeMarkerFile = new File(dir,LocalQueueLog.takeMarker);
		takeMarkerFile.delete();
	}
	
	@Test
	public void queueAndDequeue() throws Exception {
		LocalQueueLog log = new LocalQueueLog(dir);
		
		RetryHolder holder = new RetryHolder("id", "type",new Exception(),"Object");
				
		log.queue(holder);
		
		log.moveTakeMarker();
		
		log.close();
		
	}
	
	@Test
	public void queueMarkerCounts() throws IOException {
		
		deleteFiles();
		
		LocalQueueLog log = new LocalQueueLog(dir);
		RetryHolder holder1 = new RetryHolder("id", "type",new Exception(),"Object");
		RetryHolder holder2 = new RetryHolder("id2", "type",new Exception(),"Object");
		byte[]b1 = IOUtil.serialize(holder1);
		byte[]b2 = IOUtil.serialize(holder2);
		
		System.out.println("bytes total: " + (b1.length +b2.length));
		log.queue(holder1);
		log.queue(holder2);
		
		log.moveTakeMarker();
		log.moveTakeMarker();
		
		Assert.assertEquals(b1.length + b2.length + 8, log.getTakeMarker());
		
		log.close();
	}

	
	@Test
	public void replayTest() throws IOException {
		
		deleteFiles();
		
		LocalQueueLog log = new LocalQueueLog(dir);
		Random rand = new Random();
		for (int i=0;i<2;i++) {
			RetryHolder holder1 = new RetryHolder("id", "type",new Exception(),new byte[rand.nextInt(2000)]);
			
			log.queue(holder1);
		}
		
		log.close();
		
		//Simulate new process that replays
		LocalQueueLog log2 = new LocalQueueLog(dir);
		
		Collection<RetryHolder> collection = log2.replayFromFile();
		
		Assert.assertEquals(2, collection.size());
		
		collection = log2.replayFromFile();
		
		Assert.assertEquals(0, collection.size());
		
		for (int i=0;i<10;i++) {
			RetryHolder holder1 = new RetryHolder("id", "type",new Exception(),new byte[rand.nextInt(2000)]);
			
			log2.queue(holder1);
		}
		collection = log2.replayFromFile();
		Assert.assertEquals(10, collection.size());
		
	}
	
}
