package edu.illinois.cs.osl.aj.testcases;

import edu.illinois.cs.osl.aj.testcases.common.IteratingThread;
import edu.illinois.cs.osl.aj.testcases.common.Person;

/**
 * A test program with a method that performs an atomic action on one
 * (not both) its input parameters, making the method an external
 * unit of work for this parameter, but not the other.
 * 
 * Expected Atomic Sets
 * ====================
 * Person: N = {firstName}
 *
 * Expected External Units of Work
 * ===============================
 * observe: p2=N
 * swapIdentities: p1=N, p2=--
 * 
 * @author Peter Dinges <pdinges@acm.org>
 */
public class PartialExternalUnitOfWork {

	private static class SecretService {
		public static void swapIdentities(Person p1, Person p2) {
			String fn1;
			String fn2;
			// This method is an external unit of work only for p1.
			// (See the lock in WorkerThread.doWork())

			// Access to p2 can be (coarsely) interleaved between
			// both worker threads.  Thus this method should not be
			// a unit of work for p2.
			synchronized(p2) {
				fn2 = p2.getFirstName();
			}
			fn1 = p1.getFirstName();
			p1.setFirstName(fn2);
			
			synchronized(p2) {
				p2.setFirstName(fn1);
			}
		}
		
		public static void observe(Person p2) {
			synchronized(p2) {
				p2.setFirstName(p2.getFirstName());
			}
		}
	}
	
	private static class WorkerThread extends IteratingThread {
		Person p1;
		Person p2;
		
		public WorkerThread(Person p1, Person p2) {
			// NOTE: This test case needs potentially many more iterations.
			//       10000 seems to work.
			super(1000);
			this.p1 = p1;
			this.p2 = p2;
		}
		
		protected void doWork(int iteration) {
			SecretService.observe(p2);
			Thread.yield();
			synchronized(p1) {
				// swapIdentities is an external unit of work for p1.
				SecretService.swapIdentities(p1, p2);
			}
			Thread.yield();
			SecretService.observe(p2);
		}
	}

	
	public static void main(String[] args) {
		Person p1 = new Person("Foo", "Bar");
		Person p2 = new Person("Baz", "Boo");
		
		WorkerThread threadA = new WorkerThread(p1, p2);
		WorkerThread threadB = new WorkerThread(p1, p2);
		
		threadA.start();
		threadB.start();
	}
	
}
