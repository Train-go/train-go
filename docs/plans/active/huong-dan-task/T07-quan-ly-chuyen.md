# T07 Quản lý chuyến

> **Tóm tắt:** sau task này, admin mở trang chi tiết một tàu là thấy lịch chạy của tàu đó và
> thêm được chuyến mới nối vào cuối lịch. Trang `/admin/trips` liệt kê mọi chuyến, lọc được
> theo tàu và ngày, và là nơi hủy chuyến. Mỗi chuyến mới tạo có sẵn đủ chỗ để bán.

| Đợt | Phần việc | Độ khó | Cần xong trước | Task đang chờ task này | Tên branch |
| --- | --- | --- | --- | --- | --- |
| 2 | A | Vừa (khoảng 2-3 ngày) | T06 (cấu hình toa `CoachLayout`, trang chi tiết tàu), quyết định D2, D8, D9; nên có T03, T04 trước | Không task nào chờ; task này hoàn thành luồng admin của `SPEC.md` mục 50 | `feat/t07-trip-admin` |

Các từ như transaction, `@EntityGraph`, flash message được giải thích trong `README.md`
cùng thư mục. Cách chia package theo tầng (`controller/`, `service/`, `model/`...) xem
ADR 0006. Luật nối chặng của task này là quyết định D9, chi tiết ở
`docs/decisions/0011-chuyen-tao-tu-tau-noi-chang.md`.

## 1. Task này làm gì

Một **chuyến** (`Trip`) là một lần một tàu chạy trên một tuyến, từ một ngày giờ khởi hành
tới một ngày giờ đến cụ thể. Ví dụ: tàu SE1 chạy Đà Nẵng → Nha Trang, khởi hành 08:00, đến
16:45 cùng ngày.

Chuyến **thuộc về tàu**, không thuộc về tuyến (D9). Các chuyến chưa hủy của một tàu, xếp theo
giờ khởi hành, là **lịch chạy** của tàu đó, và chúng **nối chặng**: chuyến sau xuất phát từ ga
mà chuyến trước vừa đến. Dữ liệu mẫu cho thấy rõ điều này: SE1 chạy Hà Nội → Thanh Hóa, rồi
Thanh Hóa → Vinh, rồi Vinh → Đồng Hới..., mỗi ngày một chặng.

Trang `/admin/trips` **đã hiện danh sách chuyến thật** từ database (anh Phong làm ở commit
`9ba771b`), nhưng chỉ để xem, và code đang nằm chung trong `AdminController` và
`AdminService`. Task này làm bốn việc:

1. Chuyển danh sách chuyến ra controller và service riêng theo ADR 0006, thêm bộ lọc theo tàu
   và ngày.
2. Thêm phần **Lịch chạy** vào trang chi tiết tàu `/admin/trains/{id}` của T06: các chuyến của
   tàu theo thứ tự thời gian, có đánh dấu chỗ chuỗi bị đứt.
3. Cho admin thêm chuyến **từ trang chi tiết tàu**, theo luật nối chặng.
4. Cho admin hủy chuyến ở `/admin/trips` (D2).

Điểm quan trọng nhất khi tạo chuyến: hệ thống tự sinh một dòng `TripSeat` trạng thái
`AVAILABLE` (còn trống) cho **mọi** chỗ trong cấu hình toa cố định `CoachLayout` do T06 tạo
(ADR 0009; `SPEC.md` mục 38). Mọi tàu có 96 chỗ nên chuyến mới luôn có 96 dòng
`trip_seats`. Vì sao cần: trạng thái đặt chỗ thuộc về từng chuyến, không thuộc về tàu. Cùng
một ghế A01 có thể đã bán ở chuyến hôm nay nhưng còn trống ở chuyến ngày mai.

## 2. Phạm vi

Làm:

- Chuyển danh sách chuyến sang `controller/TripAdminController` và `service/TripAdminService`,
  lọc theo tàu và ngày.
- Phần Lịch chạy trên trang chi tiết tàu.
- Thêm chuyến từ trang chi tiết tàu, kiểm tra luật nối chặng.
- Hủy chuyến (theo quyết định D2).

Không làm:

- Sửa chuyến đã tạo.
- Xóa chuyến (quyết định D2: chuyến chỉ hủy, không xóa).
- Đổi tàu của chuyến.
- Chèn chuyến vào giữa lịch: chuyến mới chỉ nối vào **cuối** lịch (D9).
- Ga trung gian, bán vé theo đoạn: một chuyến chỉ có ga đi và ga đến (`SPEC.md` mục 10, 35).

## 3. Dữ liệu dùng tới

