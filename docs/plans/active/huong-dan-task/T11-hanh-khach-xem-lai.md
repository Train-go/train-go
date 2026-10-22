# T11 Hành khách và xem lại booking

> **Tóm tắt:** sau task này, khách đã chọn chỗ nhập được thông tin từng hành khách, xem lại
> giá từng vé và tổng tiền, rồi bấm "Tiếp tục thanh toán" để sang trang thanh toán. Cả hai
> bước chỉ làm việc trên bản nháp trong session; chưa có gì được ghi vào database.

| Đợt | Phần việc | Độ khó | Cần xong trước | Task đang chờ task này | Tên branch |
| --- | --- | --- | --- | --- | --- |
| 3 | B | Dễ đến vừa (khoảng 2 ngày) | T09, quyết định D7 | T12 | `feat/t11-passengers-review` |

Các từ như session, form object, DTO được giải thích trong `README.md` cùng thư mục.

## 1. Task này làm gì

Sau khi chọn chỗ (T09), khách nhập thông tin từng hành khách. Mỗi hành khách gắn với đúng
một chỗ đã chọn (`SPEC.md` mục 19, 20). Sau đó khách xem lại toàn bộ booking với giá từng
vé và tổng tiền (`SPEC.md` mục 24).

Khi khách bấm "Tiếp tục thanh toán", hệ thống **không** tạo booking: chỉ chuyển sang trang
thanh toán (T12). Booking và vé chỉ được tạo trong transaction thanh toán, cùng lúc khóa chỗ
(`docs/decisions/0010-bo-giu-cho.md`, quyết định D7). Vì vậy khách bỏ ngang ở bước nào cũng
không để lại dòng rác trong database.

## 2. Phạm vi

Làm:

- Form nhập hành khách.
- Trang xem lại booking, tính giá từ database.

Không làm:

- Tạo booking, tạo vé, sinh mã booking và mã vé: thuộc T12 (quyết định D5).
- Thanh toán: thuộc T12.
- Giá theo độ tuổi hay đối tượng (`SPEC.md` mục 19 không có).

## 3. Dữ liệu dùng tới

Task này **chỉ đọc** database (bảng `trips` và `trip_seats`) và ghi vào session.

```
BookingDraft (session)
  tripId, passengerCount, tripSeatIds              (từ T09)
  passengers: fullName, dateOfBirth, identityNumber  (T11 thêm, theo thứ tự của tripSeatIds)

trips 1 ──< trip_seats        giá một chỗ = trips.base_price + trip_seats.price_modifier
```

Đọc sơ đồ: bản nháp có danh sách chỗ (theo thứ tự) và, sau bước này, danh sách hành khách
theo cùng thứ tự: hành khách thứ i ngồi chỗ thứ i. Mỗi dòng `trip_seats` ghi luôn số toa, mã
chỗ và phụ thu của chỗ đó.

Giá vé tính từ database mỗi lần hiện trang xem lại; **giá được ghi vào `tickets.ticket_price`
và đóng băng lúc thanh toán (T12)**. "Đóng băng" nghĩa là sau đó admin sửa giá chuyến cũng
không ảnh hưởng booking đã tạo (`SPEC.md` mục 21). Trang xem lại chỉ là bản xem trước, nên nếu
admin đổi giá giữa chừng thì khách thấy giá mới ở lần tải lại.

## 4. Các bước làm

### Bước 1. Model: mở rộng bản nháp và tạo form

Mở rộng class `model/dto/BookingDraft` mà T09 đã tạo. Đây là bản nháp booking lưu trong
session, giữ dữ liệu giữa các bước đặt vé:

```java
private List<PassengerDraft> passengers;   // null cho tới khi nhập xong
```

Tạo class mới `model/dto/PassengerDraft` cho từng hành khách trong bản nháp. Hành khách
không lưu `tripSeatId`: thứ tự trong danh sách chính là thứ tự của `tripSeatIds`, nên khách
chọn lại chỗ (T09) thì thông tin đã nhập vẫn gán lại được mà không phải gõ lại.

