# T04 Quản lý tuyến

> **Tóm tắt:** sau task này, admin vào trang `/admin/routes` để xem, thêm, sửa và xóa
> tuyến tàu (ví dụ Sài Gòn → Nha Trang), chọn ga từ danh sách ga có sẵn.

| Đợt | Phần việc | Độ khó | Cần xong trước | Task đang chờ task này | Tên branch |
| --- | --- | --- | --- | --- | --- |
| 1 | A | Vừa (khoảng 2-3 ngày) | Quyết định D2, D9 | T07 (chọn tuyến khi tạo chuyến) | `feat/t04-route-admin` |

Các từ như `LAZY`, `@EntityGraph`, transaction được giải thích trong `README.md` cùng thư
mục. Cách chia package theo tầng (`controller/`, `service/`, `model/request/`...) nằm ở
`docs/decisions/0006-cau-truc-package-theo-tang.md`.

## 1. Task này làm gì

Hiện trang `/admin/routes` **đã hiện danh sách tuyến thật** từ database, nhưng chỉ để xem:
code nằm chung trong `service/AdminService` (method `routes()`) và
`controller/AdminController`. Task này chuyển màn hình đó sang controller và service riêng
của tuyến, rồi thêm chức năng thêm, sửa, xóa tuyến.

Đây là màn hình đầu tiên có **quan hệ giữa hai bảng** hiện ra trên form: admin chọn hai ga
từ bảng `stations`, và danh sách tuyến hiện tên ga bằng cách đi theo khóa ngoại sang bảng
`stations`. Cách làm vẫn theo mẫu module Quản lý ga; phần mới là cách đọc dữ liệu của bảng
liên quan.

Ga và tuyến là **dữ liệu nền** của trục Bắc - Nam (quyết định D9,
`docs/decisions/0011-chuyen-tao-tu-tau-noi-chang.md`). Mỗi ga có lý trình (`kmMark`, số km
tính từ ga Hà Nội), nên task này có hai điểm dựa vào lý trình: danh sách tuyến xếp theo lý
trình ga đi rồi ga đến, tức từ Bắc vào Nam; và form gợi ý khoảng cách bằng hiệu lý trình của
hai ga, admin vẫn sửa được.

## 2. Phạm vi

Làm:

- Chuyển danh sách tuyến sang controller và service riêng.
- Thêm, sửa tuyến.
- Xóa tuyến, có kiểm tra tuyến đang được dùng hay không.
- Gợi ý khoảng cách từ lý trình hai ga.

Không làm: ga trung gian (`SPEC.md` mục 48). Một tuyến chỉ có ga đi và ga đến.

## 3. Dữ liệu dùng tới

```
stations 1 ──< routes >── 1 stations
           origin_station_id   destination_station_id

routes
  id                      BIGINT PK
  origin_station_id       BIGINT NOT NULL  FK -> stations.id
  destination_station_id  BIGINT NOT NULL  FK -> stations.id
  distance_km             INT    NOT NULL  CHECK > 0
  UNIQUE (origin_station_id, destination_station_id)
  CHECK (origin_station_id <> destination_station_id)

routes 1 ──< trips (trips.route_id)

stations
  km_mark                 INT    NOT NULL  UNIQUE, CHECK >= 0   ví dụ Huế 688
```

Đọc sơ đồ: mỗi tuyến trỏ tới **hai** dòng trong bảng `stations`: ga đi
(`origin_station_id`) và ga đến (`destination_station_id`). `FK` là khóa ngoại, tức cột
chứa id của một dòng ở bảng khác. Mỗi tuyến có khoảng cách tính bằng km. Một tuyến có thể
có nhiều chuyến (`trips`) chạy trên nó.

Cột `stations.km_mark` (Java `int kmMark`) là lý trình của ga. Hai ga trên cùng trục thì
khoảng cách giữa chúng xấp xỉ hiệu hai lý trình, ví dụ Huế (688) đến Đà Nẵng (791) là 103 km.
Dữ liệu mẫu có 8 ga từ Hà Nội (0) tới Sài Gòn (1726) và 16 tuyến: 7 chặng giữa hai ga kề
nhau, mỗi chặng hai chiều, cộng Hà Nội ↔ Sài Gòn. Khoảng cách trong seed đúng bằng hiệu lý
trình.

Database đã tự chặn ba trường hợp: tuyến có ga đi trùng ga đến, hai tuyến trùng cặp ga,
và xóa tuyến đang có chuyến. Dù vậy, task này vẫn kiểm tra trước trong service. Vì sao:
lỗi từ database là thông báo kỹ thuật bằng tiếng Anh, còn kiểm tra trước thì hiện được
lời giải thích dễ hiểu cho admin.

## 4. Các bước làm

