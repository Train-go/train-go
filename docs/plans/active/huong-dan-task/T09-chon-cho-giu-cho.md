# T09 Chọn chỗ

> **Tóm tắt:** sau task này, khách chọn chỗ trên sơ đồ toa, bấm "Tiếp tục" là các chỗ đã
> chọn được ghi vào bản nháp booking trong session. Chưa có gì được ghi vào database; chỗ chỉ
> bị khóa lúc thanh toán (T12).

Tên file là `T09-chon-cho-giu-cho.md` vì lúc đầu task này là "chọn chỗ và giữ chỗ". Tính năng
giữ chỗ đã bỏ (`docs/decisions/0010-bo-giu-cho.md`), tên file giữ nguyên để các link không
gãy.

| Đợt | Phần việc | Độ khó | Cần xong trước | Task đang chờ task này | Tên branch |
| --- | --- | --- | --- | --- | --- |
| 2 | B | Vừa (khoảng 3 ngày) | T02, T05, T06, quyết định D6, D7 | T11, T12 | `feat/t09-seat-selection` |

Các từ như transaction, session, CSRF token được giải thích trong `README.md` cùng thư mục.
Cách chia package theo tầng (`controller/`, `service/`, `model/`...) xem ADR 0006.

## 1. Task này làm gì

Luồng của khách:

1. Ở trang chi tiết chuyến (T05), khách bấm "Chọn chỗ".
2. Trang chọn chỗ hiện **một toa một lần**, có dải tab toa phía trên để chuyển toa. Khách bấm
   vào sơ đồ để chọn đúng bằng số hành khách; chỗ đã chọn ở toa này vẫn còn nguyên khi khách
   chuyển sang toa khác.
3. Khách bấm "Tiếp tục". Hệ thống kiểm tra nhanh các chỗ đã chọn còn trống, lưu danh sách chỗ
   vào bản nháp booking trong session, rồi chuyển sang bước nhập hành khách (T11).

Task này **không giữ chỗ**: không có dòng nào trong database bị đổi, không có hạn giữ, không
có đồng hồ đếm ngược. Hai khách có thể cùng chọn một chỗ; ai bấm "Thanh toán" (T12) trước thì
được chỗ, người sau bị báo "chỗ vừa được người khác đặt" và quay lại trang này. Đây là chỗ
lệch `SPEC.md` mục giữ chỗ, đã chốt ở `docs/decisions/0010-bo-giu-cho.md`.

### Hai quy ước chung của task này

Hai quyết định dưới đây là quyết định nghiệp vụ trong `chia-viec-mvp.md` (D1 không còn áp
dụng):

- **D6.** Mỗi lần chọn từ 1 đến 10 chỗ, đúng bằng số hành khách.
- **D7, bản nháp nằm trong session cho tới lúc thanh toán.** Bấm vào một chỗ chỉ đánh dấu
  `SELECTED` trên trình duyệt, không lưu gì vào database (`SPEC.md` mục 17). Bấm "Tiếp tục"
  thì lưu các id chỗ vào `BookingDraft` trong session. Bước hành khách và bước xem lại chỉ
  sửa bản nháp. Bấm "Thanh toán" mới có một transaction tạo mọi thứ (T12).

## 2. Phạm vi

Làm:

- Trang chọn chỗ, một toa một lần, có dải tab toa.
- API trả sơ đồ chỗ của một chuyến (96 chỗ, mỗi chỗ `AVAILABLE` hoặc `BOOKED`).
- Kiểm tra sớm các chỗ đã chọn còn trống khi bấm "Tiếp tục".
- Bản nháp booking trong session (D7).

Không làm:

- Giữ chỗ, hết hạn, đếm ngược: đã bỏ (ADR 0010).
- Khóa chỗ: thuộc T12, lúc thanh toán.
- Nhập hành khách: thuộc T11.
- WebSocket (cập nhật tức thời qua kết nối mở sẵn): `SPEC.md` mục 18 không bắt buộc.

## 3. Dữ liệu dùng tới

```
trips 1 ──< trip_seats

trip_seats
  coach_number, coach_type, seat_code, cabin_number, price_modifier   (chép từ CoachLayout)
  status           AVAILABLE | BOOKED
  UNIQUE (trip_id, coach_number, seat_code)
```

Đọc sơ đồ:

