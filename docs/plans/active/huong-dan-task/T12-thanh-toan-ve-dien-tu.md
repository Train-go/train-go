# T12 Thanh toán mô phỏng và vé điện tử

> **Tóm tắt:** sau task này, khách bấm "Thanh toán" là hệ thống khóa chỗ, tạo booking đã
> xác nhận và vé cho từng hành khách trong **một** transaction. Mỗi vé xem được tại
> `/tickets/{ticketId}`. Không có tiền thật nào bị trừ.

| Đợt | Phần việc | Độ khó | Cần xong trước | Task đang chờ task này | Tên branch |
| --- | --- | --- | --- | --- | --- |
| 3 | C | Khó (khoảng 5 ngày) | T11, quyết định D5, D7 | T14 | `feat/t12-payment-tickets` |

Các từ như transaction, rollback, `@EntityGraph` được giải thích trong `README.md` cùng
thư mục.

## 1. Task này làm gì

Đây là nơi duy nhất trong luồng đặt vé ghi vào database. Từ T09 tới T11, mọi thứ khách chọn
chỉ nằm trong bản nháp (`BookingDraft`) trong session. Khách thấy tổng tiền trên trang thanh
toán rồi bấm "Thanh toán". Hệ thống chạy **một** transaction (`SPEC.md` mục 25, 26,
`docs/decisions/0010-bo-giu-cho.md`):

1. Kiểm tra chuyến còn `SCHEDULED` và chưa khởi hành.
2. Khóa các chỗ đã chọn bằng một câu `UPDATE` có điều kiện (`AVAILABLE` sang `BOOKED`). Nếu
   thiếu chỗ nào, nghĩa là người khác vừa mua chỗ đó: rollback toàn bộ.
3. Tạo booking `CONFIRMED` / `PAID` và các dòng `tickets` (vé) kèm mã vé.

Rồi hiện trang đặt vé thành công. Khách xem từng vé điện tử tại `/tickets/{ticketId}`
(`SPEC.md` mục 27).

Đây là thanh toán **mô phỏng**: không có ngân hàng, cổng thanh toán, form nhập thẻ,
callback hay webhook. Bấm nút là coi như đã trả tiền.

Người bấm chậm hơn khi hai người cùng chọn một chỗ sẽ thấy "Chỗ vừa được người khác đặt" và
được đưa về trang chọn chỗ (T09) với sơ đồ mới. Bản nháp còn nguyên trong session nên không
phải nhập lại hành khách. Không có hạn giữ chỗ, không có đếm ngược.

## 2. Phạm vi

Làm:

- Trang thanh toán (tóm tắt từ bản nháp).
- Transaction thanh toán: khóa chỗ, tạo booking, tạo vé.
- Sinh mã booking và mã vé (quyết định D5).
- Trang đặt vé thành công.
- Trang vé điện tử.

Không làm (để sau MVP, `SPEC.md` mục 47):

- Mã QR trên vé.
- Xuất vé ra PDF.

## 3. Dữ liệu dùng tới

```
bookings 1 ──< tickets >── 1 trip_seats

tickets: ticket_code UNIQUE NOT NULL, issued_at NOT NULL
```

Đọc sơ đồ: một booking có nhiều dòng `tickets`. Mỗi dòng là một hành khách ngồi đúng một
chỗ (`trip_seats`) và là vé của họ (xem `docs/decisions/0008-gop-ve-vao-hanh-khach.md`). Từ
ADR 0010, dòng `tickets` chỉ được tạo cùng booking đã thanh toán, nên `ticket_code` và
`issued_at` luôn có giá trị. Mã vé không được trùng.

Một lần thanh toán gồm ba thay đổi. Ba thay đổi này phải **cùng thành công hoặc cùng
không xảy ra**:

```
trip_seats.status   AVAILABLE -> BOOKED   (mọi chỗ đã chọn, bằng UPDATE có điều kiện)
bookings            tạo mới: CONFIRMED / PAID, paid_at, total_price
tickets             tạo mới cho mỗi hành khách, kèm ticket_code, issued_at, ticket_price
```

