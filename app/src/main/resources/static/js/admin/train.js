/*
 * The train page (templates/admin/train.html): the "Hiện N chặng đã chạy"
 * toggle, the add/edit sheet and soft delete. Saving and deleting go through
 * TG.api to /api/admin/trains; on success the browser opens the right train
 * page and TG.toastAfterReload shows the confirmation there. Restoring a
 * deleted train lives on the history page (history.js).
 */
(function () {
	'use strict';

	/* ---------- trips that have already run ---------- */

	const pastToggle = document.querySelector('[data-past-toggle]');
	const agenda = document.getElementById('agenda');
	if (pastToggle && agenda) {
		const count = pastToggle.dataset.count;
		const icon = pastToggle.querySelector('.bi');
		const label = pastToggle.querySelector('span');
		pastToggle.addEventListener('click', function () {
			const show = !agenda.classList.contains('show-past');
			agenda.classList.toggle('show-past', show);
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
			location.assign(pageUrl + '/' + saved.id);
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
			location.assign(pageUrl);
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
