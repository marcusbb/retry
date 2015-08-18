package ies.retry.spi.hazelcast.remote;

import static org.junit.Assert.*;
import ies.retry.Retry;
import ies.retry.RetryHolder;
import ies.retry.spi.hazelcast.HazelcastRetryImpl;
import ies.retry.spi.hazelcast.HzIntegrationTestUtil;
import ies.retry.spi.hazelcast.HzSerializableRetryHolder;
import ies.retry.spi.hazelcast.util.KryoSerializer;

import java.util.ArrayList;
import java.util.HashMap;
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
	public void dataSerializableTest() throws ExecutionException,InterruptedException {
		RetryHolder holder = new RetryHolder("id-1", "POKE", new HashMap<>());
		ArrayList<RetryHolder> list = new ArrayList<>();list.add(holder);
		HzSerializableRetryHolder serializable = new HzSerializableRetryHolder(list,new KryoSerializer());
		RemoteDataSerializableRPC<Void> rpc = new RemoteDataSerializableRPC<>("addRetry", serializable);
		rpc.setIdHandle(retryManager.getId());
		HazelcastClient client = HazelcastClient.newHazelcastClient(new ClientConfig().addAddress("localhost:6701"));
		client.getExecutorService().submit(rpc).get();
		
		
		client.shutdown();
	}
	@Test
	public void testRPCAddAndGet() throws ExecutionException, InterruptedException {
		//With a client
		HazelcastClient client = HazelcastClient.newHazelcastClient(new ClientConfig().addAddress("localhost:6701"));
		RetryRemoteManagerImpl remoteImpl = new RetryRemoteManagerImpl(client);
		//client.getExecutorService().submit(new RemoteManagerRPC<Void>("addRetry", new RetryHolder("id1","POKE") ));
		remoteImpl.addRetry(new RetryHolder("id1","POKE",new HashMap<>()));
		//sync add, so we can get it back
		List<RetryHolder> result = null;
		int i = 0;
		while (result ==null && i++ < 5) {
			Thread.sleep(1000);
			result = remoteImpl.getRetry("id1", "POKE");	
		}		
		assertEquals(1,result.size());
		assertTrue(result.get(0).getRetryData() instanceof HashMap);
		
		client.shutdown();
		
	}
	

}
