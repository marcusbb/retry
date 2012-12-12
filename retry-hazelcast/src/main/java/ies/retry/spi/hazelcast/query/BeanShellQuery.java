package ies.retry.spi.hazelcast.query;

import ies.retry.RetryHolder;
import ies.retry.spi.hazelcast.HazelcastRetryImpl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import bsh.EvalError;
import bsh.Interpreter;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;

public class BeanShellQuery implements BaseQuery {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -5500180724109304015L;
	private String mapName;
	private String beanShell;
	private int numResults = Integer.MAX_VALUE;
	public static final String DATA_PROP = "data";
	public static final String RETRY_PROP = "retry";
	
	public BeanShellQuery(String mapName, String beanShell) {
		this.mapName = mapName;
		this.beanShell = beanShell;
	}
	public BeanShellQuery(String mapName, String beanShell,int numResults) {
		this.mapName = mapName;
		this.beanShell = beanShell;
		this.numResults = numResults;
	}
	
	@Override
	public QueryResults eval() {
		QueryResults results = new QueryResults();
		Interpreter interpreter = new Interpreter();
		List<RetryHolder> list = new ArrayList<RetryHolder>();
		HazelcastInstance h1 = HazelcastRetryImpl.getHzInst();
		results.setMember(h1.getCluster().getLocalMember());
		IMap<String,List<RetryHolder>> map = h1.getMap(mapName);
		Iterator<String> iter = map.localKeySet().iterator();
		int count = 0;
		while (iter.hasNext() && count < numResults) {
			for (RetryHolder retry:map.get(iter.next())) {
				try {
				interpreter.set(DATA_PROP,retry.getRetryData() );
				interpreter.set(RETRY_PROP, retry);
				Boolean match = (Boolean)interpreter.eval(beanShell);
				if (match) {
					list.add(retry);
					count++;
				}
				}catch (EvalError e) {
					e.printStackTrace();
				}
			}
		}
		results.setResults(list);	
				
		
		return results;
	}

	@Override
	public String getMapName() {
		return mapName;
	}

	@Override
	public Integer maxResultPerNode() {
		return numResults;
	}

	

	
	
}
