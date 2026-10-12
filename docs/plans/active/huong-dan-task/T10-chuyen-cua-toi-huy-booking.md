# T10 Chuyến của tôi và hủy booking

> **Tóm tắt:** sau task này, khách đã đăng nhập vào trang `/my-trips` để xem các booking
> của mình, chia thành Sắp đi, Đã đi, Đã hủy, xem chi tiết từng booking và tự hủy booking
> còn hủy được.

| Đợt | Phần việc | Độ khó | Cần xong trước | Task đang chờ task này | Tên branch |
| --- | --- | --- | --- | --- | --- |
| 2 | C | Vừa (khoảng 2-3 ngày) | T02, quyết định D8 | T13 (admin hủy booking dùng lại service của task này) | `feat/t10-my-trips` |

Các từ như transaction, `@EntityGraph`, DTO được giải thích trong `README.md` cùng thư
mục.

## 1. Task này làm gì

Khách đã đăng nhập mở `/my-trips` và thấy các booking của chính mình, chia ba nhóm Sắp đi,
Đã đi, Đã hủy (`SPEC.md` mục 30). Từ đó khách xem chi tiết một booking, mở vé, và hủy
booking nếu còn hủy được (`SPEC.md` mục 31).

Bạn không cần chờ luồng đặt vé làm xong mới bắt đầu. Dữ liệu mẫu (seed) đã có sẵn booking,
nên bạn đăng nhập bằng các tài khoản dưới đây là thử được ngay:

| Tài khoản (mật khẩu `Customer@123`) | Booking trong seed |
| --- | --- |
| `an.nguyen@example.com` | 1 sắp đi (SNT1, 2 khách), 1 đã hủy (SE22) |
| `binh.tran@example.com` | 1 sắp đi (SE19, toa giường) |
| `cuong.le@example.com` | 1 đã đi (SNT1, chuyến `COMPLETED`) |

Seed `V3__seed_more_demo_data.sql` còn thêm booking cho ba tài khoản này và cho tám khách
khác, nên mỗi nhóm có thể dài hơn bảng trên. Bảng chỉ ghi các booking của seed `V2`.

## 2. Phạm vi

Làm:

- Danh sách booking của khách, chia ba nhóm.
- Trang chi tiết một booking.
- Hủy booking.
- Kiểm tra quyền sở hữu: khách chỉ xem và hủy được booking của chính mình.

Không làm:

- Lọc và tìm kiếm booking (`SPEC.md` mục 30 không yêu cầu).
- Phí hủy, hoàn tiền (`SPEC.md` mục 31).
- Trang vé: thuộc T12. Ở task này bạn chỉ gắn link tới trang vé.

## 3. Dữ liệu dùng tới

```
users 1 ──< bookings >── 1 trips >── routes, trains
               │
               └──< booking_passengers >── 1 trip_seats >── 1 seats >── coaches
                          │
                          └── 1 tickets

bookings: customer_id, trip_id, booking_code, total_price,
          booking_status (PENDING | CONFIRMED | CANCELLED),
          payment_status (UNPAID | PAID), paid_at, created_at
```

Đọc sơ đồ: một khách (`users`) có nhiều booking. Mỗi booking thuộc một chuyến (`trips`) và
có nhiều hành khách (`booking_passengers`). Mỗi hành khách ngồi đúng một chỗ của chuyến
(`trip_seats`) và có đúng một vé (`tickets`). Ký hiệu `1 ──<` nghĩa là "một ... có nhiều ...".

Khi khách hủy, hai thay đổi dưới đây phải xảy ra trong **cùng một** transaction
(`SPEC.md` mục 45.4). Vì sao: nếu chỉ đổi được một nửa, sẽ có booking đã hủy mà chỗ vẫn
bị khóa, hoặc chỗ đã trống mà booking vẫn còn hiệu lực.

```
bookings.booking_status   CONFIRMED -> CANCELLED
trip_seats.status         BOOKED    -> AVAILABLE   (mọi chỗ của booking)
```

`payment_status` vẫn giữ `PAID`, vì dự án không hoàn tiền thật. Vé cũng giữ nguyên; trang
vé (T12) tự hiện nhãn "Đã hủy" dựa vào trạng thái booking.

Cách chia ba nhóm (theo quyết định D8):

