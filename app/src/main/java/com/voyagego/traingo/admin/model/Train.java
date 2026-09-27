package com.voyagego.traingo.admin.model;

public class Train {

	private final String code;
	private final String name;
	private final int coaches;
	private final int seats;

	public Train(String code, String name, int coaches, int seats) {
		this.code = code;
		this.name = name;
		this.coaches = coaches;
		this.seats = seats;
	}

	public String getCode() {
		return code;
	}

	public String getName() {
		return name;
	}

	public int getCoaches() {
		return coaches;
	}

	public int getSeats() {
		return seats;
	}

}
