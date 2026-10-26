/*
 * /admin/trips (templates/admin/trips.html). The list is rendered by the
 * server; this file only:
 *
 *   - submits the filter form when the train changes, and 300 ms after the
 *     last key in the station search;
 *   - opens the trip sheet (trip-sheet.js) from a row, by click or Enter;
 *   - reloads after a cancel, so the counts and groups come from the server,
 *     and reopens the same trip's sheet through ?open=<id>.
 *
 * Loaded with defer, after admin.js and trip-sheet.js.
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

	/* ---------- rows ---------- */

	function openRow(row) {
		TG.openTrip(row.dataset.tripId, row);
	}

	document.querySelectorAll('tr[data-trip-id]').forEach(function (row) {
		row.addEventListener('click', function (event) {
			if (!event.target.closest('a, button')) {
				openRow(row);
			}
		});
		row.addEventListener('keydown', function (event) {
			if (event.key === 'Enter') {
				event.preventDefault();
				openRow(row);
			}
		});
	});

	document.addEventListener('trip:cancelled', function (event) {
		event.preventDefault();
		TG.toastAfterReload(event.detail.message);
		const url = new URL(location.href);
		url.searchParams.set('open', event.detail.tripId);
		location.replace(url);
	});

	/* ?open=<id> reopens a sheet after the reload above, or from a shared link. */
	const url = new URL(location.href);
	const openId = url.searchParams.get('open');
	if (openId) {
		url.searchParams.delete('open');
		history.replaceState(null, '', url);
		const row = document.querySelector('tr[data-trip-id="' + CSS.escape(openId) + '"]');
		TG.openTrip(openId, row);
	}
})();
