(function () {
	'use strict';
	const SVG_NS = 'http://www.w3.org/2000/svg';
	const LX = 104;
	const name = station => station.name.replace(/^Ga\s+/i, '');
	const pairKey = (a, b) => [a, b].sort((x, y) => x - y).join('|');
	function svg(tag, attributes, text) {
		const node = document.createElementNS(SVG_NS, tag);
		Object.entries(attributes || {}).forEach(([key, value]) => node.setAttribute(key, value));
		if (text !== undefined) node.textContent = text;
		return node;
	}
	function button(className, label, action, selected) {
		const node = document.createElement('button');
		node.type = 'button';
		node.className = className;
		node.setAttribute('aria-label', label);
		node.setAttribute('aria-pressed', String(selected));
		node.title = label;
		node.addEventListener('click', action);
		return node;
	}

	TG.lineMap = function (container, network, options = {}) {
		const list = [...network.stations].sort((a, b) => a.kmMark - b.kmMark);
		const selected = options.selected || {};
		const isPair = (type, a, b) => selected.type === type
			&& pairKey(selected.a, selected.b) === pairKey(a.id, b.id);
		const pairs = new Map();
		network.routes.forEach(route => {
			const key = pairKey(route.originId, route.destinationId);
			if (!pairs.has(key)) pairs.set(key, []);
			pairs.get(key).push(route);
		});
		const total = list.at(-1)?.kmMark || 1;
		const ys = [];
		list.forEach((station, i) => {
			ys[i] = i ? Math.max(ys[i - 1] + 60, 28 + station.kmMark / total * 640) : 28;
		});
		const height = (ys.at(-1) || 28) + 36;
		const line = document.createElement('div');
		line.className = 'vline';
		line.style.height = height + 'px';
		const drawing = svg('svg', { class: 'vline-svg', width: '100%', height });
		line.append(drawing);

		function drawPair(a, b, i, j, arc) {
			const routes = pairs.get(pairKey(a.id, b.id)) || [];
			const status = routes.length >= 2 ? 'both' : routes.length ? 'one' : 'none';
			const descriptions = { both: 'cả hai chiều', one: 'một chiều', none: 'chưa có tuyến' };
			const distance = arc ? routes[0].distanceKm : b.kmMark - a.kmMark;
			const label = `${name(a)} ⇄ ${name(b)}, ${arc ? 'tuyến dài ' : ''}${distance} km, ${descriptions[status]}`;
			const active = isPair('pair', a, b);
			const group = svg('g', { class: `${arc ? 'arc' : 'seg'} ${status}${active ? ' sel' : ''}`,
				role: 'button', tabindex: 0, 'aria-label': label, 'aria-pressed': active });
			group.append(svg('title', {}, label));
			const dx = Math.min(LX - 14, 26 + (ys[j] - ys[i]) * 0.1);
			const shape = arc ? { d: `M ${LX - 10} ${ys[i]} C ${LX - dx} ${ys[i]}, ${LX - dx} ${ys[j]}, ${LX - 10} ${ys[j]}` }
				: { x1: LX, y1: ys[i] + 12, x2: LX, y2: ys[j] - 12 };
			['hit', 'vis'].forEach(cls => group.append(svg(arc ? 'path' : 'line', { ...shape, class: cls })));
			group.addEventListener('click', () => options.onPair?.(a.id, b.id));
			group.addEventListener('keydown', event => {
				if (event.key === 'Enter' || event.key === ' ') {
					event.preventDefault();
					options.onPair?.(a.id, b.id);
				}
			});
			drawing.append(group);
		}
		const positions = new Map(list.map((station, i) => [station.id, i]));
		pairs.forEach(routes => {
			const indices = [positions.get(routes[0].originId), positions.get(routes[0].destinationId)].sort((a, b) => a - b);
			const [i, j] = indices;
			if (i !== undefined && j !== undefined && j - i > 1) drawPair(list[i], list[j], i, j, true);
		});
		list.slice(1).forEach((station, i) => drawPair(list[i], station, i, i + 1, false));
		list.forEach((station, i) => {
			const active = selected.type === 'station' && selected.id === station.id;
			const stop = document.createElement('div');
			stop.className = 'vstop' + (active ? ' sel' : '');
			stop.style.top = ys[i] - 22 + 'px';
			const node = button('vstop-btn', `${name(station)}, km ${station.kmMark}`,
				() => options.onStation?.(station.id), active);
			node.innerHTML = '<span class="pin" aria-hidden="true"></span><span class="vname"><b></b><small></small></span>';
			node.querySelector('b').textContent = name(station);
			node.querySelector('small').textContent = 'km ' + station.kmMark;
			stop.append(node);
			line.append(stop);
		});
		list.slice(1).forEach((station, i) => {
			const active = isPair('gap', list[i], station);
			const gap = button('vgap' + (active ? ' sel' : ''), `Chèn ga giữa ${name(list[i])} và ${name(station)}`,
				() => options.onGap?.(list[i].id, station.id), active);
			gap.style.top = (ys[i] + ys[i + 1]) / 2 - 12 + 'px';
			gap.textContent = '+';
			line.append(gap);
		});
		if (!list.length) {
			line.textContent = 'Chưa có ga trên sơ đồ.';
			line.style.height = 'auto';
		}
		container.replaceChildren(line);
	};
})();
