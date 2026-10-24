package com.voyagego.traingo.service;

import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.voyagego.traingo.repository.UserRepository;

@Service
public class TrainGoUserDetailsService implements UserDetailsService {
	private final UserRepository users;

	public TrainGoUserDetailsService(UserRepository users) {
		this.users = users;
	}

	@Override
	@Transactional(readOnly = true)
	public TrainGoUserDetails loadUserByUsername(String email) {
		return users.findByEmailIgnoreCase(email)
			.map(TrainGoUserDetails::new)
			.orElseThrow(() -> new UsernameNotFoundException("Invalid credentials"));
	}
}
