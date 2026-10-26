/*
 * The train page (templates/admin/train.html): the remembered view, the
 * "Hiện N chặng đã chạy" toggle, the add/edit sheet and soft delete. Saving
 * and deleting go through TG.api to /api/admin/trains; on success the browser
 * opens the right train page and TG.toastAfterReload shows the confirmation
 * there. Restoring a deleted train lives on the history page (history.js).
 * The "Bảng tính" view's own behaviour is in train-sheet.js.
 */
(function () {
	'use strict';

	/* ---------- the view of the last visit ---------- */

	/*
	 * ?view=ngay or ?view=bang is remembered. A train page opened without one,
	 * which is what the "Tàu" menu leads to, shows the remembered view. Storage
	 * can throw (private windows, blocked site data); then the view stays "Theo
	 * ngày".
	 */
	const VIEW_KEY = 'traingo.trainView';

	function rememberedSheetView() {
		try {
			return localStorage.getItem(VIEW_KEY) === 'bang';
		} catch (storageError) {
			return false;
		}
	}

	/*
	 * Where the browser goes after saving or deleting: the remembered view goes
	 * along, because a page that redirects again would swallow the toast that
	 * TG.toastAfterReload is waiting to show.
	 */
	function withView(url) {
		return rememberedSheetView() ? url + '?view=bang' : url;
	}

	const viewParam = new URLSearchParams(location.search).get('view');
	if (viewParam === 'ngay' || viewParam === 'bang') {
		try {
			localStorage.setItem(VIEW_KEY, viewParam);
		} catch (storageError) {
			// Nothing remembered; the page stays as the server rendered it.
		}
	} else if (rememberedSheetView()) {
		const url = new URL(location.href);
		url.searchParams.set('view', 'bang');
		location.replace(url);
		return;
	}

	/* ---------- trips that have already run ---------- */

	// The toggle names what it shows or hides in aria-controls: the agenda of
	// the day view or the schedule table of the sheet view.
	const pastToggle = document.querySelector('[data-past-toggle]');
	const pastTarget = pastToggle && document.getElementById(pastToggle.getAttribute('aria-controls'));
	if (pastToggle && pastTarget) {
		const count = pastToggle.dataset.count;
		const icon = pastToggle.querySelector('.bi');
		const label = pastToggle.querySelector('span');
		pastToggle.addEventListener('click', function () {
			const show = !pastTarget.classList.contains('show-past');
			pastTarget.classList.toggle('show-past', show);
			pastToggle.setAttribute('aria-expanded', show ? 'true' : 'false');
			icon.className = show ? 'bi bi-chevron-up' : 'bi bi-clock-history';
			label.textContent = (show ? 'Ẩn ' : 'Hiện ') + count + ' chặng đã chạy';
		});
	}

	/* ---------- add and edit sheet ---------- */

	const form = document.getElementById('train-form');
	if (!form) {
		return;
	}
	const title = document.getElementById('train-sheet-title');
	const submit = document.querySelector('[data-train-submit]');
	const codeInput = form.elements.namedItem('code');
	const nameInput = form.elements.namedItem('name');
	const codeHint = document.getElementById('train-code-hint');
	const deleteButton = document.querySelector('[data-train-delete]');
	const apiUrl = form.dataset.apiUrl;
	const pageUrl = form.dataset.pageUrl;
	const HINT = codeHint.textContent;
	const LOCKED = 'Mã tàu không đổi được khi tàu đã có chuyến.';

	// The train being edited, or null while adding one.
	let editing = null;

	function openSheet(trigger, train) {
		editing = train;
		TG.clearFieldErrors(form);
		title.textContent = train ? 'Sửa tàu ' + train.code : 'Thêm tàu';
		submit.textContent = train ? 'Lưu' : 'Thêm tàu';
		codeInput.value = train ? train.code : '';
		nameInput.value = train ? train.name : '';
		codeInput.disabled = Boolean(train && train.hasTrips);
		codeHint.textContent = codeInput.disabled ? LOCKED : HINT;
		// Only an existing train can be deleted.
		if (deleteButton) {
			deleteButton.hidden = !train;
		}
		TG.sheet.open('train-sheet', trigger);
	}

	const addButton = document.querySelector('[data-train-add]');
	addButton.addEventListener('click', function () {
		openSheet(addButton, null);
	});

	const editButton = document.querySelector('[data-train-edit]');
	if (editButton) {
		editButton.addEventListener('click', function () {
			openSheet(editButton, {
				id: editButton.dataset.id,
				code: editButton.dataset.code,
				name: editButton.dataset.name,
				hasTrips: editButton.dataset.hasTrips === 'true'
			});
		});
	}

	// Codes are stored upper case; show them that way while typing.
	codeInput.addEventListener('input', function () {
		const caret = codeInput.selectionStart;
		codeInput.value = codeInput.value.toUpperCase();
		codeInput.setSelectionRange(caret, caret);
	});

	form.addEventListener('submit', async function (event) {
		event.preventDefault();
		TG.clearFieldErrors(form);
		const body = { code: codeInput.value.trim(), name: nameInput.value.trim() };
		const url = editing ? apiUrl + '/' + editing.id : apiUrl;
		submit.disabled = true;
		try {
			const saved = await TG.api(url, { method: editing ? 'PUT' : 'POST', body: body });
			const code = body.code.toUpperCase();
			TG.toastAfterReload((editing ? 'Đã lưu tàu ' : 'Đã thêm tàu ') + code + '.');
			location.assign(withView(pageUrl + '/' + saved.id));
		} catch (error) {
			if (error.fields && Object.keys(error.fields).length) {
				TG.showFieldErrors(form, error.fields);
			} else {
				TG.toast(error.message, { error: true });
			}
			submit.disabled = false;
		}
	});

	/* ---------- soft delete, undone from the toast or the history page ---------- */

	async function confirmDelete(train) {
		const yes = await TG.confirm({
			title: 'Xóa tàu ' + train.code + '?',
			body: 'Có thể khôi phục lại trong Lịch sử.',
			confirmLabel: 'Xóa tàu',
			danger: true
		});
		if (!yes) {
			return;
		}
		try {
			await TG.api(apiUrl + '/' + train.id, { method: 'DELETE' });
			TG.toastAfterReload('Đã xóa tàu ' + train.code + '.',
					{ label: 'Hoàn tác', url: apiUrl + '/' + train.id + '/restore', method: 'POST' });
			location.assign(withView(pageUrl));
		} catch (error) {
			TG.toast(error.message, { error: true });
		}
	}

	if (deleteButton) {
		deleteButton.addEventListener('click', function () {
			const train = editing;
			// Close the sheet before asking: a modal over an open offcanvas
			// fights it for focus.
			document.getElementById('train-sheet').addEventListener('hidden.bs.offcanvas', function () {
				confirmDelete(train);
			}, { once: true });
			TG.sheet.close('train-sheet');
		});
	}
})();
