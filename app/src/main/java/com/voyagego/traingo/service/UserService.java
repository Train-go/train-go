package com.voyagego.traingo.service;

import java.time.LocalDateTime;
import java.util.Locale;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import com.voyagego.traingo.common.exception.DuplicateEmailException;
import com.voyagego.traingo.entity.User;
import com.voyagego.traingo.model.request.ProfileRequest;
import com.voyagego.traingo.model.request.RegisterRequest;
import com.voyagego.traingo.repository.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

@Service
@Validated
public class UserService {
	private final UserRepository users;
	private final PasswordEncoder passwordEncoder;

	public UserService(UserRepository users, PasswordEncoder passwordEncoder) {
		this.users = users;
		this.passwordEncoder = passwordEncoder;
	}

	@Transactional
	public User register(@NotNull @Valid RegisterRequest form) {
		String email = form.getEmail().strip().toLowerCase(Locale.ROOT);
		if (users.existsByEmailIgnoreCase(email)) {
			throw new DuplicateEmailException();
		}
		var customer = User.newCustomer(form.getFullName(), email, form.getPhoneNumber(),
			passwordEncoder.encode(form.getPassword()), LocalDateTime.now());
		try {
			// Flush inside the boundary so a concurrent unique collision is translated
			// before the transaction proxy returns to the page controller.
			return users.saveAndFlush(customer);
		} catch (DataIntegrityViolationException exception) {
			for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
				if (cause instanceof ConstraintViolationException violation
						&& violation.getSQLState() != null && violation.getSQLState().equals("23000")
						&& violation.getErrorCode() == 1062
						&& isEmailConstraint(violation.getConstraintName())) {
					throw new DuplicateEmailException();
				}
			}
			throw exception;
		}
	}

	@Transactional
	public User updateProfile(long userId, @NotNull @Valid ProfileRequest form) {
		User user = users.findById(userId).orElseThrow();
		user.updateProfile(form.getFullName(), form.getPhoneNumber());
		return user;
	}

	private boolean isEmailConstraint(String name) {
		return name != null && (name.equals("uk_users_email") || name.endsWith(".uk_users_email"));
	}
}
