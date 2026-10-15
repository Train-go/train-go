# T13 Quản trị booking, người dùng và dashboard

> **Tóm tắt:** sau task này, admin tìm được booking theo mã, xem chi tiết và hủy booking,
> mỗi màn hình admin có controller riêng, và biểu đồ doanh thu trên dashboard dùng số liệu
> thật. Khu quản trị không còn dữ liệu giả nào.

| Đợt | Phần việc | Độ khó | Cần xong trước | Task đang chờ task này | Tên branch |
| --- | --- | --- | --- | --- | --- |
| 3 | A | Vừa (khoảng 2-3 ngày) | T10 (service hủy booking) | T14 | `feat/t13-admin-bookings` |

Các từ như DTO, `@EntityGraph`, flash message được giải thích trong `README.md` cùng thư
mục.

## 1. Task này làm gì

Từ 11/10/2026, ba màn hình `/admin`, `/admin/bookings` và `/admin/users` đã đọc database
thật qua `controller/AdminController` và `service/AdminService`. Bốn con số trên dashboard
và hai danh sách đều là số thật. Phần còn thiếu, task này làm nốt:

- `/admin/bookings`: tìm theo mã booking (`SPEC.md` mục 39).
- `/admin/bookings/{id}`: xem chi tiết khách, hành khách, chuyến, chỗ, thanh toán, và hủy
  booking (`SPEC.md` mục 39).
- Chuyển màn hình booking và người dùng ra controller riêng, đúng quy tắc mỗi nghiệp vụ một
  controller (`docs/decisions/0001-cau-truc-du-an-va-tech-stack.md`).
- Thay dữ liệu giả của biểu đồ "Doanh thu theo ngày" bằng doanh thu thật của 7 ngày gần
  nhất.

`/admin/users` chỉ để xem (`SPEC.md` mục 40), nên ở màn hình này bạn chỉ chuyển code sang
controller mới, không thêm chức năng.

## 2. Phạm vi

Làm:

- Tìm booking theo mã, trang chi tiết booking, admin hủy booking.
- Ô tìm kiếm trên thanh đầu trang (header) của khu admin.
- Tách `BookingAdminController` và `UserAdminController` khỏi `AdminController`.
- Doanh thu 7 ngày cho biểu đồ dashboard.

Không làm (`SPEC.md` mục 39):

- Đổi chỗ, sửa trạng thái thanh toán, hoàn tiền, đổi hành khách.
- Thêm, sửa, xóa người dùng.
- Đổi cách tính bốn con số trên dashboard: chúng đã đúng, giữ nguyên.

## 3. Dữ liệu dùng tới

```
users 1 ──< bookings >── 1 trips >── routes, trains
               └──< booking_passengers >── trip_seats >── seats >── coaches
```

Đọc sơ đồ: một người dùng có nhiều booking, mỗi booking thuộc một chuyến (chuyến gắn với
tuyến và tàu). Một booking có nhiều hành khách; mỗi hành khách ngồi một chỗ của chuyến, chỗ
đó là một ghế thuộc một toa.

Bốn con số trên dashboard hiện tính như sau, trong `AdminService`. Task này không đổi chúng:

| Con số | Cách tính | Method |
| --- | --- | --- |
| Tổng booking | Mọi booking, kể cả đã hủy | `totalBookings()` |
| Tổng chuyến | Mọi chuyến | `totalTrips()` |
| Tổng người dùng | Mọi tài khoản, kể cả admin | `totalUsers()` |
| Doanh thu | Tổng `total_price` của booking `CONFIRMED` | `totalRevenue()` |

Booking đã hủy không tính vào doanh thu, dù vẫn `PAID`: dự án không mô phỏng hoàn tiền,
nhưng tiền của booking đã hủy cũng không phải doanh thu thật. Biểu đồ 7 ngày dùng cùng luật
này, gom theo ngày của `paid_at`.

## 4. Các bước làm

### Bước 1. Model: tạo DTO

Không thêm entity hay field nào. Tạo hai `record` trong `model/response/`, mỗi record một
file:

```java
record AdminBookingRow(Long id, String bookingCode, String customerName,
        String tripLabel, int passengerCount, long totalPrice,
        BookingStatus bookingStatus, PaymentStatus paymentStatus) { }

record DailyRevenue(LocalDate day, long amount) { }
```

- `AdminBookingRow`: một dòng trong bảng booking. Khác `AdminBooking` đang dùng ở chỗ có
  `id`, để dòng đó link được tới trang chi tiết, và giữ trạng thái dạng enum.
