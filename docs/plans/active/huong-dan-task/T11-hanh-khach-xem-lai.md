# T11 Hành khách và xem lại booking

> **Tóm tắt:** sau task này, khách đã giữ chỗ nhập được thông tin từng hành khách, xem lại
> giá từng vé và tổng tiền, rồi bấm "Tiếp tục thanh toán" để tạo booking chờ thanh toán.

| Đợt | Phần việc | Độ khó | Cần xong trước | Task đang chờ task này | Tên branch |
| --- | --- | --- | --- | --- | --- |
| 3 | B | Vừa (khoảng 2-3 ngày) | T09, quyết định D5, D7 | T12 | `feat/t11-passengers-review` |

Các từ như session, transaction, form object, DTO được giải thích trong `README.md` cùng
thư mục.

## 1. Task này làm gì

Sau khi giữ chỗ (T09), khách nhập thông tin từng hành khách. Mỗi hành khách gắn với đúng
một chỗ đã giữ (`SPEC.md` mục 19, 20). Sau đó khách xem lại toàn bộ booking với giá từng
vé và tổng tiền (`SPEC.md` mục 24).

Khi khách bấm "Tiếp tục thanh toán", hệ thống mới tạo booking trạng thái `PENDING` (chờ
thanh toán) trong database, với giá đã được "đóng băng" (quyết định D7). "Đóng băng" nghĩa
là giá được ghi lại ngay lúc này, sau đó admin sửa giá chuyến cũng không ảnh hưởng.

## 2. Phạm vi

Làm:

- Form nhập hành khách.
- Trang xem lại booking.
- Tạo booking `PENDING` kèm danh sách hành khách.
- Sinh mã booking (quyết định D5).

Không làm:

- Thanh toán: thuộc T12.
- Giá theo độ tuổi hay đối tượng (`SPEC.md` mục 19 không có).

## 3. Dữ liệu dùng tới

```
users 1 ──< bookings >── 1 trips
               │
               └──< booking_passengers >── 1 trip_seats >── seats >── coaches

bookings:           booking_code UNIQUE, customer_id, trip_id, total_price,
                    booking_status = PENDING, payment_status = UNPAID, created_at
booking_passengers: booking_id, trip_seat_id, full_name, date_of_birth,
                    identity_number, ticket_price
                    UNIQUE (booking_id, trip_seat_id)
```

Đọc sơ đồ: một khách có nhiều booking, mỗi booking thuộc một chuyến. Một booking có nhiều
hành khách (`booking_passengers`); mỗi hành khách ngồi đúng một chỗ của chuyến
(`trip_seats`), và chỗ đó thuộc một toa (`coaches`). Mã booking không được trùng; trong một
booking, một chỗ chỉ được gán cho một hành khách.

`ticket_price` = `trips.base_price` + `coaches.price_modifier`, tính **tại lúc tạo
booking**. Vì sao phải lưu lại: admin sửa giá chuyến sau đó không được làm đổi giá của
booking cũ (`SPEC.md` mục 21).

## 4. Các bước làm

### Bước 1. Model: mở rộng bản nháp và tạo form

Các entity `Booking` (constructor đã tạo sẵn `PENDING` / `UNPAID`) và `BookingPassenger`
đã có, **không thêm hay sửa field**.

Mở rộng class `model/dto/BookingDraft` mà T09 đã tạo. Đây là bản nháp booking lưu trong
session, giữ dữ liệu giữa các bước đặt vé:

```java
private List<PassengerDraft> passengers;   // null cho tới khi nhập xong
private Long bookingId;                    // có sau khi tạo booking PENDING
```

Tạo class mới `model/dto/PassengerDraft` cho từng hành khách trong bản nháp:

```java
public class PassengerDraft implements Serializable {
    private Long tripSeatId;
    private String fullName;
    private LocalDate dateOfBirth;
    private String identityNumber;
}
```

Tạo form nhận dữ liệu từ trang hành khách, hai class `model/request/PassengersRequest` và
`model/request/PassengerRequest`. `@Valid` đặt trên danh sách để Spring kiểm tra luật của
**từng** hành khách:

```java
public class PassengersRequest {
    @Valid
    private List<PassengerRequest> passengers = new ArrayList<>();
}

public class PassengerRequest {
    @NotNull private Long tripSeatId;
    @NotBlank @Size(max = 100) private String fullName;
    @NotNull @Past @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dateOfBirth;
    @NotBlank @Pattern(regexp = "[A-Za-z0-9]{6,20}",
            message = "Số CCCD hoặc hộ chiếu gồm 6 đến 20 chữ cái hoặc chữ số.")
    private String identityNumber;
}
```

