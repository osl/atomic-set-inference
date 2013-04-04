package edu.illinois.cs.osl.aj.testcases;

import edu.illinois.cs.osl.aj.testcases.common.IteratingThread;
import edu.illinois.cs.osl.aj.testcases.common.Person;

/**
 * A test program that atomically accesses an array (both: the reference to the
 * array and its contents).
 * 
 * Expected Atomic Sets
 * ====================
 * PersonDB: E = {entries={entries[]}}
 * 
 * @author Peter Dinges <pdinges@acm.org>
 */
public class AtomicArrayAtomicElements {
	
	private static class PersonDB {
		private Person[] entries;
		
		public PersonDB(Person[] entries) {
			this.entries = entries;
		}
		
		synchronized public void swapEntries() {
			Person tmp = entries[0];
			entries[0] = entries[1];
			entries[1] = tmp;
		}
	}
	
	private static class WorkerThread extends IteratingThread {
		private PersonDB database;
		
		public WorkerThread(PersonDB db) {
			this.database = db;
		}
		
		protected void doWork(int iteration) {
			// Atomic swap...
			database.swapEntries();
		}
	}

	
	public static void main(String[] args) {
		Person p1 = new Person("Foo", "Bar");
		Person p2 = new Person("Baz", "Boo");
		PersonDB db = new PersonDB(new Person[] {p1, p2});
		
		WorkerThread threadA = new WorkerThread(db);
		WorkerThread threadB = new WorkerThread(db);
		
		threadA.start();
		threadB.start();
	}
	
}
