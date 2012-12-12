package ies.retry.spi.hazelcast.util;

import ies.retry.RetryHolder;

import java.util.Comparator;

public class TSHolderComparator implements Comparator<RetryHolder>{

	@Override
	public int compare(RetryHolder o1, RetryHolder o2) {
		return new Long(o1.getSystemTs()).compareTo(new Long(02));
	}
	
	

}
