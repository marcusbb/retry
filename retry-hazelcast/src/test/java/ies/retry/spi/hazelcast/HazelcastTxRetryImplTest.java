package ies.retry.spi.hazelcast;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.List;

import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.xa.XAResource;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import ies.retry.Retry;
import ies.retry.RetryHolder;
import ies.retry.TxRetryManager;

import ies.retry.xml.XMLRetryConfigMgr;

public class HazelcastTxRetryImplTest {

	@BeforeClass
	public static void beforeClass() throws Exception {
		HzIntegrationTestUtil.beforeClass();
		XMLRetryConfigMgr.setXML_FILE("retry_config_tx.xml");
		
		
		
	}

	@AfterClass
	public static void afterClass() {

		HzIntegrationTestUtil.afterClass();
	}

	@Test
	public void testInitConfiguration() {
		TxRetryManager retryManager = (TxRetryManager)Retry.getRetryManager();
		
		assertTrue(retryManager instanceof HazelcastTxRetryImpl);
		
	}
	
	@Test
	public void testNonTxAddSuccess() {
		TxRetryManager retryManager = (TxRetryManager)Retry.getRetryManager();
		
		RetryHolder retry = new RetryHolder("id1", "POKE", new HashMap<>());
		retryManager.addRetry(retry);
		
		List<RetryHolder> list = retryManager.getRetry("id1", "POKE");
		
		assertNotNull(list);
	}
	
	@Test
	public void testTxAddSuccess() throws SystemException, RollbackException, SecurityException, IllegalStateException, HeuristicMixedException, HeuristicRollbackException {
		TxRetryManager retryManager = (TxRetryManager)Retry.getRetryManager();
		
		//enlist a transaction
		Transaction tx = new TxMock();
		retryManager.syncWith(tx);
		
		RetryHolder retry = new RetryHolder("id2", "POKE", new HashMap<>());
		retryManager.addRetry(retry);
		
		//assert not in grid yet
		assertNull(retryManager.getRetry("id2", "POKE"));
		
		tx.commit();
		
		assertNotNull(retryManager.getRetry("id2", "POKE"));
	}
	
	

	public static class TxMock implements Transaction {

		Synchronization sync = null;
		
		public TxMock() {
		}

		@Override
		public void commit() throws RollbackException, HeuristicMixedException, HeuristicRollbackException,
				SecurityException, IllegalStateException, SystemException {
			sync.afterCompletion(Status.STATUS_COMMITTED);

		}

		@Override
		public boolean delistResource(XAResource arg0, int arg1) throws IllegalStateException, SystemException {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public boolean enlistResource(XAResource arg0)
				throws RollbackException, IllegalStateException, SystemException {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public int getStatus() throws SystemException {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public void registerSynchronization(Synchronization arg0)
				throws RollbackException, IllegalStateException, SystemException {
			this.sync = arg0;

		}

		@Override
		public void rollback() throws IllegalStateException, SystemException {
			// TODO Auto-generated method stub

		}

		@Override
		public void setRollbackOnly() throws IllegalStateException, SystemException {
			// TODO Auto-generated method stub

		}

	}
}
