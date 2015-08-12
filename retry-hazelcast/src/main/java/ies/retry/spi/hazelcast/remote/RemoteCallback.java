package ies.retry.spi.hazelcast.remote;

import java.io.Serializable;
import java.util.List;

import com.hazelcast.client.ClientConfig;
import com.hazelcast.client.HazelcastClient;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.LifecycleEvent;
import com.hazelcast.core.LifecycleListener;
import com.hazelcast.core.Member;

import ies.retry.ConfigException;
import ies.retry.RetryCallback;
import ies.retry.RetryHolder;

public interface RemoteCallback extends Serializable {

	/**
	 * Get client information that would correspond to the callback's 
	 * HZ cluster
	 * 
	 * @return
	 */
	RemoteXmlConfig getClientConfig();
	
	String getType();
	
	public static class DefaultRemoteCallback implements RemoteCallback,Serializable {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1429842809278154191L;
		private transient RemoteConfigManager configMgr = null;
		private transient HazelcastClient client;
		
		
		private RemoteXmlConfig clientConfig;
		private String type;
		
		public DefaultRemoteCallback(RemoteConfigManager configMgr,String type) {
			this.configMgr = configMgr;
			if (configMgr.getClientConfig().getClusterName() == null) {
				throw new ConfigException("No client cluster name defined");
			}
			HazelcastInstance thisInst = Hazelcast.getHazelcastInstanceByName(configMgr.getClientConfig().getClusterName());
			if (thisInst == null) 
				throw new ConfigException("No client cluster name defined: " + configMgr.getClientConfig().getClusterName());
			//build client config from this cluster config
			this.clientConfig = new RemoteXmlConfig();
			clientConfig.setGroupConfig(thisInst.getConfig().getGroupConfig());
			clientConfig.setPort(thisInst.getConfig().getNetworkConfig().getPort());
			
			for (Member member:thisInst.getCluster().getMembers()) {
				clientConfig.add(member.getInetSocketAddress().getHostName());
			}
			this.type = type;
			
		}
		//@Override
//		public boolean onEvent(RetryHolder retry) throws Exception {
//			
//			//TODO: really need a better mechanism to identify client/server
//			client.getExecutorService().submit(new RemoteManagerRPC("onCallback", retry )).get(); //TODO get with timeout
//			
//			return false;
//		}
		
		@Override
		public String getType() {
			return type;
		}
		
		
		public RemoteXmlConfig getClientConfig() {
			return clientConfig;
		}
		//@Override
		public void stateChanged(LifecycleEvent event) {
			//check the state of the client connection to the server
			
		}
		
	}
}
