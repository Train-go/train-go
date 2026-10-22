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
trùng với `SGN`. Lý trình (`kmMark`, số km tính từ ga Hà Nội) bắt buộc, là số
nguyên từ 0 đến 5000, và không trùng với ga khác. Danh sách xếp theo lý trình, từ
Bắc vào Nam (`docs/decisions/0011-chuyen-tao-tu-tau-noi-chang.md`).

Đã làm xong ngày 01/10/2026, chạy trên database thật:
`controller/StationAdminController`.

## Quản lý tuyến `/admin/routes`

Xem, tạo, sửa, xóa. Điều kiện: ga đi khác ga đến. Không quản lý ga trung gian.
Danh sách xếp theo lý trình ga đi, rồi ga đến. Form gợi ý khoảng cách bằng hiệu lý
trình của hai ga; admin vẫn sửa được.

Tuyến đã có chuyến thì không xóa được, và khi sửa chỉ được đổi khoảng cách, không
đổi ga đi hay ga đến (D2, chốt 12/10/2026). Nếu cho đổi ga, vé đã bán "Sài Gòn -
Hà Nội" sẽ bỗng thành vé của một tuyến khác. Vi phạm thì màn hình báo lý do và giữ
nguyên tuyến.

## Quản lý tàu `/admin/trains` và `/admin/trains/:id`

Danh sách tàu (mã, tên), thêm, sửa, và trang chi tiết. Trang chi tiết có hai phần:

- **Lịch chạy:** các chuyến của tàu theo thời gian, nối chặng ga đến của chuyến
  trước với ga đi của chuyến sau. Nút thêm chuyến nằm ở đây (xem "Quản lý chuyến").
  Chỗ chuỗi bị đứt vì một chuyến đã hủy được đánh dấu.
- **Sơ đồ toa:** chỉ để xem, vẽ cấu hình toa cố định mà mọi tàu dùng chung
  (`docs/product/domain-model.md`). Admin không quản lý toa: không có route
  `/admin/coaches`, không có form thêm toa (`docs/decisions/0009-bo-bang-toa-va-ghe.md`).

Chốt ngày 01/10/2026: phiên bản đầu không có xóa tàu (`SPEC.md` mục 36 không
liệt kê). D2 ngày 12/10/2026 giữ nguyên luật không xóa tàu.

## Quản lý chuyến `/admin/trips`

Chuyến thuộc về tàu, nên admin thêm chuyến ở trang chi tiết tàu. `/admin/trips`
là danh sách mọi chuyến để tra cứu, lọc theo tàu và ngày, và để hủy chuyến.

Form thêm chuyến gồm: tuyến, ngày giờ khởi hành, ngày giờ đến, giá cơ bản. Tàu là
tàu của trang đang mở. Luật nối chặng (`docs/product/domain-model.md`, mục "Lịch
của tàu và luật nối chặng"):

- Tàu chưa có chuyến nào chưa hủy: chọn tuyến bất kỳ.
- Tàu đã có chuyến: ga đi bị khóa bằng ga đến của chuyến cuối, nên chỉ chọn được
  tuyến xuất phát từ ga đó. Giờ đi sớm nhất là giờ đến của chuyến cuối cộng 30
  phút, và form điền sẵn giờ này.
- Giờ đến phải sau giờ đi.
- Service kiểm tra lại mọi điều trên; vi phạm thì form báo lỗi ngay cạnh ô sai.

Khi chuyến được tạo, hệ thống sinh 96 TripSeat từ cấu hình toa cố định, tất cả ở
trạng thái `AVAILABLE`.

Chuyến không xóa được, chỉ hủy được (`CANCELLED`). Chỉ hủy được chuyến
`SCHEDULED`, chưa khởi hành, và chưa có booking `CONFIRMED` nào (D2, chốt
12/10/2026): hủy chuyến khách đã trả tiền thì phải hoàn tiền, mà hoàn tiền nằm
ngoài phạm vi.

## Quản lý booking `/admin/bookings` và `/admin/bookings/:id`

Xem danh sách, tìm theo mã booking, xem chi tiết gồm khách hàng, hành khách,
chuyến, chỗ, trạng thái thanh toán. Admin hủy được booking.

Admin **không** đổi chỗ sau khi đã đặt, không sửa trạng thái thanh toán thủ công,
không hoàn tiền, không đổi hành khách.

## Danh sách người dùng `/admin/users`

Chỉ xem: tên, email, role, ngày tạo. Không có CRUD người dùng đầy đủ.

Tài khoản Admin được tạo bằng seed data, không đăng ký qua giao diện.

## Seed data

Người dùng nằm trong `app/src/main/resources/db/migration/V2__seed_demo_data.sql` và
`V3__seed_more_demo_data.sql` (1 admin, 11 khách). Từ ngày 20/10/2026, ga, tuyến,
tàu, chuyến và booking nằm trong `V5__north_south_line.sql` (V5 xóa dữ liệu cũ của
V2, V3): 8 ga trên trục Bắc - Nam, 16 tuyến, 3 tàu `SE1`, `SE2`, `SE3`, 18 chuyến
của `SE1` chạy nối chặng, 8 booking. Các màn hình admin còn lại và trang chủ đọc
thẳng từ các bảng này, không còn danh sách viết cứng trong service. Tài khoản và
cách làm mới dữ liệu: `docs/RUNBOOK.md`.

## Xóa dữ liệu đang được dùng

Chốt ngày 12/10/2026 (D2): chặn xóa, không có xóa mềm. Ga và tuyến đang được dùng
thì không xóa được; tàu không có chức năng xóa; chuyến chỉ hủy, không xóa.
Database không có `ON DELETE` nào, nên lệnh xóa hàng đang bị tham chiếu vẫn bị
database chặn; service phải kiểm tra trước để báo lý do dễ hiểu thay vì để lộ lỗi
khóa ngoại.
