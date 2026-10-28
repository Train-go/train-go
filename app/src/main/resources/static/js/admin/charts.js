/*
 * A small chart for the admin overview: bars on the left axis and a line on
 * the right axis, drawn as SVG so it needs no library. Colours and fonts come
 * from admin.css (.ch-*), so the chart follows the theme.
 *
 *   TG.chart(container, {
 *     label,                              what a screen reader hears for the whole chart
 *     labels: ['29/10', ...],             one per column (a space makes two lines)
 *     titles?: ['Tuần 26/10 - 01/11', ...]  what a tooltip and the screen reader table say instead
 *     bars:  { name, values, format, tick, integer? },
 *     line:  { name, values, format, tick, max?, integer? },
 *     empty:                              text shown when every value is zero
 *   })
 *
 * `format` writes a value in a tooltip, `tick` writes it on an axis. A line
 * or bars with `integer` keep whole numbers on their axis; `max` fixes its top (100
 * for a percentage).
 */
(function () {
	'use strict';

	const NS = 'http://www.w3.org/2000/svg';
	const HEIGHT = 250;
	const MARGIN = { left: 50, right: 40, top: 14, bottom: 36 };
	const DIVISIONS = 4;

	function node(tag, attributes, text) {
		const element = document.createElementNS(NS, tag);
		Object.entries(attributes || {}).forEach(([key, value]) => element.setAttribute(key, value));
		if (text !== undefined) element.textContent = text;
		return element;
	}

	/** 1, 2, 5 or 10 times a power of ten that is not below `value`. */
	function niceMax(value) {
		if (value <= 0) return 1;
		const power = Math.pow(10, Math.floor(Math.log10(value)));
		const fraction = value / power;
		return (fraction <= 1 ? 1 : fraction <= 2 ? 2 : fraction <= 5 ? 5 : 10) * power;
	}

	/** 350000 -> "350k", 1200000 -> "1,2tr": the short money of an axis. */
	TG.shortMoney = function (value) {
		if (value >= 1e6) return `${String(Math.round(value / 1e5) / 10).replace('.', ',')}tr`;
		if (value >= 1e3) return `${Math.round(value / 1e3)}k`;
		return String(value);
	};

	TG.money = value => `${Math.round(value).toLocaleString('vi-VN')}đ`;

	TG.chart = function (container, spec) {
		container.replaceChildren();
		const count = spec.labels.length;
		const width = Math.max(container.clientWidth, 280);
		const plotW = width - MARGIN.left - MARGIN.right;
		const plotH = HEIGHT - MARGIN.top - MARGIN.bottom;
		const barsTop = Math.max(0, ...spec.bars.values);
		const barsMax = spec.bars.integer ? Math.max(DIVISIONS, Math.ceil(barsTop / DIVISIONS) * DIVISIONS) : niceMax(barsTop);
		let lineMax;
		if (spec.line.max) lineMax = spec.line.max;
		else if (spec.line.integer) lineMax = Math.max(DIVISIONS, Math.ceil(Math.max(0, ...spec.line.values) / DIVISIONS) * DIVISIONS);
		else lineMax = niceMax(Math.max(0, ...spec.line.values));
		const allZero = spec.bars.values.every(v => v === 0) && spec.line.values.every(v => v === 0);

		const svg = node('svg', { class: 'ch', viewBox: `0 0 ${width} ${HEIGHT}`, width, height: HEIGHT, role: 'img', 'aria-label': spec.label });
		const band = plotW / count;
		const x = i => MARGIN.left + band * (i + 0.5);
		const yBars = v => MARGIN.top + plotH * (1 - v / barsMax);
		const yLine = v => MARGIN.top + plotH * (1 - v / lineMax);

		for (let i = 0; i <= DIVISIONS; i++) {
			const y = MARGIN.top + plotH * (1 - i / DIVISIONS);
			svg.append(node('line', { class: 'ch-grid', x1: MARGIN.left, x2: width - MARGIN.right, y1: y, y2: y }));
			svg.append(node('text', { class: 'ch-tick', x: MARGIN.left - 8, y: y + 4, 'text-anchor': 'end' }, spec.bars.tick(barsMax * i / DIVISIONS)));
			svg.append(node('text', { class: 'ch-tick ch-tick-line', x: width - MARGIN.right + 8, y: y + 4, 'text-anchor': 'start' }, spec.line.tick(lineMax * i / DIVISIONS)));
		}

		// Label every n-th column, counted back from the last one so the latest day is always named
		// and two labels never touch.
		const step = Math.max(1, Math.ceil(count / Math.max(1, Math.floor(plotW / 46))));
		spec.labels.forEach((label, i) => {
			if ((count - 1 - i) % step !== 0) return;
			const text = node('text', { class: 'ch-label', x: x(i), y: HEIGHT - MARGIN.bottom + 16, 'text-anchor': 'middle' });
			label.split(' ').forEach((part, line) => text.append(node('tspan', { x: x(i), dy: line ? 13 : 0 }, part)));
			svg.append(text);
		});

		const barW = Math.min(36, band * 0.58);
		spec.labels.forEach((label, i) => {
			const group = node('g', { class: 'ch-col' });
			group.append(node('title', {}, `${(spec.titles || spec.labels)[i]}: ${spec.bars.name} ${spec.bars.format(spec.bars.values[i])}, ${spec.line.name} ${spec.line.format(spec.line.values[i])}`));
			group.append(node('rect', { class: 'ch-hit', x: x(i) - band / 2, y: MARGIN.top, width: band, height: plotH }));
			if (spec.bars.values[i] > 0) {
				const top = yBars(spec.bars.values[i]);
				group.append(node('rect', { class: 'ch-bar', x: x(i) - barW / 2, y: top, width: barW, height: MARGIN.top + plotH - top, rx: 3 }));
			}
			svg.append(group);
		});

		const points = spec.line.values.map((v, i) => `${x(i)},${yLine(v)}`).join(' ');
		svg.append(node('polyline', { class: 'ch-line', points }));
		// A long range has too many points for dots: the line alone reads better.
		if (count <= 45) {
			spec.line.values.forEach((v, i) => svg.append(node('circle', { class: 'ch-dot', cx: x(i), cy: yLine(v), r: 3.5 })));
		}
		if (allZero) {
			svg.append(node('text', { class: 'ch-empty', x: MARGIN.left + plotW / 2, y: MARGIN.top + plotH / 2, 'text-anchor': 'middle' }, spec.empty || 'Chưa có dữ liệu.'));
		}
		container.append(svg);

		const legend = document.createElement('p');
		legend.className = 'ch-legend';
		[['ch-key-bar', spec.bars.name], ['ch-key-line', spec.line.name]].forEach(([cls, name]) => {
			const item = document.createElement('span');
			const key = document.createElement('i');
			key.className = cls;
			key.setAttribute('aria-hidden', 'true');
			item.append(key, name);
			legend.append(item);
		});
		container.append(legend);

		// The same numbers as a table, for a screen reader. The class goes on a wrapper: on the
		// table itself its rows are not clipped and stretch the page.
		const hidden = document.createElement('div');
		hidden.className = 'visually-hidden';
		const table = document.createElement('table');
		hidden.append(table);
		const head = table.createTHead().insertRow();
		['Ngày', spec.bars.name, spec.line.name].forEach(title => { const th = document.createElement('th'); th.scope = 'col'; th.textContent = title; head.append(th); });
		const body = table.createTBody();
		spec.labels.forEach((label, i) => {
			const row = body.insertRow();
			[(spec.titles || spec.labels)[i], spec.bars.format(spec.bars.values[i]), spec.line.format(spec.line.values[i])].forEach(cell => { row.insertCell().textContent = cell; });
		});
		container.append(hidden);
	};
})();