Tạo DTO cho trang xem lại, mỗi record một file trong `model/response/`:

```java
record ReviewLine(String fullName, int coachNumber, String seatCode,
        long ticketPrice) { }

record BookingReview(String routeLabel, LocalDate departureDate,
        LocalTime departureTime, String trainCode, List<ReviewLine> lines,
        long totalPrice, LocalDateTime heldUntil) { }
```

### Bước 2. Repository: thêm câu truy vấn

Thêm vào `repository/TripSeatRepository`. Method này đọc mọi chỗ trong bản nháp cùng ghế,
toa và người giữ chỗ trong một lần:

```java
@EntityGraph(attributePaths = {"seat", "seat.coach", "heldBy"})
List<TripSeat> findByIdIn(Collection<Long> ids);
```

Thêm vào `repository/BookingRepository`, dùng để sinh mã booking (quyết định D5). Method trả
về booking có mã lớn nhất bắt đầu bằng một tiền tố cho trước:

```java
Optional<Booking> findTopByBookingCodeStartingWithOrderByBookingCodeDesc(String prefix);
```

### Bước 3. Service: xử lý nghiệp vụ

Tạo `service/BookingCodeGenerator` (quyết định D5). Tháng hiện tại lấy từ
`LocalDate.now()`:

- `String next()`: tiền tố là `"BK" + yyyyMM` của tháng hiện tại. Lấy mã lớn nhất có tiền
  tố đó, cộng thêm 1 vào số thứ tự, đệm cho đủ 3 chữ số. Ví dụ đã có `BK202610004` thì trả
  `BK202610005`.

Tạo `service/BookingDraftService`. Hạn giữ chỗ so với `LocalDateTime.now()`:

| Method | Việc |
| --- | --- |
| `void savePassengers(BookingDraft draft, PassengersRequest form)` | Số hành khách bằng số chỗ; mỗi `tripSeatId` trong draft xuất hiện đúng một lần; còn hạn giữ chỗ. Sai thì ném `InvalidPassengersException` |
| `BookingReview review(BookingDraft draft)` | Tính giá từng chỗ từ database, không lấy từ form |
| `@Transactional Booking createPending(BookingDraft draft, long userId)` | Xem các bước bên dưới |

Giá luôn tính lại từ database, không bao giờ lấy từ form. Vì sao: người dùng có thể sửa HTML
để gửi lên một giá khác.

`createPending` làm lần lượt:

1. Đọc mọi `TripSeat` trong bản nháp bằng `findByIdIn`.
2. Kiểm tra từng chỗ: phải đang `HELD`, `heldBy` đúng là người này, và `heldUntil` còn ở sau
   hiện tại. Sai một chỗ là ném `HoldExpiredException`, không tạo gì cả.
3. Tính `ticketPrice` cho từng chỗ; `totalPrice` là tổng các giá đó.
4. Gọi `new Booking(codeGenerator.next(), user, trip, totalPrice)` rồi lưu. Sau đó lưu từng
   `BookingPassenger` theo đúng thứ tự trong bản nháp.
5. Trả về booking; controller ghi `bookingId` vào bản nháp.

Hai exception `InvalidPassengersException` và `HoldExpiredException` đặt trong
`common/exception/`.

Hai người tạo booking cùng một giây có thể nhận cùng một mã. Khi đó khóa unique
`uk_bookings_code` trong database chặn người thứ hai. Controller bắt
`DataIntegrityViolationException` và thử lại một lần.

### Bước 4. Controller: nhận request

Tạo `controller/BookingFlowController` với `@RequestMapping("/booking")`:

| Method | Đường dẫn | Kết quả |
| --- | --- | --- |
| GET | `/booking/passengers` | Trang `booking/passengers`, một dòng cho mỗi chỗ (nhãn "Toa 01 · A05") |
| POST | `/booking/passengers` | Lỗi: hiện lại form. Xong: lưu vào bản nháp, chuyển sang `redirect:/booking/review` |
| GET | `/booking/review` | Trang `booking/review` |
| POST | `/booking/review` | Gọi `createPending`, ghi `bookingId`, chuyển sang `redirect:/booking/payment` |

Mọi handler đều đọc bản nháp bằng `BookingDraftStore` trước tiên. Nếu không có bản nháp,
hoặc đã hết hạn giữ chỗ: chuyển về `/trips/{tripId}` (hoặc `/` nếu không biết chuyến nào)
kèm thông báo "Hết thời gian giữ chỗ, vui lòng chọn lại."

