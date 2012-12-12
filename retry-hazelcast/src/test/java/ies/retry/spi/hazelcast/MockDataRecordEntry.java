package ies.retry.spi.hazelcast;

import com.hazelcast.impl.base.DataRecordEntry;
import com.hazelcast.nio.Data;
import com.hazelcast.nio.IOUtil;

class MockDataRecordEntry extends DataRecordEntry {

	Object value;
	Data data;
	Object key;
	
	public MockDataRecordEntry(Object key,Object value) {
		this.key = key;
		this.value = value;
		this.data = IOUtil.toData(value);
	}
	@Override
	public Object getValue() {
		return value;
	}

	@Override
	public Data getValueData() {
		return this.data;
	}
	@Override
	public Object getKey() {
		return this.key;
	}
	
	
}