# T05 Tìm chuyến, danh sách chuyến, chi tiết chuyến

> **Tóm tắt:** sau task này, khách chọn ga đi, ga đến, ngày đi, số hành khách ở trang chủ,
> bấm "Tìm chuyến" là thấy các chuyến phù hợp, rồi mở xem chi tiết từng toa.

| Đợt | Phần việc | Độ khó | Cần xong trước | Task đang chờ task này | Tên branch |
| --- | --- | --- | --- | --- | --- |
| 1 | B | Vừa (khoảng 2-3 ngày) | Quyết định D6, D8, D9 | T09 (nút "Chọn chỗ") | `feat/t05-trip-search` |

Các từ như `@EntityGraph`, DTO, `record` được giải thích trong `README.md` cùng thư mục.
Cách chia package theo tầng (`controller/`, `service/`, `model/request/`...) nằm ở
`docs/decisions/0006-cau-truc-package-theo-tang.md`.

## 1. Task này làm gì

Khách nhập ga đi, ga đến, ngày đi, số hành khách ở trang chủ (landing), bấm "Tìm chuyến"
và thấy danh sách chuyến phù hợp (`SPEC.md` mục 7, 8). Mỗi chuyến hiện: mã tàu, ga đi, ga
đến, giờ đi, giờ đến, thời gian di chuyển, số chỗ còn lại, giá thấp nhất. Khách chọn một
chuyến để xem chi tiết từng toa, rồi đi tiếp sang bước chọn chỗ.

Khách **không cần đăng nhập** để tìm và xem chuyến.

Hiện trang chủ **đã đọc dữ liệu thật** từ database: ô chọn ga lấy từ bảng `stations`, xếp
theo lý trình từ Bắc vào Nam, và bốn thẻ tuyến phổ biến là bốn tuyến có nhiều booking nhất
(ghi chú dưới thẻ là quãng đường). Nhưng form đang gửi **tên ga**, còn trang tìm chuyến cần
**mã ga**. Task này đổi trang chủ sang gửi mã ga, rồi làm trang danh sách và trang chi tiết
chuyến.

Khách chỉ tìm được chuyến cho **cặp ga có tuyến**. Không có vé nối chuyến (`SPEC.md` mục
32.1, ADR 0011): dữ liệu mẫu không có tuyến Hà Nội → Huế, nên tìm Hà Nội → Huế ra danh sách
rỗng, dù tàu SE1 có chạy qua Huế bằng nhiều chặng.

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
stations 1 ──< routes 1 ──< trips >── 1 trains
                             │
                             └──< trip_seats
```

Đọc sơ đồ: một tuyến (`routes`) nối hai ga và có nhiều chuyến (`trips`). Mỗi chuyến chạy
bằng một tàu (`trains`) và có 96 dòng `trip_seats`. Bảng `trip_seats` ghi **từng chỗ trên từng
chuyến**: số toa, loại toa, mã chỗ, phụ thu và trạng thái. Cùng mã chỗ `A01` của toa 1 có thể
trống ở chuyến hôm nay nhưng đã bán ở chuyến ngày mai. Không có bảng toa hay ghế (ADR 0009).

| Bảng | Cột dùng tới |
| --- | --- |
| `stations` | `id`, `code`, `name` |
| `routes` | `origin_station_id`, `destination_station_id` |
| `trips` | `route_id`, `train_id`, `departure_at`, `arrival_at`, `base_price`, `status` |
| `trip_seats` | `trip_id`, `coach_number`, `coach_type`, `price_modifier`, `status` |

Hai khái niệm dùng suốt task này:

- Một chỗ **còn trống** khi `status = AVAILABLE`. Chỗ chỉ có hai trạng thái `AVAILABLE` và
  `BOOKED`; không còn giữ chỗ, nên không có hạn giữ phải so giờ (ADR 0010).
- Một chuyến **đặt được** khi `status = SCHEDULED` và thời điểm khởi hành `departure_at` vẫn
  còn ở sau hiện tại (quyết định D8).

`departure_at` và `arrival_at` là ngày-giờ đầy đủ (`DATETIME`, Java `LocalDateTime`), và
database chặn giờ đến không sau giờ đi. Chuyến có thể đến vào hôm sau, hoặc chạy hơn 24 giờ
(Hà Nội - Sài Gòn khoảng 29 giờ). Không còn quy ước "giờ đến nhỏ hơn giờ đi là hôm sau", nên
không có phép cộng 24 giờ nào.

## 4. Các bước làm

### Bước 1. Model: thêm method cho entity, tạo request và DTO

Entity `entity/Trip` đã có `getDepartureAt()` và `getArrivalAt()` kiểu `LocalDateTime`, nên
không cần thêm method nào vào entity.

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
        String destinationName, LocalDateTime departureAt, LocalDateTime arrivalAt,
        Duration duration, long availableSeats, long lowestPrice,
        boolean enoughSeats) { }

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
/** Trips on the route leaving in [from, to), earliest first. */
@EntityGraph(attributePaths = {"train", "route.originStation",
        "route.destinationStation"})
@Query("""
    select t from Trip t
    where t.route.id = :routeId and t.status = :status
      and t.departureAt >= :from and t.departureAt < :to
    order by t.departureAt""")
List<Trip> findForSearch(Long routeId, TripStatus status,
        LocalDateTime from, LocalDateTime to);

@EntityGraph(attributePaths = {"train", "route.originStation",
        "route.destinationStation"})
Optional<Trip> findWithDetailsById(Long id);
```

