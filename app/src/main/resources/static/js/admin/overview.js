/*
 * The three tracks of the admin overview (templates/admin/overview.html) and
 * the filter they share: quick ranges (7 days, 30 days, this month,
 * last month), any from-to dates, and day / week / month grouping, all
 * applied the moment they change.
 * They fetch their numbers for the chosen range; the rest of the
 * page is printed by the server. The range is kept in the address (?from=
 * &to=&group=), so a reload or a shared link shows the same charts.
 */
(function () {
	'use strict';

	const root = document.querySelector('[data-overview]');
	if (!root) return;

	const MAX_DAYS = 366;
	const EMPTY = 'Chưa có dữ liệu trong khoảng này.';
	const form = root.querySelector('[data-filter]');
	const errorLine = form.querySelector('[data-filter-error]');
	const today = parse(form.dataset.today);

	const pad = number => String(number).padStart(2, '0');
	function ymd(date) { return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`; }
	function parse(text) {
		const [y, m, d] = text.split('-').map(Number);
		return new Date(y, m - 1, d);
	}
	const shown = text => text.split('-').reverse().join('/');
	const addDays = (date, days) => new Date(date.getFullYear(), date.getMonth(), date.getDate() + days);

	/** The days a quick range stands for, relative to the server's today. */
	const PRESETS = {
		'7d': () => [addDays(today, -6), today],
		'30d': () => [addDays(today, -29), today],
		month: () => [new Date(today.getFullYear(), today.getMonth(), 1), today],
		lastmonth: () => [new Date(today.getFullYear(), today.getMonth() - 1, 1), new Date(today.getFullYear(), today.getMonth(), 0)]
	};

	const count = value => Math.round(value).toLocaleString('vi-VN');
	const slot = name => root.querySelector(`[data-chart="${name}"]`);
	const sum = name => root.querySelector(`[data-sum="${name}"]`);

	let current = { from: '', to: '', group: '' };
	let sales = null;
	let users = null;

	function draw() {
		if (!sales || !users) return;
		const range = `từ ${shown(sales.from)} đến ${shown(sales.to)}`;
		const titles = sales.points.map(point => point.title);
		TG.track(slot('revenue'), {
			kind: 'area', height: 200, name: 'Doanh thu', titles, values: sales.points.map(point => point.revenue),
			format: TG.money, tick: TG.shortMoney, label: `Doanh thu ${range}`, empty: EMPTY
		});
		TG.track(slot('bookings'), {
			kind: 'bars', height: 96, name: 'Booking', titles, values: sales.points.map(point => point.bookings),
			format: String, tick: String, integer: true, divisions: 2, label: `Số booking đã bán ${range}`, empty: EMPTY
		});
		TG.track(slot('users'), {
			kind: 'lolli', height: 130, name: 'Người dùng mới', titles, labels: users.points.map(point => point.label),
			values: users.points.map(point => point.newUsers), format: String, tick: String, integer: true, divisions: 2,
			label: `Người dùng đăng ký mới ${range}`, empty: EMPTY
		});
		sum('revenue').textContent = TG.money(sales.revenue);
		sum('bookings').textContent = count(sales.bookings);
		sum('users').textContent = count(users.newUsers);
	}

	function showError(message) {
		sales = null;
		users = null;
		['revenue', 'bookings', 'users'].forEach(name => {
			sum(name).textContent = '';
			slot(name).replaceChildren();
		});
		const line = document.createElement('p');
		line.className = 'ov-retry';
		line.textContent = message || 'Không tải được biểu đồ. ';
		if (!message) {
			const button = document.createElement('button');
			button.type = 'button';
			button.className = 'btn btn-outline-secondary btn-sm';
			button.textContent = 'Thử lại';
			button.addEventListener('click', load);
			line.append(button);
		}
		slot('revenue').append(line);
	}

	async function load() {
		const query = new URLSearchParams({ from: current.from, to: current.to });
		if (current.group) query.set('group', current.group);
		try {
			[sales, users] = await Promise.all([
				TG.api(`/api/admin/overview/sales?${query}`),
				TG.api(`/api/admin/overview/users?${query}`)
			]);
			draw();
		} catch (error) {
			// A range the server refuses is the admin's to fix: say why, once, under the filter.
			if (error && error.message && /ngày/.test(error.message)) {
				errorLine.textContent = error.message;
				errorLine.hidden = false;
				showError('Chọn lại khoảng thời gian.');
			} else {
				showError();
			}
		}
	}

	/** Checks the range the way the server will, so a typo never costs a request. */
	function problem(from, to) {
		if (!from || !to) return 'Chọn đủ ngày bắt đầu và ngày kết thúc.';
		if (from > to) return 'Ngày bắt đầu phải trước hoặc cùng ngày kết thúc.';
		if (to > ymd(today)) return 'Ngày kết thúc không được sau hôm nay.';
		const days = Math.round((parse(to) - parse(from)) / 86400000) + 1;
		if (days > MAX_DAYS) return `Chọn tối đa ${MAX_DAYS} ngày.`;
		return '';
	}

	function apply(from, to, group) {
		const message = problem(from, to);
		errorLine.textContent = message;
		errorLine.hidden = !message;
		if (message) return false;
		current = { from, to, group };
		form.elements.from.value = from;
		form.elements.to.value = to;
		form.elements.group.value = group;
		// Highlight the quick range the dates equal, if any.
		form.querySelectorAll('[data-preset]').forEach(button => {
			const [start, end] = PRESETS[button.dataset.preset]();
			const on = ymd(start) === from && ymd(end) === to;
			button.setAttribute('aria-pressed', String(on));
		});
		const query = new URLSearchParams({ from, to });
		if (group) query.set('group', group);
		history.replaceState(null, '', `${location.pathname}?${query}`);
		load();
		return true;
	}

	form.querySelectorAll('[data-preset]').forEach(button => {
		button.addEventListener('click', () => {
			const [start, end] = PRESETS[button.dataset.preset]();
			apply(ymd(start), ymd(end), form.elements.group.value);
		});
	});
	form.addEventListener('submit', event => {
		event.preventDefault();
		apply(form.elements.from.value, form.elements.to.value, form.elements.group.value);
	});
	// Every control applies as soon as it changes; Enter in a date box does the same.
	['from', 'to', 'group'].forEach(name => {
		form.elements[name].addEventListener('change', () => {
			apply(form.elements.from.value, form.elements.to.value, form.elements.group.value);
		});
	});
	form.elements.from.max = ymd(today);
	form.elements.to.max = ymd(today);

	// Redraw when a column changes width (window resize, phone turned).
	let timer = null;
	const observer = new ResizeObserver(() => {
		clearTimeout(timer);
		timer = setTimeout(draw, 120);
	});
	observer.observe(slot('revenue'));

	// Start from the address if it names a range, else from the last 30 days.
	const params = new URLSearchParams(location.search);
	const [start, end] = PRESETS['30d']();
	const group = ['day', 'week', 'month'].includes(params.get('group')) ? params.get('group') : '';
	if (!apply(params.get('from') || ymd(start), params.get('to') || ymd(end), group)) {
		apply(ymd(start), ymd(end), group);
	}
})();
