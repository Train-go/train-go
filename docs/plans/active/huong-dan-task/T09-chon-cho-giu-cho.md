# T09 Chọn chỗ và giữ chỗ

> **Tóm tắt:** sau task này, khách chọn chỗ trên sơ đồ toa, bấm "Tiếp tục" là các chỗ đó
> được giữ riêng cho khách trong 5 phút, và không ai khác lấy được.

| Đợt | Phần việc | Độ khó | Cần xong trước | Task đang chờ task này | Tên branch |
| --- | --- | --- | --- | --- | --- |
| 2 | B | Khó (khoảng 4-5 ngày) | T02, T05, T06, quyết định D1, D6, D7 | T11, T12 | `feat/t09-seat-holding` |

Các từ như transaction, session, CSRF token được giải thích trong `README.md` cùng thư mục.
Cách chia package theo tầng (`controller/`, `service/`, `model/`...) xem ADR 0006.

## 1. Task này làm gì

Luồng của khách:

1. Ở trang chi tiết chuyến (T05), khách bấm "Chọn chỗ".
2. Khách chọn toa, rồi bấm vào sơ đồ đúng bằng số hành khách.
3. Khách bấm "Tiếp tục". Hệ thống **giữ** các chỗ đó trong 5 phút (`SPEC.md` mục 18), rồi
   chuyển sang bước nhập hành khách (T11).

Đây là nghiệp vụ rủi ro nhất của dự án. Hai người **không bao giờ** được giữ cùng một chỗ;
nếu để lọt, hai khách sẽ cùng trả tiền cho một ghế.

### Ba quy ước chung của task này

Ba quyết định dưới đây Phi đã chốt ngày 12/10/2026 (bảng "Quyết định nghiệp vụ" trong
`chia-viec-mvp.md`):

- **D1, trả lại chỗ hết hạn bằng cách kiểm tra khi cần.** Chỗ `HELD` (đang giữ) mà
  `heldUntil` đã qua thì được coi như còn trống ở mọi nơi: khi đếm chỗ trống, khi vẽ sơ
  đồ, và khi người khác muốn giữ. Vì vậy luật vẫn đúng mà không cần chương trình chạy nền
  để dọn. Không viết job dọn dẹp: nếu quên điều kiện so giờ ở một chỗ đọc trạng thái, job
  sẽ che lỗi đó thay vì để nó lộ ra lúc thử.
- **D6.** Mỗi lần giữ từ 1 đến 10 chỗ, đúng bằng số hành khách.
- **D7, giữ chỗ lúc bấm "Tiếp tục".** Bấm vào một chỗ chỉ đánh dấu `SELECTED` trên trình
  duyệt, không lưu gì vào database (`SPEC.md` mục 17). Bấm "Tiếp tục" thì giữ tất cả chỗ
  đã chọn trong một transaction, rồi lưu bản nháp booking vào session.

## 2. Phạm vi

Làm:

- Trang chọn chỗ.
- API trả trạng thái từng chỗ.
- Giữ chỗ, và trả lại chỗ hết hạn theo D1.
- Bản nháp booking trong session (D7).
- Script đếm ngược dùng chung cho các bước sau.

Không làm:

- Nhập hành khách: thuộc T11.
- Thanh toán: thuộc T12.
- WebSocket (cập nhật tức thời qua kết nối mở sẵn): `SPEC.md` mục 18 không bắt buộc.

## 3. Dữ liệu dùng tới

```
trips 1 ──< trip_seats >── 1 seats >── 1 coaches
                │
                └── held_by_user_id ──> users

trip_seats
  status           AVAILABLE | HELD | BOOKED
  held_by_user_id  bắt buộc khi HELD     CHECK ck_trip_seats_hold
  held_until       bắt buộc khi HELD
  UNIQUE (trip_id, seat_id)
```

Đọc sơ đồ:

- Mỗi chuyến có một dòng `trip_seats` cho mỗi ghế của tàu.
- Một chỗ có ba trạng thái: `AVAILABLE` (trống), `HELD` (đang có người giữ), `BOOKED`
  (đã bán).
