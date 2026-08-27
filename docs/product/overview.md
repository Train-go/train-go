# TrainGo — Tổng quan sản phẩm

Nguồn gốc: `SPEC.md` tại gốc repository. Tài liệu trong `docs/product/` là bản
chắt lọc để làm việc hằng ngày. Khi hai bên mâu thuẫn, `SPEC.md` là bản gốc và
tài liệu ở đây phải được sửa lại cho khớp.

## Bản đồ tài liệu sản phẩm

- `overview.md` — tài liệu này: phạm vi, người dùng, luồng chính, màn hình.
- `domain-model.md` — các entity, quan hệ, trạng thái và ràng buộc dữ liệu.
- `booking-flow.md` — luồng đặt vé, giữ chỗ, thanh toán mô phỏng, hủy vé.
- `admin.md` — chức năng quản trị và quy tắc dữ liệu vận hành.
- `auth.md` — đăng ký, đăng nhập, phân quyền và quy tắc bảo mật.

Vận hành ứng dụng ở máy local: `docs/RUNBOOK.md`.
Quyết định kỹ thuật đã chốt: `docs/decisions/`.
Kế hoạch đang chạy: `docs/plans/active/`.

## Sản phẩm là gì

TrainGo là website đặt vé tàu, mô phỏng quy trình đặt vé thật ở phạm vi vừa đủ:
tìm chuyến, chọn toa, chọn ghế hoặc giường trên sơ đồ trực quan, nhập thông tin
hành khách, thanh toán mô phỏng, nhận vé điện tử.

Hệ thống không mô phỏng toàn bộ hoạt động ngành đường sắt. Mọi thứ không phục vụ
trực tiếp cho việc đặt vé đều nằm ngoài phạm vi.

## Hai nhóm người dùng

**CUSTOMER** — người đặt vé. Đăng ký, đăng nhập, tìm chuyến, chọn chỗ, nhập hành
khách, thanh toán mô phỏng, xem vé, xem và hủy booking của chính mình.

**ADMIN** — người quản lý dữ liệu vận hành. Dashboard tổng quan, quản lý ga,
tuyến, tàu, toa, chuyến, booking, và xem danh sách tài khoản.

Không có role STAFF. Không có luồng kiểm vé hay check-in hành khách.

## Luồng chính của Customer

```
Landing Page
  -> Tìm chuyến
  -> Danh sách chuyến
  -> Chọn chuyến
  -> Chọn toa
  -> Chọn ghế / giường
  -> Nhập thông tin hành khách
  -> Xem lại booking
  -> Thanh toán mô phỏng
  -> Booking thành công
  -> Vé điện tử
```

Stepper hiển thị trên giao diện: `Trip → Seat → Passenger → Review → Payment → Ticket`.

Đây là luồng quan trọng nhất của toàn dự án. Mọi tính năng khác chỉ được làm sau
khi luồng này chạy thông từ đầu đến cuối.

## Điều kiện tìm chuyến

- Ga đi và ga đến đều bắt buộc.
- Ga đi khác ga đến.
- Ngày đi không nhỏ hơn ngày hiện tại.
- Số hành khách lớn hơn 0.

Phiên bản đầu không có filter theo giá, loại toa, giờ, thời gian di chuyển, và
không có sorting nâng cao.

## Danh sách màn hình

Public:

| Đường dẫn | Nội dung |
| --- | --- |
| `/` | Landing: hero + form tìm chuyến, popular routes, how it works |
| `/login` | Đăng nhập |
| `/register` | Đăng ký |

Booking:

| Đường dẫn | Nội dung |
| --- | --- |
| `/trips` | Danh sách chuyến phù hợp |
| `/trips/:id` | Chi tiết chuyến |
| `/booking/:tripId/seats` | Chọn toa và chọn ghế / giường |
| `/booking/passengers` | Nhập thông tin hành khách |
| `/booking/review` | Xem lại booking trước khi trả tiền |
| `/booking/payment` | Thanh toán mô phỏng |
| `/booking/success` | Xác nhận đặt vé thành công |

