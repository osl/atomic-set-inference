package edu.illinois.cs.osl.aj;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Properties;

import com.ibm.wala.shrikeBT.ArrayLoadInstruction;
import com.ibm.wala.shrikeBT.ArrayStoreInstruction;
import com.ibm.wala.shrikeBT.ConstantInstruction;
import com.ibm.wala.shrikeBT.Constants;
import com.ibm.wala.shrikeBT.Disassembler;
import com.ibm.wala.shrikeBT.DupInstruction;
import com.ibm.wala.shrikeBT.GetInstruction;
import com.ibm.wala.shrikeBT.IInstruction;
import com.ibm.wala.shrikeBT.Instruction;
import com.ibm.wala.shrikeBT.InvokeInstruction;
import com.ibm.wala.shrikeBT.LoadInstruction;
import com.ibm.wala.shrikeBT.MethodData;
import com.ibm.wala.shrikeBT.MethodEditor;
import com.ibm.wala.shrikeBT.MonitorInstruction;
import com.ibm.wala.shrikeBT.PopInstruction;
import com.ibm.wala.shrikeBT.PutInstruction;
import com.ibm.wala.shrikeBT.ReturnInstruction;
import com.ibm.wala.shrikeBT.SwapInstruction;
import com.ibm.wala.shrikeBT.ThrowInstruction;
import com.ibm.wala.shrikeBT.Util;
import com.ibm.wala.shrikeBT.IInvokeInstruction.Dispatch;
import com.ibm.wala.shrikeBT.shrikeCT.ClassInstrumenter;
import com.ibm.wala.shrikeBT.shrikeCT.OfflineInstrumenter;
import com.ibm.wala.shrikeCT.ClassWriter;
import com.ibm.wala.util.io.CommandLine;

/**
 * A tool for adding field access tracing to compiled Java programs.
 * <p>
 * 
 * Tracing how the methods and threads of a program access the fields of objects
 * can be used to learn the semantic grouping of fields and objects in the
 * program. For example, if during execution the fields <code>firstName</code>
 * and <code>lastName</code> of <code>Person</code> objects are always accessed
 * together, without interleaving from concurrent threads, then it is likely
 * that these two fields form a semantic unit that should always be accessed
 * atomically (synchronized, or locked).
 * <p>
 * 
 * The tool instruments a given set of Java class files and adds log function
 * calls for tracing the following events while the program executes: method
 * entry, method exit, field access (read and write), and array access (read and
 * write). The recorded information identifies the executing thread, and names
 * the accessed field, together with the object that contains the field. For
 * fields and arrays of reference types, that is, types other than the
 * primitives <code>int</code>, <code>long</code>, <code>boolean</code>, etc.,
 * the tool furthermore records the value of the field. This allows
 * reconstructing (parts of) the program's object graph from the trace.
 * 
 * @author Peter Dinges <pdinges@acm.org>
 * 
 * @see edu.illinois.cs.osl.aj.FieldAccessLog
 */
public class Instrumenter {

	private static OfflineInstrumenter instrumenter;
	/** Instrumentation report for debugging and result checking */
	private static Writer report;

	/** Map from method names to unique integer ids (used in the log) */ 
	private static MethodNameTable methodNameTable;
	/** Map from field names to their unique integer ids (used in the log) */ 
	private static FieldNameTable fieldNameTable;

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		instrumenter = new OfflineInstrumenter();
		methodNameTable = new MethodNameTable();
		fieldNameTable = new FieldNameTable();

		// Process command line arguments
		args = instrumenter.parseStandardArgs(args);
		instrumenter.setPassUnmodifiedClasses(true);

		Properties p = CommandLine.parse(args);
		String metaFileName = p.getProperty("m", "meta_tables.txt");
		String reportFileName = p.getProperty("r", "report");
		String logFileName = p.getProperty("l", "field_access.log");

		// Open the report files here so that the program fails early if there
		// is a problem with the output.
		report = new BufferedWriter(new FileWriter(reportFileName, false));
		Writer m = new BufferedWriter(new FileWriter(metaFileName, false));
		
