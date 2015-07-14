package hazelcast;

import ies.retry.Retry;
import ies.retry.RetryConfigManager;
import ies.retry.RetryConfiguration;
import ies.retry.RetryManager;
import ies.retry.spi.hazelcast.HazelcastRetryImpl;
import ies.retry.spi.hazelcast.jmx.RetryManagement;
import ies.retry.spi.hazelcast.util.HzUtil;
import ies.retry.xml.XMLRetryConfigMgr;

import java.lang.management.ManagementFactory;

import javax.management.MBeanServer;
import javax.management.ObjectName;

public class RetryJMXMain {

	public static long SLEEP = 100;
	public static long BLOCK = 100;
	
	public static void main(String[] args) throws Exception {
		
		//XMLRetryConfigMgr.setXML_FILE("retry_config_persistence.xml");
		if (args.length > 0 ) {
			if (args[0] != null)
				HzUtil.HZ_CONFIG_FILE = args[0];
			if (args[1] != null)
				XMLRetryConfigMgr.DEFAULT_XML_FILE = args[1];
		}
		XMLRetryConfigMgr.setCONFIG_DIR(".");
		
		RetryManager retryManager = Retry.getRetryManager();
		
		
		RetryManagement management = new RetryManagement();
		management.init((HazelcastRetryImpl) retryManager);

		MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
		mbs.registerMBean(management, new ObjectName(
				"retry:retry=hazelcast-retry"));
		TestCallback nodelayCallback = new TestCallback(false,-1,false);
		mbs.registerMBean(new TestRetryOps("POKE",nodelayCallback), new ObjectName("retry:test=StaticRetryAdd"));
		// You probably want to do this as a one time, or limited
		// basis
		
		retryManager.registerCallback(nodelayCallback, "POKE");
		
		//clone and add callback
		RetryConfigManager configManager = retryManager.getConfigManager();
		RetryConfiguration cloned = configManager.cloneConfiguration("POKE");
		cloned.setType("POKE_CLONED");
		retryManager.getConfigManager().addConfiguration(cloned);
		TestCallback delayCallback = new TestCallback(false,500,false);
		retryManager.registerCallback(delayCallback, "POKE_CLONED");
		
		mbs.registerMBean(new TestRetryOps( "POKE_CLONED",delayCallback),new ObjectName("retry:test=ClonedRetryAdd"));
		// You probably want to do this as a one time, or limited
		// basis
		System.out.println("PID: " + ManagementFactory.getRuntimeMXBean().getName());
		
		long colStatSleep = 5000;
		
		
		
	}

}
