package ies.retry.spi.hazelcast.persistence;

public enum DBMergePolicy {

	//finds first, then updates or inserts
	FIND_OVERWRITE,
	
	//doesn't find, just persists
	OVERWRITE,
	
	//orders by TS - merging DB and cache results ordered by create ts
	ORDER_TS,
	
	//ordered by TS - discards dup ts
	ORDER_TS_DISCARD_DUP_TS;
}