Ngày khách chọn là một `LocalDate`, còn cột là ngày-giờ. Service truyền
`from = date.atStartOfDay()` và `to = date.plusDays(1).atStartOfDay()`, tức mọi chuyến khởi
hành từ 00:00 ngày đó tới trước 00:00 hôm sau. Dùng `>=` và `<` thay vì `between`, vì
`between` lấy cả hai đầu và sẽ kéo vào chuyến khởi hành đúng 00:00 hôm sau. Câu này dùng được
chỉ mục `idx_trips_route_departure (route_id, departure_at)` có sẵn trong V5.

Thêm câu đếm chỗ trống theo từng chuyến và từng toa. Câu này đếm cho **cả danh sách** trong
một lần, thay vì hỏi database riêng cho từng chuyến. Mọi thông tin của toa (số toa, loại, phụ
thu) nằm ngay trong `trip_seats`, nên chỉ cần nhóm theo các cột đó, không cần join.

Kết quả đếm là một interface projection, đặt trong `model/dto/` giống `IdCount` đang có.
Tạo `model/dto/CoachAvailability`:

```java
public interface CoachAvailability {
    Long getTripId();
    int getCoachNumber();
    CoachType getCoachType();
    long getPriceModifier();
    long getAvailable();
}
```

Rồi thêm vào `repository/TripSeatRepository`:

```java
@Query("""
    select ts.trip.id as tripId, ts.coachNumber as coachNumber,
           ts.coachType as coachType, ts.priceModifier as priceModifier,
           sum(case when ts.status = :available then 1 else 0 end) as available
    from TripSeat ts
    where ts.trip.id in :tripIds
    group by ts.trip.id, ts.coachNumber, ts.coachType, ts.priceModifier
    order by ts.trip.id, ts.coachNumber""")
List<CoachAvailability> countAvailable(Collection<Long> tripIds,
        TripSeatStatus available);
```

Dùng `sum(case ...)` thay vì lọc ở `where` để toa hết chỗ vẫn có một dòng với `available = 0`:
trang chi tiết cần hiện đủ 4 toa, kể cả toa đã bán hết.

Giá trị enum `AVAILABLE` được truyền vào qua tham số, không viết thẳng trong câu
truy vấn. Vì sao: viết thẳng thì phải phụ thuộc vào cách Hibernate đọc tên enum, dễ lỗi
khó hiểu.

### Bước 3. Service: tìm chuyến và tính giá

Tạo `service/TripSearchService`. Thời điểm hiện tại lấy bằng `LocalDate.now()` và
`LocalDateTime.now()`.

| Method | Việc |
| --- | --- |
| `List<TripSummary> search(TripSearchRequest form)` | Kiểm tra hai luật còn lại; sai thì ném `InvalidSearchException` kèm tên field bị lỗi. Tìm tuyến theo hai mã ga; không có tuyến thì trả danh sách rỗng. Lấy các chuyến `SCHEDULED` khởi hành trong ngày bằng `findForSearch`, bỏ chuyến có `departureAt` không còn sau hiện tại, rồi ghép số chỗ trống và giá |
| `TripDetail findDetail(long tripId)` | Không thấy chuyến thì ném `TripNotFoundException` (404). Chuyến đã hủy hoặc đã khởi hành vẫn xem được, nhưng `bookable = false` |

Hai exception `InvalidSearchException` và `TripNotFoundException` đặt trong
`common/exception/`.

Cách tính:

- Giá vé của một toa = `basePrice + priceModifier` (`docs/product/domain-model.md`). Danh sách
  toa của một chuyến lấy từ chính kết quả `countAvailable`, không cần bảng toa.
- "Giá từ" là giá thấp nhất trong các toa **còn chỗ trống**.
- Thời gian di chuyển = `Duration.between(departureAt, arrivalAt)`. Kết quả luôn dương
  (database chặn giờ đến không sau giờ đi) và có thể từ 24 giờ trở lên. Không cộng thêm 24 giờ
  trong bất kỳ trường hợp nào.
