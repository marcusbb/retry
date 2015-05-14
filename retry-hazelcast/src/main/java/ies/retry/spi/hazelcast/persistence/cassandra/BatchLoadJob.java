package ies.retry.spi.hazelcast.persistence.cassandra;


import java.util.Collection;
import java.util.HashSet;

import com.datastax.driver.core.Session;

import reader.MTJobBootStrap;
import reader.ReaderConfig;
import reader.ReaderJob;

public class BatchLoadJob extends MTJobBootStrap {

	private Session session;

	RetryRowReaderLoader rowReader = null;
	
	private Collection<CassRetryEntity> results = null;
	
	private HashSet<String> types;
	
	public BatchLoadJob(HashSet<String> types) {
		this.types = types;
		
	}
	public BatchLoadJob(Collection<CassRetryEntity> results,HashSet<String> types) {
		this(types);
		this.results = results;
	}

	@Override
	public ReaderJob<?> initJob(ReaderConfig readerConfig) {
		this.session = getSession();
		if (results !=null)
			this.rowReader = new RetryRowReaderLoader(session, results,types);
		else
			this.rowReader = new RetryRowReaderLoader(session,types);
		
		return this.rowReader;
	}

	
}
