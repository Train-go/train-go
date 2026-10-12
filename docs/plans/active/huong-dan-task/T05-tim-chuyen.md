# T05 Tìm chuyến, danh sách chuyến, chi tiết chuyến

> **Tóm tắt:** sau task này, khách chọn ga đi, ga đến, ngày đi, số hành khách ở trang chủ,
> bấm "Tìm chuyến" là thấy các chuyến phù hợp, rồi mở xem chi tiết từng toa.

| Đợt | Phần việc | Độ khó | Cần xong trước | Task đang chờ task này | Tên branch |
| --- | --- | --- | --- | --- | --- |
| 1 | B | Vừa (khoảng 2-3 ngày) | Quyết định D6, D8 | T09 (nút "Chọn chỗ") | `feat/t05-trip-search` |

Các từ như `@EntityGraph`, DTO, `record` được giải thích trong `README.md` cùng thư mục.
Cách chia package theo tầng (`controller/`, `service/`, `model/request/`...) nằm ở
`docs/decisions/0006-cau-truc-package-theo-tang.md`.

## 1. Task này làm gì

Khách nhập ga đi, ga đến, ngày đi, số hành khách ở trang chủ (landing), bấm "Tìm chuyến"
và thấy danh sách chuyến phù hợp (`SPEC.md` mục 7, 8). Mỗi chuyến hiện: mã tàu, ga đi, ga
đến, giờ đi, giờ đến, thời gian di chuyển, số chỗ còn lại, giá thấp nhất. Khách chọn một
chuyến để xem chi tiết từng toa, rồi đi tiếp sang bước chọn chỗ.

Khách **không cần đăng nhập** để tìm và xem chuyến.

Hiện trang chủ **đã đọc dữ liệu thật** từ database: ô chọn ga lấy từ bảng `stations`, và
bốn thẻ tuyến phổ biến là bốn tuyến có nhiều booking nhất (ghi chú dưới thẻ là quãng
đường). Nhưng form đang gửi **tên ga**, còn trang tìm chuyến cần **mã ga**. Task này đổi
trang chủ sang gửi mã ga, rồi làm trang danh sách và trang chi tiết chuyến.

## 2. Phạm vi

Làm:

- Đổi ô chọn ga và thẻ tuyến phổ biến ở trang chủ sang dùng mã ga.
- Trang danh sách chuyến `GET /trips`.
- Trang chi tiết chuyến `GET /trips/{id}`.

Không làm (`SPEC.md` mục 7):

- Lọc theo giá, loại toa, giờ; sắp xếp nâng cao.
- Đổi cách chọn tuyến phổ biến: giữ nguyên logic hiện có.
- Trang chọn chỗ: thuộc T09.

## 3. Dữ liệu dùng tới

```
stations 1 ──< routes 1 ──< trips >── 1 trains 1 ──< coaches 1 ──< seats
                             │                                      │
                             └──────────< trip_seats >──────────────┘
```

Đọc sơ đồ: một tuyến (`routes`) nối hai ga và có nhiều chuyến (`trips`). Mỗi chuyến chạy
bằng một tàu (`trains`); tàu có nhiều toa (`coaches`), toa có nhiều ghế (`seats`). Bảng
`trip_seats` ghi trạng thái của **từng ghế trên từng chuyến**: cùng một ghế có thể trống ở
chuyến hôm nay nhưng đã bán ở chuyến ngày mai.

| Bảng | Cột dùng tới |
| --- | --- |
| `stations` | `id`, `code`, `name` |
| `routes` | `origin_station_id`, `destination_station_id` |
| `trips` | `route_id`, `train_id`, `departure_date`, `departure_time`, `arrival_time`, `base_price`, `status` |
| `coaches` | `train_id`, `coach_number`, `coach_type`, `price_modifier` |
| `trip_seats` | `trip_id`, `seat_id`, `status`, `held_until` |

Hai khái niệm dùng suốt task này:

- Một chỗ **còn trống** khi `status = AVAILABLE`, hoặc khi `status = HELD` nhưng thời hạn
  giữ `held_until` đã qua (quyết định D1). Nói cách khác, chỗ có người giữ mà hết 5 phút thì
  coi như trống lại.
- Một chuyến **đặt được** khi `status = SCHEDULED` và thời điểm khởi hành (`departure_date`
  cộng `departure_time`) vẫn còn ở sau hiện tại (quyết định D8).

## 4. Các bước làm

### Bước 1. Model: thêm method cho entity, tạo request và DTO

Thêm vào entity `entity/Trip` method `departureAt()`, trả ngày và giờ khởi hành gộp làm một
(mô tả ở T07). Task nào làm trước thì thêm; T07, T09, T10 dùng lại, không viết bản thứ hai.

