/*
 * Passenger page conveniences (templates/booking/passengers.html, issue #32).
 *
 * The server checks every name and CCCD; this only opens the name field of a
 * ticket, restores the account name, keeps the avatar, the name line and the
 * account phone in step with what is typed, and limits the CCCD to digits.
 */
(function () {
	'use strict';

	var form = document.getElementById('tgPassForm');
	if (!form) {
		return;
	}
	var accountName = form.dataset.accountName || '';

	function initials(name) {
		var words = name.trim().split(/\s+/).filter(Boolean);
		if (!words.length) {
			return '?';
		}
		var last = words[words.length - 1].charAt(0);
		return (words.length > 1 ? words[words.length - 2].charAt(0) + last : last).toUpperCase();
	}

	/** Shows the name typed on the ticket; the account phone belongs to the account holder only. */
	function sync(ticket) {
		var name = ticket.querySelector('[data-name]').value;
		var mine = name.trim() === accountName;
		ticket.querySelector('[data-person]').textContent = name.trim() || 'Chưa nhập tên';
		var avatar = ticket.querySelector('[data-avatar]');
		avatar.textContent = initials(name);
		var phone = ticket.querySelector('[data-phone]');
		if (phone) {
			phone.hidden = !mine;
		}
	}

	form.querySelectorAll('[data-ticket]').forEach(function (ticket) {
		var edit = ticket.querySelector('[data-edit]');
		var name = ticket.querySelector('[data-name]');
		var identity = ticket.querySelector('[data-identity]');

		function open(shown) {
			ticket.classList.toggle('is-editing', shown);
			if (shown) {
				name.focus();
			}
		}

		edit.addEventListener('click', function () {
			open(true);
		});
		ticket.querySelector('[data-mine]').addEventListener('click', function () {
			name.value = accountName;
			sync(ticket);
			open(false);
			edit.focus();
		});
		name.addEventListener('input', function () {
			sync(ticket);
		});
		identity.addEventListener('input', function () {
			var digits = identity.value.replace(/\D/g, '').slice(0, 12);
			if (digits !== identity.value) {
				identity.value = digits;
			}
		});
	});

	// A ticket with an error keeps its own message on screen: bring the first one into view.
	var firstBad = form.querySelector('.control.is-invalid');
	if (firstBad) {
		firstBad.scrollIntoView({ block: 'center' });
		var field = firstBad.querySelector('input');
		if (field) {
			field.focus({ preventScroll: true });
		}
	}
}());
