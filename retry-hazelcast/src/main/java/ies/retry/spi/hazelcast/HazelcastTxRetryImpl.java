package ies.retry.spi.hazelcast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.xa.Xid;

import ies.retry.ConfigException;
import ies.retry.NoCallbackException;
import ies.retry.RetryHolder;
import ies.retry.TxRetryManager;

/**
 * Retry with transaction capabilities
 * 
 * @author marcus
 *
 */
public class HazelcastTxRetryImpl extends HazelcastRetryImpl implements Synchronization,TxRetryManager {

	//done with a thread local, which is most performant and cheapest but not durable
	//add doesn't support transactions that spans multiple threads 
	private static final ThreadLocal<Map<String,List<RetryHolder>>> txMap = new ThreadLocal<>();
	
	public HazelcastTxRetryImpl() {
		super();
		
	}

	@Override
	public void addRetry(RetryHolder holder) {
		
		if (txMap.get() == null)
			throw new IllegalStateException("Transaction hasn't been registerd");
		
		if (txMap.get().get(holder.getId()) == null) {
			List<RetryHolder> l = new ArrayList<>();
			l.add(holder);
			txMap.get().put(holder.getId(), l);
		}else {
			txMap.get().get(holder.getId()).add(holder);
		}
	}
	
	
	@Override
	public void putRetry(List<RetryHolder> retryList) throws NoCallbackException, ConfigException {
		
		if (txMap.get() == null)
			throw new IllegalStateException("Transaction hasn't been registerd");
		
	}
	

	@Override
	public void afterCompletion(int status) {
		txMap.set(null);
		if (status == Status.STATUS_COMMITTED) {
			//This is where we don't guarantee 
			//things will be added with absolute certainty
			for (List<RetryHolder> retryList: txMap.get().values()) {
				super.putRetry(retryList);
			}
		}
	}

	@Override
	public void beforeCompletion() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void syncWith(Transaction tx) throws SystemException, RollbackException {
		tx.registerSynchronization(this);
		txMap.set(new HashMap<String,List<RetryHolder>>());
	}

	
	
}
