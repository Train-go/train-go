# T06 Chi tiết tàu và sơ đồ toa

> **Tóm tắt:** sau task này, admin mở trang chi tiết một tàu và thấy từng toa cùng sơ đồ
> ghế hoặc giường, do JavaScript vẽ ra từ cấu hình toa cố định của hệ thống.

| Đợt | Phần việc | Độ khó | Cần xong trước | Task đang chờ task này | Tên branch |
| --- | --- | --- | --- | --- | --- |
| 1 | B | Vừa (khoảng 2-3 ngày) | Không | T07 (dùng `CoachLayout` để sinh chỗ, thêm Lịch chạy vào trang chi tiết tàu), T09 (dùng lại hàm vẽ) | `feat/t06-train-layout` |

Các từ như DTO, `LAZY`, 401 được giải thích trong `README.md` cùng thư mục. Cách chia
package theo tầng (`controller/`, `service/`, `model/`...) xem ADR 0006.

## 1. Task này làm gì

Admin mở `/admin/trains/{id}` và thấy các toa của tàu cùng sơ đồ ghế hoặc giường. Dữ liệu
sơ đồ không nằm trong database mà lấy từ **cấu hình toa cố định** viết trong code
(`model/dto/CoachLayout`): mọi tàu dùng chung một cấu hình (ADR 0009). Trang
không vẽ sơ đồ bằng Thymeleaf. Thay vào đó, **JavaScript thuần** gọi một API trả JSON bằng
`fetch`, rồi tự vẽ sơ đồ trên trình duyệt.

Vì sao làm cách này ở trang admin: hàm vẽ sơ đồ viết trong task này sẽ được T09 dùng lại
cho trang khách chọn chỗ. Ở trang đó, ADR 0001 bắt buộc dùng JavaScript gọi `/api/...`.
Làm trước ở trang admin (chỉ xem, không bấm chọn) thì phần khó nhất của T09 đã có sẵn.

Trang chi tiết tàu có hai phần (`docs/product/admin.md`): **Lịch chạy** (các chuyến của tàu,
nối chặng, có nút thêm chuyến) và **Sơ đồ toa**. Task này dựng khung trang và phần Sơ đồ toa.
Phần Lịch chạy do T07 thêm vào chính trang và controller này, vì chuyến được tạo từ trang tàu
(quyết định D9).

## 2. Phạm vi

Làm:

- Trang chi tiết tàu, chỉ để xem.
- `model/dto/CoachLayout`: cấu hình toa cố định. T07 dùng lại nó để sinh 96 chỗ cho mỗi chuyến.
- API JSON trả sơ đồ của một tàu.
- Hàm vẽ sơ đồ dùng chung `static/js/seat-map.js`.

Không làm:

- Thêm, sửa, xóa toa: không có. Admin không quản lý toa (ADR 0009).
- Trạng thái từng chỗ theo chuyến (trống, đã đặt): thuộc T09.
- Lịch chạy của tàu và nút thêm chuyến: thuộc T07.

## 3. Dữ liệu dùng tới

```
trains: id, code, name        (chỉ để kiểm tra tàu có tồn tại và lấy mã, tên)
CoachLayout (trong code, không phải bảng):
  toa 1, 2   SEAT      32 chỗ    A01..A32   phụ thu 0
  toa 3, 4   SLEEPER   16 giường B01..B16   phụ thu 200.000
```

Đọc sơ đồ: database **không có** bảng `coaches` hay `seats` (ADR 0009). Mọi tàu có đúng 4 toa
như trên, 96 chỗ, và cấu hình đó nằm trong class `CoachLayout`. Chỗ của từng chuyến mới
được ghi vào bảng `trip_seats` (T07 sinh ra, T09 đọc). Trang của task này chỉ cần bảng
`trains` để biết tàu có thật hay không.

Quy tắc mã chỗ (`docs/product/domain-model.md`):

- Toa ghế: `A01`, `A02`..., và `cabin_number` để trống (null).
- Toa giường: `B01`, `B02`..., mỗi cabin 4 giường, nên cabin = (số thứ tự - 1) / 4 + 1.
  Ví dụ `B01`-`B04` là cabin 1, `B05` là cabin 2.