```
trains 1 ──< trips >── 1 routes >── stations (ga đi, ga đến)
              │
              └──< trip_seats

trips:      id, train_id, route_id, departure_at, arrival_at, base_price,
            status (SCHEDULED | COMPLETED | CANCELLED)
            CHECK (arrival_at > departure_at)
            INDEX idx_trips_train_departure (train_id, departure_at)
            INDEX idx_trips_route_departure (route_id, departure_at)
trip_seats: id, trip_id, coach_number, coach_type, seat_code, cabin_number,
            price_modifier, status (AVAILABLE | BOOKED)
            UNIQUE (trip_id, coach_number, seat_code)
bookings:   trip_id, booking_status   (để biết chuyến đã có booking chưa)
```

Đọc sơ đồ:

- Một chuyến thuộc đúng một tàu và một tuyến. Một tàu, một tuyến có nhiều chuyến.
- `departure_at` và `arrival_at` là ngày-giờ đầy đủ (`DATETIME`, Java `LocalDateTime`), nên
  chuyến từ 24 giờ trở lên (Hà Nội - Sài Gòn khoảng 29 giờ) lưu được. Không còn quy ước "giờ
  đến nhỏ hơn giờ đi là hôm sau". Database chặn giờ đến không sau giờ đi.
- Chỉ mục `idx_trips_train_departure` phục vụ lịch chạy của một tàu và câu tìm "chuyến cuối
  của tàu". Chỉ mục `idx_trips_route_departure` phục vụ trang tìm chuyến của T05.
- Một chuyến có nhiều `trip_seats`, mỗi dòng là một chỗ trong cấu hình toa cố định. Dòng giữ
  luôn số toa, loại toa, mã chỗ, cabin và phụ thu; không có bảng toa hay ghế để join.
- Trong một chuyến, mỗi chỗ chỉ có một dòng (`UNIQUE (trip_id, coach_number, seat_code)`).
- Bảng `bookings` chỉ dùng để kiểm tra chuyến đã có người đặt chưa, trước khi cho hủy.

### Luật nối chặng (D9)

Gọi **chuyến cuối** của một tàu là chuyến **chưa hủy** có giờ khởi hành muộn nhất.

1. Tàu chưa có chuyến nào chưa hủy: chọn tuyến bất kỳ.
2. Tàu đã có chuyến: chuyến mới nối vào cuối lịch. Ga đi bị khóa bằng ga đến của chuyến
   cuối, nên chỉ chọn được tuyến xuất phát từ ga đó.
3. Giờ khởi hành phải sau giờ đến của chuyến cuối ít nhất 30 phút (thời gian quay đầu, một
   hằng số trong code).
4. Vì chỉ nối vào cuối, hai chuyến của một tàu không bao giờ chồng giờ.

Service kiểm tra cả bốn điều trên, **không tin dữ liệu form**. Hủy một chuyến ở giữa làm chuỗi
bị đứt; trang tàu đánh dấu chỗ đó để admin biết, nhưng không chặn thao tác nào. Luật nối chặng
chỉ áp cho chuyến tạo mới.

## 4. Các bước làm

### Bước 1. Model: thêm method vào entity, tạo request và response

Entity `entity/Trip` và `entity/TripSeat` đã có. `Trip` đã có sẵn `getDepartureAt()` và
`getArrivalAt()` kiểu `LocalDateTime` (V5), nên **không cần** method ghép ngày với giờ nào.
T05, T09, T10, T12 cũng đọc thẳng `getDepartureAt()`. Constructor đã có:
`new Trip(train, route, departureAt, arrivalAt, basePrice)`, trạng thái mặc định `SCHEDULED`.

Thêm vào `Trip` một method:

```java
public void cancel() {
    if (status != TripStatus.SCHEDULED) {
        throw new IllegalStateException("Only a scheduled trip can be cancelled");
    }
    status = TripStatus.CANCELLED;
}
```

`cancel()` chỉ cho hủy chuyến đang `SCHEDULED` (đã lên lịch).

Tạo class mới `model/request/TripRequest`, chứa dữ liệu của form thêm chuyến. **Không có
`trainId`**: tàu lấy từ đường dẫn `/admin/trains/{trainId}/trips`, tức tàu của trang đang mở.

| Field | Kiểu | Luật kiểm tra |
| --- | --- | --- |
| `routeId` | `Long` | `@NotNull(message = "Vui lòng chọn tuyến.")` |
| `departureAt` | `LocalDateTime` | `@NotNull(message = "Vui lòng nhập ngày giờ khởi hành.")`, `@DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")` |
| `arrivalAt` | `LocalDateTime` | `@NotNull(message = "Vui lòng nhập ngày giờ đến.")`, `@DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")` |
| `basePrice` | `Long` | `@NotNull`, `@Positive`, `@Max(20_000_000)`, mỗi luật kèm thông báo tiếng Việt |

