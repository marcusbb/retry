package ies.retry.spi.hazelcast.disttasks;

import ies.retry.RetryHolder;
import ies.retry.spi.hazelcast.HazelcastRetryImpl;

import java.io.Serializable;
import java.util.List;
import java.util.concurrent.Callable;

import com.hazelcast.core.IMap;

/**
 * Gets {@link IMap#localKeySet()} size for a particular type.
 * 
 * @author msimonsen
 *
 */
public class KeySetSizeTask implements Callable<Integer>,Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -706773686125305120L;
	private String type;
	
	public KeySetSizeTask(String type) {
		this.type = type;
	}
	@Override
	public Integer call() throws Exception {
		Integer ret = 0;
		IMap<String,List<RetryHolder>> map = HazelcastRetryImpl.getHzInst().getMap(type);
		
		if (map != null)
			ret = map.localKeySet().size();
		
		return ret;
		
	}

}
