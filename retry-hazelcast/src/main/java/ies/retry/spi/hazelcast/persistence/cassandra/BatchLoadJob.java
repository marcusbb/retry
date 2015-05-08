package ies.retry.spi.hazelcast.persistence.cassandra;


import java.util.Collection;

import com.datastax.driver.core.Session;

import reader.MTJobBootStrap;
import reader.ReaderConfig;
import reader.ReaderJob;

public class BatchLoadJob extends MTJobBootStrap {

	private Session session;

	RetryRowReaderLoader rowReader = null;
	
	private Collection<CassRetryEntity> results = null;
	
	public BatchLoadJob(int nThreads) {
		super(nThreads);
		
	}
	public BatchLoadJob(int nThreads,Collection<CassRetryEntity> results) {
		super(nThreads);
		this.results = results;
	}

	@Override
	public ReaderJob<?> initJob(ReaderConfig readerConfig) {
		this.session = getSession();
		if (results !=null)
			this.rowReader = new RetryRowReaderLoader(session, results);
		else
			this.rowReader = new RetryRowReaderLoader(session);
		
		return this.rowReader;
	}

	
}
