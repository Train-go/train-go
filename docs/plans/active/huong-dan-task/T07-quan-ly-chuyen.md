# T07 Quản lý chuyến

> **Tóm tắt:** sau task này, admin ở trang `/admin/trips` không chỉ xem danh sách chuyến
> mà còn tạo chuyến mới và hủy chuyến. Mỗi chuyến mới tạo có sẵn đủ chỗ để bán.

| Đợt | Phần việc | Độ khó | Cần xong trước | Task đang chờ task này | Tên branch |
| --- | --- | --- | --- | --- | --- |
| 2 | A | Vừa (khoảng 2-3 ngày) | quyết định D2, D8; nên có T03, T04 trước | Không task nào chờ; task này hoàn thành luồng admin của `SPEC.md` mục 50 | `feat/t07-trip-admin` |

Các từ như transaction, `@EntityGraph`, flash message được giải thích trong `README.md`
cùng thư mục. Cách chia package theo tầng (`controller/`, `service/`, `model/`...) xem
ADR 0006.

## 1. Task này làm gì

Một **chuyến** (`Trip`) là một lần một tàu chạy trên một tuyến vào một ngày giờ cụ thể.
Ví dụ: tàu SE19 chạy Hà Nội - Đà Nẵng lúc 20:05 ngày 06/10/2026.

Trang `/admin/trips` **đã hiện danh sách chuyến thật** từ database (anh Phong làm ở commit
`9ba771b`), nhưng chỉ để xem, và code đang nằm chung trong `AdminController` và
`AdminService`. Task này làm ba việc:

1. Chuyển màn hình chuyến ra controller và service riêng, theo ADR 0006.
2. Cho admin tạo chuyến mới.
3. Cho admin hủy chuyến.

Điểm quan trọng nhất: khi tạo chuyến, hệ thống tự sinh một dòng `TripSeat` trạng thái
`AVAILABLE` (còn trống) cho **mọi** chỗ của tàu (`SPEC.md` mục 38). Tàu có 96 chỗ thì
chuyến mới có 96 dòng `trip_seats`. Vì sao cần: trạng thái đặt chỗ thuộc về từng chuyến,
không thuộc về tàu. Cùng một ghế A01 có thể đã bán ở chuyến hôm nay nhưng còn trống ở
chuyến ngày mai.

## 2. Phạm vi

Làm:

- Chuyển danh sách chuyến sang `controller/TripAdminController` và `service/TripAdminService`.
- Tạo chuyến.
- Hủy chuyến (theo quyết định D2).

Không làm:

- Sửa chuyến đã tạo.
- Xóa chuyến (quyết định D2: chuyến chỉ hủy, không xóa).
- Đổi tàu của chuyến.

## 3. Dữ liệu dùng tới

```
trains 1 ──< trips >── 1 routes
              │
              └──< trip_seats >── 1 seats ──> coaches ──> trains

trips:      id, train_id, route_id, departure_date, departure_time,
            arrival_time, base_price, status (SCHEDULED | COMPLETED | CANCELLED)
trip_seats: id, trip_id, seat_id, status, held_by_user_id, held_until
            UNIQUE (trip_id, seat_id)
bookings:   trip_id, booking_status   (để biết chuyến đã có booking chưa)
```

Đọc sơ đồ:

- Một chuyến thuộc đúng một tàu và một tuyến. Một tàu, một tuyến có nhiều chuyến.
- Một chuyến có nhiều `trip_seats`, mỗi dòng ứng với một ghế của tàu đó.
- Trong một chuyến, mỗi ghế chỉ có một dòng (`UNIQUE (trip_id, seat_id)`).
- Bảng `bookings` chỉ dùng để kiểm tra chuyến đã có người đặt chưa, trước khi cho hủy.

## 4. Các bước làm

### Bước 1. Model: thêm method vào entity và tạo request

Entity `entity/Trip` và `entity/TripSeat` đã có. Thêm vào `Trip` hai method:

```java
/** Departure date and time together. */
public LocalDateTime departureAt() {
    return LocalDateTime.of(departureDate, departureTime);
}

public void cancel() {
    if (status != TripStatus.SCHEDULED) {
        throw new IllegalStateException("Only a scheduled trip can be cancelled");
    }
    status = TripStatus.CANCELLED;
}
```

- `departureAt()` ghép ngày và giờ khởi hành thành một thời điểm. T05, T09, T10 cũng dùng
  method này. Task nào làm trước thì thêm; task sau dùng lại, không viết bản thứ hai.
- `cancel()` chỉ cho hủy chuyến đang `SCHEDULED` (đã lên lịch).

Tạo class mới `model/request/TripRequest`, chứa dữ liệu của form tạo chuyến:

| Field | Kiểu | Luật kiểm tra |
| --- | --- | --- |
| `trainId` | `Long` | `@NotNull` |
| `routeId` | `Long` | `@NotNull` |
| `departureDate` | `LocalDate` | `@NotNull`, `@DateTimeFormat(iso = ISO.DATE)` |
| `departureTime` | `LocalTime` | `@NotNull`, `@DateTimeFormat(pattern = "HH:mm")` |
| `arrivalTime` | `LocalTime` | `@NotNull`, `@DateTimeFormat(pattern = "HH:mm")` |
| `basePrice` | `Long` | `@NotNull`, `@Positive`, `@Max(20_000_000)` |

`@DateTimeFormat` cho Spring biết cách đọc ngày và giờ từ ô nhập của form.

Dòng hiển thị trên bảng hiện là `model/response/AdminTrip` (các field đã định dạng sẵn
thành chuỗi). Theo ADR 0006, class `Admin*` biến mất khi màn hình nối hẳn vào nghiệp vụ
riêng. Bạn có thể đưa thẳng `Trip` ra view rồi định dạng ngày giờ trong template, hoặc
tạo một response mới cho bảng chuyến; xong thì xóa `AdminTrip`.

### Bước 2. Repository: dùng lại câu truy vấn có sẵn

Danh sách chuyến **không cần câu truy vấn mới**. `repository/TripRepository` đã có:

- `findAllByOrderByDepartureDateAscDepartureTimeAsc()`, kèm `@EntityGraph` đọc luôn tàu,
  tuyến và hai ga trong cùng một câu truy vấn. Thiếu `@EntityGraph` thì trang danh sách lỗi
  `LazyInitializationException` khi hiện tên ga (xem `README.md`). Danh sách sắp từ chuyến
  sớm nhất tới muộn nhất.

Thêm vào `repository/BookingRepository`, để biết chuyến đã có booking ở một trạng thái nào
đó chưa:

```java
boolean existsByTripIdAndBookingStatus(Long tripId, BookingStatus status);
```

Với `repository/SeatRepository`, dùng lại câu truy vấn T06 đã viết:
`findByCoachTrainIdOrderByCoachCoachNumberAscCodeAsc(trainId)`.

### Bước 3. Service: danh sách, tạo và hủy chuyến

Tạo `service/TripAdminService`:

| Method | Việc |
| --- | --- |
| `List<Trip> findAll()` | Danh sách, gọi câu truy vấn có sẵn ở Bước 2 |
| `@Transactional Trip create(TripRequest form)` | Kiểm tra các luật bên dưới; lưu `Trip`; sinh `TripSeat` cho mọi ghế của tàu; trả chuyến |
| `@Transactional Trip cancel(long tripId)` | Theo D2: chỉ hủy chuyến `SCHEDULED`, chưa khởi hành, chưa có booking `CONFIRMED`. Sai luật thì ném `TripCannotBeCancelledException` kèm lý do |

`TripCannotBeCancelledException` đặt ở `common/exception/`, như mọi exception nghiệp vụ
khác (ADR 0006).

