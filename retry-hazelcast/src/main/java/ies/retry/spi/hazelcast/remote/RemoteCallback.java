package ies.retry.spi.hazelcast.remote;

import java.io.Serializable;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.Member;

import ies.retry.ConfigException;

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
		
		public DefaultRemoteCallback(RemoteConfigManager configMgr,String type,String idhandle) {
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
				clientConfig.add(member.getInetSocketAddress().getHostName() + ":" + thisInst.getConfig().getNetworkConfig().getPort());
			}
			this.type = type;
			this.clientConfig.setIdHandle(idhandle);
			
		}

		
		@Override
		public String getType() {
			return type;
		}
		
		
		public RemoteXmlConfig getClientConfig() {
			return clientConfig;
		}
		
		
	}
}
