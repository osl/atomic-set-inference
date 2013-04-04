package edu.illinois.cs.osl.aj;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

public class FieldNameTable {
	
	private class TableEntry {
		String scopeName;
		String fieldName;
		String typeName;
		int id;
		
		TableEntry(final String scopeName, final String fieldName, final String typeName, int id) {
			this.scopeName = scopeName;
			this.fieldName = fieldName;
			this.typeName = typeName;
			this.id = id;
		}
	}
	
	private Map<String, TableEntry> fieldNames = new HashMap<String, TableEntry>();
	
	public int idFor(final String scopeName, final String fieldName, final String typeName) {
		String k = key(scopeName, fieldName, typeName);
		if (fieldNames.containsKey(k)) {
			return fieldNames.get(k).id;
		} else {
			int id = k.hashCode();
			fieldNames.put(k, new TableEntry(scopeName, fieldName, typeName, id));
			return id;
		}
	}
	
	private static String key(final String scopeName, final String fieldName, final String typeName) {
		return scopeName + "|" + fieldName + "|" + typeName;
	}

	
	public String toString() {
		StringWriter result = new StringWriter();
		
		for (Map.Entry<String, TableEntry> e: fieldNames.entrySet()) {
			TableEntry te = e.getValue();
			result.write(te.id + "|" + te.scopeName + "|" + te.fieldName + "|" + te.typeName + "\n");
		}
		
		return result.toString();
	}
}
