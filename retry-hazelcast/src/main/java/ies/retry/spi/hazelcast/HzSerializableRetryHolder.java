package ies.retry.spi.hazelcast;

import ies.retry.Retry;
import ies.retry.RetryHolder;
import ies.retry.RetrySerializer;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.hazelcast.nio.DataSerializable;

public class HzSerializableRetryHolder implements DataSerializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private List<RetryHolder> holderList;
	private RetrySerializer serizlizer;
	
	public HzSerializableRetryHolder(RetryHolder holder,RetrySerializer serializer) {
		this.holderList = new ArrayList<>(1);
		holderList.add(holder);
		this.serizlizer = serializer;
	}
	
	public HzSerializableRetryHolder(List<RetryHolder> list,RetrySerializer serializer) {
		this.holderList = list;
		this.serizlizer = serializer;
	}
	@Override
	public void readData(DataInput input) throws IOException {
		holderList = new ArrayList<>(input.readInt());
		for (RetryHolder holder:holderList) {
			holder.setId(input.readUTF());
			holder.setType(input.readUTF());
			holder.setCount(input.readInt());
			holder.setSystemTs(input.readLong());
			holder.setNextAttempt(input.readLong());
			int ps = input.readInt();
			if (ps > 0) {
				byte []payload = new byte[ps];
				input.readFully(payload);
				holder.setRetryData(serizlizer.serializeToObject(payload) );
			}
			
			
		}
		
		
	}

	@Override
	public void writeData(DataOutput output) throws IOException {
		output.write(holderList.size());
		for (RetryHolder holder:holderList) {
			output.writeUTF(holder.getId());
			output.writeUTF(holder.getType());
			output.write(holder.getCount());
			output.writeLong(holder.getSystemTs());
			output.writeLong(holder.getNextAttempt());
			if (holder.getRetryData() != null) {
				byte []payload = serizlizer.serializeToByte(holder.getRetryData());
				output.writeInt(payload.length);
				output.write(payload);
			}else {
				output.write(0);
			}
			
			
			
		}
		
	}
	public List<RetryHolder> getHolderList() {
		return holderList;
	}
	
	

}