## 4. Các bước làm

### Bước 1. Model: tạo DTO để trả ra JSON

Trước hết tạo `model/dto/CoachLayout`, một class `final` chỉ chứa dữ liệu tĩnh (không phải
bean Spring, constructor `private`):

```java
public final class CoachLayout {
    public record CoachSpec(int coachNumber, CoachType coachType, int capacity,
            long priceModifier) { }

    public record SeatSpec(int coachNumber, CoachType coachType, String seatCode,
            Integer cabinNumber, long priceModifier) { }

    /** Bốn toa của mọi tàu: toa 1, 2 là SEAT 32 chỗ, toa 3, 4 là SLEEPER 16 giường. */
    public static final List<CoachSpec> COACHES = List.of(
            new CoachSpec(1, CoachType.SEAT, 32, 0),
            new CoachSpec(2, CoachType.SEAT, 32, 0),
            new CoachSpec(3, CoachType.SLEEPER, 16, 200_000),
            new CoachSpec(4, CoachType.SLEEPER, 16, 200_000));

    /** Mở rộng COACHES thành 96 SeatSpec: A01..A32 (cabin null), B01..B16. */
    public static List<SeatSpec> seats() { ... }

    private CoachLayout() { }
}
```

Hàm `seats()` sinh mã `A%02d` cho toa `SEAT` với `cabinNumber = null`, và mã `B%02d` cho
toa `SLEEPER` với `cabinNumber = (n - 1) / 4 + 1`, `n` là thứ tự từ 1. T07 gọi hàm này khi
tạo chuyến, nên giữ đúng tên `CoachLayout`, `COACHES`, `seats()`, `SeatSpec`.

Rồi tạo các `record` sau trong `model/response/`. Đây là những class nhỏ chỉ để chở dữ liệu
ra JSON:

```java
record SeatView(String code, Integer cabinNumber) { }

record CoachView(int coachNumber, CoachType coachType, int capacity,
        long priceModifier, List<SeatView> seats) { }

record TrainLayout(Long trainId, String trainCode, String trainName,
        List<CoachView> coaches) { }
```

**Không trả thẳng entity ra JSON.** Vì sao: mọi quan hệ `@ManyToOne` trong dự án đều
`LAZY` (chưa đọc dữ liệu bảng liên quan), và `open-in-view` đã tắt. Khi Jackson (thư viện
đổi object thành JSON) chạm vào một quan hệ chưa đọc, ứng dụng sẽ lỗi
`LazyInitializationException`. Dựng DTO bên trong service thì tránh được lỗi này.

### Bước 2. Repository: không cần thêm

Sơ đồ lấy từ `CoachLayout`, nên task này **không** viết repository nào mới. Chỉ dùng
`TrainRepository.findById(...)` (đã có) để kiểm tra tàu tồn tại.

### Bước 3. Service: dựng sơ đồ tàu

Tạo `service/TrainLayoutService` với một method:

- `@Transactional(readOnly = true) TrainLayout getLayout(long trainId)`:
  1. Tìm tàu bằng `TrainRepository.findById`. Không thấy thì ném `TrainNotFoundException`,
     người dùng nhận trang 404.
  2. Lấy `CoachLayout.seats()` và gom theo `coachNumber`, giữ thứ tự `COACHES`.
  3. Dựng `TrainLayout` từ mã, tên tàu và các toa đó, rồi trả về.

Chỉ có bước kiểm tra tàu đọc từ database; phần còn lại là dữ liệu hằng số, nên mọi tàu cho
ra cùng một sơ đồ.

`TrainNotFoundException` thuộc T03 và nằm ở `common/exception/` (ADR 0006: mọi exception
nghiệp vụ ở đó). T03 và T06 làm song song cùng lúc. Nếu T03 chưa merge, bạn tạo class này
đúng tên, đúng package, có `@ResponseStatus(NOT_FOUND)`. Khi merge, chỉ giữ lại một bản.

### Bước 4. Controller: trang chi tiết và API