- `enoughSeats = availableSeats >= passengers`. Chuyến không đủ chỗ vẫn hiện trong danh
  sách, nhưng không có nút chọn.

### Bước 4. Trang chủ: gửi mã ga thay vì tên ga

`service/HomeService` hiện có hai method:

- `stations()` trả `List<String>` là **tên** ga, lấy từ
  `StationRepository.findAllByOrderByKmMarkAsc()`, tức xếp theo lý trình từ Bắc vào Nam.
- `popularRoutes()` chọn bốn tuyến có nhiều booking chưa hủy nhất; bằng nhau thì tuyến
  nhiều chuyến hơn đứng trước, rồi tới id nhỏ hơn. Ghi chú dưới thẻ là quãng đường
  (`distanceKm + " km"`). Kết quả là `model/response/PopularRoute` chỉ chứa tên ga.

Sửa như sau:

- `stations()` trả `List<Station>` thay vì chỉ tên, để trang chủ có cả mã lẫn tên ga. **Giữ
  nguyên** thứ tự theo lý trình.
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
  mỗi chuyến là một thẻ giống ví dụ ở `SPEC.md` mục 8. Giờ đi, giờ đến định dạng bằng
  `#temporals.format(..., 'HH:mm')`; khi ngày đến khác ngày đi thì hiện thêm ngày đến, ví dụ
  `00:30 22/10`. Thời gian di chuyển hiện bằng giờ và phút, ví dụ "8 giờ 45 phút". Không có
  chuyến nào thì hiện "Không có chuyến phù hợp" kèm gợi ý đổi ngày.
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

Dữ liệu mẫu chỉ có chuyến của tàu SE1, mỗi ngày một chặng lúc 08:00, tính từ ngày seed chạy
(`docs/RUNBOOK.md`, mục "Dữ liệu thử"). Câu SQL sau cho id, ngày giờ và tuyến của mọi chuyến,
dùng cho các bước bên dưới:

```sql
SELECT t.id, o.code AS di, d.code AS den, t.departure_at, t.arrival_at, t.status
FROM trips t
JOIN routes r ON r.id = t.route_id
JOIN stations o ON o.id = r.origin_station_id
JOIN stations d ON d.id = r.destination_station_id
ORDER BY t.departure_at;
```

1. Ở trang chủ, chọn Đà Nẵng → Nha Trang, ngày của chuyến `DNG` → `NTR` trong kết quả SQL
   trên (ngày sau ngày seed chạy; nếu vừa làm mới database thì là ngày mai), 2 khách, bấm "Tìm
   chuyến". Đường dẫn có `from=DNG&to=NTR`, không phải tên ga.
2. Thấy chuyến SE1, 08:00 → 16:45, thời gian di chuyển **8 giờ 45 phút**, giá từ 520.000đ
   (toa ghế), còn 95 chỗ: booking `BK...003` của dữ liệu mẫu đã mua một ghế. Mở chi tiết: toa
   giường giá 720.000đ (520.000 + 200.000).
3. Chuyến đến vào hôm sau và chạy hơn 24 giờ. Lấy id chuyến `NTR` → `DNG` (chưa có booking),
   tạm đổi giờ đến thành 24 giờ 30 phút sau giờ đi:

```sql
UPDATE trips SET arrival_at = departure_at + INTERVAL 1470 MINUTE WHERE id = <id>;
```

   Tìm Nha Trang → Đà Nẵng đúng ngày đó: chuyến hiện 08:00 → 08:30 kèm ngày đến (hôm sau),
   thời gian di chuyển **24 giờ 30 phút**, không phải số âm hay 30 phút. Thử xong thì trả về
   như cũ (chặng 524 km chạy 525 phút):

```sql
UPDATE trips SET arrival_at = departure_at + INTERVAL 525 MINUTE WHERE id = <id>;
```

4. Bấm một thẻ tuyến phổ biến: hai ô ga được điền đúng.
5. Thử lỗi: chọn ga đi trùng ga đến; sửa đường dẫn cho `date` là hôm qua; sửa
   `passengers=11`. Cả ba lần đều báo lỗi, không hiện danh sách.
6. Không có tuyến thì không có chuyến. Tìm Hà Nội → Huế bất kỳ ngày nào: "Không có chuyến phù
   hợp", vì dữ liệu mẫu không có tuyến này (SE1 chỉ chạy qua Huế bằng nhiều chặng).
7. Chỗ đã bán không được đếm là còn trống. Lấy id một chuyến đang hiện trong danh sách, ghi
   lại số chỗ còn, rồi chạy:

