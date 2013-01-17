package hazelcast;

import junit.framework.Assert;
import ies.retry.RetryState;
import ies.retry.spi.hazelcast.config.HazelcastConfigManager;
import ies.retry.spi.hazelcast.util.TypeMemberKey;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.hazelcast.config.ClasspathXmlConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;

@Ignore
public class KeyTest {

	protected static HazelcastInstance h1 = null;
	protected static HazelcastInstance h2 = null;
	
	@BeforeClass
	public static void beforeClass() {
		ClasspathXmlConfig config = new ClasspathXmlConfig("hazelcast.xml");
		
		h1 = Hazelcast.getDefaultInstance();
		//h2 = Hazelcast.newHazelcastInstance(config);
	}
	
	@Test
	public void test() {
		h1.getLifecycleService().shutdown();
		//h2.getLifecycleService().shutdown();
		IMap<TypeMemberKey,RetryState> map = h1.getMap("NEAR--WHATEVER");
		TypeMemberKey key1 = new TypeMemberKey("type", h1.getCluster().getLocalMember().toString());
		//TypeMemberKey key2 = new TypeMemberKey("type", h2.getCluster().getLocalMember().toString());
		
		map.put(key1,RetryState.DRAINED);
		//map.put(key2,RetryState.DRAINED);
		
		Assert.assertEquals(2, map.size());
		
	}

}
