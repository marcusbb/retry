package ies.retry.spi.hazelcast.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class IOUtil {

	public static String load(String file) throws IOException {
		FileInputStream fin = new FileInputStream(file);
		byte []b = new byte[fin.available()];
		fin.read(b);
		String str =  new String(b);
		fin.close();
		return str;
	}

	public static byte[] serialize(Object obj) throws IOException {
		byte []b = null;
		ObjectOutputStream out = null;
		try {
			ByteArrayOutputStream bout = new ByteArrayOutputStream();
			out = new ObjectOutputStream(bout);
			out.writeObject(obj);
			b = bout.toByteArray();
		} finally {
			if (out != null) {
				out.close();
			}
		}
		return b;
	}
	
	public static  Object deserialize(byte []b) throws IOException,ClassNotFoundException {
		Object obj = null;
		ObjectInputStream oin = null;
		try {
			//System.out.println("read obj: " + b.length);
			ByteArrayInputStream bin = new ByteArrayInputStream(b);
			oin = new ObjectInputStream(bin);
			obj = oin.readObject();
			return obj;
		} finally {
			if (oin != null) {
				oin.close();
			}
		}
	}
}