Vì sao phải "cùng": giả sử server lỗi ngay sau khi tạo booking `CONFIRMED` nhưng chưa kịp
đổi chỗ sang `BOOKED`. Khách tưởng đã có vé, trong khi chỗ đó vẫn bán được cho người khác.
Transaction đảm bảo trường hợp đó không bao giờ xảy ra.

Database cũng tự chặn hai trạng thái sai: `ck_bookings_confirmed_is_paid` (booking
`CONFIRMED` mà chưa `PAID`) và `ck_bookings_paid_at` (`PAID` mà thiếu `paid_at`). Ngoài ra
không được tồn tại booking `CONFIRMED` mà chỗ vẫn `AVAILABLE`, hay chỗ `BOOKED` mà không có
vé nào trỏ tới.

Booking `PENDING` và `UNPAID` vẫn còn trong các ràng buộc `CHECK` của database nhưng không
còn luồng nào tạo ra chúng; đừng viết code dựa vào hai trạng thái này.

## 4. Các bước làm

### Bước 1. Model: constructor tạo booking đã thanh toán và DTO

Các entity trong `entity/` đã có, **không thêm hay sửa field**. Bạn chỉ thêm cách tạo đúng
trạng thái:

- `Booking`: một constructor hoặc phương thức tĩnh tạo booking với `CONFIRMED` / `PAID` và
  `paidAt` do service truyền vào, ví dụ `Booking.paid(bookingCode, customer, trip,
  totalPrice, paidAt)`. Booking không còn được tạo ở trạng thái `PENDING` / `UNPAID` ở đâu
  cả.
- `Ticket`: dùng constructor có sẵn, nhận mã vé và tự đặt `issuedAt` bằng giờ hiện tại:

  ```java
  new Ticket(booking, tripSeat, fullName, dateOfBirth, identityNumber,
          ticketPrice, ticketCode)
  ```

  Không có method `Ticket.issue(...)`; vé không bao giờ tồn tại ở trạng thái chưa phát hành.

Tạo DTO cho trang vé, `model/response/TicketView`:

```java
record TicketView(Long ticketId, String ticketCode, String bookingCode,
        BookingStatus bookingStatus, String passengerName, String routeLabel,
        String trainCode, LocalDateTime departureAt,
        int coachNumber, String seatCode, CoachType coachType) { }
```

### Bước 2. Repository: thêm câu truy vấn

Thêm vào `repository/TripSeatRepository` câu khóa chỗ. Đây là trái tim của task, và là chỗ
duy nhất ngăn hai người cùng mua một ghế:

```java
/** Books the seats that are still free. Returns how many rows it changed. */
@Modifying(flushAutomatically = true, clearAutomatically = true)
@Query("""
    update TripSeat ts
    set ts.status = :booked
    where ts.id in :ids and ts.trip.id = :tripId
      and ts.status = :available""")
int bookIfAvailable(Collection<Long> ids, Long tripId,
        TripSeatStatus booked, TripSeatStatus available);
```

**Vì sao khóa chỗ bằng một câu `UPDATE` có điều kiện, thay vì "đọc rồi ghi".** Thử hình
dung cách làm "đọc rồi ghi": đọc trạng thái ghế A05, thấy `AVAILABLE`, kiểm tra trong Java,
rồi mới ghi `BOOKED`. Nếu khách A và khách B bấm "Thanh toán" gần như cùng lúc, cả hai cùng
đọc thấy A05 trống, và cả hai cùng ghi `BOOKED`. Kết quả: hai người mua cùng một ghế.

Với một câu `UPDATE ... WHERE status = AVAILABLE`, việc kiểm tra và việc ghi diễn ra một
lần, không bị chen ngang. MySQL khóa từng dòng đang sửa: transaction của B phải đợi
transaction của A commit xong, rồi mới đọc lại A05, thấy đã là `BOOKED`, nên không đổi dòng
đó. Số dòng trả về nhỏ hơn số chỗ yêu cầu là biết có chỗ đã bị người khác lấy mất.

`repository/BookingRepository` dùng lại method T10 đã thêm, chỉ tìm thấy booking khi nó
thuộc đúng khách đó, và thêm method sinh mã (quyết định D5):