| Nhóm | Điều kiện |
| --- | --- |
| Sắp đi | `CONFIRMED` và thời điểm khởi hành còn ở sau hiện tại |
| Đã đi | `CONFIRMED` và (chuyến `COMPLETED` hoặc thời điểm khởi hành đã qua) |
| Đã hủy | `CANCELLED` |

Booking `PENDING` (đang chờ thanh toán, theo quyết định D7) không hiện ở trang này.

## 4. Các bước làm

### Bước 1. Model: thêm method nghiệp vụ và tạo DTO

Các entity `Booking`, `BookingPassenger`, `Ticket`, `TripSeat` trong `entity/` đã có,
**không thêm hay sửa field**. Bạn chỉ thêm hai method để đổi trạng thái. Mỗi method tự kiểm
tra trạng thái hiện tại trước khi đổi, để không bao giờ hủy nhầm một booking chưa xác nhận:

```java
// Booking
public void cancel() {
    if (bookingStatus != BookingStatus.CONFIRMED) {
        throw new IllegalStateException("Only a confirmed booking can be cancelled");
    }
    bookingStatus = BookingStatus.CANCELLED;
}

// TripSeat
public void releaseBooked() {
    if (status != TripSeatStatus.BOOKED) {
        throw new IllegalStateException("Seat is not booked");
    }
    status = TripSeatStatus.AVAILABLE;
}
```

Tạo các DTO bằng `record` trong `model/response/`, mỗi record một file. Đây là dữ liệu đưa
ra trang, đã gom sẵn những gì view cần:

```java
record PassengerLine(String fullName, int coachNumber, String seatCode,
        long ticketPrice, Long ticketId) { }

record MyBooking(Long id, String bookingCode, String routeLabel, String trainCode,
        LocalDate departureDate, LocalTime departureTime, BookingStatus bookingStatus,
        PaymentStatus paymentStatus, long totalPrice, boolean cancellable,
        List<PassengerLine> passengers) { }

record MyTrips(List<MyBooking> upcoming, List<MyBooking> completed,
        List<MyBooking> cancelled) { }
```

### Bước 2. Repository: thêm câu truy vấn

Thêm vào `repository/BookingRepository`:

```java
@EntityGraph(attributePaths = {"trip", "trip.train", "trip.route.originStation",
        "trip.route.destinationStation"})
List<Booking> findByCustomerIdAndBookingStatusInOrderByCreatedAtDesc(
        Long customerId, Collection<BookingStatus> statuses);

@EntityGraph(attributePaths = {"trip", "trip.train", "trip.route.originStation",
        "trip.route.destinationStation"})
Optional<Booking> findByIdAndCustomerId(Long id, Long customerId);
```

- Method đầu lấy mọi booking của một khách theo các trạng thái cho trước, mới nhất trước.
- Method sau chỉ tìm thấy booking khi nó **thuộc đúng khách đó**. Đây là cách kiểm tra quyền
  sở hữu ngay trong câu truy vấn.
- `@EntityGraph` đọc luôn chuyến, tàu và hai ga trong cùng câu `SELECT`, để view không bị
  lỗi `LazyInitializationException`.

Thêm vào `repository/BookingPassengerRepository`. Method này lấy hành khách của **mọi**
booking trong một lần, thay vì hỏi database riêng cho từng booking:

```java
@EntityGraph(attributePaths = {"tripSeat", "tripSeat.seat", "tripSeat.seat.coach"})
List<BookingPassenger> findByBookingIdInOrderByIdAsc(Collection<Long> bookingIds);
```

Thêm vào `repository/TicketRepository`:

```java
List<Ticket> findByBookingPassengerIdIn(Collection<Long> passengerIds);
```

### Bước 3. Service: xử lý nghiệp vụ

Tạo `service/MyTripsService`. Service này lấy "bây giờ" bằng `LocalDateTime.now()`:

- `@Transactional(readOnly = true) MyTrips findFor(long customerId)`: chạy ba câu truy vấn
  ở bước 2, gom hành khách và vé theo từng booking, chia ba nhóm, rồi dựng DTO **ngay bên
  trong** transaction.
- `@Transactional(readOnly = true) MyBooking findOne(long bookingId, long customerId)`: dùng
  `findByIdAndCustomerId`; không thấy thì ném `BookingNotFoundException` (trang 404).

Tạo `service/BookingCancellationService`. T13 sẽ dùng lại service này cho admin:

