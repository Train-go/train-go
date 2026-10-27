(function () {
	'use strict';
	const name = station => station.name.replace(/^Ga\s+/i, '');
	const clone = id => document.getElementById(`net-${id}-template`).content.cloneNode(true);
	const text = (root, selector, value) => { root.querySelector(selector).textContent = value; };
	function blockDelete(button, reason, message) {
		button.disabled = Boolean(message);
		reason.hidden = !message;
		reason.textContent = message;
		if (message) button.setAttribute('aria-describedby', reason.id);
	}
	function overview(panel, ctx) {
		panel.append(clone('overview'));
		const form = panel.querySelector('form');
		['a', 'b'].forEach(key => {
			const select = form.elements.namedItem(key);
			select.add(new Option('Chọn ga', ''));
			ctx.network.stations.forEach(station => select.add(new Option(station.name, station.id)));
		});
		form.addEventListener('submit', event => {
			event.preventDefault();
			const a = Number(form.elements.a.value), b = Number(form.elements.b.value);
			const fields = {};
			if (!a) fields.a = 'Vui lòng chọn ga thứ nhất.';
			if (!b) fields.b = 'Vui lòng chọn ga thứ hai.';
			else if (a === b) fields.b = 'Ga thứ hai phải khác ga thứ nhất.';
			TG.showFieldErrors(form, fields);
			if (!Object.keys(fields).length) ctx.select({ type: 'pair', a, b });
		});
	}
	function stationDetails(panel, ctx, station) {
		panel.append(clone('station'));
		text(panel, '#net-panel-h', station.name);
		['code', 'kmMark', 'city', 'address'].forEach(key => {
			text(panel, `[data-value="${key}"]`, station[key] ?? (key === 'city' ? 'Chưa nhập tỉnh' : 'Chưa nhập địa chỉ'));
		});
		const remove = panel.querySelector('[data-action=delete-station]');
		blockDelete(remove, panel.querySelector('#station-delete-reason'), station.routeCount
			? `Không xóa được: ga đang có ${station.routeCount} tuyến. Xóa các tuyến đó trước.` : '');
		TG.fanOut(panel.querySelector('[data-fan-host]'), { station, network: ctx.network, commit: ctx.commit });
		panel.querySelector('[data-action=edit-station]').onclick = () => ctx.select({ type: 'station', id: station.id, edit: true });
		remove.onclick = () => ctx.remove(`/api/admin/stations/${station.id}`, `Xóa ${station.name}?`,
			'Ga sẽ không còn trong danh sách chọn ga khi tạo tuyến.', 'Xóa ga', 'Đã xóa ga.', { type: 'overview' }, remove);
	}
	function stationForm(panel, ctx, station, a, b) {
		panel.append(clone(station ? 'edit' : 'gap'));
		const form = panel.querySelector('form');
		form.querySelector('[data-station-fields]').append(clone('station-fields'));
		text(form, '#net-panel-h', station ? `Sửa ${station.name}` : `Chèn ga giữa ${name(a)} và ${name(b)}`);
		if (station) {
			['name', 'code', 'city', 'address', 'kmMark'].forEach(key => { form.elements.namedItem(key).value = station[key] ?? ''; });
			if (station.routeCount) {
				form.elements.code.readOnly = true;
				form.elements.code.setAttribute('aria-describedby', 'station-code-help');
				form.querySelector('#station-code-help').hidden = false;
			}
		} else TG.stationDraft(form, {
			stations: ctx.network.stations,
			slot: { lo: a.kmMark, hi: b.kmMark, prev: a, next: b }
		});
		form.querySelector('[data-action=cancel]').onclick = () => ctx.select(station ? { type: 'station', id: station.id } : { type: 'overview' });
		form.addEventListener('submit', event => {
			event.preventDefault();
			const body = {};
			['name', 'code', 'city', 'address'].forEach(key => {
				const value = form.elements.namedItem(key).value.trim();
				body[key] = value || (key === 'city' || key === 'address' ? null : '');
			});
			const km = form.elements.kmMark;
			if (km.validity.badInput || (km.value && !Number.isInteger(Number(km.value)))) {
				TG.showFieldErrors(form, { kmMark: 'Lý trình phải là số km nguyên.' });
				return;
			}
			body.kmMark = km.value === '' ? null : Number(km.value);
			ctx.write(station ? `/api/admin/stations/${station.id}` : '/api/admin/stations',
				{ method: station ? 'PUT' : 'POST', body }, form, station ? 'Đã lưu ga.' : 'Đã thêm ga.',
				saved => (station ? { type: 'station', id: saved.id } : { type: 'suggest', id: saved.id }));
		});
	}
	function pair(panel, ctx, a, b) {
		panel.append(clone('pair'));
		text(panel, '#net-panel-h', `${name(a)} ⇄ ${name(b)}`);
		const distance = Math.abs(a.kmMark - b.kmMark);
		text(panel, '[data-pair-distance]', `${distance} km theo lý trình. Mỗi chiều là một tuyến.`);
		[[a, b], [b, a]].forEach(([origin, destination]) => {
			const fragment = clone('direction');
			const form = fragment.querySelector('form');
			const key = `${origin.code}-${destination.code}`;
			const label = `${name(origin)} → ${name(destination)}`;
			form.dataset.direction = key;
			text(form, '[data-direction-name]', label);
			text(form, '[data-direction-code]', `${origin.code} → ${destination.code}`);
			const route = ctx.network.routes.find(r => r.originId === origin.id && r.destinationId === destination.id);
			const remove = form.querySelector('[data-action=delete-route]');
			const create = form.querySelector('[data-action=create-route]');
			if (route) {
				text(form, '[data-direction-meta]', `${route.distanceKm} km · ${route.upcomingTrips} chuyến sắp chạy`);
				form.querySelector('[data-distance-field]').remove();
				create.remove();
				const reason = form.querySelector('[data-delete-reason]');
				reason.id = `route-delete-reason-${key}`;
				blockDelete(remove, reason, route.totalTrips ? `Không xóa được: tuyến đã có ${route.totalTrips} chuyến.` : '');
				remove.onclick = () => ctx.remove(`/api/admin/routes/${route.id}`, `Xóa tuyến ${label}?`,
					'Không thể hoàn tác. Chiều ngược được giữ nguyên.', 'Xóa tuyến', 'Đã xóa tuyến.', ctx.state, remove);
				form.addEventListener('submit', event => event.preventDefault());
			} else {
				remove.remove();
				text(form, '[data-direction-meta]', 'Chưa có');
				const input = form.elements.distanceKm;
				input.id = `distance-${key}`;
				input.value = distance;
				form.querySelector('label').htmlFor = input.id;
				form.addEventListener('submit', event => {
					event.preventDefault();
					if (!input.value || !Number.isInteger(Number(input.value)) || Number(input.value) < 1 || Number(input.value) > 2147483647) {
						TG.showFieldErrors(form, { distanceKm: 'Khoảng cách phải là số km nguyên dương không quá 2147483647.' });
						return;
					}
					ctx.write('/api/admin/routes', { method: 'POST', body: { originStationId: origin.id,
						destinationStationId: destination.id, distanceKm: Number(input.value), bothWays: false } },
					form, 'Đã tạo tuyến.', () => ctx.state);
				});
			}
			panel.querySelector('.net-directions').append(fragment);
		});
	}
	/** After a new station is saved: the routes worth creating for it. */
	function suggestions(panel, ctx, station) {
		panel.append(clone('suggest'));
		text(panel, '#net-panel-h', `Nối ${name(station)} vào tuyến`);
		const leave = () => ctx.select({ type: 'station', id: station.id });
		TG.routeSuggestions(panel.querySelector('[data-suggest-host]'), {
			station, network: ctx.network, commit: ctx.commit, onSkip: leave
		});
	}
	TG.stationPanels = function (panel, ctx) {
		panel.replaceChildren();
		const find = id => ctx.network.stations.find(station => station.id === id);
		const state = ctx.state;
		if (state.type === 'suggest') suggestions(panel, ctx, find(state.id));
		else if (state.type === 'station') {
			if (state.edit) stationForm(panel, ctx, find(state.id));
			else stationDetails(panel, ctx, find(state.id));
		} else if (state.type === 'gap') stationForm(panel, ctx, null, find(state.a), find(state.b));
		else if (state.type === 'pair') pair(panel, ctx, find(state.a), find(state.b));
		else overview(panel, ctx);
	};
})();
