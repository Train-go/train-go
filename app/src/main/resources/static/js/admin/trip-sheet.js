/*
 * The trip sheet (templates/admin/fragments/trip-sheet.html), shared by the
 * trains, trips and bookings pages:
 *
 *   TG.openTrip(tripId, triggerEl)   load GET /api/admin/trips/{id}, fill and open
 *
 * A page needs no script of its own to use it: any element with
 * data-trip-id and tabindex="0" opens the sheet on click or Enter, and
 * ?trip=<id> in the URL opens it on load.
 *
 * After a cancel the sheet fires a cancelable
 *
 *   document 'trip:cancelled'  detail { tripId, message }
 *
 * By default the page then reloads with ?trip=<id>, so its counts and lists
 * come from the server and the sheet shows the cancelled trip again. A page
 * that refreshes itself calls event.preventDefault() and shows the toast.
 *
 * Loaded with defer, after admin.js. Text from the server is placed with
 * textContent only: passenger names are typed in by customers.
 */
(function () {
	'use strict';

	const SHEET_ID = 'trip-sheet';
	const OPEN_PARAM = 'trip';
	const sheetEl = document.getElementById(SHEET_ID);
	if (!sheetEl) {
		return;
	}
	const rowTemplate = document.getElementById('trip-sheet-passenger');
	const cancelButton = slot('cancel');

	let trip = null;
	let coachNumber = null;
	let pickedSeat = null;

	function slot(name) {
		return sheetEl.querySelector('[data-trip="' + name + '"]');
	}

	function money(amount) {
		return amount.toLocaleString('vi-VN') + 'đ';
	}

	function seatKey(coach, code) {
		return coach + '-' + code;
	}

	function route(detail) {
		return detail.originName + ' → ' + detail.destinationName;
	}

	async function openTrip(tripId, triggerEl) {
		try {
			show(await load(tripId), true);
		} catch (error) {
			TG.toast(error.message, { error: true });
			return;
		}
		TG.sheet.open(SHEET_ID, triggerEl);
	}

	function load(tripId) {
		return TG.api(sheetEl.dataset.apiUrl + '/' + encodeURIComponent(tripId));
	}

	/* ---------- render ---------- */

	function show(detail, fresh) {
		trip = detail;
		if (fresh) {
			// Open on the first coach with sold seats, so the map never looks empty by accident.
			const withSold = detail.coaches.find(function (coach) { return coach.sold > 0; });
			coachNumber = (withSold || detail.coaches[0] || {}).number;
			pickedSeat = null;
		}

		const train = slot('train');
		train.href = sheetEl.dataset.trainsUrl + '/' + detail.trainId;
		train.textContent = detail.trainCode + ' ' + detail.trainName;
		slot('id').textContent = detail.id;
		slot('title').textContent = route(detail);
		slot('phase').className = 'tag ' + detail.phase;
		slot('phase').textContent = detail.phaseLabel;
		slot('schedule').textContent = detail.schedule;
		slot('distance').textContent = detail.distanceKm + ' km · ' + detail.duration;
		slot('price').textContent = money(detail.basePrice);
		slot('sold').textContent = detail.soldSeats + '/' + detail.totalSeats + ' chỗ';

		renderCoachTabs();
		renderSeats();
		renderPassengers();
		renderFooter();
	}

	function renderCoachTabs() {
		const tabs = slot('coach-tabs');
		tabs.replaceChildren();
		trip.coaches.forEach(function (coach) {
			const button = document.createElement('button');
			button.type = 'button';
			button.setAttribute('role', 'tab');
			button.setAttribute('aria-selected', String(coach.number === coachNumber));
			const name = document.createElement('b');
			name.textContent = 'Toa ' + coach.number;
			const info = document.createElement('small');
			info.textContent = coach.typeLabel + ' · ' + coach.sold + '/' + coach.capacity;
			button.append(name, info);
			button.addEventListener('click', function () {
				selectCoach(coach.number);
			});
			tabs.append(button);
		});
	}

	/*
	 * Updates the tabs in place instead of rebuilding them: a rebuilt tab would
	 * drop keyboard focus to <body>, where Esc no longer reaches the sheet.
	 */
	function selectCoach(number) {
		coachNumber = number;
		slot('coach-tabs').querySelectorAll('[role="tab"]').forEach(function (tab, index) {
			tab.setAttribute('aria-selected', String(trip.coaches[index].number === number));
		});
		renderSeats();
	}

	/* Seat coaches: 8 seats a row. Sleeper coaches: cabins of 4 beds. */
	function renderSeats() {
		const panel = slot('seats');
		panel.replaceChildren();
		const coach = trip.coaches.find(function (c) { return c.number === coachNumber; });
		if (!coach) {
			return;
		}
		const grid = document.createElement('div');
		if (coach.type === 'SLEEPER') {
			grid.className = 'seats cabins';
			const cabins = new Map();
			coach.seats.forEach(function (seat) {
				if (!cabins.has(seat.cabin)) {
					const cabin = document.createElement('div');
					cabin.className = 'cabin';
					const label = document.createElement('small');
					label.textContent = 'Khoang ' + seat.cabin;
					cabin.append(label);
					cabins.set(seat.cabin, cabin);
					grid.append(cabin);
				}
				cabins.get(seat.cabin).append(seatButton(coach, seat));
			});
		} else {
			grid.className = 'seats';
			coach.seats.forEach(function (seat) {
				grid.append(seatButton(coach, seat));
			});
		}
		panel.append(grid);
	}

	function seatButton(coach, seat) {
		const key = seatKey(coach.number, seat.code);
		const button = document.createElement('button');
		button.type = 'button';
		button.className = 'seat' + (seat.sold ? ' sold' : '');
		button.disabled = !seat.sold;
		button.textContent = seat.code;
		button.setAttribute('aria-label', seat.sold
			? seat.code + ', đã bán cho ' + seat.passengerName
			: seat.code + ', còn trống');
		if (seat.sold) {
			button.dataset.key = key;
			button.setAttribute('aria-pressed', String(pickedSeat === key));
			button.addEventListener('click', function () {
				pickedSeat = pickedSeat === key ? null : key;
				markPickedSeat();
				markPickedPassenger();
			});
		}
		return button;
	}

	/* In place, like selectCoach, so the pressed seat keeps focus. */
	function markPickedSeat() {
		slot('seats').querySelectorAll('.seat.sold').forEach(function (button) {
			button.setAttribute('aria-pressed', String(button.dataset.key === pickedSeat));
		});
	}

	function renderPassengers() {
		const table = slot('passengers');
		const body = table.querySelector('tbody');
		body.replaceChildren();
		trip.passengers.forEach(function (passenger) {
			const row = rowTemplate.content.firstElementChild.cloneNode(true);
			row.dataset.seat = seatKey(passenger.coachNumber, passenger.seatCode);
			row.querySelector('[data-cell="name"]').textContent = passenger.fullName;
			row.querySelector('[data-cell="identity"]').textContent = 'CCCD ' + passenger.maskedIdentity;
			row.querySelector('[data-cell="seat"]').textContent = 'Toa ' + passenger.coachNumber + ' · ' + passenger.seatCode;
			const booking = row.querySelector('[data-cell="booking"]');
			booking.href = sheetEl.dataset.bookingsUrl + '?open=' + encodeURIComponent(passenger.bookingCode);
			booking.textContent = passenger.bookingCode;
			row.querySelector('[data-cell="price"]').textContent = money(passenger.ticketPrice);
			body.append(row);
		});
		const empty = trip.passengers.length === 0;
		table.hidden = empty;
		slot('no-passengers').hidden = !empty;
		markPickedPassenger();
	}

	function markPickedPassenger() {
		slot('passengers').querySelectorAll('tbody tr').forEach(function (row) {
			const picked = row.dataset.seat === pickedSeat;
			row.classList.toggle('picked', picked);
			if (picked) {
				row.scrollIntoView({ block: 'nearest' });
			}
		});
	}

	function renderFooter() {
		const reason = trip.cancelBlockReason;
		slot('cancel-note').textContent = reason || 'Chưa có booking nào, có thể hủy chuyến.';
		cancelButton.hidden = trip.cancelled;
		cancelButton.disabled = reason !== null;
	}

	/* ---------- cancel ---------- */

	cancelButton.addEventListener('click', async function () {
		if (!trip || trip.cancelBlockReason !== null) {
			return;
		}
		const shown = trip;
		const yes = await TG.confirm({
			title: 'Hủy chuyến này?',
			body: shown.trainCode + ' ' + route(shown) + ', ' + shown.departureShort
				+ '. Khách sẽ không đặt được chuyến này nữa. Lịch của tàu bị đứt ở chặng này.',
			confirmLabel: 'Hủy chuyến',
			danger: true
		});
		if (!yes) {
			return;
		}
		cancelButton.disabled = true;
		try {
			await TG.api(sheetEl.dataset.apiUrl + '/' + shown.id + '/cancel', { method: 'POST' });
		} catch (error) {
			TG.toast(error.message, { error: true });
			refresh(shown.id);
			return;
		}
		const message = 'Đã hủy chuyến ' + shown.trainCode + ' ' + route(shown) + '.';
		const event = new CustomEvent('trip:cancelled', { detail: { tripId: shown.id, message: message }, cancelable: true });
		if (document.dispatchEvent(event)) {
			TG.toastAfterReload(message);
			const url = new URL(location.href);
			url.searchParams.set(OPEN_PARAM, shown.id);
			location.replace(url);
			return;
		}
		await refresh(shown.id);
		// The focused cancel button is now hidden; keep focus inside the sheet so Esc still closes it.
		if (cancelButton.hidden) {
			sheetEl.focus();
		}
	});

	/* Shows the trip as the server has it now; keeps the coach and the picked seat. */
	async function refresh(tripId) {
		try {
			show(await load(tripId), false);
		} catch (error) {
			TG.toast(error.message, { error: true });
		}
	}

	sheetEl.addEventListener('hidden.bs.offcanvas', function () {
		trip = null;
		pickedSeat = null;
	});

	/* ---------- openers ---------- */

	/*
	 * The marked element itself may be a button, as in the booking sheet. A
	 * link or button inside a marked row keeps its own job.
	 */
	function opener(target) {
		const el = target.closest('[data-trip-id]');
		if (!el || sheetEl.contains(el)) {
			return null;
		}
		const control = target.closest('a, button');
		return control && control !== el && el.contains(control) ? null : el;
	}

	document.addEventListener('click', function (event) {
		const el = opener(event.target);
		if (el) {
			openTrip(el.dataset.tripId, el);
		}
	});

	// Buttons and links turn Enter into a click themselves; this is for rows and list items.
	document.addEventListener('keydown', function (event) {
		const target = event.target;
		if (event.key !== 'Enter' || !target.matches('[data-trip-id]') || target.matches('a, button')) {
			return;
		}
		const el = opener(target);
		if (el) {
			event.preventDefault();
			openTrip(el.dataset.tripId, el);
		}
	});

	/*
	 * ?trip=<id>: after the reload of a cancel, or from a shared link. Not
	 * ?open=, which the bookings page uses for its own sheet.
	 */
	const url = new URL(location.href);
	const openId = url.searchParams.get(OPEN_PARAM);
	if (openId) {
		url.searchParams.delete(OPEN_PARAM);
		history.replaceState(null, '', url);
		openTrip(openId, document.querySelector('[data-trip-id="' + CSS.escape(openId) + '"]'));
	}

	TG.openTrip = openTrip;
})();