```java
@EntityGraph(attributePaths = {"trip", "trip.train", "trip.route.originStation",
        "trip.route.destinationStation"})
Optional<Booking> findByIdAndCustomerId(Long id, Long customerId);   // có từ T10

Optional<Booking> findTopByBookingCodeStartingWithOrderByBookingCodeDesc(String prefix);
```

Thêm vào `repository/TicketRepository`. Method đầu đọc mọi hành khách của booking cùng chỗ
(đã kèm số toa, mã chỗ) trong một lần:

```java
@EntityGraph(attributePaths = {"tripSeat"})
List<Ticket> findByBookingIdOrderByIdAsc(Long bookingId);
```

Cũng trong `TicketRepository`: method sau đọc một vé cùng mọi thông tin cần in lên vé:

```java
@EntityGraph(attributePaths = {"booking", "booking.customer", "booking.trip",
        "booking.trip.train", "booking.trip.route.originStation",
        "booking.trip.route.destinationStation", "tripSeat"})
Optional<Ticket> findWithDetailsById(Long id);
```

`TripSeatRepository.findByIdIn` (T11) dùng lại để đọc chỗ và tính giá.

### Bước 3. Service: xử lý nghiệp vụ

Tạo `service/BookingCodeGenerator` (quyết định D5). Tháng hiện tại lấy từ `LocalDate.now()`:

- `String next()`: tiền tố là `"BK" + yyyyMM` của tháng hiện tại. Lấy mã lớn nhất có tiền
  tố đó, cộng thêm 1 vào số thứ tự, đệm cho đủ 3 chữ số. Ví dụ dữ liệu mẫu chạy trong tháng
  10/2026 có `BK202610001` tới `BK202610008`, thì mã tiếp theo là `BK202610009`.
- Mã vé: đổi `BK` thành `TK` của mã booking, thêm `-` và số thứ tự hành khách đệm đủ 2 chữ
  số. Ví dụ `TK202610009-01`, giống dữ liệu mẫu (seed).

Tạo `service/PaymentService`. "Bây giờ" lấy bằng `LocalDateTime.now()`:

```java
@Transactional
public Booking pay(BookingDraft draft, long customerId) { ... }
```

Toàn bộ method là **một** transaction. Thứ tự các bước quan trọng:

1. Đọc chuyến. Không `SCHEDULED` hoặc đã khởi hành (`trip.getDepartureAt()` không còn ở sau
   hiện tại) thì ném `TripNotBookableException` (của T09).
2. Kiểm tra bản nháp đủ: có hành khách, số hành khách bằng số chỗ. Thiếu thì ném
   `InvalidPassengersException` (của T11).
3. Gọi `changed = bookIfAvailable(ids, tripId, BOOKED, AVAILABLE)`. Nếu
   `changed != ids.size()` thì ném `SeatsUnavailableException` (của T09). Exception làm
   transaction rollback, kể cả những chỗ đã đổi được trong câu `UPDATE` này: không chỗ nào bị
   khóa dở dang.
4. Đọc các `TripSeat` bằng `findByIdIn`, tính `ticketPrice` từng chỗ
   (`trip.getBasePrice()` + `priceModifier`) và `totalPrice`. Giá lấy từ database, không từ
   bản nháp hay form.
5. Sinh mã booking, tạo booking `CONFIRMED` / `PAID` với `paidAt = now` và lưu. Rồi tạo từng
   `Ticket` theo thứ tự trong bản nháp (hành khách thứ i ngồi chỗ thứ i), kèm mã vé theo D5.
6. Trả booking.

Ví dụ khi hai người tranh một chỗ: khách A và khách B đều chọn ghế A05 và đã tới trang thanh
toán. A bấm trước: `bookIfAvailable` đổi A05 thành `BOOKED`, trả 1, A có booking. B bấm sau:
câu `UPDATE` của B không tìm thấy dòng nào còn `AVAILABLE`, trả 0, khác số chỗ B chọn (1), nên
method ném `SeatsUnavailableException` trước khi tạo bất cứ booking hay vé nào. Nếu B chọn
2 chỗ mà chỉ 1 chỗ bị mất, câu `UPDATE` đã đổi chỗ còn lại sang `BOOKED`, nhưng exception làm
transaction rollback nên chỗ đó trở lại `AVAILABLE`.

