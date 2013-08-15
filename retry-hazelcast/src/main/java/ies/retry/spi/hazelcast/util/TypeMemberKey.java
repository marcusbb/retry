package ies.retry.spi.hazelcast.util;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.Serializable;

import com.hazelcast.core.Member;
import com.hazelcast.nio.DataSerializable;

/**
 * Composite type and member key.
 * 
 * @author msimonsen
 *
 */
public class TypeMemberKey implements Serializable,DataSerializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7117438265808721289L;

	
	private String type;
		
	private String hzMember;
	//Required for hazelcast
	public TypeMemberKey() {
		
	}
	
	public TypeMemberKey(String type,Member hzMember) {
		this.type = type;
		this.hzMember = hzMember.toString();
	
	}
	
	public TypeMemberKey(String type,String hzMember) {
		this.type = type;
		this.hzMember = hzMember;
	
	}

	
	/*@Override
	public int hashCode() {
		final int PRIME = 31;
        int result = type.hashCode();
        result = PRIME * result +  hzMember.hashCode();
        return result;
	}


	@Override
	public boolean equals(Object obj) {
		boolean eq = false;
		TypeMemberKey tmk = (TypeMemberKey)obj;
		if (tmk.type.equals(type) && tmk.hzMember.equals(hzMember)) {
			eq = true;
		}
		
		return eq;
		
	}*/


	


	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	

	public String getHzMember() {
		return hzMember;
	}


	public void setHzMember(String hzMember) {
		this.hzMember = hzMember;
	}


	@Override
	public void writeData(DataOutput out) throws IOException {
		out.writeUTF(type);
		out.writeUTF(hzMember);
		
		
	}


	@Override
	public void readData(DataInput in) throws IOException {
		type = in.readUTF();
		hzMember = in.readUTF();
		
	}


	@Override
	public String toString() {
		return "TypeMemberKey [type=" + type + ", hzMember=" + hzMember + "]";
	}
	
	
	
}
