package com.voyagego.traingo.model.response;

public class AdminTrain {

	private final String code;
	private final String name;

	public AdminTrain(String code, String name) {
		this.code = code;
		this.name = name;
	}

	public String getCode() {
		return code;
	}

	public String getName() {
		return name;
	}

}
