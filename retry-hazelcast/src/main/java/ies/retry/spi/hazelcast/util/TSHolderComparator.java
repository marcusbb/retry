package ies.retry.spi.hazelcast.util;

import ies.retry.RetryHolder;

import java.util.Comparator;

/*
 * Not consistent with equals method of RetryHolder. Use at your own risk
 */
public class TSHolderComparator implements Comparator<RetryHolder>{

	@Override
	public int compare(RetryHolder o1, RetryHolder o2) {
		return new Long(o1.getSystemTs()).compareTo(new Long(o2.getSystemTs()));
	}
}