### Bước 1. Model: thêm method cho entity và tạo request

Entity `entity/Route` đã có hai field `originStation`, `destinationStation` (đều là
`@ManyToOne(fetch = LAZY)`) và `distanceKm`. Không sửa field, chỉ thêm hai method:

```java
public void update(Station origin, Station destination, int distanceKm) { ... }

/** "SGN-NTR", the code shown in the route list. */
public String getCode() {
    return originStation.getCode() + "-" + destinationStation.getCode();
}
```

`getCode()` tạo mã tuyến như `SGN-NTR` từ mã của hai ga, để hiện ở cột Mã tuyến.

Tạo class mới `model/request/RouteRequest` (dữ liệu form gửi lên):

| Field | Kiểu | Luật kiểm tra |
| --- | --- | --- |
| `originStationId` | `Long` | `@NotNull(message = "Vui lòng chọn ga đi.")` |
| `destinationStationId` | `Long` | `@NotNull` |
| `distanceKm` | `Integer` | `@NotNull`, `@Positive`, `@Max(5000)` |

Request chỉ chứa **id** của ga mà người dùng chọn, không chứa cả object `Station`.

### Bước 2. Repository: đọc tuyến kèm hai ga

`repository/RouteRepository` **đã có** câu truy vấn cho danh sách, dùng lại, không viết
thêm:

```java
@Query("""
        select r from Route r join fetch r.originStation o join fetch r.destinationStation d
        order by o.kmMark, d.kmMark""")
List<Route> findAllWithStations();
```

`join fetch` bảo Hibernate đọc luôn hai ga trong cùng câu `SELECT` với tuyến, giống việc
`@EntityGraph` làm. Vì sao cần: thiếu nó, template đọc `route.originStation.name` sẽ ném
`LazyInitializationException`, vì dự án tắt `open-in-view` (ADR 0005).

`order by o.kmMark, d.kmMark` xếp tuyến theo lý trình ga đi, rồi ga đến: mọi tuyến đi từ Hà
Nội đứng đầu, trong đó Hà Nội → Thanh Hóa trước Hà Nội → Sài Gòn. Không sắp lại trong Java
hay trong template.

Thêm vào `repository/RouteRepository`:

```java
@EntityGraph(attributePaths = {"originStation", "destinationStation"})
Optional<Route> findWithStationsById(Long id);

boolean existsByOriginStationIdAndDestinationStationId(Long originId, Long destinationId);
boolean existsByOriginStationIdAndDestinationStationIdAndIdNot(
        Long originId, Long destinationId, Long id);
```

Thêm vào `repository/TripRepository`, để biết tuyến có chuyến nào dùng không:

```java
boolean existsByRouteId(Long routeId);
```

### Bước 3. Service: luật của tuyến

Tạo `service/RouteService`. Các exception mới (`RouteNotFoundException`,
`SameStationException`, `DuplicateRouteException`, `RouteInUseException`) đặt trong
`common/exception/`, cạnh các exception của ga.

| Method | Việc |
| --- | --- |
| `List<Route> findAll()` | Gọi `findAllWithStations()` |
| `Route findById(long id)` | Gọi `findWithStationsById`; không thấy thì ném `RouteNotFoundException` (404) |
| `Route create(RouteRequest form)` | Ga đi trùng ga đến: ném `SameStationException`. Cặp ga đã có tuyến: ném `DuplicateRouteException`. Nạp hai `Station` bằng `StationRepository.findById`; ga không tồn tại cũng là lỗi form. Rồi `save` |
| `Route update(long id, RouteRequest form)` | Như khi thêm, nhưng dùng method `...AndIdNot`. Theo quyết định D2: tuyến đã có chuyến thì chỉ được đổi `distanceKm`; đổi ga thì ném `RouteInUseException` |
| `Route delete(long id)` | Có chuyến dùng tuyến (`existsByRouteId`) thì ném `RouteInUseException`; không có thì xóa |

Làm giống `StationService.delete`: kiểm tra trước rồi mới xóa. Không để database báo lỗi
khóa ngoại rồi bắt lỗi đó bên trong transaction, vì transaction sẽ bị đánh dấu hỏng và
lúc kết thúc vẫn báo lỗi.

### Bước 4. Controller: nhận request

Tạo `controller/RouteAdminController` với `@RequestMapping("/admin/routes")`. Làm giống
`controller/StationAdminController`, gồm các handler `list`, `createForm`, `create`,
`editForm`, `update`, `delete`.

Thêm một method `@ModelAttribute("stations")` trả `stationService.findAll()`. Method này
chạy trước mọi handler, nên cả form thêm lẫn form sửa đều có sẵn danh sách ga cho hai ô
chọn. `stationService.findAll()` đọc `StationRepository.findAllByOrderByKmMarkAsc()`, nên
hai ô chọn ga cũng xếp từ Bắc vào Nam.