T12 sẽ thêm handler `/booking/payment` và `/booking/success` vào chính controller này. Hai
người thêm hai nhóm method khác nhau; nếu merge bị conflict thì giữ cả hai.

### Bước 5. Giao diện

Dùng khung trang cho khách. Thanh các bước (stepper) đang ở "Hành khách" rồi "Xem lại":

- `templates/booking/passengers.html`: mỗi hành khách một khung (card) gồm nhãn chỗ, ô họ
  tên, ô ngày sinh (`type="date"`), ô CCCD hoặc hộ chiếu. Tên field đánh theo số thứ tự,
  ví dụ `th:field="*{passengers[__${i}__].fullName}"`. Đồng hồ đếm ngược dùng
  `hold-countdown.js` (của T09) với `th:data-held-until="${draft.heldUntil}"`.
- `templates/booking/review.html`: tuyến, ngày giờ đi, tàu, bảng hành khách (tên, toa, chỗ,
  giá) và tổng tiền. Nút "Quay lại" về `/booking/passengers`; bản nháp giữ nguyên dữ liệu
  đã nhập. Nút "Tiếp tục thanh toán" là form `POST`.

## 5. Luật và lỗi phải xử lý

- Số hành khách phải bằng số chỗ đã giữ; mỗi chỗ gán đúng một hành khách.
- Họ tên không trống, ngày sinh ở quá khứ, CCCD hoặc hộ chiếu gồm 6 đến 20 chữ cái hoặc chữ
  số. Sai ô nào thì báo lỗi dưới đúng ô đó, dữ liệu các ô khác vẫn còn.
- Giá tính từ database tại lúc tạo booking, không lấy từ form.
- Hết thời gian giữ chỗ: không tạo booking, quay về chi tiết chuyến kèm thông báo.

## 6. Kịch bản thử bằng tay

`AGENTS.md` coi "số hành khách bằng số chỗ" và công thức giá là nghiệp vụ bắt buộc phải thử
kỹ. Chạy ứng dụng bằng `.\mvnw.cmd spring-boot:run`, đăng nhập một tài khoản khách (ví dụ
`an.nguyen@example.com`, mật khẩu `Customer@123`). Khi cần xem hay sửa database, mở MySQL
bằng lệnh:

```
docker exec -it traingo-mysql mysql -utraingo -ptraingo --default-character-set=utf8mb4 traingo
```

**Kịch bản 1. Nhập hành khách và xem lại giá.**

1. Tìm một chuyến, giữ 1 chỗ ở toa ghế và 1 chỗ ở toa giường (T09).
2. Trang hành khách hiện đúng 2 khung, mỗi khung có nhãn chỗ dạng "Toa 01 · A05".
3. Xem giá gốc của chuyến và phụ thu của hai toa:

   ```sql
   SELECT t.id, t.base_price, c.coach_number, c.price_modifier
   FROM trips t JOIN coaches c ON c.train_id = t.train_id
   WHERE t.id = <id chuyến>;
   ```

4. Nhập 2 hành khách hợp lệ, sang trang xem lại. Kết quả đúng: giá từng vé bằng
   `base_price + price_modifier` của toa đó, tổng tiền bằng tổng hai vé. Ví dụ chuyến có
   giá gốc 650.000, toa giường phụ thu 200.000 thì vé giường là 850.000; hai vé ghế
   350.000 cộng lại là 700.000.

**Kịch bản 2. Lỗi từng ô.**

1. Ở trang hành khách, bỏ trống họ tên người thứ nhất, nhập ngày sinh ngày mai cho người
   thứ hai, nhập CCCD có dấu cách (ví dụ `0123 4567`).
2. Kết quả đúng: mỗi lỗi hiện dưới đúng ô của nó; các ô đã nhập đúng vẫn giữ nguyên giá trị.

**Kịch bản 3. Số hành khách khác số chỗ.**

1. Giữ 3 chỗ, mở trang hành khách.
2. Mở DevTools, xóa hẳn một khung hành khách khỏi HTML, rồi gửi form với 2 hành khách.
3. Làm lại với một khung bị sửa ô ẩn `tripSeatId` cho trùng với khung khác.
4. Kết quả đúng: cả hai lần đều bị từ chối với thông báo lỗi, không sang trang xem lại.

**Kịch bản 4. Tạo booking `PENDING` và mã booking.**

