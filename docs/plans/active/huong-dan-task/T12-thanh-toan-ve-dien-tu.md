# T12 Thanh toán mô phỏng và vé điện tử

| Wave | Lane | Cỡ | Phụ thuộc | Mở khóa | Branch |
| --- | --- | --- | --- | --- | --- |
| 3 | C | L | T11, D5, D7 | T14 (luồng xuyên suốt) | `feat/t12-payment-tickets` |

## Mục tiêu

Khách thấy mã booking và tổng tiền, bấm "Thanh toán". Hệ thống xác nhận booking trong
**một** transaction (`SPEC.md` mục 25, 26), sinh vé cho từng hành khách, rồi hiện trang
thành công. Khách xem từng vé điện tử tại `/tickets/{ticketId}` (mục 27).

Không có ngân hàng, cổng thanh toán, form thẻ, callback hay webhook.

## Phạm vi

Trong phạm vi: trang thanh toán, xác nhận booking, sinh vé, trang thành công, trang vé.

Ngoài phạm vi (làm sau MVP, `SPEC.md` mục 47): mã QR, xuất PDF vé.

## Database

```
bookings 1 ──< booking_passengers 1 ── 1 tickets
                      │
                      └── 1 trip_seats

tickets: ticket_code UNIQUE, booking_passenger_id UNIQUE, issued_at
```

Bốn thay đổi của một lần thanh toán, phải cùng thành công hoặc cùng không xảy ra:

```
bookings.payment_status   UNPAID  -> PAID      (kèm paid_at)
bookings.booking_status   PENDING -> CONFIRMED
trip_seats.status         HELD    -> BOOKED    (mọi chỗ của booking)
tickets                   tạo một vé cho mỗi hành khách
```

Database tự chặn hai trạng thái sai: `ck_bookings_confirmed_is_paid` (`CONFIRMED` mà chưa
`PAID`) và `ck_bookings_paid_at` (`PAID` mà thiếu `paid_at`). Không được tồn tại booking
`CONFIRMED` mà chỗ vẫn `AVAILABLE`, hay chỗ `BOOKED` mà booking chưa xác nhận.

## Model

Thêm method nghiệp vụ:

```java
// Booking
public void confirmPayment(LocalDateTime paidAt) {
    if (bookingStatus != BookingStatus.PENDING) {
        throw new IllegalStateException("Only a pending booking can be paid");
    }
    this.paymentStatus = PaymentStatus.PAID;
    this.paidAt = paidAt;
    this.bookingStatus = BookingStatus.CONFIRMED;
}

/** The hold ran out before payment. */
public void expireUnpaid() {
    if (bookingStatus != BookingStatus.PENDING) {
        throw new IllegalStateException("Only a pending booking can expire");
    }
    this.bookingStatus = BookingStatus.CANCELLED;
}

// TripSeat
public void book() {
    if (status != TripSeatStatus.HELD) {
        throw new IllegalStateException("Seat is not held");
    }
    status = TripSeatStatus.BOOKED;
    heldBy = null;
    heldUntil = null;
}
```

Trang vé dùng DTO:

```java
record TicketView(Long ticketId, String ticketCode, String bookingCode,
        BookingStatus bookingStatus, String passengerName, String routeLabel,
        String trainCode, LocalDate departureDate, LocalTime departureTime,
        int coachNumber, String seatCode, SeatType seatType) { }
```

## Repository

`BookingRepository`:

```java
@EntityGraph(attributePaths = {"trip", "trip.train", "trip.route.originStation",
        "trip.route.destinationStation"})
Optional<Booking> findByIdAndCustomerId(Long id, Long customerId);   // có từ T10
```

`BookingPassengerRepository`:

```java
@EntityGraph(attributePaths = {"tripSeat", "tripSeat.seat", "tripSeat.seat.coach",
        "tripSeat.heldBy"})
List<BookingPassenger> findByBookingIdOrderByIdAsc(Long bookingId);
```

`TicketRepository`:

```java
@EntityGraph(attributePaths = {"bookingPassenger", "bookingPassenger.booking",
        "bookingPassenger.booking.customer", "bookingPassenger.booking.trip",
        "bookingPassenger.booking.trip.train",
        "bookingPassenger.booking.trip.route.originStation",
        "bookingPassenger.booking.trip.route.destinationStation",
        "bookingPassenger.tripSeat.seat.coach"})
Optional<Ticket> findWithDetailsById(Long id);

List<Ticket> findByBookingPassengerBookingIdOrderByIdAsc(Long bookingId);
```

## Service

`booking/service/PaymentService`, nhận `Clock`:

```java
@Transactional
public Booking payAndConfirm(long bookingId, long customerId) { ... }
```

**Kiểm tra hết rồi mới đổi.** Mọi điều kiện được kiểm tra trước khi đổi bất kỳ dòng nào,
nên khi một điều kiện sai thì không có gì bị đổi dở:

