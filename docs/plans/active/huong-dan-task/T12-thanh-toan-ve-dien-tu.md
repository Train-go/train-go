# T12 Thanh toán mô phỏng và vé điện tử

> **Tóm tắt:** sau task này, khách bấm "Thanh toán" là booking được xác nhận, mỗi hành
> khách có một vé điện tử xem được tại `/tickets/{ticketId}`. Không có tiền thật nào bị trừ.

| Đợt | Phần việc | Độ khó | Cần xong trước | Task đang chờ task này | Tên branch |
| --- | --- | --- | --- | --- | --- |
| 3 | C | Khó (khoảng 4-5 ngày) | T11, quyết định D5, D7 | T14 | `feat/t12-payment-tickets` |

Các từ như transaction, rollback, `@EntityGraph` được giải thích trong `README.md` cùng
thư mục.

## 1. Task này làm gì

Khách thấy mã booking và tổng tiền, rồi bấm "Thanh toán". Hệ thống xác nhận booking trong
**một** transaction (`SPEC.md` mục 25, 26), sinh vé cho từng hành khách, rồi hiện trang
đặt vé thành công. Khách xem từng vé điện tử tại `/tickets/{ticketId}` (`SPEC.md` mục 27).

Đây là thanh toán **mô phỏng**: không có ngân hàng, cổng thanh toán, form nhập thẻ,
callback hay webhook. Bấm nút là coi như đã trả tiền.

## 2. Phạm vi

Làm:

- Trang thanh toán.
- Xác nhận booking và sinh vé.
- Trang đặt vé thành công.
- Trang vé điện tử.

Không làm (để sau MVP, `SPEC.md` mục 47):

- Mã QR trên vé.
- Xuất vé ra PDF.

## 3. Dữ liệu dùng tới

```
bookings 1 ──< booking_passengers 1 ── 1 tickets
                      │
                      └── 1 trip_seats

tickets: ticket_code UNIQUE, booking_passenger_id UNIQUE, issued_at
```

Đọc sơ đồ: một booking có nhiều hành khách. Mỗi hành khách ngồi đúng một chỗ
(`trip_seats`) và có đúng một vé (`tickets`). Ký hiệu `1 ── 1` nghĩa là "đúng một với đúng
một". Mã vé không được trùng, và mỗi hành khách chỉ có một vé.

Một lần thanh toán gồm bốn thay đổi. Bốn thay đổi này phải **cùng thành công hoặc cùng
không xảy ra**:

```
bookings.payment_status   UNPAID  -> PAID      (kèm paid_at)
bookings.booking_status   PENDING -> CONFIRMED
trip_seats.status         HELD    -> BOOKED    (mọi chỗ của booking)
tickets                   tạo một vé cho mỗi hành khách
```

Vì sao phải "cùng": giả sử server lỗi ngay sau khi đổi booking sang `CONFIRMED` nhưng chưa
kịp đổi chỗ sang `BOOKED`. Khách tưởng đã có vé, trong khi chỗ đó vẫn bán được cho người
khác. Transaction đảm bảo trường hợp đó không bao giờ xảy ra.

Database cũng tự chặn hai trạng thái sai: `ck_bookings_confirmed_is_paid` (booking
`CONFIRMED` mà chưa `PAID`) và `ck_bookings_paid_at` (`PAID` mà thiếu `paid_at`). Ngoài ra
không được tồn tại booking `CONFIRMED` mà chỗ vẫn `AVAILABLE`, hay chỗ `BOOKED` mà booking
chưa được xác nhận.

## 4. Các bước làm

### Bước 1. Model: thêm method nghiệp vụ và tạo DTO

Các entity trong `entity/` đã có, **không thêm hay sửa field**. Bạn thêm ba method để đổi
trạng thái. Mỗi method tự kiểm tra trạng thái hiện tại trước khi đổi:

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

- `confirmPayment`: booking đã trả tiền và được xác nhận.
- `expireUnpaid`: hết giờ giữ chỗ trước khi trả tiền, booking bị hủy.
- `book`: chỗ chuyển từ "đang giữ" sang "đã đặt", xóa thông tin người giữ.

Tạo DTO cho trang vé, `model/response/TicketView`:

```java
record TicketView(Long ticketId, String ticketCode, String bookingCode,
        BookingStatus bookingStatus, String passengerName, String routeLabel,
        String trainCode, LocalDate departureDate, LocalTime departureTime,
        int coachNumber, String seatCode, SeatType seatType) { }
```

### Bước 2. Repository: thêm câu truy vấn

`repository/BookingRepository` dùng lại method T10 đã thêm. Method này chỉ tìm thấy booking
khi nó thuộc đúng khách đó:

