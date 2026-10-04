# T05 Tìm chuyến, danh sách chuyến, chi tiết chuyến

| Wave | Lane | Cỡ | Phụ thuộc | Mở khóa | Branch |
| --- | --- | --- | --- | --- | --- |
| 1 | B | M | T01 (bean `Clock`), D6, D8 | T09 (nút "Chọn chỗ") | `feat/t05-trip-search` |

## Mục tiêu

Khách nhập ga đi, ga đến, ngày đi, số hành khách ở landing, bấm "Tìm chuyến" và
thấy danh sách chuyến phù hợp (`SPEC.md` mục 7, 8): mã tàu, ga đi, ga đến, giờ đi,
giờ đến, thời gian di chuyển, số chỗ còn lại, giá thấp nhất. Chọn một chuyến để
xem chi tiết từng toa và đi tiếp sang chọn chỗ.

Không cần đăng nhập để tìm và xem chuyến.

## Phạm vi

Trong phạm vi: landing đọc ga thật từ database, `GET /trips`, `GET /trips/{id}`.

Ngoài phạm vi (`SPEC.md` mục 7): lọc theo giá, loại toa, giờ; sắp xếp nâng cao;
trang chọn chỗ (T09).

## Database

```
stations 1 ──< routes 1 ──< trips >── 1 trains 1 ──< coaches 1 ──< seats
                             │                                      │
                             └──────────< trip_seats >──────────────┘
```

| Bảng | Cột dùng tới |
| --- | --- |
| `stations` | `id`, `code`, `name` |
| `routes` | `origin_station_id`, `destination_station_id` |
| `trips` | `route_id`, `train_id`, `departure_date`, `departure_time`, `arrival_time`, `base_price`, `status` |
| `coaches` | `train_id`, `coach_number`, `coach_type`, `price_modifier` |
| `trip_seats` | `trip_id`, `seat_id`, `status`, `held_until` |

Một chỗ được coi là **còn trống** khi `status = AVAILABLE`, hoặc `status = HELD` mà
`held_until` đã qua (D1).

Một chuyến **đặt được** khi `status = SCHEDULED` và thời điểm khởi hành
(`departure_date` + `departure_time`) còn ở sau hiện tại (D8).

## Model

Thêm vào entity `Trip` method `departureAt()` (ngày + giờ khởi hành), như mô tả ở T07.
T05 làm trước T07 nên thêm ở đây; T07, T09, T10 dùng lại.

Viết mới trong `trip/model/`:

`TripSearchForm`, khớp tên các ô đang có trong form landing:

| Field | Kiểu | Validation |
| --- | --- | --- |
| `from` | `String` (mã ga) | `@NotBlank(message = "Vui lòng chọn ga đi.")` |
| `to` | `String` (mã ga) | `@NotBlank` |
| `date` | `LocalDate` | `@NotNull`, kèm `@DateTimeFormat(iso = ISO.DATE)` |
| `passengers` | `Integer` | `@NotNull`, `@Min(1)`, `@Max(10)` (D6) |

Hai luật còn lại kiểm tra trong service, vì cần nhiều field hoặc cần `Clock`: ga đi
khác ga đến; ngày đi không trước hôm nay.

DTO trả cho view, viết bằng Java `record`:

```java
record TripSummary(Long tripId, String trainCode, String originName,
        String destinationName, LocalDate departureDate, LocalTime departureTime,
        LocalTime arrivalTime, boolean arrivesNextDay, Duration duration,
        long availableSeats, long lowestPrice, boolean enoughSeats) { }

record CoachOffer(int coachNumber, CoachType coachType, long ticketPrice,
        long availableSeats) { }

record TripDetail(TripSummary summary, TripStatus status, boolean bookable,
        List<CoachOffer> coaches) { }
```

## Repository

`RouteRepository`:

```java
Optional<Route> findByOriginStationCodeAndDestinationStationCode(
        String originCode, String destinationCode);
```

`TripRepository`:

```java
@EntityGraph(attributePaths = {"train", "route.originStation",
        "route.destinationStation"})
List<Trip> findByRouteIdAndDepartureDateAndStatusOrderByDepartureTimeAsc(
        Long routeId, LocalDate date, TripStatus status);

@EntityGraph(attributePaths = {"train", "route.originStation",
        "route.destinationStation"})
Optional<Trip> findWithDetailsById(Long id);
```

`CoachRepository`:

```java
List<Coach> findByTrainIdInOrderByCoachNumberAsc(Collection<Long> trainIds);
```

`TripSeatRepository`: đếm chỗ trống theo chuyến và toa trong **một** query cho cả
danh sách, không query từng chuyến:

```java
interface CoachAvailability {
    Long getTripId();
    Long getCoachId();
    long getAvailable();
}

@Query("""
    select ts.trip.id as tripId, s.coach.id as coachId, count(ts) as available
    from TripSeat ts join ts.seat s
    where ts.trip.id in :tripIds
      and (ts.status = :available
           or (ts.status = :held and ts.heldUntil < :now))
    group by ts.trip.id, s.coach.id""")
List<CoachAvailability> countAvailable(Collection<Long> tripIds,
        TripSeatStatus available, TripSeatStatus held, LocalDateTime now);
```

