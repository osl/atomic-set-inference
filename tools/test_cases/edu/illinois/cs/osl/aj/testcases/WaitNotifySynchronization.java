package edu.illinois.cs.osl.aj.testcases;

import edu.illinois.cs.osl.aj.testcases.common.IteratingThread;
import edu.illinois.cs.osl.aj.testcases.common.Person;

/**
 * A test program that uses wait()/notify() synchronization to generates a
 * field access pattern of a single atomic set.
 * 
 * Expected Atomic Sets
 * ====================
 * Person: N = {firstName, lastName}
 * 
 * @author Peter Dinges <pdinges@acm.org>
 */
public class WaitNotifySynchronization {

	private static MyPerson sharedPerson;
	private static MyPerson lock;

	// Subclass Person so we can define an internal unit of work.
	private static class MyPerson extends Person {
		public MyPerson(String firstName, String lastName) {
			super(firstName, lastName);
		}
		
		// Access to firstName and lastName is always synchronized.
		public void setNames(String fn, String ln) {
			synchronized(lock) {
				try {
					this.setFirstName(fn);
					this.setLastName(ln);
					// Calling wait() releases the lock and re-acquires it
					// before the call returns.  Thus, the access to firstName
					// and lastName is still synchronized.
					lock.wait();
					this.setFirstName(fn);
					this.setLastName(ln);
				}
				catch (InterruptedException e) {}
			}
		}
	}
	
	private static class WorkerThread extends IteratingThread {
		public void doWork(int i) {
			long tId = Thread.currentThread().getId();
			sharedPerson.setNames("Foo" + tId, "Bar" + tId);
		}
	}
	
	private static class NotifyThread extends Thread {
		public void run() {
			// Continuously wake up the worker threads.
			while(true) {
				synchronized(lock) {
					lock.notifyAll();
				}
				try {
					sleep(10);
				} catch (InterruptedException e) {}
			}
		}
	}
	
	public static void main(String[] args) {
		sharedPerson = new MyPerson("Foo", "Bar");
		lock = new MyPerson("goldi", "locks");
		
		WorkerThread threadA = new WorkerThread();
		WorkerThread threadB = new WorkerThread();
		NotifyThread threadN = new NotifyThread();

		threadA.start();
		threadB.start();
		
		// The NotifyThread contains an endless loop and should not prevent
		// the program from exiting (when the two WorkerThreads are done).
		threadN.setDaemon(true);
		threadN.start();
	}
}