```java
@EntityGraph(attributePaths = {"trip", "trip.train", "trip.route.originStation",
        "trip.route.destinationStation"})
Optional<Booking> findByIdAndCustomerId(Long id, Long customerId);   // có từ T10
```

Thêm vào `repository/BookingPassengerRepository`. Method này đọc mọi hành khách của booking
cùng chỗ, ghế, toa và người giữ chỗ trong một lần:

```java
@EntityGraph(attributePaths = {"tripSeat", "tripSeat.seat", "tripSeat.seat.coach",
        "tripSeat.heldBy"})
List<BookingPassenger> findByBookingIdOrderByIdAsc(Long bookingId);
```

Thêm vào `repository/TicketRepository`. Method đầu đọc một vé cùng mọi thông tin cần in lên
vé; method sau lấy các vé của một booking:

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

### Bước 3. Service: xử lý nghiệp vụ

Tạo `service/PaymentService`. "Bây giờ" lấy bằng `LocalDateTime.now()`:

```java
@Transactional
public Booking payAndConfirm(long bookingId, long customerId) { ... }
```

**Kiểm tra hết rồi mới đổi.** Method kiểm tra mọi điều kiện trước, rồi mới đổi bất kỳ dòng
nào. Nhờ vậy khi một điều kiện sai, không có gì bị đổi dở dang:

1. Gọi `findByIdAndCustomerId`; không thấy thì ném `BookingNotFoundException` (trang 404).
2. Booking phải đang `PENDING` / `UNPAID`.
3. Chuyến phải `SCHEDULED` và chưa khởi hành.
4. Mọi chỗ của booking phải đang `HELD`, `heldBy` đúng là người này, và `heldUntil` còn ở
   sau hiện tại. Sai một chỗ thì ném `HoldExpiredException`.
5. Đến đây mới đổi: gọi `tripSeat.book()` cho từng chỗ, `booking.confirmPayment(now)`, rồi
   tạo và lưu một `Ticket` cho từng hành khách.

Ví dụ tình huống hết giờ: khách giữ chỗ lúc 10:00, giữ được 5 phút, nhưng tới 10:06 mới bấm
"Thanh toán". Ở bước 4, `heldUntil` (10:05) đã qua, nên method ném `HoldExpiredException`
trước khi đổi gì. Kết quả: không chỗ nào thành `BOOKED`, không vé nào được tạo, booking vẫn
`PENDING`.

Mã vé (quyết định D5): lấy mã booking, đổi `BK` thành `TK`, thêm `-` và số thứ tự hành
khách đệm đủ 2 chữ số. Ví dụ `TK202610005-01`, giống dữ liệu mẫu (seed).

Khi giữ chỗ đã hết hạn như ví dụ trên, controller gọi thêm một method
`@Transactional expire(bookingId, customerId)`. Method này gọi `booking.expireUnpaid()`,
rồi trả về `AVAILABLE` những chỗ **vẫn đang được chính người này giữ**, bằng
`TripSeatRepository.releaseHeldBy(...)` của T09. Chỗ đã bị người khác giữ đè thì không đụng
tới. Đây là một transaction riêng, chạy sau khi transaction thanh toán đã rollback.

Tạo `service/TicketService`:

- `@Transactional(readOnly = true) TicketView findForCustomer(long ticketId, long customerId)`:
  nếu vé thuộc booking của người khác thì ném `TicketNotFoundException` (trang 404).

`TicketNotFoundException` đặt trong `common/exception/`, cạnh các exception của T10, T11.

### Bước 4. Controller: nhận request

Thêm vào `controller/BookingFlowController` (T11 đã tạo controller này):

| Method | Đường dẫn | Kết quả |
| --- | --- | --- |
| GET | `/booking/payment` | Trang `booking/payment`: mã booking, tổng tiền, đồng hồ đếm ngược |
| POST | `/booking/payment` | Thành công: xóa bản nháp, chuyển sang `redirect:/booking/success?code=BK...`. Hết hạn: gọi `expire(...)`, chuyển về chi tiết chuyến kèm thông báo |
| GET | `/booking/success?code=` | Trang `booking/success`: chỉ hiện booking của chính người đó, kèm link từng vé |

Tạo `controller/TicketController`:

| Method | Đường dẫn | Kết quả |
| --- | --- | --- |
| GET | `/tickets/{ticketId}` | Trang `tickets/detail`; vé của người khác: 404 |

### Bước 5. Giao diện

Dùng khung trang cho khách:

