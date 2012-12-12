package timer;

import static org.junit.Assert.*;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

public class STPETests {

	@Test
	public void test() throws Exception {
		ScheduledThreadPoolExecutor stpe = new ScheduledThreadPoolExecutor(1);
		
		stpe.scheduleWithFixedDelay(new Task(), 
				1000, 
				1000, TimeUnit.MILLISECONDS);
		
		Thread t = new Thread();
		
		Thread.sleep(1000);
	}

}
class CancelTask {
	CancelTask(ScheduledThreadPoolExecutor stpe) {
		
	}
}
class Task implements Runnable {

	AtomicInteger count = new AtomicInteger();
	
	public void run() {
		count.getAndIncrement();
		System.out.println("Running " + count.intValue());
		
	}
	
}
