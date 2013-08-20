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
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Queue;


/**
 * Commit log writer, which stores all objects in a single file (TODO and rolls it)
 * 
 * There are is additionally a state maintained, being the "take" marker, which is persisted
 * as well if the JVM is to crash. 
 * 
 * WorkFlow
 * <ol>
 * 	<li>{@link #queue(RetryHolder)} - stores the data
 * 	<li>{@link #moveTakeMarker()} - increments the take marker to the next point of where a potential write will occurr
 * 	<li>{@link #replayFromFile()} - starting at the take marker will replay and return the objects from the last take marker
 * </ol>
 * 
 * Serialized commit queue writer, that adds and takes 
 * in a serialized way - queue 
 * 
 * 
 * @author msimonsen
 *
 */
public class LocalQueueLog {

	String workingDir = ".";
	 FileOutputStream commitFout;
	 //rename this variable
	 public static String curFile = "commitlog";
	 public static String takeMarker = "take.marker";
	 RandomAccessFile takeOs;
	 Queue<Integer> writeQueue;
	 public static int INT_BYTE_SIZE = 4;
	 
	 int curAddMarker = 0;
	 int curTakeMarker = 0;
	 
	public LocalQueueLog(String dirName) throws FileNotFoundException,IOException {
		this.workingDir = dirName;
		File dir = new File(dirName);
		if (dir.isDirectory()) {

			File commitLog = new File(dir, curFile);
			if (commitLog.exists()) {

			}
			commitFout = new FileOutputStream(commitLog, true);
			// We don't commit log files that big > Integer.MAX_VALUE
			curAddMarker = (int) (commitLog.length() - 1);
		}
		File takeMarkerFile = new File(dir, takeMarker);
		takeOs = new RandomAccessFile(takeMarkerFile, "rw");

		byte[] b = new byte[4];
		takeOs.read(b);
		curTakeMarker = ByteBuffer.wrap(b).getInt();

		writeQueue = new LinkedList<Integer>();
	}
	
	public void queue(RetryHolder holder) throws IOException {
		
		//TODO check to roll the file:
		
		byte [] b = IOUtil.serialize(holder);
		
		java.nio.ByteBuffer bb = java.nio.ByteBuffer.allocate(INT_BYTE_SIZE);
		bb.putInt(b.length);
		//write length and payload
		commitFout.write(bb.array());
		commitFout.write(b);
		
		curAddMarker += b.length + 4 ;
		writeQueue.add(b.length);
		
		
	}
	
	/**
	 * Increment the take marker - which will be called when 
	 * @throws IOException
	 */
	public void moveTakeMarker() throws IOException {
		
		curTakeMarker += writeQueue.poll() + INT_BYTE_SIZE;
		 
		ByteBuffer  bb = ByteBuffer.allocate(INT_BYTE_SIZE);
		bb.putInt(curTakeMarker);
		
		//we may not write this all the time
		takeOs.write(bb.array(),0,INT_BYTE_SIZE);
		takeOs.seek(0);
			
		
	}
	 
	
	public void close() throws IOException {
		commitFout.close();
		takeOs.close();
	}
	/**
	 * Replay at the current take marker
	 * Will also increment the marker
	 *  Since this method is quite expensive, and will
	 *  only be executed when there is a "recovery" scenario
	 *  
	 * @return
	 */
	public Collection<RetryHolder> replayFromFile() throws IOException {
		FileInputStream fin = null;
		Collection<RetryHolder> col = new ArrayList<RetryHolder>();
		
		try {
			fin = new FileInputStream(new File(workingDir, curFile));
		
			fin.skip(curTakeMarker);
			 
			boolean EOF = false;
			byte []b = new byte[INT_BYTE_SIZE];
			//now loop over the file and serialize the objects
			while (!EOF) {
				
				if (fin.read(b) ==-1) 
					break;
				
				int nextBytes = ByteBuffer.wrap(b).getInt();
				byte []bObj = new byte[nextBytes];
				
				EOF = fin.read(bObj,0,nextBytes) == -1;
				try {
					col.add( (RetryHolder)IOUtil.deserialize(bObj) );
				}catch (Exception e) {
					//TODO
					e.printStackTrace();
				}
				curTakeMarker += nextBytes +4;
			}
			
		}finally {
			if (fin != null)
				fin.close();
		}
		return col;
	}
	
	
	public void removeFiles() throws IOException {
		close();
		
		File commitLog = new File(workingDir, curFile);
		commitLog.delete();
		
		File takeMarkerFile = new File(workingDir,takeMarker);
		takeMarkerFile.delete();
		
	}
	/**
	 * This is a big TODO, and affects all the above operations.
	 */
	private void rollFile() {
		
	}
	
	public int getTakeMarker() {
		return this.curTakeMarker;
	}
	public int getAddMarker() {
		return this.curAddMarker;
	}
}
