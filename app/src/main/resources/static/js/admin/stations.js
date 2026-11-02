(function () {
	'use strict';
	const root = document.getElementById('station-network');
	if (!root) return;
	const split = root.querySelector('.net');
	const panel = root.querySelector('.net-panel');
	const sheet = root.querySelector('#net-sheet');
	const actions = root.querySelector('#net-actions');
	const lineHost = root.querySelector('[data-line]');
	const routesHost = root.querySelector('[data-routes]');
	const search = root.querySelector('[data-route-search]');
	const loadError = root.querySelector('#net-load-error');
	let network = { stations: [], routes: [] };
	let state = { type: 'overview' };
	let busy = false;

	function focusPanel() {
		panel.querySelector('#net-panel-h')?.focus({ preventScroll: true });
	}
	function showSheet() {
		bootstrap.Offcanvas.getOrCreateInstance(sheet).show();
	}
	/** Resolves once the sheet is gone: a modal over an open offcanvas fights it for focus. */
	function hideSheet() {
		return new Promise(resolve => {
			if (!sheet.classList.contains('show')) { resolve(); return; }
			sheet.addEventListener('hidden.bs.offcanvas', resolve, { once: true });
			bootstrap.Offcanvas.getOrCreateInstance(sheet).hide();
		});
	}
	function renderLists() {
		lineHost.innerHTML = TG.stationLine.line(network);
		routesHost.innerHTML = TG.stationLine.routes(network, search.value);
	}
	function render(focus = false) {
		const ids = new Set(network.stations.map(station => station.id));
		if ((['station', 'suggest'].includes(state.type) && !ids.has(state.id))
			|| (['pair', 'gap'].includes(state.type) && (!ids.has(state.a) || !ids.has(state.b)))) state = { type: 'overview' };
		renderLists();
		actions.hidden = false;
		root.querySelector('[data-add-station]').hidden = network.stations.length < 2;
		if (state.type === 'overview') {
			panel.replaceChildren();
			hideSheet();
			return;
		}
		TG.stationPanels(panel, { network, state, select, write, remove, commit });
		showSheet();
		if (focus) focusPanel();
	}
	function select(next) {
		if (busy) return;
		state = next;
		render(true);
	}
	function setBusy(value) {
		busy = value;
		split.inert = value;
		root.setAttribute('aria-busy', String(value));
	}
	async function reload(focus = false) {
		loadError.hidden = true;
		try {
			const result = await TG.api('/api/admin/network');
			if (!result || !Array.isArray(result.stations) || !Array.isArray(result.routes)) throw new Error('Không tải được mạng ga. Vui lòng thử lại hoặc đăng nhập lại.');
			network = result;
			network.stations.sort((a, b) => a.kmMark - b.kmMark);
			split.hidden = false;
			render();
			return true;
		} catch (error) {
			split.hidden = true;
			actions.hidden = true;
			loadError.querySelector('span').textContent = error.message || 'Không tải được mạng ga.';
			loadError.hidden = false;
			if (focus) loadError.querySelector('button').focus();
			return false;
		}
	}
	function report(error, form) {
		const fields = error.fields || {};
		if (form) {
			// Direction forms share field names; clear their old feedback to keep error IDs unique.
			panel.querySelectorAll('form').forEach(other => TG.clearFieldErrors(other));
			TG.showFieldErrors(form, fields);
		}
		if (!form || !Object.keys(fields).some(key => form.elements.namedItem(key))) {
			TG.toast(error.message || 'Có lỗi xảy ra, vui lòng thử lại.', { error: true });
		}
	}
	async function write(url, options, form, message, nextState) {
		if (busy) return;
		if (form) TG.clearFieldErrors(form);
		const submit = form?.querySelector('[type=submit]');
		const label = submit?.textContent;
		if (submit) { submit.disabled = true; submit.textContent = 'Đang lưu…'; }
		setBusy(true);
		let failure;
		let refreshed = false;
		try {
			const result = await TG.api(url, options);
			state = nextState(result);
			TG.toast(message);
			refreshed = await reload(true);
		} catch (error) { failure = error; }
		finally {
			setBusy(false);
			if (submit) { submit.disabled = false; submit.textContent = label; }
		}
		if (failure) report(failure, form);
		else if (refreshed) focusPanel();
	}
	/**
	 * Runs `task` (API calls) with the page locked, then shows `nextState` on
	 * fresh data. Resolves true when both worked; a failure becomes a toast.
	 */
	async function commit(task, nextState) {
		if (busy) return false;
		setBusy(true);
		let failure;
		let refreshed = false;
		try {
			await task();
			state = nextState;
			refreshed = await reload(true);
		} catch (error) { failure = error; }
		finally { setBusy(false); }
		if (failure) {
			TG.toast(failure.message || 'Có lỗi xảy ra, vui lòng thử lại.', { error: true });
			return false;
		}
		if (refreshed) focusPanel();
		return refreshed;
	}
	async function remove(url, title, body, confirmLabel, message, nextState, trigger) {
		if (busy || trigger.disabled) return;
		setBusy(true);
		let confirmed = false;
		try {
			await hideSheet();
			confirmed = await TG.confirm({ title, body, confirmLabel, danger: true });
		} finally { setBusy(false); }
		if (confirmed) await write(url, { method: 'DELETE' }, null, message, () => nextState);
		else { showSheet(); trigger.focus(); }
	}
	async function load() {
		if (busy) return;
		setBusy(true);
		loadError.querySelector('button').disabled = true;
		const loaded = await reload();
		setBusy(false);
		loadError.querySelector('button').disabled = false;
		if (loaded && document.activeElement === loadError.querySelector('button')) focusPanel();
	}

	/* ---------- creating routes straight from the line and the table ---------- */

	const find = id => network.stations.find(station => station.id === Number(id));
	const pairOf = text => text.split('-').map(find);
	const label = (a, b) => `${a.name} → ${b.name}`;

	/** Creates the given directions and offers to undo them in the toast. */
	async function create(directions, message) {
		const before = new Set(network.routes.map(route => route.id));
		const ok = await commit(async () => {
			for (const [origin, destination, distanceKm] of directions) {
				await TG.api('/api/admin/routes', { method: 'POST', body: {
					originStationId: origin.id, destinationStationId: destination.id, distanceKm, bothWays: false } });
			}
		}, state);
		if (!ok) return;
		const created = network.routes.filter(route => !before.has(route.id));
		TG.toast(message, { label: 'Hoàn tác', run: () => commit(async () => {
			for (const route of created) await TG.api(`/api/admin/routes/${route.id}`, { method: 'DELETE' });
		}, state) });
	}
	function missingOf(a, b) {
		const km = Math.abs(b.kmMark - a.kmMark);
		const list = [];
		if (!TG.stationLine.routeOf(network, a, b)) list.push([a, b, km]);
		if (!TG.stationLine.routeOf(network, b, a)) list.push([b, a, km]);
		return list;
	}

	split.addEventListener('click', event => {
		const hit = name => event.target.closest(`[${name}]`);
		let target;
		if ((target = hit('data-fill'))) {
			const [a, b] = pairOf(target.dataset.fill);
			create(missingOf(a, b), `Đã tạo tuyến ${a.name} ⇄ ${b.name}.`);
		} else if ((target = hit('data-fill-all'))) {
			const all = TG.stationLine.gaps(network).flatMap(([a, b]) => missingOf(a, b));
			create(all, `Đã tạo ${all.length} tuyến.`);
		} else if ((target = hit('data-make'))) {
			const [from, to] = pairOf(target.dataset.make);
			const back = TG.stationLine.routeOf(network, to, from);
			create([[from, to, back.distanceKm]], `Đã tạo tuyến ${label(from, to)}.`);
		} else if ((target = hit('data-station'))) {
			select({ type: 'station', id: Number(target.dataset.station) });
		} else if ((target = hit('data-clear-search'))) {
			search.value = '';
			routesHost.innerHTML = TG.stationLine.routes(network, '');
			search.focus();
		} else if ((target = hit('data-pair'))) {
			const [a, b] = target.dataset.pair.split('-').map(Number);
			select({ type: 'pair', a, b });
		}
	});
	search.addEventListener('input', () => {
		routesHost.innerHTML = TG.stationLine.routes(network, search.value);
	});
	root.querySelector('[data-add-station]').addEventListener('click', () => {
		const list = network.stations;
		select({ type: 'gap', a: list[0].id, b: list[list.length - 1].id, add: true });
	});
	// Closing the sheet by hand goes back to the lists.
	sheet.addEventListener('hidden.bs.offcanvas', () => {
		if (!busy && state.type !== 'overview') {
			state = { type: 'overview' };
			panel.replaceChildren();
		}
	});
	loadError.querySelector('button').addEventListener('click', load);
	load();
})();
