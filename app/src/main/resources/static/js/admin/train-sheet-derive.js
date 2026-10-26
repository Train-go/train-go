/*
 * The rules of the train page's "Bảng tính" view, as pure functions with no
 * DOM, exported as window.TrainSheetDerive. train-sheet.js draws what
 * derive() returns.
 *
 * Draft rows chain: every row starts at the station where the one above ends,
 * and its earliest departure is that row's arrival plus the turnaround. Any
 * time or price the admin has typed (day, time, arrTouched, priceTouched)
 * stays as typed; everything else is recomputed from the chain on every
 * change. The server checks the same rules again when the legs are saved
 * (docs/decisions/0011-chuyen-tao-tu-tau-noi-chang.md).
 *
 * Dates are plain Date objects holding the server's wall-clock time (the
 * server sends LocalDateTime without a zone), so they are only ever built
 * from and printed as local components.
 */
window.TrainSheetDerive = (function () {
	'use strict';

	/** Minutes a train needs at a station before its next leg; TrainScheduleService.TURNAROUND. */
	const TURNAROUND_MIN = 30;
	const SHORT_WEEKDAYS = ['CN', 'T2', 'T3', 'T4', 'T5', 'T6', 'T7'];
	const TIME_PATTERN = /^([01]\d|2[0-3]):[0-5]\d$/;
	const DAY_MS = 86400000;

	/* ---------- dates ---------- */

	function pad(n) {
		return n < 10 ? '0' + n : String(n);
	}

	/** "2026-10-27T08:00" (or with seconds) to a Date. */
	function parseIso(text) {
		const m = /^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2})/.exec(text || '');
		return m ? new Date(+m[1], +m[2] - 1, +m[3], +m[4], +m[5]) : null;
	}

	/** A Date to "2026-10-27T08:00", the shape the server reads. */
	function toIso(d) {
		return d.getFullYear() + '-' + pad(d.getMonth() + 1) + '-' + pad(d.getDate()) + 'T' + pad(d.getHours()) + ':'
				+ pad(d.getMinutes());
	}

	function addMinutes(d, minutes) {
		return new Date(d.getTime() + minutes * 60000);
	}

	function startOfDay(d) {
		return new Date(d.getFullYear(), d.getMonth(), d.getDate());
	}

	function addDays(d, days) {
		return new Date(d.getFullYear(), d.getMonth(), d.getDate() + days, d.getHours(), d.getMinutes());
	}

	/** Whole days between two dates' calendar days. */
	function daysBetween(from, to) {
		return Math.round((startOfDay(to) - startOfDay(from)) / DAY_MS);
	}

	/** The day with a "HH:mm" time, or null when the time is not valid. */
	function atTime(day, time) {
		if (!TIME_PATTERN.test(time || '')) {
			return null;
		}
		return new Date(day.getFullYear(), day.getMonth(), day.getDate(), +time.slice(0, 2), +time.slice(3));
	}

	/** The first 08:00 that is not before d. */
	function nextEight(d) {
		const same = new Date(d.getFullYear(), d.getMonth(), d.getDate(), 8, 0);
		return same >= d ? same : addDays(same, 1);
	}

	function fmtTime(d) {
		return pad(d.getHours()) + ':' + pad(d.getMinutes());
	}

	function fmtDayTime(d) {
		return pad(d.getDate()) + '/' + pad(d.getMonth() + 1) + ' ' + fmtTime(d);
	}

	/** "T5 22/10". */
	function dayLabel(d) {
		return SHORT_WEEKDAYS[d.getDay()] + ' ' + pad(d.getDate()) + '/' + pad(d.getMonth() + 1);
	}

	/* ---------- suggestions: same formulas as TrainScheduleService ---------- */

	function suggestedMinutes(km) {
		return Math.max(5, Math.round(km / 5) * 5);
	}

	function suggestedPrice(km) {
		return Math.max(10000, Math.round(km * 1000 / 10000) * 10000);
	}

	/* ---------- drafts ---------- */

	let nextId = 1;

	/**
	 * A fresh draft row. pace picks the automatic departure: DAILY is the next
	 * 08:00, NONSTOP is right after the turnaround.
	 */
	function blank(extra) {
		return Object.assign({
			id: nextId++, to: null, day: null, time: '', pace: 'DAILY', arrTime: '', arrOff: 0, arrTouched: false,
			price: NaN, priceTouched: false, touched: false
		}, extra || {});
	}

	/**
	 * Recomputes every row from the chain.
	 *
	 * ctx: { chainOrigin (station id the train stands at, or null), origin0 (the
	 * first station when the train has no trip yet), earliest (Date), chainArrival
	 * (Date of the last trip's arrival, or null), kmBetween(fromId, toId) -> km or
	 * null, stationName(id) -> string }
	 *
	 * Returns one object per row: { x (the draft), origin, km, floor, depDay,
	 * depTime, dep, arr, autoArr, price, errors }.
	 */
	function derive(ctx, drafts) {
		let origin = ctx.chainOrigin || ctx.origin0 || null;
		let floor = ctx.earliest;
		let prevArr = ctx.chainArrival;
		return drafts.map(function (x) {
			const km = origin && x.to ? ctx.kmBetween(origin, x.to) : null;
			const autoDep = x.pace === 'NONSTOP' ? floor : nextEight(floor);
			const depDay = x.day != null ? new Date(x.day) : startOfDay(autoDep);
			const depTime = x.time || fmtTime(autoDep);
			const dep = atTime(depDay, depTime);
			const autoArr = km != null && dep ? addMinutes(dep, suggestedMinutes(km)) : null;
			const arr = x.arrTouched
				? (dep ? atTime(addDays(startOfDay(dep), x.arrOff), x.arrTime) : null)
				: autoArr;
			const price = x.priceTouched ? x.price : (km != null ? suggestedPrice(km) : NaN);

			const errors = [];
			if (!origin) {
				errors.push('Chọn ga xuất phát.');
			}
			if (!x.to) {
				errors.push('Chọn ga đến.');
			} else if (origin && km == null) {
				errors.push('Chưa có tuyến ' + ctx.stationName(origin) + ' → ' + ctx.stationName(x.to) + '.');
			}
			if (!dep) {
				errors.push('Giờ đi không hợp lệ, nhập dạng 24 giờ như 08:00.');
			} else if (dep < floor) {
				errors.push(prevArr
					? 'Đi sớm nhất ' + fmtDayTime(floor) + ': tàu đến ' + ctx.stationName(origin) + ' lúc '
						+ fmtTime(prevArr) + ' và cần ' + TURNAROUND_MIN + ' phút quay đầu.'
					: 'Giờ đi phải sau ' + fmtDayTime(floor) + '.');
			}
			if (x.arrTouched && !arr) {
				errors.push('Giờ đến không hợp lệ.');
			} else if (arr && dep && arr <= dep) {
				errors.push('Giờ đến phải sau giờ đi.');
			}
			if (Number.isNaN(price)) {
				if (km != null) {
					errors.push('Nhập giá cơ bản.');
				}
			} else if (price < 0) {
				errors.push('Giá không được âm.');
			} else if (price % 1000 !== 0) {
				errors.push('Giá làm tròn tới nghìn đồng.');
			}

			const row = { x: x, origin: origin, km: km, floor: floor, depDay: depDay, depTime: depTime, dep: dep,
				arr: arr, autoArr: autoArr, price: price, errors: errors };
			if (x.to) {
				origin = x.to;
			}
			if (arr) {
				prevArr = arr;
				floor = addMinutes(arr, TURNAROUND_MIN);
			}
			return row;
		});
	}

	/** The station a train stands at after the rows that have a destination. */
	function lastOrigin(ctx, rows) {
		let origin = ctx.chainOrigin || ctx.origin0 || null;
		rows.forEach(function (row) {
			if (row.x.to) {
				origin = row.x.to;
			}
		});
		return origin;
	}

	/** The earliest departure for a row added after {@code rows}. */
	function nextFloor(ctx, rows) {
		let floor = ctx.earliest;
		rows.forEach(function (row) {
			if (row.arr) {
				floor = addMinutes(row.arr, TURNAROUND_MIN);
			}
		});
		return floor;
	}

	return {
		TURNAROUND_MIN: TURNAROUND_MIN, TIME_PATTERN: TIME_PATTERN,
		parseIso: parseIso, toIso: toIso, startOfDay: startOfDay, addDays: addDays, daysBetween: daysBetween,
		nextEight: nextEight, fmtTime: fmtTime, fmtDayTime: fmtDayTime, dayLabel: dayLabel,
		suggestedMinutes: suggestedMinutes, suggestedPrice: suggestedPrice,
		blank: blank, derive: derive, lastOrigin: lastOrigin, nextFloor: nextFloor
	};
}());
