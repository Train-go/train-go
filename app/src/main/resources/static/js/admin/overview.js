/*
 * The two charts of the admin overview (templates/admin/overview.html):
 * revenue and bookings over the last 7, 14 or 30 days, and what the bookings
 * already made are worth for the trips of the next seven days. The rest of
 * the page is printed by the server.
 */
(function () {
	'use strict';

	const root = document.querySelector('[data-overview]');
	if (!root) return;

	const sales = {
		box: root.querySelector('[data-chart=sales]'),
		data: null,
		days: 14
	};
	const forecast = { box: root.querySelector('[data-chart=forecast]'), data: null };

	const plot = chart => chart.box.querySelector('[data-plot]');
	const summary = chart => chart.box.querySelector('[data-summary]');

	function showError(chart, retry) {
		summary(chart).textContent = '';
		plot(chart).replaceChildren();
		const message = document.createElement('p');
		message.className = 'ov-empty';
		message.textContent = 'Không tải được biểu đồ. ';
		const button = document.createElement('button');
		button.type = 'button';
		button.className = 'btn btn-outline-secondary btn-sm';
		button.textContent = 'Thử lại';
		button.addEventListener('click', retry);
		message.append(button);
		plot(chart).append(message);
	}

	function drawSales() {
		const data = sales.data;
		if (!data) return;
		summary(sales).textContent = `${data.days} ngày qua: ${data.bookings} booking · ${TG.money(data.revenue)}`;
		TG.chart(plot(sales), {
			label: `Doanh thu và số booking đã bán mỗi ngày trong ${data.days} ngày qua`,
			labels: data.points.map(point => point.label),
			bars: { name: 'Doanh thu', values: data.points.map(point => point.revenue), format: TG.money, tick: TG.shortMoney },
			line: { name: 'Booking', values: data.points.map(point => point.bookings), format: String, tick: String, integer: true },
			empty: 'Chưa có booking trong khoảng này.'
		});
	}

	function drawForecast() {
		const data = forecast.data;
		if (!data) return;
		const fill = data.capacity ? Math.round(data.sold * 100 / data.capacity) : 0;
		summary(forecast).textContent = `Đã đặt cho 7 ngày tới: ${TG.money(data.revenue)} · ${data.sold}/${data.capacity} chỗ (${fill}%)`;
		TG.chart(plot(forecast), {
			label: 'Doanh thu đã đặt và tỷ lệ lấp đầy theo ngày khởi hành trong 7 ngày tới',
			labels: data.points.map(point => point.label),
			bars: { name: 'Doanh thu đã đặt', values: data.points.map(point => point.revenue), format: TG.money, tick: TG.shortMoney },
			line: { name: 'Tỷ lệ lấp đầy', values: data.points.map(point => point.fillPercent), format: value => `${value}%`, tick: value => `${Math.round(value)}%`, max: 100 },
			empty: 'Chưa có chuyến nào trong 7 ngày tới.'
		});
	}

	async function loadSales(days) {
		sales.days = days;
		root.querySelectorAll('[data-days]').forEach(button => {
			const on = Number(button.dataset.days) === days;
			button.classList.toggle('active', on);
			button.setAttribute('aria-pressed', String(on));
		});
		try {
			sales.data = await TG.api(`/api/admin/overview/sales?days=${days}`);
			drawSales();
		} catch (error) {
			showError(sales, () => loadSales(days));
		}
	}

	async function loadForecast() {
		try {
			forecast.data = await TG.api('/api/admin/overview/forecast');
			drawForecast();
		} catch (error) {
			showError(forecast, loadForecast);
		}
	}

	root.querySelectorAll('[data-days]').forEach(button => {
		button.addEventListener('click', () => loadSales(Number(button.dataset.days)));
	});

	// Redraw when the column changes width (window resize, sidebar, phone turned).
	let timer = null;
	const observer = new ResizeObserver(() => {
		clearTimeout(timer);
		timer = setTimeout(() => { drawSales(); drawForecast(); }, 120);
	});
	observer.observe(plot(sales));
	observer.observe(plot(forecast));

	loadSales(sales.days);
	loadForecast();
})();
