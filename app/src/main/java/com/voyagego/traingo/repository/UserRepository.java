package com.voyagego.traingo.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.voyagego.traingo.entity.User;
import com.voyagego.traingo.entity.UserRole;

public interface UserRepository extends JpaRepository<User, Long> {

	Optional<User> findByEmailIgnoreCase(String email);

	boolean existsByEmailIgnoreCase(String email);

	/** Newest account first, for the admin's user list. */
	List<User> findByRoleOrderByCreatedAtDesc(UserRole role);

	long countByRole(UserRole role);

}