Hai người tạo booking cùng một giây có thể nhận cùng một mã. Khi đó khóa unique
`uk_bookings_code` trong database chặn người thứ hai và rollback cả transaction (chỗ cũng trả
lại). Controller bắt `DataIntegrityViolationException` và gọi `pay` lại một lần.

Tạo `service/TicketService`:

- `@Transactional(readOnly = true) TicketView findForCustomer(long ticketId, long customerId)`:
  nếu vé thuộc booking của người khác thì ném `TicketNotFoundException` (trang 404).

`TicketNotFoundException` đặt trong `common/exception/`, cạnh các exception của T10, T11.

### Bước 4. Controller: nhận request

Thêm vào `controller/BookingFlowController` (T11 đã tạo controller này):

| Method | Đường dẫn | Kết quả |
| --- | --- | --- |
| GET | `/booking/payment` | Trang `booking/payment`: tóm tắt từ bản nháp (dùng lại `BookingDraftService.review`), tổng tiền. Chưa có mã booking vì booking chưa tạo. Không có bản nháp hoặc chưa nhập hành khách: về `/` kèm thông báo |
| POST | `/booking/payment` | Thành công: xóa bản nháp, chuyển sang `redirect:/booking/success?code=BK...`. `SeatsUnavailableException`: chuyển về `/booking/{tripId}/seats?passengers=N` kèm flash "Chỗ vừa được người khác đặt, vui lòng chọn lại." (bản nháp giữ nguyên). `TripNotBookableException`: chuyển về chi tiết chuyến kèm thông báo |
| GET | `/booking/success?code=` | Trang `booking/success`: chỉ hiện booking của chính người đó, kèm link từng vé |

Người đang đăng nhập lấy bằng `@AuthenticationPrincipal TrainGoUserDetails me`, không bao giờ
từ tham số request.

Tạo `controller/TicketController`:

| Method | Đường dẫn | Kết quả |
| --- | --- | --- |
| GET | `/tickets/{ticketId}` | Trang `tickets/detail`; vé của người khác: 404 |

### Bước 5. Giao diện

Dùng khung trang cho khách:

- `templates/booking/payment.html`: thanh các bước (stepper) ở "Thanh toán"; tóm tắt chuyến,
  danh sách hành khách và chỗ, tổng tiền (`520.000 VND`), dòng "Đây là thanh toán mô phỏng,
  không trừ tiền thật" và dòng "Chỗ chỉ được giữ cho bạn sau khi thanh toán thành công"; nút
  "Thanh toán" là form `POST`, tự khóa khi bấm để tránh gửi hai lần. Không có ô nhập thẻ,
  không có đồng hồ đếm ngược.
- `templates/booking/success.html`: stepper ở "Vé"; dòng "Đặt vé thành công", mã booking,
  danh sách vé (tên hành khách, toa, chỗ, link xem vé), link "Chuyến của tôi".
- `templates/tickets/detail.html`: bố cục theo ví dụ ở `SPEC.md` mục 27: mã vé, mã booking,
  ga đi → ga đến, tàu, ngày giờ khởi hành, toa, chỗ (ghế hoặc giường), hành khách. Booking
  đã hủy thì hiện nhãn "Đã hủy". Thêm CSS `@media print` để in vé gọn trong một trang.

Vé **không** hiện số CCCD đầy đủ. Nếu muốn hiện, che bớt (`079*****1234`). Vì sao: vé có
thể bị người khác nhìn thấy hoặc in ra. Nếu sau này làm mã QR, mã chỉ chứa `ticketCode`
(`docs/product/booking-flow.md`).

## 5. Luật và lỗi phải xử lý

- Ba thay đổi của một lần thanh toán (khóa chỗ, tạo booking, tạo vé) cùng xảy ra hoặc cùng
  không xảy ra.
- Chỉ khóa chỗ bằng `UPDATE ... WHERE status = 'AVAILABLE'` và so số dòng; không "đọc rồi ghi".
- Chỗ vừa bị người khác mua: báo "Chỗ vừa được người khác đặt", về trang chọn chỗ với sơ đồ
  mới, không tạo booking hay vé, bản nháp giữ nguyên.
