# T09 Chọn chỗ và giữ chỗ

| Wave | Lane | Cỡ | Phụ thuộc | Mở khóa | Branch |
| --- | --- | --- | --- | --- | --- |
| 2 | B | L | T02, T05, T06, D1, D6, D7 | T11, T12 | `feat/t09-seat-holding` |

## Mục tiêu

Từ trang chi tiết chuyến (T05), khách bấm "Chọn chỗ", chọn toa, bấm vào đúng số chỗ
bằng số hành khách trên sơ đồ, rồi bấm "Tiếp tục". Hệ thống **giữ** các chỗ đó trong
5 phút (`SPEC.md` mục 18) và chuyển sang bước nhập hành khách (T11).

Đây là nghiệp vụ rủi ro nhất của dự án: hai người không bao giờ được giữ cùng một chỗ.

## Phạm vi

Trong phạm vi: trang chọn chỗ, API trạng thái chỗ, giữ chỗ, thu hồi chỗ hết hạn (D1),
bản nháp booking trong session (D7), script đếm ngược dùng chung.

Ngoài phạm vi: nhập hành khách (T11), thanh toán (T12), WebSocket (`SPEC.md` mục 18
không bắt buộc).

## Hợp đồng đã chốt cho task này

Ba quyết định, đề xuất trong plan, phải được Phi chốt trước khi bắt đầu:

- **D1, thu hồi chỗ hết hạn bằng kiểm tra lười.** Chỗ `HELD` có `heldUntil` đã qua được
  coi là trống ở mọi nơi: đếm chỗ trống, vẽ sơ đồ, và được phép giữ đè. Không cần job
  chạy nền để luật đúng; job dọn dẹp là tùy chọn.
- **D6.** Mỗi lần giữ từ 1 đến 10 chỗ, đúng bằng số hành khách.
- **D7, giữ chỗ khi bấm "Tiếp tục".** Bấm vào một chỗ chỉ là `SELECTED` trên trình duyệt,
  không lưu gì (`SPEC.md` mục 17). Bấm "Tiếp tục" thì giữ tất cả chỗ đã chọn trong một
  transaction, rồi lưu bản nháp booking vào session.

## Database

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

Chuyển trạng thái do task này phụ trách:

```
AVAILABLE --giữ--> HELD
HELD (đã hết hạn) --giữ đè--> HELD (người mới)
HELD --người đó chọn lại--> AVAILABLE
```

## Model

Có sẵn: `TripSeat`, `TripSeatStatus`, `Trip` (`departureAt()` của T07; ai làm trước thì
thêm).

Viết mới `booking/model/BookingDraft`, lưu trong session (implement `Serializable`):

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

Viết mới `booking/service/BookingDraftStore` (`@Component`), gói việc đọc ghi session
để T11, T12 dùng chung:

```java
Optional<BookingDraft> find(HttpSession session);
void save(HttpSession session, BookingDraft draft);
void clear(HttpSession session);
```

DTO cho API, `record` trong `booking/model/`:

```java
record SeatMapSeat(Long id, String code, SeatType type, Integer cabinNumber,
        String status) { }   // id là tripSeatId; status AVAILABLE|HELD|BOOKED|MINE

record SeatMapCoach(int coachNumber, CoachType coachType, long ticketPrice,
        List<SeatMapSeat> seats) { }

record SeatMap(Long tripId, String trainCode, LocalDateTime heldUntil,
        List<SeatMapCoach> coaches) { }
```

Cấu trúc này cố ý giống `TrainLayout` của T06, để `TrainGoSeatMap.render` dùng lại
được: mỗi ghế có `id`, `code`, `type`, `cabinNumber`, thêm `status`.

## Repository

`TripSeatRepository`:

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

