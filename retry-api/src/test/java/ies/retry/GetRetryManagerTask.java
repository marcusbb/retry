package ies.retry;

import java.util.concurrent.Callable;

public class GetRetryManagerTask implements Callable<RetryManager>{

	public RetryManager call() throws Exception {
		return Retry.getRetryManager();
	}

}
