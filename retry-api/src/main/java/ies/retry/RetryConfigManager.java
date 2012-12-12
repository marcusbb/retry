package ies.retry;

import java.util.Collection;
import java.util.Map;

/**
 * 
 * This is the loading configuration manager.
 * 
 * 
 * 
 * @author msimonsen
 *
 */
public interface RetryConfigManager {

	public RetryConfiguration getConfiguration(String type);
	
	public void addConfiguration(RetryConfiguration config);
	
	public Collection<RetryConfiguration> getAll();
	//Get configuration by keyed by type
	public Map<String, RetryConfiguration> getConfigMap();
	
	public RetryConfiguration cloneConfiguration(String type);
}
