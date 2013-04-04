/**
 * 
 */
package edu.illinois.cs.osl.aj;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * @author Peter Dinges <pdinges@acm.org>
 *
 */
public class FieldAccessLog {
	
	public final static byte OP_ENTER = 0x1;
	public final static byte OP_VALUE = 0x2;
	public final static byte OP_GET_PRIMITIVE = 0x4;
	public final static byte OP_GET_REFERENCE = 0x14;
	public final static byte OP_GET_ARRAY = 0x24;
	public final static byte OP_PUT_PRIMITIVE = 0x8;
	public final static byte OP_PUT_REFERENCE = 0x18;
	public final static byte OP_PUT_ARRAY = 0x28;
	public final static byte OP_MONITOR_ENTER = 0x30;
	public final static byte OP_MONITOR_EXIT = 0x31;
	public final static byte OP_EXIT = 0x40;
	
	// FileChannel supposedly offers the best performance.
	// http://stackoverflow.com/questions/4358875/fastest-way-to-write-an-array-of-integers-to-a-file-in-java
	
	private static FileChannel log = null;
	
	public static void open(final String outputFileName) {
		try {
			if (log == null) {
				System.err.println("Opening log " + outputFileName);
				log = new FileOutputStream(outputFileName).getChannel();
			}
		} catch (FileNotFoundException e) {
			System.err.println(">>> ERROR: Setting up the field access log failed.");
			System.err.println("    Could not open log file '" + outputFileName + "'");
			System.exit(1);
		}
	}
	
	private static void write(ByteBuffer buf) {
		try {
			buf.rewind();
			log.write(buf);
		} catch (IOException e) {
			System.err.println(">>> ERROR: Writing to the log failed.  Aborting.");
			System.exit(2);
		}
	}
	
	public static void close() {
		try {
			if (log.isOpen()) {
				log.force(true);
//				log.close();
			}
		} catch (IOException e) {
			System.err.println(">>> ERROR: Closing the field access log failed.");
		}
	}

	
	public static void logMethodEntry(final int methodId) {
		ByteBuffer buf = ByteBuffer.allocate(13);
	
		buf.put(OP_ENTER);
		buf.putLong(Thread.currentThread().getId());
		buf.putInt(methodId);
		
		write(buf);
	}
	
	public static void logMethodExit() {
		ByteBuffer buf = ByteBuffer.allocate(9);

		buf.put(OP_EXIT);
		buf.putLong(Thread.currentThread().getId());
		
		write(buf);
	}
	
	public static void logParameter(final byte parameter, final Object value) {
		ByteBuffer buf = ByteBuffer.allocate(14);

		buf.put(OP_VALUE);
		buf.putLong(Thread.currentThread().getId());
		buf.put(parameter);
		buf.putInt(System.identityHashCode(value));
		
		write(buf);
	}
	
	public static void logMonitorEntry(final Object value, final int synchronizedBlockId) {
		ByteBuffer buf = ByteBuffer.allocate(17);
	
		buf.put(OP_MONITOR_ENTER);
		buf.putLong(Thread.currentThread().getId());
		buf.putInt(System.identityHashCode(value));
		buf.putInt(synchronizedBlockId);
		
		write(buf);
	}
	
	public static void logMonitorExit() {
		ByteBuffer buf = ByteBuffer.allocate(9);

		buf.put(OP_MONITOR_EXIT);
		buf.putLong(Thread.currentThread().getId());
		
		write(buf);
	}
	
	public static void logPrimitiveGet(final Object owner, final int fieldId) {
		ByteBuffer buf = ByteBuffer.allocate(17);

		buf.put(OP_GET_PRIMITIVE);
		buf.putLong(Thread.currentThread().getId());
		buf.putInt(System.identityHashCode(owner));
		buf.putInt(fieldId);
		
		write(buf);
	}
	
	public static void logReferenceGet(final Object owner, final Object value, final int fieldId) {
		ByteBuffer buf = ByteBuffer.allocate(21);

		buf.put(OP_GET_REFERENCE);
		buf.putLong(Thread.currentThread().getId());
		buf.putInt(System.identityHashCode(owner));
		buf.putInt(fieldId);
		buf.putInt(System.identityHashCode(value));
		
		write(buf);
	}

	public static void logArrayLoad(final Object array, final Object value, final int index) {
		ByteBuffer buf = ByteBuffer.allocate(21);

		buf.put(OP_GET_ARRAY);
		buf.putLong(Thread.currentThread().getId());
		buf.putInt(System.identityHashCode(array));
		buf.putInt(index);
		buf.putInt(System.identityHashCode(value));
		
		write(buf);
	}
	
	public static void logPrimitivePut(final Object owner, final int fieldId) {
		ByteBuffer buf = ByteBuffer.allocate(17);

		buf.put(OP_PUT_PRIMITIVE);
		buf.putLong(Thread.currentThread().getId());
		buf.putInt(System.identityHashCode(owner));
		buf.putInt(fieldId);
		
		write(buf);
	}
	
	public static void logReferencePut(final Object owner, final Object value, final int fieldId) {
		ByteBuffer buf = ByteBuffer.allocate(21);

		buf.put(OP_PUT_REFERENCE);
		buf.putLong(Thread.currentThread().getId());
		buf.putInt(System.identityHashCode(owner));
		buf.putInt(fieldId);
		buf.putInt(System.identityHashCode(value));
		
		write(buf);
	}

	public static void logArrayStore(final Object array, final Object value, final int index) {
		ByteBuffer buf = ByteBuffer.allocate(21);

		buf.put(OP_PUT_ARRAY);
		buf.putLong(Thread.currentThread().getId());
		buf.putInt(System.identityHashCode(array));
		buf.putInt(index);
		buf.putInt(System.identityHashCode(value));
		
		write(buf);
	}


	public static void main(String[] args) {
		ByteBuffer bb = ByteBuffer.allocate(32);
		open("foo");

		bb.putLong(Thread.currentThread().getId());
		bb.putInt(System.identityHashCode(new Object()));
		bb.putInt(System.identityHashCode(new Object()));
		bb.rewind();
		
		try {
			log.write(bb);
		} catch (IOException e) {
			System.err.println(">>> ERROR: Writing to the log failed.  Aborting.");
			System.exit(2);
		}
		close();
	}
}