Chuyển luôn phần đổi trạng thái thành nhãn tiếng Việt ("Mở bán", "Đã khởi hành", "Đã
hủy") từ `AdminService.tripStatusLabel(...)` sang service này hoặc sang template, để bảng
vẫn hiện đúng như bây giờ.

Luật khi tạo chuyến:

- Tàu và tuyến phải tồn tại.
- Tàu phải có ít nhất một toa. Vì sao: tàu không có toa thì chuyến không có chỗ nào để bán.
- Thời điểm khởi hành phải sau hiện tại (lấy bằng `LocalDateTime.now()`).
- Giờ đến phải khác giờ đi. Giờ đến nhỏ hơn giờ đi nghĩa là đến vào hôm sau
  (`docs/product/domain-model.md`).

Sinh chỗ trong **cùng transaction** với việc lưu chuyến. Vì sao: nếu đang sinh chỗ mà gặp
lỗi, cả chuyến lẫn các chỗ đã sinh đều bị hủy. Nhờ vậy không bao giờ có chuyến thiếu chỗ.

```java
List<TripSeat> tripSeats = seatRepository
        .findByCoachTrainIdOrderByCoachCoachNumberAscCodeAsc(train.getId())
        .stream()
        .map(seat -> new TripSeat(trip, seat))
        .toList();
tripSeatRepository.saveAll(tripSeats);
```

Đoạn code trên lấy mọi ghế của tàu, tạo một `TripSeat` cho mỗi ghế, rồi lưu tất cả. 96 câu
`INSERT` mỗi chuyến là chấp nhận được ở quy mô dự án. Nếu cần nhanh hơn, viết một câu HQL
`insert ... select` trong `TripSeatRepository`.

Phần tùy chọn, theo D8: một method `@Scheduled(cron = "0 0 * * * *")` chạy mỗi giờ, chuyển
chuyến `SCHEDULED` đã khởi hành sang `COMPLETED`. Cần thêm `@EnableScheduling` ở một class
config. D8 chốt job này là không bắt buộc: chỉ làm sau khi phần chính đã xong và đã thử
tay. Không có job thì mọi luật vẫn đúng, vì luật nào cũng so giờ khởi hành với hiện tại.

### Bước 4. Controller: nhận request

Tạo `controller/TripAdminController` với `@RequestMapping("/admin/trips")`, và
`activeNav = "trips"` để sidebar sáng đúng mục:

| Method | Đường dẫn | Kết quả |
| --- | --- | --- |
| GET | `/admin/trips` | Trang danh sách |
| GET | `/admin/trips/new` | Form, kèm `@ModelAttribute("trains")` và `("routes")` cho hai ô chọn |
| POST | `/admin/trips` | Tạo xong thì chuyển về danh sách kèm "Đã tạo chuyến SE19 ngày 06/10/2026 (96 chỗ)."; lỗi thì hiện lại form |
| POST | `/admin/trips/{id}/cancel` | Chuyển về danh sách kèm thông báo xanh (thành công) hoặc đỏ (bị từ chối); nút hủy có `th:data-confirm` để hỏi lại |

### Bước 5. Giao diện

- `templates/admin/trips.html`: đã có bảng với các cột Tàu, Tuyến, Ngày đi, Giờ đi, Giờ
  đến, Giá cơ bản, Trạng thái. Sửa lại cho khớp dữ liệu mới, thêm "+1 ngày" khi chuyến qua
  đêm, thêm cột Thao tác với nút Hủy (chỉ hiện khi chuyến còn hủy được) và nút "Tạo chuyến"
  trỏ tới `/admin/trips/new`.
- `templates/admin/trip-form.html`: ô chọn tàu (hiện `mã · tên`), ô chọn tuyến (hiện mã
  tuyến như `SGN-NTR`), một `<input type="date">`, hai `<input type="time">`, và ô giá.

### Bước 6. Gỡ màn hình chuyến khỏi phần dùng chung

- `controller/AdminController`: xóa handler `trips`.
- `service/AdminService`: xóa method `trips()` và `tripStatusLabel(...)` (nếu không còn ai
  dùng).
- Xóa `model/response/AdminTrip.java` khi không còn ai dùng.

Dashboard **không** bị ảnh hưởng: số chuyến trên dashboard đếm bằng
`tripRepository.count()` qua `AdminService.totalTrips()`, không dùng `trips()`.

## 5. Luật và lỗi phải xử lý

- Tạo chuyến với tàu chưa có toa: báo lỗi, không lưu gì.
- Tạo chuyến khởi hành trong quá khứ: báo lỗi.
- Hủy chuyến đã có booking `CONFIRMED`, chuyến đã khởi hành, hoặc chuyến không còn
  `SCHEDULED`: từ chối, báo rõ lý do bằng thông báo đỏ.

## 6. Kịch bản thử bằng tay

Dự án không viết test tự động (ADR 0007), nên bạn tự thử các trường hợp sau trên ứng dụng
đang chạy. Mở database bằng:

```
docker exec -it traingo-mysql mysql -utraingo -ptraingo --default-character-set=utf8mb4 traingo
```

1. **Tạo chuyến sinh đủ chỗ.** Tạo chuyến cho tàu SE19 (96 chỗ) vào tuần sau. Lấy id chuyến
   vừa tạo, chạy `SELECT status, COUNT(*) FROM trip_seats WHERE trip_id = <id> GROUP BY status;`.
   Kết quả phải là đúng 96 dòng, tất cả `AVAILABLE`.
2. **Tàu chưa có toa bị từ chối.** Tạo một tàu mới ở `/admin/trains` (chưa thêm toa), rồi
   tạo chuyến cho tàu đó: form báo lỗi, bảng `trips` không có dòng mới.
3. **Không tạo được chuyến trong quá khứ.** Chọn ngày hôm qua: form báo lỗi.
4. **Hủy chuyến đã có người đặt bị từ chối.** Tìm một chuyến có booking `CONFIRMED` trong dữ
   liệu mẫu bằng `SELECT DISTINCT trip_id FROM bookings WHERE booking_status = 'CONFIRMED';`,
   bấm Hủy chuyến đó: thông báo đỏ, chuyến vẫn `SCHEDULED`.
5. **Hủy chuyến vừa tạo.** Hủy chuyến ở bước 1: thông báo xanh, trạng thái thành "Đã hủy".

## 7. Lỗi hay gặp

- Thiếu `@EntityGraph` ở câu truy vấn danh sách: trang lỗi `LazyInitializationException`
  khi hiện tên ga của tuyến. Dùng lại câu truy vấn có sẵn thì không gặp lỗi này.
- Lưu chuyến và sinh chỗ ở hai transaction khác nhau: lỗi giữa chừng để lại chuyến thiếu
  chỗ.
- Viết thêm một câu truy vấn danh sách mới trong khi `TripRepository` đã có sẵn: hai câu
  gần giống nhau, sau này sửa một quên một.

## 8. Kiểm tra trước khi báo xong

- [ ] Tạo chuyến SE19 cho tuần sau: database có 96 dòng `trip_seats` mới của chuyến đó, và
      trang tìm chuyến của T05 tìm thấy chuyến này.
- [ ] Hủy chuyến vừa tạo thành công; T05 không còn hiện chuyến đó.
- [ ] Hủy chuyến có booking trong dữ liệu mẫu (seed) bị từ chối kèm lý do.
- [ ] `AdminController` không còn handler `trips`; dashboard vẫn hiện đúng số chuyến.
- [ ] Đã thử hết các bước ở mục 6.
- [ ] `.\mvnw.cmd compile` chạy không lỗi và ứng dụng khởi động được
      (`.\mvnw.cmd spring-boot:run`).
- [ ] Có một dòng trong `docs/WORKLOG.md`, nằm trong cùng commit với code.
