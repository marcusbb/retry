package ies.retry.spi.hazelcast;

import ies.retry.RetryHolder;
import ies.retry.spi.hazelcast.util.IOUtil;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Queue;


/**
 * Serialized commit queue writer, that adds and takes 
 * in a serialized way - queue 
 * NOT THREAD SAFE  
 * 
 * @author msimonsen
 *
 */
public class LocalQueueLog {

	String workingDir = ".";
	 FileOutputStream commitFout;
	 public static String curFile = "commitlog";
	 public static String takeMarker = "take.marker";
	 RandomAccessFile takeOs;
	 Queue<Integer> writeQueue;
	 
	 int curAddMarker = 0;
	 int curHolderSize = 0; //this is not necessary 
	 int curTakeMarker = 0;
	 
	 public LocalQueueLog(String dirName) throws FileNotFoundException,IOException {
		this.workingDir = dirName;
		 File dir = new File(dirName);
		 if (dir.isDirectory()) {
			 
			 File commitLog = new File(dir, curFile);
			 if (commitLog.exists()) {
				 
			 }
			 commitFout = new FileOutputStream(commitLog, true);
		 }
		 File takeMarkerFile = new File(dir,takeMarker);
		 takeOs = new RandomAccessFile( takeMarkerFile,"rw" );
		 		
		 	byte []b = new byte[4];
			takeOs.read(b);
			 curTakeMarker = ByteBuffer.wrap(b).getInt();
			
		 	
		 
		 writeQueue = new LinkedList<Integer>();
	 }
	
	public void queue(RetryHolder holder) throws IOException {
		
		//TODO check to roll the file:
		
		byte [] b = IOUtil.serialize(holder);
		
		java.nio.ByteBuffer bb = java.nio.ByteBuffer.allocate(4);
		bb.putInt(b.length);
		//write length and payload
		commitFout.write(bb.array());
		commitFout.write(b);
		curHolderSize = b.length;
		curAddMarker += b.length + 4 ;
		writeQueue.add(b.length);
		
		
	}
	//marks the point (and increments the point of the last take marker)
	//
	public void dequeue() throws IOException {
		
		curTakeMarker += writeQueue.poll() + 4;
		//System.out.println("Cur take marker: " + curTakeMarker);
		 
		ByteBuffer  bb = java.nio.ByteBuffer.allocate(4);
		bb.putInt(curTakeMarker);
		
		//we may not write this all the time
		takeOs.write(bb.array(),0,4);
		takeOs.seek(0);
			
		
	}
	 
	public void close() throws IOException {
		commitFout.close();
		takeOs.close();
	}
	/**
	 * Replay at the current take marker
//	 * will it increment the take marker?
	 * @return
	 */
	public Collection<RetryHolder> replay() {
		
		
		return null;
	}
	
	public void removeFiles() throws IOException {
		close();
		
		File commitLog = new File(workingDir, curFile);
		commitLog.delete();
		
		File takeMarkerFile = new File(workingDir,takeMarker);
		takeMarkerFile.delete();
		
	}
	
	private void rollFile() {
		
	}
}
