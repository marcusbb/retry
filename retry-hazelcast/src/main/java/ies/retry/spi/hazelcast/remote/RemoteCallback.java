package ies.retry.spi.hazelcast.remote;

import java.io.Serializable;
import java.util.List;

import com.hazelcast.client.ClientConfig;
import com.hazelcast.core.HazelcastInstance;

import ies.retry.RetryCallback;
import ies.retry.RetryHolder;

public interface RemoteCallback extends RetryCallback,Serializable {

	/**
	 * Get client information that would correspond to the callback's 
	 * HZ cluster
	 * 
	 * @return
	 */
	ClientConfig getClientConfig();
	
	public static class DefaultRemoteCallback implements RemoteCallback {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1429842809278154191L;
		private ClientConfig cc = null;
		private transient HazelcastInstance client;
		private transient RetryCallback callback;
		
		public DefaultRemoteCallback(ClientConfig config,RetryCallback wrappedCallback) {
			this.cc = config;
		}
		@Override
		public boolean onEvent(RetryHolder retry) throws Exception {
			
			client.getExecutorService().submit(new RemoteRPCTask<List<RetryHolder>>("getRetry", "id1","POKE" )).get();
			
			return false;
		}

		@Override
		public ClientConfig getClientConfig() {
			return cc;
		}
		
	}
}
