package com.voyagego.traingo.admin.model;

public class Station {

	private final String code;
	private final String name;
	private final String city;
	private final boolean active;

	public Station(String code, String name, String city, boolean active) {
		this.code = code;
		this.name = name;
		this.city = city;
		this.active = active;
	}

	public String getCode() {
		return code;
	}

	public String getName() {
		return name;
	}

	public String getCity() {
		return city;
	}

	public boolean isActive() {
		return active;
	}

}
