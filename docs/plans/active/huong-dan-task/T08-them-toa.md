# T08 Thêm toa vào tàu

| Wave | Lane | Cỡ | Phụ thuộc | Mở khóa | Branch |
| --- | --- | --- | --- | --- | --- |
| 2 | C | M | T06, D3 | "Cấu hình toa" và "Sinh ghế tự động" của `SPEC.md` mục 37 | `feat/t08-add-coach` |

## Mục tiêu

Trên trang chi tiết tàu (T06), admin nhập loại toa, sức chứa, phụ thu, bấm "Thêm toa".
Hệ thống tự đánh số toa, tự sinh ghế hoặc giường, và theo D3 sinh thêm chỗ cho các
chuyến sắp chạy của tàu đó. Admin không bao giờ nhập từng ghế.

## Phạm vi

Trong phạm vi: thêm toa. Ngoài phạm vi: sửa, xóa toa (`docs/product/admin.md`).

## Database

```
trains 1 ──< coaches 1 ──< seats
trips (của tàu) 1 ──< trip_seats >── 1 seats

coaches: train_id, coach_number, coach_type, capacity, price_modifier
         UNIQUE (train_id, coach_number), CHECK capacity > 0, price_modifier >= 0
seats:   coach_id, code, type, cabin_number
         UNIQUE (coach_id, code)
```

## Quy tắc sinh ghế

Đã chốt trong `docs/product/domain-model.md`:

| Loại toa | Sức chứa hợp lệ | Ghế sinh ra |
| --- | --- | --- |
| `SEAT` | 1 đến 80 | `A01`...`A80`, type `SEAT`, `cabinNumber` null |
| `SLEEPER` | 4 đến 40, chia hết cho 4 | `B01`...`B40`, type `BED`, cabin = (i - 1) / 4 + 1 |

Số toa mới = số toa lớn nhất hiện có của tàu + 1.

## Model

Có sẵn: `Coach`, `Seat`, `CoachType`, `SeatType`.

Viết mới `train/model/CoachForm`:

| Field | Kiểu | Validation |
| --- | --- | --- |
| `coachType` | `CoachType` | `@NotNull` |
| `capacity` | `Integer` | `@NotNull`, `@Min(1)`, `@Max(80)` |
| `priceModifier` | `Long` | `@NotNull`, `@PositiveOrZero`, `@Max(10_000_000)` |

Luật phụ thuộc loại toa (giường 4-40, chia hết cho 4) kiểm tra trong service.

Viết mới `train/service/SeatCodeGenerator`, một class Java thuần, không phụ thuộc
Spring, để test được bằng unit test:

```java
record SeatSpec(String code, SeatType type, Integer cabinNumber) { }

static List<SeatSpec> generate(CoachType type, int capacity) { ... }
```

## Repository

`CoachRepository`:

```java
@Query("select coalesce(max(c.coachNumber), 0) from Coach c where c.train.id = :trainId")
int findMaxCoachNumber(Long trainId);
```

`TripRepository`, để áp D3:

```java
List<Trip> findByTrainIdAndStatusAndDepartureDateGreaterThanEqual(
        Long trainId, TripStatus status, LocalDate fromDate);
```

Lọc thêm trong Java những chuyến có `departureAt()` sau hiện tại (T07).

## Service

`train/service/CoachService`, nhận `Clock`:

- `@Transactional Coach addCoach(long trainId, CoachForm form)`:
  1. Tìm tàu, không thấy thì 404.
  2. Kiểm tra sức chứa theo loại toa, sai thì ném `InvalidCapacityException`.
  3. `coachNumber = findMaxCoachNumber(trainId) + 1`.
  4. Lưu `Coach`, rồi lưu các `Seat` từ `SeatCodeGenerator.generate(...)`.
  5. D3: với mỗi chuyến `SCHEDULED` chưa khởi hành của tàu, lưu `TripSeat` mới
     cho từng ghế mới.

Cả năm bước trong một transaction: lỗi ở bước nào thì không có gì được ghi.

Hai admin thêm toa cùng lúc có thể cùng tính ra một số toa; khóa unique
`uk_coaches_train_number` chặn người thứ hai. Controller bắt
`DataIntegrityViolationException` và báo "Vui lòng thử lại".

## Controller

`train/controller/CoachAdminController`, `@RequestMapping("/admin/trains/{trainId}/coaches")`.
Đây là controller riêng, để không sửa controller của T06:

| Method | URL | Kết quả |
| --- | --- | --- |
| POST | `/admin/trains/{trainId}/coaches` | Thành công: redirect `/admin/trains/{trainId}` kèm "Đã thêm Toa 05 (32 chỗ)." Lỗi: redirect về trang chi tiết kèm thông báo lỗi và dữ liệu đã nhập (flash) |

Form nằm trên trang chi tiết, không có trang form riêng.

## View

Thêm vào `templates/admin/train-detail.html` (của T06) một card "Thêm toa": ô chọn
loại (Ghế ngồi / Giường nằm), sức chứa, phụ thu, nút "Thêm toa". Ghi chú ngay dưới ô
sức chứa: "Giường: 4 đến 40, chia hết cho 4". Sau khi redirect, sơ đồ do T06 vẽ tự có
toa mới vì trang tải lại API.

## Test

- Unit test `SeatCodeGeneratorTest`: `SEAT` 16 ra `A01`-`A16`; `SLEEPER` 8 ra
  `B01`-`B08`, `B04` thuộc cabin 1 và `B05` thuộc cabin 2.
- Unit test `CoachServiceTest`: `SLEEPER` 10 bị từ chối; tàu có 4 toa thì toa mới là
  số 5; tàu có 2 chuyến sắp chạy thì sinh thêm `TripSeat` cho cả hai chuyến.

## Xong khi

- [ ] Thêm toa giường 8 chỗ cho SP1: sơ đồ hiện Toa 05 với 2 cabin.
- [ ] Database có `TripSeat` của toa mới cho mọi chuyến SP1 sắp chạy, không có cho
      chuyến đã qua hay đã hủy.
- [ ] Nhập giường 10 chỗ bị báo lỗi, không có gì được ghi.
- [ ] `.\mvnw.cmd test` xanh. Một dòng `docs/WORKLOG.md`.
