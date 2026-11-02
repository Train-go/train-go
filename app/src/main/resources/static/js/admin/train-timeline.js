/*
 * The train page, view "Theo ngày": the calendar of "Lịch theo giờ", drawn by
 * FullCalendar (static/vendor/fullcalendar). The bar above it picks a date
 * (‹ ›, "Hôm nay", and a button that names the range in dd/mm and opens the
 * browser's own date picker) and a span (Ngày, Tuần, Tháng):
 *
 *   Ngày   the picked day, hours running down
 *   Tuần   seven days from the picked day, a column each
 *   Tháng  the picked day's month, a cell per day with its first trips and
 *          "+N chuyến"; that link and each day number open the day
 *
 * On a narrow screen Tuần and Tháng become a list by day, because seven
 * columns do not fit a phone. The trips come from
 * GET /api/admin/trains/{id}/timeline.
 *
 * Trips that have run and cancelled trips are hidden until their toggle asks
 * for them. A click (or Enter) on a trip opens the trip sheet (TG.openTrip,
 * trip-sheet.js); on the dashed "Chuyến mới" block it opens the trip wizard.
 */
(function () {
	'use strict';

	const root = document.getElementById('hour-chart');
	if (!root || !window.FullCalendar) {
		return;
	}
	const canvas = root.querySelector('[data-tl-canvas]');
	const errorBox = root.querySelector('[data-tl-error]');
	const alertList = root.querySelector('[data-tl-alerts]');
	const title = root.querySelector('[data-tl-title]');
	const dateField = root.querySelector('[data-tl-date]');
	const pickButton = root.querySelector('[data-tl-pick]');
	const modeField = root.querySelector('[data-tl-mode]');
	const toggleHost = document.querySelector('[data-tl-toggles]');
	const HOUR = 3600 * 1000;
	const SLOT_ID = 'slot';
	const NARROW = 560;
	const MODE_KEY = 'tg.train-calendar.mode';
	const MODES = ['day', 'week', 'month'];
	const SHORT_DAYS = ['CN', 'T2', 'T3', 'T4', 'T5', 'T6', 'T7'];
	const LONG_DAYS = ['Chủ nhật', 'Thứ Hai', 'Thứ Ba', 'Thứ Tư', 'Thứ Năm', 'Thứ Sáu', 'Thứ Bảy'];

	let calendar = null;
	let items = [];
	let mode = savedMode();
	let anchor = startOfDay(new Date());
	let narrow = false;
	let showPast = false;
	let showCancelled = false;

	function element(tag, className, text) {
		const node = document.createElement(tag);
		if (className) {
			node.className = className;
		}
		if (text !== undefined) {
			node.textContent = text;
		}
		return node;
	}

	function pad(n) {
		return String(n).padStart(2, '0');
	}

	function dayMonth(date) {
		return pad(date.getDate()) + '/' + pad(date.getMonth() + 1);
	}

	function startOfDay(date) {
		return new Date(date.getFullYear(), date.getMonth(), date.getDate());
	}

	/* "2026-11-02", the value of a date field. */
	function isoDay(date) {
		return date.getFullYear() + '-' + pad(date.getMonth() + 1) + '-' + pad(date.getDate());
	}

	function savedMode() {
		try {
			const saved = localStorage.getItem(MODE_KEY);
			return MODES.indexOf(saved) >= 0 ? saved : 'week';
		} catch (e) {
			return 'week';
		}
	}

	function saveMode() {
		try {
			localStorage.setItem(MODE_KEY, mode);
		} catch (e) {
			// The span is a convenience; without storage the page opens on Tuần.
		}
	}

	/* The inside of a trip in the hour grid and the list: the hours, the route, the seats sold and a line for them. */
	function legNode(leg, withFill) {
		const node = element('div', 'tl-ev');
		const hours = element('b', null, leg.time);
		hours.append(element('span', 'tl-until', ' – ' + leg.until));
		node.append(hours, element('span', 'tl-route', leg.route));
		if (leg.sold) {
			node.append(element('small', null, leg.sold));
		}
		if (withFill && leg.fill !== null && leg.fill !== undefined) {
			const fill = element('i', 'tl-fill');
			fill.style.setProperty('--p', leg.fill + '%');
			node.append(fill);
		}
		return node;
	}

	/* A trip in a month cell: one line, the departure then the route. */
	function lineNode(leg) {
		const node = element('div', 'tl-ev tl-line');
		node.append(element('b', null, leg.time), element('span', null, leg.route));
		return node;
	}

	function slotNode(slot) {
		const node = element('div', 'tl-ev tl-slot');
		const icon = element('i', 'bi bi-plus-lg');
		icon.setAttribute('aria-hidden', 'true');
		node.append(icon, element('span', null, slot.label));
		return node;
	}

	function itemsOf(data) {
		const list = data.legs.map(function (leg) {
			return {
				id: 'trip-' + leg.tripId,
				start: leg.start,
				end: leg.end,
				classNames: leg.css.split(' '),
				extendedProps: { leg: leg }
			};
		});
		if (data.slot) {
			list.push({
				id: SLOT_ID,
				start: data.slot.at,
				end: new Date(new Date(data.slot.at).getTime() + HOUR),
				classNames: ['is-slot'],
				extendedProps: { slot: data.slot }
			});
		}
		return list;
	}

	function visible(item) {
		const leg = item.extendedProps.leg;
		if (!leg) {
			return true;
		}
		if (leg.cancelled) {
			return showCancelled;
		}
		return !leg.past || showPast;
	}

	function viewName() {
		if (mode === 'day') {
			return 'grid1';
		}
		if (mode === 'week') {
			return narrow ? 'list7' : 'grid7';
		}
		return narrow ? 'listMonth' : 'dayGridMonth';
	}

	/* "Thứ Hai, 02/11/2026", "02/11 – 08/11/2026" or "Tháng 11/2026". */
	function rangeLabel(view) {
		const start = view.currentStart;
		const last = new Date(view.currentEnd.getTime() - 1);
		if (mode === 'day') {
			return LONG_DAYS[start.getDay()] + ', ' + dayMonth(start) + '/' + start.getFullYear();
		}
		if (mode === 'month') {
			return 'Tháng ' + pad(start.getMonth() + 1) + '/' + start.getFullYear();
		}
		return dayMonth(start) + ' – ' + dayMonth(last) + '/' + last.getFullYear();
	}

	/*
	 * Bring the first shown trip of the range into sight. A range without one
	 * opens at the present hour when it holds today, else at 06:00.
	 */
	function scrollToFirstTrip() {
		const view = calendar.view;
		if (view.type.indexOf('grid') !== 0) {
			return;
		}
		const hours = items.filter(visible).map(function (item) {
			return new Date(item.start);
		}).filter(function (start) {
			return start >= view.activeStart && start < view.activeEnd;
		}).map(function (start) {
			return start.getHours();
		});
		const now = new Date();
		const hour = hours.length ? Math.min.apply(null, hours)
			: now >= view.activeStart && now < view.activeEnd ? now.getHours() : 7;
		calendar.scrollToTime({ hours: Math.max(0, hour - 1) });
	}

	/* Draws the picked span around the picked day, and keeps the bar in step. */
	function show() {
		const name = viewName();
		calendar.setOption('contentHeight', name.indexOf('grid') === 0 ? 560 : 'auto');
		calendar.changeView(name, isoDay(anchor));
		dateField.value = isoDay(anchor);
		modeField.value = mode;
	}

	/* ‹ and ›: a day, seven days or a month back or forward. */
	function step(direction) {
		if (mode === 'month') {
			const target = new Date(anchor.getFullYear(), anchor.getMonth() + direction, 1);
			const lastDay = new Date(target.getFullYear(), target.getMonth() + 1, 0).getDate();
			anchor = new Date(target.getFullYear(), target.getMonth(), Math.min(anchor.getDate(), lastDay));
		} else {
			anchor = new Date(anchor.getFullYear(), anchor.getMonth(), anchor.getDate() + direction * (mode === 'week' ? 7 : 1));
		}
		show();
	}

	function openDay(date) {
		mode = 'day';
		anchor = startOfDay(date);
		saveMode();
		show();
	}

	/* A toggle for one kind of hidden trip, shown only when there is one. */
	function addToggle(count, hiddenLabel, shownLabel, iconName, onChange) {
		if (count === 0) {
			return;
		}
		const button = element('button', 'btn btn-link btn-sm tr-past');
		button.type = 'button';
		button.setAttribute('aria-pressed', 'false');
		const icon = element('i', 'bi bi-' + iconName);
		icon.setAttribute('aria-hidden', 'true');
		const label = element('span', null, hiddenLabel.replace('{n}', count));
		button.append(icon, label);
		button.addEventListener('click', function () {
			const on = button.getAttribute('aria-pressed') !== 'true';
			button.setAttribute('aria-pressed', String(on));
			label.textContent = (on ? shownLabel : hiddenLabel).replace('{n}', count);
			onChange(on);
			calendar.refetchEvents();
			requestAnimationFrame(scrollToFirstTrip);
		});
		toggleHost.append(button);
	}

	function render(data) {
		items = itemsOf(data);
		narrow = canvas.clientWidth < NARROW;
		calendar = new FullCalendar.Calendar(canvas, {
			initialView: viewName(),
			initialDate: isoDay(anchor),
			views: {
				grid1: { type: 'timeGrid', duration: { days: 1 } },
				grid7: { type: 'timeGrid', duration: { days: 7 } },
				list7: { type: 'list', duration: { days: 7 } }
			},
			headerToolbar: false,
			firstDay: 1,
			allDaySlot: false,
			nowIndicator: true,
			navLinks: true,
			navLinkDayClick: openDay,
			slotDuration: '01:00:00',
			slotEventOverlap: false,
			eventMinHeight: 22,
			eventShortHeight: 40,
			contentHeight: viewName().indexOf('grid') === 0 ? 560 : 'auto',
			expandRows: false,
			displayEventTime: false,
			eventDisplay: 'block',
			eventInteractive: true,
			dayMaxEvents: 3,
			moreLinkContent: function (arg) {
				return '+' + arg.num + ' chuyến';
			},
			moreLinkClick: function (info) {
				openDay(info.date);
				return 'none';
			},
			noEventsContent: 'Không có chuyến nào trong những ngày này.',
			events: function (info, success) {
				success(items.filter(visible));
			},
			slotLabelContent: function (arg) {
				return pad(arg.date.getHours()) + ':00';
			},
			dayHeaderContent: function (arg) {
				const type = arg.view.type;
				if (type.indexOf('list') === 0) {
					return LONG_DAYS[arg.date.getDay()] + ', ' + dayMonth(arg.date);
				}
				if (type === 'dayGridMonth') {
					return SHORT_DAYS[arg.date.getDay()];
				}
				const node = element('span', 'tl-dayhead');
				node.append(element('small', null, SHORT_DAYS[arg.date.getDay()]), element('b', null, dayMonth(arg.date)));
				return { domNodes: [node] };
			},
			eventContent: function (arg) {
				const props = arg.event.extendedProps;
				const type = arg.view.type;
				if (props.slot) {
					return { domNodes: [slotNode(props.slot)] };
				}
				if (type === 'dayGridMonth') {
					return { domNodes: [lineNode(props.leg)] };
				}
				return { domNodes: [legNode(props.leg, type.indexOf('grid') === 0)] };
			},
			eventDidMount: function (info) {
				const props = info.event.extendedProps;
				const text = props.slot ? props.slot.aria : props.leg.aria;
				info.el.title = text;
				info.el.setAttribute('aria-label', text);
			},
			eventClick: function (info) {
				info.jsEvent.preventDefault();
				const props = info.event.extendedProps;
				if (props.slot) {
					location.assign(root.dataset.newUrl);
				} else {
					TG.openTrip(String(props.leg.tripId), info.el);
				}
			},
			datesSet: function (info) {
				const label = rangeLabel(info.view);
				title.textContent = label;
				pickButton.setAttribute('aria-label', 'Chọn ngày, đang xem ' + label);
				requestAnimationFrame(scrollToFirstTrip);
			},
			windowResize: function () {
				const now = canvas.clientWidth < NARROW;
				if (now !== narrow) {
					narrow = now;
					show();
				}
			}
		});
		calendar.render();
		show();

		// The field stays hidden: its own text follows the browser's language (11/03/2026 in English), the button speaks dd/mm.
		pickButton.addEventListener('click', function () {
			try {
				dateField.showPicker();
			} catch (e) {
				pickButton.parentElement.classList.add('is-plain');
				dateField.removeAttribute('tabindex');
				dateField.removeAttribute('aria-hidden');
				dateField.setAttribute('aria-label', 'Chọn ngày xem');
				dateField.focus();
			}
		});
		dateField.addEventListener('change', function () {
			const parts = dateField.value.split('-').map(Number);
			if (parts.length === 3 && parts.every(function (n) { return n > 0; })) {
				anchor = new Date(parts[0], parts[1] - 1, parts[2]);
				show();
			}
		});
		modeField.addEventListener('change', function () {
			mode = modeField.value;
			saveMode();
			show();
		});
		root.querySelector('[data-tl-prev]').addEventListener('click', function () { step(-1); });
		root.querySelector('[data-tl-next]').addEventListener('click', function () { step(1); });
		root.querySelector('[data-tl-today]').addEventListener('click', function () {
			anchor = startOfDay(new Date());
			show();
		});

		const past = data.legs.filter(function (leg) { return leg.past; }).length;
		const cancelled = data.legs.filter(function (leg) { return leg.cancelled; }).length;
		addToggle(past, 'Hiện {n} chuyến đã chạy', 'Ẩn {n} chuyến đã chạy', 'clock-history', function (on) { showPast = on; });
		addToggle(cancelled, 'Hiện {n} chuyến đã hủy', 'Ẩn {n} chuyến đã hủy', 'x-circle', function (on) { showCancelled = on; });

		data.alerts.forEach(function (text) {
			const line = element('li');
			const icon = element('i', 'bi bi-exclamation-triangle');
			icon.setAttribute('aria-hidden', 'true');
			line.append(icon, element('span', null, text));
			alertList.append(line);
		});
	}

	TG.api(root.dataset.url).then(render).catch(function () {
		errorBox.hidden = false;
	});
})();
