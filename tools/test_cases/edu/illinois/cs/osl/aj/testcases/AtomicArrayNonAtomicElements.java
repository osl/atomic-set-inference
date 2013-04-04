package edu.illinois.cs.osl.aj.testcases;

import edu.illinois.cs.osl.aj.testcases.common.IteratingThread;
import edu.illinois.cs.osl.aj.testcases.common.Person;

/**
 * A test program that atomically accesses the reference to an array, while
 * updating its content in an interleaved manner.
 * 
 * Expected Atomic Sets
 * ====================
 * PersonDB: E = {entries}, !lock
 * 
 * @author Peter Dinges <pdinges@acm.org>
 */
public class AtomicArrayNonAtomicElements {
	
	private static class PersonDB {
		private Person[] entries;
		private Object lock;
		
		public PersonDB(Person[] entries) {
			this.entries = entries;
			this.lock = new Object();
		}

		public void swapEntries() {
			Person tmp = entries[0];
			entries[0] = entries[1];
			entries[1] = tmp;
		}
		
		public Person[] getEntries() {
			synchronized (lock) {
				// Access the array elements to generate atomic set suggestions.
				// Atomic swap...
				swapEntries();
				return entries;
			}
		}
	}
	
	private static class WorkerThread extends IteratingThread {
		private PersonDB database;
		
		public WorkerThread(PersonDB db) {
			super(1000);
			this.database = db;
		}
		
		protected void doWork(int iteration) {
			Person[] entries = database.getEntries();
			// Racy swap...
			Person tmp = entries[0];
			entries[0] = entries[1];
			entries[1] = tmp;
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