		// Instrument all classes and store the used name abbreviation
		// tables for the replay tool.
		instrumenter.beginTraversal();
		ClassInstrumenter ci;
		while ((ci = instrumenter.nextClass()) != null) {
			System.out.println(instrumenter.getLastClassResourceName());
			instrumentClass(ci, logFileName);
		}
		instrumenter.close();

		outputNameTables(m);
		m.close();
	}


	/**
	 * Write a plain text representations of the {@link methodNameTable} and
	 * {@link fieldNameTable} to the given Writer.
	 * 
	 * @param targetWriter
	 *            writer to use for outputting the tables.
	 * @throws IOException
	 *            if writing to <code>targetWriter</code> failed.
	 */
	private static void outputNameTables(Writer targetWriter) throws IOException {
		targetWriter.write("### Method Meta-Data ###\n");
		targetWriter.write("# ID | scope name | method name | signature | static? | access flags\n");
		targetWriter.write(methodNameTable.toString());

		targetWriter.write("\n");
		
		targetWriter.write("### Field Meta-Data ###\n");
		targetWriter.write("# ID | scope name | field name | field type\n");
		targetWriter.write(fieldNameTable.toString());
	}


	
	/*- Instrumentation -----------------------------------------------------*/
	
	
	/**
	 * Add field access tracing calls to all methods of the given class.
	 * Furthermore, add calls for opening and closing the trace log to the main
	 * method of the class.
	 * <p>
	 * 
	 * Note that the passed name of the trace log file will be hard coded into
	 * the instrumented program.
	 * <p>
	 * 
	 * Also note that the called logging functions assume that the log file has
	 * been opened and is available for writing. Consequently, the main
	 * class of the program <em>must</em> be instrumented.
	 * 
	 * @param ci
	 *            class whose methods to instrument
	 * @param logFileName
	 *            name of the trace log file to write when the program is
	 *            executed
	 * @throws Exception
	 */
	private static void instrumentClass(final ClassInstrumenter ci, String logFileName) throws Exception {
		for (int m = 0; m < ci.getReader().getMethodCount(); m++) {
			MethodData md = ci.visitMethod(m);

			// md could be null, e.g., if the method is abstract or native
			if (md == null) continue;

			report.write(">>> Instrumenting " + Util.makeClass(md.getClassType()) + "."
					+ md.getName() + md.getSignature() + ":\n");
	        report.write("*** Initial ShrikeBT code:\n");
			(new Disassembler(md)).disassembleTo(report);
			report.flush();
			
			// Store the method's meta data.  This ensures that the meta data
			// of all instrumented methods will be available to the replay tool.
			// (The replay tool builds on this assumption.)
			methodNameTable.update(md.getClassType(), md.getName(),
					md.getSignature(), md.getIsStatic(), md.getAccess());
			
			// Instrument the method
			addFieldAccessLogging(md, ci.getReader().getSuperName());
			
			if (isMainMethod(md) || isClassConstructor(md)) {
				addLogInitialization(md, logFileName);
			}
			
			if (isMainMethod(md)) {
				addLogFinalization(md);
			}

			report.write("\n*** Final ShrikeBT code:\n");
			(new Disassembler(md)).disassembleTo(report);
			report.write("\n");
			report.flush();
		}		

	    if (ci.isChanged()) {
			ClassWriter cw = ci.emitClass();
	        instrumenter.outputModifiedClass(ci, cw);
	    }
	}

	
	private static void addFieldAccessLogging(final MethodData md, final String superClassName) {
		MethodEditor me = new MethodEditor(md);
		me.beginPass();

		IInstruction[] instructions = md.getInstructions();

		// Method entry
		me.insertAtStart(makeEntryPatch(md));

		int firstInstruction = 0;
		
		// If the method is a constructor, only instrument instructions
		// that run after the object-initialization (through the super
		// constructors).  Using the object beforehand is illegal,
		// which prevents us from getting its hash (id).
		if (isConstructor(md)) {
			// The superClassName, somewhat erratically, lacks the pre- and postfix.
			String superTypeName = (superClassName != null) ? ("L" + superClassName + ";") : null;
			firstInstruction = superIndex(superTypeName, instructions) + 1;
		}

		// Get, put, return, and array access
		for (int i = firstInstruction; i < instructions.length; ++i) {
			if (instructions[i] instanceof ReturnInstruction) {
				me.insertBefore(i, makeReturnPatch());
			}
			else if (instructions[i] instanceof GetInstruction) {
				GetInstruction gi = (GetInstruction) instructions[i];
				// This patch _replaces_ the instruction.
				me.replaceWith(i, makeGetPatch(gi));
			}
			else if (instructions[i] instanceof PutInstruction) {
				PutInstruction pi = (PutInstruction) instructions[i];
				me.insertBefore(i, makePutPatch(pi));
			}
			else if (instructions[i] instanceof ArrayLoadInstruction) {
				ArrayLoadInstruction ali = (ArrayLoadInstruction) instructions[i];
				// For some obscure reason, replacing the load instruction
				// (with an appropriate patch) as in the Get case leads to
				// stack underflows.  Hence, load the value just for logging
				// and restore the stack at the end of the patch, so it can
				// be loaded again by the original instruction.
				me.insertBefore(i, makeALoadPatch(ali));
			}
			else if (instructions[i] instanceof ArrayStoreInstruction) {
				ArrayStoreInstruction asi = (ArrayStoreInstruction) instructions[i];
				me.insertBefore(i, makeAStorePatch(asi));
			} else if (instructions[i] instanceof MonitorInstruction) {
				MonitorInstruction mi = (MonitorInstruction) instructions[i];
				if (mi.isEnter()) {
					me.replaceWith(i, makeMonitorEntryPatch(mi, i));
				} else {
					me.insertBefore(i, makeMonitorExitPatch());
				}
			} else if (instructions[i] instanceof InvokeInstruction) {
				InvokeInstruction ii = (InvokeInstruction) instructions[i];
				// Calls to wait() release the respective lock and re-acquire
				// it before returning.  Because other threads may access the
				// shared objects (atomically) in the meantime, we split the
				// current synchronized block in two, putting the call to wait()
				// as the first statement in the second half.
				//
				// NOTE: If we started the second half _after_ the wait, then
				//       the surrounding scope would detect interleaved
				//       operations on the shared object if another thread
				//       accesses them.
				//
				// FIXME: This only detects the blocking wait(), nothing else.
				if (ii.getInvocationCode() == Dispatch.VIRTUAL &&
						ii.getClassType().equals(Constants.TYPE_Object) &&
						ii.getMethodName().equals("wait") &&
						ii.getMethodSignature().equals("()V")) {
					me.insertBefore(i, makeWaitPatch(i));
				}
			}
		}
		
		// Parameters
		// Note: This patch could be applied to the same place as a
		//       return patch.  In that case it is important that
		//       the value patch is executed first.  Consequently, it
		//       must be applied last (see insertBefore() documentation).
		me.insertBefore(firstInstruction, makeParametersPatch(md));

		// Return (via uncaught exceptions)
		me.addMethodExceptionHandler(null, makeExceptionPatch());
		
		me.applyPatches();
	}
	
	
	private static void addLogInitialization(final MethodData md, final String logFileName) throws IOException {
		report.write("*** Adding log initialization.\n");
		
		MethodEditor me = new MethodEditor(md);
		me.beginPass();

		me.insertAtStart(new MethodEditor.Patch() {
			@Override
			public void emitTo(MethodEditor.Output o) {
				o.emit(ConstantInstruction.makeString(logFileName));
				o.emit(callOpen);
			}
		});
		
		me.applyPatches();
	}

	
	private static void addLogFinalization(final MethodData md) throws IOException {
		report.write("*** Adding log finalization.\n");
		
		MethodEditor me = new MethodEditor(md);
		me.beginPass();
		
		IInstruction[] instructions = md.getInstructions();
		
		for (int i=0; i < instructions.length; ++i) {
			if (instructions[i] instanceof ReturnInstruction) {
				me.insertBefore(i, new MethodEditor.Patch() {
					@Override
					public void emitTo(MethodEditor.Output o) {
						o.emit(callClose);
					}
				});
			}
		}
		
		me.addMethodExceptionHandler(null, new MethodEditor.Patch() {
			@Override
			public void emitTo(MethodEditor.Output o) {
				o.emit(callClose);
				o.emit(ThrowInstruction.make(false));
			}
		});
		
		me.applyPatches();
	}

	
	
	/*- Patch Generation ----------------------------------------------------*/

	/**
	 * Return an instruction patch that, upon execution, adds an entry event for
	 * the given method to the global log. After the patch has executed, the
	 * stack will be in its original state.
	 * 
	 * @param md method whose name will be included in the logged entry event 
	 * @return patch that logs an entry event
	 */
	private static MethodEditor.Patch makeEntryPatch(final MethodData md) {
		return new MethodEditor.Patch() {
			@Override
			public void emitTo(MethodEditor.Output o) {
				int methodId = methodNameTable.idFor(md.getClassType(), md.getName(), md.getSignature());
				o.emit(ConstantInstruction.make(methodId));
				o.emit(callLogMethodEntry);
			}
		};
	}

	/**
	 * Return an instruction patch that, upon execution, records the indices
	 * and values of the given method's parameters in the global log.  Only
	 * parameters of non-primitive types will be logged.  After the patch has
	 * executed, the stack will be in its original state.
	 * <p>
	 * 
	 * The patch assumes that it is applied to the instructions of the given
	 * method.
	 * 
	 * @param md method whose parameters will be recorded 
	 * @return patch that logs the parameters of the given method
	 */
	private static MethodEditor.Patch makeParametersPatch(final MethodData md) {
		return new MethodEditor.Patch() {
			@Override
			public void emitTo(MethodEditor.Output o) {
				String thisClassType = md.getIsStatic() ? null : md.getClassType();
				String[] paramsTypes = Util.getParamsTypes(thisClassType, md.getSignature()); 
				for (int i=0; i < paramsTypes.length; ++i) {
					if (paramsTypes[i].replace("[", "").startsWith("L")) {
						o.emit(ConstantInstruction.make(i));
						o.emit(LoadInstruction.make(paramsTypes[i], i));
						o.emit(callLogParameter);
					}
				}
			}
		};
	}
	
	/**
	 * Return an instruction patch that, upon execution, records a method
	 * exit event in the global log.  The patch does not modify the stack.
	 * 
	 * @return patch that logs an exit event
	 */
	private static MethodEditor.Patch makeReturnPatch() {
		return new MethodEditor.Patch() {
			@Override
			public void emitTo(MethodEditor.Output o) {
				o.emit(callLogMethodExit);
			}
		};
	}

	/**
	 * Return an instruction patch that records a method exit event in the
	 * global log and re-throws the current exception. The patch is intended to
	 * be used in conjunction with a method-wide "catch-all" exception handler.
	 * 
	 * @return patch that logs an exit event and re-throws the current exception
	 */
	private static MethodEditor.Patch makeExceptionPatch() {
		return new MethodEditor.Patch() {
			@Override
			public void emitTo(MethodEditor.Output o) {
				o.emit(callLogMethodExit);
				o.emit(ThrowInstruction.make(false));
			}
		};
	}

	private static MethodEditor.Patch makeMonitorEntryPatch(final MonitorInstruction mi, final int instructionNumber) {
		return new MethodEditor.Patch() {
			@Override
			public void emitTo(MethodEditor.Output o) {
				o.emit(DupInstruction.make(0));
				o.emit(mi);
				o.emit(ConstantInstruction.make(instructionNumber));
				o.emit(callLogMonitorEntry);
			}
		};
	}
	
	private static MethodEditor.Patch makeMonitorExitPatch() {
		return new MethodEditor.Patch() {
			@Override
			public void emitTo(MethodEditor.Output o) {
				o.emit(callLogMonitorExit);
			}
		};
	}
	
	private static MethodEditor.Patch makeWaitPatch(final int instructionNumber) {
		return new MethodEditor.Patch() {
			@Override
			public void emitTo(MethodEditor.Output o) {
				o.emit(callLogMonitorExit);
				o.emit(ConstantInstruction.make(Constants.TYPE_null, null));
				o.emit(ConstantInstruction.make(instructionNumber));
				o.emit(callLogMonitorEntry);
			}
		};
	}
	
	/**
	 * Return a patch for logging the read-field event triggered by the given
	 * "get" instruction. The logged event will contain the field's id, as well
	 * as the id of the object that owns the field. For static fields, the owner
	 * id will be <code>null</code>. If the field read is of non-primitive type,
	 * the event will contain the read value.
	 * </p>
	 * 
	 * The patch consumes the reference to the field-owning object from the top
	 * of the stack (if the field is not static). The patch is meant to
	 * <em>replace</em> the given GetInstruction.
	 * 
	 * @param gi
	 *            read-field ("get") instruction to log
	 * @return patch that replaces the "get" instruction with one that also
	 *         records a read-field event in the global log
	 */
	private static MethodEditor.Patch makeGetPatch(final GetInstruction gi) {
		return new MethodEditor.Patch() {
			@Override
			public void emitTo(MethodEditor.Output o) {
				// See http://en.wikipedia.org/wiki/Java_bytecode_instruction_listings

				// Put the field owner on the stack
				if (gi.isStatic()) {
					o.emit(ConstantInstruction.make(null, null));
				} else {
					// The top of the stack is the object that contains the accessed field
					o.emit(DupInstruction.make(0));
				}

				// Retrieve the field's value.  This consumes all objects that were
				// present in the un-instrumented version of the get instruction.
				o.emit(gi);

				if (gi.getFieldType().replace("[", "").startsWith("L")) {
					// Duplicate the value if it is a reference and put it under the field owner
					o.emit(DupInstruction.make(1));
				} else {
					// Otherwise, just put it under the field owner
					o.emit(SwapInstruction.make());
				}

				// Put the field id on the stack
				o.emit(ConstantInstruction.make(fieldNameTable.idFor(gi.getClassType(), gi.getFieldName(), gi.getFieldType())));

				// Invoke the logging functions.  This consumes all extra objects from the stack.
				if (gi.getFieldType().replace("[", "").startsWith("L")) {
					o.emit(callLogReferenceGet);
				} else {
					o.emit(callLogPrimitiveGet);
				}
			}
		};
	}

	/**
	 * Return a patch for logging the write-field event triggered by the given
	 * "put" instruction. The logged event will contain the field's id, as well
	 * as the id of the object that owns the field. For static fields, the owner
	 * id will be <code>null</code>. If the field read is of non-primitive type,
	 * the event will contain the written value. After the patch has executed,
	 * the stack will be in its original state.
	 * 
	 * @param pi
	 *            write-field ("put") instruction to log
	 * @return patch that records a read-field event for the given instruction
	 *         in the global log
	 */
	private static MethodEditor.Patch makePutPatch(final PutInstruction pi) {
		return new MethodEditor.Patch() {
			@Override
			public void emitTo(MethodEditor.Output o) {
				// Put the field owner on the stack
				if (pi.isStatic()) {
					o.emit(ConstantInstruction.make(null, null));
				} else {
					// The top of the stack is the value that is written, the
					// object that contains the accessed field is second from the top.
					// Stack (top is right):               ... R V
					o.emit(SwapInstruction.make());		// ... V R
					o.emit(DupInstruction.make(1));		// ... R V R
				}

				if (pi.getFieldType().replace("[", "").startsWith("L")) {
					// Duplicate the value if it is a reference or an array.
					o.emit(SwapInstruction.make());		// ... R R V
					o.emit(DupInstruction.make(1));		// ... R V R V
				}
				
				// Put the field id on the stack and invoke the logging function.
				o.emit(ConstantInstruction.make(fieldNameTable.idFor(pi.getClassType(), pi.getFieldName(), pi.getFieldType())));

				if (pi.getFieldType().replace("[", "").startsWith("L")) {
					o.emit(callLogReferencePut);
				} else {
					o.emit(callLogPrimitivePut);
				}
			}
		};
	}

	/**
	 * Return a patch that records the read event triggered by the given array
	 * load instruction --- if the array elements are of a non-primitive type.
	 * The logged event will contain the array's id, the accessed index, as well
	 * as the read value. After the patch has executed, the stack will be in its
	 * original state.
	 * 
	 * @param ali
	 *            array read instruction to log
	 * @return patch that records a read-array event for the given instruction
	 *         if the array elements are of non-primitive type (or does nothing
	 *         if they <em>are</em> of primitive type)
	 */
	private static MethodEditor.Patch makeALoadPatch(final ArrayLoadInstruction ali) {
		return new MethodEditor.Patch() {
			@Override
			public void emitTo(MethodEditor.Output o) {
				if (ali.getType().startsWith("L")) {
					// The top of the stack is the index I with the array
					// reference A beneath.  The logging function wants
					// both A and I, as well as the actual value loaded V.

					// Stack after the instruction (top right):
					//                                     ... A I
					o.emit(SwapInstruction.make());		// ... I A
					o.emit(DupInstruction.make(1));		// ... A I A
					o.emit(SwapInstruction.make());		// ... A A I
					o.emit(DupInstruction.make(1));		// ... A I A I

					o.emit(SwapInstruction.make());		// ... A I I A
					o.emit(DupInstruction.make(1));		// ... A I A I A
					o.emit(SwapInstruction.make());		// ... A I A A I
					o.emit(DupInstruction.make(1));		// ... A I A I A I

					o.emit(ali);                        // ... A I A I V
					
					o.emit(SwapInstruction.make());     // ... A I A V I
					o.emit(callLogArrayLoad);           // ... A I
				}
			}
		};
	}

	/**
	 * Return a patch that records the write event triggered by the given array
	 * store instruction --- if the array elements are of a non-primitive type.
	 * The logged event will contain the array's id, the accessed index, as well
	 * as the written value. After the patch has executed, the stack will be in
	 * its original state.
	 * 
	 * @param ali
	 *            array store instruction to log
	 * @return patch that records a write-array event for the given instruction
	 *         if the array elements are of non-primitive type (or does nothing
	 *         if they <em>are</em> of primitive type)
	 */
	private static MethodEditor.Patch makeAStorePatch(final ArrayStoreInstruction asi) {
		return new MethodEditor.Patch() {
			@Override
			public void emitTo(MethodEditor.Output o) {
				if (asi.getType().startsWith("L")) {
					// The top of the stack is the value V to be stored,
					// with the index I and the array reference A beneath.
					// The logging function wants all three as arguments,
					// so all three have to be duplicated.
					
					// Stack after the instruction (top right):
					//                                     ... A I V
					o.emit(DupInstruction.make(2));		// ... V A I V
					o.emit(DupInstruction.make(2));		// ... V V A I V
					o.emit(PopInstruction.make(1));     // ... V V A I
					o.emit(DupInstruction.make(2));		// ... V I V A I
					o.emit(SwapInstruction.make());		// ... V I V I A
					o.emit(DupInstruction.make(2));		// ... V I A V I A
					o.emit(DupInstruction.make(2));		// ... V I A A V I A
					o.emit(PopInstruction.make(1));     // ... V I A A V I
					
					o.emit(callLogArrayStore);          // ... V I A

					o.emit(DupInstruction.make(2));		// ... A V I A
					o.emit(PopInstruction.make(1));     // ... A V I
					o.emit(SwapInstruction.make());		// ... A I V
				}
			}
		};
	}


	// Log management
	private final static Instruction callOpen =
			Util.makeInvoke(FieldAccessLog.class, "open", new Class[] { String.class });
	private final static Instruction callClose =
			Util.makeInvoke(FieldAccessLog.class, "close", new Class[] {});
	
	// Logging method entry and exit events
	private final static Instruction callLogMethodEntry =
			Util.makeInvoke(FieldAccessLog.class, "logMethodEntry", new Class[] { int.class });
	private final static Instruction callLogMethodExit =
			Util.makeInvoke(FieldAccessLog.class, "logMethodExit", new Class[] {});
	private final static Instruction callLogParameter =
			Util.makeInvoke(FieldAccessLog.class, "logParameter", new Class[] { byte.class, Object.class });
	private final static Instruction callLogMonitorEntry =
			Util.makeInvoke(FieldAccessLog.class, "logMonitorEntry", new Class[] { Object.class, int.class });
	private final static Instruction callLogMonitorExit =
			Util.makeInvoke(FieldAccessLog.class, "logMonitorExit", new Class[] {});
	
	// Logging read events
	private final static Instruction callLogPrimitiveGet =
			Util.makeInvoke(FieldAccessLog.class, "logPrimitiveGet", new Class[] { Object.class, int.class });
	private final static Instruction callLogReferenceGet =
			Util.makeInvoke(FieldAccessLog.class, "logReferenceGet", new Class[] { Object.class, Object.class, int.class });
	private final static Instruction callLogArrayLoad =
			Util.makeInvoke(FieldAccessLog.class, "logArrayLoad", new Class[] { Object.class, Object.class, int.class });

	// Logging write events
	private final static Instruction callLogPrimitivePut =
			Util.makeInvoke(FieldAccessLog.class, "logPrimitivePut", new Class[] { Object.class, int.class });
	private final static Instruction callLogReferencePut =
			Util.makeInvoke(FieldAccessLog.class, "logReferencePut", new Class[] { Object.class, Object.class, int.class });
	private final static Instruction callLogArrayStore =
			Util.makeInvoke(FieldAccessLog.class, "logArrayStore", new Class[] { Object.class, Object.class, int.class });
	

	
	/*- Auxiliary Functions -------------------------------------------------*/
	
	
	private static boolean isConstructor(final MethodData md) {
		return !md.getIsStatic() && md.getName().equals("<init>");
	}
	
	private static boolean isClassConstructor(final MethodData md) {
		return md.getIsStatic() && md.getName().equals("<clinit>");
	}
	
	private static boolean isMainMethod(final MethodData md) {
		return md.getIsStatic() && md.getName().equals("main")
				&& md.getSignature().equals("([Ljava/lang/String;)V");
	}
	

	/**
	 * Return the index of the (first) instruction that invokes the constructor
	 * of the given super class.
	 * 
	 * Passing <code>null</code> as <code>superTypeName</code> is equivalent to
	 * saying the super type is <code>java.lang.Object</code>.
	 * 
	 * @param superTypeName
	 *            type name of the super class in JVM format, for example
	 *            "Ljava/lang/Object;"
	 * @param instructions
	 *            instructions to scan for the constructor invocation
	 * @return index of the (first) super type constructor invocation
	 */
	private static int superIndex(String superTypeName, IInstruction[] instructions) {
		if (superTypeName == null) {
			superTypeName = Constants.TYPE_Object;
		}
		for (int i=0; i < instructions.length; ++i) {
			if (instructions[i] instanceof InvokeInstruction) {
				InvokeInstruction ii = (InvokeInstruction) instructions[i];

				// If this is an invocation of the super type constructor...
				if (ii.getInvocationMode() == Constants.OP_invokespecial &&
						ii.getMethodName().equals("<init>") &&
						ii.getClassType().equals(superTypeName)) {
					
					// ... and the receiver is "this"
					int receiverIndex = i - ii.getPoppedCount();
					// FIXME This requires loading "this" explicitly; it does not
					//       cover DupInstructions.
					if (receiverIndex >= 0 &&
							instructions[receiverIndex] instanceof LoadInstruction &&
							((LoadInstruction) instructions[receiverIndex]).getVarIndex() == 0) {
						return i;
					}
				}
			}
		}
		
		// TODO Is this a sane fall-through?
		System.err.println(">>> WARNING: Could not find super() invocation.");
		return instructions.length - 2;
	}
}
