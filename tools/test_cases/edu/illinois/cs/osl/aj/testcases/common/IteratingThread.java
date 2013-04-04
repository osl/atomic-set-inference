package edu.illinois.cs.osl.aj.testcases.common;

public abstract class IteratingThread extends Thread {

	private int iterations;
	
	public IteratingThread() {
		this.iterations = 100;
	}
	
	public IteratingThread(int iterations) {
		this.iterations = iterations;
	}
	
	public void run() {
		for (int i=0; i < this.iterations; ++i) {
			doWork(i);
		}
	}
	
	protected abstract void doWork(int i);
}
