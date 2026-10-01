# Khu vực quản trị

Nguồn gốc: `SPEC.md` mục 33–40. Admin chỉ quản lý dữ liệu cần cho việc đặt vé,
không quản lý vận hành đường sắt.

## Dashboard `/admin`

Bốn con số tổng quan: tổng booking, tổng chuyến, tổng người dùng, tổng doanh thu.
Không cần biểu đồ phức tạp.

## Quản lý ga `/admin/stations`

Xem danh sách, thêm, sửa, xóa. Chỉ xóa được ga chưa là điểm đi hay điểm đến của
tuyến nào; xóa ga đang được dùng thì màn hình báo lý do và giữ nguyên ga.

Validation: `code` không được trùng, `name` không được để trống. `code` chỉ gồm
chữ cái không dấu và chữ số, tối đa 10 ký tự, và được tự viết hoa, nên `sgn`
trùng với `SGN`.

Đã làm xong ngày 01/10/2026, chạy trên database thật:
`station/controller/StationAdminController`.

## Quản lý tuyến `/admin/routes`

Xem, tạo, sửa, xóa. Điều kiện: ga đi khác ga đến. Không quản lý ga trung gian.

## Quản lý tàu `/admin/trains` và `/admin/trains/:id`

Danh sách tàu, thêm, sửa, và trang chi tiết. Toa được quản lý **bên trong** trang
chi tiết tàu, không có route `/admin/coaches` riêng.

Khi thêm toa, Admin nhập:

- loại toa: `SEAT` hoặc `SLEEPER`
- `capacity`
- `priceModifier`

Hệ thống tự sinh các ghế tương ứng. Ví dụ toa `SEAT` capacity 16 sẽ có `A01` đến
`A16`. Admin không nhập tay từng ghế. Số toa tự tăng theo tàu: toa đầu tiên là
`01`. Quy tắc sinh mã chỗ và giới hạn `capacity` nằm ở
`docs/product/domain-model.md`.

Chốt ngày 01/10/2026: phiên bản đầu không có xóa tàu (`SPEC.md` mục 36 không
liệt kê), và toa chỉ thêm được, chưa sửa hay xóa.

## Quản lý chuyến `/admin/trips`

Admin tạo chuyến từ: tàu, tuyến, ngày khởi hành, giờ khởi hành, giờ đến, giá cơ
bản.

Khi chuyến được tạo, hệ thống sinh TripSeat cho toàn bộ ghế của tàu, tất cả ở
trạng thái `AVAILABLE`.

## Quản lý booking `/admin/bookings` và `/admin/bookings/:id`

Xem danh sách, tìm theo mã booking, xem chi tiết gồm khách hàng, hành khách,
chuyến, chỗ, trạng thái thanh toán. Admin hủy được booking.

Admin **không** đổi chỗ sau khi đã đặt, không sửa trạng thái thanh toán thủ công,
không hoàn tiền, không đổi hành khách.

## Danh sách người dùng `/admin/users`

Chỉ xem: tên, email, role, ngày tạo. Không có CRUD người dùng đầy đủ.

Tài khoản Admin được tạo bằng seed data, không đăng ký qua giao diện.

## Seed data

Đã chốt ngày 01/10/2026, nằm trong
`app/src/main/resources/db/migration/V2__seed_demo_data.sql`: 8 ga, 8 tuyến
(bốn cặp hai chiều), 8 tàu mỗi tàu 2 toa ghế 32 chỗ và 2 toa giường 16 chỗ,
chuyến hằng ngày trong 14 ngày tới, 1 admin, 3 khách và 4 booking mẫu. Tài
khoản và cách làm mới dữ liệu: `docs/RUNBOOK.md`.

## Điểm chưa chốt

- Quy tắc xóa khi dữ liệu đã được tham chiếu, với các entity ngoài ga: chặn
  xóa, hay xóa mềm. Ga đã chốt là chặn xóa theo `SPEC.md`. Database không có
  `ON DELETE` nào, nên hiện tại mọi lệnh xóa hàng đang bị tham chiếu đều bị
  chặn; service phải kiểm tra trước để báo lý do.
