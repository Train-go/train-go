# 0014 Giao diện admin v2: xanh, menu trắng, CSS theo từng trang

Date: 2026-10-30

## Status

Accepted. Thay phần màu, font và cách tổ chức CSS của `0012-giao-dien-admin-moi.md`. Các phần khác
của ADR 0012 vẫn giữ: Bootstrap 5.3.3 dùng chung, `--bs-*` là lớp theme, overlay do Bootstrap và
`window.TG` lo, không nạp `app.css` và `admin.css` trên cùng một trang, không dùng CDN.

## Context

Phi chốt giao diện mới trong prototype `traingo-redesign-v2` (30/10/2026) và yêu cầu đưa vào app:
giữ Bootstrap, phần nào cần thì tự viết, và chia CSS theo từng trang.

## Decision

- Token: xanh `#2563EB`, nền slate, Plus Jakarta Sans tự phục vụ ở
  `static/fonts/plus-jakarta-sans/`, bo góc 8/12/16px, menu trắng. Khu admin không dùng màu cam;
  mốc "bây giờ" dùng màu `warning`.
- `css/admin/admin.css` giữ theme, khung trang và phần nhiều trang cùng dùng (thẻ, thanh lọc, bảng,
  ngăn chi tiết chuyến, overlay). Mỗi trang có file riêng `css/admin/pages/<trang>.css`, nạp từ
  `<head>` của trang; layout nhận tham số `page(title, styles, content)`.
- Thành phần riêng của trang giữ tiền tố của prototype (`dc-`, `ct-`, `cv-`, `cc-`, `tb-`, `tr-`,
  `bm-`, `us-`, `route-`) để đối chiếu được với mẫu. Không chép `.btn`, `.table`, `.toast` của mẫu.
- Server dựng danh sách và lịch bằng Thymeleaf; vị trí thanh trong lịch theo giờ do Java tính và
  truyền qua biến CSS. JavaScript của mẫu dùng dữ liệu giả nên không chép.
- Trang Tuyến gộp vào Ga và tuyến; `/admin/routes` chuyển hướng, chỉ còn hai form thêm và sửa.
  Trình hướng dẫn thêm chuyến vẫn giữ, đổi theo theme mới.
- Biểu thức trong tham số fragment của Thymeleaf không được gọi lớp tĩnh `T(...)`, nên phép tính
  của giao diện nằm trong view model.

## Consequences

- Một trang chỉ tải CSS của nó, nhưng sửa phần dùng chung vẫn phải đi qua mọi trang admin.
- `line-map.js` và sơ đồ SVG cũ bị bỏ; chèn ga và cặp ga mở từ đường tàu và bảng tuyến.
- Thêm trang admin mới phải có file CSS riêng và thẻ `<link>` trong `<head>`.
