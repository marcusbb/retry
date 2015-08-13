package ies.retry.spi.hazelcast;

import com.hazelcast.client.ClientConfig;
import com.hazelcast.client.HazelcastClient;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.LifecycleEvent;
import com.hazelcast.core.LifecycleEvent.LifecycleState;
import com.hazelcast.core.LifecycleListener;
import com.hazelcast.logging.ILogger;

import ies.retry.Retry;
import ies.retry.RetryCallback;
import ies.retry.RetryHolder;
import ies.retry.spi.hazelcast.remote.RemoteManagerRPC;
import ies.retry.spi.hazelcast.remote.RemoteRPC;
import ies.retry.spi.hazelcast.remote.Remoteable;
import provision.services.logging.Logger;

/**
 * Handles connections back to calling hz cluster.
 * 
 *
 */
public class CallbackRemoteProxy extends Remoteable implements RetryCallback,LifecycleListener {

	private ClientConfig remoteClusterConfig;
	
		
	public CallbackRemoteProxy(ClientConfig cc) {
		
		this.remoteClusterConfig = cc;
		
		HazelcastClient client = HazelcastClient.newHazelcastClient(remoteClusterConfig);
		this.hzClient = client;
		client.getLifecycleService().addLifecycleListener(this);
		
	}
	
	@Override
	public boolean onEvent(RetryHolder retry) throws Exception {
		return submitRPC("onCallback", retry);
		
	}
	@Override
	public void stateChanged(LifecycleEvent event) {
		if (event.getState() == LifecycleState.CLIENT_CONNECTION_LOST) {
			Logger.warn(CallbackRemoteProxy.class.getName(), "remote_cluster_connection_lost","",event.getState());
			//TODO: eventually Hz will give up if gone for too long.
			//perhaps we need to fill the void here
		}
		
	}

	@Override
	protected <T> RemoteRPC<T> rpcClass(String method, Object... signature) {
		return new RemoteManagerRPC<>(method, signature);
	}

}