```sql
UPDATE trip_seats SET status = 'BOOKED'
WHERE trip_id = <id> AND status = 'AVAILABLE' LIMIT 2;
```

   Tải lại trang: số chỗ còn giảm 2. Thử xong thì trả hai chỗ về trạng thái trống:

```sql
UPDATE trip_seats SET status = 'AVAILABLE'
WHERE trip_id = <id> AND status = 'BOOKED' AND id IN (<id hai chỗ vừa đổi>);
```

   (Lấy hai id bằng `SELECT id FROM trip_seats WHERE trip_id = <id> AND status = 'BOOKED';`
   trước khi trả lại, vì chuyến có thể đã có chỗ `BOOKED` từ dữ liệu mẫu.)

8. Chuyến đã hủy không hiện. Chuyến `DNG` → `HUE` chỉ có một booking đã hủy. Tạm hủy nó:
   `UPDATE trips SET status = 'CANCELLED' WHERE id = <id>;`. Tìm Đà Nẵng → Huế đúng ngày đó:
   không thấy chuyến. Mở thẳng `/trips/<id>`: nhãn "Đã hủy", không có nút "Chọn chỗ". Xong thì
   trả về `SCHEDULED`.
9. Chuyến đã khởi hành không đặt được (quyết định D8). Ghi lại `departure_at` của một chuyến
   sắp chạy, rồi tạm dời nó về hôm nay lúc 00:01, tức đã khởi hành:

```sql
UPDATE trips SET departure_at = TIMESTAMP(CURRENT_DATE, '00:01:00') WHERE id = <id>;
```

   Tìm chuyến hôm nay trên tuyến đó: chuyến không hiện. Mở thẳng `/trips/<id>`: vẫn xem
   được, nhưng không có nút "Chọn chỗ". Thử xong thì trả `departure_at` về giá trị đã ghi lại,
   hoặc làm mới database theo `docs/RUNBOOK.md`.

10. Mở `/trips/999999`: trang 404.

## 7. Lỗi hay gặp

- Quên `@EntityGraph` ở câu lấy chuyến: template đọc tên ga hay mã tàu sẽ lỗi
  `LazyInitializationException`.
- Đếm chỗ trống cho từng chuyến bằng một câu truy vấn riêng: trang danh sách chậm dần khi
  có nhiều chuyến. Dùng `countAvailable` đếm một lần cho cả danh sách.
- Đổi ô chọn ga sang mã nhưng quên đổi `data-from`, `data-to` của thẻ tuyến phổ biến: bấm
  thẻ thì hai ô ga không chọn được gì, vì tên ga không khớp giá trị nào trong ô chọn.
- Đặt tên tham số khác `passengers` ở link "Chọn chỗ": T09 không đọc được số khách.
- Lọc ngày bằng `between date 00:00 and date+1 00:00`: chuyến khởi hành đúng 00:00 hôm sau hiện
  ở cả hai ngày. Dùng `>= from` và `< to`.
- Giữ lại phép "giờ đến trước giờ đi thì cộng 24 giờ" của code cũ: giờ đã là ngày-giờ đầy đủ,
  phép cộng đó làm sai chuyến dài.

## 8. Kiểm tra trước khi báo xong

- [ ] Ô chọn ga ở trang chủ gửi mã ga, xếp từ Bắc vào Nam; thêm một ga ở `/admin/stations`
      là nó hiện ra ở trang chủ, đúng vị trí theo lý trình.
- [ ] Bốn thẻ tuyến phổ biến vẫn là bốn tuyến như trước khi làm task, cùng ghi chú quãng
      đường, và bấm vào thì điền đúng hai ga.
- [ ] Tìm Đà Nẵng → Nha Trang đúng ngày của chuyến, 2 khách: thấy chuyến SE1, 08:00 → 16:45,
      8 giờ 45 phút, giá từ 520.000đ, số chỗ còn khớp với database (95 với dữ liệu mẫu).
- [ ] Chuyến chạy hơn 24 giờ hiện đúng ngày đến và thời gian di chuyển (kịch bản 3).
- [ ] Chuyến đã hủy không hiện trong danh sách; cặp ga không có tuyến ra danh sách rỗng.
- [ ] Đủ các bước trong kịch bản ở mục 6 cho đúng kết quả mong đợi.
- [ ] Kiểm tra giao diện ở bề rộng 375px và 1280px: không bị tràn ngang.
- [ ] `.\mvnw.cmd compile` chạy không lỗi và ứng dụng khởi động được
      (`.\mvnw.cmd spring-boot:run`).
- [ ] Có một dòng trong `docs/WORKLOG.md`, nằm trong cùng commit với code.
