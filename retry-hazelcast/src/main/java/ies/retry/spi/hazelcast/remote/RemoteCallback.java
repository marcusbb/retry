package ies.retry.spi.hazelcast.remote;

import com.hazelcast.client.ClientConfig;

import ies.retry.RetryCallback;

public interface RemoteCallback extends RetryCallback {

	/**
	 * Get client information that would correspond to the callback's 
	 * HZ cluster
	 * 
	 * @return
	 */
	ClientConfig getClientConfig();
}