- `DailyRevenue`: doanh thu của một ngày, dùng cho biểu đồ.

Trang chi tiết booking dùng lại `MyBooking` của T10, cộng thêm tên và email khách.

### Bước 2. Repository: thêm câu truy vấn

Các method dashboard cần đã có sẵn: `findAllByOrderByCreatedAtDesc`,
`findTop5ByOrderByCreatedAtDesc`, `sumTotalPriceByBookingStatus` trong
`repository/BookingRepository`, `countByBooking()` trong
`repository/BookingPassengerRepository` (đếm hành khách của mọi booking trong một câu truy
vấn) và `findAllByOrderByCreatedAtAsc` trong `repository/UserRepository`. Bạn dùng lại,
không viết thêm bản thứ hai.

Thêm vào `repository/BookingRepository` hai method:

```java
@EntityGraph(attributePaths = {"customer", "trip", "trip.train",
        "trip.route.originStation", "trip.route.destinationStation"})
List<Booking> findByBookingCodeContainingIgnoreCaseOrderByCreatedAtDesc(String q);

List<Booking> findByBookingStatusAndPaidAtGreaterThanEqual(
        BookingStatus status, LocalDateTime from);
```

- `findByBookingCodeContainingIgnoreCase...`: tìm booking có mã chứa chuỗi `q`, không phân
  biệt hoa thường.
- `findByBookingStatusAndPaidAtGreaterThanEqual`: booking trả tiền từ một mốc thời gian, để
  tính doanh thu 7 ngày.

Trang chi tiết đọc hành khách bằng `findByBookingIdOrderByIdAsc` (T12) hoặc
`findByBookingIdInOrderByIdAsc` (T10) của `BookingPassengerRepository`, và vé bằng
`findByBookingPassengerIdIn` (T10) của `TicketRepository`.

### Bước 3. Service: xử lý nghiệp vụ

| Class | Method | Việc |
| --- | --- | --- |
| `service/AdminBookingService` (mới) | `List<AdminBookingRow> search(String q)` | `q` rỗng: lấy mọi booking; có `q`: lọc theo mã |
| | `AdminBookingDetail findDetail(long id)` | Không thấy thì ném `BookingNotFoundException` (T10), trang 404 |
| | `Booking cancel(long id)` | Gọi `BookingCancellationService.cancelByAdmin(id)` của T10; cùng luật với khách |
| `service/UserAdminService` (mới) | `List<User> findAll()` | Chuyển từ `AdminService.users()` sang, `readOnly` |
| `service/AdminService` (sửa) | `List<AdminBookingRow> recentBookings()` | Đổi kiểu trả về sang `AdminBookingRow` |
| | `List<DailyRevenue> lastSevenDays()` (mới) | Doanh thu 7 ngày gần nhất, gom theo `paidAt.toLocalDate()` trong Java; ngày không có booking vẫn có dòng với số 0 |

Phần dựng dòng bảng (`toRows` và nhãn trạng thái) chuyển từ `AdminService` sang
`AdminBookingService`. `recentBookings()` của dashboard gọi lại phần đó, không chép thêm bản
thứ hai. `AdminBookingDetail` là record `MyBooking` của T10 cộng tên và email khách, đặt
trong `model/response/`.

Hủy booking dùng lại service của T10, không viết lại luật hủy. Vì sao: một luật chỉ nên nằm
ở một chỗ, để khách và admin luôn bị cùng một luật kiểm tra.

### Bước 4. Controller: nhận request

Tạo `controller/BookingAdminController` với `@RequestMapping("/admin/bookings")` và
`activeNav = "bookings"`:

| Method | Đường dẫn | Kết quả |
| --- | --- | --- |
| GET | `/admin/bookings?q=` | Danh sách, ô tìm kiếm điền sẵn `q` |
| GET | `/admin/bookings/{id}` | Chi tiết booking |
| POST | `/admin/bookings/{id}/cancel` | Chuyển về trang chi tiết kèm thông báo xanh hoặc đỏ |

Tạo `controller/UserAdminController`: `GET /admin/users`, `activeNav = "users"`.

Sửa `controller/AdminController`: xóa hai handler `bookings` và `users`. Handler
`dashboard` thêm thuộc tính `revenueByDay` từ `adminService.lastSevenDays()`. Sau khi T03,
T04, T07 và task này cùng xong, `AdminController` chỉ còn `GET /admin`.

### Bước 5. Giao diện

