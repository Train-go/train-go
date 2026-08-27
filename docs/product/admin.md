# Khu vực quản trị

Nguồn gốc: `SPEC.md` mục 33–40. Admin chỉ quản lý dữ liệu cần cho việc đặt vé,
không quản lý vận hành đường sắt.

## Dashboard `/admin`

Bốn con số tổng quan: tổng booking, tổng chuyến, tổng người dùng, tổng doanh thu.
Không cần biểu đồ phức tạp.

## Quản lý ga `/admin/stations`

Xem danh sách, thêm, sửa, xóa. Chỉ xóa được ga chưa được dùng ở nơi khác.

Validation: `code` không được trùng, `name` không được để trống.

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
`A16`. Admin không nhập tay từng ghế.

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

## Điểm chưa chốt

- Nội dung seed data cụ thể: bao nhiêu ga, tuyến, tàu, chuyến mẫu và tài khoản
  admin nào. Cần chốt trước khi viết seed, vì đây cũng là dữ liệu dùng để demo.
- Quy tắc xóa khi dữ liệu đã được tham chiếu: chặn xóa, hay xóa mềm. `SPEC.md`
  mới chỉ nói ga "xóa nếu chưa được sử dụng"; các entity còn lại chưa có quy tắc.
