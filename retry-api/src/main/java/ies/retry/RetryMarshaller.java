package ies.retry;

import java.io.Serializable;

public interface RetryMarshaller {
	
    public Serializable marshallToObject(byte []b);
    
    public byte[] marshallToByte(Serializable t);

}