- Chuyến bị hủy hoặc đã khởi hành trong lúc khách đang đi qua các bước: từ chối thanh toán,
  không đổi gì.
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
       ts.id AS trip_seat_id, ts.status, t.ticket_code, t.issued_at
FROM bookings b
JOIN tickets t ON t.booking_id = b.id
JOIN trip_seats ts ON ts.id = t.trip_seat_id
WHERE b.booking_code = '<mã booking>';
```

Và câu xem nhanh các chỗ bạn đang thử (thay bằng id chỗ ghi lại ở trang chọn chỗ, hoặc xem
trong DevTools ô ẩn `tripSeatIds`):

```sql
SELECT id, coach_number, seat_code, status FROM trip_seats WHERE id IN (<id1>, <id2>);
```

**Kịch bản 1. Thanh toán thành công.**

1. Đi trọn luồng: tìm chuyến, chọn 2 chỗ, nhập 2 hành khách, xem lại, bấm "Tiếp tục thanh
   toán". Ghi lại id hai chỗ.
2. Trước khi bấm "Thanh toán", chạy câu xem chỗ: cả hai `AVAILABLE`. Ghi lại số booking và vé:
   `SELECT (SELECT COUNT(*) FROM bookings) AS b, (SELECT COUNT(*) FROM tickets) AS t;`.
3. Bấm "Thanh toán". Trang thành công hiện mã booking và 2 vé.
4. Chạy câu SQL chung với mã booking vừa có. Kết quả đúng: `CONFIRMED`, `PAID`, `paid_at` có
   giá trị, 2 chỗ `BOOKED`, 2 mã vé dạng `TK<năm tháng><số>-01` và `-02`, `issued_at` có giá
   trị. Số booking tăng 1, số vé tăng 2.
5. Mở từng vé: đủ thông tin như `SPEC.md` mục 27, không hiện đủ số CCCD.
6. Mở `/my-trips` (T10): booking mới nằm ở nhóm Sắp đi.
7. Bấm in trang vé (Ctrl+P): vé gọn trong một trang A4.

**Kịch bản 2. Hai người tranh một chỗ (hai trình duyệt).**

1. Khách A (`an.nguyen@example.com`) và khách B (`binh.tran@example.com`), mỗi người ở một
   trình duyệt, cùng chọn **cùng một chỗ** trên cùng một chuyến, nhập hành khách, tới trang
   thanh toán. Chưa ai bấm "Thanh toán". Ghi lại id chỗ đó.
2. A bấm "Thanh toán": thành công.
3. B bấm "Thanh toán". Kết quả đúng: B về trang chọn chỗ với thông báo "Chỗ vừa được người
   khác đặt"; sơ đồ hiện chỗ đó là đã bán; tên, ngày sinh, CCCD B đã nhập vẫn còn khi B chọn
   chỗ khác rồi sang trang hành khách.
4. Chạy SQL: chỗ đó `BOOKED` và chỉ có **một** dòng `tickets` trỏ tới nó (của A). B không có
   booking nào mới.
5. B chọn chỗ khác và thanh toán: thành công.

**Kịch bản 3. Mất một trong hai chỗ thì rollback cả hai.**

1. Với tài khoản bất kỳ, chọn 2 chỗ (chỗ X và chỗ Y), nhập hành khách, tới trang thanh toán.
   Chưa bấm "Thanh toán".
2. Giả lập người khác vừa mua chỗ X:

   ```sql
   UPDATE trip_seats SET status = 'BOOKED' WHERE id = <id chỗ X>;
   ```

3. Bấm "Thanh toán".
4. Kết quả đúng: báo chỗ vừa được đặt, về trang chọn chỗ. Chạy câu xem chỗ: X vẫn `BOOKED`
   (do SQL của bạn), **Y vẫn `AVAILABLE`** (không bị khóa dở dang). Số booking và vé không
   đổi. Xong thì trả X về `AVAILABLE`.

**Kịch bản 4. Chuyến bị hủy hoặc đã khởi hành giữa các bước.**

1. Đi tới trang thanh toán, ghi lại id chuyến và id các chỗ.
2. Hủy chuyến bằng SQL:

   ```sql
   UPDATE trips SET status = 'CANCELLED' WHERE id = <id chuyến>;
   ```

3. Bấm "Thanh toán".
4. Kết quả đúng: về chi tiết chuyến kèm thông báo chuyến không còn đặt được, không chỗ nào
   `BOOKED`, số booking và vé không đổi. Trả chuyến về `SCHEDULED`.
5. Làm lại tới trang thanh toán, ghi lại `departure_at` của chuyến, rồi cho chuyến đã khởi
   hành:

   ```sql
   UPDATE trips SET departure_at = TIMESTAMP(CURRENT_DATE, '00:01:00')
   WHERE id = <id chuyến>;
   ```

   Bấm "Thanh toán": cùng kết quả như bước 4. Xong thì trả `departure_at` về giá trị đã ghi
   lại.

**Kịch bản 5. Bản nháp mất hoặc đã dùng rồi.**

1. Sau khi thanh toán thành công (kịch bản 1), bấm nút Back của trình duyệt rồi gửi lại form
   "Thanh toán", hoặc mở thẳng `/booking/payment`.
2. Kết quả đúng: về trang chủ kèm thông báo phiên đặt vé đã hết; không có booking thứ hai nào
   được tạo.

**Kịch bản 6. Vé và booking của người khác.**

1. Tìm id một vé không phải của bạn:

   ```sql
   SELECT t.id, t.ticket_code, u.email
   FROM tickets t
   JOIN bookings b ON b.id = t.booking_id
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

