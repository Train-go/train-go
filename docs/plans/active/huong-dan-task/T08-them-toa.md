# T08 Thêm toa vào tàu

> **Tóm tắt:** sau task này, admin thêm được toa mới ngay trên trang chi tiết tàu; hệ
> thống tự đánh số toa và tự sinh đủ ghế hoặc giường.

| Đợt | Phần việc | Độ khó | Cần xong trước | Task đang chờ task này | Tên branch |
| --- | --- | --- | --- | --- | --- |
| 2 | C | Vừa (khoảng 2-3 ngày) | T06, quyết định D3 | Không task nào chờ; task này hoàn thành "Cấu hình toa" và "Sinh ghế tự động" của `SPEC.md` mục 37 | `feat/t08-add-coach` |

Các từ như transaction, flash message được giải thích trong `README.md` cùng thư mục. Cách
chia package theo tầng (`controller/`, `service/`, `model/`...) xem ADR 0006.

## 1. Task này làm gì

Trên trang chi tiết tàu (do T06 làm), admin chọn loại toa, nhập sức chứa và phụ thu, rồi
bấm "Thêm toa". Hệ thống tự làm phần còn lại:

- Tự đánh số toa mới.
- Tự sinh đủ ghế hoặc giường cho toa đó.
- Theo quyết định D3: tự sinh thêm chỗ cho các chuyến sắp chạy của tàu, để khách đặt được
  chỗ trên toa mới.

Admin không bao giờ phải nhập từng ghế.

## 2. Phạm vi

Làm:

- Thêm toa, kèm sinh ghế và sinh chỗ cho chuyến sắp chạy.

Không làm:

- Sửa toa, xóa toa (`docs/product/admin.md`).

## 3. Dữ liệu dùng tới

```
trains 1 ──< coaches 1 ──< seats
trips (của tàu) 1 ──< trip_seats >── 1 seats

coaches: train_id, coach_number, coach_type, capacity, price_modifier
         UNIQUE (train_id, coach_number), CHECK capacity > 0, price_modifier >= 0
seats:   coach_id, code, type, cabin_number
         UNIQUE (coach_id, code)
```

Đọc sơ đồ:

- Một tàu có nhiều toa, một toa có nhiều ghế.
- Mỗi chuyến của tàu có một dòng `trip_seats` cho mỗi ghế. Toa mới có ghế mới, nên các
  chuyến sắp chạy cũng cần thêm dòng `trip_seats` cho những ghế đó.
- Trong một tàu, số toa không trùng. Sức chứa phải lớn hơn 0, phụ thu không âm.

### Quy tắc sinh ghế

Đã chốt trong `docs/product/domain-model.md`:

| Loại toa | Sức chứa hợp lệ | Ghế sinh ra |
| --- | --- | --- |
| `SEAT` | 1 đến 80 | `A01`...`A80`, type `SEAT`, `cabinNumber` null |
| `SLEEPER` | 4 đến 40, chia hết cho 4 | `B01`...`B40`, type `BED`, cabin = (i - 1) / 4 + 1 |

Số toa mới = số toa lớn nhất hiện có của tàu + 1. Ví dụ tàu đang có 4 toa thì toa mới là
toa số 5.

Vì sao giới hạn 80 ghế và 40 giường: để mã chỗ luôn có đúng 2 chữ số. Vì sao giường phải
chia hết cho 4: mỗi cabin có đúng 4 giường.

## 4. Các bước làm

### Bước 1. Model: tạo request và class sinh mã ghế

Entity `entity/Coach`, `entity/Seat`, `entity/CoachType`, `entity/SeatType` đã có, không sửa.

Tạo class mới `model/request/CoachRequest`, chứa dữ liệu của form thêm toa:

| Field | Kiểu | Luật kiểm tra |
| --- | --- | --- |
| `coachType` | `CoachType` | `@NotNull` |
| `capacity` | `Integer` | `@NotNull`, `@Min(1)`, `@Max(80)` |
| `priceModifier` | `Long` | `@NotNull`, `@PositiveOrZero`, `@Max(10_000_000)` |