- Khi chỗ là `HELD`, database bắt buộc phải có người giữ (`held_by_user_id`) và hạn giữ
  (`held_until`). Thiếu một trong hai thì database từ chối ghi.

Các thay đổi trạng thái mà task này phụ trách:

```
AVAILABLE --giữ--> HELD
HELD (đã hết hạn) --giữ đè--> HELD (người mới)
HELD --người đó chọn lại--> AVAILABLE
```

## 4. Các bước làm

### Bước 1. Model: bản nháp booking, nơi lưu bản nháp, và DTO

Entity `entity/TripSeat`, `entity/TripSeatStatus`, `entity/Trip` đã có. Task này cần method
`departureAt()` của `Trip` (mô tả ở T07); task nào làm trước thì thêm.

Tạo class mới `model/dto/BookingDraft`. Đây là bản nháp booking, lưu trong session giữa
các bước đặt vé, nên phải `implements Serializable`:

```java
public class BookingDraft implements Serializable {
    private Long tripId;
    private int passengerCount;
    private List<Long> tripSeatIds;   // thứ tự = thứ tự hành khách ở T11
    private LocalDateTime heldUntil;
    // T11 thêm: List<PassengerDraft> passengers
    // T11 thêm: Long bookingId (booking PENDING đã tạo)
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
record SeatMapSeat(Long id, String code, SeatType type, Integer cabinNumber,
        String status) { }   // id là tripSeatId; status AVAILABLE|HELD|BOOKED|MINE

record SeatMapCoach(int coachNumber, CoachType coachType, long ticketPrice,
        List<SeatMapSeat> seats) { }

record SeatMap(Long tripId, String trainCode, LocalDateTime heldUntil,
        List<SeatMapCoach> coaches) { }
```

Cấu trúc này cố ý giống `TrainLayout` của T06: mỗi ghế có `id`, `code`, `type`,
`cabinNumber`, thêm `status`. Nhờ vậy hàm vẽ `TrainGoSeatMap.render` của T06 dùng lại được
mà không phải sửa.

### Bước 2. Repository: giữ chỗ bằng một câu UPDATE

Thêm vào `repository/TripSeatRepository`:

```java
@EntityGraph(attributePaths = {"seat", "seat.coach"})
List<TripSeat> findByTripIdOrderBySeatCoachCoachNumberAscSeatCodeAsc(Long tripId);

/** Holds the seats that are free now. Returns how many rows it changed. */
@Modifying(flushAutomatically = true, clearAutomatically = true)
@Query("""
    update TripSeat ts
    set ts.status = :held, ts.heldBy = :user, ts.heldUntil = :until
    where ts.id in :ids and ts.trip.id = :tripId
      and (ts.status = :available
           or (ts.status = :held and ts.heldUntil < :now))""")
int holdIfFree(Collection<Long> ids, Long tripId, User user,
        LocalDateTime until, LocalDateTime now,
        TripSeatStatus held, TripSeatStatus available);

@Modifying(flushAutomatically = true, clearAutomatically = true)
@Query("""
    update TripSeat ts
    set ts.status = :available, ts.heldBy = null, ts.heldUntil = null
    where ts.heldBy.id = :userId and ts.status = :held""")
int releaseHeldBy(Long userId, TripSeatStatus held, TripSeatStatus available);
```

- `findByTripId...`: lấy mọi chỗ của chuyến, kèm ghế và toa, để vẽ sơ đồ.
- `holdIfFree`: chuyển sang `HELD` những chỗ **đang trống** (hoặc `HELD` đã hết hạn), rồi
  trả về số dòng đã đổi.
- `releaseHeldBy`: trả về `AVAILABLE` mọi chỗ mà một người đang giữ.

**Vì sao giữ chỗ bằng một câu `UPDATE` có điều kiện, thay vì "đọc rồi ghi".** Thử hình
dung cách làm "đọc rồi ghi": đọc trạng thái ghế A05, thấy `AVAILABLE`, kiểm tra trong Java,
rồi mới ghi `HELD`. Nếu khách A và khách B bấm "Tiếp tục" gần như cùng lúc, cả hai cùng
đọc thấy A05 trống, và cả hai cùng ghi `HELD`. Kết quả: hai người giữ cùng một ghế.

