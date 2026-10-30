/*
 * The Booking page (templates/admin/bookings.html): the search box that
 * submits itself, and the detail sheet. A row opens the sheet through
 * /api/admin/bookings/{code} and writes ?open=BK... into the address, so the
 * link of an open booking can be sent to someone; closing the sheet removes
 * it. A page loaded with ?open= opens that booking at once. The sheet's foot
 * cancels the booking, and its trip button opens the trip sheet.
 */
(function () {
	'use strict';

	const SEARCH_DELAY_MS = 300;

	/* ---------- search: submits 300ms after the last keystroke ---------- */

	const search = document.querySelector('[data-booking-search]');
	if (search) {
		const input = search.elements.namedItem('q');
		const customer = search.elements.namedItem('customer');
		let timer = null;
		function submitSoon() {
			if (customer) {
				customer.remove();
			}
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
	const CANCEL_NOTE = 'Hủy booking sẽ trả các chỗ về trạng thái còn trống.';
	// The booking on show.
	let current = null;
	// What had focus when the sheet opened, so a declined cancel can give it back.
	let opener = null;

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
		current = booking;
		const trip = field('trip');
		trip.textContent = booking.trainCode + ' · ' + booking.routeLabel;
		trip.dataset.tripId = booking.tripId;
		field('departure').textContent = booking.departureLabel;
		field('payment').textContent = money.format(booking.totalPrice) + 'đ'
				+ (booking.paidLabel ? ' · ' + booking.paidLabel : '');

		// A cancelled booking has nothing left to do; otherwise the reason it cannot be
		// cancelled replaces the usual note and disables the button.
		field('footer').hidden = booking.cancelled;
		field('cancel-note').textContent = booking.cancelBlockReason || CANCEL_NOTE;
		field('cancel').disabled = Boolean(booking.cancelBlockReason);

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
			opener = trigger;
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

	/*
	 * "Hủy booking": the sheet closes first, because a modal opened over an open
	 * offcanvas fights it for focus, then TG.confirm asks. Declining reopens the
	 * booking. A cancelled booking reloads the page, so the lists and counts
	 * follow, with the toast carried across the reload.
	 */
	field('cancel').addEventListener('click', function (event) {
		const booking = current;
		// One press only; fill() enables the button again when the sheet reopens.
		event.currentTarget.disabled = true;
		const trigger = opener;
		sheet.addEventListener('hidden.bs.offcanvas', async function () {
			const yes = await TG.confirm({
				title: 'Hủy booking ' + booking.code + '?',
				body: booking.tickets.length + ' chỗ trên chuyến ' + booking.trainCode + ' ' + booking.departureLabel
						+ ' sẽ mở bán lại. Thanh toán chỉ là mô phỏng nên không có hoàn tiền.',
				confirmLabel: 'Hủy booking',
				danger: true
			});
			if (!yes) {
				openBooking(booking.code, trigger);
				return;
			}
			try {
				const result = await TG.api(apiUrl + '/' + encodeURIComponent(booking.code) + '/cancel',
						{ method: 'POST' });
				TG.toastAfterReload('Đã hủy ' + booking.code + '. ' + result.releasedSeats + ' chỗ đã mở bán lại.');
				location.reload();
			} catch (error) {
				TG.toast(error.message, { error: true });
				openBooking(booking.code, trigger);
			}
		}, { once: true });
		TG.sheet.close('booking-sheet');
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
