package edu.illinois.cs.osl.aj;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

public class MethodNameTable {
	
	private class TableEntry {
		String scopeName;
		String methodName;
		String signature;
		boolean isStatic;
		int accessFlags;
		int id;
		
		TableEntry(final String scopeName, final String methodName,
				final String signature, boolean isStatic, int accessFlags,
				int id) {
			this.scopeName = scopeName;
			this.methodName = methodName;
			this.signature = signature;
			this.isStatic = isStatic;
			this.accessFlags = accessFlags;
			this.id = id;
		}
	}
	
	private Map<String, TableEntry> methodNames = new HashMap<String, TableEntry>();
	
	public void update(final String scopeName, final String methodName,
				final String signature, boolean isStatic, int accessFlags) {
		String k = key(scopeName, methodName, signature);
		if (methodNames.containsKey(k)) {
			TableEntry te = methodNames.get(k);
			te.isStatic = isStatic;
			te.accessFlags = accessFlags;
		} else {
			int id = k.hashCode();
			methodNames.put(k, new TableEntry(scopeName, methodName, signature, isStatic, accessFlags, id));
		}
	}
	
	public int idFor(final String scopeName, final String methodName, final String signature) {
		String k = key(scopeName, methodName, signature);
		if (methodNames.containsKey(k)) {
			return methodNames.get(k).id;
		} else {
			int id = k.hashCode();
			methodNames.put(k, new TableEntry(scopeName, methodName, signature, false, 0, id));
			return id;
		}
	}
	
	private static String key(final String scopeName, final String fieldName, final String typeName) {
		return scopeName + "|" + fieldName + "|" + typeName;
	}

	
	public String toString() {
		StringWriter result = new StringWriter();
		
		for (Map.Entry<String, TableEntry> e: methodNames.entrySet()) {
			TableEntry te = e.getValue();
			result.write(te.id + "|");
			result.write(te.scopeName + "|");
			result.write(te.methodName + "|");
			result.write(te.signature + "|");
			result.write(te.isStatic + "|");
			result.write(te.accessFlags + "\n");
		}
		
		return result.toString();
	}
}
