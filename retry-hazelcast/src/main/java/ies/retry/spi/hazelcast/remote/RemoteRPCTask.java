package ies.retry.spi.hazelcast.remote;

import ies.retry.Retry;
import ies.retry.spi.hazelcast.HazelcastRetryImpl;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.concurrent.Callable;

/**
 * 
 * Proof of concept
 *
 * @param <T>
 */
public class RemoteRPCTask<T> implements Callable<T>,Serializable {

	
	private static final long serialVersionUID = -4144188157501414137L;
	private String method;
	private Object []signature;
	
	public RemoteRPCTask(String method,Object...signature) {
		this.method = method;
		this.signature = signature;
	}
	@Override
	public T call() throws Exception {
		HazelcastRetryImpl retryManager = (HazelcastRetryImpl)Retry.getRetryManager();
		//determine signature types
		Class<?> []parameterTypes = new Class<?>[signature.length];
		int i = 0;
		for (Object o:signature) 
			parameterTypes[i++] = o.getClass();
		
		Method m = HazelcastRetryImpl.class.getMethod(method, parameterTypes);
		
		return (T)m.invoke(retryManager, signature);
	}

	
}
