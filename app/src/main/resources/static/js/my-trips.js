/*
 * The cancel dialog of a booking (templates/my-trips/detail.html). A button
 * marked data-cancel-open opens the <dialog> it names; the dialog's
 * data-cancel-close button and Esc close it, and the browser gives focus back
 * to the button that opened it. The form inside posts the cancel itself.
 */
(function () {
	document.querySelectorAll('[data-cancel-open]').forEach(function (button) {
		var dialog = document.getElementById(button.dataset.cancelOpen);
		if (!dialog) {
			return;
		}
		button.addEventListener('click', function () {
			dialog.showModal();
		});
		dialog.querySelectorAll('[data-cancel-close]').forEach(function (close) {
			close.addEventListener('click', function () {
				dialog.close();
			});
		});
		// A click on the backdrop lands on the dialog element itself.
		dialog.addEventListener('click', function (event) {
			if (event.target === dialog) {
				dialog.close();
			}
		});
		// Sending twice would only bounce off "Booking đã hủy.", but say it is on its way.
		dialog.querySelector('form').addEventListener('submit', function (event) {
			var submit = event.target.querySelector('[type="submit"]');
			submit.disabled = true;
			submit.textContent = 'Đang hủy…';
		});
	});
})();
