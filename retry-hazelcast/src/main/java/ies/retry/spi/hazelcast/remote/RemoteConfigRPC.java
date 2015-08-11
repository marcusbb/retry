package ies.retry.spi.hazelcast.remote;

import ies.retry.Retry;

public class RemoteConfigRPC<T> extends RemoteRPC<T> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5304143937592531395L;

	public RemoteConfigRPC(String method, Object... signature) {
		super(method, signature);
		
	}

	@Override
	Object target() {
		return Retry.getRetryManager().getConfigManager();
	}
	
}
