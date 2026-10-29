/*
 * The "chèn ga" form of the Ga và tuyến page, made smart (#38).
 *
 *   TG.stationDraft(form, { stations, slot })
 *
 * The name drives the code, the province and the km until the admin types
 * into one of them: a field the admin edited (`touched`) is never overwritten
 * again. The name box is a combobox of the real stations of the line that the
 * system lacks, those inside the gap first. `slot` is { lo, hi, prev, next }
 * for the gap being filled.
 */
(function () {
	'use strict';

	const LIST_ID = 'station-name-list';

	function freeMid(stations, lo, hi) {
		let mid = Math.round((lo + hi) / 2);
		while (stations.some(station => station.kmMark === mid)) mid += 1;
		return mid;
	}

	TG.stationDraft = function (form, { stations, slot }) {
		const name = form.elements.name;
		const code = form.elements.code;
		const city = form.elements.city;
		const km = form.elements.kmMark;
		const taken = new Set(stations.map(station => station.code));
		const touched = {};
		let active = 0;
		let shown = false;
		let blurTimer = null;

		// The name input becomes a combobox: wrap it, and add the list under it.
		const combo = document.createElement('div');
		combo.className = 'combo';
		name.before(combo);
		combo.append(name);
		const list = document.createElement('ul');
		list.className = 'combo-list';
		list.id = LIST_ID;
		list.setAttribute('role', 'listbox');
		list.setAttribute('aria-label', 'Ga có thật trên tuyến Thống Nhất');
		list.hidden = true;
		combo.append(list);
		name.setAttribute('role', 'combobox');
		name.setAttribute('aria-autocomplete', 'list');
		name.setAttribute('aria-controls', LIST_ID);
		name.setAttribute('aria-expanded', 'false');
		name.setAttribute('autocomplete', 'off');
		name.placeholder = 'Ví dụ Quảng Ngãi';

		// Notes about what was filled in, under the km field.
		const notes = document.createElement('div');
		notes.className = 'draft-notes';
		notes.setAttribute('aria-live', 'polite');
		km.closest('div').append(notes);
		const warning = form.querySelector('#km-range-warning');
		if (warning) {
			warning.textContent = `Km nằm ngoài khoảng giữa ${slot.prev.name.replace(/^Ga\s+/i, '')} (${slot.prev.kmMark}) và ${slot.next.name.replace(/^Ga\s+/i, '')} (${slot.next.kmMark}).`;
			km.setAttribute('aria-describedby', 'km-range-warning');
		}

		const options = () => TG.knownPlaces(stations, name.value, slot).slice(0, 6);
		const exactKnown = () => TG.GAZETTEER.find(place => TG.fold(place.name) === TG.fold(TG.placeName(name.value))) || null;

		function renderList() {
			const found = options();
			const only = found.length === 1 && TG.fold(found[0].name) === TG.fold(TG.placeName(name.value));
			shown = !only && found.length > 0 && document.activeElement === name;
			list.hidden = !shown;
			name.setAttribute('aria-expanded', String(shown));
			if (shown) name.setAttribute('aria-activedescendant', `${LIST_ID}-${active}`);
			else name.removeAttribute('aria-activedescendant');
			list.replaceChildren();
			if (!shown) return;
			found.forEach((place, index) => {
				const item = document.createElement('li');
				item.id = `${LIST_ID}-${index}`;
				item.setAttribute('role', 'option');
				item.setAttribute('aria-selected', String(index === active));
				const title = document.createElement('b');
				title.textContent = place.name;
				const sub = document.createElement('small');
				sub.textContent = `Km ${place.km} · ${place.city}`;
				item.append(title, sub);
				if (place.inRange) {
					const tag = document.createElement('span');
					tag.className = 'in-range';
					tag.textContent = 'Trong khoảng';
					item.append(tag);
				}
				// mousedown, not click: the input would lose focus first and close the list.
				item.addEventListener('mousedown', event => { event.preventDefault(); choose(place); });
				item.addEventListener('mouseenter', () => { active = index; markActive(); });
				list.append(item);
			});
		}

		function markActive() {
			list.querySelectorAll('[role=option]').forEach((item, index) => item.setAttribute('aria-selected', String(index === active)));
			name.setAttribute('aria-activedescendant', `${LIST_ID}-${active}`);
		}

		function choose(place) {
			name.value = `Ga ${place.name}`;
			touched.code = touched.city = touched.km = false;
			active = 0;
			update();
			shown = false;
			list.hidden = true;
			name.setAttribute('aria-expanded', 'false');
			name.removeAttribute('aria-activedescendant');
		}

		/** Recomputes every field the admin has not edited, and the notes. */
		function update() {
			const known = exactKnown();
			const codes = TG.suggestCodes(name.value, taken);
			const autoKm = known ? known.km : freeMid(stations, slot.lo, slot.hi);
			if (!touched.code) code.value = codes[0] || '';
			if (!touched.city) city.value = known ? known.city : '';
			if (!touched.km) km.value = autoKm;
			if (warning) {
				const value = Number(km.value);
				warning.hidden = km.value === '' || (value > slot.lo && value < slot.hi);
			}
			notes.replaceChildren();
			const add = text => {
				const line = document.createElement('p');
				line.className = 'form-text';
				line.textContent = text;
				notes.append(line);
			};
			if (!touched.km && known) add(`Km ${known.km}, mã và tỉnh điền theo ga ${known.name}.`);
			else if (!touched.km && name.value.trim()) add(`Km ${autoKm} là điểm giữa hai ga, sửa nếu biết số chính xác.`);
			if (!touched.code && codes.length > 1) add(`Mã khác còn trống: ${codes.slice(1).join(', ')}.`);
			if (touched.km && Number(km.value) !== autoKm) {
				const reset = document.createElement('button');
				reset.type = 'button';
				reset.className = 'btn btn-link';
				reset.textContent = 'Dùng lại km gợi ý';
				reset.addEventListener('click', () => { touched.km = false; update(); km.focus(); });
				notes.append(reset);
			}
		}

		name.addEventListener('input', () => { active = 0; update(); renderList(); });
		name.addEventListener('focus', () => { clearTimeout(blurTimer); renderList(); });
		name.addEventListener('blur', () => {
			blurTimer = setTimeout(() => { shown = false; list.hidden = true; name.setAttribute('aria-expanded', 'false'); }, 120);
		});
		name.addEventListener('keydown', event => {
			if (!shown) return;
			const found = options();
			if (event.key === 'ArrowDown') { event.preventDefault(); active = (active + 1) % found.length; markActive(); }
			else if (event.key === 'ArrowUp') { event.preventDefault(); active = (active - 1 + found.length) % found.length; markActive(); }
			else if (event.key === 'Enter') { event.preventDefault(); choose(found[active]); }
			else if (event.key === 'Escape') {
				// Close the list only: the panel and its form stay open.
				event.preventDefault();
				event.stopPropagation();
				shown = false;
				list.hidden = true;
				name.setAttribute('aria-expanded', 'false');
				name.removeAttribute('aria-activedescendant');
			}
		});
		['code', 'city', 'kmMark'].forEach(key => {
			form.elements.namedItem(key).addEventListener('input', () => {
				touched[key === 'kmMark' ? 'km' : key] = true;
				update();
			});
		});

		update();
	};
})();