Truyền enum qua tham số thay vì viết thẳng trong JPQL, để không phụ thuộc cách
Hibernate đọc tên enum.

## Service

`trip/service/TripSearchService`, nhận `Clock` qua constructor (T01):

| Method | Việc |
| --- | --- |
| `List<TripSummary> search(TripSearchForm form)` | Kiểm tra hai luật còn lại, ném `InvalidSearchException` kèm tên field. Tìm tuyến theo hai mã ga; không có tuyến thì trả danh sách rỗng. Lấy chuyến `SCHEDULED` trong ngày, bỏ chuyến đã khởi hành, ghép số chỗ trống và giá |
| `TripDetail findDetail(long tripId)` | Không thấy: `TripNotFoundException` (404). Chuyến hủy hoặc đã khởi hành vẫn xem được, nhưng `bookable = false` |

Cách tính:

- Giá vé của một toa = `basePrice + priceModifier` (`docs/product/domain-model.md`).
- "Giá từ" = giá thấp nhất trong các toa **còn chỗ trống**.
- Đến ngày hôm sau khi `arrivalTime` trước `departureTime`; thời gian di chuyển cộng
  thêm 24 giờ trong trường hợp đó.
- `enoughSeats = availableSeats >= passengers`. Chuyến không đủ chỗ vẫn hiện, nhưng
  không có nút chọn.

`home/service/HomeService`: thay danh sách ga viết cứng bằng ga thật từ
`StationRepository.findAllByOrderByNameAsc()`. Giữ bốn tuyến phổ biến dưới dạng cặp
mã ga (`SGN-NTR`, `SGN-DNG`, `HNI-DNG`, `HNI-LCI`), đổi sang tên ga khi hiển thị, và
bỏ cặp nào có ga không còn trong database.

## Controller

`trip/controller/TripController` (`@Controller`, trang cho khách):

| Method | URL | Handler | Kết quả |
| --- | --- | --- | --- |
| GET | `/trips?from&to&date&passengers` | `search(@Valid @ModelAttribute("search") TripSearchForm, BindingResult, Model)` | View `trips/list`; lỗi thì hiện form kèm thông báo, không có danh sách |
| GET | `/trips/{id}?passengers=` | `detail(@PathVariable long id, @RequestParam(defaultValue = "1") int passengers, Model)` | View `trips/detail` |

Nút "Chọn chỗ" ở trang chi tiết trỏ tới `/booking/{tripId}/seats?passengers=N`. Đây
là URL của T09; hai task phải dùng đúng tên tham số `passengers`.

`/trips/**` đã được T02 mở cho mọi người. Trước khi T02 merge, đăng nhập bằng tài
khoản tạm để thử.

## View

Khung khách `layout/base.html`, CSS `app.css`, class riêng mang tiền tố `tg-`.

- `templates/index.html`: `<option th:value="${station.code}" th:text="${station.name}">`;
  thẻ tuyến phổ biến mang `data-from`, `data-to` là mã ga. `landing.js` không phải sửa.
- `templates/trips/list.html`: form tìm rút gọn ở đầu trang, điền sẵn điều kiện vừa
  tìm; mỗi chuyến một thẻ như ví dụ ở `SPEC.md` mục 8; danh sách rỗng hiện "Không có
  chuyến phù hợp" kèm gợi ý đổi ngày.
- `templates/trips/detail.html`: thông tin chuyến và bảng toa (số toa, loại, giá vé,
  chỗ còn). Chuyến hủy hiện nhãn "Đã hủy", không có nút chọn chỗ.
- Tiền định dạng `350.000đ`: `#numbers.formatInteger(price, 0, 'POINT')`.

## Quy tắc và lỗi phải xử lý

- Ga đi trùng ga đến, ngày trong quá khứ, số khách ngoài 1-10: báo lỗi trên trang,
  không hiện danh sách.
- Mã ga không tồn tại (người dùng sửa URL): coi như không có tuyến, danh sách rỗng.
- Chuyến không tồn tại: 404.

## Test

- `@DataJpaTest` cho `countAvailable` (D1): chỗ `HELD` đã hết hạn được đếm là trống,
  chỗ `HELD` còn hạn và chỗ `BOOKED` thì không.
- Unit test cho phép tính thời gian di chuyển qua nửa đêm (20:30 tới 04:50 là 8 giờ
  20 phút) và luật "chuyến đã khởi hành thì không đặt được", với `Clock` cố định.

## Xong khi

- [ ] Ô chọn ga ở landing lấy từ database; thêm một ga ở `/admin/stations` là nó hiện
      ra ở landing.
- [ ] Tìm Sài Gòn → Nha Trang ngày mai, 2 khách: thấy chuyến SNT1, 20:30 → 04:50
      (+1 ngày), 8 giờ 20 phút, còn 96 chỗ, giá từ 350.000đ.
- [ ] Tìm Hà Nội → Lào Cai đúng ngày SP1 bị hủy trong seed: không thấy chuyến đó.
- [ ] Thẻ tuyến phổ biến điền đúng hai ga.
- [ ] Kiểm tra giao diện ở 375px và 1280px: không tràn ngang.
- [ ] `.\mvnw.cmd test` xanh. Một dòng `docs/WORKLOG.md`.
