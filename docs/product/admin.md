# Khu vực quản trị

Nguồn gốc: `SPEC.md` mục 33–40. Admin chỉ quản lý dữ liệu cần cho việc đặt vé,
không quản lý vận hành đường sắt.

## Dashboard `/admin`

Bốn con số tổng quan: tổng booking, tổng chuyến, tổng người dùng, tổng doanh thu.
Không cần biểu đồ phức tạp.

Từ 23/10/2026 (#20) mẫu giao diện admin chưa có trang này, nên `/admin` chuyển
hướng sang `/admin/trains`. Task "Trang Tổng quan" (#37) dựng lại trang và bỏ
chuyển hướng.

## Ga và tuyến `/admin/stations`

Trang "Ga và tuyến": bên trái là sơ đồ đường tàu dọc Bắc–Nam, bên phải là một
ngăn đổi theo thứ admin bấm trên sơ đồ. Thao tác gọi REST `/api/admin/network`,
`/api/admin/stations`, `/api/admin/routes` và vẽ lại sơ đồ, không tải lại trang.

- **Sơ đồ:** ga đặt theo lý trình, hai ga cách nhau ít nhất 60px. Đoạn giữa hai
  ga kề có ba kiểu: hai chiều, một chiều, chưa có tuyến. Tuyến giữa hai ga không
  kề (Hà Nội ⇄ Sài Gòn) là một cung bên trái. Dấu `+` giữa hai ga để chèn ga. Ga,
  đoạn, cung và dấu `+` bấm được bằng chuột và bàn phím (`Tab`, `Enter`/`Space`),
  mỗi thứ có `aria-label` như "Huế ⇄ Đà Nẵng, 103 km, cả hai chiều".
- **Tổng quan** (mặc định): hướng dẫn và bộ chọn hai ga để mở ngăn cặp ga, kể cả
  cặp chưa có tuyến.
- **Ngăn ga:** km, tỉnh, địa chỉ; sửa ngay trong ngăn. Ga đã có tuyến thì giữ mã
  ("Ga đã có tuyến nên giữ mã.") và không xóa được ("Không xóa được: ga đang có N
  tuyến. Xóa các tuyến đó trước."). Km trùng báo tên ga đang giữ km đó.
- **Ngăn cặp ga:** hai chiều A → B, B → A với km và số chuyến sắp chạy, hoặc
  "Chưa có" kèm nút "Tạo chiều này" (km điền sẵn theo lý trình). Xóa một chiều
  bị chặn khi tuyến đã có chuyến.
- **Chèn ga:** km điền sẵn là km trống gần điểm giữa hai ga nhất; km ngoài khoảng
  hai ga thì cảnh báo nhưng vẫn cho lưu. Lưu xong ngăn chuyển sang ga mới.
- Từ 1100px sơ đồ và ngăn nằm cạnh nhau, sơ đồ dính khi cuộn; hẹp hơn thì ngăn
  xuống dưới và trang cuộn tới ngăn khi chọn.

Validation ga: `code` không trùng, chỉ gồm chữ cái không dấu và chữ số, tối đa 10
ký tự, tự viết hoa (`sgn` trùng `SGN`); `name` bắt buộc; lý trình (`kmMark`, km
tính từ ga Hà Nội) bắt buộc, nguyên từ 0 đến 5000, không trùng ga khác
(`docs/decisions/0011-chuyen-tao-tu-tau-noi-chang.md`).

Theo quyết định ngày 26/10/2026, trang **không có ma trận tuyến** (#24 không làm
ma trận); bộ chọn cặp ga ở ngăn Tổng quan thay cho ô trống của ma trận. Chèn ga
thông minh và tạo nhiều tuyến một lần thuộc #38, #39.

Làm ngày 26/10/2026 (#27): `controller/NetworkAdminApiController`,
`service/NetworkService`, `static/js/admin/line-map.js`, `stations.js`,
`station-panels.js`. Form ga cũ (`/admin/stations/new`, `/{id}/edit`) đã gỡ.

## Quản lý tuyến `/admin/routes`

Xem, tạo, sửa, xóa. Điều kiện: ga đi khác ga đến. Không quản lý ga trung gian.
Danh sách xếp theo lý trình ga đi, rồi ga đến. Form gợi ý khoảng cách bằng hiệu lý
trình của hai ga; admin vẫn sửa được.

Tuyến đã có chuyến thì không xóa được, và khi sửa chỉ được đổi khoảng cách, không
đổi ga đi hay ga đến (D2, chốt 12/10/2026). Nếu cho đổi ga, vé đã bán "Sài Gòn -
Hà Nội" sẽ bỗng thành vé của một tuyến khác. Vi phạm thì màn hình báo lý do và giữ
nguyên tuyến.

Checkpoint server #24 ngày 23/10/2026: `RouteService` đã hỗ trợ tạo một chiều hoặc
hai chiều trong cùng transaction; nếu chiều ngược đã có thì giữ nguyên chiều đó.
Ga phải tồn tại và khác nhau, khoảng cách là số nguyên dương, không trùng cặp ga
có hướng. Service chỉ cho sửa khoảng cách; chặn xóa khi có bất kỳ chuyến nào,
kể cả chuyến đã hủy hoặc hoàn thành. Khóa ga theo thứ tự ID bảo vệ tạo đồng thời;
khóa tuyến bảo vệ sửa/xóa và tranh chấp với việc thêm chuyến.

`RouteRow` tách khỏi entity, gồm hai ga, khoảng cách, số chuyến tổng, số chuyến
`SCHEDULED` có giờ đi sau hiện tại và cờ có chiều ngược. Danh sách dùng ba query
(tuyến kèm hai ga, tổng chuyến theo tuyến, chuyến sắp chạy theo tuyến), xếp theo
lý trình ga đi rồi ga đến; gợi ý khoảng cách là trị tuyệt đối hiệu lý trình.

Giao diện (26/10/2026, `RouteAdminController`): trang Thymeleaf thường,
Post/Redirect/Get. Danh sách có cột tuyến (tên và mã hai ga), khoảng
cách, chiều ngược (Đã có/Chưa có), chuyến sắp chạy, tổng chuyến. Ô tìm `?q=` lọc
theo tên hoặc mã ga, không phân biệt hoa thường và dấu ("vinh", "VIN", "hue").
Form thêm chọn ga đi/ga đến theo lý trình, tự điền khoảng cách theo lý trình khi
admin chưa gõ tay, tích "Tạo luôn chiều ngược" mặc định bật. Lỗi cùng ga và tuyến
trùng hiện ở ô ga đến. Form sửa giữ nguyên hai ga, chỉ đổi khoảng cách. Nút xóa bị
khóa kèm lý do khi tuyến đã có chuyến; gửi thẳng POST xóa vẫn bị service chặn và
báo "Không xóa được: tuyến đã có N chuyến.".

Theo quyết định ngày 25/10/2026, trang Tuyến **không làm ma trận ga × ga**, ô bật
ra và hoàn tác của issue #24. REST tạo/xóa tuyến có từ #27 (trang "Ga và tuyến").

## Quản lý tàu `/admin/trains` và `/admin/trains/:id`

Danh sách tàu (mã, tên), thêm, sửa, và trang chi tiết. Trang chi tiết có hai phần:

- **Lịch chạy:** các chuyến của tàu theo thời gian, nối chặng ga đến của chuyến
  trước với ga đi của chuyến sau. Nút thêm chuyến nằm ở đây (xem "Quản lý chuyến").
  Chỗ chuỗi bị đứt vì một chuyến đã hủy được đánh dấu.
- **Sơ đồ toa:** chỉ để xem, vẽ cấu hình toa cố định mà mọi tàu dùng chung
  (`docs/product/domain-model.md`). Admin không quản lý toa: không có route
  `/admin/coaches`, không có form thêm toa (`docs/decisions/0009-bo-bang-toa-va-ghe.md`).

Chốt ngày 01/10/2026: phiên bản đầu không có xóa tàu (`SPEC.md` mục 36 không
liệt kê). D2 ngày 12/10/2026 giữ nguyên luật không xóa tàu. **Phi đổi ngày
26/10/2026: tàu được xóa mềm** (xem "Xóa mềm tàu" bên dưới); issue #25 AC6 ghi
"không có chức năng xóa tàu" là trước quyết định này.

View "Theo ngày" (#25, 26/10/2026) là phần lịch chạy đã có:

- `/admin/trains` mở tàu đầu tiên theo mã; mỗi tàu là một tab "mã + tên", trên
  điện thoại chỉ còn mã.
- Banner ghi ga đi của chặng tiếp theo, vị trí tàu lúc này ("Đang chạy A → B",
  "Đang ở [ga]"), lịch xếp đến lúc nào và giờ sớm nhất đi lại. Giờ sớm nhất là
  giờ đến của chuyến cuối cộng 30 phút; nếu mốc đó đã qua, hoặc tàu chưa có
  chuyến, thì lấy giờ hiện tại làm tròn lên 5 phút, vì chuyến mới phải khởi hành
  trong tương lai. Tàu chưa có chuyến chưa hủy: "[mã] cần chặng đầu tiên".
- Lịch nhóm theo ngày ("Hôm nay", "Ngày mai", "Hôm qua" hoặc thứ trong tuần).
  Mỗi chuyến ghi giờ đi, giờ đến (kèm ngày nếu đến hôm sau), hai ga, giá, số vé
  đã bán trên 96 và trạng thái Sắp chạy / Đang chạy / Đã chạy / Đã hủy theo D8.
  Chuyến đã đến nơi được ẩn sẵn sau nút "Hiện N chặng đã chạy".
- Thêm và sửa tàu trong ngăn kéo, lưu qua `POST`/`PUT /api/admin/trains`. Mã gồm
  1-4 chữ cái rồi 1-4 chữ số, lưu viết hoa, không trùng; tên bắt buộc. Tàu đã có
  chuyến (kể cả chuyến đã hủy) thì không đổi được mã; service chặn cả khi gọi
  API trực tiếp (409).

### Xóa mềm tàu

Phi chốt ngày 26/10/2026, migration `V8__train_soft_delete.sql`:

- Xóa tàu chỉ bật cờ `trains.is_delete`; dòng tàu, chuyến, booking và vé giữ
  nguyên. Tàu đã xóa không còn ở các tab, `/admin/trains/{id}` của nó trả 404.
- Chỉ xóa được khi tàu **không còn chuyến sắp chạy hoặc đang chạy**: mọi chuyến
  chưa hủy đã đến nơi. Nút "Xóa tàu" trong ngăn Sửa tàu chỉ hiện với tàu xóa
  được; tàu chưa xóa được thì không có nút, cũng không có câu giải thích (Phi
  chốt 26/10/2026: luật nghiệp vụ không đưa lên giao diện).
  `DELETE /api/admin/trains/{id}` vẫn bị service chặn (409).
- Xóa xong có toast "Hoàn tác". Tàu đã xóa nằm ở trang **Lịch sử**
  (`/admin/history`, ở chân menu admin ngay trên Đăng xuất; trên điện thoại chỉ còn icon), mỗi tàu có nút "Khôi phục"
  (`POST /api/admin/trains/{id}/restore`); khôi phục xong mở trang của tàu đó.
- Mã của tàu đã xóa được tàu mới dùng lại. Database chỉ bắt mã không trùng giữa
  các tàu chưa xóa (khóa duy nhất trên cột sinh `active_code`). Khôi phục bị chặn
  (409) khi mã đã có tàu khác đang dùng; admin đổi mã tàu đó trước.
- Danh sách chuyến và booking cũ vẫn hiện mã của tàu đã xóa. Tạo chuyến (#28)
  phải nạp tàu qua `TrainService.findById`, vốn từ chối tàu đã xóa.

## Quản lý chuyến `/admin/trips`

Chuyến thuộc về tàu, nên admin thêm chuyến ở trang chi tiết tàu. `/admin/trips`
là danh sách mọi chuyến để tra cứu, lọc theo tàu và ngày, và để hủy chuyến.

Form thêm chuyến gồm: tuyến, ngày giờ khởi hành, ngày giờ đến, giá cơ bản. Tàu là
tàu của trang đang mở. Luật nối chặng (`docs/product/domain-model.md`, mục "Lịch
của tàu và luật nối chặng"):

- Tàu chưa có chuyến nào chưa hủy: chọn tuyến bất kỳ.
- Tàu đã có chuyến: ga đi bị khóa bằng ga đến của chuyến cuối, nên chỉ chọn được
  tuyến xuất phát từ ga đó. Giờ đi sớm nhất là giờ đến của chuyến cuối cộng 30
  phút, và form điền sẵn giờ này.
- Giờ đến phải sau giờ đi.
- Service kiểm tra lại mọi điều trên; vi phạm thì form báo lỗi ngay cạnh ô sai.

Khi chuyến được tạo, hệ thống sinh 96 TripSeat từ cấu hình toa cố định, tất cả ở
trạng thái `AVAILABLE`.

**Ngăn chi tiết chuyến** mở bên phải trang khi bấm một chuyến ở bất kỳ đâu trong
khu quản trị (phần tử có `data-trip-id`, hoặc `TG.openTrip(id)`): tàu và link về
trang tàu, ga đi → ga đến, trạng thái, giờ đi → giờ đến, quãng đường, giá, số chỗ
đã bán. Sơ đồ chỗ có bốn tab toa ("Toa n · Ghế ngồi/Giường nằm · đã bán/sức chứa"):
toa ghế 8 chỗ một hàng, toa giường 4 khoang x 4 giường. Chỗ đã bán bấm được và tô
dòng hành khách tương ứng; ngăn mở sẵn ở toa đầu tiên có chỗ đã bán. Bảng hành
khách chỉ tính vé của booking `CONFIRMED`: họ tên kèm 4 số cuối CCCD, toa và chỗ,
mã booking (link `/admin/bookings?open=BK…`), giá. Dữ liệu từ
`GET /api/admin/trips/{id}`. Hủy chuyến và trang `/admin/trips` làm riêng (#29).

Chuyến không xóa được, chỉ hủy được (`CANCELLED`). Chỉ hủy được chuyến
`SCHEDULED`, chưa khởi hành, và chưa có booking `CONFIRMED` nào (D2, chốt
12/10/2026): hủy chuyến khách đã trả tiền thì phải hoàn tiền, mà hoàn tiền nằm
ngoài phạm vi.

## Quản lý booking `/admin/bookings` và `/admin/bookings/:id`

Xem danh sách, tìm theo mã booking, xem chi tiết gồm khách hàng, hành khách,
chuyến, chỗ, trạng thái thanh toán. Admin hủy được booking.

Admin **không** đổi chỗ sau khi đã đặt, không sửa trạng thái thanh toán thủ công,
không hoàn tiền, không đổi hành khách.

## Danh sách người dùng `/admin/users`

Chỉ xem: tên, email, role, ngày tạo. Không có CRUD người dùng đầy đủ.

Tài khoản Admin được tạo bằng seed data, không đăng ký qua giao diện.

## Seed data

Người dùng nằm trong `app/src/main/resources/db/migration/V2__seed_demo_data.sql` và
`V3__seed_more_demo_data.sql` (1 admin, 11 khách). Từ ngày 20/10/2026, ga, tuyến,
tàu, chuyến và booking nằm trong `V5__north_south_line.sql` (V5 xóa dữ liệu cũ của
V2, V3): 8 ga trên trục Bắc - Nam, 16 tuyến, 3 tàu `SE1`, `SE2`, `SE3`, 18 chuyến
của `SE1` chạy nối chặng, 8 booking. Các màn hình admin còn lại và trang chủ đọc
thẳng từ các bảng này, không còn danh sách viết cứng trong service. Tài khoản và
cách làm mới dữ liệu: `docs/RUNBOOK.md`.

## Xóa dữ liệu đang được dùng

Chốt ngày 12/10/2026 (D2): chặn xóa, không có xóa mềm. Ga và tuyến đang được dùng
thì không xóa được; chuyến chỉ hủy, không xóa. Ngoại lệ từ 26/10/2026: tàu được
xóa mềm khi không còn chuyến sắp chạy hoặc đang chạy (mục "Xóa mềm tàu" ở trên).
Database không có `ON DELETE` nào, nên lệnh xóa hàng đang bị tham chiếu vẫn bị
database chặn; service phải kiểm tra trước để báo lý do dễ hiểu thay vì để lộ lỗi
khóa ngoại.
