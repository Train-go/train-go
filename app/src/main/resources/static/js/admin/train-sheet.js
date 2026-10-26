/*
 * The "Bảng tính" view of the train page (templates/admin/fragments/
 * train-sheet-view.html): draws the draft rows under the schedule table and
 * handles "Thêm dòng" and "Điền nhanh". The chaining rules live in
 * train-sheet-derive.js; this file keeps the drafts, asks derive() for every
 * row after each change, and writes the result into the DOM.
 *
 * The page is rendered by the server, so the root's data-* attributes say
 * where the train stands; the stations and routes come once from
 * GET /api/admin/network. A row's controls are updated in place and never
 * rebuilt while they have focus, so typing in a time or price box keeps the
 * caret.
 */
(function () {
	'use strict';

	const root = document.querySelector('[data-train-sheet]');
	if (!root) {
		return;
	}
	const D = window.TrainSheetDerive;
	const body = document.getElementById('draft-rows');
	const rowTemplate = document.getElementById('draft-row');
	const addButton = root.querySelector('[data-draft-add]');
	const targetSelect = root.querySelector('[data-fill-target]');
	const paceSelect = root.querySelector('[data-fill-pace]');
	const fillButton = root.querySelector('[data-fill]');
	const errSummary = root.querySelector('[data-err-summary]');
	const DAYS_OFFERED = 14;

	// Stations and routes of the network; null until GET /api/admin/network answers.
	let network = null;
	let drafts = [D.blank()];
	// First station of a train that has no trip yet; the admin picks it in the first row.
	let origin0 = null;
	// The derived rows of the last render, for handlers that need a row's suggestions.
	let derived = [];
	// Draft id -> its two table rows.
	const domRows = new Map();
	// A plan request is on its way; "Điền" stays off until it answers.
	let filling = false;

	/* ---------- network ---------- */

	function buildNetwork(view) {
		const stations = view.stations.slice().sort(function (a, b) {
			return a.kmMark - b.kmMark;
		});
		const names = new Map();
		const kms = new Map();
		const reach = new Map();
		stations.forEach(function (station) {
			names.set(station.id, station.name);
			reach.set(station.id, []);
		});
		view.routes.forEach(function (route) {
			kms.set(route.originId + ':' + route.destinationId, route.distanceKm);
			reach.get(route.originId).push({ to: route.destinationId, km: route.distanceKm });
		});
		const order = new Map(stations.map(function (station, i) {
			return [station.id, i];
		}));
		reach.forEach(function (list) {
			list.sort(function (a, b) {
				return order.get(a.to) - order.get(b.to);
			});
		});
		return { stations: stations, names: names, kms: kms, reach: reach };
	}

	function context() {
		const num = function (text) {
			return text ? Number(text) : null;
		};
		return {
			chainOrigin: num(root.dataset.chainOrigin),
			origin0: origin0,
			earliest: D.parseIso(root.dataset.earliest),
			chainArrival: D.parseIso(root.dataset.chainArrival),
			kmBetween: function (from, to) {
				const km = network.kms.get(from + ':' + to);
				return km === undefined ? null : km;
			},
			stationName: function (id) {
				return network.names.get(id) || '';
			}
		};
	}

	/* ---------- small DOM helpers ---------- */

	// A control of the draft row, or of the error line under it.
	function field(dom, name) {
		const selector = '[data-f="' + name + '"]';
		return dom.row.querySelector(selector) || dom.help.querySelector(selector);
	}

	// A control the admin is typing in keeps its text.
	function setValue(el, value) {
		if (el.value !== value && document.activeElement !== el) {
			el.value = value;
		}
	}

	function option(value, text, disabled) {
		const el = document.createElement('option');
		el.value = value;
		el.textContent = text;
		el.disabled = Boolean(disabled);
		return el;
	}

	function group(label, options) {
		const el = document.createElement('optgroup');
		el.label = label;
		options.forEach(function (o) {
			el.appendChild(o);
		});
		return el;
	}

	// Rebuilds a select's options only when the key says they changed.
	function fillSelect(select, key, build) {
		if (select.dataset.key === key) {
			return;
		}
		const value = select.value;
		select.replaceChildren();
		build().forEach(function (el) {
			select.appendChild(el);
		});
		select.dataset.key = key;
		select.value = value;
	}

	/* ---------- rows ---------- */

	function createRow(draft) {
		const fragment = rowTemplate.content.cloneNode(true);
		const row = fragment.querySelector('tr.draft');
		const help = fragment.querySelector('tr.draft-help');
		row.dataset.id = draft.id;
		body.appendChild(row);
		body.appendChild(help);
		return { row: row, help: help };
	}

	function updateDayOptions(select, row) {
		const floorDay = D.startOfDay(row.floor);
		const lastOffered = D.addDays(floorDay, DAYS_OFFERED - 1).getTime();
		const chosen = D.startOfDay(row.depDay).getTime();
		const outside = chosen < floorDay.getTime() || chosen > lastOffered;
		fillSelect(select, floorDay.getTime() + ':' + (outside ? chosen : ''), function () {
			const options = [];
			for (let i = 0; i < DAYS_OFFERED; i++) {
				const day = D.addDays(floorDay, i);
				options.push(option(String(day.getTime()), D.dayLabel(day)));
			}
			// A day outside the offered ones, before the earliest day (next to its
			// error) or after the last, stays selectable instead of going blank.
			if (outside) {
				const extra = option(String(chosen), D.dayLabel(new Date(chosen)));
				if (chosen < floorDay.getTime()) {
					options.unshift(extra);
				} else {
					options.push(extra);
				}
			}
			return options;
		});
		select.value = String(chosen);
	}

	function updateOrigin(dom, row, index) {
		const locked = field(dom, 'origin');
		const select = field(dom, 'origin-select');
		// Only the first row of a train without trips lets the admin pick where it starts.
		const pick = index === 0 && !context().chainOrigin;
		locked.hidden = pick;
		select.hidden = !pick;
		if (pick) {
			fillSelect(select, 'all', function () {
				return [option('', 'Chọn ga')].concat(network.stations.map(function (station) {
					return option(String(station.id), station.name);
				}));
			});
			select.value = origin0 ? String(origin0) : '';
		} else {
			locked.textContent = row.origin ? network.names.get(row.origin) : '';
		}
	}

	function updateDestination(dom, row) {
		const select = field(dom, 'to');
		select.disabled = !row.origin;
		fillSelect(select, String(row.origin || ''), function () {
			const options = [option('', 'Chọn ga')];
			const reachable = row.origin ? network.reach.get(row.origin) : [];
			const reachSet = new Set(reachable.map(function (r) {
				return r.to;
			}));
			if (reachable.length) {
				options.push(group('Có tuyến', reachable.map(function (r) {
					return option(String(r.to), network.names.get(r.to) + ' · ' + r.km + ' km');
				})));
			}
			const others = network.stations.filter(function (station) {
				return station.id !== row.origin && !reachSet.has(station.id);
			});
			if (others.length) {
				options.push(group('Chưa có tuyến', others.map(function (station) {
					return option(String(station.id), station.name, true);
				})));
			}
			return options;
		});
		select.value = row.x.to ? String(row.x.to) : '';
	}

	function updateErrors(dom, row, show) {
		const list = field(dom, 'errors');
		const messages = show ? row.errors : [];
		dom.help.hidden = messages.length === 0;
		if (list.dataset.key === messages.join('|')) {
			return;
		}
		list.dataset.key = messages.join('|');
		list.replaceChildren();
		messages.forEach(function (message) {
			const li = document.createElement('li');
			const icon = document.createElement('i');
			icon.className = 'bi bi-exclamation-circle';
			icon.setAttribute('aria-hidden', 'true');
			li.appendChild(icon);
			li.appendChild(document.createTextNode(message));
			list.appendChild(li);
		});
	}

	function updateRow(dom, row, index, count, show) {
		const label = function (text) {
			return text + ', dòng nháp ' + (index + 1);
		};
		field(dom, 'day').setAttribute('aria-label', label('Ngày đi'));
		field(dom, 'time').setAttribute('aria-label', label('Giờ đi'));
		field(dom, 'origin-select').setAttribute('aria-label', label('Ga đi'));
		field(dom, 'to').setAttribute('aria-label', label('Ga đến'));
		field(dom, 'arr').setAttribute('aria-label', label('Giờ đến'));
		field(dom, 'price').setAttribute('aria-label', label('Giá cơ bản'));
		field(dom, 'remove').setAttribute('aria-label', 'Bỏ dòng nháp ' + (index + 1));

		updateDayOptions(field(dom, 'day'), row);
		setValue(field(dom, 'time'), row.depTime);
		updateOrigin(dom, row, index);
		updateDestination(dom, row);

		const arrText = row.x.arrTouched ? row.x.arrTime : (row.autoArr ? D.fmtTime(row.autoArr) : '');
		setValue(field(dom, 'arr'), arrText);
		const plus = field(dom, 'plus');
		const plusDays = row.arr && row.dep ? D.daysBetween(row.dep, row.arr) : 0;
		plus.hidden = plusDays <= 0;
		plus.textContent = '+' + plusDays;
		field(dom, 'arr-reset').hidden = !row.x.arrTouched;

		field(dom, 'km').textContent = row.km == null ? '' : String(row.km);
		setValue(field(dom, 'price'), Number.isNaN(row.price) ? '' : String(row.price));
		field(dom, 'remove').hidden = count < 2;
		updateErrors(dom, row, show);
	}

	/* ---------- the fill-in bar ---------- */

	function updateFillBar(origin) {
		addButton.disabled = !origin;
		targetSelect.disabled = !origin;
		fillSelectKeepingValue(origin);
		fillButton.disabled = filling || !origin || !targetSelect.value;
	}

	function fillSelectKeepingValue(origin) {
		fillSelect(targetSelect, String(origin || ''), function () {
			return [option('', 'Chọn ga cuối')].concat(network.stations.filter(function (station) {
				return station.id !== origin;
			}).map(function (station) {
				return option(String(station.id), station.name);
			}));
		});
	}

	/* ---------- render ---------- */

	function render() {
		const c = context();
		derived = D.derive(c, drafts);
		const filled = derived.filter(function (row) {
			return row.x.to;
		});

		// Rows the admin removed.
		const alive = new Set(drafts.map(function (x) {
			return x.id;
		}));
		domRows.forEach(function (dom, id) {
			if (!alive.has(id)) {
				dom.row.remove();
				dom.help.remove();
				domRows.delete(id);
			}
		});

		let errorCount = 0;
		let previous = body.querySelector('.drafts-h');
		derived.forEach(function (row, index) {
			let dom = domRows.get(row.x.id);
			if (!dom) {
				dom = createRow(row.x);
				domRows.set(row.x.id, dom);
			}
			// Moving a node would drop its focus, so only move what is out of place.
			if (dom.row.previousElementSibling !== previous) {
				previous.after(dom.row);
			}
			if (dom.help.previousElementSibling !== dom.row) {
				dom.row.after(dom.help);
			}
			previous = dom.help;
			// A row without a destination is ignored, except the first one of an empty sheet.
			const counts = Boolean(row.x.to) || (index === 0 && filled.length === 0);
			const show = counts && row.x.touched;
			if (show) {
				errorCount += row.errors.length;
			}
			updateRow(dom, row, index, derived.length, show);
		});

		errSummary.hidden = errorCount === 0;
		errSummary.textContent = 'Còn ' + errorCount + ' lỗi ở dòng nháp';
		updateFillBar(D.lastOrigin(c, derived));
	}

	/* ---------- events ---------- */

	function draftOf(el) {
		const id = Number(el.closest('tr.draft').dataset.id);
		return drafts.find(function (x) {
			return x.id === id;
		});
	}

	function onEdit(event) {
		const el = event.target.closest('[data-f]');
		if (!el) {
			return;
		}
		const x = draftOf(el);
		switch (el.dataset.f) {
		case 'day':
			x.day = Number(el.value);
			break;
		case 'time':
			x.time = el.value.trim();
			break;
		case 'arr': {
			if (!x.arrTouched) {
				// The first edit keeps the day the suggestion fell on.
				const row = derived.find(function (r) {
					return r.x === x;
				});
				x.arrOff = row && row.autoArr && row.dep ? D.daysBetween(row.dep, row.autoArr) : 0;
			}
			x.arrTouched = true;
			x.arrTime = el.value.trim();
			break;
		}
		case 'price':
			x.priceTouched = true;
			x.price = el.value === '' ? NaN : Number(el.value);
			break;
		case 'to':
			// A new destination brings its own suggested time and price back.
			x.to = el.value ? Number(el.value) : null;
			x.arrTouched = false;
			x.priceTouched = false;
			break;
		case 'origin-select':
			origin0 = el.value ? Number(el.value) : null;
			break;
		default:
			return;
		}
		x.touched = true;
		render();
	}

	body.addEventListener('input', onEdit);
	body.addEventListener('change', onEdit);

	body.addEventListener('click', function (event) {
		const button = event.target.closest('[data-f="remove"], [data-f="arr-reset"]');
		if (!button) {
			return;
		}
		const x = draftOf(button);
		let focusRow = x;
		if (button.dataset.f === 'remove') {
			const index = drafts.indexOf(x);
			drafts = drafts.filter(function (d) {
				return d !== x;
			});
			// Focus goes to the row that took its place, or the last one.
			focusRow = drafts[Math.min(index, drafts.length - 1)];
		} else {
			x.arrTouched = false;
		}
		render();
		const target = domRows.get(focusRow.id);
		if (target) {
			field(target, button.dataset.f === 'remove' ? 'to' : 'arr').focus();
		}
	});

	// A box the admin emptied shows the suggestion again once they leave it.
	body.addEventListener('focusout', function () {
		render();
	});

	addButton.addEventListener('click', function () {
		drafts.push(D.blank());
		render();
	});

	targetSelect.addEventListener('change', function () {
		fillButton.disabled = !targetSelect.value;
	});

	/*
	 * "Điền": asks the server for the legs between the station the train will
	 * stand at and the chosen one, which also checks that every pair of
	 * neighbouring stations on the way has a route. Only the stations are
	 * taken from the answer; the rows then derive their times from the chain,
	 * so changing the first row still moves the ones below it.
	 */
	fillButton.addEventListener('click', async function () {
		const c = context();
		const rows = D.derive(c, drafts);
		const kept = rows.filter(function (row) {
			return row.x.to;
		});
		const pace = paceSelect.value;
		const floor = D.nextFloor(c, kept);
		const first = pace === 'NONSTOP' ? floor : D.nextEight(floor);
		const query = new URLSearchParams({
			from: String(D.lastOrigin(c, rows)), to: targetSelect.value, pace: pace, firstDeparture: D.toIso(first)
		});
		filling = true;
		fillButton.disabled = true;
		try {
			const legs = await TG.api(root.dataset.planUrl + '?' + query.toString());
			drafts = kept.map(function (row) {
				return row.x;
			}).concat(legs.map(function (leg) {
				return D.blank({ to: leg.destinationStationId, pace: pace, touched: true });
			}));
			// The station list is rebuilt for the new last station, which clears the choice.
			targetSelect.dataset.key = '';
			targetSelect.value = '';
			render();
		} catch (error) {
			TG.toast(error.message, { error: true });
		} finally {
			filling = false;
			render();
		}
	});

	/* ---------- start ---------- */

	TG.api(root.dataset.networkUrl).then(function (view) {
		network = buildNetwork(view);
		render();
	}).catch(function (error) {
		TG.toast(error.message, { error: true });
	});
}());
