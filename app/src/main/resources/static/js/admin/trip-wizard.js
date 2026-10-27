/*
 * The trip wizard (templates/admin/trip-wizard.html): one leg after the
 * train's last trip, in four steps, Đi đâu, Khi nào, Giá, Xác nhận.
 *
 * The choices come from GET /api/admin/trains/{id}/next-leg: where the leg
 * starts, the earliest departure, one card per route from there with its
 * suggested running time and price, and the last two trips for the preview.
 * The arrival and the price follow those suggestions until the admin types
 * one; a typed value is never overwritten. The server checks every rule again
 * when the leg is added (docs/decisions/0011-chuyen-tao-tu-tau-noi-chang.md),
 * and a message for legs[0].x takes the admin back to the step holding x.
 *
 * Dates are plain Date objects holding the server's wall-clock time, built
 * and printed through the helpers of train-sheet-derive.js.
 */
(function () {
	'use strict';

	const root = document.querySelector('[data-trip-wizard]');
	if (!root) {
		return;
	}
	const D = window.TrainSheetDerive;
	const body = root.querySelector('[data-wz-body]');
	const stepsBar = root.querySelector('[data-wz-steps]');
	const title = root.querySelector('[data-wz-title]');
	const preview = root.querySelector('[data-wz-preview]');
	const previewList = preview.querySelector('[data-pv-list]');
	const previewAfter = preview.querySelector('[data-pv-after]');
	const backLink = root.querySelector('[data-wz-back]');
	const trainCode = root.dataset.trainCode;
	const seatsPerTrip = root.dataset.seatsPerTrip;

	const STEPS = ['Đi đâu', 'Khi nào', 'Giá', 'Xác nhận'];
	const STEP_TEMPLATES = ['wz-step-where', 'wz-step-when', 'wz-step-price', 'wz-step-confirm'];
	// The error slots each step checks before the next one opens.
	const STEP_ERRORS = [['where'], ['dep', 'arr'], ['price'], []];
	// "legs[0].departureAt" from the server: the error slot of that field.
	const LEG_FIELD = /^legs\[0\]\.(\w+)$/;
	const FIELD_SLOT = {
		originStationId: 'where', destinationStationId: 'where', departureAt: 'dep', arrivalAt: 'arr',
		basePrice: 'price'
	};
	const DAYS_OFFERED = 14;
	const DAYS_SHOWN = 7;
	const QUICK_TIMES = ['06:00', '08:00', '13:00', '20:00'];
	const WEEKDAYS = ['Chủ nhật', 'Thứ Hai', 'Thứ Ba', 'Thứ Tư', 'Thứ Năm', 'Thứ Sáu', 'Thứ Bảy'];
	const SHORT_WEEKDAYS = ['CN', 'T2', 'T3', 'T4', 'T5', 'T6', 'T7'];
	const money = new Intl.NumberFormat('vi-VN');

	// NextLegOptions of the last answer; null until it comes.
	let options = null;
	// What the admin has chosen and typed.
	let draft = null;
	let step = 0;
	// Steps whose "Tiếp tục" was pressed, so all their errors show.
	let tried = [];
	// Error slots the admin has changed, so their errors show before "Tiếp tục".
	let touched = {};
	// Messages from the server, by error slot, until that field changes.
	let serverErrors = {};
	let saving = false;

	/* ---------- formatting ---------- */

	function duration(minutes) {
		const hours = Math.floor(minutes / 60);
		const rest = minutes % 60;
		if (!hours) {
			return rest + ' phút';
		}
		return rest ? hours + ' giờ ' + (rest < 10 ? '0' + rest : rest) + ' phút' : hours + ' giờ';
	}

	function vnd(amount) {
		return money.format(amount) + 'đ';
	}

	function dayTime(d) {
		return D.dayLabel(d) + ' ' + D.fmtTime(d);
	}

	/** "8", "845", "8:45", "8h45" or "8.45" to "08:45"; null when it is no time of day. */
	function normalizeTime(raw) {
		const text = String(raw || '').trim();
		let hours;
		let minutes;
		const parts = /^(\d{1,2})[:h.](\d{1,2})$/.exec(text);
		if (parts) {
			hours = +parts[1];
			minutes = +parts[2];
		} else if (/^\d{1,4}$/.test(text)) {
			hours = text.length <= 2 ? +text : +text.slice(0, text.length - 2);
			minutes = text.length <= 2 ? 0 : +text.slice(-2);
		} else {
			return null;
		}
		if (hours > 23 || minutes > 59) {
			return null;
		}
		return (hours < 10 ? '0' : '') + hours + ':' + (minutes < 10 ? '0' : '') + minutes;
	}

	function sameDay(a, b) {
		return D.daysBetween(a, b) === 0;
	}

	/* ---------- state ---------- */

	function freshDraft() {
		const departure = D.parseIso(options.defaultDeparture);
		return {
			destination: null, day: D.startOfDay(departure), depTime: D.fmtTime(departure), arrTime: '',
			arrOffset: 0, arrTouched: false, arrOpen: false, price: NaN, priceTouched: false, page: null,
			showMissing: false
		};
	}

	/** The leg as chosen so far, with every suggestion filled in and every error found. */
	function derive() {
		const earliest = D.parseIso(options.earliestDeparture);
		const dest = options.destinations.find(function (d) {
			return d.stationId === draft.destination;
		}) || null;
		const dep = D.atTime(draft.day, draft.depTime);
		const autoArr = dest && dep ? new Date(dep.getTime() + dest.minutes * 60000) : null;
		const arr = draft.arrTouched
			? (dep ? D.atTime(D.addDays(D.startOfDay(dep), draft.arrOffset), draft.arrTime) : null)
			: autoArr;
		const price = draft.priceTouched ? draft.price : (dest ? dest.suggestedPrice : NaN);

		const errors = {};
		if (!options.origin) {
			errors.where = 'Chọn ga xuất phát.';
		} else if (!dest) {
			errors.where = 'Chọn ga đến.';
		}
		if (!dep) {
			errors.dep = 'Giờ không hợp lệ. Nhập dạng 24 giờ, ví dụ 08:00.';
		} else if (dep < earliest) {
			errors.dep = earliestMessage(earliest);
		}
		if (draft.arrTouched && !arr) {
			errors.arr = 'Giờ không hợp lệ. Nhập dạng 24 giờ, ví dụ 09:45.';
		} else if (arr && dep && arr <= dep) {
			errors.arr = 'Giờ đến phải sau giờ đi.';
		}
		if (Number.isNaN(price)) {
			errors.price = 'Nhập giá cơ bản.';
		} else if (price % 1000 !== 0) {
			errors.price = 'Giá làm tròn tới nghìn đồng.';
		}
		Object.keys(serverErrors).forEach(function (slot) {
			errors[slot] = errors[slot] || serverErrors[slot];
		});
		return { earliest: earliest, dest: dest, dep: dep, arr: arr, autoArr: autoArr, price: price, errors: errors };
	}

	/** Same wording as TripCreationService: the turnaround after the last arrival, or simply the present. */
	function earliestMessage(earliest) {
		const last = options.chained && options.lastTrips.length
			? D.parseIso(options.lastTrips[options.lastTrips.length - 1].arrivalAt)
			: null;
		if (last && last.getTime() + D.TURNAROUND_MIN * 60000 === earliest.getTime()) {
			return 'Sớm nhất ' + D.fmtDayTime(earliest) + ': tàu đến ' + options.origin.name + ' lúc '
					+ D.fmtTime(last) + ' và cần ' + D.TURNAROUND_MIN + ' phút quay đầu.';
		}
		return 'Giờ khởi hành phải từ ' + D.fmtDayTime(earliest) + ' trở đi.';
	}

	function shows(slot) {
		const owner = STEP_ERRORS.findIndex(function (slots) {
			return slots.indexOf(slot) >= 0;
		});
		return tried[owner] || touched[slot] || serverErrors[slot];
	}

	/** The admin changed what an error slot is about: its server message no longer applies. */
	function edited(slot) {
		touched[slot] = true;
		delete serverErrors[slot];
	}

	/* ---------- loading ---------- */

	async function load(originId) {
		const url = root.dataset.nextLegUrl + (originId ? '?origin=' + originId : '');
		try {
			options = await TG.api(url);
		} catch (error) {
			TG.toast(error.message, { error: true });
			return false;
		}
		title.textContent = (options.chained ? 'Thêm chuyến cho ' : 'Chuyến đầu tiên của ') + trainCode;
		preview.hidden = false;
		return true;
	}

	async function start() {
		if (!(await load(null))) {
			body.replaceChildren(textLine('Không tải được lịch tàu. Tải lại trang để thử lại.'));
			return;
		}
		draft = freshDraft();
		go(0);
	}

	/* ---------- drawing ---------- */

	function textLine(text) {
		const p = document.createElement('p');
		p.className = 'text-body-secondary mb-0';
		p.textContent = text;
		return p;
	}

	function q(selector) {
		return body.querySelector(selector);
	}

	function el(tag, className, text) {
		const node = document.createElement(tag);
		if (className) {
			node.className = className;
		}
		if (text != null) {
			node.textContent = text;
		}
		return node;
	}

	function icon(name) {
		const i = el('i', 'bi bi-' + name);
		i.setAttribute('aria-hidden', 'true');
		return i;
	}

	/** Opens step i: draws it from its template, then focuses its first control. */
	function go(i, focusSelector) {
		step = i;
		body.replaceChildren(document.getElementById(STEP_TEMPLATES[i]).content.cloneNode(true));
		body.append(document.getElementById('wz-actions').content.cloneNode(true));
		[drawWhere, drawWhen, drawPrice, drawConfirm][i]();
		wireActions();
		update();
		const target = (focusSelector && q(focusSelector))
				|| Array.from(body.querySelectorAll('[aria-pressed="true"], input, button')).find(function (node) {
					return !node.disabled && node.offsetParent !== null;
				});
		if (target) {
			target.focus({ preventScroll: true });
		}
	}

	function wireActions() {
		const prev = q('[data-wz-prev]');
		const next = q('[data-wz-next]');
		prev.hidden = step === 0;
		prev.addEventListener('click', function () {
			go(step - 1);
		});
		if (step === STEPS.length - 1) {
			next.append('Thêm chuyến', icon('check-lg'));
			next.addEventListener('click', add);
		} else {
			next.append('Tiếp tục', icon('arrow-right'));
			next.addEventListener('click', function () {
				tried[step] = true;
				const errors = derive().errors;
				if (STEP_ERRORS[step].some(function (slot) {
					return errors[slot];
				})) {
					update();
					return;
				}
				go(step + 1);
			});
		}
	}

	function drawSteps() {
		stepsBar.replaceChildren();
		STEPS.forEach(function (label, i) {
			const li = el('li', i < step ? 'done' : (i === step ? 'now' : ''));
			let inner;
			if (i < step) {
				inner = el('button');
				inner.type = 'button';
				inner.append(el('span', 'n'), label);
				inner.firstChild.append(icon('check-lg'));
				inner.addEventListener('click', function () {
					go(i);
				});
			} else {
				inner = el('span');
				inner.append(el('span', 'n', String(i + 1)), label);
			}
			if (i === step) {
				li.setAttribute('aria-current', 'step');
			}
			li.append(inner);
			stepsBar.append(li);
		});
	}

	/* Step 1: where the leg goes. */
	function drawWhere() {
		const origin = options.origin;
		if (!options.chained) {
			q('[data-origin-field]').hidden = false;
			const chips = q('[data-origins]');
			options.origins.forEach(function (station) {
				const chip = el('button', 'chip', station.name);
				chip.type = 'button';
				chip.setAttribute('aria-pressed', String(!!origin && origin.stationId === station.stationId));
				chip.addEventListener('click', function () {
					pickOrigin(station.stationId);
				});
				chips.append(chip);
			});
		} else {
			q('[data-standing]').hidden = false;
			q('[data-standing-name]').textContent = origin.name;
		}
		if (!origin) {
			return;
		}
		q('[data-dest-wrap]').hidden = false;
		q('[data-dests-label]').hidden = options.chained || !options.destinations.length;
		const dests = q('[data-dests]');
		options.destinations.forEach(function (d) {
			const card = el('button', 'dest');
			card.type = 'button';
			card.dataset.station = d.stationId;
			card.append(el('span', 'dir-hint', d.side), el('b', null, d.name),
					el('small', null, d.distanceKm + ' km · ' + duration(d.minutes)));
			card.addEventListener('click', function () {
				if (draft.destination !== d.stationId) {
					// A new destination brings its own suggested arrival and price back.
					draft.destination = d.stationId;
					draft.arrTouched = false;
					draft.arrOpen = false;
					draft.priceTouched = false;
				}
				edited('where');
				go(1);
			});
			dests.append(card);
		});
		dests.hidden = !options.destinations.length;
		if (!options.destinations.length) {
			q('[data-no-route]').hidden = false;
			q('[data-no-route-text]').textContent = 'Từ ' + origin.name + ' chưa có tuyến nào.';
		}
		if (options.withoutRoute.length) {
			const more = q('[data-more]');
			const list = q('[data-missing]');
			more.hidden = false;
			q('[data-more-label]').textContent = options.withoutRoute.length + ' ga chưa có tuyến từ ' + origin.name;
			options.withoutRoute.forEach(function (station) {
				const li = el('li');
				li.append(el('span', null, station.name), el('small', 'text-body-secondary', ' · ' + station.distanceKm + ' km'));
				list.append(li);
			});
			// With no route at all, the list is the only thing to read, so it starts open.
			const open = draft.showMissing || !options.destinations.length;
			list.hidden = !open;
			more.setAttribute('aria-expanded', String(open));
			more.querySelector('.bi').className = 'bi bi-chevron-' + (open ? 'up' : 'down');
			more.addEventListener('click', function () {
				draft.showMissing = list.hidden;
				list.hidden = !draft.showMissing;
				more.setAttribute('aria-expanded', String(draft.showMissing));
				more.querySelector('.bi').className = 'bi bi-chevron-' + (draft.showMissing ? 'up' : 'down');
			});
		}
	}

	async function pickOrigin(stationId) {
		if (options.origin && options.origin.stationId === stationId) {
			return;
		}
		if (!(await load(stationId))) {
			return;
		}
		draft.destination = null;
		draft.priceTouched = false;
		draft.arrTouched = false;
		draft.arrOpen = false;
		draft.showMissing = false;
		// A new origin is no answer yet to "where to", so the slot stays untouched.
		delete serverErrors.where;
		go(0, '[data-origins] [aria-pressed="true"]');
	}

	/* Step 2: when it leaves and arrives. */
	function drawWhen() {
		const v = derive();
		q('[data-question]').textContent = options.origin.name + ' → ' + v.dest.name + ' khởi hành lúc nào?';
		q('[data-day-prev]').addEventListener('click', function () {
			draft.page = 0;
			drawDays();
			update();
		});
		q('[data-day-next]').addEventListener('click', function () {
			draft.page = 1;
			drawDays();
			update();
		});
		drawDays();

		const dep = q('[data-dep]');
		dep.value = draft.depTime;
		const commitDep = function () {
			const time = normalizeTime(dep.value);
			draft.depTime = time || dep.value.trim();
			if (time) {
				dep.value = time;
			}
			edited('dep');
			edited('arr');
			update();
		};
		dep.addEventListener('change', commitDep);
		dep.addEventListener('keydown', function (event) {
			if (event.key === 'Enter') {
				commitDep();
			}
		});
		const chips = q('[data-dep-chips]');
		const earliestChip = el('button', 'chip');
		earliestChip.type = 'button';
		earliestChip.dataset.earliest = '';
		chips.append(earliestChip);
		QUICK_TIMES.forEach(function (time) {
			const chip = el('button', 'chip', time);
			chip.type = 'button';
			chip.dataset.time = time;
			chips.append(chip);
		});
		chips.addEventListener('click', function (event) {
			const chip = event.target.closest('.chip');
			if (!chip || chip.disabled) {
				return;
			}
			draft.depTime = chip.dataset.time;
			dep.value = draft.depTime;
			edited('dep');
			edited('arr');
			update();
		});

		q('[data-arr-edit]').addEventListener('click', function () {
			draft.arrOpen = true;
			update();
			q('[data-arr]').focus();
		});
		const arr = q('[data-arr]');
		const commitArr = function () {
			const shown = derive();
			const time = normalizeTime(arr.value);
			if (!draft.arrTouched) {
				draft.arrOffset = shown.autoArr ? D.daysBetween(shown.dep, shown.autoArr) : 0;
			}
			draft.arrTouched = true;
			draft.arrTime = time || arr.value.trim();
			if (time) {
				arr.value = time;
			}
			edited('arr');
			update();
		};
		arr.addEventListener('change', commitArr);
		arr.addEventListener('keydown', function (event) {
			if (event.key === 'Enter') {
				commitArr();
			}
		});
		const shift = function (days) {
			const shown = derive();
			if (!draft.arrTouched) {
				draft.arrTime = shown.autoArr ? D.fmtTime(shown.autoArr) : '';
				draft.arrOffset = shown.autoArr ? D.daysBetween(shown.dep, shown.autoArr) : 0;
			}
			draft.arrTouched = true;
			draft.arrOffset = Math.max(0, draft.arrOffset + days);
			edited('arr');
			update();
		};
		q('[data-arr-minus]').addEventListener('click', function () {
			shift(-1);
		});
		q('[data-arr-plus]').addEventListener('click', function () {
			shift(1);
		});
		q('[data-arr-reset]').addEventListener('click', function () {
			draft.arrTouched = false;
			draft.arrOpen = false;
			edited('arr');
			update();
			q('[data-arr-edit]').focus();
		});
	}

	/** The 14 days from the earliest one, seven at a time. */
	function drawDays() {
		const first = D.startOfDay(D.parseIso(options.earliestDeparture));
		if (draft.page == null) {
			draft.page = D.daysBetween(first, draft.day) >= DAYS_SHOWN ? 1 : 0;
		}
		const today = new Date();
		const strip = q('[data-days]');
		strip.replaceChildren();
		for (let i = 0; i < DAYS_SHOWN; i++) {
			const day = D.addDays(first, draft.page * DAYS_SHOWN + i);
			const button = el('button');
			button.type = 'button';
			button.dataset.day = D.toIso(day);
			button.setAttribute('aria-label', WEEKDAYS[day.getDay()] + ' ' + D.fmtDayTime(day).slice(0, 5));
			button.append(el('small', null, sameDay(day, today) ? 'Hôm nay' : SHORT_WEEKDAYS[day.getDay()]),
					el('b', null, D.fmtDayTime(day).slice(0, 5)));
			button.addEventListener('click', function () {
				draft.day = day;
				edited('dep');
				edited('arr');
				update();
			});
			strip.append(button);
		}
		q('[data-day-prev]').disabled = draft.page === 0;
		q('[data-day-next]').disabled = draft.page * DAYS_SHOWN + DAYS_SHOWN >= DAYS_OFFERED;
	}

	/* Step 3: the price. */
	function drawPrice() {
		const input = q('[data-price]');
		const v = derive();
		input.value = Number.isNaN(v.price) ? '' : money.format(v.price);
		input.addEventListener('input', function () {
			const digits = input.value.replace(/\D/g, '');
			draft.price = digits ? Number(digits) : NaN;
			draft.priceTouched = true;
			edited('price');
			update();
		});
		input.addEventListener('change', function () {
			input.value = Number.isNaN(draft.price) ? '' : money.format(draft.price);
		});
		q('[data-price-reset]').addEventListener('click', function () {
			draft.priceTouched = false;
			edited('price');
			input.value = money.format(derive().price);
			update();
			input.focus();
		});
	}

	/* Step 4: the leg as it will be added. */
	function drawConfirm() {
		const v = derive();
		q('[data-confirm-route]').textContent = options.origin.name + ' → ' + v.dest.name;
		q('[data-confirm-time]').textContent = dayTime(v.dep) + ' → '
				+ (sameDay(v.dep, v.arr) ? D.fmtTime(v.arr) : dayTime(v.arr));
		q('[data-confirm-price]').textContent = vnd(v.price);
		q('[data-confirm-sum]').textContent = '1 chuyến, ' + money.format(v.dest.distanceKm) + ' km. Mỗi chuyến mở bán '
				+ seatsPerTrip + ' chỗ.';
	}

	/** Writes an error slot and ties it to its control for screen readers. */
	function showError(slot, message, control) {
		const box = q('[data-err="' + slot + '"]');
		if (!box) {
			return;
		}
		box.replaceChildren();
		box.hidden = !message;
		if (message) {
			box.append(icon('exclamation-circle'), el('span', null, message));
		}
		if (control) {
			control.classList.toggle('is-invalid', !!message);
			if (message) {
				control.setAttribute('aria-invalid', 'true');
				control.setAttribute('aria-describedby', box.id);
			} else {
				control.removeAttribute('aria-invalid');
				control.removeAttribute('aria-describedby');
			}
		}
	}

	/** Brings the open step, the steps bar and the preview in line with the draft. */
	function update() {
		const v = derive();
		drawSteps();
		drawPreview(v);
		const errorOf = function (slot) {
			return shows(slot) ? v.errors[slot] : null;
		};

		if (step === 0) {
			body.querySelectorAll('[data-dests] .dest').forEach(function (card) {
				card.setAttribute('aria-pressed', String(Number(card.dataset.station) === draft.destination));
			});
			showError('where', errorOf('where'));
		} else if (step === 1) {
			body.querySelectorAll('[data-days] button').forEach(function (button) {
				const day = D.parseIso(button.dataset.day);
				button.disabled = day < D.startOfDay(v.earliest);
				button.setAttribute('aria-pressed', String(sameDay(day, draft.day)));
			});
			const earliestChip = q('[data-earliest]');
			const earliestTime = D.fmtTime(v.earliest);
			earliestChip.hidden = !sameDay(draft.day, v.earliest) || earliestTime === '08:00';
			earliestChip.dataset.time = earliestTime;
			earliestChip.textContent = 'Sớm nhất ' + earliestTime;
			body.querySelectorAll('[data-dep-chips] .chip').forEach(function (chip) {
				chip.disabled = D.atTime(draft.day, chip.dataset.time) < v.earliest;
				chip.setAttribute('aria-pressed', String(chip.dataset.time === draft.depTime));
			});
			showError('dep', errorOf('dep'), q('[data-dep]'));

			const open = draft.arrOpen || draft.arrTouched;
			const ran = v.arr && v.dep && v.arr > v.dep ? 'Chạy ' + duration(Math.round((v.arr - v.dep) / 60000)) : '';
			q('[data-arr-locked]').hidden = open;
			q('[data-arr-open]').hidden = !open;
			body.querySelectorAll('[data-arr-label]').forEach(function (label) {
				label.textContent = 'Đến ' + v.dest.name;
			});
			q('[data-arr-text]').textContent = v.arr ? D.dayLabel(v.arr) + ' · ' + D.fmtTime(v.arr) : '';
			q('[data-arr-edit]').hidden = !v.arr;
			q('[data-arr-hint]').textContent = ran ? ran + ', tính theo 60 km/h.' : '';
			const arrInput = q('[data-arr]');
			const offset = draft.arrTouched ? draft.arrOffset : (v.autoArr ? D.daysBetween(v.dep, v.autoArr) : 0);
			if (document.activeElement !== arrInput) {
				arrInput.value = draft.arrTouched ? draft.arrTime : (v.autoArr ? D.fmtTime(v.autoArr) : '');
			}
			q('[data-arr-offset]').textContent = offset === 0 ? 'Cùng ngày' : '+' + offset + ' ngày';
			q('[data-arr-minus]').disabled = offset === 0;
			const openHint = q('[data-arr-open-hint]');
			openHint.textContent = ran;
			openHint.hidden = !ran || !!errorOf('arr');
			const reset = q('[data-arr-reset]');
			reset.hidden = !v.autoArr;
			reset.textContent = v.autoArr ? 'Dùng giờ gợi ý ' + D.fmtTime(v.autoArr) : '';
			showError('arr', errorOf('arr'), open ? arrInput : null);
		} else if (step === 2) {
			q('[data-price-reset]').hidden = !draft.priceTouched;
			q('[data-price-hint]').textContent = 'Gợi ý ' + vnd(v.dest.suggestedPrice) + ' theo 1.000đ/km.';
			showError('price', errorOf('price'), q('[data-price]'));
		} else if (step === 3) {
			q('[data-wz-next]').disabled = saving;
		}
	}

	function drawPreview(v) {
		previewList.replaceChildren();
		options.lastTrips.forEach(function (trip) {
			const li = el('li', 'old');
			li.append(el('span', null, dayTime(D.parseIso(trip.departureAt))),
					el('b', null, trip.originName + ' → ' + trip.destinationName));
			previewList.append(li);
		});
		const added = el('li', 'new');
		added.append(el('span', null, v.dest && v.dep ? dayTime(v.dep) : 'Chưa chọn giờ'),
				el('b', null, (options.origin ? options.origin.name : '?') + ' → ' + (v.dest ? v.dest.name : '?')));
		previewList.append(added);
		const after = v.dest && v.arr && v.dep && v.arr > v.dep ? 'Sau đó tàu ở ' + v.dest.name + ' từ ' + dayTime(v.arr) + '.' : '';
		previewAfter.textContent = after;
		previewAfter.hidden = !after;
	}

	/* ---------- adding ---------- */

	async function add() {
		const v = derive();
		if (saving) {
			return;
		}
		saving = true;
		update();
		const leg = {
			// The server compares this with where the train stands now, so a schedule
			// another admin changed meanwhile is refused instead of chained wrongly.
			originStationId: options.origin.stationId,
			destinationStationId: v.dest.stationId,
			departureAt: D.toIso(v.dep),
			arrivalAt: D.toIso(v.arr),
			basePrice: v.price
		};
		try {
			await TG.api(root.dataset.tripsUrl, { method: 'POST', body: { legs: [leg] } });
		} catch (error) {
			saving = false;
			let first = null;
			Object.keys(error.fields || {}).forEach(function (key) {
				const match = LEG_FIELD.exec(key);
				const slot = match && FIELD_SLOT[match[1]];
				if (slot) {
					serverErrors[slot] = error.fields[key];
					const owner = STEP_ERRORS.findIndex(function (slots) {
						return slots.indexOf(slot) >= 0;
					});
					first = first == null ? owner : Math.min(first, owner);
				}
			});
			if (first == null) {
				TG.toast(error.message, { error: true });
				update();
				return;
			}
			go(first);
			return;
		}
		saving = false;
		done(options.origin.name, v.dest, v.arr);
	}

	function done(originName, dest, arrival) {
		stepsBar.hidden = true;
		preview.hidden = true;
		body.classList.add('is-done');
		body.replaceChildren(document.getElementById('wz-done').content.cloneNode(true));
		q('[data-done-title]').textContent = 'Đã thêm chuyến ' + originName + ' → ' + dest.name;
		q('[data-done-text]').textContent = 'Tàu ở ' + dest.name + ' từ ' + dayTime(arrival) + '.';
		q('[data-done-again-label]').textContent = 'Thêm chuyến từ ' + dest.name;
		const back = q('[data-done-back]');
		back.href = backLink.href;
		back.textContent = 'Về tàu ' + trainCode;
		q('[data-done-again]').addEventListener('click', async function () {
			if (!(await load(null))) {
				return;
			}
			draft = freshDraft();
			tried = [];
			touched = {};
			serverErrors = {};
			stepsBar.hidden = false;
			body.classList.remove('is-done');
			go(0);
		});
		q('.wz-done').focus();
	}

	start();
}());