1. Xem mã booking lớn nhất của tháng này:

   ```sql
   SELECT booking_code FROM bookings
   WHERE booking_code LIKE CONCAT('BK', DATE_FORMAT(CURDATE(), '%Y%m'), '%')
   ORDER BY booking_code DESC LIMIT 1;
   ```

2. Ở trang xem lại, bấm "Tiếp tục thanh toán".
3. Chạy lại câu SQL trên. Kết quả đúng: có mã mới bằng mã cũ cộng 1, đệm đủ 3 chữ số
   (database được seed trong tháng này thì đang dừng ở `...020`, nên mã mới là `...021`;
   tháng chưa có mã nào thì là `...001`).
4. Kiểm tra booking vừa tạo:

   ```sql
   SELECT b.booking_code, b.booking_status, b.payment_status, b.total_price,
          bp.full_name, bp.trip_seat_id, bp.ticket_price
   FROM bookings b JOIN booking_passengers bp ON bp.booking_id = b.id
   WHERE b.booking_code = '<mã mới>';
   ```

   Kết quả đúng: `PENDING`, `UNPAID`, mỗi hành khách một dòng với giá đúng như trang xem
   lại, `total_price` bằng tổng các dòng.

**Kịch bản 5. Giá được đóng băng.**

1. Sau kịch bản 4, đổi giá gốc của chuyến:

   ```sql
   UPDATE trips SET base_price = base_price + 100000 WHERE id = <id chuyến>;
   ```

2. Chạy lại câu SQL kiểm tra booking ở kịch bản 4. Kết quả đúng: `ticket_price` và
   `total_price` của booking cũ không đổi.

**Kịch bản 6. Hết hạn giữ chỗ trước khi bấm "Tiếp tục thanh toán".**

Không cần đợi 5 phút: bạn đẩy hạn giữ chỗ về quá khứ bằng SQL.

1. Giữ chỗ, nhập hành khách, dừng ở trang xem lại.
2. Tìm id của bạn và đẩy hạn giữ chỗ về quá khứ:

   ```sql
   SELECT id FROM users WHERE email = 'an.nguyen@example.com';
   UPDATE trip_seats SET held_until = NOW() - INTERVAL 1 MINUTE
   WHERE held_by_user_id = <id của bạn> AND status = 'HELD';
   ```

3. Bấm "Tiếp tục thanh toán".
4. Kết quả đúng: quay về trang chi tiết chuyến với thông báo "Hết thời gian giữ chỗ, vui
   lòng chọn lại."; chạy SQL ở kịch bản 4 bước 1 thấy không có mã booking mới nào.
5. Làm lại bước 2 khi đang ở trang hành khách rồi gửi form: cũng bị chuyển về chi tiết
   chuyến.

## 7. Lỗi hay gặp

- Lấy giá từ form gửi lên: người dùng sửa HTML là đổi được giá vé.
- Quên `@Valid` trên danh sách `passengers`: luật của từng hành khách không được kiểm tra.
- Tạo booking ngay khi lưu hành khách: nếu khách bỏ ngang, database đầy booking rác. Theo
  quyết định D7, chỉ tạo khi bấm "Tiếp tục thanh toán".
- Không kiểm tra lại hạn giữ chỗ trong `createPending`: tạo được booking cho chỗ đã bị người
  khác giữ.

## 8. Kiểm tra trước khi báo xong

- [ ] Giữ 2 chỗ (T09), nhập 2 hành khách, trang xem lại hiện đúng giá và tổng tiền.
- [ ] Bỏ trống họ tên, nhập ngày sinh ở tương lai, CCCD có dấu cách: báo lỗi đúng ô, dữ liệu
      các ô khác vẫn còn.
- [ ] Bấm "Tiếp tục thanh toán": database có 1 booking `PENDING` / `UNPAID` với mã
      `BK<năm tháng><số>` và đủ dòng `booking_passengers` có giá đúng.
- [ ] Hạn giữ chỗ đã qua (đẩy bằng SQL) rồi bấm: báo hết thời gian, không có booking nào
      được tạo.
- [ ] Đã đi hết các kịch bản ở mục 6.
- [ ] Kiểm tra giao diện ở màn hình rộng 375px.
- [ ] `.\mvnw.cmd compile` chạy không lỗi và ứng dụng khởi động được
      (`.\mvnw.cmd spring-boot:run`).
- [ ] Có một dòng trong `docs/WORKLOG.md`, nằm trong cùng commit với code.