Tạo class mới `model/request/TripSearchRequest`. Tên các field phải khớp đúng tên các ô
đang có trong form ở trang chủ:

| Field | Kiểu | Luật kiểm tra |
| --- | --- | --- |
| `from` | `String` (mã ga) | `@NotBlank(message = "Vui lòng chọn ga đi.")` |
| `to` | `String` (mã ga) | `@NotBlank` |
| `date` | `LocalDate` | `@NotNull`, kèm `@DateTimeFormat(iso = ISO.DATE)` |
| `passengers` | `Integer` | `@NotNull`, `@Min(1)`, `@Max(10)` (quyết định D6) |

Hai luật còn lại kiểm tra trong service, vì cần so hai field với nhau hoặc cần biết hôm
nay là ngày nào: ga đi phải khác ga đến; ngày đi không được trước hôm nay.

Tạo các DTO trả cho view trong `model/response/`, viết bằng Java `record`, mỗi record một
file:

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

`TripSummary` là một dòng trong danh sách chuyến, `CoachOffer` là một toa trong trang chi
tiết, `TripDetail` là cả trang chi tiết.

### Bước 2. Repository: các câu truy vấn

Thêm vào `repository/RouteRepository`, để tìm tuyến từ hai mã ga:

```java
Optional<Route> findByOriginStationCodeAndDestinationStationCode(
        String originCode, String destinationCode);
```

Thêm vào `repository/TripRepository`, để lấy chuyến kèm tàu và hai ga trong một lần đọc:

```java
@EntityGraph(attributePaths = {"train", "route.originStation",
        "route.destinationStation"})
List<Trip> findByRouteIdAndDepartureDateAndStatusOrderByDepartureTimeAsc(
        Long routeId, LocalDate date, TripStatus status);

@EntityGraph(attributePaths = {"train", "route.originStation",
        "route.destinationStation"})
Optional<Trip> findWithDetailsById(Long id);
```

Thêm vào `repository/CoachRepository`, để lấy toa của nhiều tàu cùng lúc:

```java
List<Coach> findByTrainIdInOrderByCoachNumberAsc(Collection<Long> trainIds);
```

Thêm câu đếm chỗ trống theo từng chuyến và từng toa. Câu này đếm cho **cả danh sách** trong
một lần, thay vì hỏi database riêng cho từng chuyến.

Kết quả đếm là một interface projection, đặt trong `model/dto/` giống `IdCount` và
`TrainCapacity` đang có. Tạo `model/dto/CoachAvailability`:

```java
public interface CoachAvailability {
    Long getTripId();
    Long getCoachId();
    long getAvailable();
}
```

Rồi thêm vào `repository/TripSeatRepository`:

