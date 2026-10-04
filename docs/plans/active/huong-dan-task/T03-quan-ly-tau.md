# T03 Quản lý tàu

| Wave | Lane | Cỡ | Phụ thuộc | Mở khóa | Branch |
| --- | --- | --- | --- | --- | --- |
| 1 | A | S | Không | T07 (chọn tàu khi tạo chuyến) | `feat/t03-train-admin` |

## Mục tiêu

Admin xem danh sách tàu, thêm tàu, sửa tàu tại `/admin/trains`, trên database thật.
Đây là bài tập MVC thuần đầu tiên của lane A: làm giống hệt module ga, đổi đối
tượng từ ga sang tàu.

## Phạm vi

Trong phạm vi: danh sách, thêm, sửa. Mở rộng nếu còn thời gian: cột Số toa, Số chỗ.

Ngoài phạm vi: xóa tàu (`SPEC.md` mục 36 không có; D2), trang chi tiết tàu và toa
(T06), thêm toa (T08).

## Database

```
trains
  id    BIGINT PK
  code  VARCHAR(10)  NOT NULL, UNIQUE (uk_trains_code)   ví dụ SE19
  name  VARCHAR(100) NOT NULL                            ví dụ Hà Nội - Đà Nẵng

trains 1 ──< coaches (coaches.train_id)
trains 1 ──< trips   (trips.train_id)
```

Seed có 8 tàu, mỗi tàu 4 toa, 96 chỗ.

## Model

Có sẵn, không sửa field: `train/model/Train`. Thêm vào entity:

```java
public void update(String code, String name) {
    this.code = normalizeCode(code);
    this.name = name;
}

public static String normalizeCode(String code) {
    return code.trim().toUpperCase(Locale.ROOT);
}
```

và cho constructor gọi `update(...)`, giống `Station`.

Viết mới `train/model/TrainForm`:

| Field | Validation |
| --- | --- |
| `code` | `@NotBlank`, `@Size(max = 10)`, `@Pattern(regexp = "[A-Za-z0-9]+")` |
| `name` | `@NotBlank`, `@Size(max = 100)` |

Kèm `static TrainForm from(Train train)` để điền form sửa.

## Repository

Thêm vào `train/repository/TrainRepository`:

```java
List<Train> findAllByOrderByCodeAsc();
boolean existsByCode(String code);
boolean existsByCodeAndIdNot(String code, Long id);
```

Mở rộng (cột Số toa, Số chỗ), thêm vào `CoachRepository` một query gom nhóm cho
**mọi** tàu trong một lần:

```java
interface TrainCapacity {
    Long getTrainId();
    long getCoachCount();
    long getSeatCount();
}

@Query("""
    select c.train.id as trainId, count(c) as coachCount,
           sum(c.capacity) as seatCount
    from Coach c group by c.train.id""")
List<TrainCapacity> summarizeByTrain();
```

## Service

`train/service/TrainService`, cùng khuôn với `StationService`:

| Method | Transaction | Việc |
| --- | --- | --- |
| `List<Train> findAll()` | `readOnly` | Danh sách theo mã |
| `Train findById(long id)` | `readOnly` | Không thấy thì ném `TrainNotFoundException` (`@ResponseStatus(NOT_FOUND)`) |
| `Train create(TrainForm form)` | ghi | Chuẩn hóa mã, trùng thì ném `DuplicateTrainCodeException`, rồi `save` |
| `Train update(long id, TrainForm form)` | ghi | `existsByCodeAndIdNot`, `train.update(...)`, `flush()` |

## Controller

`train/controller/TrainAdminController`, `@RequestMapping("/admin/trains")`:

| Method | URL | Handler | Kết quả |
| --- | --- | --- | --- |
| GET | `/admin/trains` | `list` | View `admin/trains` |
| GET | `/admin/trains/new` | `createForm` | View `admin/train-form` |
| POST | `/admin/trains` | `create` | Redirect, hoặc form kèm lỗi |
| GET | `/admin/trains/{id}/edit` | `editForm` | Form điền sẵn, hoặc 404 |
| POST | `/admin/trains/{id}` | `update` | Redirect, hoặc form kèm lỗi |

- `@InitBinder` với `StringTrimmerEditor(true)`; `activeNav()` trả `"trains"`.
- Không khai `GET /admin/trains/{id}`: URL đó là của T06.
- Mã trùng: bắt `DuplicateTrainCodeException | DataIntegrityViolationException`, gắn
  lỗi vào ô `code`.

## View

- `templates/admin/trains.html`: bỏ `disabled`; nút "Thêm tàu" trỏ
  `/admin/trains/new`; mỗi dòng có nút xem chi tiết (`/admin/trains/{id}`, của T06)
  và nút sửa. Bảng tạm có hai cột Mã tàu, Tên tàu; thêm Số toa, Số chỗ nếu làm phần
  mở rộng.
- `templates/admin/train-form.html`: chép `station-form.html`, giữ hai ô `code`, `name`.

## Gỡ dữ liệu giả

- `AdminController`: xóa handler `trains`.
- `AdminService`: xóa `TRAINS` và `trains()`.
- Xóa `admin/model/Train.java`.

Dashboard không dùng danh sách tàu, nên gỡ không ảnh hưởng gì.

## Test

CRUD admin không bắt buộc có test (ADR 0002). Nên có một `@WebMvcTest` cho trường
hợp mã trùng hiện lại form với lỗi ở ô `code`.

## Xong khi

- [ ] Thêm tàu `se1` hiện thành `SE1`; thêm lại `SE1` bị báo trùng.
- [ ] Sửa tên tàu, thông báo "Đã cập nhật ..."; F5 không gửi lại form.
- [ ] Sidebar sáng mục "Quản lý tàu" ở cả danh sách lẫn form.
- [ ] `.\mvnw.cmd test` xanh. Một dòng `docs/WORKLOG.md`.
