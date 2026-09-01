/*
 * TrainGo admin shell behaviour.
 *
 * Ported from the third-party dashboard template under D:\IUH\Eclipse\demo,
 * keeping only the sidebar toggle, the responsive sidebar sync and the
 * back-to-top button. The template's theme toggle, palette picker and
 * menu-position code were dropped with the customizer
 * (docs/decisions/0004-khu-vuc-quan-tri.md).
 *
 * Note for anyone tempted to port that theme block back: upstream calls
 * setTheme(getPreferredTheme()) before it checks whether #theme-toggle-icon
 * exists, so without that button in the header it throws on null and takes
 * the sidebar down with it.
 */
(function() {
    "use strict";

    const select = (el, all = false) => {
        el = typeof el === 'string' ? el.trim() : el;
        if (all) {
            return Array.from(document.querySelectorAll(el));
        }
        return document.querySelector(el);
    };

    const on = (type, el, listener, all = false) => {
        const selectEl = select(el, all);
        if (!selectEl) {
            return;
        }
        if (all) {
            selectEl.forEach(e => e.addEventListener(type, listener));
        } else {
            selectEl.addEventListener(type, listener);
        }
    };

    const onscroll = (el, listener) => {
        el.addEventListener('scroll', listener);
    };

    if (select('.toggle-sidebar-btn')) {
        on('click', '.toggle-sidebar-btn', () => {
            const bodyEl = select('body');
            if (bodyEl) {
                bodyEl.classList.toggle('toggle-sidebar');
            }
        });
    }

    const responsiveSidebarMq = window.matchMedia('(max-width: 991px)');

    const syncSidebarWithViewport = (isSmallScreen) => {
        const bodyEl = select('body');
        if (!bodyEl || !bodyEl.classList.contains('app-body')) {
            return;
        }
        if (bodyEl.classList.contains('menu-top')) {
            bodyEl.classList.remove('toggle-sidebar');
            return;
        }
        if (isSmallScreen) {
            bodyEl.classList.add('toggle-sidebar');
        } else {
            bodyEl.classList.remove('toggle-sidebar');
        }
    };

    syncSidebarWithViewport(responsiveSidebarMq.matches);

    const handleSidebarBreakpoint = (event) => syncSidebarWithViewport(event.matches);

    if (typeof responsiveSidebarMq.addEventListener === 'function') {
        responsiveSidebarMq.addEventListener('change', handleSidebarBreakpoint);
    } else if (typeof responsiveSidebarMq.addListener === 'function') {
        responsiveSidebarMq.addListener(handleSidebarBreakpoint);
    }

    window.addEventListener('load', () => syncSidebarWithViewport(responsiveSidebarMq.matches));

    const backtotop = select('.back-to-top');
    if (backtotop) {
        const toggleBacktotop = () => {
            if (window.scrollY > 100) {
                backtotop.classList.add('active');
            } else {
                backtotop.classList.remove('active');
            }
        };
        window.addEventListener('load', toggleBacktotop);
        onscroll(document, toggleBacktotop);
    }
})();

/*
 * Turns every table marked .datatable into a searchable, sortable, paginated
 * one. Lives here rather than in each page so the Vietnamese labels are
 * written once. Guarded on both sides: pages without a .datatable and pages
 * that never loaded the library both just do nothing.
 */
document.addEventListener('DOMContentLoaded', function () {
    if (typeof simpleDatatables === 'undefined') {
        return;
    }
    document.querySelectorAll('.datatable').forEach(function (table) {
        new simpleDatatables.DataTable(table, {
            labels: {
                placeholder: 'Tìm kiếm...',
                searchTitle: 'Tìm trong bảng',
                // v9 renders the page-size select itself and puts this text
                // after it, so no {select} placeholder here.
                perPage: 'dòng mỗi trang',
                pageTitle: 'Trang {page}',
                noRows: 'Không có dữ liệu',
                noResults: 'Không tìm thấy kết quả phù hợp',
                info: 'Hiển thị {start}-{end} trong {rows} dòng'
            }
        });
    });
});
