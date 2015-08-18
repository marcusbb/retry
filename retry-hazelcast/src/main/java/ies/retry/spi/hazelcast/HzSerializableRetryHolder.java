package ies.retry.spi.hazelcast;

import ies.retry.RetryHolder;
import ies.retry.RetrySerializer;
import ies.retry.spi.hazelcast.util.KryoSerializer;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import com.hazelcast.core.PartitionAware;
import com.hazelcast.nio.DataSerializable;


public class HzSerializableRetryHolder implements Serializable,DataSerializable,PartitionAware<String>,List<RetryHolder> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private List<RetryHolder> holderList;
	private transient RetrySerializer serializer;
	private boolean deferPayloadSerialization = false; 
	
	
	public HzSerializableRetryHolder() {
		this.serializer = new KryoSerializer();
	}
	public HzSerializableRetryHolder(RetryHolder holder,RetrySerializer serializer) {
		this.holderList = new ArrayList<>(1);
		holderList.add(holder);
		this.serializer = serializer;
	}
	
	public HzSerializableRetryHolder(List<RetryHolder> list,RetrySerializer serializer) {
		this.holderList = list;
		this.serializer = serializer;
	}
	
	public void setSerializer(RetrySerializer serializer) {
		this.serializer = serializer;
	}
	@Override
	public void readData(DataInput input) throws IOException {
		//TODO: this is not ideal mechanism, but we need to transmit
		//some information of serializer in the payload
		//unless we make a call to retry configuration here (not ideal either)
		try {
			this.serializer = (RetrySerializer)(Class.forName(input.readUTF()).newInstance());
		}catch (Exception e) {
			this.serializer = new KryoSerializer();
		}
		int len = input.readByte();
		this.deferPayloadSerialization = input.readBoolean();
		holderList = new ArrayList<>();
		for (int i=0;i<len;i++) {
			RetryHolder holder = new RetryHolder(null,null);
			holder.setId(input.readUTF());
			holder.setType(input.readUTF());
			holder.setCount(input.readByte());
			holder.setSystemTs(input.readLong());
			holder.setNextAttempt(input.readLong());
			holder.setSecondaryIndex(input.readUTF());
			int ps = input.readByte();
			if (ps > 0) {
				byte []payload = new byte[ps];
				input.readFully(payload);
				if (deferPayloadSerialization)
					holder.setPayload(payload);
				else
					holder.setRetryData(serializer.serializeToObject(payload) );
			}
			holderList.add(holder);
			
			
		}
		
		
	}

	@Override
	public void writeData(DataOutput output) throws IOException {
		if (serializer == null)
			this.serializer = new KryoSerializer();
		output.writeUTF(this.serializer.getClass().getName());
		output.writeByte(holderList.size());
		output.writeBoolean(deferPayloadSerialization);
		if (holderList == null)
			throw new IllegalArgumentException("holderList is null");
		for (RetryHolder holder:holderList) {
			output.writeUTF(holder.getId());
			output.writeUTF(holder.getType());
			output.write(holder.getCount());
			output.writeLong(holder.getSystemTs());
			output.writeLong(holder.getNextAttempt());
			output.writeUTF(holder.getSecondaryIndex());
			if (holder.getRetryData() != null) {
				byte []payload = serializer.serializeToByte(holder.getRetryData());
				output.write(payload.length);
				output.write(payload);
			}else {
				output.write(0);
			}
			
			
			
		}
		
	}
	public boolean isDeferPayloadSerialization() {
		return deferPayloadSerialization;
	}
	public void setDeferPayloadSerialization(boolean deferPayloadSerialization) {
		this.deferPayloadSerialization = deferPayloadSerialization;
	}
	
	public List<RetryHolder> getHolderList() {
		return holderList;
	}

	@Override
	public int size() {
		return holderList.size();
	}

	@Override
	public boolean isEmpty() {
		return holderList.isEmpty();
	}

	@Override
	public boolean contains(Object o) {
		return holderList.contains(o);
	}

	@Override
	public Iterator<RetryHolder> iterator() {
		return holderList.iterator();
	}

	@Override
	public Object[] toArray() {
		return holderList.toArray();
	}

	@Override
	public <T> T[] toArray(T[] a) {
		return holderList.toArray(a);
	}

	@Override
	public boolean add(RetryHolder e) {
		return holderList.add(e);
	}

	@Override
	public boolean remove(Object o) {
		return holderList.remove(o);
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		return holderList.containsAll(c);
	}

	@Override
	public boolean addAll(Collection<? extends RetryHolder> c) {
		return holderList.addAll(c);
	}

	@Override
	public boolean addAll(int index, Collection<? extends RetryHolder> c) {
		return holderList.addAll(index, c);
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		return holderList.removeAll(c);
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		return holderList.retainAll(c);
	}

	@Override
	public void clear() {
		holderList.clear();
		
	}

	@Override
	public RetryHolder get(int index) {
		return holderList.get(index);
	}

	@Override
	public RetryHolder set(int index, RetryHolder element) {
		return holderList.set(index, element);
	}

	@Override
	public void add(int index, RetryHolder element) {
		holderList.add(index,element);
		
	}

	@Override
	public RetryHolder remove(int index) {
		return holderList.remove(index);
	}

	@Override
	public int indexOf(Object o) {
		return holderList.indexOf(o);
	}

	@Override
	public int lastIndexOf(Object o) {
		return holderList.lastIndexOf(o);
	}

	@Override
	public ListIterator<RetryHolder> listIterator() {
		return holderList.listIterator();
	}

	@Override
	public ListIterator<RetryHolder> listIterator(int index) {
		return holderList.listIterator(index);
	}

	@Override
	public List<RetryHolder> subList(int fromIndex, int toIndex) {
		return holderList.subList(fromIndex, toIndex);
	}
	@Override
	public String getPartitionKey() {
		if (holderList != null && holderList.get(0) != null)
			return holderList.get(0).getId();
		
		throw new IllegalArgumentException("holderList is null");
	}
	
	
	

}
