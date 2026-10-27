/*
 * Seat page, booking step 2 (templates/booking/seats.html, issue #30).
 *
 * Loads GET /api/trips/{id}/seat-map and draws the train strip, the map of
 * the coach being viewed and the chosen-seat list. The chosen seats live in
 * a Map keyed by seat id; "Tiếp tục" copies their ids into hidden inputs and
 * posts the form. Nothing is locked here: the server checks the seats again
 * on that post (ADR 0010), and a seat sold meanwhile comes back as a message.
 */
(function () {
	'use strict';

	var root = document.getElementById('tgSeats');
	if (!root) {
		return;
	}

	var MAX = parseInt(root.dataset.max, 10) || 10;
	var form = document.getElementById('tgSeatForm');
	var trainEl = document.getElementById('tgTrain');
	var mapEl = document.getElementById('tgCoachMap');
	var pickedEl = document.getElementById('tgPicked');
	var nextEl = document.getElementById('tgNext');
	var toastEl = document.getElementById('tgSeatsToast');

	var data = null;        // the last seat map from the server
	var activeCoach = 1;    // number of the coach shown
	var chosen = new Map(); // seat id -> { id, code, coach, label }
	var toastTimer = null;

	function money(value) {
		return value.toLocaleString('vi-VN') + 'đ';
	}

	function el(tag, className, text) {
		var node = document.createElement(tag);
		if (className) {
			node.className = className;
		}
		if (text !== undefined) {
			node.textContent = text;
		}
		return node;
	}

	function icon(name) {
		var node = el('i', 'bi bi-' + name);
		node.setAttribute('aria-hidden', 'true');
		return node;
	}

	function toast(message) {
		toastEl.textContent = message;
		toastEl.hidden = false;
		clearTimeout(toastTimer);
		toastTimer = setTimeout(function () {
			toastEl.hidden = true;
		}, 3000);
	}

	function coachOf(number) {
		return data.coaches.filter(function (coach) {
			return coach.number === number;
		})[0];
	}

	function chosenIn(number) {
		var count = 0;
		chosen.forEach(function (seat) {
			if (seat.coach === number) {
				count++;
			}
		});
		return count;
	}

	/** The coach to open: the type asked for, else the one holding a chosen seat, else the first with room. */
	function firstCoach() {
		var type = root.dataset.type;
		var coaches = data.coaches;
		var pick = function (test) {
			return coaches.filter(test)[0];
		};
		var found = type && (pick(function (c) { return c.type === type && c.available > 0; })
			|| pick(function (c) { return c.type === type; }));
		found = found || (chosen.size && coachOf(chosen.values().next().value.coach))
			|| pick(function (c) { return c.available > 0; }) || coaches[0];
		return found ? found.number : 1;
	}

	function toggle(coach, cell) {
		if (chosen.has(cell.id)) {
			chosen.delete(cell.id);
		} else if (chosen.size >= MAX) {
			toast('Mỗi lần đặt tối đa ' + MAX + ' chỗ.');
			return;
		} else {
			chosen.set(cell.id, { id: cell.id, code: cell.code, coach: coach.number, label: coach.label });
		}
		render();
	}

	function seatButton(coach, cell) {
		var picked = chosen.has(cell.id);
		var button = el('button', 'tg-seat', cell.code);
		button.type = 'button';
		button.disabled = !cell.available;
		button.setAttribute('aria-pressed', String(picked));
		button.setAttribute('aria-label', 'Toa ' + coach.number + ' chỗ ' + cell.code
			+ (!cell.available ? ', đã bán' : picked ? ', bạn đã chọn' : ', còn trống'));
		button.addEventListener('click', function () {
			toggle(coach, cell);
			var again = mapEl.querySelector('[data-seat-id="' + cell.id + '"]');
			if (again) {
				again.focus();
			}
		});
		button.dataset.seatId = cell.id;
		return button;
	}

	function drawTrain() {
		trainEl.textContent = '';
		var loco = el('span', 'tg-loco');
		loco.setAttribute('aria-hidden', 'true');
		loco.appendChild(icon('train-front-fill'));
		trainEl.appendChild(loco);

		data.coaches.forEach(function (coach) {
			var mine = chosenIn(coach.number);
			var button = el('button', 'tg-car' + (coach.available ? '' : ' is-full'));
			button.type = 'button';
			button.setAttribute('role', 'tab');
			button.setAttribute('aria-selected', String(coach.number === activeCoach));
			button.appendChild(el('b', '', 'Toa ' + coach.number));
			button.appendChild(el('small', '', mine ? 'Đã chọn ' + mine
				: coach.available ? coach.label + ' · còn ' + coach.available : 'Hết chỗ'));

			// Six windows; the filled ones show how much of the coach is sold.
			var windows = el('span', 'tg-car-windows');
			windows.setAttribute('aria-hidden', 'true');
			var sold = Math.round((coach.capacity - coach.available) / coach.capacity * 6);
			for (var i = 0; i < 6; i++) {
				windows.appendChild(el('i', i < sold ? 'is-sold' : ''));
			}
			button.appendChild(windows);

			button.addEventListener('click', function () {
				activeCoach = coach.number;
				render();
			});
			trainEl.appendChild(button);
		});
	}

	function drawCoach() {
		var coach = coachOf(activeCoach);
		mapEl.textContent = '';
		document.getElementById('tgCoachTitle').textContent = 'Toa ' + coach.number;
		document.getElementById('tgCoachKind').textContent = coach.label;
		document.getElementById('tgCoachPrice').textContent = money(data.trip.price);
		document.getElementById('tgSeatsHeading').textContent = coach.type === 'SEAT' ? 'Chọn ghế ngồi' : 'Chọn giường nằm';

		if (coach.type === 'SLEEPER') {
			var cabins = el('div', 'tg-cabins');
			var byCabin = {};
			coach.seats.forEach(function (cell) {
				(byCabin[cell.cabin] = byCabin[cell.cabin] || []).push(cell);
			});
			Object.keys(byCabin).forEach(function (cabin) {
				var box = el('div', 'tg-cabin');
				box.appendChild(el('p', '', 'Khoang ' + cabin));
				var beds = el('div');
				byCabin[cabin].forEach(function (cell) {
					beds.appendChild(seatButton(coach, cell));
				});
				box.appendChild(beds);
				cabins.appendChild(box);
			});
			mapEl.appendChild(cabins);
			return;
		}

		// Rows of four, two seats either side of the aisle.
		var grid = el('div', 'tg-seat-grid');
		coach.seats.forEach(function (cell, index) {
			grid.appendChild(seatButton(coach, cell));
			if (index % 4 === 1) {
				grid.appendChild(el('span', 'tg-aisle', index === 1 ? 'Lối đi' : ''));
			}
		});
		mapEl.appendChild(grid);
	}

	function drawSummary() {
		pickedEl.textContent = '';
		var total = 0;
		chosen.forEach(function (seat) {
			total += data.trip.price;
			var item = el('li');
			item.appendChild(el('span', 'tg-picked-code', seat.code));
			var what = el('span', 'tg-picked-what');
			what.appendChild(el('b', '', 'Toa ' + seat.coach + ' · ' + seat.label));
			what.appendChild(el('small', '', money(data.trip.price)));
			item.appendChild(what);
			var remove = el('button');
			remove.type = 'button';
			remove.setAttribute('aria-label', 'Bỏ chỗ ' + seat.code + ' toa ' + seat.coach);
			remove.appendChild(icon('x-lg'));
			remove.addEventListener('click', function () {
				chosen.delete(seat.id);
				render();
			});
			item.appendChild(remove);
			pickedEl.appendChild(item);
		});
		document.getElementById('tgTotal').textContent = money(total);
		nextEl.disabled = chosen.size === 0;
		nextEl.textContent = chosen.size ? 'Tiếp tục · ' + chosen.size + ' vé' : 'Chọn ít nhất 1 chỗ';
	}

	function render() {
		drawTrain();
		drawCoach();
		drawSummary();
	}

	/** Arrow keys move to the nearest free seat in that direction, whatever the layout. */
	mapEl.addEventListener('keydown', function (event) {
		var vectors = { ArrowLeft: [-1, 0], ArrowRight: [1, 0], ArrowUp: [0, -1], ArrowDown: [0, 1] };
		var vector = vectors[event.key];
		var from = event.target.closest && event.target.closest('.tg-seat');
		if (!vector || !from) {
			return;
		}
		var start = from.getBoundingClientRect();
		var best = null;
		var bestScore = Infinity;
		mapEl.querySelectorAll('.tg-seat:not(:disabled)').forEach(function (seat) {
			if (seat === from) {
				return;
			}
			var box = seat.getBoundingClientRect();
			var dx = box.left - start.left;
			var dy = box.top - start.top;
			var along = dx * vector[0] + dy * vector[1];
			var across = Math.abs(dx * vector[1]) + Math.abs(dy * vector[0]);
			if (along <= 1) {
				return;
			}
			var score = along + across * 3;
			if (score < bestScore) {
				best = seat;
				bestScore = score;
			}
		});
		if (best) {
			event.preventDefault();
			best.focus();
		}
	});

	form.addEventListener('submit', function (event) {
		if (!chosen.size) {
			event.preventDefault();
			return;
		}
		form.querySelectorAll('input[name="seatIds"]').forEach(function (input) {
			input.remove();
		});
		chosen.forEach(function (seat) {
			var input = el('input');
			input.type = 'hidden';
			input.name = 'seatIds';
			input.value = seat.id;
			form.appendChild(input);
		});
		nextEl.disabled = true;
	});

	function failed(message) {
		mapEl.textContent = '';
		var box = el('div', 'tg-seats-loading');
		box.appendChild(el('p', '', message));
		var retry = el('button', 'btn btn-outline-primary', 'Thử lại');
		retry.type = 'button';
		retry.addEventListener('click', load);
		box.appendChild(retry);
		mapEl.appendChild(box);
	}

	function load() {
		fetch(root.dataset.apiUrl, { headers: { Accept: 'application/json' } })
			.then(function (response) {
				if (response.status === 409 || response.status === 404) {
					// The trip was cancelled or left meanwhile; the page answers with the reason.
					window.location.reload();
					return null;
				}
				if (!response.ok) {
					throw new Error('seat-map ' + response.status);
				}
				return response.json();
			})
			.then(function (map) {
				if (!map) {
					return;
				}
				data = map;
				var wanted = (root.dataset.selected || '').split(',').filter(Boolean).map(Number);
				var dropped = [];
				wanted.forEach(function (id) {
					var found = null;
					data.coaches.forEach(function (coach) {
						coach.seats.forEach(function (cell) {
							if (cell.id === id) {
								found = { coach: coach, cell: cell };
							}
						});
					});
					if (found && found.cell.available) {
						chosen.set(id, { id: id, code: found.cell.code, coach: found.coach.number, label: found.coach.label });
					} else if (found) {
						dropped.push(found.cell.code);
					}
				});
				root.dataset.selected = '';
				activeCoach = firstCoach();
				render();
				if (dropped.length && !document.querySelector('.alert-danger')) {
					toast('Chỗ ' + dropped.join(', ') + ' vừa được người khác đặt.');
				}
			})
			.catch(function () {
				failed('Không tải được sơ đồ chỗ. Vui lòng thử lại.');
			});
	}

	load();
}());
