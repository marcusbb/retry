package ies.retry.spi.hazelcast.persistence.cassandra;

import ies.retry.RetryHolder;
import ies.retry.spi.hazelcast.util.KryoSerializer;

import java.io.IOException;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;



/**
 * 
 * 
 * 
 * CREATE TABLE retry_archive (id varchar,type varchar,payload blob, orig_ts timestamp, ts timestamp, primary key (id,type,orig_ts))
 * 
 */
@Entity
@Table(name="RETRY_ARCHIVE")
public class CassArchiveRetryEntity  {

	public CassArchiveRetryEntity() {}
	
	public CassArchiveRetryEntity(RetryHolder holder) throws IOException {
		List<RetryHolder> holders = new ArrayList<RetryHolder>(1);
		holders.add(holder);
		setHolderList(holders);
		populate(holders);
	}
	
	public CassArchiveRetryEntity(List<RetryHolder> holderList) throws IOException,ClassNotFoundException{
		setHolderList(holderList);
		populate(holderList);
	}

	@Transient
	private List<RetryHolder> holderList;
	
	@Embeddable
	public static class Id implements Serializable {
		
		private static final long serialVersionUID = 6572258050209005515L;
		public Id() {}
		public Id(String id,String type,Date origDate) {
			this.id = id;
			this.type = type;
			this.origDate = origDate;
		}
		@Column(name="id")
		private String id;
		
		@Column(name="type")
		private String type;
		
		@Column(name="orig_ts")
		private java.util.Date origDate;
		
		public String getId() {
			return id;
		}
		public void setId(String id) {
			this.id = id;
		}
		public String getType() {
			return type;
		}
		public void setType(String type) {
			this.type = type;
		}
		
		public java.util.Date getOrigDate() {
			return origDate;
		}
		public void setOrigDate(java.util.Date origDate) {
			this.origDate = origDate;
		}
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((id == null) ? 0 : id.hashCode());
			result = prime * result + ((type == null) ? 0 : type.hashCode());
			result = prime * result + ((origDate == null) ? 0 : type.hashCode());
			return result;
		}
		@Override
		public boolean equals(Object obj) {
			boolean equals = false;
			CassArchiveRetryEntity.Id retryid = (CassArchiveRetryEntity.Id)obj;
			if (id.equals(retryid.getId()) && type.equals(retryid.getType()) && origDate.equals(retryid.getOrigDate()) )
				equals = true;
			return equals;
		}
		
		
	}
	
	@EmbeddedId
	private Id id;
	
	@Column(name="PAYLOAD")
	private ByteBuffer retryData;
	
	@Transient
	private Exception exception;
	
	@Transient
	private String exceptionMsg;
	
	@Column(name="ts")
	private Date curDate;
	
	public void populate(List<RetryHolder> holderList) throws IOException {
		RetryHolder holder = holderList.get(0);
		
		id = new Id(holder.getId(),holder.getType(),new Date(holder.getSystemTs()));
			
		//Serialize
		this.retryData = ByteBuffer.wrap(toByte(holderList));
		
	}
	
	private byte[] toByte(List<RetryHolder> retryHolder) throws IOException {
		return new KryoSerializer().serializeToByte((Serializable)retryHolder);
		
	}
	
	@SuppressWarnings("unchecked")
	public List<RetryHolder> fromByte(byte []b) throws IOException,ClassNotFoundException {
		throw new UnsupportedOperationException();
	}
	
	public List<RetryHolder> convertPayload() {
		ByteBuffer bb = getRetryData();
		if (bb == null)
			return new ArrayList<RetryHolder>();
		byte []b = new byte[bb.remaining()];
		int i=0;
		while (bb.remaining() >0)
			b[i++] = bb.get();
		
		return (List<RetryHolder>)new KryoSerializer().serializeToObject(b);
	}
	
	
	
	public List<RetryHolder> getHolderList() {
		
		return holderList;
	}

	public void setHolderList(List<RetryHolder> holderList) {
		this.holderList = holderList;
	}

	public Id getId() {
		return id;
	}

	public void setId(Id id) {
		this.id = id;
	}

	public ByteBuffer getRetryData() {
		return retryData;
	}
	public void setRetryData(ByteBuffer retryData) {
		this.retryData = retryData;
	}
	public Exception getException() {
		return exception;
	}

	public void setException(Exception exception) {
		this.exception = exception;
	}

	public String getExceptionMsg() {
		return exceptionMsg;
	}

	public void setExceptionMsg(String exceptionMsg) {
		this.exceptionMsg = exceptionMsg;
	}

	public Date getCurDate() {
		return curDate;
	}

	public void setCurDate(Date curDate) {
		this.curDate = curDate;
	}
	
	
}
