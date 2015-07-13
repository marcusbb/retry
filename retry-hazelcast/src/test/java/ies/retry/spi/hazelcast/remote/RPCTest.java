package ies.retry.spi.hazelcast.remote;

import static org.junit.Assert.*;
import ies.retry.Retry;
import ies.retry.RetryHolder;
import ies.retry.spi.hazelcast.HazelcastRetryImpl;
import ies.retry.spi.hazelcast.HzIntegrationTestUtil;
import ies.retry.spi.hazelcast.remote.RemoteRPCTask;

import java.util.List;
import java.util.concurrent.ExecutionException;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

import com.hazelcast.client.ClientConfig;
import com.hazelcast.client.HazelcastClient;

public class RPCTest {

	HazelcastRetryImpl retryManager = null;
	
	@Before
	public void before() {
		retryManager = (HazelcastRetryImpl)Retry.getRetryManager();
		//retryManager.getConfigManager().cloneConfiguration("POKE");
	}
	@AfterClass
	public static void afterClass() {
		HzIntegrationTestUtil.afterClass();
	}
	@Test
	public void testRPCAdd() throws ExecutionException, InterruptedException {
		//With a client
		HazelcastClient client = HazelcastClient.newHazelcastClient(new ClientConfig().addAddress("localhost:6701"));
		
		client.getExecutorService().submit(new RemoteRPCTask<Void>("addRetry", new RetryHolder("id1","POKE") ));
		//sync add, so we can get it back
		List<RetryHolder> result = client.getExecutorService().submit(new RemoteRPCTask<List<RetryHolder>>("getRetry", "id1","POKE" )).get();
		
		assertEquals(1,result.size());
		
		
	}

}