- Mỗi chuyến có 96 dòng `trip_seats`, một dòng cho mỗi chỗ trong cấu hình toa cố định. Dòng
  giữ luôn số toa, loại toa, mã chỗ và phụ thu, nên sơ đồ chỉ đọc từ bảng này, không join
  (ADR 0009).
- Một chỗ chỉ có hai trạng thái: `AVAILABLE` (trống) và `BOOKED` (đã bán). Không còn trạng
  thái `HELD` hay cột người giữ, hạn giữ (ADR 0010).
- Task này **chỉ đọc** `trip_seats`. Đổi `AVAILABLE` sang `BOOKED` là việc của T12.

## 4. Các bước làm

### Bước 1. Model: bản nháp booking, nơi lưu bản nháp, và DTO

Entity `entity/TripSeat`, `entity/TripSeatStatus`, `entity/Trip` đã có. Để biết chuyến đã
khởi hành chưa, dùng thẳng `trip.getDepartureAt()` (kiểu `LocalDateTime`) so với
`LocalDateTime.now()`; không cần thêm method nào vào `Trip`.

Tạo class mới `model/dto/BookingDraft`. Đây là bản nháp booking, lưu trong session giữa
các bước đặt vé, nên phải `implements Serializable`:

```java
public class BookingDraft implements Serializable {
    private Long tripId;
    private int passengerCount;
    private List<Long> tripSeatIds;   // thứ tự = thứ tự hành khách ở T11
    // T11 thêm: List<PassengerDraft> passengers
}
```

Tạo class mới `service/BookingDraftStore` (`@Component`). Class này gói việc đọc, ghi, xóa
bản nháp trong session, để T11, T12 dùng chung một cách:

```java
Optional<BookingDraft> find(HttpSession session);
void save(HttpSession session, BookingDraft draft);
void clear(HttpSession session);
```

Tạo các DTO cho API, là `record` trong `model/response/`:

```java
record SeatMapSeat(Long id, String code, Integer cabinNumber,
        String status) { }   // id là tripSeatId; status AVAILABLE | BOOKED

record SeatMapCoach(int coachNumber, CoachType coachType, long ticketPrice,
        List<SeatMapSeat> seats) { }

record SeatMap(Long tripId, String trainCode, List<SeatMapCoach> coaches) { }
```

Cấu trúc này cố ý giống `TrainLayout` của T06: mỗi chỗ có `code`, `cabinNumber`, thêm `id`
(của dòng `trip_seats`) và `status`. Nhờ vậy hàm vẽ `TrainGoSeatMap.render` của T06 dùng lại được
mà không phải sửa.

### Bước 2. Repository: đọc chỗ của chuyến

Thêm vào `repository/TripSeatRepository`:

```java
List<TripSeat> findByTripIdOrderByCoachNumberAscSeatCodeAsc(Long tripId);

long countByIdInAndTripIdAndStatus(Collection<Long> ids, Long tripId,
        TripSeatStatus status);
```

- `findByTripId...`: lấy mọi chỗ của chuyến (đã kèm số toa, loại toa, mã chỗ, phụ thu), để
  vẽ sơ đồ.
- `countByIdInAndTripIdAndStatus`: đếm trong các id khách gửi lên có bao nhiêu chỗ thật sự
  thuộc chuyến này **và** đang `AVAILABLE`. Số đếm nhỏ hơn số id nghĩa là có id sai chuyến,
  không tồn tại, hoặc chỗ đã bán.

Lưu ý: kiểm tra ở đây chỉ là kiểm tra sớm để khách không phải nhập hành khách vô ích. Giữa
lúc bấm "Tiếp tục" và lúc thanh toán, người khác vẫn có thể mua chỗ đó. Việc chặn hai người
cùng một ghế là của câu `UPDATE` có điều kiện ở T12, không phải ở đây.

### Bước 3. Service: kiểm tra lựa chọn và dựng sơ đồ

Tạo `service/SeatSelectionService`.

```java
@Transactional(readOnly = true)
public BookingDraft select(long tripId, List<Long> tripSeatIds, int passengers) { ... }
```

Method `select` làm bốn bước:

1. Tìm chuyến. Không thấy thì trả 404. Chuyến không `SCHEDULED` hoặc đã khởi hành thì ném
   `TripNotBookableException`.