```java
@Transactional
public Booking cancelByCustomer(long bookingId, long customerId) { ... }

@Transactional
public Booking cancelByAdmin(long bookingId) { ... }
```

Cả hai method cùng gọi một method riêng làm các việc sau:

1. Kiểm tra booking đang `CONFIRMED`.
2. Kiểm tra chuyến chưa khởi hành: `trip.departureAt()` còn ở sau `LocalDateTime.now()`.
3. Gọi `booking.cancel()`, rồi `releaseBooked()` cho mọi `TripSeat` của booking.

Sai một điều kiện thì ném `BookingCannotBeCancelledException` kèm lý do. Hai method chỉ khác
nhau ở một điểm: bản của khách tìm booking bằng `findByIdAndCustomerId`, nên khách không
bao giờ hủy được booking của người khác.

Hai exception `BookingNotFoundException` (`@ResponseStatus(NOT_FOUND)`) và
`BookingCannotBeCancelledException` đặt trong `common/exception/`, cạnh
`StationNotFoundException`.

### Bước 4. Controller: nhận request

Tạo `controller/MyTripsController` với `@RequestMapping("/my-trips")`:

| Method | Đường dẫn | Kết quả |
| --- | --- | --- |
| GET | `/my-trips` | Trang `my-trips/list` với `MyTrips` |
| GET | `/my-trips/{bookingId}` | Trang `my-trips/detail`; booking của người khác hoặc không tồn tại: 404 |
| POST | `/my-trips/{bookingId}/cancel` | Chuyển về `/my-trips/{bookingId}` kèm thông báo xanh hoặc đỏ |

Luôn lấy người đang đăng nhập bằng `@AuthenticationPrincipal TrainGoUserDetails me` và dùng
`me.getId()`. Không bao giờ nhận id khách từ form hay đường dẫn.

**Booking của người khác trả 404, không trả 403.** Vì sao: 403 nghĩa là "có nhưng bạn không
được xem", tức là cho người dò biết mã booking đó có tồn tại. 404 thì không lộ gì.

### Bước 5. Giao diện

Dùng khung trang cho khách và `app.css`:

- `templates/my-trips/list.html`: ba tab hoặc ba khối. Mỗi dòng có tuyến, ngày đi, tàu, toa
  và chỗ, trạng thái booking, trạng thái thanh toán (`SPEC.md` mục 30), và nút Xem. Nhóm
  nào rỗng thì hiện "Chưa có chuyến nào".
- `templates/my-trips/detail.html`: thông tin chuyến, bảng hành khách (tên, toa, chỗ, giá,
  link `/tickets/{ticketId}` của T12) và tổng tiền. Nút "Hủy booking" là form `POST` có
  `th:data-confirm` để hỏi lại, và chỉ hiện khi `cancellable`.

## 5. Luật và lỗi phải xử lý

- Chỉ hủy được booking `CONFIRMED` của chuyến chưa khởi hành.
- Hủy thì booking thành `CANCELLED` và mọi chỗ của nó trống lại, trong cùng một transaction.
- Booking của người khác, hoặc không tồn tại: trả 404.
- Hủy không được (đã hủy rồi, chuyến đã đi): báo lý do bằng thông báo đỏ, không đổi gì.

## 6. Kịch bản thử bằng tay

`AGENTS.md` coi hủy booking và quyền sở hữu là nghiệp vụ bắt buộc phải thử kỹ. Chạy ứng
dụng bằng `.\mvnw.cmd spring-boot:run`, mở `http://localhost:8081`. Khi cần xem database,
mở MySQL bằng lệnh:

```
docker exec -it traingo-mysql mysql -utraingo -ptraingo --default-character-set=utf8mb4 traingo
```

**Kịch bản 1. Xem danh sách theo ba nhóm.**

1. Đăng nhập `an.nguyen@example.com`, mở `/my-trips`.
2. Kết quả đúng: booking SNT1 (2 khách) nằm ở Sắp đi, booking SE22 nằm ở Đã hủy. Mỗi dòng
   có tuyến, ngày đi, tàu, toa và chỗ, trạng thái booking, trạng thái thanh toán.
3. Đăng nhập `cuong.le@example.com`: booking trên chuyến `COMPLETED` nằm ở Đã đi.

**Kịch bản 2. Hủy booking sắp đi.**

