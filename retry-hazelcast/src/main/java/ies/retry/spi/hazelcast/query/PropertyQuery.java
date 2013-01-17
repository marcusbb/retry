package ies.retry.spi.hazelcast.query;

import ies.retry.Retry;
import ies.retry.RetryHolder;
import ies.retry.spi.hazelcast.HazelcastRetryImpl;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;

public class PropertyQuery implements BaseQuery {

	/**
	 * 
	 */
	private static final long serialVersionUID = 8889785224143284744L;

	private String mapName;
	private String property;
	private Method method = null;
	private Object match;
	
	public PropertyQuery(String mapName,String property,Object match) {
		this.mapName = mapName;
		this.property = property;
		this.match = match;
		
		
	}
	@Override
	public QueryResults eval() {
		QueryResults results = new QueryResults();
		List<RetryHolder> list = new ArrayList<RetryHolder>();
		HazelcastInstance h1 = ((HazelcastRetryImpl)Retry.getRetryManager()).getH1();
		results.setMember(h1.getCluster().getLocalMember());
		IMap<String,List<RetryHolder>> map = h1.getMap(mapName);
		Iterator<String> iter = map.localKeySet().iterator();
		
		//the meat
		while (iter.hasNext()) {
			for (RetryHolder retry:map.get(iter.next())) {
				Serializable data = retry.getRetryData();
				if (isMatch(data))
					list.add(retry);
			}
			
			
		}
		results.setResults(list);
		return results;
	}
	protected boolean isMatch(Serializable data) {
		boolean matchBool = false;
		try {
			if (method == null) {
				String getMeth = "get" + property.substring(0,1).toUpperCase() + property.substring(1);
				method = data.getClass().getMethod(getMeth);
			}
			if (method != null)
				matchBool = match.equals(method.invoke(data));
		}catch (Exception e) {
			//TODO
			e.printStackTrace();
		}
		
		
		return matchBool;
	}
	@Override
	public String getMapName() {
		return this.mapName;
	}
	@Override
	public Integer maxResultPerNode() {
		// TODO Auto-generated method stub
		return null;
	}

	
}