```java
public class PassengerDraft implements Serializable {
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
    @NotNull private Long tripSeatId;   // ô ẩn, chỉ để đối chiếu với bản nháp
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

record BookingReview(String routeLabel, LocalDateTime departureAt,
        String trainCode, List<ReviewLine> lines, long totalPrice) { }
```

### Bước 2. Repository: thêm câu truy vấn

Thêm vào `repository/TripSeatRepository`. Method này đọc mọi chỗ trong bản nháp (đã kèm
số toa, mã chỗ, phụ thu) trong một lần:

```java
List<TripSeat> findByIdIn(Collection<Long> ids);
```

Không cần thêm gì vào `BookingRepository`: sinh mã booking là việc của T12.

### Bước 3. Service: xử lý nghiệp vụ

Tạo `service/BookingDraftService`:

| Method | Việc |
| --- | --- |
| `void savePassengers(BookingDraft draft, PassengersRequest form)` | Số hành khách bằng số chỗ trong bản nháp; tập `tripSeatId` trong form đúng bằng tập `tripSeatIds` của bản nháp, mỗi id xuất hiện đúng một lần. Sai thì ném `InvalidPassengersException`. Đúng thì chép tên, ngày sinh, CCCD vào `draft.passengers` theo thứ tự của `tripSeatIds` |
| `@Transactional(readOnly = true) BookingReview review(BookingDraft draft)` | Đọc chuyến và các chỗ của bản nháp từ database, tính giá từng chỗ, dựng DTO. Không đọc giá từ form |

Giá luôn tính lại từ database, không bao giờ lấy từ form. Vì sao: người dùng có thể sửa HTML
để gửi lên một giá khác. Giá một chỗ là `trip.getBasePrice()` cộng `priceModifier` của chỗ;
`totalPrice` là tổng các giá đó.

`InvalidPassengersException` đặt trong `common/exception/`.

`review` còn kiểm tra chuyến vẫn `SCHEDULED` và chưa khởi hành (`trip.getDepartureAt()` còn
ở sau `LocalDateTime.now()`). Sai thì ném
`TripNotBookableException` (của T09). Chưa kiểm tra chỗ còn trống ở đây: việc đó làm lúc
thanh toán (T12), nên khách vẫn có thể thấy trang xem lại của một chỗ vừa bị mua mất.

### Bước 4. Controller: nhận request

Tạo `controller/BookingFlowController` với `@RequestMapping("/booking")`:

| Method | Đường dẫn | Kết quả |
| --- | --- | --- |
| GET | `/booking/passengers` | Trang `booking/passengers`, một dòng cho mỗi chỗ (nhãn "Toa 01 · A05"); điền sẵn nếu bản nháp đã có hành khách |
| POST | `/booking/passengers` | Lỗi: hiện lại form. Xong: lưu vào bản nháp, chuyển sang `redirect:/booking/review` |
| GET | `/booking/review` | Trang `booking/review`; chưa có hành khách trong bản nháp thì chuyển về `/booking/passengers` |
| POST | `/booking/review` | Chuyển sang `redirect:/booking/payment`. Không ghi database |

Mọi handler đều đọc bản nháp bằng `BookingDraftStore` trước tiên. Nếu không có bản nháp
(session mới, đã thanh toán xong, hoặc khách mở thẳng đường dẫn): chuyển về `/` kèm thông
báo "Phiên đặt vé đã hết, vui lòng chọn lại chuyến." Nếu biết `tripId` thì chuyển về
`/trips/{tripId}`.

T12 sẽ thêm handler `/booking/payment` và `/booking/success` vào chính controller này. Hai
người thêm hai nhóm method khác nhau; nếu merge bị conflict thì giữ cả hai.

### Bước 5. Giao diện

Dùng khung trang cho khách. Thanh các bước (stepper) đang ở "Hành khách" rồi "Xem lại":

- `templates/booking/passengers.html`: mỗi hành khách một khung (card) gồm nhãn chỗ, ô họ
  tên, ô ngày sinh (`type="date"`), ô CCCD hoặc hộ chiếu, và một ô ẩn `tripSeatId`. Tên field
  đánh theo số thứ tự, ví dụ `th:field="*{passengers[__${i}__].fullName}"`. Có liên kết
  "Đổi chỗ" về `/booking/{tripId}/seats` (T09). Không có đồng hồ đếm ngược.