| Class | Method | URL | Trả về |
| --- | --- | --- | --- |
| `controller/TrainDetailController` (`@Controller`) | GET | `/admin/trains/{id}` | View `admin/train-detail` với `train` (mã, tên) và `activeNav = "trains"` |
| `controller/TrainLayoutApiController` (`@RestController`) | GET | `/api/admin/trains/{id}/layout` | JSON `TrainLayout` |

- `TrainDetailController` trả trang HTML, chỉ có khung trang và tên tàu. T07 sẽ thêm vào
  handler này dữ liệu Lịch chạy (`timetable`, `nextSlot`).
- `TrainLayoutApiController` trả JSON, để JavaScript lấy về vẽ sơ đồ.

Ví dụ JSON mà API trả về:

```json
{
  "trainId": 5, "trainCode": "SE1", "trainName": "Thống Nhất 1",
  "coaches": [
    { "coachNumber": 3, "coachType": "SLEEPER", "capacity": 16,
      "priceModifier": 200000,
      "seats": [ { "code": "B01", "cabinNumber": 1 } ] }
  ]
}
```

`/api/admin/**` chỉ dành cho ADMIN (T02 cài đặt). Chưa đăng nhập thì API trả 401, không
chuyển sang trang đăng nhập.

### Bước 5. JavaScript: hàm vẽ dùng chung `static/js/seat-map.js`

T06 là chủ của file này. T09 sẽ dùng lại nó cho trang khách, nên file phải độc lập, không
dính gì tới trang admin:

```javascript
window.TrainGoSeatMap = {
    /**
     * Vẽ các toa vào container. Không tự gọi fetch, không chứa CSS.
     * seat.id, seat.status (nếu có): id của dòng trip_seats (trang khách, T09);
     * status là 'AVAILABLE' | 'BOOKED'
     * options.onSeatClick(seat, button): nếu có thì ghế bấm được
     */
    render(container, coaches, options) { ... }
};
```

Hàm sinh ra các phần tử HTML như sau. Mỗi khu vực (admin, khách) tự tô màu bằng CSS của
mình:

```
div.tg-coach
  h3.tg-coach__title          "Toa 03 · Giường nằm"
  div.tg-seat-grid            toa ghế: lưới 4 cột (SPEC.md mục 14)
  div.tg-cabin                toa giường: mỗi cabin một khối 2x2 (mục 15)
    button.tg-seat            data-seat-id (nếu có seat.id), text là mã chỗ
                              thêm tg-seat--available | --booked (T09 thêm --selected)
```

Ba quy tắc khi viết hàm:

- Tạo phần tử bằng `document.createElement`, gán chữ bằng `textContent`, không dùng
  `innerHTML`. Vì sao: `innerHTML` chèn nguyên chuỗi làm HTML, dữ liệu lạ có thể biến
  thành mã chạy được trên trang.
- Ghế không có `seat.status` thì không gắn class trạng thái. Trang admin chỉ xem cấu trúc
  toa, nên dữ liệu của nó không có `id` lẫn `status`. Loại chỗ lấy từ `coachType` của toa
  (`SEAT` là ghế, `SLEEPER` là giường), không có trường loại riêng cho từng chỗ.
- **Không** đặt CSS trong file JS dùng chung. CSS cho trang admin đặt ở cuối `admin.css`;
  T09 tự viết CSS cho trang khách trong `app.css`.

### Bước 6. Giao diện và JavaScript của trang chi tiết

- `templates/admin/train-detail.html` (khung admin): tên tàu, nút quay lại danh sách, và
  một `<div id="tgTrainLayout" th:data-layout-url="@{/api/admin/trains/{id}/layout(id=${train.id})}">`
  để JavaScript biết gọi API nào. Trang có hai trạng thái ngoài sơ đồ: đang tải và lỗi. Đặt
  sơ đồ trong một card "Sơ đồ toa" riêng, để T07 thêm card "Lịch chạy" phía trên mà không phải
  dựng lại trang.
- Hai thẻ script đặt **bên trong** `<main>`. Vì sao: khung admin chỉ nhận phần `title` và
  `main` của trang, script đặt ngoài `<main>` sẽ bị bỏ.

