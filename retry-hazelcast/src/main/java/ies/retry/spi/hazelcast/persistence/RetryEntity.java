package ies.retry.spi.hazelcast.persistence;

import ies.retry.RetryHolder;
import ies.retry.spi.hazelcast.util.IOUtil;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.Version;

import provision.services.logging.Logger;

@Entity
@Table(name="RETRIES")
public class RetryEntity implements Serializable{

	private static String CALLER = RetryEntity.class.getName();
	private static final long serialVersionUID = -5185244774028196569L;

	/**
	 * Required constructor
	 */
	public RetryEntity() {
		
	}
	public RetryEntity(RetryHolder holder) throws IOException {
		List<RetryHolder> holders = new ArrayList<RetryHolder>(1);
		holders.add(holder);
		setHolderList(holders);
		populate(holders);
	}
	
	public RetryEntity(List<RetryHolder> holderList) throws IOException,ClassNotFoundException{
		setHolderList(holderList);
		populate(holderList);
	}

	@Transient
	private List<RetryHolder> holderList;
	
	@EmbeddedId
	private RetryId id;
	
	@Column(name="PAYLOAD_DATA")
	@Lob
	private byte[] retryData;
	
	@Transient
	private Exception exception;
	
	@Transient
	private String exceptionMsg;
	
	@Version
	@Column(name="RETRY_VER")
	private Integer version;
	
	public void populate(List<RetryHolder> holderList) throws IOException {
		RetryHolder holder = holderList.get(0);
		
		id = new RetryId(holder.getId(),holder.getType());
		//1.0.9 HF - remove the exception from persistence
		Logger.debug(CALLER, "Dropping exception from RetryHolder","","ID", id,"TYPE",holder.getType());
		Exception[] exceptionBackup = new Exception[holderList.size()];
		int count = 0;
		for (RetryHolder th:holderList) {			
			
			if (th.getException() != null)
				//Logger.warn(CALLER, "Dropped_exception", th.getException());
			
			exceptionBackup[count++] = th.getException();
			th.setException(null);
		}
	
		//Serialize
		this.retryData = toByte(holderList);
				
		//Restore exception references		
		for (int i=0;i<count;i++)
		{
			holderList.get(i).setException(exceptionBackup[i]);
		}
	}
	
	private byte[] toByte(List<RetryHolder> retryHolder) throws IOException {
		return IOUtil.serialize(retryHolder);
	}
	
	public List<RetryHolder> fromByte(byte []b) throws IOException,ClassNotFoundException {
		return (List<RetryHolder>)IOUtil.deserialize(b);
	}
	
	public RetryHolder copyToHolder() {
		RetryHolder holder = null;
		if (id != null) {
			holder = new RetryHolder(id.getId(),id.getType());
			//holder.setFailed(failed);
			holder.setRetryData(retryData);
			
		}
		return holder;
	}
	
	
	public List<RetryHolder> getHolderList() {
		
		return holderList;
	}

	public void setHolderList(List<RetryHolder> holderList) {
		this.holderList = holderList;
	}

	public RetryId getId() {
		return id;
	}

	public void setId(RetryId id) {
		this.id = id;
	}

	public byte[] getRetryData() {
		return retryData;
	}
	public void setRetryData(byte[] retryData) {
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
	public Integer getVersion() {
		return version;
	}
	public void setVersion(Integer version) {
		this.version = version;
	}
	
}
