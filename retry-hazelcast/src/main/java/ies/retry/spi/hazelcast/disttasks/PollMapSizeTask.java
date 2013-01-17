package ies.retry.spi.hazelcast.disttasks;

import ies.retry.Retry;
import ies.retry.spi.hazelcast.HazelcastRetryImpl;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * Gets a map of local queue sizes.
 * 
 * @author msimonsen
 *
 */
public class PollMapSizeTask implements Callable<Map<String,Integer>>,Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4209742421163482535L;

	@Override
	public Map<String,Integer> call() throws Exception {
		HazelcastRetryImpl retry = (HazelcastRetryImpl)Retry.getRetryManager();
		HashMap<String, Integer> map = new HashMap<String, Integer>();
		
		for (String type:retry.getConfigManager().getConfigMap().keySet()) {
			
			map.put(type, 
					((HazelcastRetryImpl)Retry.getRetryManager()).getH1()
						.getMap(type).localKeySet().size()
					);
			
		}
		
		return map;
		
	}

}