2. Kiểm tra `tripSeatIds` không trùng nhau, số lượng bằng `passengers`, và trong khoảng
   1-10. Sai thì ném `InvalidSeatSelectionException`.
3. Gọi `countByIdInAndTripIdAndStatus(...)`. Nếu số đếm khác `tripSeatIds.size()` thì ném
   `SeatsUnavailableException`.
4. Trả `BookingDraft` mới (`tripId`, `passengerCount`, `tripSeatIds`). Method này **không ghi**
   database; controller lưu bản nháp vào session.

Ba exception `TripNotBookableException`, `InvalidSeatSelectionException`,
`SeatsUnavailableException` đặt ở `common/exception/`, như mọi exception nghiệp vụ theo
ADR 0006. T12 dùng lại `SeatsUnavailableException` và `TripNotBookableException`.

Nếu khách đã có bản nháp của cùng chuyến với cùng số hành khách (ví dụ quay lại chọn chỗ vì
thanh toán báo chỗ vừa bị đặt), controller giữ lại danh sách hành khách đã nhập và gán lại
theo thứ tự cho các chỗ mới, để khách không phải gõ lại (xem T11). Khác chuyến hoặc khác số
hành khách thì bản nháp mới thay hẳn bản cũ.

Tạo `service/SeatMapService`:

- `@Transactional(readOnly = true) SeatMap getSeatMap(long tripId)`: lấy các `TripSeat` bằng
  câu truy vấn ở Bước 2, gom theo `coachNumber` thành các toa (`ticketPrice` =
  `trip.getBasePrice()` + `priceModifier` của toa), rồi đặt `status` cho từng chỗ đúng bằng
  trạng thái trong database (`AVAILABLE` hoặc `BOOKED`). Không có tính toán theo giờ hay theo
  người xem.

### Bước 4. Controller: trang chọn chỗ và API

Tạo `controller/SeatSelectionController` (`@Controller`):

| Method | Đường dẫn | Việc |
| --- | --- | --- |
| GET | `/booking/{tripId}/seats?passengers=N` | Trang `booking/seats`: thông tin chuyến, số khách, dải tab toa, khung chứa sơ đồ. Nếu có bản nháp cùng chuyến thì truyền sẵn các id đã chọn để trang chọn lại |
| POST | `/booking/{tripId}/seats` | Nhận `passengers` và danh sách `tripSeatIds`, gọi `select(...)`, lưu `BookingDraft` bằng `BookingDraftStore`, chuyển sang `/booking/passengers`. Lỗi thì chuyển về trang chọn chỗ kèm flash `errorMessage` |

Tạo `controller/SeatMapApiController` (`@RestController`):

| Method | Đường dẫn | Trả về |
| --- | --- | --- |
| GET | `/api/trips/{tripId}/seat-map` | JSON `SeatMap` của chuyến |

`/booking/**` và `/api/trips/**` chỉ dành cho CUSTOMER (T02 đã khai quyền). Task này không cần
biết khách là ai để dựng sơ đồ, vì sơ đồ giống nhau với mọi người.

### Bước 5. Giao diện và JavaScript

- `templates/booking/seats.html` (khung khách) gồm:
  - thanh các bước (stepper) `Chuyến → Chỗ → Hành khách → Xem lại → Thanh toán → Vé`,
    đang ở "Chỗ";
  - tóm tắt chuyến;
  - dải tab toa: một tab cho mỗi toa ("Toa 1 · Ghế ngồi"...), mỗi tab có số chỗ còn trống
    của toa và số chỗ khách đã chọn ở toa đó;
  - chú thích màu: trống, đang chọn, đã bán;
  - bộ đếm "Đã chọn 1/2";
  - một form `th:action` chứa ô ẩn `passengers` và nút "Tiếp tục". Nút bị khóa cho tới khi
    chọn đủ số chỗ.
