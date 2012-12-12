package hazelcast;

import ies.retry.Retry;
import ies.retry.RetryHolder;
import ies.retry.RetryManager;
import ies.retry.spi.hazelcast.HazelcastRetryImpl;
import ies.retry.spi.hazelcast.jmx.RetryManagement;
import ies.retry.xml.XMLRetryConfigMgr;

import java.io.IOException;
import java.lang.management.ManagementFactory;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import test.util.PersistenceUtil;

public class RetyMultiFailWithPersistence {

	public static void main(String[] args) throws Exception {
		//PersistenceUtil.createEntityManager("retryPool");
		
		XMLRetryConfigMgr.setXML_FILE("retry_config_persistence.xml");
		
		RetryManager retryManager = Retry.getRetryManager();

		RetryManagement management = new RetryManagement();
		management.init((HazelcastRetryImpl) retryManager);

		MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
		mbs.registerMBean(management, new ObjectName(
				"retry:retry=hazelcast-retry"));

		// You probably want to do this as a one time, or limited
		// basis
		retryManager.registerCallback(new FailCallback(), "POKE");
		// You probably want to do this as a one time, or limited
		// basis
		retryManager.registerCallback(new FailCallback(), "POKE_BY_PIN");
		
		
		for (int i = 0; i < 1; i++) {
			RetryHolder holder = new RetryHolder("12334" + i, "POKE",
					new IOException("Houston there is a problem"),
					"Useful Serializable object ");
			retryManager.addRetry(holder);
		}
		
		/*for (int i = 0; i < 200; i++) {
			RetryHolder holder = new RetryHolder("12334" + i, "POKE_BY_PIN",
					new IOException("Houston there is a problem"),
					"Useful Serializable object ");
			retryManager.addRetry(holder);
		}*/
	}

}