**Vì sao một câu `UPDATE` có điều kiện thay vì "đọc rồi ghi".** Đọc trạng thái, kiểm tra
trong Java rồi mới ghi, thì hai request cùng đọc thấy `AVAILABLE` và cùng ghi `HELD`.
Với một câu `UPDATE ... WHERE status = AVAILABLE`, MySQL khóa từng dòng: request thứ
hai đợi request đầu commit, đọc lại thấy `HELD`, và không đổi dòng đó. Số dòng trả về
nhỏ hơn số chỗ yêu cầu là biết có chỗ đã bị lấy mất.

## Service

`booking/service/SeatHoldService`, nhận `Clock`:

```java
@Transactional
public HoldResult hold(long tripId, List<Long> tripSeatIds, int passengers,
        long userId) { ... }
```

1. Tìm chuyến; không thấy thì 404. Chuyến không `SCHEDULED` hoặc đã khởi hành thì ném
   `TripNotBookableException`.
2. `tripSeatIds` không trùng nhau, số lượng bằng `passengers`, trong khoảng 1-10; sai
   thì ném `InvalidSeatSelectionException`.
3. `now = LocalDateTime.now(clock)`, `until = now.plusMinutes(5)`.
4. `releaseHeldBy(userId, ...)`: bỏ các chỗ người này đang giữ từ lần chọn trước.
5. `changed = holdIfFree(...)`. Nếu `changed != tripSeatIds.size()` thì ném
   `SeatsUnavailableException`. Transaction rollback toàn bộ, kể cả bước 4: người dùng
   vẫn giữ những chỗ cũ, và không có chỗ mới nào bị giữ dở dang.
6. Trả `HoldResult(tripId, tripSeatIds, until)`.

Tham số `User` của `holdIfFree` lấy bằng `userRepository.getReferenceById(userId)`,
không cần `SELECT`.

`booking/service/SeatMapService`:

- `@Transactional(readOnly = true) SeatMap getSeatMap(long tripId, long userId)`: lấy
  `TripSeat` bằng query có `@EntityGraph`, tính `status` cho từng chỗ:
  - `BOOKED` nếu đã đặt;
  - `MINE` nếu `HELD`, còn hạn và `heldBy` là người đang xem;
  - `HELD` nếu `HELD` và còn hạn;
  - còn lại là `AVAILABLE` (kể cả `HELD` đã hết hạn, theo D1).

  `ts.getHeldBy().getId()` không làm Hibernate nạp thêm dữ liệu, nên so id an toàn.

Tùy chọn theo D1: một method `@Scheduled(fixedDelay = 60_000)` chuyển chỗ `HELD` hết hạn
về `AVAILABLE` cho gọn dữ liệu. Luật vẫn đúng khi không có job này.

## Controller

`booking/controller/SeatSelectionController` (`@Controller`):

| Method | URL | Việc |
| --- | --- | --- |
| GET | `/booking/{tripId}/seats?passengers=N` | View `booking/seats`: thông tin chuyến, số khách, ô chứa sơ đồ |
| POST | `/booking/{tripId}/seats` | Nhận `passengers` và danh sách `tripSeatIds`, gọi `hold(...)` với `me.getId()`, lưu `BookingDraft`, `redirect:/booking/passengers`. Lỗi thì redirect về trang chọn chỗ kèm flash `errorMessage` |

`booking/controller/SeatMapApiController` (`@RestController`):

| Method | URL | Trả về |
| --- | --- | --- |
| GET | `/api/trips/{tripId}/seat-map` | JSON `SeatMap` cho người đang đăng nhập |

Người dùng lấy bằng `@AuthenticationPrincipal TrainGoUserDetails me` (T02), không bao giờ
từ tham số request. `/booking/**` và `/api/trips/**` chỉ dành cho CUSTOMER.

## View và JavaScript

- `templates/booking/seats.html` (khung khách): stepper
  `Chuyến → Chỗ → Hành khách → Xem lại → Thanh toán → Vé`, đang ở "Chỗ"; tóm tắt chuyến;
  chú thích màu (trống, đang chọn, người khác giữ, đã đặt); bộ đếm "Đã chọn 1/2"; một
  form `th:action` chứa ô ẩn `passengers` và nút "Tiếp tục" (khóa cho tới khi chọn đủ).