1. `findByIdAndCustomerId`; không thấy thì `BookingNotFoundException` (404).
2. Booking phải `PENDING` / `UNPAID`.
3. Chuyến phải `SCHEDULED` và chưa khởi hành.
4. Mọi chỗ của booking phải `HELD`, `heldBy` là người này, `heldUntil` sau hiện tại. Sai
   một chỗ thì ném `HoldExpiredException`.
5. Đổi: `tripSeat.book()` cho từng chỗ, `booking.confirmPayment(now)`, rồi tạo và lưu
   `Ticket` cho từng hành khách.

Mã vé (D5): mã booking đổi `BK` thành `TK`, thêm `-` và số thứ tự hành khách đệm 2 chữ
số, ví dụ `TK202610005-01`, giống seed.

Khi giữ chỗ đã hết hạn: controller gọi thêm một method `@Transactional expire(bookingId,
customerId)`: `booking.expireUnpaid()`, rồi trả về `AVAILABLE` những chỗ **vẫn đang được
chính người này giữ** bằng `TripSeatRepository.releaseHeldBy(...)` của T09. Chỗ đã bị
người khác giữ đè thì không đụng tới. Đây là transaction riêng, chạy sau khi transaction
thanh toán đã rollback.

`ticket/service/TicketService`:

- `@Transactional(readOnly = true) TicketView findForCustomer(long ticketId, long customerId)`:
  vé của booking người khác thì ném `TicketNotFoundException` (404).

## Controller

Thêm vào `booking/controller/BookingFlowController` (của T11):

| Method | URL | Kết quả |
| --- | --- | --- |
| GET | `/booking/payment` | View `booking/payment`: mã booking, tổng tiền, đồng hồ đếm ngược |
| POST | `/booking/payment` | Thành công: xóa draft, `redirect:/booking/success?code=BK...`. Hết hạn: `expire(...)`, redirect về chi tiết chuyến kèm thông báo |
| GET | `/booking/success?code=` | View `booking/success`: chỉ hiện booking của chính người đó, kèm link từng vé |

`ticket/controller/TicketController`:

| Method | URL | Kết quả |
| --- | --- | --- |
| GET | `/tickets/{ticketId}` | View `tickets/detail`; vé của người khác: 404 |

## View

Khung khách:

- `templates/booking/payment.html`: stepper ở "Thanh toán"; mã booking, tổng tiền
  (`350.000 VND`), dòng "Đây là thanh toán mô phỏng, không trừ tiền thật"; đồng hồ đếm
  ngược (`hold-countdown.js`); nút "Thanh toán" (form `POST`). Không có ô nhập thẻ.
- `templates/booking/success.html`: stepper ở "Vé"; "Đặt vé thành công", mã booking, danh
  sách vé (tên hành khách, toa, chỗ, link xem vé), link "Chuyến của tôi".
- `templates/tickets/detail.html`: bố cục như ví dụ ở `SPEC.md` mục 27: mã vé, mã booking,
  ga đi → ga đến, tàu, ngày giờ khởi hành, toa, chỗ (ghế hoặc giường), hành khách. Booking
  đã hủy thì hiện nhãn "Đã hủy". Thêm CSS `@media print` để in vé gọn một trang.

Vé **không** hiện số CCCD đầy đủ. Nếu muốn hiện, che bớt (`079*****1234`). QR, nếu làm
sau này, chỉ chứa `ticketCode` (`docs/product/booking-flow.md`).

## Test bắt buộc

Theo `AGENTS.md`: xác nhận booking trong transaction.

- Unit test `PaymentServiceTest` (Mockito, `Clock` cố định):
  - đủ điều kiện: booking `CONFIRMED` / `PAID` có `paidAt`, mọi chỗ `BOOKED`, số vé bằng
    số hành khách;
  - một chỗ đã hết hạn giữ: ném `HoldExpiredException`, không chỗ nào chuyển `BOOKED`,
    booking vẫn `PENDING`, không lưu vé nào;
  - chuyến bị hủy trong lúc chờ: bị từ chối, không đổi gì;
  - booking của người khác: 404.
- `@WebMvcTest` cho `TicketController`: khách A mở vé của khách B nhận 404.

T14 thêm một test `@SpringBootTest` chạy thật cả luồng, để chứng minh rollback trên
database chứ không chỉ trên mock.

## Xong khi

- [ ] Chạy trọn: tìm chuyến → chọn 2 chỗ → nhập 2 hành khách → xem lại → thanh toán →
      trang thành công có 2 vé → mở từng vé.
- [ ] Database: booking `CONFIRMED` / `PAID` có `paid_at`, 2 chỗ `BOOKED` không còn
      `held_by_user_id`, 2 dòng `tickets`.
- [ ] Booking mới hiện trong `/my-trips` (T10) ở nhóm Sắp đi.
- [ ] Để quá 5 phút ở trang thanh toán rồi bấm: báo hết hạn, booking thành `CANCELLED` /
      `UNPAID`, chỗ trống lại.
- [ ] Trang vé in ra gọn một trang A4. Test bắt buộc xanh. Một dòng `docs/WORKLOG.md`.
