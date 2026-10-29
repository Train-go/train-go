/*
 * "Tạo tuyến từ [ga]": routes from one station to many stations at once (#39).
 *
 *   TG.fanOut(container, { station, network, commit })
 *
 * Every other station is one row with its distance and which directions
 * already exist. Ticking a station creates the missing "from this station"
 * direction, and the way back too while "Tạo cả chiều về" is ticked; a row
 * with nothing left to create is locked. The routes go through
 * POST /api/admin/routes/batch; the toast's "Hoàn tác" deletes them again.
 * `commit(task, nextState)` is the page's busy-guarded "do this, then reload".
 */
(function () {
	'use strict';

	const placeName = station => station.name.replace(/^Ga\s+/i, '');

	TG.fanOut = function (container, { station, network, commit }) {
		const others = network.stations.filter(other => other.id !== station.id).sort((a, b) => a.kmMark - b.kmMark);
		container.replaceChildren();
		if (!others.length) return;

		const has = (from, to) => network.routes.some(route => route.originId === from && route.destinationId === to);
		const status = other => ({ out: has(station.id, other.id), back: has(other.id, station.id) });
		const picked = new Set();
		let both = true;
		const name = placeName(station);

		const section = document.createElement('section');
		section.className = 'fan';
		section.setAttribute('aria-labelledby', `fan-h-${station.id}`);

		const head = document.createElement('div');
		head.className = 'fan-h';
		const title = document.createElement('h3');
		title.id = `fan-h-${station.id}`;
		title.textContent = `Tạo tuyến từ ${name}`;
		head.append(title);
		const neighbours = document.createElement('button');
		neighbours.type = 'button';
		neighbours.className = 'btn btn-outline-secondary btn-sm';
		neighbours.textContent = 'Chọn hai ga kề';
		head.append(neighbours);

		const list = document.createElement('ul');
		list.className = 'fan-list';
		const rows = others.map(other => {
			const li = document.createElement('li');
			const label = document.createElement('label');
			label.className = 'fan-check';
			const input = document.createElement('input');
			input.type = 'checkbox';
			input.id = `fan-${station.id}-${other.id}`;
			const text = document.createElement('span');
			text.className = 'fan-name';
			const b = document.createElement('b');
			b.textContent = placeName(other);
			const small = document.createElement('small');
			small.textContent = `${Math.abs(other.kmMark - station.kmMark)} km`;
			text.append(b, small);
			label.append(input, text);
			const state = document.createElement('span');
			state.className = 'fan-st';
			const st = status(other);
			state.textContent = st.out && st.back ? 'Đã có hai chiều' : st.out ? 'Đã có chiều đi' : st.back ? 'Đã có chiều về' : '';
			li.append(label, state);
			input.addEventListener('change', () => {
				if (input.checked) picked.add(other.id);
				else picked.delete(other.id);
				refresh();
			});
			list.append(li);
			return { other, input, label, st };
		});

		const bothLabel = document.createElement('label');
		bothLabel.className = 'fan-check';
		const bothInput = document.createElement('input');
		bothInput.type = 'checkbox';
		bothInput.checked = true;
		const bothText = document.createElement('span');
		bothText.textContent = `Tạo cả chiều về ${name}`;
		bothLabel.append(bothInput, bothText);
		bothInput.addEventListener('change', () => { both = bothInput.checked; refresh(); });

		const actions = document.createElement('div');
		actions.className = 'pane-actions';
		const create = document.createElement('button');
		create.type = 'button';
		create.className = 'btn btn-primary';
		create.dataset.action = 'fan-create';
		actions.append(create);

		section.append(head, list, bothLabel, actions);
		container.append(section);

		/** Whether a row has nothing left to create under the current "chiều về" setting. */
		const done = row => row.st.out && (row.st.back || !both);

		/** The directions the ticked rows would create. */
		function directions() {
			const result = [];
			rows.forEach(row => {
				if (!picked.has(row.other.id) || done(row)) return;
				const km = Math.abs(row.other.kmMark - station.kmMark);
				if (!row.st.out) result.push({ originStationId: station.id, destinationStationId: row.other.id, distanceKm: km });
				if (both && !row.st.back) result.push({ originStationId: row.other.id, destinationStationId: station.id, distanceKm: km });
			});
			return result;
		}

		function refresh() {
			rows.forEach(row => {
				const locked = done(row);
				row.input.disabled = locked;
				row.input.checked = locked || picked.has(row.other.id);
				row.label.classList.toggle('done', locked);
			});
			const count = directions().length;
			create.disabled = count === 0;
			create.textContent = count ? `Tạo ${count} tuyến` : 'Chọn ga đến';
		}

		neighbours.addEventListener('click', () => {
			const before = others.filter(other => other.kmMark < station.kmMark).pop();
			const after = others.find(other => other.kmMark > station.kmMark);
			picked.clear();
			[before, after].filter(Boolean).forEach(other => {
				const row = rows.find(r => r.other.id === other.id);
				if (!done(row)) picked.add(other.id);
			});
			refresh();
		});

		create.addEventListener('click', async () => {
			const routes = directions();
			if (!routes.length) return;
			let ids = [];
			const ok = await commit(async () => {
				ids = (await TG.api('/api/admin/routes/batch', { method: 'POST', body: { routes } })).ids;
			}, { type: 'station', id: station.id });
			if (!ok) return;
			TG.toast(`Đã tạo ${ids.length} tuyến từ ga ${name}.`, {
				label: 'Hoàn tác',
				run: () => commit(async () => {
					await TG.api(`/api/admin/routes/batch?ids=${ids.join(',')}`, { method: 'DELETE' });
				}, { type: 'station', id: station.id }).then(undone => { if (undone) TG.toast('Đã hoàn tác.'); })
			});
		});

		refresh();
	};
})();
