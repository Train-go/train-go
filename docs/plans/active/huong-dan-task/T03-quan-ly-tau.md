# T03 Quản lý tàu

> **Tóm tắt:** sau task này, admin vào trang `/admin/trains` để xem danh sách tàu, thêm
> tàu mới và sửa tàu. Dữ liệu được lưu thật trong database.

| Đợt | Phần việc | Độ khó | Cần xong trước | Task đang chờ task này | Tên branch |
| --- | --- | --- | --- | --- | --- |
| 1 | A | Dễ (khoảng 1 ngày) | Không | T07 (chọn tàu khi tạo chuyến) | `feat/t03-train-admin` |

Các từ như entity, repository, flash message được giải thích trong `README.md` cùng thư
mục. Cách chia package theo tầng (`controller/`, `service/`, `model/request/`...) nằm ở
`docs/decisions/0006-cau-truc-package-theo-tang.md`.

## 1. Task này làm gì

Hiện trang `/admin/trains` **đã hiện danh sách tàu thật**, kèm số toa và số chỗ, nhưng chỉ
để xem: code nằm chung trong `service/AdminService` (method `trains()`) và
`controller/AdminController`. Task này chuyển màn hình đó sang controller và service riêng
của tàu, rồi thêm chức năng thêm tàu và sửa tàu.

Đây là task MVC đầu tiên của phần việc A. Cách làm **giống hệt module Quản lý ga**, chỉ đổi
đối tượng từ ga sang tàu. Hãy mở `controller/StationAdminController`,
`service/StationService` và `model/request/StationRequest` bên cạnh để chép cấu trúc.

## 2. Phạm vi

Làm:

- Chuyển danh sách tàu (kèm số toa, số chỗ) sang controller và service riêng.
- Thêm tàu, sửa tàu.

Không làm:

- Xóa tàu: spec không có chức năng này (quyết định D2).
- Trang chi tiết tàu và sơ đồ toa: thuộc T06.
- Thêm toa: thuộc T08.

## 3. Dữ liệu dùng tới

```
trains
  id    BIGINT PK
  code  VARCHAR(10)  NOT NULL, UNIQUE (uk_trains_code)   ví dụ SE19
  name  VARCHAR(100) NOT NULL                            ví dụ Hà Nội - Đà Nẵng

trains 1 ──< coaches (coaches.train_id)
trains 1 ──< trips   (trips.train_id)
```

Đọc sơ đồ: mỗi tàu có một mã (`code`) không trùng và một tên. Một tàu có nhiều toa
(`coaches`) và chạy nhiều chuyến (`trips`). Ký hiệu `1 ──<` nghĩa là "một ... có nhiều ...".

Dữ liệu mẫu có sẵn 8 tàu, mỗi tàu 4 toa, tổng 96 chỗ.

## 4. Các bước làm

### Bước 1. Model: sửa entity và tạo request

Entity `entity/Train` đã có, **không thêm hay sửa field**. Chỉ thêm hai method để mã tàu
luôn được viết hoa, giống `Station`:

```java
public void update(String code, String name) {
    this.code = normalizeCode(code);
    this.name = name;
}

public static String normalizeCode(String code) {
    return code.trim().toUpperCase(Locale.ROOT);
}
```

Sửa constructor của `Train` để nó gọi `update(...)`.

Tạo class mới `model/request/TrainRequest`, chứa dữ liệu của form thêm và sửa:

| Field | Luật kiểm tra |
| --- | --- |
| `code` | `@NotBlank`, `@Size(max = 10)`, `@Pattern(regexp = "[A-Za-z0-9]+")` |
| `name` | `@NotBlank`, `@Size(max = 100)` |

Thêm method `static TrainRequest from(Train train)` để điền sẵn form khi sửa. Mỗi luật nhớ
kèm thông báo tiếng Việt, ví dụ `@NotBlank(message = "Vui lòng nhập mã tàu.")`.

Danh sách tàu cần một dòng hiển thị có cả id (để làm nút sửa, nút xem chi tiết), mã, tên,
số toa, số chỗ. Class tạm `model/response/AdminTrain` hiện chưa có id. Theo ADR 0006, các
class `Admin*` biến mất khi màn hình có controller riêng, nên hãy tạo response của task
(ví dụ `model/response/TrainRow`) có thêm `id`, rồi xóa `AdminTrain`.

### Bước 2. Repository: thêm câu truy vấn

`repository/TrainRepository` **đã có** `findAllByOrderByCodeAsc()`, và
`repository/CoachRepository` **đã có** `summarizeByTrain()` (đếm số toa, số chỗ của mọi tàu
trong một lần, trả về `model/dto/TrainCapacity`). Dùng lại cả hai cho danh sách.

Thêm vào `repository/TrainRepository` hai method kiểm tra mã trùng:

```java
List<Train> findAllByOrderByCodeAsc();
boolean existsByCode(String code);
boolean existsByCodeAndIdNot(String code, Long id);
```

(Dòng đầu đã có sẵn, chỉ thêm hai dòng sau.)

- `findAllByOrderByCodeAsc`: lấy mọi tàu, sắp theo mã.
- `existsByCode`: khi thêm, kiểm tra mã đã có chưa.
- `existsByCodeAndIdNot`: khi sửa, kiểm tra mã có thuộc **tàu khác** không (bỏ qua chính
  tàu đang sửa).

### Bước 3. Service: xử lý nghiệp vụ

Tạo `service/TrainService`, cùng khuôn với `service/StationService`:

| Method | Transaction | Việc |
| --- | --- | --- |
| `List<TrainRow> findAll()` | `readOnly` | Chuyển nguyên logic của `AdminService.trains()` sang đây, thêm `id` vào mỗi dòng |
| `Train findById(long id)` | `readOnly` | Không thấy thì ném `TrainNotFoundException` (`@ResponseStatus(NOT_FOUND)`) |
| `Train create(TrainRequest form)` | ghi | Chuẩn hóa mã, trùng thì ném `DuplicateTrainCodeException`, rồi `save` |
| `Train update(long id, TrainRequest form)` | ghi | `existsByCodeAndIdNot`, `train.update(...)`, `flush()` |

Hai class exception `TrainNotFoundException` và `DuplicateTrainCodeException` đặt trong
`common/exception/`, cạnh các exception của ga. T06 cũng dùng `TrainNotFoundException`, nên
giữ đúng tên này.

### Bước 4. Controller: nhận request

Tạo `controller/TrainAdminController` với `@RequestMapping("/admin/trains")`:

| Method | Đường dẫn | Handler | Kết quả |
| --- | --- | --- | --- |
| GET | `/admin/trains` | `list` | Trang danh sách `admin/trains` |
| GET | `/admin/trains/new` | `createForm` | Form trống `admin/train-form` |
| POST | `/admin/trains` | `create` | Lưu xong thì chuyển về danh sách; lỗi thì hiện lại form |
| GET | `/admin/trains/{id}/edit` | `editForm` | Form điền sẵn, hoặc 404 nếu không có tàu |
| POST | `/admin/trains/{id}` | `update` | Lưu xong thì chuyển về danh sách; lỗi thì hiện lại form |

Chép từ `StationAdminController` ba chi tiết:

- `@InitBinder` với `StringTrimmerEditor(true)`: tự cắt khoảng trắng hai đầu.
- Method `activeNav()` trả `"trains"` để sidebar sáng đúng mục.
- Bắt `DuplicateTrainCodeException | DataIntegrityViolationException` rồi gắn lỗi vào ô
  `code`, để người dùng thấy "Mã tàu đã tồn tại" ngay dưới ô nhập.

Không khai `GET /admin/trains/{id}`: đường dẫn đó là trang chi tiết của T06.

### Bước 5. Giao diện

- `templates/admin/trains.html`: bỏ `disabled` ở các nút. Nút "Thêm tàu" trỏ tới
  `/admin/trains/new`. Mỗi dòng có nút xem chi tiết (trỏ tới `/admin/trains/{id}` của T06)
  và nút sửa. Giữ bốn cột Mã tàu, Tên tàu, Số toa, Số chỗ như hiện tại.
- `templates/admin/train-form.html`: chép `station-form.html`, chỉ giữ hai ô `code` và
  `name`.

### Bước 6. Chuyển màn hình ra khỏi AdminController

Sau khi `TrainAdminController` chạy được:

- `controller/AdminController`: xóa handler `trains`.
- `service/AdminService`: xóa `trains()`, và bỏ `TrainRepository`, `CoachRepository` khỏi
  constructor nếu không còn chỗ nào dùng.
- Xóa `model/response/AdminTrain.java`.

Trang tổng quan (dashboard) không dùng danh sách tàu, nên xóa không ảnh hưởng gì.

## 5. Luật và lỗi phải xử lý

- Mã tàu gõ chữ thường như `se1` được lưu thành `SE1`.
- Mã đã có thì báo lỗi ngay dưới ô mã, không lưu.
- Tàu không tồn tại (người dùng tự sửa số trên đường dẫn) thì trả trang 404.

## 6. Kịch bản thử bằng tay

Chạy ứng dụng (`.\mvnw.cmd spring-boot:run`), đăng nhập admin, rồi thử lần lượt:

1. Mở `/admin/trains`: thấy 8 tàu mẫu, mỗi tàu 4 toa, 96 chỗ, như trước khi làm task.
2. Thêm tàu mã `se1`, tên bất kỳ: lưu thành `SE1`, thấy thông báo "Đã thêm ...".
3. Thêm lại mã `SE1` (hoặc `se1`): form hiện lại, lỗi "Mã tàu đã tồn tại" ngay dưới ô mã.
4. Sửa tên tàu `SE1`, giữ nguyên mã: lưu được, không bị báo trùng với chính nó.
5. Sau khi lưu, bấm F5: trình duyệt không gửi lại form.
6. Gõ `/admin/trains/999999/edit`: trang 404.

## 7. Lỗi hay gặp

- Dùng chung một method `existsByCode` cho cả thêm lẫn sửa: khi sửa tên mà giữ nguyên mã,
  hệ thống báo trùng với chính tàu đang sửa. Lúc sửa phải dùng `existsByCodeAndIdNot`.
- Quên `activeNav()`: sidebar không sáng mục "Quản lý tàu".
- Form dùng `action` thay vì `th:action`: thiếu CSRF token, server trả 403.
- Quên xóa handler `trains` trong `AdminController`: hai controller cùng nhận
  `GET /admin/trains`, ứng dụng không khởi động được.

## 8. Kiểm tra trước khi báo xong

- [ ] Thêm tàu `se1` thì hiện thành `SE1`; thêm lại `SE1` thì bị báo trùng.
- [ ] Sửa tên tàu, thấy thông báo "Đã cập nhật ..."; bấm F5 không gửi lại form.
- [ ] Danh sách vẫn hiện đúng số toa, số chỗ của mỗi tàu.
- [ ] Sidebar sáng mục "Quản lý tàu" ở cả trang danh sách lẫn trang form.
- [ ] `AdminController` không còn handler `trains`, `AdminService` không còn `trains()`.
- [ ] `.\mvnw.cmd compile` chạy không lỗi và ứng dụng khởi động được
      (`.\mvnw.cmd spring-boot:run`).
- [ ] Có một dòng trong `docs/WORKLOG.md`, nằm trong cùng commit với code.