```java
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

Giá trị enum (`AVAILABLE`, `HELD`) được truyền vào qua tham số, không viết thẳng trong câu
truy vấn. Vì sao: viết thẳng thì phải phụ thuộc vào cách Hibernate đọc tên enum, dễ lỗi
khó hiểu.

### Bước 3. Service: tìm chuyến và tính giá

Tạo `service/TripSearchService`. Thời điểm hiện tại lấy bằng `LocalDate.now()` và
`LocalDateTime.now()`.

| Method | Việc |
| --- | --- |
| `List<TripSummary> search(TripSearchRequest form)` | Kiểm tra hai luật còn lại; sai thì ném `InvalidSearchException` kèm tên field bị lỗi. Tìm tuyến theo hai mã ga; không có tuyến thì trả danh sách rỗng. Lấy các chuyến `SCHEDULED` trong ngày, bỏ chuyến đã khởi hành, rồi ghép số chỗ trống và giá |
| `TripDetail findDetail(long tripId)` | Không thấy chuyến thì ném `TripNotFoundException` (404). Chuyến đã hủy hoặc đã khởi hành vẫn xem được, nhưng `bookable = false` |

Hai exception `InvalidSearchException` và `TripNotFoundException` đặt trong
`common/exception/`.

Cách tính:

- Giá vé của một toa = `basePrice + priceModifier` (`docs/product/domain-model.md`).
- "Giá từ" là giá thấp nhất trong các toa **còn chỗ trống**.
- Khi `arrivalTime` trước `departureTime`, tàu đến vào ngày hôm sau. Khi đó thời gian di
  chuyển phải cộng thêm 24 giờ.
- `enoughSeats = availableSeats >= passengers`. Chuyến không đủ chỗ vẫn hiện trong danh
  sách, nhưng không có nút chọn.

### Bước 4. Trang chủ: gửi mã ga thay vì tên ga

`service/HomeService` hiện có hai method:

- `stations()` trả `List<String>` là **tên** ga, lấy từ
  `StationRepository.findAllByOrderByNameAsc()`.
- `popularRoutes()` chọn bốn tuyến có nhiều booking chưa hủy nhất; bằng nhau thì tuyến
  nhiều chuyến hơn đứng trước, rồi tới id nhỏ hơn. Ghi chú dưới thẻ là quãng đường
  (`distanceKm + " km"`). Kết quả là `model/response/PopularRoute` chỉ chứa tên ga.

Sửa như sau:

- `stations()` trả `List<Station>` thay vì chỉ tên, để trang chủ có cả mã lẫn tên ga.
- `PopularRoute` thêm hai field `originCode`, `destinationCode`. `popularRoutes()` điền
  thêm hai mã này. **Giữ nguyên** cách sắp xếp và ghi chú quãng đường.
- Sửa comment đầu class `HomeService`: form đã gửi mã ga.

Vì sao dùng mã ga: tên ga có dấu và khoảng trắng, dễ sai khi nằm trên đường dẫn; mã ga
(`SGN`, `NTR`) ngắn, không trùng và là thứ `RouteRepository` dùng để tìm tuyến.

### Bước 5. Controller: trang cho khách

Tạo `controller/TripController` (dùng `@Controller` vì trả trang HTML):

| Method | Đường dẫn | Handler | Kết quả |
| --- | --- | --- | --- |
| GET | `/trips?from&to&date&passengers` | `search(@Valid @ModelAttribute("search") TripSearchRequest, BindingResult, Model)` | Trang `trips/list`; lỗi thì hiện form kèm thông báo, không có danh sách |
| GET | `/trips/{id}?passengers=` | `detail(@PathVariable long id, @RequestParam(defaultValue = "1") int passengers, Model)` | Trang `trips/detail` |

Nút "Chọn chỗ" ở trang chi tiết trỏ tới `/booking/{tripId}/seats?passengers=N`. Đây là
đường dẫn của T09; hai task phải dùng đúng tên tham số `passengers`, nếu không T09 sẽ không
biết khách cần bao nhiêu chỗ.

T02 đã mở `/trips/**` cho mọi người. Trước khi T02 được merge, bạn đăng nhập bằng tài khoản
tạm để thử.

### Bước 6. Giao diện

Dùng khung trang khách `layout/base.html`, CSS `app.css`; class tự đặt mang tiền tố `tg-`.

- `templates/index.html`: ô chọn ga viết
  `<option th:value="${station.code}" th:text="${station.name}">`, tức giá trị gửi đi là
  mã ga, chữ hiện ra là tên ga. Thẻ tuyến phổ biến mang `data-from`, `data-to` là mã ga
  (`route.originCode`, `route.destinationCode`), chữ trên thẻ vẫn là tên ga. File
  `landing.js` không phải sửa: nó chép `data-from`, `data-to` vào hai ô chọn, nên chỉ cần
  hai giá trị đó khớp `th:value` của ô chọn.
- `templates/trips/list.html`: form tìm rút gọn ở đầu trang, điền sẵn điều kiện vừa tìm;
  mỗi chuyến là một thẻ giống ví dụ ở `SPEC.md` mục 8. Không có chuyến nào thì hiện
  "Không có chuyến phù hợp" kèm gợi ý đổi ngày.
- `templates/trips/detail.html`: thông tin chuyến và bảng toa (số toa, loại toa, giá vé, số
  chỗ còn). Chuyến đã hủy hiện nhãn "Đã hủy" và không có nút chọn chỗ.
- Tiền hiện dạng `350.000đ`: `#numbers.formatInteger(price, 0, 'POINT')`.

## 5. Luật và lỗi phải xử lý

- Ga đi trùng ga đến, ngày đi ở quá khứ, số khách ngoài khoảng 1-10: báo lỗi ngay trên
  trang, không hiện danh sách.
- Mã ga không tồn tại (người dùng tự sửa đường dẫn): coi như không có tuyến, trả danh sách
  rỗng.
- Chuyến không tồn tại: trang 404.

## 6. Kịch bản thử bằng tay

Chạy ứng dụng (`.\mvnw.cmd spring-boot:run`). Các bước có SQL thì mở MySQL bằng
`docker exec -it traingo-mysql mysql -utraingo -ptraingo --default-character-set=utf8mb4 traingo`.

1. Ở trang chủ, chọn Sài Gòn → Nha Trang, ngày mai, 2 khách, bấm "Tìm chuyến". Đường dẫn
   có `from=SGN&to=NTR`, không phải tên ga.
2. Thấy chuyến SNT1, 20:30 → 04:50 (+1 ngày), thời gian di chuyển **8 giờ 20 phút** (không
   phải số âm). Đây là cách kiểm tra phép tính qua nửa đêm.
3. Bấm một thẻ tuyến phổ biến: hai ô ga được điền đúng.
4. Thử lỗi: chọn ga đi trùng ga đến; sửa đường dẫn cho `date` là hôm qua; sửa
   `passengers=11`. Cả ba lần đều báo lỗi, không hiện danh sách.
5. Chỗ giữ đã hết hạn phải được đếm là trống (quyết định D1). Lấy id một chuyến đang hiện
   trong danh sách (ví dụ `12`), ghi lại số chỗ còn, rồi chạy:

```sql
UPDATE trip_seats SET status = 'HELD', held_by_user_id = 2,
       held_until = NOW() + INTERVAL 5 MINUTE
WHERE trip_id = 12 AND status = 'AVAILABLE' LIMIT 2;
```

   Tải lại trang: số chỗ còn giảm 2. Đổi `NOW() + INTERVAL 5 MINUTE` thành
   `NOW() - INTERVAL 1 MINUTE` (giữ đã hết hạn) với cùng hai chỗ đó:

```sql
UPDATE trip_seats SET held_until = NOW() - INTERVAL 1 MINUTE
WHERE trip_id = 12 AND status = 'HELD';
```

   Tải lại trang: số chỗ còn trở về như cũ. Thử xong thì trả hai chỗ về trạng thái trống:

```sql
UPDATE trip_seats SET status = 'AVAILABLE', held_by_user_id = NULL, held_until = NULL
WHERE trip_id = 12 AND status = 'HELD';
```

6. Chuyến đã khởi hành không đặt được (quyết định D8). Dữ liệu mẫu chỉ có chuyến từ ngày
   mai trở đi, nên tạm dời một chuyến về hôm nay lúc 00:01, tức đã khởi hành:

```sql
UPDATE trips SET departure_date = CURRENT_DATE, departure_time = '00:01:00' WHERE id = 12;
```

   Tìm chuyến hôm nay trên tuyến đó: chuyến không hiện. Mở thẳng `/trips/12`: vẫn xem
   được, nhưng không có nút "Chọn chỗ". Thử xong thì làm mới database theo
   `docs/RUNBOOK.md`, hoặc sửa lại ngày giờ cũ của chuyến.

7. Mở `/trips/999999`: trang 404.

## 7. Lỗi hay gặp

- Quên `@EntityGraph` ở câu lấy chuyến: template đọc tên ga hay mã tàu sẽ lỗi
  `LazyInitializationException`.
- Đếm chỗ trống cho từng chuyến bằng một câu truy vấn riêng: trang danh sách chậm dần khi
  có nhiều chuyến. Dùng `countAvailable` đếm một lần cho cả danh sách.
- Đổi ô chọn ga sang mã nhưng quên đổi `data-from`, `data-to` của thẻ tuyến phổ biến: bấm
  thẻ thì hai ô ga không chọn được gì, vì tên ga không khớp giá trị nào trong ô chọn.
- Đặt tên tham số khác `passengers` ở link "Chọn chỗ": T09 không đọc được số khách.

## 8. Kiểm tra trước khi báo xong

- [ ] Ô chọn ga ở trang chủ gửi mã ga; thêm một ga ở `/admin/stations` là nó hiện ra ở
      trang chủ.
- [ ] Bốn thẻ tuyến phổ biến vẫn là bốn tuyến như trước khi làm task, cùng ghi chú quãng
      đường, và bấm vào thì điền đúng hai ga.
- [ ] Tìm Sài Gòn → Nha Trang ngày mai, 2 khách: thấy chuyến SNT1, 20:30 → 04:50 (+1 ngày),
      8 giờ 20 phút, giá từ 350.000đ, số chỗ còn khớp với database (96 nếu chuyến chưa có
      booking).
- [ ] Tìm Hà Nội → Lào Cai đúng ngày SP1 bị hủy trong dữ liệu mẫu: không thấy chuyến đó.
- [ ] Đủ các bước trong kịch bản ở mục 6 cho đúng kết quả mong đợi.
- [ ] Kiểm tra giao diện ở bề rộng 375px và 1280px: không bị tràn ngang.
- [ ] `.\mvnw.cmd compile` chạy không lỗi và ứng dụng khởi động được
      (`.\mvnw.cmd spring-boot:run`).
- [ ] Có một dòng trong `docs/WORKLOG.md`, nằm trong cùng commit với code.
