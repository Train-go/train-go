package com.voyagego.traingo.service;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.voyagego.traingo.entity.User;
import com.voyagego.traingo.entity.UserRole;
import com.voyagego.traingo.model.dto.IdCount;
import com.voyagego.traingo.model.response.UserListView;
import com.voyagego.traingo.model.response.UserListView.UserRow;
import com.voyagego.traingo.repository.BookingRepository;
import com.voyagego.traingo.repository.UserRepository;

/**
 * The read-only user list of docs/product/admin.md: two tabs by role, a search,
 * newest account first in pages, each with its number of bookings. There is no create, edit
 * or delete.
 */
@Service
@Transactional(readOnly = true)
public class UserAdminService {

	public static final int PAGE = 40;
	private static final int MAX_SHOWN = 2000;
	private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

	private final UserRepository userRepository;
	private final BookingRepository bookingRepository;

	public UserAdminService(UserRepository userRepository, BookingRepository bookingRepository) {
		this.userRepository = userRepository;
		this.bookingRepository = bookingRepository;
	}

	/** {@code q} matches the name, email or phone number; {@code limit} grows with "Hiện thêm". */
	public UserListView list(UserRole role, String q, int limit) {
		String typed = q == null ? "" : q.strip();
		int shown = Math.max(PAGE, Math.min(limit, MAX_SHOWN));
		Map<Long, Long> bookingCounts = bookingRepository.countByCustomer().stream()
				.collect(Collectors.toMap(IdCount::getId, IdCount::getTotal));
		Page<User> page = userRepository.search(role, typed, PageRequest.of(0, shown));
		List<UserRow> rows = page.getContent().stream()
				.map(user -> new UserRow(user.getId(), user.getFullName(), initials(user.getFullName()), user.getEmail(),
						phoneLabel(user.getPhoneNumber()), user.getCreatedAt().format(DATE),
						bookingCounts.getOrDefault(user.getId(), 0L)))
				.toList();
		return new UserListView(role, typed, shown, page.getTotalElements(), userRepository.countByRole(UserRole.CUSTOMER),
				userRepository.countByRole(UserRole.ADMIN), rows);
	}

	/** The first letters of the last two words: "Nguyễn Văn An" is VA. */
	private static String initials(String name) {
		String[] words = name.strip().split("\\s+");
		StringBuilder letters = new StringBuilder();
		for (int i = Math.max(0, words.length - 2); i < words.length; i++) {
			letters.append(words[i].charAt(0));
		}
		return letters.toString().toUpperCase(Locale.ROOT);
	}

	/** "0901234567" becomes "0901 234 567"; null stays null. */
	private static String phoneLabel(String phone) {
		return phone == null ? null : phone.replaceFirst("^(\\d{4})(\\d{3})(\\d+)$", "$1 $2 $3");
	}

}