`@DateTimeFormat` cho Spring biết cách đọc ngày-giờ từ ô `<input type="datetime-local">`:
trình duyệt gửi dạng `2026-11-02T11:15`, đúng mẫu trên. Cùng mẫu đó dùng để in ngược giá trị
vào ô khi form điền sẵn hoặc hiện lại vì lỗi.

Tạo hai `record` trong `model/response/`, mỗi record một file:

```java
/** Where and when the train's next trip may start; both null for a train with no trip. */
record NextTripSlot(Station origin, LocalDateTime earliestDeparture) { }

/** One line of a train's timetable. chainBrokenAt is set when this trip does not start
    where the previous non-cancelled trip ended: it is that trip's destination. */
record TimetableRow(Trip trip, Station chainBrokenAt) { }
```

- `NextTripSlot`: ga đi bị khóa và giờ khởi hành sớm nhất của chuyến kế tiếp. Dùng để điền
  sẵn form và để lọc ô chọn tuyến.
- `TimetableRow`: một dòng trong Lịch chạy. `chainBrokenAt` khác `null` nghĩa là ngay trước
  chuyến này chuỗi bị đứt.

Dòng hiển thị trên bảng `/admin/trips` hiện là `model/response/AdminTrip` (các field đã định
dạng sẵn thành chuỗi). Theo ADR 0006, class `Admin*` biến mất khi màn hình nối hẳn vào nghiệp
vụ riêng. Bạn có thể đưa thẳng `Trip` ra view rồi định dạng ngày giờ trong template bằng
`#temporals.format(...)`, hoặc tạo một response mới cho bảng chuyến; xong thì xóa
`AdminTrip`.

### Bước 2. Repository: các câu truy vấn

`repository/TripRepository` đã có `findAllByOrderByDepartureAtAsc()`, kèm `@EntityGraph` đọc
luôn tàu, tuyến và hai ga. Thiếu `@EntityGraph` thì trang lỗi `LazyInitializationException`
khi hiện tên ga (xem `README.md`). Thêm vào `TripRepository`:

```java
/** /admin/trips with optional filters. from and to are both null, or both set. */
@EntityGraph(attributePaths = {"train", "route", "route.originStation",
        "route.destinationStation"})
@Query("""
    select t from Trip t
    where (:trainId is null or t.train.id = :trainId)
      and (:from is null or (t.departureAt >= :from and t.departureAt < :to))
    order by t.departureAt""")
List<Trip> findForAdmin(Long trainId, LocalDateTime from, LocalDateTime to);

/** The train's timetable, earliest first, cancelled trips included. */
@EntityGraph(attributePaths = {"route", "route.originStation",
        "route.destinationStation"})
List<Trip> findByTrainIdOrderByDepartureAtAsc(Long trainId);

/** The train's last trip that is not in the given status (pass CANCELLED). */
@EntityGraph(attributePaths = {"route", "route.destinationStation"})
Optional<Trip> findFirstByTrainIdAndStatusNotOrderByDepartureAtDesc(
        Long trainId, TripStatus status);
```

- `findForAdmin`: danh sách ở `/admin/trips`. Không chọn tàu thì `trainId` là `null`, không
  chọn ngày thì `from`, `to` là `null`. Câu này thay cho `findAllByOrderByDepartureAtAsc`
  trên trang đó; nếu không còn chỗ nào dùng câu cũ thì xóa nó, để không có hai câu gần giống
  nhau.
- `findByTrainId...`: lịch chạy của một tàu, theo thứ tự thời gian.
- `findFirstByTrainId...`: chuyến cuối chưa hủy. Vì chuyến chỉ được nối vào cuối và không
  chồng giờ, chuyến khởi hành muộn nhất cũng là chuyến đến muộn nhất.

Lọc theo ngày: `from = date.atStartOfDay()`, `to = date.plusDays(1).atStartOfDay()`. Dùng
`>= from` và `< to` thay vì `between`, vì `between` lấy cả hai đầu và sẽ kéo vào chuyến khởi
hành đúng 00:00 hôm sau.

Thêm vào `repository/RouteRepository`, để form chỉ đưa ra tuyến xuất phát từ ga bị khóa:

```java
@Query("""
    select r from Route r join fetch r.originStation o join fetch r.destinationStation d
    where o.id = :stationId
    order by d.kmMark""")
List<Route> findAllWithStationsByOriginStationId(Long stationId);
```

Tàu chưa có chuyến thì form dùng `findAllWithStations()` đã có (xếp theo lý trình ga đi rồi ga
đến).

