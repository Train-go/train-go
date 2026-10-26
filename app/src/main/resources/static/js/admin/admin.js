/*
 * TrainGo admin shell behaviour, shared by every admin page as window.TG.
 *
 * Bootstrap (bootstrap.bundle.min.js, loaded before this file) already does
 * the hard parts of overlays: trapping focus, closing on Esc and on the
 * backdrop, animating. TG only adds what Bootstrap leaves out, so a page never
 * has to repeat it:
 *
 *   TG.sheet.open(id, triggerEl) / TG.sheet.close(id)   right-hand offcanvas
 *   TG.confirm({ title, body, confirmLabel, danger })   Promise<boolean>
 *   TG.toast(message, { label, run, error })             at most two at once
 *   TG.toastAfterReload(message, action)                 survives location.reload()
 *   TG.api(url, { method, body })                        JSON + CSRF, throws { message, fields }
 *   TG.showFieldErrors(form, fields) / TG.clearFieldErrors(form)
 *
 * See docs/decisions/0012-giao-dien-admin-moi.md.
 */
(function () {
	'use strict';

	const TOAST_LIMIT = 2;
	const TOAST_DELAY_MS = 6000;
	const RELOAD_TOAST_KEY = 'tg.toastAfterReload';
	const GENERIC_ERROR = 'Có lỗi xảy ra, vui lòng thử lại.';

	/* ---------- sheet: Bootstrap offcanvas that gives focus back ---------- */

	/*
	 * Bootstrap only returns focus to the opener when the offcanvas was opened
	 * through data-bs-toggle. Pages open sheets from code (a table row, a cell),
	 * so the opener is remembered here and focused again once the sheet is gone.
	 */
	const sheetOpeners = new WeakMap();

	const sheet = {
		open(id, triggerEl) {
			const el = document.getElementById(id);
			if (!el) {
				return;
			}
			sheetOpeners.set(el, triggerEl || document.activeElement);
			bootstrap.Offcanvas.getOrCreateInstance(el).show();
		},
		close(id) {
			const el = document.getElementById(id);
			if (el) {
				bootstrap.Offcanvas.getOrCreateInstance(el).hide();
			}
		}
	};

	document.addEventListener('shown.bs.offcanvas', function (event) {
		const first = event.target.querySelector('input:not([type=hidden]):not([disabled]), select:not([disabled]), textarea:not([disabled])');
		if (first) {
			first.focus();
		}
	});

	document.addEventListener('hidden.bs.offcanvas', function (event) {
		const opener = sheetOpeners.get(event.target);
		sheetOpeners.delete(event.target);
		if (opener && document.contains(opener)) {
			opener.focus();
		}
	});

	/* ---------- confirm: one shared modal, rendered by layout/base.html ---------- */

	function confirm(options) {
		const opts = options || {};
		const el = document.getElementById('tgConfirm');
		const title = el.querySelector('.modal-title');
		const body = el.querySelector('.modal-body-text');
		const ok = el.querySelector('[data-tg-confirm]');
		const back = el.querySelector('[data-tg-back]');
		const opener = document.activeElement;

		title.textContent = opts.title || '';
		body.textContent = opts.body || '';
		body.hidden = !opts.body;
		ok.textContent = opts.confirmLabel || 'Đồng ý';
		ok.className = 'btn ' + (opts.danger ? 'btn-danger' : 'btn-primary');

		return new Promise(function (resolve) {
			let answer = false;
			const modal = bootstrap.Modal.getOrCreateInstance(el);
			const onOk = function () {
				answer = true;
				modal.hide();
			};
			// "Quay lại" gets focus first, as in the mockup: Enter pressed out of
			// habit must not delete anything.
			const onShown = function () {
				back.focus();
			};
			const onHidden = function () {
				ok.removeEventListener('click', onOk);
				el.removeEventListener('shown.bs.modal', onShown);
				el.removeEventListener('hidden.bs.modal', onHidden);
				if (opener && document.contains(opener)) {
					opener.focus();
				}
				resolve(answer);
			};
			ok.addEventListener('click', onOk);
			el.addEventListener('shown.bs.modal', onShown);
			el.addEventListener('hidden.bs.modal', onHidden);
			modal.show();
		});
	}

	/*
	 * A form marked data-confirm asks before it submits, so a page declares the
	 * question and writes no JavaScript:
	 *
	 *   <form method="post" data-confirm="Xóa Ga Huế?" data-confirm-body="..."
	 *         data-confirm-label="Xóa ga" data-confirm-danger>
	 *
	 * form.submit() does not fire another submit event, so the confirmed submit
	 * does not loop back here.
	 */
	document.addEventListener('submit', function (event) {
		const form = event.target;
		if (!form.dataset || form.dataset.confirm === undefined) {
			return;
		}
		event.preventDefault();
		confirm({
			title: form.dataset.confirm,
			body: form.dataset.confirmBody,
			confirmLabel: form.dataset.confirmLabel,
			danger: form.dataset.confirmDanger !== undefined
		}).then(function (yes) {
			if (yes) {
				form.submit();
			}
		});
	});

	/* ---------- toast ---------- */

	function toast(message, action) {
		const container = document.getElementById('tgToasts');
		const live = container.querySelectorAll('.toast:not(.hiding)');
		if (live.length >= TOAST_LIMIT) {
			const oldest = live[0];
			oldest.classList.add('hiding');
			bootstrap.Toast.getOrCreateInstance(oldest).hide();
		}

		const el = document.createElement('div');
		el.className = 'toast';
		el.setAttribute('role', 'status');
		const bodyEl = document.createElement('div');
		bodyEl.className = 'toast-body';
		const icon = document.createElement('i');
		icon.className = action && action.error ? 'bi bi-exclamation-circle-fill' : 'bi bi-check-circle-fill';
		icon.setAttribute('aria-hidden', 'true');
		const text = document.createElement('span');
		text.className = 'grow';
		text.textContent = message;
		bodyEl.append(icon, text);
		if (action && action.label && typeof action.run === 'function') {
			const button = document.createElement('button');
			button.type = 'button';
			button.textContent = action.label;
			button.addEventListener('click', function () {
				bootstrap.Toast.getOrCreateInstance(el).hide();
				action.run();
			});
			bodyEl.append(button);
		}
		const close = document.createElement('button');
		close.type = 'button';
		close.setAttribute('aria-label', 'Đóng thông báo');
		close.innerHTML = '<i class="bi bi-x-lg" aria-hidden="true"></i>';
		close.addEventListener('click', function () {
			bootstrap.Toast.getOrCreateInstance(el).hide();
		});
		bodyEl.append(close);
		el.append(bodyEl);

		el.addEventListener('hidden.bs.toast', function () {
			el.remove();
		});
		container.append(el);
		bootstrap.Toast.getOrCreateInstance(el, { delay: TOAST_DELAY_MS }).show();
	}

	/*
	 * A function cannot outlive a reload, so the action here is a request to
	 * replay: { label, url, method, body }. Pressing the button sends it with
	 * TG.api and reloads, e.g. "Hoàn tác" deleting the route just created.
	 *
	 * Storage can throw (private windows, blocked site data). A lost toast is
	 * acceptable; a page that stops working because of it is not.
	 */
	function toastAfterReload(message, action) {
		try {
			sessionStorage.setItem(RELOAD_TOAST_KEY, JSON.stringify({ message: message, action: action || null }));
		} catch (ignored) {
			// The toast is skipped; the reload still happens.
		}
	}

	function showPendingToast() {
		let pending = null;
		try {
			pending = JSON.parse(sessionStorage.getItem(RELOAD_TOAST_KEY));
			sessionStorage.removeItem(RELOAD_TOAST_KEY);
		} catch (ignored) {
			return;
		}
		if (!pending || !pending.message) {
			return;
		}
		const action = pending.action;
		if (!action || !action.label || !action.url) {
			toast(pending.message);
			return;
		}
		toast(pending.message, {
			label: action.label,
			run: function () {
				api(action.url, { method: action.method, body: action.body })
					.then(function () {
						location.reload();
					})
					.catch(function (error) {
						toast(error.message, { error: true });
					});
			}
		});
	}

	/* ---------- api ---------- */

	function csrfHeaders() {
		const token = document.querySelector('meta[name="_csrf"]');
		const header = document.querySelector('meta[name="_csrf_header"]');
		const headers = {};
		if (token && header) {
			headers[header.content] = token.content;
		}
		return headers;
	}

	/*
	 * Error bodies from TrainGo's own API are { message, fields }. Anything
	 * else (a 403 from a stale CSRF token, a 500 rendered by Spring Boot, a
	 * network failure) becomes the generic message, so callers handle one shape.
	 */
	async function api(url, options) {
		const opts = options || {};
		const headers = Object.assign({ Accept: 'application/json' }, csrfHeaders());
		const init = { method: opts.method || 'GET', headers: headers, credentials: 'same-origin' };
		if (opts.body !== undefined) {
			headers['Content-Type'] = 'application/json';
			init.body = JSON.stringify(opts.body);
		}

		let response;
		try {
			response = await fetch(url, init);
		} catch (networkError) {
			throw apiError(GENERIC_ERROR, {}, 0);
		}

		const isJson = (response.headers.get('Content-Type') || '').includes('application/json');
		const data = isJson && response.status !== 204 ? await response.json().catch(() => null) : null;
		if (response.ok) {
			return data;
		}
		// Only TrainGo's ApiError carries "fields". Spring Boot's own error JSON
		// also has a "message" (in English, and filled in when devtools is on),
		// so a body without "fields" is not ours and its text is not shown.
		const ours = data && typeof data.message === 'string' && data.message
				&& data.fields !== null && typeof data.fields === 'object';
		throw apiError(ours ? data.message : GENERIC_ERROR, ours ? data.fields : {}, response.status);
	}

	function apiError(message, fields, status) {
		const error = new Error(message);
		error.fields = fields;
		error.status = status;
		return error;
	}

	/* ---------- field errors ---------- */

	function clearFieldErrors(form) {
		form.querySelectorAll('.is-invalid').forEach(function (input) {
			input.classList.remove('is-invalid');
			input.removeAttribute('aria-invalid');
			const describedBy = (input.getAttribute('aria-describedby') || '')
					.split(' ').filter(id => id && !id.startsWith('tg-err-')).join(' ');
			if (describedBy) {
				input.setAttribute('aria-describedby', describedBy);
			} else {
				input.removeAttribute('aria-describedby');
			}
		});
		form.querySelectorAll('.invalid-feedback[data-tg-error]').forEach(el => el.remove());
	}

	function showFieldErrors(form, fields) {
		clearFieldErrors(form);
		let first = null;
		Object.keys(fields || {}).forEach(function (name) {
			const input = form.elements.namedItem(name);
			if (!input || !input.classList) {
				return;
			}
			const id = 'tg-err-' + name;
			const feedback = document.createElement('div');
			feedback.className = 'invalid-feedback';
			feedback.id = id;
			feedback.dataset.tgError = '';
			feedback.textContent = fields[name];
			// After an input-group, not inside it, so the message spans the group.
			const anchor = input.closest('.input-group') || input;
			anchor.insertAdjacentElement('afterend', feedback);
			input.classList.add('is-invalid');
			input.setAttribute('aria-invalid', 'true');
			input.setAttribute('aria-describedby', ((input.getAttribute('aria-describedby') || '') + ' ' + id).trim());
			first = first || input;
		});
		if (first) {
			first.focus();
		}
	}

	/* ---------- clock in the side menu ---------- */

	const weekdayFormat = new Intl.DateTimeFormat('vi-VN', { weekday: 'long' });
	const dateFormat = new Intl.DateTimeFormat('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric' });
	const timeFormat = new Intl.DateTimeFormat('vi-VN', { hour: '2-digit', minute: '2-digit', hour12: false });

	function renderClock(el) {
		const now = new Date();
		const weekday = weekdayFormat.format(now);
		el.querySelector('[data-clock-date]').textContent =
				weekday.charAt(0).toUpperCase() + weekday.slice(1) + ', ' + dateFormat.format(now);
		el.querySelector('[data-clock-time]').textContent = timeFormat.format(now);
	}

	/*
	 * Waits for the next whole minute before ticking every 60 seconds, so the
	 * time changes when the minute does instead of up to a minute late.
	 */
	function startClock() {
		const el = document.querySelector('[data-clock]');
		if (!el) {
			return;
		}
		renderClock(el);
		const now = new Date();
		const untilNextMinute = (60 - now.getSeconds()) * 1000 - now.getMilliseconds();
		setTimeout(function () {
			renderClock(el);
			setInterval(function () {
				renderClock(el);
			}, 60000);
		}, untilNextMinute);
	}

	/* ---------- start ---------- */

	window.TG = {
		sheet: sheet,
		confirm: confirm,
		toast: toast,
		toastAfterReload: toastAfterReload,
		api: api,
		showFieldErrors: showFieldErrors,
		clearFieldErrors: clearFieldErrors
	};

	startClock();
	showPendingToast();

	/*
	 * On a phone the menu is a sideways-scrolling bar; centre the current
	 * page's link instead of leaving it cut off at the edge. Not
	 * scrollIntoView(): Chrome moves the Tab starting point to the scrolled
	 * element, so the first Tab would skip the "Bỏ qua menu" link.
	 *
	 * Measured once the fonts have loaded: the fallback font is narrower, so
	 * a measure taken earlier stops short and leaves the last link, "Lịch sử",
	 * cut off by the width the real font adds.
	 */
	function centreCurrentLink() {
		const nav = document.querySelector('.admin-side nav');
		const current = nav && nav.querySelector('a[aria-current="page"]');
		if (current && nav.scrollWidth > nav.clientWidth) {
			const navBox = nav.getBoundingClientRect();
			const linkBox = current.getBoundingClientRect();
			if (linkBox.left < navBox.left || linkBox.right > navBox.right) {
				nav.scrollLeft += linkBox.left - navBox.left - (navBox.width - linkBox.width) / 2;
			}
		}
	}
	if (document.fonts) {
		document.fonts.ready.then(centreCurrentLink);
	} else {
		centreCurrentLink();
	}

	// A success flash from a redirect (Post/Redirect/Get) shows as a toast.
	const flash = document.querySelector('[data-flash-success]');
	if (flash) {
		toast(flash.textContent.trim());
		flash.remove();
	}
})();
