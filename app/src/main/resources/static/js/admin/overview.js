/*
 * The two charts of the admin overview (templates/admin/overview.html) and
 * the filter they share: quick ranges (7 days, 30 days, this month,
 * last month), any from-to dates, and day / week / month grouping, all
 * applied the moment they change.
 * Each chart fetches its own numbers for the chosen range; the rest of the
 * page is printed by the server. The range is kept in the address (?from=
 * &to=&group=), so a reload or a shared link shows the same charts.
 */
(function () {
	'use strict';

	const root = document.querySelector('[data-overview]');
	if (!root) return;

	const MAX_DAYS = 366;
	const GROUP_NAMES = { day: 'theo ngày', week: 'theo tuần', month: 'theo tháng' };
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

	const charts = {
		sales: {
			url: '/api/admin/overview/sales',
			summary: data => `${data.bookings} booking · ${TG.money(data.revenue)}`,
			spec: data => ({
				label: `Doanh thu và số booking đã bán ${GROUP_NAMES[data.group]}, từ ${shown(data.from)} đến ${shown(data.to)}`,
				labels: data.points.map(point => point.label),
				titles: data.points.map(point => point.title),
				bars: { name: 'Doanh thu', values: data.points.map(point => point.revenue), format: TG.money, tick: TG.shortMoney },
				line: { name: 'Booking', values: data.points.map(point => point.bookings), format: String, tick: String, integer: true },
				empty: 'Chưa có booking trong khoảng này.'
			})
		},
		users: {
			url: '/api/admin/overview/users',
			summary: data => `${data.newUsers} người dùng mới · hiện có ${data.total}`,
			spec: data => ({
				label: `Người dùng đăng ký mới ${GROUP_NAMES[data.group]} và tổng số người dùng, từ ${shown(data.from)} đến ${shown(data.to)}`,
				labels: data.points.map(point => point.label),
				titles: data.points.map(point => point.title),
				bars: { name: 'Người dùng mới', values: data.points.map(point => point.newUsers), format: String, tick: String, integer: true },
				line: { name: 'Tổng người dùng', values: data.points.map(point => point.total), format: String, tick: String, integer: true },
				empty: 'Chưa có người dùng nào.'
			})
		}
	};

	Object.entries(charts).forEach(([name, chart]) => {
		chart.box = root.querySelector(`[data-chart=${name}]`);
		chart.plot = chart.box.querySelector('[data-plot]');
		chart.data = null;
	});

	let current = { from: '', to: '', group: '' };

	function draw(chart) {
		if (!chart.data) return;
		const data = chart.data;
		chart.box.querySelector('[data-summary]').textContent =
			`${shown(data.from)} - ${shown(data.to)}, ${GROUP_NAMES[data.group]}: ${chart.summary(data)}`;
		TG.chart(chart.plot, chart.spec(data));
	}

	function showError(chart, message) {
		chart.data = null;
		chart.box.querySelector('[data-summary]').textContent = '';
		chart.plot.replaceChildren();
		const line = document.createElement('p');
		line.className = 'ov-empty';
		line.textContent = message || 'Không tải được biểu đồ. ';
		if (!message) {
			const button = document.createElement('button');
			button.type = 'button';
			button.className = 'btn btn-outline-secondary btn-sm';
			button.textContent = 'Thử lại';
			button.addEventListener('click', () => loadChart(chart));
			line.append(button);
		}
		chart.plot.append(line);
	}

	async function loadChart(chart) {
		const query = new URLSearchParams({ from: current.from, to: current.to });
		if (current.group) query.set('group', current.group);
		try {
			chart.data = await TG.api(`${chart.url}?${query}`);
			draw(chart);
		} catch (error) {
			// A range the server refuses is the admin's to fix: say why, once, under the filter.
			if (error && error.message && /ngày/.test(error.message)) {
				errorLine.textContent = error.message;
				errorLine.hidden = false;
				showError(chart, 'Chọn lại khoảng thời gian.');
			} else {
				showError(chart);
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
			button.classList.toggle('active', on);
			button.setAttribute('aria-pressed', String(on));
		});
		const query = new URLSearchParams({ from, to });
		if (group) query.set('group', group);
		history.replaceState(null, '', `${location.pathname}?${query}`);
		Object.values(charts).forEach(loadChart);
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
		timer = setTimeout(() => Object.values(charts).forEach(draw), 120);
	});
	Object.values(charts).forEach(chart => observer.observe(chart.plot));

	// Start from the address if it names a range, else from the last 30 days.
	const params = new URLSearchParams(location.search);
	const [start, end] = PRESETS['30d']();
	const group = ['day', 'week', 'month'].includes(params.get('group')) ? params.get('group') : '';
	if (!apply(params.get('from') || ymd(start), params.get('to') || ymd(end), group)) {
		apply(ymd(start), ymd(end), group);
	}
})();
