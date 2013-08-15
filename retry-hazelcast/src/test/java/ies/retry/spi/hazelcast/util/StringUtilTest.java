package ies.retry.spi.hazelcast.util;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import junit.framework.Assert;

import org.junit.BeforeClass;
import org.junit.Test;

public class StringUtilTest {

	private static Map<String,String> replacements = new HashMap<String,String>();
	private static String KEY1 = "KEY1";
	private static String VALUE1 = "VALUE1";
	private static String KEY2 = "KEY2";
	private static String VALUE2 = "VALUE2";
	private static Properties properties = new Properties();
	
	@BeforeClass
    public static void setUpBeforeClass() throws Throwable {
		replacements.put(KEY1, VALUE1);
		replacements.put(KEY2, VALUE2);
		properties.put(KEY1, VALUE1);
		properties.put(KEY2, VALUE2);
	}	

	@Test
	public void replaceHappyPath() {
		Assert.assertEquals(VALUE1, StringUtil.replace("${" + KEY1 + "}", replacements));
	}
	
	@Test
	public void replaceHappyPath1() {
		Assert.assertEquals(" " + VALUE1 + " ", StringUtil.replace(" ${" + KEY1 + "} ", replacements));
	}
	
	@Test
	public void replaceHappyPath2() {
		Assert.assertEquals(VALUE1 + " " + VALUE2, StringUtil.replace("${" + KEY1 + "} ${" + KEY2 + "}", replacements));
	}
	
	@Test
	public void replaceHappyPath4() {
		Assert.assertEquals(VALUE1 + VALUE2, StringUtil.replace("${" + KEY1 + "}${" + KEY2 + "}", replacements));
	}
	
	@Test
	public void replaceHappyPath5() {
		Assert.assertEquals(" " + VALUE1 + " " + VALUE2 + " ", StringUtil.replace(" ${" + KEY1 + "} ${" + KEY2 + "} ", replacements));
	}
	
	@Test
	public void replaceHappyPath6() {
		Assert.assertEquals(" " + VALUE1 + "KEY1" + VALUE2 + " ", StringUtil.replace(" ${" + KEY1 + "}KEY1${" + KEY2 + "} ", replacements));
	}
	
	@Test
	public void replaceNullMap() {
		Assert.assertEquals("${" + KEY1 + "}", StringUtil.replace("${" + KEY1 + "}", (Map)null));
	}
	
	@Test
	public void replaceEmptyMap() {
		Map<String,String> empty = new HashMap<String,String>();
		Assert.assertEquals("${" + KEY1 + "}", StringUtil.replace("${" + KEY1 + "}", empty));
	}
	
	@Test
	public void replaceNoKeyFound() {
		Map<String,String> empty = new HashMap<String,String>();
		replacements.put(KEY2, VALUE2);
		Assert.assertEquals("${" + KEY1 + "}", StringUtil.replace("${" + KEY1 + "}", empty));
	}
	
	@Test
	public void replaceCorners() {
		StringUtil util = new StringUtil();		// For the sake of coverage
		assertEquals("${Foo}", util.replace("${Foo}", replacements));
		assertEquals("Foo", util.replace("$Foo", replacements));
	}
	
	@Test
	public void replacePropertiesHappyPath() {
		Assert.assertEquals(VALUE1, StringUtil.replace("${" + KEY1 + "}", properties));
	}
}


