/*
 * One track of the admin overview's stacked chart, drawn as SVG so it needs no
 * library. Colours come from css/admin/pages/overview.css (.dc-*), and tracks
 * that share a width share their margins, so the same day sits at the same x
 * in every one of them.
 *
 *   TG.track(container, {
 *     kind: 'area' | 'bars' | 'lolli',
 *     label,                   what a screen reader hears for the whole track
 *     name,                    the figure's name in a tooltip
 *     labels?: ['29/10', ...], the axis under this track; only the last one has it
 *     titles: ['29/10/2026', ...],
 *     values: [...],
 *     height, format, tick, integer?, divisions?, empty
 *   })
 */
(function () {
	'use strict';

	const NICE = [1, 2, 2.5, 5, 10];

	function esc(text) {
		return String(text).replace(/[&<>"]/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;' }[c]));
	}

	function niceMax(value, divisions) {
		if (value <= 0) return divisions;
		const step = Math.pow(10, Math.floor(Math.log10(value / divisions)));
		return NICE.map(m => m * step).find(s => s * divisions >= value) * divisions;
	}

	function intMax(value, divisions) {
		return Math.max(divisions, Math.ceil(value / divisions) * divisions);
	}

	/** 350000 -> "350k", 1200000 -> "1,2tr": the short money of an axis. */
	TG.shortMoney = function (value) {
		if (value >= 1e6) return `${String(Math.round(value / 1e5) / 10).replace('.', ',')}tr`;
		if (value >= 1e3) return `${Math.round(value / 1e3)}k`;
		return String(Math.round(value));
	};

	TG.money = value => `${Math.round(value).toLocaleString('vi-VN')}đ`;

	TG.track = function (container, s) {
		const width = Math.max(300, Math.round(container.clientWidth));
		const m = { top: 16, right: 20, bottom: s.labels ? 30 : 6, left: 52 };
		const n = s.values.length;
		const pw = width - m.left - m.right;
		const ph = s.height - m.top - m.bottom;
		const div = s.divisions || 4;
		const top = Math.max(0, ...s.values);
		const max = s.integer ? intMax(top, div) : niceMax(top, div);
		const band = pw / n;
		const x = i => m.left + band * (i + 0.5);
		const y = v => m.top + ph * (1 - v / max);
		const parts = [];

		for (let i = 0; i <= div; i++) {
			const yy = m.top + ph * (1 - i / div);
			parts.push(`<line class="dc-grid" x1="${m.left}" x2="${width - m.right}" y1="${yy}" y2="${yy}"/>`,
				`<text class="dc-tick" x="${m.left - 8}" y="${yy + 4}" text-anchor="end">${s.tick(max * i / div)}</text>`);
		}

		if (s.kind === 'area') {
			const line = s.values.map((v, i) => `${x(i)},${y(v)}`).join(' ');
			parts.push(`<polygon class="dc-area" points="${x(0)},${y(0)} ${line} ${x(n - 1)},${y(0)}"/>`,
				`<polyline class="dc-line" points="${line}"/>`);
		}
		if (s.kind === 'bars') {
			const w = Math.min(20, band * 0.62);
			s.values.forEach((v, i) => {
				if (v > 0) parts.push(`<rect class="dc-bar" x="${x(i) - w / 2}" y="${y(v)}" width="${w}" height="${y(0) - y(v)}" rx="3"/>`);
			});
		}
		if (s.kind === 'lolli') {
			s.values.forEach((v, i) => {
				if (v > 0) parts.push(`<line class="dc-stem" x1="${x(i)}" x2="${x(i)}" y1="${y(0)}" y2="${y(v)}"/>`, `<circle class="dc-dot" cx="${x(i)}" cy="${y(v)}" r="4.5"/>`);
			});
		}

		// The latest value and the highest are named on the line itself. A label
		// above a point on the top edge would be cut off, so it goes below.
		if (s.kind === 'area' && top > 0) {
			const mark = (i, anchor) => parts.push(`<circle class="dc-end" cx="${x(i)}" cy="${y(s.values[i])}" r="4.5"/>`,
				`<text class="dc-note" x="${x(i)}" y="${y(s.values[i]) + (y(s.values[i]) < m.top + 14 ? 20 : -10)}" text-anchor="${anchor}">${s.format(s.values[i])}</text>`);
			const peak = s.values.indexOf(top);
			if (peak !== n - 1) mark(peak, peak < n / 8 ? 'start' : peak > n - n / 8 ? 'end' : 'middle');
			mark(n - 1, 'end');
		}

		// Every n-th label, counted back from the last so the latest day is always named.
		const step = Math.max(1, Math.ceil(n / Math.max(1, Math.floor(pw / 56))));
		s.values.forEach((v, i) => {
			if (s.labels && (n - 1 - i) % step === 0) parts.push(`<text class="dc-label" x="${x(i)}" y="${s.height - 8}" text-anchor="middle">${esc(s.labels[i])}</text>`);
			parts.push(`<g class="dc-col"><title>${esc(s.titles[i])}: ${esc(s.name)} ${s.format(v)}</title><rect class="dc-hit" x="${x(i) - band / 2}" y="${m.top}" width="${band}" height="${ph}"/></g>`);
		});
		if (s.values.every(v => !v)) parts.push(`<text class="dc-empty" x="${m.left + pw / 2}" y="${m.top + ph / 2}" text-anchor="middle">${esc(s.empty)}</text>`);

		const rows = s.values.map((v, i) => `<tr><td>${esc(s.titles[i])}</td><td>${s.format(v)}</td></tr>`).join('');
		// The same numbers as a table for a screen reader. The class goes on a wrapper:
		// on the table itself its rows are not clipped and stretch the page.
		container.innerHTML = `<svg class="dc-svg" viewBox="0 0 ${width} ${s.height}" width="${width}" height="${s.height}" role="img" aria-label="${esc(s.label)}">${parts.join('')}</svg>
			<div class="visually-hidden"><table><thead><tr><th scope="col">Thời gian</th><th scope="col">${esc(s.name)}</th></tr></thead><tbody>${rows}</tbody></table></div>`;
	};
})();