- Kiểm tra chỗ còn `AVAILABLE` bằng `SELECT` trong Java rồi mới ghi: hai transaction cùng đọc
  thấy trống và cùng mua. Luôn dùng `UPDATE ... WHERE status = 'AVAILABLE'` và so số dòng.
- Quên so số dòng của `bookIfAvailable` với số chỗ đã chọn: mua được một phần số chỗ, hoặc
  không báo lỗi khi mất chỗ.
- Bắt `SeatsUnavailableException` ngay trong `pay` rồi trả kết quả bình thường: transaction
  không rollback, các chỗ đã đổi được kẹt ở `BOOKED` mà không có booking. Phải để exception
  thoát khỏi method `@Transactional`, controller mới bắt.
- Tạo booking hoặc vé trước khi khóa chỗ xong rồi mới kiểm tra: phụ thuộc vào rollback để dọn,
  dễ sai khi có người sửa thứ tự.
- `@Modifying` thiếu `clearAutomatically`: các entity đã đọc trước đó trong cùng
  transaction vẫn mang trạng thái cũ, code phía sau đọc sai.
- Xóa bản nháp trước khi thanh toán thành công: thanh toán lỗi thì khách mất hết hành khách đã
  nhập. Chỉ xóa sau khi `pay` trả về.
- In đầy đủ số CCCD lên vé.

## 8. Kiểm tra trước khi báo xong

- [ ] Chạy trọn: tìm chuyến → chọn 2 chỗ → nhập 2 hành khách → xem lại → thanh toán →
      trang thành công có 2 vé → mở từng vé.
- [ ] Trước khi thanh toán, database chưa có booking hay vé mới; sau khi thanh toán: booking
      `CONFIRMED` / `PAID` có `paid_at`, 2 chỗ `BOOKED`, 2 dòng `tickets` đều có `ticket_code`
      và `issued_at`.
- [ ] Hai trình duyệt tranh một chỗ: người thứ hai bị báo lỗi, về trang chọn chỗ, hành khách
      đã nhập vẫn còn, database chỉ có một vé cho chỗ đó.
- [ ] Mất một trong hai chỗ: không chỗ nào bị khóa dở, không có booking.
- [ ] Booking mới hiện trong `/my-trips` (T10) ở nhóm Sắp đi.
- [ ] Đã đi hết các kịch bản ở mục 6.
- [ ] Trang vé in ra gọn trong một trang A4.
- [ ] `.\mvnw.cmd compile` chạy không lỗi và ứng dụng khởi động được
      (`.\mvnw.cmd spring-boot:run`).
- [ ] Có một dòng trong `docs/WORKLOG.md`, nằm trong cùng commit với code.