Với một câu `UPDATE ... WHERE status = AVAILABLE`, việc kiểm tra và việc ghi diễn ra một
lần, không bị chen ngang. MySQL khóa từng dòng đang sửa: request của B phải đợi request
của A commit xong, rồi mới đọc lại A05, thấy đã là `HELD`, nên không đổi dòng đó. Số dòng
trả về nhỏ hơn số chỗ yêu cầu là biết có chỗ đã bị người khác lấy mất.

### Bước 3. Service: giữ chỗ và dựng sơ đồ

Tạo `service/SeatHoldService`.

```java
@Transactional
public HoldResult hold(long tripId, List<Long> tripSeatIds, int passengers,
        long userId) { ... }
```

Method `hold` làm sáu bước:

1. Tìm chuyến. Không thấy thì trả 404. Chuyến không `SCHEDULED` hoặc đã khởi hành thì ném
   `TripNotBookableException`.
2. Kiểm tra `tripSeatIds` không trùng nhau, số lượng bằng `passengers`, và trong khoảng
   1-10. Sai thì ném `InvalidSeatSelectionException`.
3. Tính thời gian: `now = LocalDateTime.now()`, `until = now.plusMinutes(5)`.
4. Gọi `releaseHeldBy(userId, ...)` để bỏ những chỗ người này đang giữ từ lần chọn trước.
5. Gọi `changed = holdIfFree(...)`. Nếu `changed != tripSeatIds.size()` thì ném
   `SeatsUnavailableException`. Khi đó transaction bị hủy toàn bộ, kể cả bước 4: người
   dùng vẫn giữ những chỗ cũ, và không có chỗ mới nào bị giữ dở dang.
6. Trả `HoldResult(tripId, tripSeatIds, until)`.

Ba exception `TripNotBookableException`, `InvalidSeatSelectionException`,
`SeatsUnavailableException` đặt ở `common/exception/`, như mọi exception nghiệp vụ theo
ADR 0006. `HoldResult` là một `record` trong `model/dto/`.

Tham số `User` của `holdIfFree` lấy bằng `userRepository.getReferenceById(userId)`. Cách
này chỉ tạo một tham chiếu tới người dùng, không cần chạy thêm câu `SELECT`.

Tạo `service/SeatMapService`:

- `@Transactional(readOnly = true) SeatMap getSeatMap(long tripId, long userId)`: lấy
  các `TripSeat` bằng câu truy vấn có `@EntityGraph` ở Bước 2, rồi tính `status` cho từng
  chỗ:
  - `BOOKED` nếu đã bán;
  - `MINE` nếu đang `HELD`, còn hạn, và người giữ chính là người đang xem;
  - `HELD` nếu đang `HELD` và còn hạn;
  - còn lại là `AVAILABLE`, kể cả chỗ `HELD` đã hết hạn (theo D1).

  Gọi `ts.getHeldBy().getId()` không làm Hibernate đọc thêm dữ liệu người dùng, nên so id
  như vậy là an toàn.

### Bước 4. Controller: trang chọn chỗ và API

Tạo `controller/SeatSelectionController` (`@Controller`):

| Method | Đường dẫn | Việc |
| --- | --- | --- |
| GET | `/booking/{tripId}/seats?passengers=N` | Trang `booking/seats`: thông tin chuyến, số khách, khung chứa sơ đồ |
| POST | `/booking/{tripId}/seats` | Nhận `passengers` và danh sách `tripSeatIds`, gọi `hold(...)` với `me.getId()`, lưu `BookingDraft`, chuyển sang `/booking/passengers`. Lỗi thì chuyển về trang chọn chỗ kèm flash `errorMessage` |

Tạo `controller/SeatMapApiController` (`@RestController`):

| Method | Đường dẫn | Trả về |
| --- | --- | --- |
| GET | `/api/trips/{tripId}/seat-map` | JSON `SeatMap` cho người đang đăng nhập |

