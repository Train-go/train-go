# T10 Chuyến của tôi và hủy booking

| Wave | Lane | Cỡ | Phụ thuộc | Mở khóa | Branch |
| --- | --- | --- | --- | --- | --- |
| 2 | C | M | T02, D8 | T13 (admin hủy booking dùng lại service) | `feat/t10-my-trips` |

## Mục tiêu

Khách đã đăng nhập mở `/my-trips` và thấy booking của chính mình, chia ba nhóm Sắp đi,
Đã đi, Đã hủy (`SPEC.md` mục 30). Xem chi tiết một booking, mở vé, và hủy booking còn
hủy được (`SPEC.md` mục 31).

Không cần chờ luồng đặt vé: seed đã có sẵn booking để làm ngay.

| Tài khoản (mật khẩu `Customer@123`) | Booking trong seed |
| --- | --- |
| `an.nguyen@example.com` | 1 sắp đi (SNT1, 2 khách), 1 đã hủy (SE22) |
| `binh.tran@example.com` | 1 sắp đi (SE19, toa giường) |
| `cuong.le@example.com` | 1 đã đi (SNT1, chuyến `COMPLETED`) |

## Phạm vi

Trong phạm vi: danh sách, chi tiết, hủy, kiểm tra quyền sở hữu. Ngoài phạm vi: lọc và
tìm kiếm (`SPEC.md` mục 30), phí hủy, hoàn tiền (mục 31), trang vé (T12; ở đây chỉ gắn
link).

## Database

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

Chuyển trạng thái do task này phụ trách, trong **một** transaction (`SPEC.md` mục 45.4):

```
bookings.booking_status   CONFIRMED -> CANCELLED
trip_seats.status         BOOKED    -> AVAILABLE   (mọi chỗ của booking)
```

`payment_status` giữ `PAID` vì không hoàn tiền thật. Vé giữ nguyên; trang vé (T12) hiện
nhãn "Đã hủy" theo trạng thái booking.

## Phân nhóm (D8)

| Nhóm | Điều kiện |
| --- | --- |
| Sắp đi | `CONFIRMED` và thời điểm khởi hành còn ở sau hiện tại |
| Đã đi | `CONFIRMED` và (chuyến `COMPLETED` hoặc thời điểm khởi hành đã qua) |
| Đã hủy | `CANCELLED` |

Booking `PENDING` (đang chờ thanh toán, D7) không hiện ở trang này.

## Model

Có sẵn: `Booking`, `BookingPassenger`, `Ticket`, `TripSeat`. Thêm method nghiệp vụ:

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

DTO, `record` trong `booking/model/`:

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

## Repository

`BookingRepository`:

```java
@EntityGraph(attributePaths = {"trip", "trip.train", "trip.route.originStation",
        "trip.route.destinationStation"})
List<Booking> findByCustomerIdAndBookingStatusInOrderByCreatedAtDesc(
        Long customerId, Collection<BookingStatus> statuses);

@EntityGraph(attributePaths = {"trip", "trip.train", "trip.route.originStation",
        "trip.route.destinationStation"})
Optional<Booking> findByIdAndCustomerId(Long id, Long customerId);
```

`BookingPassengerRepository`: lấy hành khách của **mọi** booking trong một query:

```java
@EntityGraph(attributePaths = {"tripSeat", "tripSeat.seat", "tripSeat.seat.coach"})
List<BookingPassenger> findByBookingIdInOrderByIdAsc(Collection<Long> bookingIds);
```

`TicketRepository`:

```java
List<Ticket> findByBookingPassengerIdIn(Collection<Long> passengerIds);
```

## Service

`booking/service/MyTripsService`, nhận `Clock`:

- `@Transactional(readOnly = true) MyTrips findFor(long customerId)`: ba query trên, gom
  hành khách và vé theo booking, chia nhóm, dựng DTO bên trong transaction.
- `@Transactional(readOnly = true) MyBooking findOne(long bookingId, long customerId)`:
  `findByIdAndCustomerId`; không thấy thì ném `BookingNotFoundException` (404).

`booking/service/BookingCancellationService` (dùng chung với T13), nhận `Clock`:

```java
@Transactional
public Booking cancelByCustomer(long bookingId, long customerId) { ... }

@Transactional
public Booking cancelByAdmin(long bookingId) { ... }
```

Cả hai cùng đi qua một method riêng: kiểm tra booking `CONFIRMED`, chuyến chưa khởi hành
(`trip.departureAt()` sau `LocalDateTime.now(clock)`), rồi `booking.cancel()` và
`releaseBooked()` cho mọi `TripSeat` của booking. Vi phạm thì ném
`BookingCannotBeCancelledException` kèm lý do. Khác nhau duy nhất: bản của khách tìm bằng
`findByIdAndCustomerId`.

## Controller

`booking/controller/MyTripsController`, `@RequestMapping("/my-trips")`:

| Method | URL | Kết quả |
| --- | --- | --- |
| GET | `/my-trips` | View `my-trips/list` với `MyTrips` |
| GET | `/my-trips/{bookingId}` | View `my-trips/detail`; booking của người khác hoặc không tồn tại: 404 |
| POST | `/my-trips/{bookingId}/cancel` | Redirect `/my-trips/{bookingId}` kèm thông báo xanh hoặc đỏ |

Luôn dùng `@AuthenticationPrincipal TrainGoUserDetails me` và `me.getId()`.

**Booking của người khác trả 404, không phải 403.** Trả 403 là cho người dò biết mã
booking đó có tồn tại.

## View

Khung khách, `app.css`:

- `templates/my-trips/list.html`: ba tab hoặc ba khối; mỗi dòng có tuyến, ngày đi, tàu,
  toa và chỗ, trạng thái booking, trạng thái thanh toán (`SPEC.md` mục 30); nút Xem.
  Nhóm rỗng hiện "Chưa có chuyến nào".
- `templates/my-trips/detail.html`: thông tin chuyến, bảng hành khách (tên, toa, chỗ, giá,
  link `/tickets/{ticketId}` của T12), tổng tiền; nút "Hủy booking" (form `POST`,
  `th:data-confirm`) chỉ hiện khi `cancellable`.

## Test bắt buộc

Theo `AGENTS.md`: hủy booking, và phân quyền theo quyền sở hữu.

- Unit test `BookingCancellationServiceTest` (Mockito, `Clock` cố định):
  - booking `CONFIRMED`, chuyến chưa đi: chuyển `CANCELLED`, mọi chỗ về `AVAILABLE`;
  - chuyến đã khởi hành: bị từ chối, không đổi gì;
  - booking đã `CANCELLED`: bị từ chối.
- `@WebMvcTest` cho `MyTripsController`: khách A mở booking của khách B nhận 404; hủy
  booking của khách B nhận 404 và service không đổi gì.

## Xong khi

- [ ] Đăng nhập `an.nguyen@example.com`: thấy 1 booking sắp đi, 1 đã hủy.
- [ ] Hủy booking sắp đi: chuyển nhóm Đã hủy; hai chỗ A01, A02 của chuyến đó trống lại
      (kiểm tra trên trang chọn chỗ của T09, hoặc bằng SQL).
- [ ] Đổi id trên URL sang booking của người khác: 404.
- [ ] `cuong.le@example.com` thấy booking ở nhóm Đã đi, không có nút hủy.
- [ ] Test bắt buộc xanh. Kiểm tra trang ở 375px. Một dòng `docs/WORKLOG.md`.
