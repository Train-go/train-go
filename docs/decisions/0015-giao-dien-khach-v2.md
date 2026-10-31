# 0015 Giao diện khách v2: kính trên ảnh, CSS theo từng trang

Date: 2026-10-30

## Status

Accepted. Thay phần màu, font và thành phần giao diện khách của `0003`. Luật dòng chữ tiếng Việt và
việc dùng chung Bootstrap 5.3.3 vẫn giữ. Không nạp `app.css` và `admin.css` trên cùng một trang.

## Context

Phi chốt giao diện trong prototype `traingo-redesign-v2` và yêu cầu đưa vào app, giữ Bootstrap, chia
CSS theo từng trang (30/10/2026).

## Decision

- `css/app.css` giữ token, header, ô nhập, thẻ, vé điện tử, thanh bước, footer và lớp tương thích với
  Bootstrap. Mỗi trang có `css/pages/<trang>.css`, nạp từ `<head>`; layout nhận `page(title, styles, content)`.
- Giữ tên class của mẫu (`.btn-cta`, `.trip`, `.ticket`, `.seat`...) để đối chiếu được, đè lên Bootstrap.
  Icon dùng Bootstrap Icons có sẵn thay cho sprite của mẫu. Font tự phục vụ, không CDN.
- Khối hành trình nổi bật đọc từ chuyến sắp chạy trong 14 ngày; chip khu vực và "Xem thêm" là link.
  Khu vực của ga nằm trong `HomeService` cho tới khi có cột trong bảng ga.
- Giữ tính năng app mà mẫu không có: sắp xếp và lọc chuyến, đặt thành một hàng phía trên danh sách. Chọn
  ghế hay giường chuyển hết sang trang chọn chỗ, thẻ chuyến không còn mục mở rộng.

## Consequences

- Trang khách mới phải có file CSS riêng và thẻ `<link>` trong `<head>`.
- Ảnh lấy nguyên từ prototype, footer không ghi nguồn ảnh vì app chỉ chạy ở localhost (Phi, 31/10/2026).
  Trước khi công khai phải ghi lại nguồn: 8 ảnh tuyến có link gốc trong `images/tuyen/NGUON-ANH.md` của
  prototype, còn `hero-sa-pa.jpg` và `footer-duong-ray.jpg` chưa rõ nguồn nên cần thay hoặc tra lại.