```html
<script th:src="@{/js/seat-map.js}" defer></script>
<script th:src="@{/js/admin/train-detail.js}" defer></script>
```

`defer` giữ đúng thứ tự chạy: `seat-map.js` chạy trước, rồi mới tới file của trang.

- `static/js/admin/train-detail.js` làm bốn việc:
  1. Gọi `fetch(url)` với url lấy từ `data-layout-url`.
  2. Kiểm tra `response.ok`. Nếu nhận 401, báo "Phiên đăng nhập đã hết, tải lại trang".
  3. Đọc `response.json()`.
  4. Gọi `TrainGoSeatMap.render(...)` để vẽ.

## 5. Luật và lỗi phải xử lý

- Tàu không tồn tại: trang chi tiết và API đều trả 404.
- Gọi API khi chưa đăng nhập: trả 401, và trang báo người dùng tải lại.
- Mọi tàu đều có đủ 4 toa từ `CoachLayout`, nên không có trường hợp tàu rỗng.

## 6. Kịch bản thử bằng tay

Dự án không viết test tự động (ADR 0007), nên bạn tự thử các trường hợp sau trên ứng dụng
đang chạy:

1. Đăng nhập admin, mở trang chi tiết của tàu SE1. Sơ đồ hiện đủ 4 toa, đúng hàng ghế và
   cabin như mục 8. Mở thêm SE2 (chưa có chuyến nào): sơ đồ giống hệt, vì mọi tàu dùng chung
   một cấu hình.
2. Mở thẳng `/api/admin/trains/{id}/layout` trên trình duyệt (đang đăng nhập admin). Trình
   duyệt hiện JSON, trong đó `coaches[0].seats` có danh sách ghế.
3. Đổi `{id}` thành một số không có tàu, ví dụ `999999`, ở cả trang chi tiết lẫn API: cả
   hai đều trả 404.
4. So sơ đồ với database để chắc việc gom ghế theo toa đúng. Lấy id một chuyến của SE1 (chỉ
   SE1 có chuyến trong dữ liệu mẫu) rồi đếm số chỗ của từng toa trên chuyến:

```
docker exec -it traingo-mysql mysql -utraingo -ptraingo --default-character-set=utf8mb4 traingo
```

   rồi chạy `SELECT coach_number, coach_type, COUNT(*) FROM trip_seats WHERE trip_id = <id>
   GROUP BY coach_number, coach_type;` và đối chiếu với số ô vẽ ra trên từng toa (32, 32, 16,
   16).

## 7. Lỗi hay gặp

- Trả thẳng entity ra JSON: lỗi `LazyInitializationException` (xem Bước 1).
- Gắn class `datatable` cho phần do JavaScript vẽ: `admin.js` biến mọi bảng `.datatable`
  thành bảng có tìm kiếm ngay lúc trang vừa tải, trước khi dữ liệu kịp về, nên bảng hỏng.
  Phần do JS vẽ **không** mang class này.
- Đặt script ngoài `<main>`: script không chạy, trang đứng ở "đang tải".

## 8. Kiểm tra trước khi báo xong

- [ ] `CoachLayout.seats()` trả đúng 96 chỗ (toa 1-2: A01..A32; toa 3-4: B01..B16, cabin 1-4).
- [ ] Trang `/admin/trains/{id}` của SE1 vẽ 4 toa: hai toa ghế 8 hàng 4 ghế, hai toa
      giường 4 cabin 2x2.
- [ ] Sau khi T02 merge: gọi thẳng `/api/admin/trains/{id}/layout` khi chưa đăng nhập
      nhận 401.
- [ ] Hàm vẽ nằm trong `static/js/seat-map.js`, không phụ thuộc gì của trang admin.
- [ ] Console trình duyệt không có lỗi; trang hiển thị tốt ở 375px và 1280px.
- [ ] Đã thử hết các bước ở mục 6.
- [ ] `.\mvnw.cmd compile` chạy không lỗi và ứng dụng khởi động được
      (`.\mvnw.cmd spring-boot:run`).
- [ ] Có một dòng trong `docs/WORKLOG.md`, nằm trong cùng commit với code.
