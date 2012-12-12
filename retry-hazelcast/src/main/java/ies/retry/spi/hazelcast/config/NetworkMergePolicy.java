package ies.retry.spi.hazelcast.config;

import java.io.Serializable;

public enum NetworkMergePolicy implements Serializable {
	MERGE_MEM,DB_OVERWRITE;
}