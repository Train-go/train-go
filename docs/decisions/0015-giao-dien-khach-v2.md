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
- Footer không ghi nguồn ảnh vì app chỉ chạy ở localhost (Phi, 31/10/2026). Ngày 02/11/2026 ảnh prototype
  bị vỡ trên màn hình mật độ cao nên đã thay bằng ảnh đủ điểm ảnh: ảnh đầu trang, đăng nhập, đăng ký và 8
  ảnh tuyến lấy từ Pexels (`pexels.com/photo/<id>`), riêng Hòn Nội giữ ảnh cũ ở bản gốc 6000px trên
  Wikimedia Commons. Trước khi công khai phải ghi lại nguồn từng ảnh: đầu trang 6136132 (đèo Hải Vân),
  đăng nhập 2162471, đăng ký 36605975, tuyến HNI 30262557, THA 34902075, VIN 5041981, DHO 26762053, HUE
  34861581, DNG 20346283, SGN 32755075, và bốn ga thêm qua admin PLY 38093102, NDI 33758103, DHA
  31465683, NTH 5323717 (Pexels); NTR "Hòn Nội, Nha Trang Bay, Vietnam.jpg" (Commons,
  public domain). Ảnh chân trang `footer-duong-ray.jpg` giữ ảnh prototype theo ý Phi, vẫn chưa rõ nguồn.
- Ảnh ô hành trình có hai bản, gốc 2800px và `-800`, chọn bằng `srcset` (`StationPhoto`); ảnh đầu trang có
  bản cắt dọc `hero-hai-van-doc.jpg` cho màn hình rộng tới 640px. Ảnh mới phải đủ để hệ số phóng không quá
  1 ở 1440px với DPR 2 và 390px với DPR 3.
- Ga chưa có ảnh riêng trong `HomeService` (ví dụ ga mới thêm qua admin) dùng `StationPhoto.DEFAULT`, tức ảnh
  đầu trang `hero-hai-van.jpg`, nên ô hành trình và ảnh nhỏ ở chân trang không bao giờ để trống.