Người đang đăng nhập lấy bằng `@AuthenticationPrincipal TrainGoUserDetails me` (class
`service/TrainGoUserDetails` do T02 tạo), **không bao giờ** lấy từ tham số request. Vì sao:
tham số request do trình duyệt gửi lên, ai cũng sửa được. `/booking/**` và `/api/trips/**`
chỉ dành cho CUSTOMER.

### Bước 5. Giao diện và JavaScript

- `templates/booking/seats.html` (khung khách) gồm:
  - thanh các bước (stepper) `Chuyến → Chỗ → Hành khách → Xem lại → Thanh toán → Vé`,
    đang ở "Chỗ";
  - tóm tắt chuyến;
  - chú thích màu: trống, đang chọn, người khác giữ, đã bán;
  - bộ đếm "Đã chọn 1/2";
  - một form `th:action` chứa ô ẩn `passengers` và nút "Tiếp tục". Nút bị khóa cho tới khi
    chọn đủ số chỗ.
- `static/js/booking-seats.js` làm năm việc:
  1. Gọi `fetch('/api/trips/{id}/seat-map')`, kiểm tra `response.ok`. Nhận 401 nghĩa là
     phiên đăng nhập đã hết.
  2. Gọi `TrainGoSeatMap.render(container, seatMap.coaches, { onSeatClick })` để vẽ.
  3. Trong `onSeatClick`: chỉ cho chọn chỗ `AVAILABLE` hoặc `MINE`, bật tắt class
     `tg-seat--selected`, không cho chọn quá N chỗ.
  4. Cứ 15 giây tải lại sơ đồ một lần. Chỗ đang chọn mà vừa bị người khác giữ thì bỏ chọn
     và báo cho khách.
  5. Lúc gửi form, thêm vào form một `<input type="hidden" name="tripSeatIds">` cho mỗi chỗ
     đã chọn.
- `static/js/hold-countdown.js`: T09 là chủ, T11 và T12 dùng lại. Phần tử nào có
  `data-held-until` (thời điểm dạng ISO, giờ Việt Nam) thì hiện `mm:ss` còn lại. Về 0 thì
  hiện "Hết thời gian giữ chỗ" và khóa các nút gửi trong form gần nhất.
- CSS màu trạng thái chỗ cho trang khách đặt trong `app.css`, dùng các class
  `tg-seat--...` mà T06 sinh ra.

## 5. Luật và lỗi phải xử lý

- Chỗ vừa bị người khác giữ: báo "Một số chỗ bạn chọn vừa có người giữ. Vui lòng chọn
  lại."
- Chuyến đã hủy hoặc đã khởi hành: không vào được trang chọn chỗ, quay về trang chi tiết
  chuyến kèm lý do.
- Gửi sai số chỗ (ví dụ tự sửa HTML): từ chối, không giữ chỗ nào.
- Backend không tin trạng thái chỗ do trình duyệt gửi lên; chỉ tin kết quả của
  `holdIfFree`.

## 6. Kịch bản thử bằng tay

Dự án không viết test tự động (ADR 0007). Giữ chỗ là nghiệp vụ rủi ro nhất, nên bạn phải
tự thử đủ các kịch bản dưới đây trên ứng dụng đang chạy. Mở database bằng:

```
docker exec -it traingo-mysql mysql -utraingo -ptraingo --default-character-set=utf8mb4 traingo
```

Chuẩn bị hai trình duyệt (hoặc một cửa sổ thường và một cửa sổ ẩn danh), mỗi bên đăng
nhập một tài khoản khách khác nhau, ví dụ `an.nguyen@example.com` (khách A) và
`binh.tran@example.com` (khách B), mật khẩu `Customer@123`.

1. **Giữ chỗ thành công.** Khách A chọn 2 chỗ trên một chuyến còn bán, bấm "Tiếp tục".
   Chạy `SELECT id, status, held_by_user_id, held_until FROM trip_seats WHERE status = 'HELD';`:
   có 2 dòng, `held_by_user_id` là id của A, `held_until` sau hiện tại khoảng 5 phút.
