/*
 * Show/hide button for the password inputs of the login and register pages.
 * The buttons start hidden (d-none) so that without JavaScript the form still
 * submits and the password stays masked.
 */
(function () {
	document.querySelectorAll('[data-password-toggle]').forEach(function (button) {
		var input = document.getElementById(button.dataset.passwordToggle);
		if (!input) {
			return;
		}
		var icon = button.querySelector('i');
		button.classList.remove('d-none');
		button.addEventListener('click', function () {
			var show = input.type === 'password';
			input.type = show ? 'text' : 'password';
			button.setAttribute('aria-pressed', String(show));
			if (icon) {
				icon.classList.toggle('bi-eye', !show);
				icon.classList.toggle('bi-eye-slash', show);
			}
		});
	});
})();