1. Vẫn với `an.nguyen@example.com`, mở chi tiết booking SNT1 sắp đi. Ghi lại mã booking.
2. Trước khi hủy, xem trạng thái chỗ:

   ```sql
   SELECT b.booking_code, b.booking_status, b.payment_status, ts.id, ts.status
   FROM bookings b
   JOIN booking_passengers bp ON bp.booking_id = b.id
   JOIN trip_seats ts ON ts.id = bp.trip_seat_id
   WHERE b.booking_code = '<mã booking>';
   ```

   Kết quả: `CONFIRMED`, `PAID`, mọi chỗ `BOOKED`.
3. Bấm "Hủy booking", xác nhận. Trang hiện thông báo xanh, booking chuyển sang nhóm Đã hủy.
4. Chạy lại câu SQL trên. Kết quả đúng: `CANCELLED`, vẫn `PAID`, mọi chỗ `AVAILABLE`. Nếu
   T09 đã xong, trang chọn chỗ của chuyến đó cũng hiện các chỗ này là trống.

**Kịch bản 3. Không hủy được hai lần.**

1. Mở lại chi tiết booking vừa hủy: không còn nút "Hủy booking".
2. Gửi thẳng `POST /my-trips/<id>/cancel` cho booking đó (ví dụ bằng tab DevTools, hoặc mở
   trang chi tiết ở tab khác trước khi hủy rồi bấm nút ở tab cũ).
3. Kết quả đúng: thông báo đỏ nêu lý do, database không đổi gì.

**Kịch bản 4. Không hủy được chuyến đã đi.**

1. Đăng nhập `cuong.le@example.com`, mở chi tiết booking ở nhóm Đã đi: không có nút hủy.
2. Gửi thẳng `POST /my-trips/<id>/cancel` cho booking đó như kịch bản 3.
3. Kết quả đúng: thông báo đỏ, booking vẫn `CONFIRMED`, các chỗ vẫn `BOOKED`.

**Kịch bản 5. Quyền sở hữu.**

1. Tìm id một booking của người khác:

   ```sql
   SELECT b.id, b.booking_code, u.email
   FROM bookings b JOIN users u ON u.id = b.customer_id
   ORDER BY b.id;
   ```

2. Đăng nhập `an.nguyen@example.com`, mở `/my-trips/<id booking của binh.tran>`.
   Kết quả đúng: trang 404, không phải 403 và không lộ thông tin gì của booking.
3. Gửi `POST /my-trips/<id booking của binh.tran>/cancel`. Kết quả đúng: 404, và chạy lại
   SQL ở kịch bản 2 cho booking đó thấy trạng thái không đổi.
4. Mở `/my-trips/999999` (id không tồn tại): cũng 404.

Muốn chạy lại từ đầu với dữ liệu sạch, làm mới database theo `docs/RUNBOOK.md`.

## 7. Lỗi hay gặp

- Tìm booking bằng `findById` rồi mới so khách: dễ quên bước so ở một handler nào đó. Dùng
  `findByIdAndCustomerId` thì không thể quên.
- Trả 403 cho booking của người khác: lộ ra là mã booking đó có tồn tại.
- Đổi trạng thái booking và chỗ ở hai transaction khác nhau: lỗi giữa chừng để lại dữ liệu
  nửa vời.
- Dựng DTO bên ngoài service: chạm vào dữ liệu `LAZY` chưa đọc, ứng dụng báo
  `LazyInitializationException`.

## 8. Kiểm tra trước khi báo xong

- [ ] Đăng nhập `an.nguyen@example.com`: thấy booking SNT1 ở Sắp đi, booking SE22 ở Đã hủy.
- [ ] Hủy booking sắp đi: booking chuyển sang nhóm Đã hủy; mọi chỗ của booking đó trống lại
      (kiểm tra bằng SQL ở mục 6, hoặc trên trang chọn chỗ của T09).
- [ ] Đổi id trên đường dẫn sang booking của người khác: nhận 404, cả khi xem lẫn khi hủy.
- [ ] `cuong.le@example.com` thấy booking ở nhóm Đã đi, không có nút hủy.
- [ ] Đã đi hết các kịch bản ở mục 6.
- [ ] Kiểm tra giao diện ở màn hình rộng 375px.
- [ ] `.\mvnw.cmd compile` chạy không lỗi và ứng dụng khởi động được
      (`.\mvnw.cmd spring-boot:run`).
- [ ] Có một dòng trong `docs/WORKLOG.md`, nằm trong cùng commit với code.
