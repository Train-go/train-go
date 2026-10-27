/*
 * The real stations of the Thống Nhất line that the system may not have yet,
 * and the helpers that fill a new station from its name (#38).
 *
 *   TG.GAZETTEER                              13 stations: { name, km, city }
 *   TG.fold(text)                             lower case, no diacritics
 *   TG.placeName(name)                        the name without its leading "Ga "
 *   TG.suggestCodes(name, takenCodes)         up to 3 free codes, "Quảng Ngãi" -> QNG, QNI, QUN
 *   TG.knownPlaces(stations, query, range)    gazetteer entries still missing, those inside range first
 *
 * Km is the distance from Hà Nội as published for the North-South railway;
 * provinces are those after the 2025 merger.
 */
(function () {
	'use strict';

	TG.GAZETTEER = [
		['Phủ Lý', 56, 'Ninh Bình'], ['Nam Định', 87, 'Ninh Bình'], ['Ninh Bình', 115, 'Ninh Bình'],
		['Đông Hà', 622, 'Quảng Trị'], ['Kim Liên', 777, 'Đà Nẵng'], ['Tam Kỳ', 865, 'Đà Nẵng'],
		['Quảng Ngãi', 928, 'Quảng Ngãi'], ['Diêu Trì', 1096, 'Gia Lai'], ['Tuy Hòa', 1198, 'Đắk Lắk'],
		['Tháp Chàm', 1408, 'Khánh Hòa'], ['Bình Thuận', 1551, 'Lâm Đồng'], ['Biên Hòa', 1697, 'Đồng Nai'],
		['Dĩ An', 1706, 'TP. Hồ Chí Minh']
	].map(([name, km, city]) => ({ name, km, city }));

	TG.fold = function (text) {
		return String(text || '').replace(/[đĐ]/g, 'd').normalize('NFD').replace(/[̀-ͯ]/g, '')
			.toLowerCase().replace(/\s+/g, ' ').trim();
	};

	/** "Ga Quảng Ngãi" -> "Quảng Ngãi": stations are named with "Ga ", the line's places are not. */
	TG.placeName = function (name) {
		return String(name || '').replace(/^\s*ga\s+/i, '').trim();
	};

	/** Codes made from the name's letters, in the order they read best; codes in `taken` are skipped. */
	TG.suggestCodes = function (name, taken) {
		const words = TG.fold(TG.placeName(name)).toUpperCase().replace(/[^A-Z ]/g, ' ').split(' ').filter(Boolean);
		if (!words.length) return [];
		const first = words[0];
		const last = words[words.length - 1];
		const raw = words.length === 1
			? [first.slice(0, 3), first[0] + first.slice(-2), first.slice(0, 2) + first.slice(-1), first.slice(0, 4)]
			: [first[0] + last.slice(0, 2), first[0] + last[0] + last.slice(-1), first.slice(0, 2) + last[0],
				words.map(word => word[0]).join(''), first[0] + last.slice(0, 3)];
		const out = [];
		raw.forEach(code => {
			if (/^[A-Z]{2,5}$/.test(code) && !taken.has(code) && !out.includes(code)) out.push(code);
		});
		return out.slice(0, 3);
	};

	/**
	 * Gazetteer entries whose name matches `query` and that the system lacks,
	 * by name or by km. `range` is { lo, hi } of the gap being filled, or null.
	 */
	TG.knownPlaces = function (stations, query, range) {
		const have = new Set(stations.map(station => TG.fold(TG.placeName(station.name))));
		const usedKm = new Set(stations.map(station => station.kmMark));
		const q = TG.fold(TG.placeName(query));
		return TG.GAZETTEER
			.filter(place => !have.has(TG.fold(place.name)) && !usedKm.has(place.km))
			.filter(place => !q || TG.fold(place.name).includes(q))
			.map(place => ({ ...place, inRange: Boolean(range) && place.km > range.lo && place.km < range.hi }))
			.sort((x, y) => (Number(y.inRange) - Number(x.inRange)) || x.km - y.km);
	};
})();
