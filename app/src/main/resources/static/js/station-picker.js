/*
 * Turns each select[data-station-picker] into a combobox: the box itself is the
 * search, typed without diacritics if the visitor likes. The select stays in the
 * form, hidden, and keeps the value that is submitted, so the page still works
 * without this script. Setting the select from outside needs a change event.
 */
(function () {
	'use strict';

	function fold(text) {
		return text.normalize('NFD').replace(/[̀-ͯ]/g, '').replace(/[đĐ]/g, 'd').toLowerCase().trim();
	}

	function enhance(select) {
		var control = select.closest('.control');
		var label = document.querySelector('label[for="' + select.id + '"]');
		var options = Array.prototype.filter.call(select.options, function (option) { return option.value; }).map(function (option) {
			return { value: option.value, name: option.textContent, key: fold(option.textContent) };
		});
		var shown = [];
		var active = -1;

		var input = document.createElement('input');
		input.type = 'text';
		input.id = select.id + '-input';
		input.placeholder = select.options[0].textContent;
		input.autocomplete = 'off';
		input.spellcheck = false;
		input.setAttribute('role', 'combobox');
		input.setAttribute('aria-autocomplete', 'list');
		input.setAttribute('aria-expanded', 'false');
		input.setAttribute('aria-controls', select.id + '-list');

		var list = document.createElement('ul');
		list.id = select.id + '-list';
		list.className = 'picker-list';
		list.setAttribute('role', 'listbox');
		list.hidden = true;
		if (label) {
			label.htmlFor = input.id;
			label.id = label.id || select.id + '-label';
			list.setAttribute('aria-labelledby', label.id);
		}

		var chevron = document.createElement('i');
		chevron.className = 'bi bi-chevron-down picker-chevron';
		chevron.setAttribute('aria-hidden', 'true');

		// A server-side error on the select now describes the box the visitor sees.
		if (select.hasAttribute('aria-describedby')) {
			input.setAttribute('aria-describedby', select.getAttribute('aria-describedby'));
			input.setAttribute('aria-invalid', 'true');
		}

		select.hidden = true;
		select.tabIndex = -1;
		select.after(input, chevron, list);

		function selectedName() {
			return select.value ? select.options[select.selectedIndex].textContent : '';
		}

		function setActive(index) {
			var previous = list.children[active];
			if (previous) { previous.classList.remove('is-active'); }
			active = index;
			var item = list.children[active];
			if (!item || !shown.length) {
				input.removeAttribute('aria-activedescendant');
				return;
			}
			item.classList.add('is-active');
			input.setAttribute('aria-activedescendant', item.id);
			// Scrolls the list only, never the page behind it.
			if (item.offsetTop < list.scrollTop) {
				list.scrollTop = item.offsetTop;
			} else if (item.offsetTop + item.offsetHeight > list.scrollTop + list.clientHeight) {
				list.scrollTop = item.offsetTop + item.offsetHeight - list.clientHeight;
			}
		}

		function render(query) {
			var key = fold(query);
			shown = options.filter(function (option) { return option.key.indexOf(key) !== -1; });
			list.textContent = '';
			active = -1;
			shown.forEach(function (option, index) {
				var item = document.createElement('li');
				item.id = select.id + '-option-' + index;
				item.setAttribute('role', 'option');
				item.setAttribute('aria-selected', String(option.value === select.value));
				item.textContent = option.name;
				list.appendChild(item);
			});
			if (!shown.length) {
				var empty = document.createElement('li');
				empty.className = 'picker-empty';
				empty.textContent = 'Không tìm thấy ga';
				list.appendChild(empty);
			}
		}

		function isOpen() {
			return !list.hidden;
		}

		// Opens downward while a useful list fits, else toward the larger room, and
		// shrinks to that room so it is neither cut by the screen nor under the header.
		function place() {
			var box = control.getBoundingClientRect();
			var header = document.getElementById('header');
			var below = window.innerHeight - box.bottom - 12;
			var above = box.top - (header ? header.offsetHeight : 0) - 12;
			var up = below < 200 && above > below;
			control.classList.toggle('is-picker-up', up);
			list.style.maxHeight = Math.min(288, up ? above : below) + 'px';
		}

		function open(query) {
			render(query);
			list.hidden = false;
			control.classList.add('is-picker-open');
			input.setAttribute('aria-expanded', 'true');
			list.scrollTop = 0;
			place();
			var selected = shown.findIndex(function (option) { return option.value === select.value; });
			setActive(query ? 0 : selected);
		}

		function close() {
			list.hidden = true;
			control.classList.remove('is-picker-open', 'is-picker-up');
			input.setAttribute('aria-expanded', 'false');
			input.removeAttribute('aria-activedescendant');
		}

		function choose(option) {
			select.value = option.value;
			input.value = option.name;
			close();
			select.dispatchEvent(new Event('change', { bubbles: true }));
		}

		// Leaving the box: an emptied box clears the choice, half-typed text goes back to it.
		function settle() {
			if (!input.value.trim() && select.value) {
				select.value = '';
				select.dispatchEvent(new Event('change', { bubbles: true }));
			}
			input.value = selectedName();
			close();
		}

		control.addEventListener('mousedown', function (event) {
			if (event.target !== input) {
				event.preventDefault();
			}
		});
		control.addEventListener('click', function (event) {
			if (list.contains(event.target)) {
				return;
			}
			if (document.activeElement !== input) {
				input.focus();
			}
			if (!isOpen()) {
				open('');
				input.select();
			} else if (event.target !== input) {
				close();
			}
		});
		list.addEventListener('click', function (event) {
			var item = event.target.closest('[role="option"]');
			if (item) {
				choose(shown[Array.prototype.indexOf.call(list.children, item)]);
			}
		});
		input.addEventListener('input', function () {
			open(input.value);
		});
		input.addEventListener('blur', settle);
		input.addEventListener('keydown', function (event) {
			if (event.key === 'ArrowDown' || event.key === 'ArrowUp') {
				event.preventDefault();
				if (!isOpen()) {
					open('');
				} else if (shown.length) {
					var step = event.key === 'ArrowDown' ? 1 : -1;
					setActive(active < 0 && step < 0 ? shown.length - 1 : (active + step + shown.length) % shown.length);
				}
			} else if (event.key === 'Enter' && isOpen()) {
				// Enter picks from the open list; it submits only once the list is closed.
				event.preventDefault();
				if (shown[active]) {
					choose(shown[active]);
				}
			} else if (event.key === 'Tab' && isOpen() && shown[active]) {
				choose(shown[active]);
			} else if (event.key === 'Escape' && isOpen()) {
				input.value = selectedName();
				close();
			}
		});
		select.addEventListener('change', function () {
			input.value = selectedName();
		});
		// The browser restores the select when the visitor comes back with Back.
		window.addEventListener('pageshow', function () {
			input.value = selectedName();
		});
		input.value = selectedName();
	}

	document.querySelectorAll('select[data-station-picker]').forEach(enhance);
})();
