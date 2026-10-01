package com.voyagego.traingo.auth.model;

import java.time.LocalDateTime;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * A customer or admin account (SPEC.md sections 40 and 41). Admin accounts
 * come from seed data only; customers will register themselves once the auth
 * feature lands.
 */
@Entity
@Table(name = "users", uniqueConstraints = @UniqueConstraint(name = "uk_users_email", columnNames = "email"))
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 100)
	private String fullName;

	@Column(nullable = false)
	private String email;

	/** BCrypt hash, never the password itself. */
	@Column(nullable = false, length = 100)
	private String passwordHash;

	// Stored as VARCHAR, not as a MySQL ENUM column. Without @JdbcTypeCode,
	// Hibernate expects ENUM on MySQL and schema validation rejects the table.
	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.VARCHAR)
	@Column(nullable = false, length = 20)
	private UserRole role;

	@Column(nullable = false)
	private LocalDateTime createdAt;

	/** Required by JPA. */
	protected User() {
	}

	public User(String fullName, String email, String passwordHash, UserRole role) {
		this.fullName = fullName;
		this.email = email;
		this.passwordHash = passwordHash;
		this.role = role;
		this.createdAt = LocalDateTime.now();
	}

	public Long getId() {
		return id;
	}

	public String getFullName() {
		return fullName;
	}

	public String getEmail() {
		return email;
	}

	public String getPasswordHash() {
		return passwordHash;
	}

	public UserRole getRole() {
		return role;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

}
