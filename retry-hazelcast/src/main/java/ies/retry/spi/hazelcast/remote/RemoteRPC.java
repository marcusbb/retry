package ies.retry.spi.hazelcast.remote;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.concurrent.Callable;

public abstract class RemoteRPC<T> implements Callable<T>,Serializable {

	
	private static final long serialVersionUID = -4144188157501414137L;
	private String method;
	private Object []signature;
	private String idHandle;
	
	public RemoteRPC(String method,Object...signature) {
		this.method = method;
		this.signature = signature;
	}
	abstract Object target();
	

	@Override
	public T call() throws Exception {
		
		//determine signature types
		Class<?> []parameterTypes = new Class<?>[signature.length];
		int i = 0;
		for (Object o:signature) 
			parameterTypes[i++] = o.getClass();
		
		Method m = target().getClass().getMethod(method, parameterTypes);
		
		return (T)m.invoke(target(), signature);
	}
	public String getIdHandle() {
		return idHandle;
	}
	public void setIdHandle(String idHandle) {
		this.idHandle = idHandle;
	}
	
	
}
