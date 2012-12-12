package ies.retry.spi.hazelcast.util;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

public class StringUtil {

	
	
	
	public static String replace(String source, Properties properties) {
		HashMap<String, String> map = new HashMap<String, String>(properties.size());
		Iterator<Object> iter = properties.keySet().iterator();
		while(iter.hasNext()) {
			String key = (String)iter.next();
			map.put(key, properties.getProperty(key));
		}
		return replace(source,map);
	}
	/**
	 * Replaces ${__} with a map according to what is provided.
	 * 
	 * @param source
	 * @param replacements
	 * @return
	 */
	public static String replace(String source,Map<String,String> replacements) {

		if (replacements == null )
			return source;
		if (replacements.size() ==0)
			return source;
		
		
		char []ch = source.toCharArray();
		StringBuilder formatted = new StringBuilder();
		boolean skip = false;
		
		for (int i=0;i<ch.length;i++) {
			
				
			if (i>=1 && ch[i-1] == '$' && ch[i] == '{') {
				//formatted.delete(i-1, i);
				StringBuilder rep = new StringBuilder();
				while(ch[++i] != '}') {
					rep.append(ch[i]);
				}
				String replace = replacements.get(rep.toString());
				if (replace == null)
					replace = "${" + rep.toString() + "}" ;
				formatted.append(replace);
				
				
			} else if (ch[i] == '$') {
				skip = true;
			}else {
				formatted.append(ch[i]);
			}
			
		}
		return formatted.toString();
		
	}
}