- `templates/booking/payment.html`: thanh các bước (stepper) ở "Thanh toán"; mã booking,
  tổng tiền (`350.000 VND`), dòng "Đây là thanh toán mô phỏng, không trừ tiền thật"; đồng
  hồ đếm ngược (`hold-countdown.js`); nút "Thanh toán" là form `POST`. Không có ô nhập thẻ.
- `templates/booking/success.html`: stepper ở "Vé"; dòng "Đặt vé thành công", mã booking,
  danh sách vé (tên hành khách, toa, chỗ, link xem vé), link "Chuyến của tôi".
- `templates/tickets/detail.html`: bố cục theo ví dụ ở `SPEC.md` mục 27: mã vé, mã booking,
  ga đi → ga đến, tàu, ngày giờ khởi hành, toa, chỗ (ghế hoặc giường), hành khách. Booking
  đã hủy thì hiện nhãn "Đã hủy". Thêm CSS `@media print` để in vé gọn trong một trang.

Vé **không** hiện số CCCD đầy đủ. Nếu muốn hiện, che bớt (`079*****1234`). Vì sao: vé có
thể bị người khác nhìn thấy hoặc in ra. Nếu sau này làm mã QR, mã chỉ chứa `ticketCode`
(`docs/product/booking-flow.md`).

## 5. Luật và lỗi phải xử lý

- Bốn thay đổi của một lần thanh toán cùng xảy ra hoặc cùng không xảy ra.
- Kiểm tra hết điều kiện rồi mới đổi dữ liệu.
- Hết giờ giữ chỗ: báo hết hạn, booking thành `CANCELLED` / `UNPAID`, chỗ trống lại.
- Chuyến bị hủy trong lúc khách đang chờ: từ chối thanh toán, không đổi gì.
- Booking hoặc vé của người khác: trả 404.
- Vé không hiện số CCCD đầy đủ.

## 6. Kịch bản thử bằng tay

`AGENTS.md` coi việc xác nhận booking trong transaction là nghiệp vụ bắt buộc phải thử kỹ.
Chạy ứng dụng bằng `.\mvnw.cmd spring-boot:run`, đăng nhập một tài khoản khách (ví dụ
`an.nguyen@example.com`, mật khẩu `Customer@123`). Khi cần xem hay sửa database, mở MySQL
bằng lệnh:

```
docker exec -it traingo-mysql mysql -utraingo -ptraingo --default-character-set=utf8mb4 traingo
```

Câu SQL dùng lại nhiều lần trong mục này, xem trạng thái một booking cùng chỗ và vé:

```sql
SELECT b.booking_code, b.booking_status, b.payment_status, b.paid_at,
       ts.id AS trip_seat_id, ts.status, ts.held_by_user_id, t.ticket_code
FROM bookings b
JOIN booking_passengers bp ON bp.booking_id = b.id
JOIN trip_seats ts ON ts.id = bp.trip_seat_id
LEFT JOIN tickets t ON t.booking_passenger_id = bp.id
WHERE b.booking_code = '<mã booking>';
```

**Kịch bản 1. Thanh toán thành công.**

1. Đi trọn luồng: tìm chuyến, chọn 2 chỗ, nhập 2 hành khách, xem lại, bấm "Tiếp tục thanh
   toán". Ghi lại mã booking trên trang thanh toán.
2. Chạy câu SQL trên. Kết quả trước khi trả tiền: `PENDING`, `UNPAID`, `paid_at` rỗng, 2 chỗ
   `HELD`, chưa có vé.
3. Bấm "Thanh toán". Trang thành công hiện mã booking và 2 vé.
4. Chạy lại câu SQL. Kết quả đúng: `CONFIRMED`, `PAID`, `paid_at` có giá trị, 2 chỗ `BOOKED`
   với `held_by_user_id` rỗng, 2 mã vé dạng `TK<năm tháng><số>-01` và `-02`.
5. Mở từng vé: đủ thông tin như `SPEC.md` mục 27, không hiện đủ số CCCD.
6. Mở `/my-trips` (T10): booking mới nằm ở nhóm Sắp đi.
7. Bấm in trang vé (Ctrl+P): vé gọn trong một trang A4.

**Kịch bản 2. Hết thời gian giữ chỗ ở trang thanh toán.**

Không cần đợi 5 phút: bạn đẩy hạn giữ chỗ về quá khứ bằng SQL.

1. Đi tới trang thanh toán của một booking mới, ghi lại mã booking. Chưa bấm "Thanh toán".
2. Tìm id của bạn và đẩy hạn giữ chỗ về quá khứ:

   ```sql
   SELECT id FROM users WHERE email = 'an.nguyen@example.com';
   UPDATE trip_seats SET held_until = NOW() - INTERVAL 1 MINUTE
   WHERE held_by_user_id = <id của bạn> AND status = 'HELD';
   ```

