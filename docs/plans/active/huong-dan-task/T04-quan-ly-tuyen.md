# T04 Quản lý tuyến

> **Tóm tắt:** sau task này, admin vào trang `/admin/routes` để xem, thêm, sửa và xóa
> tuyến tàu (ví dụ Sài Gòn → Nha Trang), chọn ga từ danh sách ga có sẵn.

| Đợt | Phần việc | Độ khó | Cần xong trước | Task đang chờ task này | Tên branch |
| --- | --- | --- | --- | --- | --- |
| 1 | A | Vừa (khoảng 2-3 ngày) | Quyết định D2 | T07 (chọn tuyến khi tạo chuyến) | `feat/t04-route-admin` |

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

## 2. Phạm vi

Làm:

- Chuyển danh sách tuyến sang controller và service riêng.
- Thêm, sửa tuyến.
- Xóa tuyến, có kiểm tra tuyến đang được dùng hay không.

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
```

Đọc sơ đồ: mỗi tuyến trỏ tới **hai** dòng trong bảng `stations`: ga đi
(`origin_station_id`) và ga đến (`destination_station_id`). `FK` là khóa ngoại, tức cột
chứa id của một dòng ở bảng khác. Mỗi tuyến có khoảng cách tính bằng km. Một tuyến có thể
có nhiều chuyến (`trips`) chạy trên nó.

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
@Query("select r from Route r join fetch r.originStation join fetch r.destinationStation order by r.id")
List<Route> findAllWithStations();
```

`join fetch` bảo Hibernate đọc luôn hai ga trong cùng câu `SELECT` với tuyến, giống việc
`@EntityGraph` làm. Vì sao cần: thiếu nó, template đọc `route.originStation.name` sẽ ném
`LazyInitializationException`, vì dự án tắt `open-in-view` (ADR 0005).

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
chọn.

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
            th:text="|${s.name} (${s.code})|">Ga Sài Gòn (SGN)</option>
</select>
```

`th:field` tự chọn sẵn ga đang có, cả khi mở form sửa lẫn khi form hiện lại vì lỗi. Bạn
không phải tự viết điều kiện `selected`.

### Bước 6. Chuyển màn hình ra khỏi AdminController

Sau khi `RouteAdminController` chạy được:

- `controller/AdminController`: xóa handler `routes`.
- `service/AdminService`: xóa `routes()`. Dashboard không dùng method này, nên xóa không
  ảnh hưởng gì.
- Xóa `model/response/AdminRoute.java`. Theo ADR 0006, các class `Admin*` chỉ là tạm và
  biến mất khi màn hình có controller riêng.

## 5. Luật và lỗi phải xử lý

- Ga đi trùng ga đến: báo lỗi dưới ô ga đến, không lưu.
- Cặp ga đã có tuyến: báo lỗi dưới ô ga đi, không lưu. Chiều ngược lại (ví dụ NTR → SGN)
  là một tuyến khác, được phép.
- Tuyến đã có chuyến: chỉ sửa được khoảng cách, không đổi ga, không xóa (quyết định D2).
  Khi bị từ chối, thông báo phải nói rõ lý do.
- Tuyến không tồn tại (người dùng tự sửa số trên đường dẫn): trang 404.

## 6. Kịch bản thử bằng tay

Chạy ứng dụng (`.\mvnw.cmd spring-boot:run`), đăng nhập admin, rồi thử lần lượt:

1. Thêm tuyến `HUE -> DNG` 103 km: thành công, tuyến mới có trong danh sách.
2. Thêm lại đúng tuyến đó: bị báo trùng dưới ô ga đi, không có dòng mới.
3. Chọn cùng một ga cho cả hai ô: bị báo lỗi dưới ô ga đến.
4. Thêm `DNG -> HUE` (chiều ngược lại): thành công.
5. Sửa `SGN -> NTR`, đổi khoảng cách: lưu được. Đổi ga đến: bị từ chối, vì tuyến đã có
   chuyến.
6. Xóa `SGN -> NTR`: bị từ chối kèm lý do. Xóa tuyến vừa thêm ở bước 1: thành công.
7. Gõ `/admin/routes/999999/edit`: trang 404.
8. Kiểm tra trong database không có tuyến trùng:

```sql
SELECT o.code, d.code, r.distance_km FROM routes r
JOIN stations o ON o.id = r.origin_station_id
JOIN stations d ON d.id = r.destination_station_id ORDER BY r.id;
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

## 8. Kiểm tra trước khi báo xong

- [ ] Thêm tuyến `HUE -> DNG` 103 km thành công; thêm lại thì bị báo trùng; chọn cùng một
      ga cho hai đầu thì bị báo lỗi.
- [ ] Xóa `SGN -> NTR` (đang có chuyến) bị từ chối kèm lý do; xóa tuyến vừa thêm thì thành
      công.
- [ ] Xóa ga Huế ở `/admin/stations` giờ bị từ chối, vì Huế đã có tuyến.
- [ ] `AdminController` không còn handler `routes`, `AdminService` không còn `routes()`.
- [ ] `.\mvnw.cmd compile` chạy không lỗi và ứng dụng khởi động được
      (`.\mvnw.cmd spring-boot:run`).
- [ ] Có một dòng trong `docs/WORKLOG.md`, nằm trong cùng commit với code.
