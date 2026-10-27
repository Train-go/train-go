/*
 * "Nối [ga] vào tuyến": the routes worth creating for a station just saved
 * (#38). Both neighbours by km are ticked, the two ends of the line are not.
 * Every pair has its two directions, each one a button of its own; a direction
 * that exists is shown ticked and locked.
 *
 *   TG.routeSuggestions(container, { station, network, commit, onSkip, onDone })
 *
 * `commit(task, nextState)` is the page's busy-guarded "do this, then reload"
 * (stations.js); the batch goes through POST /api/admin/routes/batch and the
 * toast's "Hoàn tác" through DELETE .../batch.
 */
(function () {
	'use strict';

	const placeName = station => station.name.replace(/^Ga\s+/i, '');
	const key = (from, to) => `${from}>${to}`;

	/** The pairs to suggest and the long-haul route that the new station now sits on. */
	function suggest(network, station) {
		const others = network.stations.filter(other => other.id !== station.id).sort((a, b) => a.kmMark - b.kmMark);
		const prev = others.filter(other => other.kmMark < station.kmMark).pop() || null;
		const next = others.find(other => other.kmMark > station.kmMark) || null;
		const has = (from, to) => network.routes.some(route => route.originId === from.id && route.destinationId === to.id);
		const pair = (a, b, kind) => ({
			a, b, kind, km: Math.abs(b.kmMark - a.kmMark), ab: has(a, b), ba: has(b, a)
		});
		const items = [];
		if (prev) items.push(pair(prev, station, 'adjacent'));
		if (next) items.push(pair(station, next, 'adjacent'));
		const first = others[0];
		const last = others[others.length - 1];
		if (first && first !== prev && first.kmMark < station.kmMark) items.push(pair(first, station, 'long'));
		if (last && last !== next && last.kmMark > station.kmMark) items.push(pair(station, last, 'long'));
		const bypass = prev && next && (has(prev, next) || has(next, prev)) ? { prev, next } : null;
		return { items, bypass };
	}

	TG.routeSuggestions = function (container, { station, network, commit, onSkip, onDone }) {
		const { items, bypass } = suggest(network, station);
		const open = items.filter(item => !(item.ab && item.ba));
		const selected = new Set();
		open.filter(item => item.kind === 'adjacent').forEach(item => {
			if (!item.ab) selected.add(key(item.a.id, item.b.id));
			if (!item.ba) selected.add(key(item.b.id, item.a.id));
		});

		const section = document.createElement('section');
		section.className = 'route-sug';
		section.setAttribute('aria-label', `Tuyến cho ga ${placeName(station)}`);
		container.replaceChildren(section);

		const paragraph = (className, text) => {
			const node = document.createElement('p');
			node.className = className;
			node.textContent = text;
			return node;
		};

		function draw() {
			section.replaceChildren();
			if (!open.length) {
				section.append(paragraph('form-text', `Ga ${placeName(station)} đã có đủ tuyến hai chiều với các ga kề và hai đầu tuyến.`));
			} else {
				const listEl = document.createElement('ul');
				listEl.className = 'sug-list';
				open.forEach(item => listEl.append(row(item)));
				section.append(listEl);
			}
			if (bypass) {
				section.append(paragraph('form-text', `Tuyến ${placeName(bypass.prev)} ⇄ ${placeName(bypass.next)} vẫn giữ nguyên, thành tuyến chạy thẳng không dừng ${placeName(station)}.`));
			}
			const actions = document.createElement('div');
			actions.className = 'sug-actions';
			const skip = document.createElement('button');
			skip.type = 'button';
			skip.className = 'btn btn-outline-secondary';
			skip.textContent = open.length ? 'Để sau' : 'Đóng';
			skip.addEventListener('click', () => onSkip());
			actions.append(skip);
			if (open.length) {
				const create = document.createElement('button');
				create.type = 'button';
				create.className = 'btn btn-primary';
				create.dataset.action = 'create-routes';
				create.disabled = selected.size === 0;
				create.textContent = selected.size ? `Tạo ${selected.size} tuyến` : 'Chưa chọn tuyến nào';
				create.addEventListener('click', createSelected);
				actions.append(create);
			}
			section.append(actions);
		}

		function row(item) {
			const missing = [[item.a, item.b, item.ab], [item.b, item.a, item.ba]].filter(([, , has]) => !has);
			const on = missing.some(([from, to]) => selected.has(key(from.id, to.id)));
			const all = missing.every(([from, to]) => selected.has(key(from.id, to.id)));
			const li = document.createElement('li');
			li.classList.toggle('on', on);

			const label = document.createElement('label');
			label.className = 'sug-pair';
			const check = document.createElement('input');
			check.type = 'checkbox';
			check.checked = on;
			check.indeterminate = on && !all;
			check.addEventListener('change', () => {
				missing.forEach(([from, to]) => {
					if (check.checked) selected.add(key(from.id, to.id));
					else selected.delete(key(from.id, to.id));
				});
				draw();
				focusRow(item);
			});
			const text = document.createElement('span');
			const title = document.createElement('b');
			title.textContent = `${placeName(item.a)} ⇄ ${placeName(item.b)}`;
			const sub = document.createElement('small');
			sub.textContent = `${item.km} km · ${item.kind === 'adjacent' ? 'ga kề' : 'tuyến dài, chạy thẳng'}`;
			text.append(title, sub);
			label.append(check, text);

			const dirs = document.createElement('span');
			dirs.className = 'sug-dirs';
			dirs.setAttribute('role', 'group');
			dirs.setAttribute('aria-label', `Chiều của ${placeName(item.a)} ⇄ ${placeName(item.b)}`);
			[[item.a, item.b, item.ab], [item.b, item.a, item.ba]].forEach(([from, to, has]) => {
				if (has) {
					const done = document.createElement('span');
					done.className = 'dir-pill has';
					done.innerHTML = '<i class="bi bi-check2" aria-hidden="true"></i>';
					done.append(`${from.code} → ${to.code}`);
					dirs.append(done);
					return;
				}
				const pill = document.createElement('button');
				pill.type = 'button';
				pill.className = 'dir-pill';
				pill.textContent = `${from.code} → ${to.code}`;
				pill.setAttribute('aria-pressed', String(selected.has(key(from.id, to.id))));
				pill.addEventListener('click', () => {
					if (selected.has(key(from.id, to.id))) selected.delete(key(from.id, to.id));
					else selected.add(key(from.id, to.id));
					draw();
					focusRow(item, `${from.code} → ${to.code}`);
				});
				dirs.append(pill);
			});
			li.append(label, dirs);
			li.dataset.pair = `${item.a.id}-${item.b.id}`;
			return li;
		}

		/** The list is redrawn on every change; put the keyboard back where it was. */
		function focusRow(item, pillText) {
			const li = section.querySelector(`[data-pair="${item.a.id}-${item.b.id}"]`);
			if (!li) return;
			const target = pillText
				? [...li.querySelectorAll('.dir-pill')].find(pill => pill.textContent === pillText)
				: li.querySelector('input[type=checkbox]');
			target?.focus();
		}

		async function createSelected() {
			const kmOf = (from, to) => (items.find(item => (item.a.id === from && item.b.id === to)
				|| (item.a.id === to && item.b.id === from)) || {}).km;
			const routes = [...selected].map(value => {
				const [from, to] = value.split('>').map(Number);
				return { originStationId: from, destinationStationId: to, distanceKm: kmOf(from, to) };
			});
			let ids = [];
			const ok = await commit(async () => {
				ids = (await TG.api('/api/admin/routes/batch', { method: 'POST', body: { routes } })).ids;
			}, { type: 'station', id: station.id });
			if (!ok) return;
			TG.toast(`Đã tạo ${ids.length} tuyến cho ga ${placeName(station)}.`, {
				label: 'Hoàn tác',
				run: () => commit(async () => {
					await TG.api(`/api/admin/routes/batch?ids=${ids.join(',')}`, { method: 'DELETE' });
				}, { type: 'suggest', id: station.id }).then(undone => { if (undone) TG.toast('Đã hoàn tác.'); })
			});
			if (onDone) onDone(ids);
		}

		draw();
	};
})();