3. Bấm "Thanh toán".
4. Kết quả đúng: quay về chi tiết chuyến kèm thông báo hết hạn. Câu SQL chung cho thấy
   `CANCELLED`, `UNPAID`, các chỗ `AVAILABLE` với `held_by_user_id` rỗng, không có vé nào.

**Kịch bản 3. Chỗ đã bị người khác giữ đè.**

1. Lặp lại kịch bản 2 tới hết bước 2.
2. Giả lập một khách khác đã giữ đè một trong hai chỗ:

   ```sql
   SELECT id FROM users WHERE email = 'binh.tran@example.com';
   UPDATE trip_seats SET held_by_user_id = <id của binh.tran>,
          held_until = NOW() + INTERVAL 5 MINUTE
   WHERE id = <một trip_seat_id của booking>;
   ```

3. Bấm "Thanh toán".
4. Kết quả đúng: booking `CANCELLED`, chỗ còn lại về `AVAILABLE`, còn chỗ của `binh.tran`
   vẫn `HELD` với đúng id của người đó.

**Kịch bản 4. Chuyến bị hủy trong lúc khách đang chờ.**

1. Đi tới trang thanh toán của một booking mới, ghi lại mã booking và id chuyến.
2. Hủy chuyến bằng SQL:

   ```sql
   UPDATE trips SET status = 'CANCELLED' WHERE id = <id chuyến>;
   ```

3. Bấm "Thanh toán".
4. Kết quả đúng: báo lỗi, không có chỗ nào `BOOKED`, không có vé nào. Xong thì trả chuyến
   về `SCHEDULED` để dùng tiếp.

**Kịch bản 5. Vé và booking của người khác.**

1. Tìm id một vé không phải của bạn:

   ```sql
   SELECT t.id, t.ticket_code, u.email
   FROM tickets t
   JOIN booking_passengers bp ON bp.id = t.booking_passenger_id
   JOIN bookings b ON b.id = bp.booking_id
   JOIN users u ON u.id = b.customer_id
   ORDER BY t.id;
   ```

2. Đăng nhập `an.nguyen@example.com`, mở `/tickets/<id vé của binh.tran>`. Kết quả đúng:
   404.
3. Mở `/booking/success?code=<mã booking của binh.tran>`. Kết quả đúng: không hiện booking
   đó.
4. Mở `/tickets/999999` (id không tồn tại): 404.

Muốn chạy lại từ đầu với dữ liệu sạch, làm mới database theo `docs/RUNBOOK.md`.

## 7. Lỗi hay gặp

- Đổi trạng thái xen kẽ với kiểm tra (kiểm một chỗ, đổi một chỗ): gặp chỗ hết hạn ở giữa
  chừng thì các chỗ trước đã bị đổi. Luôn kiểm tra hết rồi mới đổi.
- Gọi `expire(...)` bên trong transaction thanh toán đang lỗi: transaction đó sẽ rollback
  luôn cả việc hủy booking. `expire` phải là transaction riêng, chạy sau.
- Trả về `AVAILABLE` mọi chỗ của booking khi hết hạn: chỗ đã bị người khác giữ đè cũng bị
  trả, làm mất chỗ của họ. Chỉ trả những chỗ vẫn do chính người này giữ.
- In đầy đủ số CCCD lên vé.

## 8. Kiểm tra trước khi báo xong

- [ ] Chạy trọn: tìm chuyến → chọn 2 chỗ → nhập 2 hành khách → xem lại → thanh toán →
      trang thành công có 2 vé → mở từng vé.
- [ ] Database: booking `CONFIRMED` / `PAID` có `paid_at`, 2 chỗ `BOOKED` không còn
      `held_by_user_id`, có 2 dòng `tickets`.
- [ ] Booking mới hiện trong `/my-trips` (T10) ở nhóm Sắp đi.
- [ ] Hạn giữ chỗ đã qua (đẩy bằng SQL) rồi bấm "Thanh toán": báo hết hạn, booking thành
      `CANCELLED` / `UNPAID`, chỗ trống lại.
- [ ] Đã đi hết các kịch bản ở mục 6.
- [ ] Trang vé in ra gọn trong một trang A4.
- [ ] `.\mvnw.cmd compile` chạy không lỗi và ứng dụng khởi động được
      (`.\mvnw.cmd spring-boot:run`).
- [ ] Có một dòng trong `docs/WORKLOG.md`, nằm trong cùng commit với code.
