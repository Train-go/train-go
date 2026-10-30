/*
 * Trip list conveniences (templates/trips/search.html). The page works
 * without this file: every search, day, filter and order is a GET link or a
 * GET form on /trips, and the server checks every value.
 */
(function () {
	'use strict';

	var fromField = document.getElementById('find-from');
	var toField = document.getElementById('find-to');
	document.getElementById('find-swap').addEventListener('click', function () {
		var origin = fromField.value;
		fromField.value = toField.value;
		toField.value = origin;
	});

	// On a phone the strip scrolls sideways; bring the day being shown to its middle.
	var shownDay = document.querySelector('.dates a[aria-current="date"]');
	if (shownDay) {
		var strip = shownDay.closest('ul');
		var stripBox = strip.getBoundingClientRect();
		var dayBox = shownDay.getBoundingClientRect();
		strip.scrollLeft += dayBox.left - stripBox.left - (stripBox.width - dayBox.width) / 2;
	}

	// The day label opens the calendar; picking a day keeps stations and filters.
	var pickDate = document.getElementById('pick-date');
	if (pickDate) {
		pickDate.addEventListener('click', function () {
			try {
				pickDate.showPicker();
			} catch (e) {
				// Browsers without showPicker() open the calendar on their own.
			}
		});
		pickDate.addEventListener('change', function () {
			if (!pickDate.value || pickDate.value < pickDate.min) {
				return;
			}
			var url = new URL(window.location.href);
			url.searchParams.set('date', pickDate.value);
			window.location.assign(url.toString());
		});
	}

	// A ticked filter sends the form at once, so its submit button is not needed.
	var filterForm = document.getElementById('filters');
	if (filterForm) {
		filterForm.querySelector('.filter-apply').hidden = true;
		filterForm.addEventListener('change', function () {
			filterForm.requestSubmit();
		});
	}
})();
