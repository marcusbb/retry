// Author: osafaeerad

package ies.retry.spi.hazelcast.util;

import static org.junit.Assert.assertEquals;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.junit.Test;

public class IOUtilTest {

	@Test
	public void testLoad_pass() {
		System.out.println("Current working directory: " + System.getProperty("user.dir"));
		String msg = "I WORK!";
		File file = new File("IOUtilTestTemp.txt");
		
		try {
			file.createNewFile();
			BufferedWriter writer = new BufferedWriter(new FileWriter(file));
		    writer.write(msg);
		    writer.close();
		    assertEquals(msg, IOUtil.load(file.getName()));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		file.delete();
	}
}