Customer:

| Đường dẫn | Nội dung |
| --- | --- |
| `/my-trips` | Danh sách booking, chia Upcoming / Completed / Cancelled |
| `/my-trips/:bookingId` | Chi tiết booking |
| `/tickets/:ticketId` | Vé điện tử |

Admin:

| Đường dẫn | Nội dung |
| --- | --- |
| `/admin` | Dashboard: tổng booking, tổng chuyến, tổng user, tổng doanh thu |
| `/admin/stations` | Quản lý ga |
| `/admin/routes` | Quản lý tuyến |
| `/admin/trains`, `/admin/trains/:id` | Quản lý tàu; toa và ghế nằm trong trang chi tiết |
| `/admin/trips` | Quản lý chuyến |
| `/admin/bookings`, `/admin/bookings/:id` | Quản lý booking |
| `/admin/users` | Danh sách tài khoản, chỉ xem |

## Phạm vi

Trong phạm vi MVP: xác thực, phân quyền, quản lý ga / tuyến / tàu / toa / chuyến,
sinh ghế tự động, TripSeat, tìm chuyến, chọn toa và chỗ trực quan, giữ chỗ, thông
tin hành khách, review, thanh toán mô phỏng, vé điện tử, My Trips, hủy booking.

Tính năng mở rộng, chỉ làm sau khi MVP ổn định: QR code trên vé, quét QR, bản đồ
đường sắt (Leaflet + OpenStreetMap), xuất vé PDF. Các tính năng này không được
làm ảnh hưởng tiến độ luồng booking chính.

Ngoài phạm vi: staff và cổng nhân viên, kiểm vé, check-in, thanh toán ngân hàng
thật, payment gateway, hoàn tiền thật, ga trung gian, `RouteStation`,
`TripStation`, GPS, theo dõi tàu real-time, multi-train hoặc round-trip trong một
booking, dynamic pricing, giá theo độ tuổi hoặc đối tượng, loyalty, voucher,
khuyến mãi, đặt đồ ăn, thông báo email và SMS, hóa đơn điện tử, kế toán,
analytics nâng cao, filter tìm kiếm nâng cao, OAuth, quên mật khẩu, đặt lại mật
khẩu.

## Yêu cầu phi chức năng

- **Responsive**: các trang Customer dùng được trên desktop, tablet và mobile.
  Ưu tiên landing, tìm kiếm, chọn chỗ, My Trips và vé.
- **Validation**: backend luôn tự kiểm tra dữ liệu, không tin validation ở
  frontend.
- **Security**: mật khẩu phải được hash; Customer không vào được trang Admin và
  không xem được booking của người khác; backend kiểm tra cả role lẫn quyền sở
  hữu.
- **Data consistency**: xác nhận booking, hủy booking và giữ chỗ phải chạy trong
  transaction.
- **Error handling**: xử lý rõ các trường hợp chuyến không tồn tại, chuyến bị
  hủy, ghế vừa bị người khác đặt, hết hạn giữ chỗ, booking không tồn tại, booking
  không thuộc về Customer đang đăng nhập, và form không hợp lệ.

## Định hướng giao diện

Phong cách Modern Travel: nền sáng, card đơn giản, typography rõ, một màu primary
dễ nhận diện, whitespace hợp lý. Trạng thái ghế phải nhìn là hiểu ngay. Landing
ưu tiên branding và form tìm chuyến; trang booking ưu tiên chức năng và trạng
thái rõ ràng; trang admin ưu tiên bảng, form và CRUD mạch lạc.

## Điểm chưa chốt

Tên hiển thị đã chốt là **TrainGo** ngày 24/08/2026; `voyage-go` chỉ còn là tên
repository. Xem `docs/decisions/0003-he-thong-giao-dien.md`.

Các điểm còn mở nằm ở `docs/plans/active/mvp-traingo.md`: cách thu hồi TripSeat
hết hạn giữ chỗ, nội dung seed data, và quy tắc xóa dữ liệu đã bị tham chiếu.
