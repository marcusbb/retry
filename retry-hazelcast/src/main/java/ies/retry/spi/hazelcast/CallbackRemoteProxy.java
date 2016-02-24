package ies.retry.spi.hazelcast;

import org.slf4j.Logger;

import com.hazelcast.client.ClientConfig;
import com.hazelcast.client.HazelcastClient;
import com.hazelcast.core.LifecycleEvent;
import com.hazelcast.core.LifecycleEvent.LifecycleState;
import com.hazelcast.core.LifecycleListener;

import ies.retry.RetryCallback;
import ies.retry.RetryHolder;
import ies.retry.spi.hazelcast.remote.RemoteManagerRPC;
import ies.retry.spi.hazelcast.remote.RemoteRPC;
import ies.retry.spi.hazelcast.remote.RemoteXmlConfig;
import ies.retry.spi.hazelcast.remote.Remoteable;

/**
 * Handles connections back to calling hz cluster.
 * 
 *
 */
public class CallbackRemoteProxy extends Remoteable implements RetryCallback,LifecycleListener {

	private RemoteXmlConfig remoteClusterConfig;
	private static Logger logger = org.slf4j.LoggerFactory.getLogger(CallbackRemoteProxy.class);
		
	public CallbackRemoteProxy(RemoteXmlConfig config) {
		
		this.remoteClusterConfig = config;
		
		HazelcastClient client = HazelcastClient.newHazelcastClient(remoteClusterConfig.getHzClientConfig());
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
			logger.warn("remote_cluster_connection_lost: state={}",event.getState());
			//TODO: eventually Hz will give up if gone for too long.
			//perhaps we need to fill the void here
		}
		
	}

	@Override
	protected <T> RemoteRPC<T> rpcClass(String method, Object... signature) {
		RemoteManagerRPC<T> inst =  new RemoteManagerRPC<>(method, signature);
		inst.setIdHandle(remoteClusterConfig.getIdHandle());
		return inst;
	}

}
