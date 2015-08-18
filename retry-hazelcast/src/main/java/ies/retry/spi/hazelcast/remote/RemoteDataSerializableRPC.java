package ies.retry.spi.hazelcast.remote;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.concurrent.Callable;

import com.hazelcast.core.PartitionAware;
import com.hazelcast.nio.DataSerializable;

import ies.retry.Retry;
import ies.retry.spi.hazelcast.HzSerializableRetryHolder;

public class RemoteDataSerializableRPC<T> implements Serializable,Callable<T>,DataSerializable,PartitionAware<Object> {

	
	private static final long serialVersionUID = -4144188157501414137L;
	private String method;
	private HzSerializableRetryHolder serializable;
	private String idHandle;
	private Object partitionKey;
	
	public RemoteDataSerializableRPC() {
		this.method = "undefined";
		
	}
	public RemoteDataSerializableRPC(String method,HzSerializableRetryHolder serializable) {
		this.method = method;
		this.serializable = serializable;
	}
	public RemoteDataSerializableRPC(String method,HzSerializableRetryHolder serializable,Object partitionKey) {
		this.method = method;
		this.serializable = serializable;
		this.partitionKey = partitionKey;
	}
	public Object target() {
		if (getIdHandle() != null)
			return Retry.getByInst(getIdHandle());
		return Retry.getRetryManager();
	}
	

	@Override
	public T call() throws Exception {
		
		//determine signature types
//		Class<? extends DataSerializable> []parameterTypes = new Class[1];
				
		Method m = target().getClass().getMethod(method, HzSerializableRetryHolder.class); 
		
		return (T)m.invoke(target(), serializable);
	}
	@Override
	public void writeData(DataOutput out) throws IOException {
		out.writeUTF(method);
		out.writeUTF(idHandle);
		this.serializable.writeData(out);
		
	}
	@Override
	public void readData(DataInput in) throws IOException {
		this.method = in.readUTF();
		this.idHandle = in.readUTF();
		this.serializable = new HzSerializableRetryHolder();
		this.serializable.readData(in);
		
	}
	public String getIdHandle() {
		return idHandle;
	}
	public void setIdHandle(String idHandle) {
		this.idHandle = idHandle;
	}
	@Override
	public Object getPartitionKey() {
		return partitionKey;
	}
	
	
}