Luật phụ thuộc loại toa (giường 4 đến 40, chia hết cho 4) không viết bằng annotation được,
nên kiểm tra trong service ở Bước 3.

Tạo class mới `service/SeatCodeGenerator`. Đây là một class Java thuần, không dùng gì của
Spring. Vì sao: tách riêng phần sinh mã ghế ra một chỗ thì dễ đọc, dễ kiểm tra bằng mắt, và
`CoachService` không bị dài.

```java
record SeatSpec(String code, SeatType type, Integer cabinNumber) { }

static List<SeatSpec> generate(CoachType type, int capacity) { ... }
```

`generate` nhận loại toa và sức chứa, trả về danh sách mô tả từng ghế cần tạo (mã, loại,
số cabin) theo đúng bảng quy tắc ở mục 3.

### Bước 2. Repository: thêm câu truy vấn

Thêm vào `repository/CoachRepository`, để tìm số toa lớn nhất của tàu (tàu chưa có toa thì
trả 0):

```java
@Query("select coalesce(max(c.coachNumber), 0) from Coach c where c.train.id = :trainId")
int findMaxCoachNumber(Long trainId);
```

Thêm vào `repository/TripRepository`, để tìm các chuyến cần sinh thêm chỗ theo D3:

```java
List<Trip> findByTrainIdAndStatusAndDepartureDateGreaterThanEqual(
        Long trainId, TripStatus status, LocalDate fromDate);
```

Câu này lọc theo ngày. Sau đó lọc thêm trong Java, chỉ giữ những chuyến có `departureAt()`
sau hiện tại. Vì sao: chuyến hôm nay lúc 06:00 cùng ngày nhưng đã chạy rồi. `departureAt()`
là method T07 thêm vào `Trip`.

### Bước 3. Service: thêm toa

Tạo `service/CoachService`.

`@Transactional Coach addCoach(long trainId, CoachRequest form)` làm năm bước:

1. Tìm tàu. Không thấy thì trả 404.
2. Kiểm tra sức chứa theo loại toa. Sai thì ném `InvalidCapacityException` (đặt ở
   `common/exception/`, như mọi exception nghiệp vụ theo ADR 0006).
3. Tính số toa mới: `coachNumber = findMaxCoachNumber(trainId) + 1`.
4. Lưu `Coach`, rồi lưu các `Seat` lấy từ `SeatCodeGenerator.generate(...)`.
5. Theo D3: với mỗi chuyến `SCHEDULED` chưa khởi hành của tàu (so với
   `LocalDateTime.now()`), lưu `TripSeat` mới cho từng ghế mới.

Cả năm bước nằm trong **một transaction**. Vì sao: lỗi ở bước nào thì mọi thứ đều bị hủy,
không có toa nào mới tạo một nửa ghế, hay chuyến nào thiếu chỗ của toa mới.

Trường hợp hiếm: hai admin thêm toa cùng lúc, cả hai cùng tính ra một số toa. Khóa unique
`uk_coaches_train_number` trong database chặn người thứ hai. Controller bắt
`DataIntegrityViolationException` và báo "Vui lòng thử lại".

### Bước 4. Controller: nhận request

Tạo controller riêng `controller/CoachAdminController` với
`@RequestMapping("/admin/trains/{trainId}/coaches")`. Vì sao tách riêng: để không phải sửa
controller của T06.

| Method | Đường dẫn | Kết quả |
| --- | --- | --- |
| POST | `/admin/trains/{trainId}/coaches` | Thành công: chuyển về `/admin/trains/{trainId}` kèm "Đã thêm Toa 05 (32 chỗ)." Lỗi: chuyển về trang chi tiết kèm thông báo lỗi và dữ liệu đã nhập (flash) |

Form nằm ngay trên trang chi tiết tàu, không có trang form riêng.

