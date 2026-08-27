/*
 * Landing page interactions: default date, swap stations, prefill from a
 * popular route card, and a client-side guard for same-origin-and-destination.
 *
 * The backend must repeat every check here — SPEC.md 45.2. Nothing in this file
 * is a substitute for server-side validation.
 */
(function () {
	'use strict';

	var form = document.getElementById('tgSearchForm');
	if (!form) {
		return;
	}

	var fromField = document.getElementById('tgFrom');
	var toField = document.getElementById('tgTo');
	var dateField = document.getElementById('tgDate');
	var errorBox = document.getElementById('tgSearchError');

	// Today in local time. toISOString() would shift the date in UTC+7 for
	// anyone loading the page before 07:00.
	function today() {
		var now = new Date();
		var month = String(now.getMonth() + 1).padStart(2, '0');
		var day = String(now.getDate()).padStart(2, '0');
		return now.getFullYear() + '-' + month + '-' + day;
	}

	var startOfToday = today();
	dateField.min = startOfToday;
	if (!dateField.value) {
		dateField.value = startOfToday;
	}

	function showError(message) {
		errorBox.textContent = message;
		errorBox.classList.remove('d-none');
	}

	function clearError() {
		errorBox.textContent = '';
		errorBox.classList.add('d-none');
	}

	document.getElementById('tgSwap').addEventListener('click', function () {
		var origin = fromField.value;
		fromField.value = toField.value;
		toField.value = origin;
		clearError();
	});

	document.querySelectorAll('.tg-route-card').forEach(function (card) {
		card.addEventListener('click', function () {
			fromField.value = card.dataset.from;
			toField.value = card.dataset.to;
			clearError();
			form.scrollIntoView({ behavior: 'smooth', block: 'center' });
		});
	});

	form.addEventListener('submit', function (event) {
		if (fromField.value && fromField.value === toField.value) {
			event.preventDefault();
			showError('Ga đi và ga đến phải khác nhau.');
			return;
		}
		if (dateField.value < startOfToday) {
			event.preventDefault();
			showError('Ngày đi không được ở quá khứ.');
			return;
		}
		clearError();
	});
})();
