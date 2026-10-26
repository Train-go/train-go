/*
 * The Booking page (templates/admin/bookings.html): the search box that
 * submits itself, and the detail sheet. A row opens the sheet through
 * /api/admin/bookings/{code} and writes ?open=BK... into the address, so the
 * link of an open booking can be sent to someone; closing the sheet removes
 * it. A page loaded with ?open= opens that booking at once.
 */
(function () {
	'use strict';

	const SEARCH_DELAY_MS = 300;

	/* ---------- search: submits 300ms after the last keystroke ---------- */

	const search = document.querySelector('[data-booking-search]');
	if (search) {
		const input = search.elements.namedItem('q');
		let timer = null;
		function submitSoon() {
			clearTimeout(timer);
			timer = setTimeout(function () {
				search.requestSubmit();
			}, SEARCH_DELAY_MS);
		}
		// A Telex or VNI word is not finished until composition ends; submitting
		// half of it would reload the page and lose it.
		input.addEventListener('input', function (event) {
			if (!event.isComposing) {
				submitSoon();
			}
		});
		input.addEventListener('compositionend', submitSoon);
		// The page reloads on every search, so give the caret back to the box.
		if (new URLSearchParams(location.search).get('q')) {
			input.focus();
			input.setSelectionRange(input.value.length, input.value.length);
		}
	}

	/* ---------- detail sheet ---------- */

	const sheet = document.getElementById('booking-sheet');
	if (!sheet) {
		return;
	}
	const apiUrl = sheet.dataset.apiUrl;
	const money = new Intl.NumberFormat('vi-VN');

	function field(name) {
		return sheet.querySelector('[data-field="' + name + '"]');
	}

	function cell(text, className) {
		const td = document.createElement('td');
		td.textContent = text;
		if (className) {
			td.className = className;
		}
		return td;
	}

	// A passenger's name with the masked identity number under it.
	function passengerCell(ticket) {
		const td = cell(ticket.fullName);
		const sub = document.createElement('span');
		sub.className = 'sub mono';
		sub.textContent = ticket.idMask;
		td.appendChild(sub);
		return td;
	}

	function fill(booking) {
		document.getElementById('booking-sheet-title').textContent = booking.code;
		const status = field('status');
		status.textContent = booking.statusLabel;
		status.className = 'tag ' + booking.statusCss;
		field('customer').textContent = booking.customerName + ' · ' + booking.customerEmail
				+ (booking.customerPhone ? ' · ' + booking.customerPhone : '');
		field('trip').textContent = booking.trainCode + ' · ' + booking.routeLabel;
		field('departure').textContent = booking.departureLabel;
		field('payment').textContent = money.format(booking.totalPrice) + 'đ'
				+ (booking.paidLabel ? ' · ' + booking.paidLabel : '');

		const body = field('tickets');
		body.replaceChildren();
		booking.tickets.forEach(function (ticket) {
			const tr = document.createElement('tr');
			tr.appendChild(cell(ticket.ticketCode, 'mono'));
			tr.appendChild(passengerCell(ticket));
			tr.appendChild(cell(ticket.seatLabel));
			tr.appendChild(cell(money.format(ticket.price) + 'đ', 'num'));
			body.appendChild(tr);
		});
	}

	// Keeps the address in step with the sheet without adding history entries.
	function setOpenParam(code) {
		const url = new URL(location.href);
		if (code) {
			url.searchParams.set('open', code);
		} else {
			url.searchParams.delete('open');
		}
		history.replaceState(null, '', url);
	}

	function openBooking(code, trigger) {
		TG.api(apiUrl + '/' + encodeURIComponent(code)).then(function (booking) {
			fill(booking);
			setOpenParam(booking.code);
			TG.sheet.open('booking-sheet', trigger);
		}).catch(function (error) {
			// An unknown code in ?open= must not leave itself in the address.
			setOpenParam(null);
			TG.toast(error.message, { error: true });
		});
	}

	sheet.addEventListener('hidden.bs.offcanvas', function () {
		setOpenParam(null);
	});

	// The code is a link, so Tab, Enter and screen readers work; a click anywhere
	// on the row opens the sheet in place instead of following the link.
	document.querySelectorAll('[data-booking-row]').forEach(function (row) {
		row.addEventListener('click', function (event) {
			event.preventDefault();
			openBooking(row.dataset.code, event.target.closest('a') || row);
		});
	});

	if (sheet.dataset.open) {
		openBooking(sheet.dataset.open, null);
	}
}());
