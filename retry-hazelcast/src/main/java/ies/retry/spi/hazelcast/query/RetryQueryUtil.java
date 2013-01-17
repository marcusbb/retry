package ies.retry.spi.hazelcast.query;

import ies.retry.Retry;
import ies.retry.RetryHolder;
import ies.retry.spi.hazelcast.HazelcastRetryImpl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ExecutorService;

import bsh.EvalError;
import bsh.Interpreter;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.MultiTask;

public class RetryQueryUtil {

	static HazelcastInstance  h1 =  ((HazelcastRetryImpl)Retry.getRetryManager()).getH1();
	
	public static QueryResults execute(BaseQuery query)  {
		QueryResults finalResult = new QueryResults();
		ArrayList<RetryHolder> listHolder = new ArrayList<RetryHolder>();
				
		ExecutorService execService = h1.getExecutorService();
		
		DistQueryTask task = new DistQueryTask(query);
		MultiTask<QueryResults> multiTask = new MultiTask<QueryResults>(task, h1.getCluster().getMembers());
		execService.submit(multiTask);
		try {
			Collection<QueryResults> multiTaskResult = multiTask.get();
			for (QueryResults nodeResult:multiTaskResult) {
				listHolder.addAll(nodeResult.getResults());
			}
			finalResult.setResults(listHolder);
		}catch (Exception e) {
			throw new QueryException(e.getMessage(), e);
		}
		return finalResult;
	}
	
	/**
	 * This will replace the method on top.
	 * @param query
	 * @return
	 */
	public static ClusterQueryResults executeAll(BaseQuery query)  {
		ClusterQueryResults finalResult = new ClusterQueryResults();
		
				
		ExecutorService execService = h1.getExecutorService();
		
		DistQueryTask task = new DistQueryTask(query);
		MultiTask<QueryResults> multiTask = new MultiTask<QueryResults>(task, h1.getCluster().getMembers());
		execService.submit(multiTask);
		try {
			Collection<QueryResults> multiTaskResult = multiTask.get();
			for (QueryResults nodeResult:multiTaskResult) {
				finalResult.getQueryResults().add(nodeResult);
			}
			
		}catch (Exception e) {
			throw new QueryException(e.getMessage(), e);
		}
		return finalResult;
	}
	
	/**
	 * 
	 * 
	 * @param totalResults
	 * @return
	 */
	public static int maxResultPerNode(int totalResults) {
		if (totalResults == 0)
			return 0;
		
		return totalResults/h1.getCluster().getMembers().size();
	}
	public static boolean preParseExpression(String expression) throws EvalError {
		Interpreter interpreter = new Interpreter();
		RetryHolder holder = new RetryHolder("NONE", "NONE");
		holder.setRetryData(new String());
		holder.setException(new Exception());
		interpreter.set(BeanShellQuery.DATA_PROP, holder.getRetryData());
		interpreter.set(BeanShellQuery.RETRY_PROP, holder);
		interpreter.eval(expression);
		
		return true;
	}
	public static HazelcastInstance getH1() {
		return h1;
	}

	public static void setH1(HazelcastInstance h1) {
		RetryQueryUtil.h1 = h1;
	}
	
	
}
