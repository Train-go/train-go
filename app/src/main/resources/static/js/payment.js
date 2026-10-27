/*
 * Payment page (templates/booking/payment.html, issue #33): locks the button
 * the moment the form is sent, so a second press cannot send it again. The
 * server also pays one booking per draft, so this is only the first guard.
 */
(function () {
	'use strict';

	var form = document.getElementById('tgPayForm');
	var button = document.getElementById('tgPayButton');
	if (!form || !button) {
		return;
	}
	form.addEventListener('submit', function (event) {
		if (button.disabled) {
			event.preventDefault();
			return;
		}
		// Disabled after the submit starts: a button disabled during its own click would not send the form.
		setTimeout(function () {
			button.disabled = true;
			button.classList.add('is-busy');
		}, 0);
	});
}());
