package ies.retry.spi.hazelcast;

import ies.retry.RetryCallback;
import ies.retry.RetryHolder;

public class SuccessCallback implements RetryCallback {

	public boolean onEvent(RetryHolder retry) throws Exception {
		System.out.println("Success " + retry.getId());
		return true;
	}

}
