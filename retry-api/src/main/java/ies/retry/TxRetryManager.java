package ies.retry;

import javax.transaction.RollbackException;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.xa.Xid;

public interface TxRetryManager extends RetryManager {

		
	void syncWith(Transaction tx) throws SystemException,RollbackException;
	
	
//	public static class RetrySynchronization implements Synchronization {
//
//		public RetrySynchronization(Transaction tx) throws SystemException,RollbackException {
//			tx.registerSynchronization(this);
//		}
//		@Override
//		public void afterCompletion(int arg0) {
//			
//			
//		}
//
//		@Override
//		public void beforeCompletion() {
//			// TODO Auto-generated method stub
//			
//		}
//		
//	}
}
