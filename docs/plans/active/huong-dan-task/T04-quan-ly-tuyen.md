# T04 Quản lý tuyến

| Wave | Lane | Cỡ | Phụ thuộc | Mở khóa | Branch |
| --- | --- | --- | --- | --- | --- |
| 1 | A | M | D2 | T07 (chọn tuyến khi tạo chuyến) | `feat/t04-route-admin` |

## Mục tiêu

Admin xem, thêm, sửa, xóa tuyến tại `/admin/routes`. Đây là màn hình đầu tiên có
**relation trên giao diện**: form chọn hai ga từ bảng `stations`, danh sách hiện
tên ga qua khóa ngoại.

## Phạm vi

Trong phạm vi: danh sách, thêm, sửa, xóa có kiểm tra. Ngoài phạm vi: ga trung gian
(`SPEC.md` mục 48).

## Database

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

Database đã tự chặn tuyến trùng ga, tuyến trùng cặp và xóa tuyến đang có chuyến.
Task này kiểm tra trước trong service để báo lỗi bằng lời.

## Model

Có sẵn: `route/model/Route` (`originStation`, `destinationStation` đều
`@ManyToOne(fetch = LAZY)`, `distanceKm`). Thêm vào entity:

```java
public void update(Station origin, Station destination, int distanceKm) { ... }

/** "SGN-NTR", the code shown in the route list. */
public String getCode() {
    return originStation.getCode() + "-" + destinationStation.getCode();
}
```

Viết mới `route/model/RouteForm`:

| Field | Kiểu | Validation |
| --- | --- | --- |
| `originStationId` | `Long` | `@NotNull(message = "Vui lòng chọn ga đi.")` |
| `destinationStationId` | `Long` | `@NotNull` |
| `distanceKm` | `Integer` | `@NotNull`, `@Positive`, `@Max(5000)` |

## Repository

Thêm vào `route/repository/RouteRepository`:

```java
@EntityGraph(attributePaths = {"originStation", "destinationStation"})
List<Route> findAllByOrderByIdAsc();

@EntityGraph(attributePaths = {"originStation", "destinationStation"})
Optional<Route> findWithStationsById(Long id);

boolean existsByOriginStationIdAndDestinationStationId(Long originId, Long destinationId);
boolean existsByOriginStationIdAndDestinationStationIdAndIdNot(
        Long originId, Long destinationId, Long id);
```

Thêm vào `trip/repository/TripRepository`:

```java
boolean existsByRouteId(Long routeId);
```

`@EntityGraph` nạp luôn hai ga trong cùng câu `SELECT`. Thiếu nó thì template đọc
`route.originStation.name` sẽ ném `LazyInitializationException`, vì dự án tắt
`open-in-view` (ADR 0005).

## Service

`route/service/RouteService`:

| Method | Việc |
| --- | --- |
| `List<Route> findAll()` | `findAllByOrderByIdAsc()` |
| `Route findById(long id)` | `findWithStationsById`, không thấy thì `RouteNotFoundException` (404) |
| `Route create(RouteForm form)` | Ga đi trùng ga đến: ném `SameStationException`. Cặp đã có: `DuplicateRouteException`. Nạp hai `Station` bằng `StationRepository.findById`, ga không tồn tại cũng là lỗi form. Rồi `save` |
| `Route update(long id, RouteForm form)` | Như trên, dùng `...AndIdNot`. Theo D2: tuyến đã có chuyến thì chỉ được đổi `distanceKm`, đổi ga thì ném `RouteInUseException` |
| `Route delete(long id)` | Có chuyến dùng (`existsByRouteId`) thì ném `RouteInUseException`; không thì xóa |

Như `StationService.delete`: kiểm tra trước rồi mới xóa, không bắt lỗi khóa ngoại
bên trong transaction.

## Controller

`route/controller/RouteAdminController`, `@RequestMapping("/admin/routes")`, giống
`StationAdminController`: `list`, `createForm`, `create`, `editForm`, `update`,
`delete`. Thêm một method `@ModelAttribute("stations")` trả
`stationService.findAll()`, để cả form thêm lẫn sửa có danh sách ga cho hai ô chọn.

| Method | URL | Kết quả |
| --- | --- | --- |
| GET | `/admin/routes` | Danh sách |
| GET | `/admin/routes/new` | Form trống |
| POST | `/admin/routes` | Redirect, hoặc form kèm lỗi |
| GET | `/admin/routes/{id}/edit` | Form điền sẵn |
| POST | `/admin/routes/{id}` | Redirect, hoặc form kèm lỗi |
| POST | `/admin/routes/{id}/delete` | Redirect kèm thông báo xanh hoặc đỏ |

Gắn lỗi vào đúng ô: ga trùng gắn vào `destinationStationId`, cặp trùng gắn vào
`originStationId`.

## View

- `templates/admin/routes.html`: cột Mã tuyến (`route.code`), Ga đi, Ga đến, Khoảng
  cách (`#numbers.formatInteger(route.distanceKm, 0, 'POINT')` + " km"), Thao tác.
  Nút xóa dùng form `POST` có `th:data-confirm`, như danh sách ga.
- `templates/admin/route-form.html`: hai ô chọn ga:

```html
<select class="form-select" th:field="*{originStationId}"
        th:errorclass="is-invalid">
    <option value="">Chọn ga đi</option>
    <option th:each="s : ${stations}" th:value="${s.id}"
            th:text="|${s.name} (${s.code})|">Ga Sài Gòn (SGN)</option>
</select>
```

`th:field` tự đánh dấu `selected` cho ga đang chọn, cả khi sửa lẫn khi form hiện lại
vì lỗi.

## Gỡ dữ liệu giả

`AdminController` xóa handler `routes`; `AdminService` xóa `ROUTES`, `routes()`;
xóa `admin/model/Route.java`.

## Test

Không bắt buộc (CRUD admin). Nên có unit test `RouteServiceTest` cho hai luật: ga đi
trùng ga đến bị từ chối, xóa tuyến có chuyến bị từ chối.

## Xong khi

- [ ] Thêm tuyến `HUE -> DNG` 103 km thành công; thêm lại bị báo trùng; chọn cùng
      một ga hai đầu bị báo lỗi.
- [ ] Xóa `SGN -> NTR` (đang có chuyến) bị từ chối kèm lý do; xóa tuyến vừa thêm
      thành công.
- [ ] Xóa ga Huế ở `/admin/stations` giờ bị từ chối, vì Huế đã có tuyến.
- [ ] `.\mvnw.cmd test` xanh. Một dòng `docs/WORKLOG.md`.
