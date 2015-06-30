package ies.retry.spi.hazelcast.util;

import ies.retry.RetryHolder;
import ies.retry.RetrySerializer;

import java.io.Serializable;

import org.objenesis.strategy.StdInstantiatorStrategy;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;


public class KryoSerializer implements RetrySerializer {

	private static final Kryo kryo = new Kryo();

	 static {
		 kryo.setRegistrationRequired(false);
		 kryo.setInstantiatorStrategy(new StdInstantiatorStrategy()); // the way to instantiate object without default constructor like RetryHolder
		 
		 /**
		  * specific serializers might me created for serialized classes.
		  * not really performant serializers below. 
		  */
//		 kryo.setInstantiatorStrategy(new SerializingInstantiatorStrategy());
//		 CompatibleFieldSerializer<RetryHolder> retrySerializer = new CompatibleFieldSerializer<RetryHolder>(kryo,RetryHolder.class);
//		 kryo.register(ArrayList.class,new CollectionSerializer(RetryHolder.class, retrySerializer));
		 
		 /**
		  * of little performance gain
		  */
		 kryo.register(RetryHolder.class);
	 }

	@Override
	public Serializable serializeToObject(byte[] b) {

		Object o = kryo.readClassAndObject(new Input(b));
		return (o instanceof Serializable) ? (Serializable) o : null;
	}

	@Override
	public byte[] serializeToByte(Serializable t) {
		Output output = new Output(1000 << 4, 1000 << 18); // buffer size of 16K and expanding to 256M as needed - throws exception if exceeded
		kryo.writeClassAndObject(output, t);
		return output.toBytes();
	}

}
