/*
 * Homepage interactions: the header over the photo, the reveal on scroll, the
 * swap button and a client-side guard for the search. The backend repeats every
 * check here (SPEC.md 45.2).
 */
(function () {
	'use strict';

	// Clear over the photo, glass once the page scrolls.
	var header = document.getElementById('header');
	if (header && header.classList.contains('is-fixed')) {
		var onScroll = function () {
			header.classList.toggle('is-over-photo', window.scrollY < 40);
		};
		onScroll();
		window.addEventListener('scroll', onScroll, { passive: true });
	}

	// The page is fully visible without this: is-armed is what hides a block until it is seen.
	var revealed = document.querySelectorAll('[data-reveal]');
	if ('IntersectionObserver' in window && revealed.length && !matchMedia('(prefers-reduced-motion: reduce)').matches) {
		var observer = new IntersectionObserver(function (entries) {
			entries.forEach(function (entry) {
				if (entry.isIntersecting) {
					entry.target.classList.add('is-in');
					observer.unobserve(entry.target);
				}
			});
		}, { rootMargin: '0px 0px -8% 0px' });
		revealed.forEach(function (el) {
			el.classList.add('is-armed');
			observer.observe(el);
		});
	}

	var form = document.getElementById('search');
	var from = document.getElementById('search-from');
	var to = document.getElementById('search-to');
	var date = document.getElementById('search-date');
	var error = document.getElementById('search-error');

	// What the visitor actually types in: station-picker.js hides the select behind an input.
	function box(field) {
		return field.closest('.control').querySelector('input') || field;
	}

	function clear() {
		[from, to, date].forEach(function (field) {
			field.closest('.control').classList.remove('is-invalid');
			box(field).removeAttribute('aria-invalid');
			box(field).removeAttribute('aria-describedby');
		});
		error.textContent = '';
	}

	function fail(event, field, message) {
		event.preventDefault();
		field.closest('.control').classList.add('is-invalid');
		box(field).setAttribute('aria-invalid', 'true');
		box(field).setAttribute('aria-describedby', 'search-error');
		error.textContent = message;
		box(field).focus();
	}

	document.getElementById('search-swap').addEventListener('click', function () {
		var origin = from.value;
		from.value = to.value;
		to.value = origin;
		[from, to].forEach(function (field) {
			field.dispatchEvent(new Event('change', { bubbles: true }));
		});
		clear();
	});

	form.addEventListener('submit', function (event) {
		clear();
		if (!from.value) {
			fail(event, from, 'Chọn ga đi.');
		} else if (!to.value) {
			fail(event, to, 'Chọn ga đến.');
		} else if (from.value === to.value) {
			fail(event, to, 'Ga đến phải khác ga đi.');
		} else if (!date.value || date.value < date.min) {
			fail(event, date, 'Chọn ngày từ hôm nay trở đi.');
		}
	});
})();
