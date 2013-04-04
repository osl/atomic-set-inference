package edu.illinois.cs.osl.aj.testcases.common;

public class Address {

	private String street;
	private int houseNumber;
	private String city;
	private int zip;
	
	public Address(String street, int houseNumber, String city, int zip) {
		this.street = street;
		this.houseNumber = houseNumber; 
		this.city = city;
		this.zip = zip;
	}
	
	public String getStreet() {
		return street;
	}
	
	public void setStreet(String street) {
		this.street = street;
	}

	public int getHouseNumber() {
		return houseNumber;
	}

	public void setHouseNumber(int houseNumber) {
		this.houseNumber = houseNumber;
	}

	public String getCity() {
		return city;
	}

	public void setCity(String city) {
		this.city = city;
	}

	public int getZip() {
		return zip;
	}

	public void setZip(int zip) {
		this.zip = zip;
	}
		
}
