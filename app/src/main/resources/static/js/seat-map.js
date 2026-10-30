/*
 * Seat page, booking step 2 (templates/booking/seats.html, issue #30).
 *
 * Loads GET /api/trips/{id}/seat-map and draws the coach tabs, the map of
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
	var noticeEl = document.getElementById('tgNotice');

	var data = null;        // the last seat map from the server
	var activeCoach = 1;    // number of the coach shown
	var chosen = new Map(); // seat id -> { id, code, coach, label }
	var noticeTimer = null;

	function money(value) {
		return value.toLocaleString('vi-VN') + ' đ';
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
		noticeEl.textContent = message;
		clearTimeout(noticeTimer);
		noticeTimer = setTimeout(function () {
			noticeEl.textContent = '';
		}, 4000);
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

	/** A seat's grid position, two rows either side of the aisle: across on a wide screen, along on a phone. */
	function place(button, index) {
		var row = [1, 2, 4, 5][index % 4];
		var line = Math.floor(index / 4) + 1;
		button.style.setProperty('--dr', row);
		button.style.setProperty('--dc', line);
		button.style.setProperty('--mr', line);
		button.style.setProperty('--mc', row);
	}

	function seatButton(coach, cell) {
		var picked = chosen.has(cell.id);
		var noun = coach.type === 'SEAT' ? 'Ghế ' : 'Giường ';
		var button = el('button', 'seat', cell.code);
		button.type = 'button';
		button.disabled = !cell.available;
		button.setAttribute('aria-pressed', String(picked));
		button.setAttribute('aria-label', noun + cell.code + (!cell.available ? ', đã có người đặt' : picked ? ', bạn đã chọn' : ''));
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
		data.coaches.forEach(function (coach) {
			var mine = chosenIn(coach.number);
			var seat = coach.type === 'SEAT';
			var active = coach.number === activeCoach;
			var button = el('button', 'car ' + (seat ? 'car-seat' : 'car-bed') + (mine ? ' is-success' : '') + (coach.available ? '' : ' is-full'));
			button.type = 'button';
			button.setAttribute('role', 'tab');
			button.setAttribute('aria-selected', String(active));
			button.setAttribute('aria-controls', 'coach-panel');
			button.setAttribute('aria-label', 'Toa ' + coach.number + ', ' + coach.label.toLowerCase() + ', '
				+ (coach.available ? 'còn ' + coach.available + ' chỗ' : 'hết chỗ') + (mine ? ', đã chọn ' + mine : ''));

			var top = el('span', 'car-top');
			var name = el('b');
			name.appendChild(icon(seat ? 'grid-3x2-gap' : 'moon-stars'));
			name.appendChild(document.createTextNode('Toa ' + coach.number));
			top.appendChild(name);
			if (mine) {
				var badge = el('span', 'badge badge-primary');
				badge.appendChild(icon('check-lg'));
				badge.appendChild(document.createTextNode(String(mine)));
				top.appendChild(badge);
			}
			button.appendChild(top);

			var glass = el('span', 'car-glass');
			glass.setAttribute('aria-hidden', 'true');
			button.appendChild(glass);

			var info = el('span', 'car-info');
			info.appendChild(el('small', '', coach.label));
			info.appendChild(el('strong', '', coach.available ? 'Còn ' + coach.available + ' chỗ' : 'Hết chỗ'));
			button.appendChild(info);

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
		document.getElementById('tgCoachTitle').textContent = 'Toa ' + coach.number + ', ' + coach.label.toLowerCase();
		document.getElementById('tgPrice').textContent = money(data.trip.price);

		var carriage = el('div', 'carriage');
		if (coach.type === 'SLEEPER') {
			var cabins = el('div', 'cabins');
			var byCabin = {};
			coach.seats.forEach(function (cell) {
				(byCabin[cell.cabin] = byCabin[cell.cabin] || []).push(cell);
			});
			Object.keys(byCabin).forEach(function (cabin) {
				var box = el('div', 'cabin');
				box.setAttribute('role', 'group');
				box.appendChild(el('h3', '', 'Khoang ' + cabin));
				var tiers = el('div', 'cabin-tiers');
				var beds = byCabin[cabin];
				tiers.appendChild(el('span', '', 'Tầng 2'));
				tiers.appendChild(seatButton(coach, beds[2]));
				tiers.appendChild(seatButton(coach, beds[3]));
				tiers.appendChild(el('span', '', 'Tầng 1'));
				tiers.appendChild(seatButton(coach, beds[0]));
				tiers.appendChild(seatButton(coach, beds[1]));
				box.appendChild(tiers);
				cabins.appendChild(box);
			});
			carriage.appendChild(cabins);
			mapEl.appendChild(carriage);
			return;
		}

		var ends = el('div', 'carriage-ends');
		ends.setAttribute('aria-hidden', 'true');
		var head = el('span');
		head.appendChild(icon('train-front'));
		head.appendChild(document.createTextNode('Phía đầu tàu'));
		ends.appendChild(head);
		ends.appendChild(el('span', '', 'Phía cuối toa'));
		carriage.appendChild(ends);

		var grid = el('div', 'seats');
		grid.setAttribute('role', 'group');
		grid.setAttribute('aria-label', 'Toa ' + coach.number + ', ' + coach.label.toLowerCase());
		coach.seats.forEach(function (cell, index) {
			var button = seatButton(coach, cell);
			place(button, index);
			grid.appendChild(button);
		});
		var aisle = el('span', 'aisle', 'Lối đi');
		aisle.setAttribute('aria-hidden', 'true');
		grid.appendChild(aisle);
		carriage.appendChild(grid);
		mapEl.appendChild(carriage);
	}

	function drawSummary() {
		pickedEl.textContent = '';
		var total = 0;
		chosen.forEach(function (seat) {
			total += data.trip.price;
			var item = el('li');
			item.appendChild(el('span', 'pick-seat', seat.code));
			item.appendChild(el('span', 'pick-coach', 'Toa ' + seat.coach));
			item.appendChild(el('span', 'pick-price', money(data.trip.price)));
			var remove = el('button', 'icon-btn');
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
		document.getElementById('tgPickedEmpty').hidden = chosen.size > 0;
		document.getElementById('tgCount').textContent = chosen.size + ' vé';
		document.getElementById('tgTotal').textContent = money(total);
		nextEl.disabled = chosen.size === 0;
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
		var from = event.target.closest && event.target.closest('.seat');
		if (!vector || !from) {
			return;
		}
		var start = from.getBoundingClientRect();
		var best = null;
		var bestScore = Infinity;
		mapEl.querySelectorAll('.seat:not(:disabled)').forEach(function (seat) {
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
		var box = el('div', 'seats-loading');
		box.appendChild(el('p', '', message));
		var retry = el('button', 'btn btn-outline btn-sm', 'Thử lại');
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
