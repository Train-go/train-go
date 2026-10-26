/*
 * The history page (templates/admin/history.html): restoring a deleted train
 * through TG.api, then opening its page, where TG.toastAfterReload confirms.
 */
(function () {
	'use strict';

	const section = document.querySelector('[data-api-url]');
	if (!section) {
		return;
	}
	const apiUrl = section.dataset.apiUrl;
	const pageUrl = section.dataset.pageUrl;

	section.querySelectorAll('[data-train-restore]').forEach(function (button) {
		button.addEventListener('click', async function () {
			button.disabled = true;
			try {
				const restored = await TG.api(apiUrl + '/' + button.dataset.id + '/restore', { method: 'POST' });
				TG.toastAfterReload('Đã khôi phục tàu ' + button.dataset.code + '.');
				location.assign(pageUrl + '/' + restored.id);
			} catch (error) {
				TG.toast(error.message, { error: true });
				button.disabled = false;
			}
		});
	});
})();