- `static/js/booking-seats.js` làm năm việc:
  1. Gọi `fetch('/api/trips/{id}/seat-map')`, kiểm tra `response.ok`. Nhận 401 nghĩa là
     phiên đăng nhập đã hết.
  2. Chỉ vẽ **toa đang chọn** bằng `TrainGoSeatMap.render(container, [coach], { onSeatClick })`
     (truyền mảng một toa). Bấm tab thì vẽ lại toa tương ứng. Tập hợp id chỗ đã chọn giữ ở một
     biến riêng của trang, không giữ trong DOM, nên chuyển toa không làm mất lựa chọn.
  3. Trong `onSeatClick`: chỉ cho chọn chỗ `AVAILABLE`, bật tắt class `tg-seat--selected`,
     không cho chọn quá N chỗ. Cập nhật bộ đếm và số chỗ đã chọn trên từng tab.
  4. Chọn sẵn các id mà server truyền xuống (từ bản nháp), nhưng chỉ những chỗ vẫn
     `AVAILABLE` trong sơ đồ vừa tải. Chỗ nào đã bán thì bỏ và báo "Chỗ ... vừa được người
     khác đặt, vui lòng chọn chỗ khác."
  5. Lúc gửi form, thêm vào form một `<input type="hidden" name="tripSeatIds">` cho mỗi chỗ
     đã chọn.
- Không có `hold-countdown.js` và không có trạng thái `HELD` hay `MINE` trong
  `seat-map.js`. Nếu bản `seat-map.js` của T06 còn các class đó thì để nguyên, T09 không dùng.
- CSS màu trạng thái chỗ cho trang khách đặt trong `app.css`, dùng các class
  `tg-seat--...` mà T06 sinh ra, và thêm style cho dải tab toa.

## 5. Luật và lỗi phải xử lý

- Chuyến đã hủy hoặc đã khởi hành: không vào được trang chọn chỗ, quay về trang chi tiết
  chuyến kèm lý do.
- Gửi sai số chỗ (ví dụ tự sửa HTML): từ chối, bản nháp không đổi.
- Chỗ đã `BOOKED` hoặc không thuộc chuyến: từ chối với thông báo "Một số chỗ bạn chọn không
  còn trống. Vui lòng chọn lại."
- Backend không tin trạng thái chỗ do trình duyệt gửi lên; chỉ tin kết quả đếm từ database.
- Đây chỉ là kiểm tra sớm. Chỗ vẫn có thể bị người khác mua trước khi khách thanh toán, và
  khi đó T12 báo lỗi.

## 6. Kịch bản thử bằng tay

Dự án không viết test tự động (ADR 0007). Chọn chỗ là bước đầu của nghiệp vụ rủi ro nhất,
nên bạn phải tự thử đủ các kịch bản dưới đây trên ứng dụng đang chạy. Mở database bằng:

```
docker exec -it traingo-mysql mysql -utraingo -ptraingo --default-character-set=utf8mb4 traingo
```

Chuẩn bị hai trình duyệt (hoặc một cửa sổ thường và một cửa sổ ẩn danh), mỗi bên đăng
nhập một tài khoản khách khác nhau, ví dụ `an.nguyen@example.com` (khách A) và
`binh.tran@example.com` (khách B), mật khẩu `Customer@123`.

1. **Chọn chỗ không ghi gì vào database.** Khách A chọn 2 chỗ trên một chuyến còn bán, bấm
   "Tiếp tục" và dừng ở trang hành khách. Chạy
   `SELECT status, COUNT(*) FROM trip_seats WHERE trip_id = <id chuyến> GROUP BY status;`:
   chỉ có `AVAILABLE` và `BOOKED`, số lượng không đổi so với trước khi chọn. Bảng `bookings`
   cũng không có dòng mới.
2. **Một toa một lần, lựa chọn giữ qua các toa.** Chọn 1 chỗ ở toa 1, bấm tab toa 3, chọn
   1 chỗ nữa. Bộ đếm báo "Đã chọn 2/2", tab toa 1 và toa 3 mỗi tab hiện 1 chỗ đã chọn. Quay
   lại toa 1: chỗ đã chọn vẫn được tô màu "đang chọn".
3. **Không chọn quá số hành khách.** Với 2 khách, bấm chỗ thứ ba: không chọn thêm được. Chỉ
   chọn 1 chỗ: nút "Tiếp tục" bị khóa.
4. **Hai người cùng thấy một chỗ trống.** Khách B mở cùng chuyến: các chỗ A đang chọn vẫn hiện
   là trống (đúng thiết kế, chưa ai khóa). Chỗ chỉ biết bị tranh khi thanh toán (kịch bản của
   T12).