- `templates/admin/bookings.html`: cột Mã booking (link tới chi tiết), Khách hàng, Chuyến,
  Số khách, Tổng tiền, Thanh toán; nhãn (badge) có màu theo trạng thái. Nút "Xem chi tiết"
  ở cột Thao tác bỏ `disabled` và trỏ tới `/admin/bookings/{id}`; nút hủy cũng dẫn tới trang
  chi tiết, nơi admin xác nhận hủy.
- `templates/admin/booking-detail.html` (tạo mới): khách (tên, email), chuyến, bảng hành
  khách (tên, toa, chỗ, giá), thông tin thanh toán (`paidAt`), và nút Hủy có
  `th:data-confirm` khi booking còn hủy được.
- `templates/admin/users.html`: cột Tên, Email, Vai trò, Ngày tạo đọc thẳng từ entity
  `User`: `user.fullName`, `user.email`, `user.role`,
  `#temporals.format(user.createdAt, 'dd/MM/yyyy')`.
- `templates/admin/dashboard.html`: bảng "Booking gần đây" dùng `AdminBookingRow`, mã booking
  link tới `/admin/bookings/{id}`. Biểu đồ ApexCharts bỏ mảng số viết cứng, nhận
  `revenueByDay` qua `<script th:inline="javascript">`.
- Ô tìm kiếm trên header (`admin/fragments/header.html`) đã gửi `GET /admin/bookings` với ô
  tên `q`. Bạn chỉ cần kiểm tra tên ô đúng là `q`; nếu phải sửa file đó thì báo người làm
  T02.

### Bước 6. Dọn class giả còn lại

Khi không còn chỗ nào dùng, xóa `model/response/AdminBooking.java` và
`model/response/AdminUser.java`, cùng hai method `bookings()` và `users()` của
`AdminService`. Sau khi T03, T04, T07 và task này cùng xong, `AdminService` chỉ còn phần
dashboard và trong `model/response/` không còn class `Admin*` cũ nào (`AdminBookingRow` và
`AdminBookingDetail` là của task này).

## 5. Luật và lỗi phải xử lý

- Bốn con số trên dashboard giữ đúng cách tính ở mục 3.
- Booking `CANCELLED` và `PENDING` không tính vào doanh thu, kể cả trên biểu đồ.
- Admin hủy booking theo đúng luật hủy của khách (dùng chung service của T10).
- Booking không tồn tại: trả 404.
- Danh sách người dùng chỉ để xem, không có nút thêm, sửa, xóa.

## 6. Kịch bản thử bằng tay

Chạy ứng dụng bằng `.\mvnw.cmd spring-boot:run`, đăng nhập `admin@traingo.vn` (mật khẩu
`Admin@123`). Khi cần xem database, mở MySQL bằng lệnh:

```
docker exec -it traingo-mysql mysql -utraingo -ptraingo --default-character-set=utf8mb4 traingo
```

Đừng so với một con số cố định: dữ liệu đổi theo ngày tạo database và theo những gì bạn đã
đặt hay hủy khi thử. Luôn so trang với kết quả SQL chạy cùng lúc.

**Kịch bản 1. Dashboard.**

1. Mở `/admin`, chạy:

   ```sql
   SELECT COUNT(*) FROM bookings;
   SELECT COUNT(*) FROM trips;
   SELECT COUNT(*) FROM users;
   SELECT COALESCE(SUM(total_price), 0) FROM bookings WHERE booking_status = 'CONFIRMED';
   ```

   Kết quả đúng: bốn con số trên trang bằng bốn kết quả SQL.
2. So biểu đồ "Doanh thu theo ngày" với:

   ```sql
   SELECT DATE(paid_at) AS day, SUM(total_price) AS amount
   FROM bookings
   WHERE booking_status = 'CONFIRMED' AND paid_at >= CURDATE() - INTERVAL 6 DAY
   GROUP BY DATE(paid_at) ORDER BY day;
   ```

   Kết quả đúng: biểu đồ có đủ 7 ngày, ngày nào có trong kết quả SQL thì đúng số đó, ngày
   không có thì bằng 0.
3. Bấm một mã booking trong bảng "Booking gần đây": mở đúng trang chi tiết của booking đó.

**Kịch bản 2. Tìm theo mã.**

1. Gõ `004` vào ô tìm kiếm trên header, Enter. Kết quả đúng: trang `/admin/bookings?q=004`
   chỉ còn booking có mã kết thúc bằng `004` (booking đã hủy của `an.nguyen` trên SE22), ô
   tìm kiếm điền sẵn `004`.