- `templates/booking/review.html`: tuyến, ngày giờ đi, tàu, bảng hành khách (tên, toa, chỗ,
  giá) và tổng tiền. Nút "Quay lại" về `/booking/passengers`; bản nháp giữ nguyên dữ liệu
  đã nhập. Nút "Tiếp tục thanh toán" là form `POST`. Dòng chú thích "Chỗ chỉ được giữ cho bạn
  sau khi thanh toán thành công."

## 5. Luật và lỗi phải xử lý

- Số hành khách phải bằng số chỗ đã chọn; mỗi chỗ gán đúng một hành khách.
- Họ tên không trống, ngày sinh ở quá khứ, CCCD hoặc hộ chiếu gồm 6 đến 20 chữ cái hoặc chữ
  số. Sai ô nào thì báo lỗi dưới đúng ô đó, dữ liệu các ô khác vẫn còn.
- Giá tính từ database, không lấy từ form.
- Không có bản nháp, hoặc chuyến đã hủy hoặc đã khởi hành: quay về chi tiết chuyến (hoặc
  trang chủ) kèm thông báo.
- Không có booking, vé hay thay đổi trạng thái chỗ nào trong database ở task này.

## 6. Kịch bản thử bằng tay

`AGENTS.md` coi "số hành khách bằng số chỗ" và công thức giá là nghiệp vụ bắt buộc phải thử
kỹ. Chạy ứng dụng bằng `.\mvnw.cmd spring-boot:run`, đăng nhập một tài khoản khách (ví dụ
`an.nguyen@example.com`, mật khẩu `Customer@123`). Khi cần xem hay sửa database, mở MySQL
bằng lệnh:

```
docker exec -it traingo-mysql mysql -utraingo -ptraingo --default-character-set=utf8mb4 traingo
```

**Kịch bản 1. Nhập hành khách và xem lại giá.**

1. Tìm một chuyến, chọn 1 chỗ ở toa ghế và 1 chỗ ở toa giường (T09).
2. Trang hành khách hiện đúng 2 khung, mỗi khung có nhãn chỗ dạng "Toa 01 · A05".
3. Xem giá gốc của chuyến và phụ thu của hai toa:

   ```sql
   SELECT DISTINCT t.id, t.base_price, ts.coach_number, ts.coach_type, ts.price_modifier
   FROM trips t JOIN trip_seats ts ON ts.trip_id = t.id
   WHERE t.id = <id chuyến> ORDER BY ts.coach_number;
   ```

4. Nhập 2 hành khách hợp lệ, sang trang xem lại. Kết quả đúng: giá từng vé bằng
   `base_price + price_modifier` của toa đó, tổng tiền bằng tổng hai vé. Ví dụ chuyến Đà Nẵng
   → Nha Trang của SE1 trong dữ liệu mẫu có giá gốc 520.000: vé ghế 520.000, vé giường
   (phụ thu 200.000) 720.000, tổng 1.240.000.

**Kịch bản 2. Lỗi từng ô.**

1. Ở trang hành khách, bỏ trống họ tên người thứ nhất, nhập ngày sinh ngày mai cho người
   thứ hai, nhập CCCD có dấu cách (ví dụ `0123 4567`).
2. Kết quả đúng: mỗi lỗi hiện dưới đúng ô của nó; các ô đã nhập đúng vẫn giữ nguyên giá trị.

**Kịch bản 3. Số hành khách khác số chỗ.**

1. Chọn 3 chỗ, mở trang hành khách.
2. Mở DevTools, xóa hẳn một khung hành khách khỏi HTML, rồi gửi form với 2 hành khách.
3. Làm lại với một khung bị sửa ô ẩn `tripSeatId` cho trùng với khung khác.
4. Kết quả đúng: cả hai lần đều bị từ chối với thông báo lỗi, không sang trang xem lại.

**Kịch bản 4. "Tiếp tục thanh toán" không ghi database.**

