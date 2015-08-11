package ies.retry.spi.hazelcast.remote;

import ies.retry.Retry;

import java.io.Serializable;

public class  RemoteManagerRPC<T> extends RemoteRPC<T> implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -591081922138736719L;

	public RemoteManagerRPC(String method, Object... signature) {
		super(method, signature);
	}

	@Override
	Object target() {
		return Retry.getRetryManager();
	}
	
	
}
