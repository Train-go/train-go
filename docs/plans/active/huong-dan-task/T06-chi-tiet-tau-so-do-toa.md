# T06 Chi tiết tàu và sơ đồ toa

| Wave | Lane | Cỡ | Phụ thuộc | Mở khóa | Branch |
| --- | --- | --- | --- | --- | --- |
| 1 | B | M | Không | T08 (thêm toa), T09 (dùng lại hàm vẽ) | `feat/t06-train-layout` |

## Mục tiêu

Admin mở `/admin/trains/{id}` và thấy các toa của tàu cùng sơ đồ ghế hoặc giường.
Danh sách toa và sơ đồ được **JavaScript thuần** lấy từ một API JSON bằng `fetch`
rồi vẽ ra.

Lý do dùng `fetch` ở trang admin: hàm vẽ sơ đồ viết ở đây sẽ được T09 dùng lại cho
trang khách chọn chỗ, nơi ADR 0001 bắt buộc dùng JavaScript gọi `/api/...`. Làm ở
trang admin trước thì phần khó nhất của T09 đã có sẵn.

## Phạm vi

Trong phạm vi: trang chi tiết tàu (chỉ đọc), API JSON sơ đồ tàu, hàm vẽ dùng chung.

Ngoài phạm vi: form thêm toa (T08), trạng thái chỗ theo chuyến (T09).

## Database

```
trains 1 ──< coaches 1 ──< seats

coaches: id, train_id, coach_number (1, 2...), coach_type (SEAT | SLEEPER),
         capacity, price_modifier
         UNIQUE (train_id, coach_number)
seats:   id, coach_id, code (A01 | B01), type (SEAT | BED), cabin_number
         UNIQUE (coach_id, code)
```

Quy tắc mã chỗ: toa ghế `A01`... và `cabin_number` null; toa giường `B01`... với
cabin = (số thứ tự - 1) / 4 + 1 (`docs/product/domain-model.md`).

## Model

DTO trả ra JSON, viết bằng `record` trong `train/model/`:

```java
record SeatView(Long id, String code, SeatType type, Integer cabinNumber) { }

record CoachView(Long id, int coachNumber, CoachType coachType, int capacity,
        long priceModifier, List<SeatView> seats) { }

record TrainLayout(Long trainId, String trainCode, String trainName,
        List<CoachView> coaches) { }
```

**Không trả entity ra JSON.** Mọi `@ManyToOne` đều `LAZY` và `open-in-view` đã tắt:
Jackson chạm vào relation chưa nạp sẽ ném `LazyInitializationException`.

## Repository

`CoachRepository`:

```java
List<Coach> findByTrainIdOrderByCoachNumberAsc(Long trainId);
```

`SeatRepository`: lấy mọi ghế của tàu trong một query, đã sắp sẵn:

```java
List<Seat> findByCoachTrainIdOrderByCoachCoachNumberAscCodeAsc(Long trainId);
```

## Service

`train/service/TrainLayoutService`:

- `@Transactional(readOnly = true) TrainLayout getLayout(long trainId)`: tìm tàu (không
  thấy thì `TrainNotFoundException`, 404), lấy toa và ghế bằng hai query trên, gom ghế
  theo `seat.getCoach().getId()`, dựng DTO **bên trong** transaction.

`TrainNotFoundException` thuộc T03 (`train/service/`). T03 và T06 chạy song song: nếu T03
chưa merge, tạo class đó đúng tên, đúng package, có `@ResponseStatus(NOT_FOUND)`; khi
merge chỉ giữ một bản.

## Controller

| Class | Method | URL | Trả về |
| --- | --- | --- | --- |
| `train/controller/TrainDetailController` (`@Controller`) | GET | `/admin/trains/{id}` | View `admin/train-detail` với `train` (mã, tên) và `activeNav = "trains"` |
| `train/controller/TrainLayoutApiController` (`@RestController`) | GET | `/api/admin/trains/{id}/layout` | JSON `TrainLayout` |

Ví dụ JSON:

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

`/api/admin/**` chỉ dành cho ADMIN (T02); chưa đăng nhập thì trả 401.

## Hàm vẽ dùng chung: `static/js/seat-map.js` (chủ sở hữu: T06)

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

DOM sinh ra, để mỗi khu vực tự tô màu trong CSS của mình:

```
div.tg-coach
  h3.tg-coach__title          "Toa 03 · Giường nằm"
  div.tg-seat-grid            toa ghế: lưới 4 cột (SPEC.md mục 14)
  div.tg-cabin                toa giường: mỗi cabin một khối 2x2 (mục 15)
    button.tg-seat            data-seat-id, text là mã chỗ
                              thêm tg-seat--available | --held | --booked | --mine
```

- Tạo phần tử bằng `document.createElement`, gán chữ bằng `textContent`, không dùng
  `innerHTML`.
- Không có `seat.status` thì không gắn class trạng thái: trang admin chỉ xem cấu trúc.
- CSS cho trang admin đặt ở cuối `admin.css`; T09 tự viết CSS cho trang khách trong
  `app.css`. **Không** đặt CSS trong file JS dùng chung.

## View

- `templates/admin/train-detail.html` (khung admin): tên tàu, nút quay lại danh sách,
  một `<div id="tgTrainLayout" th:data-layout-url="@{/api/admin/trains/{id}/layout(id=${train.id})}">`.
  Ba trạng thái: đang tải, tàu chưa có toa, lỗi.
- Hai script đặt **bên trong** `<main>`, vì layout admin chỉ nhận `title` và `main`:

```html
<script th:src="@{/js/seat-map.js}" defer></script>
<script th:src="@{/js/admin/train-detail.js}" defer></script>
```

  `defer` giữ đúng thứ tự: `seat-map.js` chạy trước.
- `static/js/admin/train-detail.js`: `fetch(url)`, kiểm tra `response.ok` (401 thì báo
  "Phiên đăng nhập đã hết, tải lại trang"), `response.json()`, rồi gọi
  `TrainGoSeatMap.render(...)`. Không gắn class `datatable` cho phần do JS vẽ.

## Test

- `@WebMvcTest` cho `TrainLayoutApiController`: trả JSON có `coaches[0].seats`, và tàu
  không tồn tại trả 404.
- Unit test hàm gom ghế theo toa trong service.

## Xong khi

- [ ] `/admin/trains/{id}` của SE19 vẽ 4 toa: hai toa ghế 8 hàng 4 ghế, hai toa giường
      4 cabin 2x2.
- [ ] Gọi thẳng `/api/admin/trains/{id}/layout` khi chưa đăng nhập nhận 401 (sau khi
      T02 merge).
- [ ] Hàm vẽ nằm trong `static/js/seat-map.js`, không phụ thuộc gì của trang admin.
- [ ] Không lỗi trong console; kiểm tra ở 375px và 1280px.
- [ ] `.\mvnw.cmd test` xanh. Một dòng `docs/WORKLOG.md`.
