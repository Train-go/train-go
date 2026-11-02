/*
 * /admin/users (templates/admin/users.html): the search box. The list is
 * rendered by the server. Loaded with defer, after admin.js.
 */
(function () {
	'use strict';

	const form = document.querySelector('[data-user-search]');
	TG.searchBox(form, form.elements.namedItem('q'));
})();
