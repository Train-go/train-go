package com.voyagego.traingo.model.response;

/**
 * The four numbers of the admin overview (#37), each with the note printed
 * under it. They are read from the database, never cached.
 *
 * @param bookings          CONFIRMED bookings
 * @param cancelledBookings CANCELLED bookings, the note under "Booking"
 * @param trips             trips that are not cancelled
 * @param upcomingTrips     SCHEDULED trips that have not left, the note under "Chuyến"
 * @param customers         users with the CUSTOMER role
 * @param revenue           total_price of CONFIRMED bookings, in dong
 */
public record Overview(long bookings, long cancelledBookings, long trips, long upcomingTrips, long customers,
		long revenue) {
}
