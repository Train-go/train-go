# T06 Chi tiết tàu và sơ đồ toa

> **Tóm tắt:** sau task này, admin mở trang chi tiết một tàu và thấy từng toa cùng sơ đồ
> ghế hoặc giường, do JavaScript vẽ ra từ dữ liệu thật.

| Đợt | Phần việc | Độ khó | Cần xong trước | Task đang chờ task này | Tên branch |
| --- | --- | --- | --- | --- | --- |
| 1 | B | Vừa (khoảng 2-3 ngày) | Không | T08 (thêm toa), T09 (dùng lại hàm vẽ) | `feat/t06-train-layout` |

Các từ như DTO, `LAZY`, 401 được giải thích trong `README.md` cùng thư mục. Cách chia
package theo tầng (`controller/`, `service/`, `model/`...) xem ADR 0006.

## 1. Task này làm gì

Admin mở `/admin/trains/{id}` và thấy các toa của tàu cùng sơ đồ ghế hoặc giường. Trang
không vẽ sơ đồ bằng Thymeleaf. Thay vào đó, **JavaScript thuần** gọi một API trả JSON bằng
`fetch`, rồi tự vẽ sơ đồ trên trình duyệt.

Vì sao làm cách này ở trang admin: hàm vẽ sơ đồ viết trong task này sẽ được T09 dùng lại
cho trang khách chọn chỗ. Ở trang đó, ADR 0001 bắt buộc dùng JavaScript gọi `/api/...`.
Làm trước ở trang admin (chỉ xem, không bấm chọn) thì phần khó nhất của T09 đã có sẵn.

## 2. Phạm vi

Làm:

- Trang chi tiết tàu, chỉ để xem.
- API JSON trả sơ đồ của một tàu.
- Hàm vẽ sơ đồ dùng chung `static/js/seat-map.js`.

Không làm:

- Form thêm toa: thuộc T08.
- Trạng thái từng chỗ theo chuyến (trống, đang giữ, đã đặt): thuộc T09.

## 3. Dữ liệu dùng tới

```
trains 1 ──< coaches 1 ──< seats

coaches: id, train_id, coach_number (1, 2...), coach_type (SEAT | SLEEPER),
         capacity, price_modifier
         UNIQUE (train_id, coach_number)
seats:   id, coach_id, code (A01 | B01), type (SEAT | BED), cabin_number
         UNIQUE (coach_id, code)
```

Đọc sơ đồ: một tàu có nhiều toa, một toa có nhiều chỗ. Trong một tàu, số toa không trùng
nhau. Trong một toa, mã chỗ không trùng nhau.

Quy tắc mã chỗ (`docs/product/domain-model.md`):

- Toa ghế: `A01`, `A02`..., và `cabin_number` để trống (null).
- Toa giường: `B01`, `B02`..., mỗi cabin 4 giường, nên cabin = (số thứ tự - 1) / 4 + 1.
  Ví dụ `B01`-`B04` là cabin 1, `B05` là cabin 2.

## 4. Các bước làm

### Bước 1. Model: tạo DTO để trả ra JSON

Tạo các `record` sau trong `model/response/`. Đây là những class nhỏ chỉ để chở dữ liệu ra
JSON:

```java
record SeatView(Long id, String code, SeatType type, Integer cabinNumber) { }

record CoachView(Long id, int coachNumber, CoachType coachType, int capacity,
        long priceModifier, List<SeatView> seats) { }

record TrainLayout(Long trainId, String trainCode, String trainName,
        List<CoachView> coaches) { }
```

**Không trả thẳng entity ra JSON.** Vì sao: mọi quan hệ `@ManyToOne` trong dự án đều
`LAZY` (chưa đọc dữ liệu bảng liên quan), và `open-in-view` đã tắt. Khi Jackson (thư viện
đổi object thành JSON) chạm vào một quan hệ chưa đọc, ứng dụng sẽ lỗi
`LazyInitializationException`. Dựng DTO bên trong service thì tránh được lỗi này.

### Bước 2. Repository: lấy toa và ghế

Thêm vào `repository/CoachRepository` (file đã có, đang chứa `summarizeByTrain()`), để lấy
các toa của một tàu theo thứ tự số toa:

```java
List<Coach> findByTrainIdOrderByCoachNumberAsc(Long trainId);
```

Thêm vào `repository/SeatRepository`, để lấy **mọi** ghế của tàu trong một câu truy vấn,
đã sắp sẵn theo số toa rồi mã ghế:

```java
List<Seat> findByCoachTrainIdOrderByCoachCoachNumberAscCodeAsc(Long trainId);
```

Lấy một lần cho cả tàu, thay vì hỏi database riêng cho từng toa, để trang không chậm khi
tàu có nhiều toa.

### Bước 3. Service: dựng sơ đồ tàu

Tạo `service/TrainLayoutService` với một method:

- `@Transactional(readOnly = true) TrainLayout getLayout(long trainId)`:
  1. Tìm tàu. Không thấy thì ném `TrainNotFoundException`, người dùng nhận trang 404.
  2. Lấy toa và ghế bằng hai câu truy vấn ở Bước 2.
  3. Gom ghế theo toa bằng `seat.getCoach().getId()`.
  4. Dựng DTO **bên trong** transaction, rồi mới trả về.

