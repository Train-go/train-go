(function () {
	'use strict';
	const root = document.getElementById('station-network');
	if (!root) return;
	const split = root.querySelector('.net-split');
	const panel = root.querySelector('.net-panel');
	const overview = root.querySelector('#net-overview-btn');
	const loadError = root.querySelector('#net-load-error');
	let network = { stations: [], routes: [] };
	let state = { type: 'overview' };
	let busy = false;

	function focusPanel() {
		panel.querySelector('#net-panel-h')?.focus({ preventScroll: true });
		if (window.matchMedia('(max-width: 1099px)').matches) {
			panel.scrollIntoView({ block: 'start', behavior: 'instant' });
		}
	}
	function render(focus = false) {
		const ids = new Set(network.stations.map(station => station.id));
		if ((['station', 'suggest'].includes(state.type) && !ids.has(state.id))
			|| (['pair', 'gap'].includes(state.type) && (!ids.has(state.a) || !ids.has(state.b)))) state = { type: 'overview' };
		root.querySelector('#net-count').textContent = `${network.stations.length} ga · ${network.routes.length} tuyến`;
		overview.hidden = state.type === 'overview';
		TG.lineMap(root.querySelector('#net-map-content'), network, {
			selected: state,
			onStation: id => select({ type: 'station', id }),
			onPair: (a, b) => select({ type: 'pair', a, b }),
			onGap: (a, b) => select({ type: 'gap', a, b })
		});
		TG.stationPanels(panel, { network, state, select, write, remove, commit });
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
		overview.disabled = value;
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
			overview.hidden = true;
			root.querySelector('#net-count').textContent = 'Chưa tải được dữ liệu';
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
		try { confirmed = await TG.confirm({ title, body, confirmLabel, danger: true }); }
		finally { setBusy(false); }
		if (confirmed) await write(url, { method: 'DELETE' }, null, message, () => nextState);
		else trigger.focus();
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
	overview.addEventListener('click', () => select({ type: 'overview' }));
	loadError.querySelector('button').addEventListener('click', load);
	load();
})();
