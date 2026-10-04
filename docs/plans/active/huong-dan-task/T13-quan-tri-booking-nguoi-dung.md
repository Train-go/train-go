# T13 Quản trị booking, người dùng và dashboard

| Wave | Lane | Cỡ | Phụ thuộc | Mở khóa | Branch |
| --- | --- | --- | --- | --- | --- |
| 3 | A | M | T10 (service hủy booking) | Xóa hết dữ liệu giả của khu admin | `feat/t13-admin-bookings` |

## Mục tiêu

Ba màn hình admin cuối cùng đọc database thật:

- `/admin/bookings` và `/admin/bookings/{id}`: xem danh sách, tìm theo mã booking, xem chi
  tiết khách, hành khách, chuyến, chỗ, thanh toán, và hủy booking (`SPEC.md` mục 39).
- `/admin/users`: danh sách tài khoản, chỉ xem (mục 40).
- `/admin`: dashboard với bốn con số thật (mục 33).

Xong task này, khu admin không còn dữ liệu giả nào.

## Phạm vi

Trong phạm vi: ba màn hình trên, ô tìm kiếm trên header admin.

Ngoài phạm vi (`SPEC.md` mục 39): đổi chỗ, sửa thanh toán, hoàn tiền, đổi hành khách,
CRUD người dùng.

## Database

```
users 1 ──< bookings >── 1 trips >── routes, trains
               └──< booking_passengers >── trip_seats >── seats >── coaches
```

Số liệu dashboard:

| Con số | Cách tính |
| --- | --- |
| Tổng booking | Số booking `CONFIRMED` (không tính `PENDING` đang chờ và `CANCELLED`) |
| Tổng chuyến | `trips.count()` |
| Tổng người dùng | Số tài khoản `CUSTOMER` |
| Doanh thu | Tổng `total_price` của booking `CONFIRMED` |

Booking đã hủy không tính vào doanh thu, dù vẫn `PAID`: dự án không mô phỏng hoàn tiền,
nhưng tiền của booking đã hủy cũng không phải doanh thu thật. Phi xác nhận cách tính này
trước khi làm.

## Model

DTO, `record` trong `booking/model/` và `admin/model/`:

```java
record AdminBookingRow(Long id, String bookingCode, String customerName,
        String tripLabel, int passengerCount, long totalPrice,
        BookingStatus bookingStatus, PaymentStatus paymentStatus) { }

record DashboardStats(long confirmedBookings, long trips, long customers,
        long revenue, List<AdminBookingRow> recentBookings,
        List<DailyRevenue> lastSevenDays) { }

record DailyRevenue(LocalDate day, long amount) { }
```

Chi tiết booking dùng lại `MyBooking` của T10, cộng thêm tên và email khách.

## Repository

`BookingRepository`:

```java
@EntityGraph(attributePaths = {"customer", "trip", "trip.train",
        "trip.route.originStation", "trip.route.destinationStation"})
List<Booking> findByBookingCodeContainingIgnoreCaseOrderByCreatedAtDesc(String q);

@EntityGraph(attributePaths = {"customer", "trip", "trip.train",
        "trip.route.originStation", "trip.route.destinationStation"})
List<Booking> findTop5ByOrderByCreatedAtDesc();

long countByBookingStatus(BookingStatus status);

@Query("""
    select coalesce(sum(b.totalPrice), 0) from Booking b
    where b.bookingStatus = :status""")
long sumTotalPrice(BookingStatus status);

List<Booking> findByBookingStatusAndPaidAtGreaterThanEqual(
        BookingStatus status, LocalDateTime from);
```

`BookingPassengerRepository`: đếm hành khách của nhiều booking trong một query:

```java
@Query("""
    select bp.booking.id as bookingId, count(bp) as passengers
    from BookingPassenger bp where bp.booking.id in :ids
    group by bp.booking.id""")
List<BookingPassengerCount> countByBooking(Collection<Long> ids);
```

`UserRepository`:

```java
List<User> findAllByOrderByCreatedAtDesc();
long countByRole(UserRole role);
```

## Service

| Class | Method | Việc |
| --- | --- | --- |
| `booking/service/AdminBookingService` | `List<AdminBookingRow> search(String q)` | `q` rỗng: mọi booking; có `q`: lọc theo mã |
| | `AdminBookingDetail findDetail(long id)` | Không thấy thì 404 |
| | `Booking cancel(long id)` | Gọi `BookingCancellationService.cancelByAdmin(id)` của T10; cùng luật với khách |
| `admin/service/DashboardService` | `DashboardStats load()` | Bốn con số, 5 booking mới nhất, doanh thu 7 ngày gần nhất (gom theo `paidAt.toLocalDate()` trong Java) |

## Controller

`booking/controller/BookingAdminController`, `@RequestMapping("/admin/bookings")`,
`activeNav = "bookings"`:

| Method | URL | Kết quả |
| --- | --- | --- |
| GET | `/admin/bookings?q=` | Danh sách, ô tìm điền sẵn `q` |
| GET | `/admin/bookings/{id}` | Chi tiết |
| POST | `/admin/bookings/{id}/cancel` | Redirect về chi tiết kèm thông báo xanh hoặc đỏ |

`auth/controller/UserAdminController`: `GET /admin/users`, `activeNav = "users"`.

`admin/controller/AdminController`: chỉ còn `GET /admin`, đọc `DashboardService`.

## View

- `templates/admin/bookings.html`: cột Mã booking (link chi tiết), Khách hàng, Chuyến, Số
  khách, Tổng tiền, Thanh toán; badge màu theo trạng thái.
- `templates/admin/booking-detail.html` (mới): khách (tên, email), chuyến, bảng hành khách
  (tên, toa, chỗ, giá), thanh toán (`paidAt`), nút Hủy với `th:data-confirm` khi còn hủy
  được.
- `templates/admin/users.html`: cột Tên, Email, Vai trò, Ngày tạo
  (`#temporals.format(user.createdAt, 'dd/MM/yyyy')`).
- `templates/admin/dashboard.html`: thay bốn con số; bảng "Booking gần đây" dùng
  `AdminBookingRow`; biểu đồ ApexCharts nhận dữ liệu thật qua
  `<script th:inline="javascript">`.
- Ô tìm kiếm trên header (`admin/fragments/header.html`, của T02) đã gửi
  `GET /admin/bookings` với ô tên `q`. Chỉ cần kiểm tra tên ô là `q`; nếu phải sửa thì
  báo người làm T02.

## Gỡ dữ liệu giả

Xóa `admin/service/AdminService.java` và cả thư mục `admin/model/` (`Booking`, `Trip`,
`User` và những file còn lại). Sau task này, `grep -r "AdminService" app/src` không còn
kết quả nào.

## Test

CRUD admin không bắt buộc. Phân quyền `/admin/**` đã có test ở T02. Nên có unit test cho
`DashboardService`: booking `CANCELLED` không tính vào doanh thu.

## Xong khi

- [ ] Dashboard với seed: 3 booking `CONFIRMED`, 114 chuyến, 3 khách hàng, doanh thu
      1.900.000đ (700.000 + 850.000 + 350.000).
- [ ] Tìm `004` trên header ra đúng booking `...004`; mở chi tiết thấy hành khách và chỗ.
- [ ] Hủy booking sắp đi từ trang admin: chỗ trống lại, booking hiện ở nhóm Đã hủy trong
      `/my-trips` của khách đó.
- [ ] `/admin/users` hiện 4 tài khoản, mới nhất trước.
- [ ] Không còn `AdminService`. `.\mvnw.cmd test` xanh. Một dòng `docs/WORKLOG.md`.