5. **Chỗ đã bán không chọn được.** Đổi một chỗ thành đã bán:
   `UPDATE trip_seats SET status = 'BOOKED' WHERE id = <id>;`. Tải lại trang: chỗ đó hiện
   màu "đã bán" và không bấm được. Để thử phía backend, mở DevTools, sửa giá trị một ô ẩn
   `tripSeatIds` thành id chỗ đó rồi gửi form: bị từ chối với thông báo "Một số chỗ bạn chọn
   không còn trống", bản nháp không đổi.
6. **Chỗ của chuyến khác bị từ chối.** Sửa ô ẩn `tripSeatIds` thành id một chỗ của chuyến
   khác rồi gửi: bị từ chối như kịch bản 5.
7. **Chuyến đã hủy bị chặn.** Dữ liệu mẫu không có chuyến đã hủy, nên tạm hủy một chuyến chưa
   có booking `CONFIRMED`, ví dụ chuyến Đà Nẵng → Huế của SE1:
   `UPDATE trips SET status = 'CANCELLED' WHERE id = <id chuyến>;`. Vào trang chọn chỗ của chuyến
   đó: bị đưa về trang chi tiết chuyến kèm lý do. Làm thêm với một chuyến đã khởi hành (trạng
   thái `COMPLETED`, ở đầu lịch của SE1): cùng kết quả.
8. **Sai số chỗ bị từ chối.** Chọn 2 khách nhưng xóa bớt một ô ẩn `tripSeatIds` trước khi
   gửi: bị từ chối, không sang trang hành khách.
9. **Quay lại chọn chỗ.** Khách A đã chọn 2 chỗ, sang trang hành khách, bấm quay lại trang
   chọn chỗ: 2 chỗ cũ được chọn sẵn (nếu vẫn trống). Đổi sang 2 chỗ khác rồi "Tiếp tục": bản
   nháp có 2 chỗ mới.

Sau khi thử xong, trả dữ liệu đã sửa tay về như cũ, ví dụ
`UPDATE trip_seats SET status = 'AVAILABLE' WHERE id = <id>;` cho chỗ đã đổi ở bước 5, và
`UPDATE trips SET status = 'SCHEDULED' WHERE id = <id chuyến>;` cho chuyến đã hủy ở bước 7.

## 7. Lỗi hay gặp

- Ghi `HELD` hoặc ghi bất cứ thứ gì vào database ở bước này: nếu khách bỏ ngang thì chỗ bị
  khóa vô lý. Task này chỉ ghi vào session.
- Tin kết quả kiểm tra ở đây là đủ: người khác vẫn mua được chỗ ngay sau đó. Chặn trùng là việc
  của T12.
- Dùng mã chỗ (`A01`) làm định danh trong JSON thay vì `id` của `trip_seats`: cùng mã chỗ
  xuất hiện ở mọi chuyến, và mã chỉ không trùng trong phạm vi một toa của một chuyến, nên
  phải dùng `id` của `trip_seats`.
- Giữ danh sách chỗ đã chọn trong DOM của toa đang vẽ: chuyển toa là vẽ lại, lựa chọn mất.
  Giữ ở một biến riêng.
- Gắn `onclick` trực tiếp cho từng ghế: mỗi lần vẽ lại sơ đồ, các listener đó mất theo.
  Hãy truyền `onSeatClick` vào hàm vẽ như ở Bước 5.

## 8. Kiểm tra trước khi báo xong

- [ ] Khách A chọn 2 chỗ (có thể ở hai toa), bấm "Tiếp tục": sang trang hành khách, database
      không có dòng nào đổi.
- [ ] Trang chọn chỗ hiện một toa một lần, có dải tab; chuyển toa không làm mất chỗ đã chọn.
- [ ] Chỗ `BOOKED` hiện màu "đã bán", không bấm được; cố gửi form chứa chỗ đó thì bị từ chối.
- [ ] Chuyến đã hủy (hủy tạm bằng SQL ở kịch bản 7) và chuyến đã khởi hành không vào được
      trang chọn chỗ.
- [ ] Không còn `HELD`, `MINE`, đồng hồ đếm ngược hay lời gọi giữ chỗ nào trong code của task.
- [ ] Đã thử hết các kịch bản ở mục 6.
- [ ] Trang hiển thị tốt ở 375px.
- [ ] `.\mvnw.cmd compile` chạy không lỗi và ứng dụng khởi động được
      (`.\mvnw.cmd spring-boot:run`).
- [ ] Có một dòng trong `docs/WORKLOG.md`, nằm trong cùng commit với code.
