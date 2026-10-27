package com.voyagego.traingo.service;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.voyagego.traingo.model.dto.BookingDraft;

import jakarta.servlet.http.HttpSession;

/** Reads and writes the booking draft in the {@code bookingDraft} session attribute. */
@Component
public class BookingDraftStore {

	static final String ATTRIBUTE = "bookingDraft";

	public Optional<BookingDraft> get(HttpSession session) {
		return session.getAttribute(ATTRIBUTE) instanceof BookingDraft draft ? Optional.of(draft) : Optional.empty();
	}

	public void save(HttpSession session, BookingDraft draft) {
		session.setAttribute(ATTRIBUTE, draft);
	}

	public void clear(HttpSession session) {
		session.removeAttribute(ATTRIBUTE);
	}

}
