// Author: osafaeerad

package ies.retry.spi.hazelcast.util;

import static org.junit.Assert.assertEquals;
import org.junit.Test;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class IOUtilTest {

	@Test
	public void testLoad_pass() {
		System.out.println("Current working directory: " + System.getProperty("user.dir"));
		String msg = "I WORK!";
		IOUtil util = new IOUtil();		// For the sake of coverage
		File file = new File("IOUtilTestTemp.txt");
		
		try {
			file.createNewFile();
			BufferedWriter writer = new BufferedWriter(new FileWriter(file));
		    writer.write(msg);
		    writer.close();
		    assertEquals(msg, util.load(file.getName()));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		file.delete();
	}
}