### Bước 5. Giao diện

Thêm vào `templates/admin/train-detail.html` (file của T06) một card "Thêm toa", gồm:

- Ô chọn loại toa: Ghế ngồi / Giường nằm.
- Ô sức chứa, kèm ghi chú ngay bên dưới: "Giường: 4 đến 40, chia hết cho 4".
- Ô phụ thu.
- Nút "Thêm toa".

Sau khi lưu và chuyển trang, trang tải lại API của T06, nên sơ đồ tự hiện toa mới.

## 5. Luật và lỗi phải xử lý

- Toa giường có sức chứa không chia hết cho 4, hoặc ngoài khoảng 4-40: báo lỗi, không lưu
  gì.
- Toa ghế ngoài khoảng 1-80, phụ thu âm: báo lỗi.
- Tàu không tồn tại: 404.
- Hai admin thêm toa cùng lúc trùng số: báo "Vui lòng thử lại".

## 6. Kịch bản thử bằng tay

Dự án không viết test tự động (ADR 0007), nên bạn tự thử các trường hợp sau trên ứng dụng
đang chạy. Mở database bằng:

```
docker exec -it traingo-mysql mysql -utraingo -ptraingo --default-character-set=utf8mb4 traingo
```

1. **Mã ghế toa ghế.** Thêm toa ghế 16 chỗ cho một tàu. Sơ đồ hiện `A01` đến `A16`.
2. **Mã giường và cabin.** Thêm toa giường 8 chỗ cho SP1. Sơ đồ hiện `B01` đến `B08` thành
   2 cabin; kiểm tra bằng
   `SELECT code, cabin_number FROM seats WHERE coach_id = <id toa mới> ORDER BY code;`:
   `B04` thuộc cabin 1, `B05` thuộc cabin 2.
3. **Số toa tự tăng.** SP1 có sẵn 4 toa, nên toa vừa thêm ở bước 2 là Toa 05.
4. **Sức chứa sai bị từ chối.** Thêm toa giường 10 chỗ: báo lỗi, bảng `coaches` không có
   dòng mới.
5. **Sinh chỗ cho chuyến sắp chạy.** Sau bước 2, chạy
   `SELECT t.id, t.departure_date, t.status, COUNT(ts.id) FROM trips t JOIN trip_seats ts ON ts.trip_id = t.id JOIN seats s ON s.id = ts.seat_id WHERE s.coach_id = <id toa mới> GROUP BY t.id, t.departure_date, t.status;`.
   Mọi chuyến SP1 `SCHEDULED` chưa khởi hành có đúng 8 dòng; chuyến đã qua hoặc đã hủy
   không có dòng nào.

## 7. Lỗi hay gặp

- Kiểm tra "chia hết cho 4" bằng annotation trên form: annotation không biết loại toa, nên
  luật này phải nằm trong service.
- Chỉ lọc chuyến theo ngày mà quên lọc theo giờ: chuyến đã chạy sáng nay vẫn bị sinh thêm
  chỗ.
- Sinh ghế ở một transaction, sinh `TripSeat` ở transaction khác: lỗi giữa chừng để lại dữ
  liệu nửa vời.

## 8. Kiểm tra trước khi báo xong

- [ ] Thêm toa giường 8 chỗ cho SP1: sơ đồ hiện Toa 05 với 2 cabin.
- [ ] Database có `TripSeat` của toa mới cho mọi chuyến SP1 sắp chạy, không có cho chuyến
      đã qua hay đã hủy.
- [ ] Nhập toa giường 10 chỗ bị báo lỗi, không có gì được ghi.
- [ ] Đã thử hết các bước ở mục 6.
- [ ] `.\mvnw.cmd compile` chạy không lỗi và ứng dụng khởi động được
      (`.\mvnw.cmd spring-boot:run`).
- [ ] Có một dòng trong `docs/WORKLOG.md`, nằm trong cùng commit với code.