2. Gõ `bk` chữ thường: ra mọi booking.
3. Gõ `xyz`: danh sách rỗng, có thông báo không tìm thấy, không lỗi.

**Kịch bản 3. Chi tiết booking.**

1. Mở chi tiết booking `...004`: thấy tên và email khách, chuyến, hành khách, toa, chỗ, giá,
   thời điểm thanh toán.
2. Mở `/admin/bookings/999999`: trang 404.

**Kịch bản 4. Admin hủy booking.**

1. Chọn một booking sắp đi, ví dụ booking `...002` của `binh.tran`. Xem trạng thái:

   ```sql
   SELECT b.booking_code, b.booking_status, b.payment_status, ts.id, ts.status
   FROM bookings b
   JOIN booking_passengers bp ON bp.booking_id = b.id
   JOIN trip_seats ts ON ts.id = bp.trip_seat_id
   WHERE b.booking_code LIKE '%002';
   ```

   Kết quả: `CONFIRMED`, `PAID`, chỗ `BOOKED`.
2. Mở chi tiết booking đó trong trang admin, bấm Hủy, xác nhận. Trang hiện thông báo xanh.
3. Chạy lại câu SQL. Kết quả đúng: `CANCELLED`, vẫn `PAID`, chỗ `AVAILABLE`.
4. Đăng xuất, đăng nhập `binh.tran@example.com`, mở `/my-trips`: booking nằm ở nhóm Đã hủy.
5. Quay lại admin, mở chi tiết booking vừa hủy: không còn nút Hủy. Gửi thẳng
   `POST /admin/bookings/<id>/cancel` (ví dụ bấm nút ở một tab mở từ trước): thông báo đỏ,
   database không đổi.
6. Làm như bước 5 với một booking trên chuyến đã đi (`...003` của `cuong.le`): không có nút
   Hủy, gửi thẳng thì bị từ chối.

**Kịch bản 5. Danh sách người dùng.**

1. Mở `/admin/users`, so với `SELECT COUNT(*) FROM users;`: số dòng bằng nhau.
2. Mỗi dòng có tên, email, vai trò, ngày tạo dạng `dd/MM/yyyy`; không có nút thêm, sửa, xóa.

**Kịch bản 6. Sidebar và phân quyền.**

1. Ở `/admin/bookings`, `/admin/bookings/{id}` và `/admin/users`, sidebar sáng đúng mục.
2. Đăng nhập một tài khoản khách, gõ thẳng `/admin/bookings`: bị chặn (phần phân quyền của
   T02).

Muốn chạy lại từ đầu với dữ liệu sạch, làm mới database theo `docs/RUNBOOK.md`.

## 7. Lỗi hay gặp

- Đếm hành khách bằng cách hỏi database riêng cho từng booking: danh sách 50 booking là 50
  câu truy vấn. Dùng `countByBooking()` để đếm trong một lần.
- Viết lại luật hủy booking trong `AdminBookingService`: hai bản luật dễ lệch nhau. Gọi
  `cancelByAdmin` của T10.
- Tính doanh thu gồm cả booking đã hủy, nhất là trên biểu đồ 7 ngày.
- Chép `toRows` sang `AdminBookingService` nhưng quên xóa bản cũ trong `AdminService`: hai
  bản dựng dòng bảng dễ lệch nhau.
- Quên `activeNav`: sidebar không sáng mục đang mở.

## 8. Kiểm tra trước khi báo xong

- [ ] Bốn con số trên dashboard bằng kết quả SQL ở mục 6; biểu đồ hiện doanh thu thật của
      7 ngày gần nhất.
- [ ] Tìm `004` trên header ra đúng booking `...004`; mở chi tiết thấy hành khách và chỗ.
- [ ] Hủy booking sắp đi từ trang admin: chỗ trống lại, booking hiện ở nhóm Đã hủy trong
      `/my-trips` của khách đó.
- [ ] `/admin/users` hiện đủ tài khoản, không có nút thêm, sửa, xóa.
- [ ] `AdminController` không còn handler `bookings`, `users`; không còn
      `AdminBooking.java`, `AdminUser.java`.
- [ ] Đã đi hết các kịch bản ở mục 6.
- [ ] `.\mvnw.cmd compile` chạy không lỗi và ứng dụng khởi động được
      (`.\mvnw.cmd spring-boot:run`).
- [ ] Có một dòng trong `docs/WORKLOG.md`, nằm trong cùng commit với code.
