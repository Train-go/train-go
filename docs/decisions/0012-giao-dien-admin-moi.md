# 0012 Giao diện admin tự dựng trên Bootstrap dùng chung

Date: 2026-10-23

## Status

Accepted. Thay phần "template dashboard bên ngoài" của
`0004-khu-vuc-quan-tri.md`. Các luật khác của ADR 0004 vẫn giữ: namespace ba
tầng, static admin dưới `static/css/admin/` và `static/js/admin/`, không nạp
`app.css` và `admin.css` trên cùng một trang, `activeNav` do controller đặt.

## Context

Nhóm chọn giao diện admin mới trong `docs/reports/ui-mau/admin/index.html`:
menu trái chia ba nhóm, bảng dày, ngăn kéo bên phải, hộp xác nhận, toast. Khung
cũ của ADR 0004 là template dashboard của tác giả khác. `admin.css` cũ dài gần
5.000 dòng, đòi font Nunito không có trong repo nên trình duyệt rơi về font hệ
thống, và kéo theo sáu thư viện trong `static/vendor/` mà chỉ trang Tổng quan
dùng tới hai.

Mẫu không dùng Bootstrap và tự đặt các class `.btn`, `.table`, `.toast`. Ba tên
này trùng class của Bootstrap. Nạp cả hai thì hai bộ rule đè nhau; riêng
`.toast:not(.show) { display: none }` của Bootstrap làm toast của mẫu không bao
giờ hiện.

Phi muốn cả hai khu giao diện cùng dùng Bootstrap (chốt ngày 22/10/2026, Q1 của
`docs/plans/active/khung-giao-dien-admin-va-client.md`).

## Decision

**Admin dùng đúng file Bootstrap 5.3.3 mà khu khách dùng**
(`static/vendor/bootstrap/`). Không chép CSS thành phần của mẫu. `admin.css` là
lớp theme: đặt biến `--bs-*` theo màu, chữ, bo góc của mẫu, cộng khung menu và
vài thành phần Bootstrap không có (`.icon-btn`, `.tag`, `.kv`, `.empty`,
`.code`). Trang admin viết markup Bootstrap thường:

| Mẫu | Bootstrap |
| --- | --- |
| `.btn`, `.btn.secondary`, `.btn.quiet` | `.btn-primary`, `.btn-outline-secondary`, `.btn-link` |
| `.btn.danger`, `.btn.danger-quiet` | `.btn-danger`, `.btn-outline-danger` |
| `.input`, `.select`, `.input-affix` | `.form-control`, `.form-select`, `.input-group` |
| `.field`, `.label`, `.hint`, `.error` | `.form-label`, `.form-text`, `.invalid-feedback` + `.is-invalid` |
| `.table`, `.table-wrap` | `.table`, `.table-responsive` |
| `.note` | `.alert` |
| `.sheet`, `.dialog`, `.toasts` | `.offcanvas-end`, `.modal`, `.toast-container` |
| `.sr-only`, `.skip` | `.visually-hidden`, `.visually-hidden-focusable` |

**Font tự phục vụ:** Manrope cho chữ, JetBrains Mono cho mã và số, bản
variable `woff2` hai subset `latin` và `vietnamese` trong `static/fonts/`, kèm
`OFL.txt`. Font đặt qua `--bs-body-font-family` và `--bs-font-monospace` để mọi
thành phần Bootstrap nhận theo.

**JavaScript thuần qua `window.TG`** trong `static/js/admin/admin.js`, bọc thành
phần Bootstrap thay vì tự viết lại: `TG.sheet` (Offcanvas, tự trả focus về nút
đã mở), `TG.confirm` (Modal dùng chung trong layout, trả `Promise<boolean>`),
`TG.toast`, `TG.toastAfterReload`, `TG.api` (JSON, header CSRF, ném lỗi
`{ message, fields }`), `TG.showFieldErrors`, `TG.clearFieldErrors`. Form có
`data-confirm` tự hỏi trước khi gửi.

**Xóa khỏi `static/vendor/`:** `apexcharts`, `simple-datatables`, `boxicons`,
`ckeditor`, `editorjs`, `quill`. Còn lại `bootstrap` và `bootstrap-icons`.

## Alternatives Considered

1. **Chép nguyên CSS của mẫu, bỏ Bootstrap khỏi admin.** Giống mẫu nhất và
   không trùng class. Bị loại vì Phi muốn toàn bộ giao diện dùng Bootstrap, và
   vì khi đó mọi thành phần overlay (khóa focus, `Esc`, nền mờ) phải tự viết lại
   trong `admin.js`.
2. **Giữ Bootstrap và chép CSS của mẫu đè lên.** Bị loại vì trùng `.btn`,
   `.table`, `.toast` như phần Context.
3. **Giữ template dashboard cũ, chỉ đổi màu.** Bị loại vì bố cục cũ (thanh trên,
   menu phẳng) không phải bố cục nhóm đã chọn, và vẫn kéo theo gần 5.000 dòng CSS
   cùng các thư viện không dùng.

## Consequences

Positive:

- Một bộ class Bootstrap cho cả hai khu; người viết trang admin không phải học
  thêm bộ thành phần thứ hai.
- `admin.css` còn khoảng 480 dòng thay vì gần 5.000; đổi màu cả khu admin là sửa token trong
  `:root`.
- Overlay có sẵn khóa focus, `Esc`, nền mờ của Bootstrap; `TG` chỉ thêm phần
  Bootstrap thiếu.

Tradeoffs:

- Một số chi tiết của mẫu chỉ gần đúng sau khi qua Bootstrap: animation mở ngăn
  kéo và hộp xác nhận là của Bootstrap, không phải của mẫu.
- Mất `simple-datatables`: bảng admin không còn ô tìm và phân trang tự động cho
  tới khi từng trang làm lại toolbar của mình.

## Follow-Up

- #37 "Trang Tổng quan" đưa mục Tổng quan trở lại menu; tới lúc đó `/admin`
  chuyển hướng sang `/admin/trains`.
- #22 "Đăng nhập" điền tên người đăng nhập vào `data-slot="user-name"` ở đáy
  menu.
