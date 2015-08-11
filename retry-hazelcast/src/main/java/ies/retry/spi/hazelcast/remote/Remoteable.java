package ies.retry.spi.hazelcast.remote;

import java.util.concurrent.ExecutionException;

import com.hazelcast.core.HazelcastInstance;

public abstract class Remoteable {

	HazelcastInstance hzClient = null;
	
	public Remoteable(HazelcastInstance hzClientInstane) {
		this.hzClient = hzClientInstane;
	}
	protected <T> T submitRPC(String method,Object...signature) {
		try {
			return (T)hzClient.getExecutorService().submit(rpcClass(method,signature)).get();
		}catch (ExecutionException | InterruptedException e) {
			//TODO - wrap in a suitable runtime exception
			throw new RuntimeException(e.getMessage(),e);
		}finally {
		
		}
		
	}
	protected abstract <T>RemoteRPC<T> rpcClass(String method,Object...signature);
}
