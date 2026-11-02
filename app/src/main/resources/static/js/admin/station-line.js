/*
 * "Ga và tuyến": the markup of the line and of the route table, pure functions
 * of the network from /api/admin/network. A missing segment and a missing
 * direction carry their own action; stations.js handles the clicks.
 */
(function () {
	'use strict';

	const esc = text => String(text).replace(/[&<>"]/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;' }[c]));
	const count = n => n.toLocaleString('vi-VN');
	const fold = text => String(text).normalize('NFD').replace(/[\u0300-\u036f]/g, '').replace(/đ/g, 'd').replace(/Đ/g, 'D').toLowerCase();
	const icon = name => `<i class="bi bi-${name}" aria-hidden="true"></i>`;

	function routeOf(network, a, b) {
		return network.routes.find(route => route.originId === a.id && route.destinationId === b.id) || null;
	}

	/*
	 * Between two stations: a plain connector when both directions exist (the
	 * stations already carry their km), a row with the warning and "Tạo tuyến"
	 * when one is missing.
	 */
	function segRow(network, a, b) {
		const ab = routeOf(network, a, b);
		const ba = routeOf(network, b, a);
		if (ab && ba) return '<li class="link-seg is-ok" aria-hidden="true"></li>';
		const state = ab || ba ? 'partial' : 'missing';
		const way = ab ? `${b.name} → ${a.name}` : `${a.name} → ${b.name}`;
		const text = state === 'partial' ? `<span class="miss-mark">${icon('exclamation-triangle')}Thiếu chiều ${esc(way)}</span>`
			: `<span class="miss-mark">${icon('exclamation-triangle')}Chưa có tuyến</span>`;
		const make = `<button class="btn btn-outline-secondary btn-sm" type="button" data-fill="${a.id}-${b.id}" aria-label="Tạo tuyến ${esc(a.name)} ⇄ ${esc(b.name)}">${icon('plus-lg')}Tạo tuyến</button>`;
		return `<li class="link-seg is-${state}"><span class="link-seg-info">${text}</span>
			<span class="link-seg-act">${make}</span></li>`;
	}

	function stopRow(station) {
		const place = station.city && station.city !== station.name ? `, ${esc(station.city)}` : '';
		return `<li><button class="stop-btn" type="button" data-station="${station.id}" aria-label="Xem ${esc(station.name)}">
			<span class="stop-dot" aria-hidden="true"></span>
			<span class="stop-name">${esc(station.name)}<small><code class="code">${esc(station.code)}</code>${place}</small></span>
			<span class="stop-km">${count(station.kmMark)} km</span>
		</button></li>`;
	}

	/** Neighbouring stations whose pair lacks a direction. */
	function gaps(network) {
		const list = network.stations;
		const found = [];
		for (let i = 0; i + 1 < list.length; i++) {
			if (!routeOf(network, list[i], list[i + 1]) || !routeOf(network, list[i + 1], list[i])) found.push([list[i], list[i + 1]]);
		}
		return found;
	}

	function line(network) {
		const list = network.stations;
		const missing = gaps(network).length;
		const banner = missing > 1 ? `<div class="gap-banner">${icon('exclamation-triangle')}<span>${missing} đoạn chưa có tuyến hai chiều</span><button class="btn btn-primary btn-sm" type="button" data-fill-all>Tạo tất cả</button></div>` : '';
		const rows = list.map((station, i) => stopRow(station) + (list[i + 1] ? segRow(network, station, list[i + 1]) : '')).join('');
		return `${banner}<ol class="line">${rows}</ol>`;
	}

	/** One row per pair of stations, north to south. */
	function pairs(network) {
		const byId = new Map(network.stations.map(station => [station.id, station]));
		const seen = new Map();
		network.routes.forEach(route => {
			const one = byId.get(route.originId);
			const two = byId.get(route.destinationId);
			if (!one || !two) return;
			const [a, b] = one.kmMark < two.kmMark ? [one, two] : [two, one];
			const key = `${a.id}-${b.id}`;
			if (!seen.has(key)) seen.set(key, { a, b });
		});
		return [...seen.values()].sort((p, q) => p.a.kmMark - q.a.kmMark || p.b.kmMark - q.b.kmMark);
	}

	function dirsCell(south, north, p) {
		if (south && north) return `<span class="route-both">${icon('check-lg')}Hai chiều</span>`;
		const [has, from, to, way] = south ? ['vào Nam', p.b, p.a, 'ra Bắc'] : ['ra Bắc', p.a, p.b, 'vào Nam'];
		return `<span class="route-dirs"><span class="miss-mark">${icon('exclamation-triangle')}Chỉ chiều ${has}</span>
			<button class="btn btn-link btn-sm" type="button" data-make="${from.id}-${to.id}" aria-label="Tạo tuyến ${esc(from.name)} → ${esc(to.name)}">${icon('plus-lg')}Tạo chiều ${way}</button></span>`;
	}

	function routes(network, query) {
		const needle = fold(query).trim();
		const order = network.stations.map(station => station.id);
		const list = pairs(network).filter(p => !needle
			|| [p.a.name, p.a.code, p.b.name, p.b.code].some(value => fold(value).includes(needle)));
		if (!list.length) {
			return needle ? `<div class="cv-empty">${icon('search')}<b>Không có tuyến nào khớp "${esc(query)}"</b><button class="btn btn-outline-secondary btn-sm" type="button" data-clear-search>Xóa tìm kiếm</button></div>`
				: `<div class="cv-empty">${icon('bezier2')}<b>Chưa có tuyến nào</b></div>`;
		}
		const rows = list.map(p => {
			const south = routeOf(network, p.a, p.b);
			const north = routeOf(network, p.b, p.a);
			const soon = [south, north].filter(Boolean).reduce((n, route) => n + route.upcomingTrips, 0);
			const skips = Math.abs(order.indexOf(p.a.id) - order.indexOf(p.b.id)) > 1;
			const key = `${p.a.id}-${p.b.id}`;
			return `<tr class="${south && north ? '' : 'is-gap'}" data-pair="${key}">
				<td><button class="route-name" type="button" data-pair="${key}"><span class="pair-name"><span class="route-stn">${esc(p.a.name)} ⇄</span> <span class="route-stn">${esc(p.b.name)}</span></span>${skips ? '<small>Nối thẳng</small>' : ''}</button></td>
				<td class="cc-n">${count((south || north).distanceKm)} km</td>
				<td>${dirsCell(south, north, p)}</td>
				<td class="cc-n">${soon ? `${count(soon)}<span class="route-unit"> chuyến sắp chạy</span>` : '<span class="route-quiet">Chưa có<span class="route-unit"> chuyến sắp chạy</span></span>'}</td>
			</tr>`;
		}).join('');
		return `<div class="dc-scroll"><table class="cc-table route-table">
			<thead><tr><th scope="col">Tuyến</th><th scope="col" class="cc-n">Khoảng cách</th><th scope="col">Chiều</th><th scope="col" class="cc-n">Chuyến sắp chạy</th></tr></thead>
			<tbody>${rows}</tbody></table></div>`;
	}

	TG.stationLine = { line, routes, gaps, routeOf };
})();
