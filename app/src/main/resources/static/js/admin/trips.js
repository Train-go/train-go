/*
 * /admin/trips (templates/admin/trips.html). The list is rendered by the
 * server and trip-sheet.js opens a trip from its row; this file only submits
 * the filter form when the train or the sort changes, and shortly after the
 * last key in the station search.
 *
 * Loaded with defer, after admin.js.
 */
(function () {
	'use strict';

	const form = document.querySelector('[data-trip-filters]');
	['trip-train', 'trip-sort'].forEach(function (id) {
		document.getElementById(id).addEventListener('change', function () {
			form.requestSubmit();
		});
	});
	TG.searchBox(form, document.getElementById('trip-q'));
})();
