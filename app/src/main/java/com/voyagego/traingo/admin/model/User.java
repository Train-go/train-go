package com.voyagego.traingo.admin.model;

public class User {

	private final String name;
	private final String email;
	private final String role;
	private final String createdAt;

	public User(String name, String email, String role, String createdAt) {
		this.name = name;
		this.email = email;
		this.role = role;
		this.createdAt = createdAt;
	}

	public String getName() {
		return name;
	}

	public String getEmail() {
		return email;
	}

	public String getRole() {
		return role;
	}

	public String getCreatedAt() {
		return createdAt;
	}

}