`TrainNotFoundException` thuộc T03 và nằm ở `common/exception/` (ADR 0006: mọi exception
nghiệp vụ ở đó). T03 và T06 làm song song cùng lúc. Nếu T03 chưa merge, bạn tạo class này
đúng tên, đúng package, có `@ResponseStatus(NOT_FOUND)`. Khi merge, chỉ giữ lại một bản.

### Bước 4. Controller: trang chi tiết và API

| Class | Method | URL | Trả về |
| --- | --- | --- | --- |
| `controller/TrainDetailController` (`@Controller`) | GET | `/admin/trains/{id}` | View `admin/train-detail` với `train` (mã, tên) và `activeNav = "trains"` |
| `controller/TrainLayoutApiController` (`@RestController`) | GET | `/api/admin/trains/{id}/layout` | JSON `TrainLayout` |

- `TrainDetailController` trả trang HTML, chỉ có khung trang và tên tàu.
- `TrainLayoutApiController` trả JSON, để JavaScript lấy về vẽ sơ đồ.

Ví dụ JSON mà API trả về:

```json
{
  "trainId": 5, "trainCode": "SE19", "trainName": "Hà Nội - Đà Nẵng",
  "coaches": [
    { "id": 17, "coachNumber": 3, "coachType": "SLEEPER", "capacity": 16,
      "priceModifier": 200000,
      "seats": [ { "id": 449, "code": "B01", "type": "BED", "cabinNumber": 1 } ] }
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
     * seat.status (nếu có): 'AVAILABLE' | 'HELD' | 'BOOKED' | 'MINE'
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
    button.tg-seat            data-seat-id, text là mã chỗ
                              thêm tg-seat--available | --held | --booked | --mine
```

Ba quy tắc khi viết hàm:

- Tạo phần tử bằng `document.createElement`, gán chữ bằng `textContent`, không dùng
  `innerHTML`. Vì sao: `innerHTML` chèn nguyên chuỗi làm HTML, dữ liệu lạ có thể biến
  thành mã chạy được trên trang.
- Ghế không có `seat.status` thì không gắn class trạng thái. Trang admin chỉ xem cấu trúc
  toa, nên dữ liệu của nó không có `status`.
- **Không** đặt CSS trong file JS dùng chung. CSS cho trang admin đặt ở cuối `admin.css`;
  T09 tự viết CSS cho trang khách trong `app.css`.

### Bước 6. Giao diện và JavaScript của trang chi tiết

- `templates/admin/train-detail.html` (khung admin): tên tàu, nút quay lại danh sách, và
  một `<div id="tgTrainLayout" th:data-layout-url="@{/api/admin/trains/{id}/layout(id=${train.id})}">`
  để JavaScript biết gọi API nào. Trang có ba trạng thái: đang tải, tàu chưa có toa, lỗi.
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
- Tàu chưa có toa nào: trang hiện thông báo thay vì để trống.

## 6. Kịch bản thử bằng tay

Dự án không viết test tự động (ADR 0007), nên bạn tự thử các trường hợp sau trên ứng dụng
đang chạy:

1. Đăng nhập admin, mở trang chi tiết của tàu SE19. Sơ đồ hiện đủ 4 toa, đúng hàng ghế và
   cabin như mục 8.
2. Mở thẳng `/api/admin/trains/{id}/layout` trên trình duyệt (đang đăng nhập admin). Trình
   duyệt hiện JSON, trong đó `coaches[0].seats` có danh sách ghế.
3. Đổi `{id}` thành một số không có tàu, ví dụ `999999`, ở cả trang chi tiết lẫn API: cả
   hai đều trả 404.
4. So sơ đồ với database để chắc việc gom ghế theo toa đúng, ví dụ đếm số ghế của từng toa:

```
docker exec -it traingo-mysql mysql -utraingo -ptraingo --default-character-set=utf8mb4 traingo
```

   rồi chạy `SELECT coach_id, COUNT(*) FROM seats GROUP BY coach_id;` và đối chiếu với số
   ô vẽ ra trên từng toa.

## 7. Lỗi hay gặp

- Trả thẳng entity ra JSON: lỗi `LazyInitializationException` (xem Bước 1).
- Gắn class `datatable` cho phần do JavaScript vẽ: `admin.js` biến mọi bảng `.datatable`
  thành bảng có tìm kiếm ngay lúc trang vừa tải, trước khi dữ liệu kịp về, nên bảng hỏng.
  Phần do JS vẽ **không** mang class này.
- Đặt script ngoài `<main>`: script không chạy, trang đứng ở "đang tải".

## 8. Kiểm tra trước khi báo xong

- [ ] Trang `/admin/trains/{id}` của SE19 vẽ 4 toa: hai toa ghế 8 hàng 4 ghế, hai toa
      giường 4 cabin 2x2.
- [ ] Sau khi T02 merge: gọi thẳng `/api/admin/trains/{id}/layout` khi chưa đăng nhập
      nhận 401.
- [ ] Hàm vẽ nằm trong `static/js/seat-map.js`, không phụ thuộc gì của trang admin.
- [ ] Console trình duyệt không có lỗi; trang hiển thị tốt ở 375px và 1280px.
- [ ] Đã thử hết các bước ở mục 6.
- [ ] `.\mvnw.cmd compile` chạy không lỗi và ứng dụng khởi động được
      (`.\mvnw.cmd spring-boot:run`).
- [ ] Có một dòng trong `docs/WORKLOG.md`, nằm trong cùng commit với code.