- `static/js/booking-seats.js`:
  1. `fetch('/api/trips/{id}/seat-map')`, kiểm tra `response.ok` (401: phiên hết hạn).
  2. Gọi `TrainGoSeatMap.render(container, seatMap.coaches, { onSeatClick })`.
  3. `onSeatClick` chỉ nhận chỗ `AVAILABLE` hoặc `MINE`, bật tắt class
     `tg-seat--selected`, không cho chọn quá N.
  4. Mỗi 15 giây tải lại sơ đồ. Chỗ đang chọn mà vừa bị người khác giữ thì bỏ chọn và báo.
  5. Lúc submit, thêm vào form một `<input type="hidden" name="tripSeatIds">` cho mỗi chỗ.
- `static/js/hold-countdown.js` (chủ sở hữu: T09, dùng ở T11, T12): phần tử nào có
  `data-held-until` (ISO, giờ Việt Nam) thì hiện `mm:ss` còn lại; về 0 thì hiện "Hết
  thời gian giữ chỗ" và khóa các nút submit trong form gần nhất.
- CSS trạng thái chỗ cho trang khách đặt trong `app.css`, class `tg-seat--...` (T06).

## Quy tắc và lỗi phải xử lý

- Chỗ vừa bị người khác giữ: "Một số chỗ bạn chọn vừa có người giữ. Vui lòng chọn lại."
- Chuyến hủy hoặc đã khởi hành: không vào được trang chọn chỗ, quay về chi tiết chuyến
  kèm lý do.
- Gửi sai số chỗ (sửa HTML bằng tay): từ chối, không giữ gì.
- Backend không tin trạng thái chỗ trình duyệt gửi lên; chỉ tin kết quả `holdIfFree`.

## Test bắt buộc

Theo `AGENTS.md` (giữ chỗ) và ADR 0002:

- `@DataJpaTest` cho `holdIfFree`, dùng `Clock` cố định:
  - chỗ `AVAILABLE` chuyển sang `HELD`, trả 1;
  - chỗ người khác đang giữ còn hạn: không đổi, trả 0;
  - chỗ `HELD` đã hết hạn: giữ đè được, trả 1;
  - chỗ `BOOKED`: không đổi, trả 0.
- Unit test `SeatHoldServiceTest` (Mockito): số chỗ khác số khách bị từ chối; chuyến
  `CANCELLED` bị từ chối; chuyến đã khởi hành bị từ chối; `holdIfFree` trả thiếu thì ném
  `SeatsUnavailableException`.

## Bẫy đã biết

- `@Modifying` thiếu `clearAutomatically` thì entity đã nạp trước đó vẫn mang trạng
  thái cũ trong cùng transaction.
- Ghế trong JSON phải dùng `id` của `trip_seats`, không phải của `seats`: một ghế vật lý
  có nhiều `TripSeat`, mỗi chuyến một dòng.
- Gắn `onclick` cho từng ghế rồi vẽ lại sơ đồ thì listener mất theo. Truyền `onSeatClick`
  vào hàm vẽ như trên.

## Xong khi

- [ ] Khách A chọn 2 chỗ, bấm Tiếp tục: database có 2 dòng `HELD`, `held_by_user_id`
      của A, `held_until` sau 5 phút.
- [ ] Khách B (trình duyệt khác) thấy hai chỗ đó màu "người khác giữ", không bấm được;
      cố gửi form chứa chỗ đó thì bị từ chối.
- [ ] Chờ quá 5 phút, B giữ được chỗ đó.
- [ ] Chuyến SP1 bị hủy trong seed không vào được trang chọn chỗ.
- [ ] Test bắt buộc xanh trong `.\mvnw.cmd test`. Kiểm tra trang ở 375px.
- [ ] Ghi D1, D6, D7 vào `docs/product/booking-flow.md` (mục "Điểm cần quyết").
      Một dòng `docs/WORKLOG.md`.
