package com.voyagego.traingo.service;

import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import com.voyagego.traingo.entity.User;
import com.voyagego.traingo.entity.UserRole;

/** Session principal detached from the persistence context. */
public final class TrainGoUserDetails implements UserDetails {
	private static final long serialVersionUID = 1L;
	private final Long userId;
	private final String fullName;
	private final String email;
	private final String phoneNumber;
	private final String passwordHash;
	private final UserRole role;

	public TrainGoUserDetails(User user) {
		this.userId = user.getId();
		this.fullName = user.getFullName();
		this.email = user.getEmail();
		this.phoneNumber = user.getPhoneNumber();
		this.passwordHash = user.getPasswordHash();
		this.role = user.getRole();
	}

	public Long getUserId() { return userId; }
	public String getFullName() { return fullName; }
	public String getEmail() { return email; }
	public String getPhoneNumber() { return phoneNumber; }
	public UserRole getRole() { return role; }

	@Override
	public String getUsername() { return email; }

	@Override
	public String getPassword() { return passwordHash; }

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
	}
}
