package ies.retry.spi.hazelcast.config;

public interface ConfigListener {

	
	public void onConfigChange(HazelcastXmlConfig config);
	
	
	
}
