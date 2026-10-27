/*
 * Trip list conveniences (templates/trips/search.html). The page works
 * without this file: every search, day, filter and order is a GET link or a
 * GET form on /trips, and the server checks every value.
 */
(function () {
	'use strict';

	var fromField = document.getElementById('tgFindFrom');
	var toField = document.getElementById('tgFindTo');
	document.getElementById('tgFindSwap').addEventListener('click', function () {
		var origin = fromField.value;
		fromField.value = toField.value;
		toField.value = origin;
	});

	// On a phone the strip scrolls sideways; bring the day being shown to its middle.
	var shownDay = document.querySelector('.tg-day[aria-current="date"]');
	if (shownDay) {
		var strip = shownDay.parentElement;
		var stripBox = strip.getBoundingClientRect();
		var dayBox = shownDay.getBoundingClientRect();
		strip.scrollLeft += dayBox.left - stripBox.left - (stripBox.width - dayBox.width) / 2;
	}

	// The day label opens the calendar; picking a day keeps stations and filters.
	var pickDate = document.getElementById('tgPickDate');
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
	var filterForm = document.getElementById('tgFilterForm');
	if (filterForm) {
		filterForm.querySelector('.tg-filters-apply').hidden = true;
		filterForm.addEventListener('change', function () {
			filterForm.requestSubmit();
		});
	}

	// On a phone the filters fold behind the "Lọc chuyến" button.
	var filters = document.getElementById('tgFilters');
	var toggle = document.getElementById('tgFiltersToggle');
	if (filters && toggle) {
		filters.classList.add('is-foldable');
		toggle.hidden = false;
		toggle.addEventListener('click', function () {
			var open = filters.classList.toggle('is-open');
			toggle.setAttribute('aria-expanded', String(open));
		});
	}
})();
