# T07 Quản lý chuyến

| Wave | Lane | Cỡ | Phụ thuộc | Mở khóa | Branch |
| --- | --- | --- | --- | --- | --- |
| 2 | A | M | D2, D8 (nên có T03, T04 trước) | Luồng admin đầy đủ của `SPEC.md` mục 50 | `feat/t07-trip-admin` |

## Mục tiêu

Admin xem danh sách chuyến, tạo chuyến mới, hủy chuyến tại `/admin/trips`. Khi tạo
chuyến, hệ thống tự sinh một `TripSeat` trạng thái `AVAILABLE` cho **mọi** chỗ của
tàu (`SPEC.md` mục 38): tàu 96 chỗ thì chuyến mới có 96 dòng `trip_seats`.

## Phạm vi

Trong phạm vi: danh sách, tạo, hủy chuyến (D2). Ngoài phạm vi: sửa chuyến đã tạo,
xóa chuyến (D2), đổi tàu của chuyến.

## Database

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

## Model

Có sẵn: `trip/model/Trip`, `TripSeat`. Thêm vào `Trip`:

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

`departureAt()` cũng được T05, T09, T10 dùng. Ai làm trước thì thêm; người sau dùng
lại, không viết thêm bản thứ hai.

Viết mới `trip/model/TripForm`:

| Field | Kiểu | Validation |
| --- | --- | --- |
| `trainId` | `Long` | `@NotNull` |
| `routeId` | `Long` | `@NotNull` |
| `departureDate` | `LocalDate` | `@NotNull`, `@DateTimeFormat(iso = ISO.DATE)` |
| `departureTime` | `LocalTime` | `@NotNull`, `@DateTimeFormat(pattern = "HH:mm")` |
| `arrivalTime` | `LocalTime` | `@NotNull`, `@DateTimeFormat(pattern = "HH:mm")` |
| `basePrice` | `Long` | `@NotNull`, `@Positive`, `@Max(20_000_000)` |

## Repository

`TripRepository`:

```java
@EntityGraph(attributePaths = {"train", "route.originStation",
        "route.destinationStation"})
List<Trip> findAllByOrderByDepartureDateDescDepartureTimeAsc();
```

`BookingRepository`:

```java
boolean existsByTripIdAndBookingStatus(Long tripId, BookingStatus status);
```

`SeatRepository`: dùng lại query của T06,
`findByCoachTrainIdOrderByCoachCoachNumberAscCodeAsc(trainId)`.

## Service

`trip/service/TripAdminService`, nhận `Clock`:

| Method | Việc |
| --- | --- |
| `List<Trip> findAll()` | Danh sách, mới nhất trước |
| `@Transactional Trip create(TripForm form)` | Xem các luật bên dưới; lưu `Trip`; sinh `TripSeat` cho mọi ghế của tàu; trả chuyến |
| `@Transactional Trip cancel(long tripId)` | Theo D2: chỉ hủy chuyến `SCHEDULED`, chưa khởi hành, chưa có booking `CONFIRMED`. Vi phạm thì ném `TripCannotBeCancelledException` kèm lý do |

Luật khi tạo chuyến:

- Tàu và tuyến phải tồn tại.
- Tàu phải có ít nhất một toa; không thì chuyến không có chỗ nào để bán.
- Thời điểm khởi hành phải sau hiện tại (dùng `Clock`).
- Giờ đến khác giờ đi. Giờ đến nhỏ hơn giờ đi nghĩa là đến hôm sau
  (`docs/product/domain-model.md`).

Sinh chỗ trong **cùng transaction** với việc lưu chuyến, để không bao giờ có chuyến
thiếu chỗ:

```java
List<TripSeat> tripSeats = seatRepository
        .findByCoachTrainIdOrderByCoachCoachNumberAscCodeAsc(train.getId())
        .stream()
        .map(seat -> new TripSeat(trip, seat))
        .toList();
tripSeatRepository.saveAll(tripSeats);
```

96 câu `INSERT` mỗi chuyến là chấp nhận được ở quy mô này. Nếu cần nhanh hơn, viết
một câu HQL `insert ... select` trong `TripSeatRepository`.

Tùy chọn theo D8: một method `@Scheduled(cron = "0 0 * * * *")` chuyển chuyến
`SCHEDULED` đã khởi hành sang `COMPLETED`. Cần thêm `@EnableScheduling` ở một class
config. Chỉ làm khi Phi chốt D8 có job.

## Controller

`trip/controller/TripAdminController`, `@RequestMapping("/admin/trips")`,
`activeNav = "trips"`:

| Method | URL | Kết quả |
| --- | --- | --- |
| GET | `/admin/trips` | Danh sách |
| GET | `/admin/trips/new` | Form, kèm `@ModelAttribute("trains")` và `("routes")` cho hai ô chọn |
| POST | `/admin/trips` | Redirect kèm "Đã tạo chuyến SE19 ngày 06/10/2026 (96 chỗ).", hoặc form kèm lỗi |
| POST | `/admin/trips/{id}/cancel` | Redirect kèm thông báo xanh hoặc đỏ, có `th:data-confirm` |

## View

- `templates/admin/trips.html`: cột Tàu, Tuyến, Ngày đi, Giờ đi, Giờ đến (thêm
  "+1 ngày" khi qua đêm), Giá cơ bản, Trạng thái (badge), Thao tác (nút Hủy chỉ hiện
  khi chuyến còn hủy được).
- `templates/admin/trip-form.html`: ô chọn tàu (`mã · tên`), ô chọn tuyến
  (`route.code`), `<input type="date">`, hai `<input type="time">`, giá.

## Gỡ dữ liệu giả

`AdminController` xóa handler `trips`; xóa `admin/model/Trip.java`.

**Giữ** `AdminService.trips()` cho tới khi T13 làm lại dashboard, vì dashboard đang
dùng `adminService.trips().size()`. T13 sẽ xóa nốt.

## Test

- Unit test `TripAdminServiceTest` (Mockito, `Clock` cố định): tạo chuyến cho tàu 96
  chỗ thì `saveAll` nhận đúng 96 `TripSeat`, tất cả `AVAILABLE`; tàu chưa có toa bị
  từ chối; hủy chuyến đã có booking `CONFIRMED` bị từ chối.

## Xong khi

- [ ] Tạo chuyến SE19 cho tuần sau: database có 96 dòng `trip_seats` mới của chuyến
      đó, và T05 tìm thấy chuyến.
- [ ] Hủy chuyến vừa tạo thành công; T05 không còn hiện chuyến đó.
- [ ] Hủy chuyến có booking của seed bị từ chối kèm lý do.
- [ ] `.\mvnw.cmd test` xanh. Một dòng `docs/WORKLOG.md`.
