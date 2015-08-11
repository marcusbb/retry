package ies.retry.spi.hazelcast.remote;

import ies.retry.ConfigException;
import ies.retry.RetryConfigManager;
import ies.retry.RetryConfiguration;
import ies.retry.xml.XMLRetryConfigMgr;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

import javax.xml.bind.JAXBException;

import com.hazelcast.core.HazelcastInstance;

/**
 * 
 * Wrapping class for remote access to config manager
 *
 */
public class RemoteConfigManager extends XMLRetryConfigMgr  {

	HazelcastInstance hzClient = null;
	RetryConfigManager configMgrWrapped;
	Remote remoteHandle = null;
	
	public class Remote extends Remoteable {
		
		
		
		public Remote(HazelcastInstance hzClientInstane) {
			super(hzClientInstane);
					
		}

		@Override
		protected <T> RemoteRPC<T> rpcClass(String method, Object... signature) {
			return new RemoteConfigRPC<>(method, signature);
		}
		
	}
	
	
	protected RemoteConfigManager() {
		setFactory(new RemoteXmlConfig.XmlConfFactory());
		setJaxbConfigClass(RemoteXmlConfig.class);
		
		try {
			load();
		} catch (IOException | JAXBException e) {
			throw new ConfigException(e);
		}
	}
	
	protected void setHzInstance(HazelcastInstance hzClientInstance) {
		this.remoteHandle = new Remote(hzClientInstance);
	}
	
	public RemoteConfigManager(HazelcastInstance hzClientInst) {
		this.remoteHandle = new Remote(hzClientInst);
	}
	@Override
	public RetryConfiguration getConfiguration(String type) {
		return remoteHandle.submitRPC("getConfiguration", type);
	}

	@Override
	public void addConfiguration(RetryConfiguration config) {
		remoteHandle.submitRPC("addConfiguration", config);
		
	}

	@Override
	public Collection<RetryConfiguration> getAll() {
		return remoteHandle.submitRPC("getAll");
	}

	public Map<String, RetryConfiguration> getConfigMapLocal() {
		return super.getConfigMap();
	}
	
	@Override
	public Map<String, RetryConfiguration> getConfigMap() {
		return remoteHandle.submitRPC("getConfigMap");
	}

	@Override
	public RetryConfiguration cloneConfiguration(String type) {
		return remoteHandle.submitRPC("cloneConfiguration",type);
	}

	
}
