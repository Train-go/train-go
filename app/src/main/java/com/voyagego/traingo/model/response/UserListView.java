package com.voyagego.traingo.model.response;

import java.util.List;

import com.voyagego.traingo.entity.UserRole;

/**
 * What /admin/users shows: the accounts of one role, newest first, and the
 * size of each tab. Built by UserAdminService.list.
 */
public record UserListView(UserRole role, long customerCount, long adminCount, List<UserRow> rows) {

	/** {@code bookingCount} counts cancelled bookings too, so it matches the Booking page. */
	public record UserRow(long id, String fullName, String email, String phoneNumber, String createdLabel,
			long bookingCount) {
	}

}