2. **Hai người không giữ được cùng một chỗ.** Khách B mở cùng chuyến: hai chỗ của A hiện
   màu "người khác giữ" và không bấm được. Để thử phía backend, ở trình duyệt của B mở
   DevTools, sửa giá trị một ô ẩn `tripSeatIds` thành id một chỗ A đang giữ rồi gửi form:
   B bị từ chối với thông báo "Một số chỗ bạn chọn vừa có người giữ", database không đổi.
3. **Chỗ hết hạn được người khác giữ.** Không cần chờ 5 phút; đẩy hạn giữ của A về quá khứ:
   `UPDATE trip_seats SET held_until = NOW() - INTERVAL 1 MINUTE WHERE status = 'HELD' AND held_by_user_id = <id của A>;`.
   Tải lại trang của B: hai chỗ đó hiện là trống. B chọn và giữ được; chạy lại câu
   `SELECT` ở bước 1 thấy `held_by_user_id` đã là id của B.
4. **Chỗ đã bán không giữ được.** Đổi một chỗ thành đã bán:
   `UPDATE trip_seats SET status = 'BOOKED' WHERE id = <id>;`. Chỗ đó hiện màu "đã bán";
   cố gửi form chứa id đó (sửa ô ẩn như bước 2) thì bị từ chối.
5. **Chuyến đã hủy bị chặn.** Vào trang chọn chỗ của chuyến SP1 đã hủy trong seed: bị đưa
   về trang chi tiết chuyến kèm lý do.
6. **Sai số chỗ bị từ chối.** Chọn 2 khách nhưng xóa bớt một ô ẩn `tripSeatIds` trước khi
   gửi: bị từ chối, bảng `trip_seats` không có dòng `HELD` mới.
7. **Chọn lại thì bỏ chỗ cũ.** Khách A giữ 2 chỗ, quay lại trang chọn chỗ, chọn 2 chỗ khác
   rồi bấm "Tiếp tục": hai chỗ cũ về `AVAILABLE`, chỉ hai chỗ mới là `HELD`.

Sau khi thử xong, trả dữ liệu đã sửa tay về như cũ, ví dụ
`UPDATE trip_seats SET status = 'AVAILABLE' WHERE id = <id>;` cho chỗ đã đổi ở bước 4.

## 7. Lỗi hay gặp

- `@Modifying` thiếu `clearAutomatically`: các entity đã đọc trước đó trong cùng
  transaction vẫn mang trạng thái cũ, code phía sau đọc sai.
- Dùng `id` của bảng `seats` trong JSON thay vì `id` của `trip_seats`: một ghế vật lý có
  nhiều `TripSeat`, mỗi chuyến một dòng, nên phải dùng `id` của `trip_seats`.
- Gắn `onclick` trực tiếp cho từng ghế: mỗi lần vẽ lại sơ đồ, các listener đó mất theo.
  Hãy truyền `onSeatClick` vào hàm vẽ như ở Bước 5.

## 8. Kiểm tra trước khi báo xong

- [ ] Khách A chọn 2 chỗ, bấm "Tiếp tục": database có 2 dòng `HELD`, `held_by_user_id` là
      của A, `held_until` sau 5 phút.
- [ ] Khách B (ở trình duyệt khác) thấy hai chỗ đó màu "người khác giữ", không bấm được; cố
      gửi form chứa chỗ đó thì bị từ chối.
- [ ] Sau khi hạn giữ đã qua (đẩy `held_until` về quá khứ bằng SQL), B giữ được chỗ đó.
- [ ] Chuyến SP1 bị hủy trong dữ liệu mẫu (seed) không vào được trang chọn chỗ.
- [ ] Đã thử hết các kịch bản ở mục 6.
- [ ] Trang hiển thị tốt ở 375px.
- [ ] `.\mvnw.cmd compile` chạy không lỗi và ứng dụng khởi động được
      (`.\mvnw.cmd spring-boot:run`).
- [ ] Có một dòng trong `docs/WORKLOG.md`, nằm trong cùng commit với code.
