package edu.illinois.cs.osl.aj.testcases.common;

public class Person {

	private String firstName;
	private String lastName;
	
	public Person(String firstName, String lastName) {
		this.firstName = firstName;
		this.lastName = lastName;
	}
	
	public void setFirstName(String name) {
		this.firstName = name;
	}
	
	public String getFirstName() {
		return this.firstName;
	}

	public void setLastName(String name) {
		this.lastName = name;
	}
	
	public String getLastName() {
		return this.lastName;
	}

}
