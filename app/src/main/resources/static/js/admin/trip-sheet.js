/*
 * The trip sheet (templates/admin/fragments/trip-sheet.html), shared by every
 * admin page that mentions a trip. It adds TG.openTrip(tripId, triggerEl),
 * which reads GET /api/admin/trips/{id} and opens the sheet; any element with
 * data-trip-id opens it on a click or Enter, so a page only marks its rows.
 *
 * The sheet draws the four coaches as tabs with a seat map: sold seats can be
 * pressed and highlight their passenger's row; free seats are disabled. It
 * opens on the first coach that has a sold seat, so the map never looks
 * empty by accident.
 */
(function () {
	'use strict';

	const sheet = document.getElementById('trip-sheet');
	if (!sheet) {
		return;
	}
	const money = new Intl.NumberFormat('vi-VN');
	// The trip on show and its coach tab; render() fills them.
	let current = null;
	let openCoach = 1;

	function field(name) {
		return sheet.querySelector('[data-f="' + name + '"]');
	}

	function element(tag, className, text) {
		const el = document.createElement(tag);
		if (className) {
			el.className = className;
		}
		if (text !== undefined) {
			el.textContent = text;
		}
		return el;
	}

	function cell(text, className) {
		return element('td', className, text);
	}

	/* ---------- seat map ---------- */

	function seatButton(coach, seat) {
		const button = element('button', 'seat' + (seat.sold ? ' sold' : ''), seat.code);
		button.type = 'button';
		button.disabled = !seat.sold;
		button.dataset.key = coach.number + '-' + seat.code;
		button.setAttribute('aria-label', 'Toa ' + coach.number + ' chỗ ' + seat.code
				+ (seat.sold ? ', đã bán' + (seat.passengerName ? ' cho ' + seat.passengerName : '') : ', còn trống'));
		return button;
	}

	// Beds sit four to a cabin; seats are one run of eight to a row.
	function drawSeats(coach) {
		const map = field('seat-map');
		map.replaceChildren();
		if (!coach.sleeper) {
			const grid = element('div', 'seats');
			coach.seats.forEach(function (seat) {
				grid.appendChild(seatButton(coach, seat));
			});
			map.appendChild(grid);
			return;
		}
		const grid = element('div', 'seats cabins');
		const cabins = new Map();
		coach.seats.forEach(function (seat) {
			if (!cabins.has(seat.cabin)) {
				const cabin = element('div', 'cabin');
				cabin.setAttribute('role', 'group');
				cabin.setAttribute('aria-label', 'Khoang ' + seat.cabin);
				cabins.set(seat.cabin, cabin);
				grid.appendChild(cabin);
			}
			cabins.get(seat.cabin).appendChild(seatButton(coach, seat));
		});
		map.appendChild(grid);
	}

	function showCoach(number) {
		openCoach = number;
		field('coach-tabs').querySelectorAll('[role="tab"]').forEach(function (tab) {
			tab.setAttribute('aria-selected', String(Number(tab.dataset.coach) === number));
		});
		drawSeats(current.coaches.find(function (coach) {
			return coach.number === number;
		}));
		clearHighlight();
	}

	function clearHighlight() {
		field('passengers').querySelectorAll('tr.hl').forEach(function (row) {
			row.classList.remove('hl');
		});
	}

	/* ---------- the rest of the sheet ---------- */

	function passengerRow(passenger) {
		const tr = element('tr');
		tr.dataset.key = passenger.coachNumber + '-' + passenger.seatCode;
		const name = cell(passenger.fullName);
		name.appendChild(element('span', 'sub mono', passenger.idMask));
		tr.appendChild(name);
		tr.appendChild(cell('Toa ' + passenger.coachNumber + ' · ' + passenger.seatCode));
		const booking = element('td');
		const link = element('a', 'code', passenger.bookingCode);
		link.href = sheet.dataset.bookingsUrl + '?open=' + encodeURIComponent(passenger.bookingCode);
		booking.appendChild(link);
		tr.appendChild(booking);
		tr.appendChild(cell(money.format(passenger.price) + 'đ', 'num'));
		return tr;
	}

	function render(trip) {
		current = trip;
		document.getElementById('trip-sheet-title').textContent = trip.routeLabel;
		const trainLink = field('train-link');
		trainLink.textContent = trip.trainCode + ' ' + trip.trainName;
		trainLink.href = sheet.dataset.trainsUrl + '/' + trip.trainId;
		field('trip-id').textContent = 'chuyến #' + trip.id;
		const status = field('status');
		status.textContent = trip.statusLabel;
		status.className = 'tag ' + trip.statusCss;
		field('times').textContent = trip.departureLabel + ' → ' + trip.arrivalLabel;
		field('distance').textContent = trip.distanceKm + ' km';
		field('price').textContent = money.format(trip.basePrice) + 'đ';
		field('sold').textContent = trip.bookedCount + '/' + trip.totalSeats + ' chỗ';

		const tabs = field('coach-tabs');
		tabs.replaceChildren();
		trip.coaches.forEach(function (coach) {
			const tab = element('button', 'coach-tab', 'Toa ' + coach.number + ' · ' + coach.typeLabel + ' · '
					+ coach.sold + '/' + coach.capacity);
			tab.type = 'button';
			tab.setAttribute('role', 'tab');
			tab.dataset.coach = coach.number;
			tabs.appendChild(tab);
		});

		const body = field('passengers');
		body.replaceChildren();
		trip.passengers.forEach(function (passenger) {
			body.appendChild(passengerRow(passenger));
		});
		if (!trip.passengers.length) {
			const empty = element('tr');
			const td = cell('Chưa có booking nào.', 'text-body-secondary');
			td.colSpan = 4;
			empty.appendChild(td);
			body.appendChild(empty);
		}

		const firstSold = trip.coaches.find(function (coach) {
			return coach.sold > 0;
		});
		showCoach((firstSold || trip.coaches[0]).number);
	}

	field('coach-tabs').addEventListener('click', function (event) {
		const tab = event.target.closest('[role="tab"]');
		if (tab) {
			showCoach(Number(tab.dataset.coach));
		}
	});

	// Pressing a sold seat highlights its passenger.
	field('seat-map').addEventListener('click', function (event) {
		const seat = event.target.closest('.seat.sold');
		if (!seat) {
			return;
		}
		clearHighlight();
		const row = field('passengers').querySelector('tr[data-key="' + seat.dataset.key + '"]');
		if (row) {
			row.classList.add('hl');
			row.scrollIntoView({ block: 'nearest' });
		}
	});

	/* ---------- opening ---------- */

	let latestRequest = 0;

	TG.openTrip = function (tripId, triggerEl) {
		const request = ++latestRequest;
		return TG.api(sheet.dataset.apiUrl + '/' + encodeURIComponent(tripId)).then(function (trip) {
			if (request !== latestRequest) {
				return;
			}
			render(trip);
			TG.sheet.open('trip-sheet', triggerEl);
		}).catch(function (error) {
			TG.toast(error.message, { error: true });
		});
	};

	// Any element marked data-trip-id opens its trip.
	document.addEventListener('click', function (event) {
		const trigger = event.target.closest('[data-trip-id]');
		if (trigger) {
			TG.openTrip(trigger.dataset.tripId, trigger);
		}
	});
	document.addEventListener('keydown', function (event) {
		const trigger = event.target.closest && event.target.closest('[data-trip-id]');
		// Buttons and links get a click from Enter themselves; this is for rows and list items.
		if (trigger && event.key === 'Enter' && event.target === trigger && !trigger.matches('button, a')) {
			TG.openTrip(trigger.dataset.tripId, trigger);
		}
	});
}());
