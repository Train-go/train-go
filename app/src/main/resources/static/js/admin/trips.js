/*
 * /admin/trips (templates/admin/trips.html). The list is rendered by the
 * server and trip-sheet.js opens a trip from its row; this file only submits
 * the filter form when the train changes, and 300 ms after the last key in
 * the station search.
 *
 * Loaded with defer, after admin.js.
 */
(function () {
	'use strict';

	const SEARCH_DELAY_MS = 300;
	const FOCUS_SEARCH_KEY = 'tg.trips.focusSearch';

	const form = document.querySelector('[data-trip-filters]');
	const search = document.getElementById('trip-q');
	let searchTimer = null;

	document.getElementById('trip-train').addEventListener('change', function () {
		form.requestSubmit();
	});

	search.addEventListener('input', function () {
		clearTimeout(searchTimer);
		searchTimer = setTimeout(function () {
			// The page reloads while the admin is typing; put the cursor back afterwards.
			try {
				sessionStorage.setItem(FOCUS_SEARCH_KEY, '1');
			} catch (ignored) {
				// Private mode: the search still works, focus just stays lost.
			}
			form.requestSubmit();
		}, SEARCH_DELAY_MS);
	});

	try {
		if (sessionStorage.getItem(FOCUS_SEARCH_KEY)) {
			sessionStorage.removeItem(FOCUS_SEARCH_KEY);
			search.focus();
			search.setSelectionRange(search.value.length, search.value.length);
		}
	} catch (ignored) {
		// See above.
	}
})();
