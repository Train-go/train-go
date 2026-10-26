package com.voyagego.traingo.service;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.voyagego.traingo.entity.UserRole;
import com.voyagego.traingo.model.dto.IdCount;
import com.voyagego.traingo.model.response.UserListView;
import com.voyagego.traingo.model.response.UserListView.UserRow;
import com.voyagego.traingo.repository.BookingRepository;
import com.voyagego.traingo.repository.UserRepository;

/**
 * The read-only user list of docs/product/admin.md: two tabs by role, newest
 * account first, each with its number of bookings. There is no create, edit
 * or delete.
 */
@Service
@Transactional(readOnly = true)
public class UserAdminService {

	private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

	private final UserRepository userRepository;
	private final BookingRepository bookingRepository;

	public UserAdminService(UserRepository userRepository, BookingRepository bookingRepository) {
		this.userRepository = userRepository;
		this.bookingRepository = bookingRepository;
	}

	public UserListView list(UserRole role) {
		Map<Long, Long> bookingCounts = bookingRepository.countByCustomer().stream()
				.collect(Collectors.toMap(IdCount::getId, IdCount::getTotal));
		List<UserRow> rows = userRepository.findByRoleOrderByCreatedAtDesc(role).stream()
				.map(user -> new UserRow(user.getId(), user.getFullName(), user.getEmail(), user.getPhoneNumber(),
						user.getCreatedAt().format(DATE), bookingCounts.getOrDefault(user.getId(), 0L)))
				.toList();
		return new UserListView(role, userRepository.countByRole(UserRole.CUSTOMER),
				userRepository.countByRole(UserRole.ADMIN), rows);
	}

}