1. Ghi lại số booking và số vé hiện có:

   ```sql
   SELECT (SELECT COUNT(*) FROM bookings) AS bookings,
          (SELECT COUNT(*) FROM tickets) AS tickets,
          (SELECT COUNT(*) FROM trip_seats WHERE status = 'BOOKED') AS booked_seats;
   ```

2. Đi từ trang chọn chỗ tới trang xem lại, bấm "Tiếp tục thanh toán" (trang thanh toán chưa
   có nếu T12 chưa xong; chỉ cần thấy yêu cầu chuyển hướng tới `/booking/payment`).
3. Chạy lại câu SQL trên. Kết quả đúng: ba con số không đổi.

**Kịch bản 5. Giá lấy từ database, không từ form.**

1. Ở trang xem lại, ghi lại tổng tiền.
2. Đổi giá gốc của chuyến:

   ```sql
   UPDATE trips SET base_price = base_price + 100000 WHERE id = <id chuyến>;
   ```

3. Tải lại trang xem lại. Kết quả đúng: giá từng vé tăng 100.000 và tổng tiền tăng tương
   ứng. Xong thì trả giá về như cũ (`base_price - 100000`).

**Kịch bản 6. Không có bản nháp hoặc chuyến không còn đặt được.**

1. Mở ngay `/booking/passengers` ở một cửa sổ ẩn danh vừa đăng nhập (chưa chọn chỗ). Kết
   quả đúng: chuyển về trang chủ kèm thông báo phiên đặt vé đã hết.
2. Chọn chỗ, nhập hành khách, dừng ở trang xem lại. Hủy chuyến bằng SQL
   (`UPDATE trips SET status = 'CANCELLED' WHERE id = <id chuyến>;`), tải lại trang xem lại.
   Kết quả đúng: chuyển về chi tiết chuyến kèm thông báo chuyến không còn đặt được. Xong thì
   trả chuyến về `SCHEDULED`.

**Kịch bản 7. Chọn lại chỗ không phải nhập lại hành khách.**

1. Chọn 2 chỗ, nhập 2 hành khách, sang trang xem lại.
2. Quay lại trang chọn chỗ (T09), đổi sang 2 chỗ khác, bấm "Tiếp tục".
3. Kết quả đúng: trang hành khách điền sẵn tên, ngày sinh, CCCD đã nhập, nhãn chỗ là chỗ mới.

## 7. Lỗi hay gặp

- Lấy giá từ form gửi lên: người dùng sửa HTML là đổi được giá vé.
- Quên `@Valid` trên danh sách `passengers`: luật của từng hành khách không được kiểm tra.
- Tạo booking hay vé khi bấm "Tiếp tục thanh toán": database đầy dòng rác khi khách bỏ ngang,
  và trái ADR 0010. Booking chỉ được tạo trong transaction thanh toán ở T12.
- Lưu `tripSeatId` trong `PassengerDraft`: khách chọn lại chỗ thì hành khách trỏ nhầm chỗ cũ.
  Ghép theo thứ tự.

## 8. Kiểm tra trước khi báo xong

- [ ] Chọn 2 chỗ (T09), nhập 2 hành khách, trang xem lại hiện đúng giá và tổng tiền.
- [ ] Bỏ trống họ tên, nhập ngày sinh ở tương lai, CCCD có dấu cách: báo lỗi đúng ô, dữ liệu
      các ô khác vẫn còn.
- [ ] Bấm "Tiếp tục thanh toán": chuyển sang `/booking/payment`, số `bookings`, `tickets`
      và chỗ `BOOKED` trong database không đổi.
- [ ] Mở thẳng `/booking/passengers` khi chưa có bản nháp: chuyển về trang chủ kèm thông báo.
- [ ] Đã đi hết các kịch bản ở mục 6.
- [ ] Kiểm tra giao diện ở màn hình rộng 375px.
- [ ] `.\mvnw.cmd compile` chạy không lỗi và ứng dụng khởi động được
      (`.\mvnw.cmd spring-boot:run`).
- [ ] Có một dòng trong `docs/WORKLOG.md`, nằm trong cùng commit với code.
