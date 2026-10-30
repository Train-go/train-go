package com.voyagego.traingo.model.response;

import java.util.List;

import com.voyagego.traingo.entity.UserRole;

/**
 * What /admin/users shows: the first {@code rows.size()} of {@code total}
 * accounts of one role, newest first, and the size of each tab. Built by
 * UserAdminService.list.
 */
public record UserListView(UserRole role, String q, int limit, long total, long customerCount, long adminCount,
		List<UserRow> rows) {

	public boolean isFiltered() {
		return !q.isEmpty();
	}

	/** How many "Hiện thêm" would add. */
	public long more() {
		return Math.min(40, total - rows.size());
	}

	/** {@code bookingCount} counts cancelled bookings too, so it matches the Booking page. */
	public record UserRow(long id, String fullName, String initials, String email, String phoneLabel,
			String createdLabel, long bookingCount) {
	}

}
