package hazelcast;

import ies.retry.Retry;
import ies.retry.RetryHolder;
import ies.retry.spi.hazelcast.HazelcastRetryImpl;

import java.io.Serializable;
import java.util.List;
import java.util.concurrent.Callable;

import com.hazelcast.core.IMap;

public class GetCallBack implements Callable<List<RetryHolder>>,Serializable {

	String id;
	String type;
	public GetCallBack(String id, String type) {
		this.id = id;
		this.type = type;
	}
	@Override
	public List<RetryHolder> call() throws Exception {
		HazelcastRetryImpl rm = (HazelcastRetryImpl)Retry.getRetryManager();
	
		IMap<String, List<RetryHolder>> map = rm.getHzInst().getMap(type);
		List<RetryHolder> list = map.get(id);
		
		return list;
	}

	

}
