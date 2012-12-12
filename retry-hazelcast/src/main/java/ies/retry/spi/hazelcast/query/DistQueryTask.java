package ies.retry.spi.hazelcast.query;

import java.io.Serializable;
import java.util.concurrent.Callable;

public class DistQueryTask implements Callable<QueryResults>,Serializable {

	
	private static final long serialVersionUID = 1L;

	private BaseQuery query;
	
	public DistQueryTask(BaseQuery query) {
		this.query = query;
	}
	@Override
	public QueryResults call() throws Exception {

		return query.eval();
	}

}
