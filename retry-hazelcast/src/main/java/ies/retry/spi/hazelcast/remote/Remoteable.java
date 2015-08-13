package ies.retry.spi.hazelcast.remote;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.ExecutionException;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.MultiTask;

public abstract class Remoteable {

	HazelcastInstance hzClient = null;
	String executorName = null;
	
	public Remoteable(HazelcastInstance hzClientInstane) {
		this.hzClient = hzClientInstane;
	}
	public Remoteable(HazelcastInstance hzClientInstane,String name) {
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
	protected <T> Collection<T> submitToAll(String method,Object...signature) {
		try {
			MultiTask<?> task = new MultiTask<>(rpcClass(method, signature), hzClient.getCluster().getMembers());
			hzClient.getExecutorService().submit(task);
			ArrayList<T> list = new ArrayList<>(); 
			Iterator<T> iter = (Iterator<T>)task.get().iterator();
			while(iter.hasNext()) {
				list.add(iter.next());
			}
			return list;
			
			
		}catch (ExecutionException | InterruptedException e) {
			e.printStackTrace(System.out);
			//TODO - wrap in a suitable runtime exception
			throw new RuntimeException(e.getMessage(),e);
		}finally {
		
		}
		
	}
	protected abstract <T>RemoteRPC<T> rpcClass(String method,Object...signature);
}