Thêm vào `repository/TrainRepository` một câu khóa dòng tàu trong lúc tạo chuyến (lý do ở
Bước 3):

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
Optional<Train> findWithLockById(Long id);
```

Thêm vào `repository/BookingRepository`, để biết chuyến đã có booking ở một trạng thái nào
đó chưa:

```java
boolean existsByTripIdAndBookingStatus(Long tripId, BookingStatus status);
```

Danh sách chỗ để sinh không đọc từ database mà lấy từ `CoachLayout.seats()` của T06, nên
không cần repository nào cho toa hay ghế.

### Bước 3. Service: danh sách, lịch chạy, tạo và hủy chuyến

Tạo `service/TripAdminService`, với hằng số thời gian quay đầu:

```java
/** A train needs this long at a station before its next trip leaves (D9). */
static final Duration TURNAROUND = Duration.ofMinutes(30);
```

| Method | Việc |
| --- | --- |
| `List<Trip> findAll(Long trainId, LocalDate date)` | Danh sách `/admin/trips`, gọi `findForAdmin` như Bước 2 |
| `List<TimetableRow> timetable(long trainId)` | Lịch chạy của tàu: đọc `findByTrainIdOrderByDepartureAtAsc`, rồi đặt `chainBrokenAt` theo cách bên dưới |
| `NextTripSlot nextSlot(long trainId)` | Đọc chuyến cuối chưa hủy. Không có: `NextTripSlot(null, null)`. Có: ga đến của chuyến đó và `arrivalAt + TURNAROUND` |
| `List<Route> routeOptions(NextTripSlot slot)` | `slot.origin()` là `null`: mọi tuyến. Không thì chỉ tuyến xuất phát từ ga đó |
| `@Transactional Trip create(long trainId, TripRequest form)` | Khóa dòng tàu, kiểm tra các luật bên dưới, lưu `Trip`, sinh 96 `TripSeat` từ `CoachLayout.seats()`, trả chuyến |
| `@Transactional Trip cancel(long tripId)` | Theo D2: chỉ hủy chuyến `SCHEDULED`, chưa khởi hành, chưa có booking `CONFIRMED`. Sai luật thì ném `TripCannotBeCancelledException` kèm lý do |

Cách đặt `chainBrokenAt` trong `timetable`: đi lần lượt các chuyến theo thứ tự, nhớ ga đến của
chuyến **chưa hủy** gần nhất. Gặp một chuyến chưa hủy mà ga đi khác ga đã nhớ thì
`chainBrokenAt` của chuyến đó là ga đã nhớ. Chuyến đã hủy vẫn có dòng trong lịch (hiện nhãn
"Đã hủy"), nhưng không tính khi so ga.

Ví dụ với dữ liệu mẫu: hủy chuyến Đà Nẵng → Huế của SE1. Chuyến chưa hủy liền trước nó là Nha
Trang → Đà Nẵng, đến Đà Nẵng. Chuyến chưa hủy liền sau là Huế → Đồng Hới, đi từ Huế. Huế khác
Đà Nẵng, nên dòng Huế → Đồng Hới có `chainBrokenAt` là Ga Đà Nẵng.

Luật khi tạo chuyến, kiểm tra theo thứ tự. Sai điều nào thì ném `InvalidTripException` mang
tên ô bị lỗi và thông báo tiếng Việt, để controller gắn lỗi ngay cạnh ô đó:

| Luật | Ô | Thông báo |
| --- | --- | --- |
| Tàu không tồn tại | (trang 404) | `TrainNotFoundException` của T03 |
| Tuyến không tồn tại | `routeId` | "Tuyến không tồn tại." |
| `arrivalAt` không sau `departureAt` | `arrivalAt` | "Giờ đến phải sau giờ khởi hành." |
| `departureAt` không sau hiện tại (`LocalDateTime.now()`) | `departureAt` | "Giờ khởi hành phải sau thời điểm hiện tại." |
| Tàu có chuyến cuối, mà ga đi của tuyến khác ga đến của chuyến cuối | `routeId` | "Tàu SE1 đang ở Ga Huế sau chuyến trước, nên chuyến mới phải xuất phát từ Ga Huế." |
| Tàu có chuyến cuối, mà `departureAt` sớm hơn `arrivalAt` của nó cộng 30 phút | `departureAt` | "Giờ khởi hành sớm nhất là 11:15 02/11/2026, tức 30 phút sau khi chuyến trước đến Ga Huế." |

Mã tàu, tên ga và giờ trong hai thông báo cuối lấy từ dữ liệu thật; ví dụ trên là của SE1.

Vì sao phải khóa dòng tàu (`findWithLockById`) trước khi đọc chuyến cuối: nếu hai admin cùng
thêm chuyến cho một tàu, cả hai cùng đọc thấy một chuyến cuối, cùng qua kiểm tra, và tàu có hai
chuyến chồng giờ. Khóa dòng tàu bắt transaction thứ hai đợi transaction thứ nhất commit xong,
rồi mới đọc chuyến cuối, lúc đó đã là chuyến vừa thêm.

`TripCannotBeCancelledException` và `InvalidTripException` đặt ở `common/exception/`, như mọi
exception nghiệp vụ khác (ADR 0006).

Chuyển luôn phần đổi trạng thái thành nhãn tiếng Việt ("Mở bán", "Đã khởi hành", "Đã
hủy") từ `AdminService.tripStatusLabel(...)` sang service này hoặc sang template, để bảng
vẫn hiện đúng như bây giờ. Nhãn này cũng dùng cho Lịch chạy.

Sinh chỗ trong **cùng transaction** với việc lưu chuyến. Vì sao: nếu đang sinh chỗ mà gặp
lỗi, cả chuyến lẫn các chỗ đã sinh đều bị hủy. Nhờ vậy không bao giờ có chuyến thiếu chỗ.

```java
List<TripSeat> tripSeats = CoachLayout.seats().stream()
        .map(s -> new TripSeat(trip, s.coachNumber(), s.coachType(), s.seatCode(),
                s.cabinNumber(), s.priceModifier()))
        .toList();
