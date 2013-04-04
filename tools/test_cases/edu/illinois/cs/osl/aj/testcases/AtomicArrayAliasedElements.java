package edu.illinois.cs.osl.aj.testcases;

import edu.illinois.cs.osl.aj.testcases.common.IteratingThread;
import edu.illinois.cs.osl.aj.testcases.common.Person;

/**
 * A test program that atomically accesses the elements of an array (which
 * leads to aliasing between the atomic sets of the elements and the array's
 * atomic set).
 * 
 * Expected Atomic Sets
 * ====================
 * PersonDB: E = {entries={entries[]=N}}
 * Person: N = {firstName}
 * 
 * @author Peter Dinges <pdinges@acm.org>
 */
public class AtomicArrayAliasedElements {

	private static class PersonDB {
		private Person[] entries;
		
		public PersonDB(Person[] entries) {
			this.entries = entries;
		}
		
		synchronized public void updateNamesAt(int index, String fn, String ln) {
			entries[index].setFirstName(fn);
		}
	}
	
	private static class WorkerThread extends IteratingThread {
		private PersonDB database;
		
		public WorkerThread(PersonDB db) {
			this.database = db;
		}
		
		protected void doWork(int iteration) {
			long tId = Thread.currentThread().getId();
			
			database.updateNamesAt(0, "Foo" + tId, "Bar" + tId);
			database.updateNamesAt(1, "Baz" + tId, "Boo" + tId);
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
