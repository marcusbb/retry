package ies.retry.spi.hazelcast;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({
	/*CallbackManagerTest.class,
	CallbackTest.class,
		ConcurrentOpsIntegrationTest.class,*/
		/*DataLossUponLoadingFromDBTest.class, 
		HazelcastRetryImplTest.class,*/
		//LocalQueueLogTests.class, 
		LocalQueuerTest.class,
		ManagementIntegrationTest.class, 
		/*NetworkDBMergeTest.class,
		QueryTest.class,
		StackTraceCountTest.class, 
		StackTraceCountTrace.class,
		StateManagerTest.class, 
		StoreOpMergeTest.class */
		}
)
public class AllTests {

}