tripSeatRepository.saveAll(tripSeats);
```

Đoạn code trên lấy 96 chỗ của cấu hình toa, tạo một `TripSeat` cho mỗi chỗ (chép luôn số toa,
mã chỗ và phụ thu vào dòng), rồi lưu tất cả. Chép phụ thu vào dòng nghĩa là đổi hằng số sau
này không làm đổi giá chuyến đã tạo (ADR 0009). 96 câu
`INSERT` mỗi chuyến là chấp nhận được ở quy mô dự án. Nếu cần nhanh hơn, viết một câu HQL
`insert ... select` trong `TripSeatRepository`.

Phần tùy chọn, theo D8: một method `@Scheduled(cron = "0 0 * * * *")` chạy mỗi giờ, chuyển
chuyến `SCHEDULED` đã khởi hành sang `COMPLETED`. Cần thêm `@EnableScheduling` ở một class
config. D8 chốt job này là không bắt buộc: chỉ làm sau khi phần chính đã xong và đã thử
tay. Không có job thì mọi luật vẫn đúng, vì luật nào cũng so giờ khởi hành với hiện tại.

### Bước 4. Controller: nhận request

Hai controller, vì hai nhóm trang sáng hai mục khác nhau trên sidebar.

`controller/TripAdminController` với `@RequestMapping("/admin/trips")` và
`activeNav = "trips"`:

| Method | Đường dẫn | Kết quả |
| --- | --- | --- |
| GET | `/admin/trips?trainId=&date=` | Trang danh sách. Hai tham số đều không bắt buộc (`@RequestParam(required = false)`, `date` kèm `@DateTimeFormat(iso = ISO.DATE)`). Thêm danh sách tàu cho ô lọc |
| POST | `/admin/trips/{id}/cancel` | Chuyển về danh sách kèm thông báo xanh (thành công) hoặc đỏ (bị từ chối); nút hủy có `th:data-confirm` để hỏi lại |

`controller/TrainTripAdminController` với `@RequestMapping("/admin/trains/{trainId}/trips")`
và `activeNav = "trains"`:

| Method | Đường dẫn | Kết quả |
| --- | --- | --- |
| GET | `/admin/trains/{trainId}/trips/new` | Form thêm chuyến. Model có tàu, `nextSlot`, `routeOptions`. Form điền sẵn `departureAt = nextSlot.earliestDeparture()` nếu có. Tàu không tồn tại: 404 |
| POST | `/admin/trains/{trainId}/trips` | Tạo xong thì chuyển về `/admin/trains/{trainId}` kèm "Đã thêm chuyến SE1 Ga Huế → Ga Đà Nẵng, khởi hành 08:00 03/11/2026 (96 chỗ)."; lỗi validation hoặc `InvalidTripException` thì gắn lỗi vào đúng ô và hiện lại form, kèm lại `nextSlot` và `routeOptions` |

Trang chi tiết tàu `GET /admin/trains/{id}` là của `TrainDetailController` (T06). Task này chỉ
thêm vào handler đó hai thuộc tính: `timetable` (từ `tripAdminService.timetable(id)`) và
`nextSlot`. Không khai lại đường dẫn này ở controller khác.

### Bước 5. Giao diện

- `templates/admin/train-detail.html` (của T06): thêm một card **Lịch chạy** trước phần sơ đồ
  toa. Bảng có các cột Khởi hành, Tuyến (ga đi → ga đến), Đến, Giá cơ bản, Trạng thái. Giờ đến
  hiện kèm ngày khi khác ngày khởi hành. Dòng có `chainBrokenAt` thì có thêm một dòng cảnh báo
  ngay phía trên, ví dụ "Chuỗi bị đứt: chuyến trước đến Ga Đà Nẵng, chuyến này đi từ Ga Huế."
  Dưới bảng ghi "Chuyến tiếp theo đi từ Ga Huế, sớm nhất 11:15 02/11/2026" (hoặc "Tàu chưa có
  chuyến, chọn tuyến bất kỳ"), và nút "Thêm chuyến" trỏ tới
  `/admin/trains/{id}/trips/new`. Tàu chưa có chuyến thì bảng hiện "Chưa có chuyến nào".
- `templates/admin/trip-form.html` (khung admin): dòng chữ chỉ đọc "Tàu SE1 · Thống Nhất 1";
  khi ga đi bị khóa thì thêm dòng chỉ đọc "Ga đi: Ga Huế (ga đến của chuyến trước)"; ô chọn
  tuyến từ `routeOptions` (hiện "Ga Huế → Ga Đà Nẵng · 103 km"); hai
  `<input type="datetime-local">` cho giờ khởi hành và giờ đến; ô giá cơ bản. Nút "Hủy" quay
  về trang chi tiết tàu. Không có ô chọn tàu, cũng không có ô chọn ga đi.
- `templates/admin/trips.html`: đã có bảng với các cột Tàu, Tuyến, Ngày đi, Giờ đi, Giờ đến,
  Giá cơ bản, Trạng thái, Thao tác. Sửa lại cho khớp dữ liệu mới. Giờ đến hiện kèm ngày khi
  khác ngày khởi hành, như `AdminService.arrivalText(...)` đang làm. Thêm một form `GET` lọc
  theo tàu (ô chọn "Tất cả tàu" và từng tàu) và ngày (`<input type="date">`), điền sẵn điều
  kiện đang lọc. Cột Thao tác chỉ còn nút Hủy, chỉ hiện khi chuyến còn hủy được; bỏ hai nút
  Sửa và Xóa đang `disabled`. Bỏ nút "Thêm chuyến" ở đầu bảng, thay bằng dòng chú thích "Thêm
  chuyến ở trang chi tiết của từng tàu."

### Bước 6. Gỡ màn hình chuyến khỏi phần dùng chung

- `controller/AdminController`: xóa handler `trips`.
- `service/AdminService`: xóa method `trips()`, `arrivalText(...)` và `tripStatusLabel(...)`
  (nếu không còn ai dùng).
- Xóa `model/response/AdminTrip.java` khi không còn ai dùng.

Dashboard **không** bị ảnh hưởng: số chuyến trên dashboard đếm bằng
`tripRepository.count()` qua `AdminService.totalTrips()`, không dùng `trips()`.

## 5. Luật và lỗi phải xử lý

- Tàu đã có chuyến: chuyến mới phải xuất phát từ ga đến của chuyến cuối chưa hủy. Ô chọn tuyến
  chỉ đưa ra tuyến hợp lệ, nhưng service vẫn kiểm tra lại, vì form có thể bị sửa.
- Giờ khởi hành sớm hơn giờ đến của chuyến cuối cộng 30 phút: báo lỗi dưới ô giờ khởi hành.
  Đúng bằng 30 phút thì được.
- Giờ đến không sau giờ khởi hành: báo lỗi dưới ô giờ đến.
- Tạo chuyến khởi hành trong quá khứ: báo lỗi.
- Chuyến 24 giờ trở lên lưu được bình thường.
- Hủy chuyến đã có booking `CONFIRMED`, chuyến đã khởi hành, hoặc chuyến không còn
  `SCHEDULED`: từ chối, báo rõ lý do bằng thông báo đỏ.
- Hủy chuyến ở giữa lịch không bị chặn; trang tàu đánh dấu chuỗi bị đứt.
- Tàu không tồn tại (người dùng tự sửa số trên đường dẫn): trang 404.

## 6. Kịch bản thử bằng tay

Dự án không viết test tự động (ADR 0007), nên bạn tự thử các trường hợp sau trên ứng dụng
đang chạy. Mở database bằng:

```
docker exec -it traingo-mysql mysql -utraingo -ptraingo --default-character-set=utf8mb4 traingo
```

Ngày của chuyến mẫu tính từ ngày seed chạy (`docs/RUNBOOK.md`, mục "Dữ liệu thử"). Xem lịch
của SE1 bằng:

```sql
SELECT t.id, o.code AS di, d.code AS den, t.departure_at, t.arrival_at, t.status
FROM trips t
JOIN trains tr ON tr.id = t.train_id
JOIN routes r ON r.id = t.route_id
JOIN stations o ON o.id = r.origin_station_id
JOIN stations d ON d.id = r.destination_station_id
WHERE tr.code = 'SE1'
ORDER BY t.departure_at;
```

Với dữ liệu mẫu, SE1 có 18 chuyến, chuyến cuối là Đồng Hới → Huế, khởi hành 08:00 và đến 10:45
ngày thứ 13 sau ngày seed chạy. Các ví dụ dưới đây gọi ngày đó là **ngày C**.

1. **Lịch chạy liền mạch.** Mở `/admin/trains/{id}` của SE1: thấy 18 chuyến theo thứ tự thời
   gian, ga đến của mỗi chuyến bằng ga đi của chuyến sau, không có dòng "Chuỗi bị đứt". Dưới
   bảng ghi chuyến tiếp theo đi từ Ga Huế, sớm nhất 11:15 ngày C.
2. **Form điền sẵn theo chuyến trước.** Bấm "Thêm chuyến" của SE1: dòng "Ga đi: Ga Huế"; ô
   tuyến chỉ có Huế → Đồng Hới và Huế → Đà Nẵng; ô giờ khởi hành điền sẵn 11:15 ngày C.
3. **Thêm chuyến nối chặng.** Chọn Huế → Đà Nẵng, khởi hành 08:00 ngày sau ngày C, đến 09:45,
   giá 100.000: thông báo xanh, lịch có thêm dòng cuối. Lấy id chuyến vừa tạo, chạy
   `SELECT status, COUNT(*) FROM trip_seats WHERE trip_id = <id> GROUP BY status;`. Kết quả
   phải là đúng 96 dòng, tất cả `AVAILABLE`.
4. **Chỗ khớp cấu hình toa.** Với chuyến vừa tạo, chạy `SELECT coach_number, coach_type,
   price_modifier, COUNT(*) FROM trip_seats WHERE trip_id = <id> GROUP BY coach_number,
   coach_type, price_modifier;`. Phải ra đúng 4 dòng: toa 1 và 2 `SEAT` phụ thu 0 mỗi toa 32
   chỗ, toa 3 và 4 `SLEEPER` phụ thu 200000 mỗi toa 16 chỗ.
5. **Sai ga đi bị từ chối.** Mở lại form của SE1 (giờ ga đi là Ga Đà Nẵng). Lấy id tuyến Nha
   Trang → Sài Gòn bằng SQL, mở DevTools, sửa `value` của một `<option>` trong ô tuyến thành id
   đó, chọn option ấy rồi gửi: form hiện lại, lỗi dưới ô tuyến "Tàu SE1 đang ở Ga Đà Nẵng sau
   chuyến trước, nên chuyến mới phải xuất phát từ Ga Đà Nẵng." Không có chuyến mới.
6. **Giờ khởi hành quá sớm bị từ chối.** Chuyến cuối giờ đến Đà Nẵng lúc 09:45. Chọn Đà Nẵng →
   Huế, khởi hành 10:00 cùng ngày (mới 15 phút), đến 12:00: lỗi dưới ô giờ khởi hành, nêu giờ
   sớm nhất 10:15. Đổi thành đúng 10:15: lưu được (đủ 30 phút là hợp lệ).
7. **Không thể chồng giờ.** Chuyến cuối của SE1 giờ chạy 10:15 - 12:00. Thử giờ khởi hành 11:00
   cùng ngày (lúc chuyến cuối còn đang chạy), rồi thử một ngày nằm giữa lịch, ví dụ ngày sau
   ngày seed chạy: cả hai lần đều bị lỗi giờ khởi hành sớm nhất. Không có cách nào chèn chuyến
   vào giữa lịch.
8. **Giờ đến không sau giờ đi.** Nhập giờ đến bằng hoặc trước giờ khởi hành: lỗi dưới ô giờ
   đến.
9. **Tàu chưa có chuyến chọn tuyến bất kỳ, chuyến dài hơn 24 giờ lưu được.** Mở form của SE2:
   không có dòng ga đi, ô tuyến có đủ 16 tuyến xếp theo lý trình, ô giờ khởi hành để trống. Chọn
   Hà Nội → Sài Gòn, khởi hành 19:00 một ngày tuần sau, đến 00:30 hai ngày sau đó (29 giờ 30
   phút), giá 1.730.000: lưu được, lịch của SE2 hiện giờ đến kèm ngày. Mở form của SE2 lần nữa:
   ga đi bị khóa ở Ga Sài Gòn.
10. **Không tạo được chuyến trong quá khứ.** Mở form của SE3 (chưa có chuyến), chọn ngày hôm
    qua: lỗi dưới ô giờ khởi hành.
11. **Hủy chuyến đã có người đặt bị từ chối.** Tìm một chuyến có booking `CONFIRMED` bằng
    `SELECT DISTINCT trip_id FROM bookings WHERE booking_status = 'CONFIRMED';` (ví dụ chuyến
    Đà Nẵng → Nha Trang ngày sau ngày seed chạy). Ở `/admin/trips`, bấm Hủy chuyến đó: thông báo
    đỏ, chuyến vẫn `SCHEDULED`.
12. **Hủy chuyến ở giữa làm chuỗi đứt.** Chuyến Đà Nẵng → Huế của SE1 (ngày thứ 5 sau ngày seed
    chạy) chỉ có booking đã hủy, nên hủy được: bấm Hủy, thông báo xanh, trạng thái "Đã hủy".
    Mở trang SE1: dòng Đà Nẵng → Huế hiện "Đã hủy", và ngay trên dòng Huế → Đồng Hới có cảnh báo
    "Chuỗi bị đứt: chuyến trước đến Ga Đà Nẵng, chuyến này đi từ Ga Huế." Nút "Thêm chuyến" vẫn
    dùng được.
13. **Chuyến đã hủy không tính là chuyến cuối.** Hủy chuyến cuối vừa thêm ở bước 6 (Đà Nẵng →
    Huế, 10:15 - 12:00): mở form của SE1, ga đi quay về Ga Đà Nẵng, giờ sớm nhất quay về 10:15
    (30 phút sau chuyến Huế → Đà Nẵng ở bước 3).
14. **Không hủy được chuyến đã khởi hành.** Bấm Hủy một chuyến `COMPLETED` của SE1 (4 chuyến
    đầu): thông báo đỏ.
15. **Lọc danh sách.** Ở `/admin/trips`, chọn SE1 và ngày sau ngày seed chạy: chỉ còn chuyến Đà
    Nẵng → Nha Trang. Chọn SE2, bỏ trống ngày: chỉ còn chuyến tạo ở bước 9.
16. Gõ `/admin/trains/999999/trips/new`: trang 404.

Thử xong, muốn trả dữ liệu mẫu về như cũ thì làm mới database theo `docs/RUNBOOK.md`.

## 7. Lỗi hay gặp

- Thiếu `@EntityGraph` ở câu truy vấn danh sách hay lịch chạy: trang lỗi
  `LazyInitializationException` khi hiện tên ga của tuyến.
- Lấy chuyến cuối mà quên bỏ chuyến đã hủy: hủy chuyến cuối xong, form vẫn khóa ga đi theo
  chuyến đã hủy.
- Chỉ lọc ô chọn tuyến ở giao diện mà không kiểm tra lại trong service: sửa HTML là tạo được
  chuyến "dịch chuyển" sang một ga khác.
- Nhận `trainId` từ form thay vì từ đường dẫn: admin thêm chuyến cho tàu này lại ghi vào tàu
  khác.
- Không khóa dòng tàu khi tạo chuyến: hai tab cùng thêm chuyến có thể cho ra hai chuyến chồng
  giờ.
- Cộng 24 giờ hay so "giờ đến nhỏ hơn giờ đi" như cách cũ: giờ đã là ngày-giờ đầy đủ, chỉ cần
  so `arrivalAt` với `departureAt`.
- Gắn class `datatable` cho bảng Lịch chạy: `admin.js` cho bảng đó sắp xếp lại được, thứ tự
  thời gian và dòng cảnh báo chuỗi đứt bị xáo trộn.
- Lưu chuyến và sinh chỗ ở hai transaction khác nhau: lỗi giữa chừng để lại chuyến thiếu
  chỗ.

## 8. Kiểm tra trước khi báo xong

- [ ] Trang SE1 hiện lịch 18 chuyến liền mạch; thêm chuyến nối chặng thành công, database có
      96 dòng `trip_seats` mới của chuyến đó (đúng 4 toa như `CoachLayout`), và trang tìm
      chuyến của T05 tìm thấy chuyến này.
- [ ] Sai ga đi, giờ khởi hành sớm hơn 30 phút sau chuyến trước, giờ đến không sau giờ đi:
      đều bị từ chối, lỗi nằm dưới đúng ô.
- [ ] Tàu chưa có chuyến chọn được tuyến bất kỳ; chuyến Hà Nội → Sài Gòn hơn 24 giờ lưu được.
- [ ] Hủy chuyến ở giữa: trang tàu đánh dấu chuỗi bị đứt; hủy chuyến cuối: ga đi và giờ sớm
      nhất của form lùi về chuyến chưa hủy trước đó. T05 không còn hiện chuyến đã hủy.
- [ ] Hủy chuyến có booking `CONFIRMED` trong dữ liệu mẫu (seed) bị từ chối kèm lý do.
- [ ] `/admin/trips` lọc được theo tàu và ngày.
- [ ] `AdminController` không còn handler `trips`; dashboard vẫn hiện đúng số chuyến.
- [ ] Đã thử hết các bước ở mục 6.
- [ ] `.\mvnw.cmd compile` chạy không lỗi và ứng dụng khởi động được
      (`.\mvnw.cmd spring-boot:run`).
- [ ] Có một dòng trong `docs/WORKLOG.md`, nằm trong cùng commit với code.
