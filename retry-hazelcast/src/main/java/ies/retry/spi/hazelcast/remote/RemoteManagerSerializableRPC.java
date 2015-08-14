package ies.retry.spi.hazelcast.remote;


import ies.retry.Retry;
import ies.retry.spi.hazelcast.HzSerializableRetryHolder;

public class RemoteManagerSerializableRPC<T> extends RemoteDataSerializableRPC<T>{

	

	/**
	 * 
	 */
	private static final long serialVersionUID = -1110462181359517500L;

	public RemoteManagerSerializableRPC(String method, HzSerializableRetryHolder serializable) {
		super(method, serializable);
		
	}

	@Override
	Object target() {
		if (getIdHandle() != null)
			return Retry.getByInst(getIdHandle());
		return Retry.getRetryManager();
	}

}
