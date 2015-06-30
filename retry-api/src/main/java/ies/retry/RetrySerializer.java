package ies.retry;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public interface RetrySerializer {
	
    public Serializable serializeToObject(byte []b);
    
    public byte[] serializeToByte(Serializable t);
    
    public static class JavaSerializer implements RetrySerializer {

		@Override
		public Serializable serializeToObject(byte[] b) {
			Object obj = null;
			ObjectInputStream oin = null;
			try {
				//System.out.println("read obj: " + b.length);
				ByteArrayInputStream bin = new ByteArrayInputStream(b);
				oin = new ObjectInputStream(bin);
				obj = oin.readObject();
				
			} catch ( IOException e) {
				throw new RuntimeException(e);
			} catch (ClassNotFoundException e) {
				throw new RuntimeException(e);
			} finally {
				if (oin != null) {
					try {oin.close(); } catch (IOException e){}
				}
			}
			return (Serializable)obj;
		}

		@Override
		public byte[] serializeToByte(Serializable obj) {
			byte []b = null;
			ObjectOutputStream out = null;
			try {
				ByteArrayOutputStream bout = new ByteArrayOutputStream();
				out = new ObjectOutputStream(bout);
				out.writeObject(obj);
				b = bout.toByteArray();
			} catch (IOException e) {
				throw new RuntimeException(e);
			} finally {
				if (out != null) {
					try {out.close();} catch (IOException e){}
				}
			}
			return b;
		}
    	
    }

}
