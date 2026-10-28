/*
 * The two charts of the admin overview (templates/admin/overview.html):
 * revenue and bookings over the last 7, 14 or 30 days, and customers
 * registered over the same choice of days. Each chart has its own range
 * switch and fetches its own numbers; the rest of the page is printed by the
 * server.
 */
(function () {
	'use strict';

	const root = document.querySelector('[data-overview]');
	if (!root) return;

	const charts = {
		sales: {
			url: '/api/admin/overview/sales',
			summary: data => `${data.days} ngày qua: ${data.bookings} booking · ${TG.money(data.revenue)}`,
			spec: data => ({
				label: `Doanh thu và số booking đã bán mỗi ngày trong ${data.days} ngày qua`,
				labels: data.points.map(point => point.label),
				bars: { name: 'Doanh thu', values: data.points.map(point => point.revenue), format: TG.money, tick: TG.shortMoney },
				line: { name: 'Booking', values: data.points.map(point => point.bookings), format: String, tick: String, integer: true },
				empty: 'Chưa có booking trong khoảng này.'
			})
		},
		users: {
			url: '/api/admin/overview/users',
			summary: data => `${data.days} ngày qua: ${data.newUsers} người dùng mới · hiện có ${data.total}`,
			spec: data => ({
				label: `Người dùng đăng ký mới mỗi ngày và tổng số người dùng trong ${data.days} ngày qua`,
				labels: data.points.map(point => point.label),
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
		chart.days = 14;
	});

	function draw(chart) {
		if (!chart.data) return;
		chart.box.querySelector('[data-summary]').textContent = chart.summary(chart.data);
		TG.chart(chart.plot, chart.spec(chart.data));
	}

	function showError(chart) {
		chart.box.querySelector('[data-summary]').textContent = '';
		chart.plot.replaceChildren();
		const message = document.createElement('p');
		message.className = 'ov-empty';
		message.textContent = 'Không tải được biểu đồ. ';
		const button = document.createElement('button');
		button.type = 'button';
		button.className = 'btn btn-outline-secondary btn-sm';
		button.textContent = 'Thử lại';
		button.addEventListener('click', () => load(chart, chart.days));
		message.append(button);
		chart.plot.append(message);
	}

	async function load(chart, days) {
		chart.days = days;
		chart.box.querySelectorAll('[data-days]').forEach(button => {
			const on = Number(button.dataset.days) === days;
			button.classList.toggle('active', on);
			button.setAttribute('aria-pressed', String(on));
		});
		try {
			chart.data = await TG.api(`${chart.url}?days=${days}`);
			draw(chart);
		} catch (error) {
			showError(chart);
		}
	}

	Object.values(charts).forEach(chart => {
		chart.box.querySelectorAll('[data-days]').forEach(button => {
			button.addEventListener('click', () => load(chart, Number(button.dataset.days)));
		});
	});

	// Redraw when a column changes width (window resize, phone turned).
	let timer = null;
	const observer = new ResizeObserver(() => {
		clearTimeout(timer);
		timer = setTimeout(() => Object.values(charts).forEach(draw), 120);
	});
	Object.values(charts).forEach(chart => observer.observe(chart.plot));

	Object.values(charts).forEach(chart => load(chart, chart.days));
})();
