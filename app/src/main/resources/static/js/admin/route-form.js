/* Suggest a distance from station km marks until the user types their own. */
(function () {
	'use strict';

	const origin = document.getElementById('originStationId');
	const destination = document.getElementById('destinationStationId');
	const distance = document.getElementById('distanceKm');
	if (!origin || !destination || !distance) {
		return;
	}

	// Keep manual-entry state across validation errors, including zero or blank.
	const distanceEdited = distance.form.elements.namedItem('distanceEdited');
	distance.addEventListener('input', function () {
		distanceEdited.value = 'true';
	});

	function suggestDistance() {
		if (distanceEdited.value === 'true' || !origin.value || !destination.value) {
			return;
		}
		const originKm = Number(origin.selectedOptions[0].dataset.kmMark);
		const destinationKm = Number(destination.selectedOptions[0].dataset.kmMark);
		if (Number.isFinite(originKm) && Number.isFinite(destinationKm)) {
			distance.value = Math.abs(originKm - destinationKm);
		}
	}

	origin.addEventListener('change', suggestDistance);
	destination.addEventListener('change', suggestDistance);
})();