| Method | Đường dẫn | Kết quả |
| --- | --- | --- |
| GET | `/admin/routes` | Danh sách |
| GET | `/admin/routes/new` | Form trống |
| POST | `/admin/routes` | Lưu xong thì chuyển về danh sách; lỗi thì hiện lại form |
| GET | `/admin/routes/{id}/edit` | Form điền sẵn |
| POST | `/admin/routes/{id}` | Lưu xong thì chuyển về danh sách; lỗi thì hiện lại form |
| POST | `/admin/routes/{id}/delete` | Chuyển về danh sách kèm thông báo xanh (đã xóa) hoặc đỏ (không xóa được) |

Gắn lỗi vào đúng ô để người dùng biết sửa chỗ nào: lỗi ga trùng gắn vào ô
`destinationStationId`, lỗi cặp ga đã có tuyến gắn vào ô `originStationId`.

### Bước 5. Giao diện

- `templates/admin/routes.html`: đang đọc các dòng `AdminRoute`. Cho nó đọc danh sách
  `Route` mới: các cột Mã tuyến (`route.code`), Ga đi, Ga đến, Khoảng cách
  (`#numbers.formatInteger(route.distanceKm, 0, 'POINT')` + " km"), Thao tác. Bỏ
  `disabled` ở các nút. Nút xóa là form `POST` có `th:data-confirm`, giống danh sách ga.
- `templates/admin/route-form.html`: hai ô chọn ga, viết như sau:

```html
<select class="form-select" th:field="*{originStationId}"
        th:errorclass="is-invalid">
    <option value="">Chọn ga đi</option>
    <option th:each="s : ${stations}" th:value="${s.id}"
            th:data-km-mark="${s.kmMark}"
            th:text="|${s.name} (${s.code}) · km ${s.kmMark}|">Ga Sài Gòn (SGN) · km 1726</option>
</select>
```

`th:field` tự chọn sẵn ga đang có, cả khi mở form sửa lẫn khi form hiện lại vì lỗi. Bạn
không phải tự viết điều kiện `selected`. `data-km-mark` mang lý trình của ga cho JavaScript
gợi ý khoảng cách.

- `static/js/admin/route-form.js`: mỗi khi admin đổi một trong hai ô ga và cả hai ô đã có ga,
  điền ô `distanceKm` bằng `Math.abs(kmĐến - kmĐi)`. Chỉ điền khi ô ga **thay đổi**, không
  điền lúc trang vừa mở, để form sửa không ghi đè khoảng cách đã lưu. Admin gõ số khác vào ô
  thì số đó được giữ, cho tới khi admin đổi ga lần nữa. Chọn cùng một ga cho hai ô thì không
  gợi ý gì, để service báo lỗi. Thẻ script đặt **bên trong** `<main>`, vì khung admin chỉ nhận
  phần `title` và `main` của trang (giống T06).

Gợi ý chỉ là tiện ích trên trình duyệt. Server vẫn nhận và kiểm tra `distanceKm` như mọi ô
khác, không tự tính lại.

### Bước 6. Chuyển màn hình ra khỏi AdminController

Sau khi `RouteAdminController` chạy được:

- `controller/AdminController`: xóa handler `routes`.
- `service/AdminService`: xóa `routes()`. Dashboard không dùng method này, nên xóa không
  ảnh hưởng gì.
- Xóa `model/response/AdminRoute.java`. Theo ADR 0006, các class `Admin*` chỉ là tạm và
  biến mất khi màn hình có controller riêng.

## 5. Luật và lỗi phải xử lý

- Ga đi trùng ga đến: báo lỗi dưới ô ga đến, không lưu.
- Cặp ga đã có tuyến: báo lỗi dưới ô ga đi, không lưu. Chiều ngược lại (ví dụ HUE → HNI so
  với HNI → HUE) là một tuyến khác, được phép.
- Danh sách tuyến xếp theo lý trình ga đi, rồi ga đến.
- Khoảng cách được gợi ý bằng hiệu lý trình hai ga, nhưng admin sửa được, và số admin nhập là
  số được lưu.
- Tuyến đã có chuyến: chỉ sửa được khoảng cách, không đổi ga, không xóa (quyết định D2).
  Khi bị từ chối, thông báo phải nói rõ lý do.
- Tuyến không tồn tại (người dùng tự sửa số trên đường dẫn): trang 404.

## 6. Kịch bản thử bằng tay

Chạy ứng dụng (`.\mvnw.cmd spring-boot:run`), đăng nhập admin, rồi thử lần lượt:

1. Mở `/admin/routes`: thấy 16 tuyến mẫu xếp theo lý trình ga đi rồi ga đến: `HNI-THA`,
   `HNI-SGN`, `THA-HNI`, `THA-VIN`, `VIN-THA`, `VIN-DHO`... cuối cùng là `SGN-HNI`, `SGN-NTR`.
2. Mở form thêm, chọn ga đi Hà Nội, ga đến Huế: ô khoảng cách tự điền 688. Lưu: tuyến
   `HNI-HUE` hiện giữa `HNI-THA` và `HNI-SGN`, đúng thứ tự lý trình.
3. Thêm lại đúng tuyến đó: bị báo trùng dưới ô ga đi, không có dòng mới.
4. Chọn cùng một ga cho cả hai ô: không có gợi ý, gửi form thì bị báo lỗi dưới ô ga đến.
5. Thêm `HUE -> HNI` (chiều ngược lại): ô khoảng cách cũng gợi ý 688, sửa tay thành 700 rồi
   lưu: thành công, danh sách ghi 700 km (số admin nhập, không phải số gợi ý).
6. Mở form sửa `HUE -> HNI`: ô khoảng cách vẫn là 700, không bị gợi ý ghi đè lúc trang mở.
7. Sửa `DNG -> NTR`, đổi khoảng cách: lưu được. Đổi ga đến: bị từ chối, vì tuyến đã có
   chuyến của SE1.
8. Xóa `DNG -> NTR`: bị từ chối kèm lý do. Xóa hai tuyến vừa thêm ở bước 2 và 5: thành công.
9. Ga dùng trong tuyến thì không xóa được. Ở `/admin/stations`, thêm ga Quảng Ngãi, mã `QNG`,
   lý trình 928. Thêm tuyến `DNG -> QNG`: gợi ý 137 km. Quay lại xóa ga Quảng Ngãi: bị từ chối
   vì ga đã có tuyến. Xóa tuyến `DNG -> QNG` rồi xóa ga: thành công.
10. Gõ `/admin/routes/999999/edit`: trang 404.
11. Kiểm tra trong database không có tuyến trùng:

```sql
SELECT o.code, d.code, r.distance_km FROM routes r
JOIN stations o ON o.id = r.origin_station_id
JOIN stations d ON d.id = r.destination_station_id ORDER BY o.km_mark, d.km_mark;
```

Mở MySQL bằng `docker exec -it traingo-mysql mysql -utraingo -ptraingo --default-character-set=utf8mb4 traingo`.

## 7. Lỗi hay gặp

- Đọc tuyến bằng `findAll()` thường thay vì `findAllWithStations()`: trang danh sách lỗi
  `LazyInitializationException` khi đọc tên ga.
- Dùng `existsByOriginStationIdAndDestinationStationId` khi sửa: sửa khoảng cách mà giữ
  nguyên hai ga thì bị báo trùng với chính tuyến đang sửa. Lúc sửa phải dùng method có
  `AndIdNot`.
- Bắt lỗi khóa ngoại của database bên trong method `@Transactional` thay vì kiểm tra trước:
  lúc kết thúc transaction vẫn ném lỗi.
- Quên xóa handler `routes` trong `AdminController`: hai controller cùng nhận
  `GET /admin/routes`, ứng dụng không khởi động được.
- Gợi ý khoảng cách ngay lúc trang vừa tải: mở form sửa là khoảng cách đã lưu bị thay bằng
  số gợi ý, admin bấm Lưu mà không để ý.
- Sắp tuyến theo tên ga hay theo id: danh sách không còn đọc từ Bắc vào Nam. Dùng thứ tự của
  `findAllWithStations()`.

## 8. Kiểm tra trước khi báo xong

- [ ] Danh sách 16 tuyến mẫu xếp theo lý trình ga đi rồi ga đến; tuyến mới thêm nằm đúng chỗ.
- [ ] Thêm tuyến `HNI -> HUE` với gợi ý 688 km thành công; thêm lại thì bị báo trùng; chọn
      cùng một ga cho hai đầu thì bị báo lỗi.
- [ ] Sửa tay số gợi ý thì số đã sửa được lưu; mở form sửa không làm đổi khoảng cách.
- [ ] Xóa `DNG -> NTR` (đang có chuyến) bị từ chối kèm lý do; xóa tuyến vừa thêm thì thành
      công.
- [ ] Ga Quảng Ngãi có tuyến thì không xóa được; xóa tuyến rồi thì xóa được.
- [ ] `AdminController` không còn handler `routes`, `AdminService` không còn `routes()`.
- [ ] `.\mvnw.cmd compile` chạy không lỗi và ứng dụng khởi động được
      (`.\mvnw.cmd spring-boot:run`).
- [ ] Có một dòng trong `docs/WORKLOG.md`, nằm trong cùng commit với code.
