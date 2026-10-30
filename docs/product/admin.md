# Khu vực quản trị

Nguồn gốc: `SPEC.md` mục 33–40. Admin chỉ quản lý dữ liệu cần cho việc đặt vé,
không quản lý vận hành đường sắt.

## Dashboard `/admin`

Trang Tổng quan (#37) là trang đầu của khu quản trị và là nơi admin vào sau khi đăng
nhập. Bốn ô số đọc thật từ database, không có biểu đồ:

- **Booking:** số booking `CONFIRMED`, ghi chú số booking đã hủy;
- **Chuyến:** số chuyến chưa hủy, ghi chú số chuyến sắp chạy (`SCHEDULED`, chưa khởi hành);
- **Người dùng:** số tài khoản `CUSTOMER`;
- **Doanh thu:** tổng `total_price` của booking `CONFIRMED`. Booking đã hủy không tính vì
  dự án không có hoàn tiền.

Menu có "Tổng quan" ở đầu và chữ TrainGo dẫn về `/admin`.

Từ 30/10/2026 trang theo bố cục "đài điều khiển" (Phi chọn hướng E), từ trên xuống:

- **Đầu trang:** ngày hôm nay và bộ lọc biểu đồ: mốc nhanh (7 ngày, 30 ngày, Tháng này, Tháng
  trước), chọn từ ngày đến ngày bất kỳ (không quá 366 ngày, không sau hôm nay) và gộp theo ngày,
  tuần hoặc tháng; để "Tự động" thì khoảng đến 31 ngày gộp theo ngày, đến 180 ngày theo tuần, dài
  hơn theo tháng. Tuần bắt đầu từ thứ Hai, tuần và tháng ở hai đầu được cắt theo khoảng. Mọi điều
  khiển áp dụng ngay khi đổi. Khoảng đang chọn nằm trên địa chỉ (`?from=&to=&group=`).
- **Ba đường trên một trục thời gian:** Doanh thu (vùng), Booking đã bán (cột), Người dùng mới
  (que), kèm tổng của khoảng đã chọn. Doanh thu và booking tính theo ngày thanh toán của booking
  `CONFIRMED`.
- **Các số chính** bên cạnh: Doanh thu, Booking, Người dùng, mỗi số có hai thanh tuần này so với
  tuần trước; Chuyến có thanh sắp chạy so với đã chạy. Tên mỗi số là liên kết tới trang chi tiết;
  "đã hủy" mở tab Đã hủy của Booking, "sắp chạy" mở trang Chuyến.
- **Cần chú ý:** ba cột Khẩn, Lưu ý, Theo dõi: tàu chưa có chuyến sắp chạy, tàu sắp hết lịch chạy
  (chuyến cuối nằm trong 7 ngày tới), tàu trống lịch trong 7 ngày tới, chuyến khởi hành trong 48
  giờ tới bán dưới 30% chỗ hoặc từ 90% chỗ. Hiện tối đa 8 và ghi rõ khi còn nhiều hơn.
- **Lịch tàu 7 ngày tới:** mỗi tàu một hàng, mỗi ô là các chuyến trong ngày (giờ, tuyến, số đã
  bán, thanh lấp đầy); ngày chưa có chuyến nổi bật. Tàu cần chú ý xếp trước, tối đa 8 tàu.

Biểu đồ vẽ bằng SVG (`static/js/admin/charts.js`), lấy số từ `GET /api/admin/overview/sales`
và `/users` (tham số `from`, `to`, `group`). Mỗi khối dùng một câu truy vấn gom nhóm nên chi phí không tăng theo số booking.
Bố cục: hai cột ở màn hình rộng, một cột ở 1100px trở xuống.

## Ga và tuyến `/admin/stations`

Trang "Ga và tuyến" (30/10/2026, gộp cả trang Tuyến): bên trái là đường tàu Bắc – Nam, bên phải
là bảng tuyến; mọi thao tác mở trong ngăn kéo bên phải. Thao tác gọi REST `/api/admin/network`,
`/api/admin/stations`, `/api/admin/routes` và vẽ lại trang, không tải lại. Màn hình không in tổng
số ga hay tuyến.

- **Đường tàu:** các ga theo lý trình; giữa hai ga kề ghi km và trạng thái Hai chiều, Thiếu chiều
  hoặc Chưa có tuyến. Đoạn thiếu có nút "Tạo tuyến" tạo ngay phần còn thiếu theo km lý trình, toast
  có "Hoàn tác". Từ hai đoạn thiếu trở lên có "Tạo tất cả". Mỗi đoạn có "Chèn ga".
- **Bảng tuyến:** mỗi cặp ga một dòng gồm Tuyến ("Nối thẳng" khi hai ga không kề), Khoảng cách,
  Chiều (thiếu chiều thì có nút "Tạo chiều ..."), Chuyến sắp chạy; ô tìm theo tên hoặc mã ga không
  phân biệt dấu. Bấm một dòng mở ngăn cặp ga.
- **Đầu trang:** "Thêm ga" mở ngăn nhập ga; "Thêm tuyến" mở form `/admin/routes/new`, dùng cho
  cặp ga không kề.
- **Ngăn ga:** km, tỉnh, địa chỉ; sửa ngay trong ngăn. Ga đã có tuyến thì giữ mã
  ("Ga đã có tuyến nên giữ mã.") và không xóa được ("Không xóa được: ga đang có N
  tuyến. Xóa các tuyến đó trước."). Km trùng báo tên ga đang giữ km đó.
- **Ngăn cặp ga:** hai chiều A → B, B → A với km và số chuyến sắp chạy, hoặc
  "Chưa có" kèm nút "Tạo chiều này" (km điền sẵn theo lý trình). Chiều đã có thì có
  "Sửa khoảng cách" và "Xóa"; xóa bị chặn khi tuyến đã có chuyến.
- **Chèn ga:** km điền sẵn là km trống gần điểm giữa hai ga nhất; km ngoài khoảng
  hai ga thì cảnh báo nhưng vẫn cho lưu. Lưu xong ngăn chuyển sang ga mới.
- Từ 1200px đường tàu và bảng tuyến nằm cạnh nhau; hẹp hơn thì xếp chồng.

Validation ga: `code` không trùng, chỉ gồm chữ cái không dấu và chữ số, tối đa 10
ký tự, tự viết hoa (`sgn` trùng `SGN`); `name` bắt buộc; lý trình (`kmMark`, km
tính từ ga Hà Nội) bắt buộc, nguyên từ 0 đến 5000, không trùng ga khác
(`docs/decisions/0011-chuyen-tao-tu-tau-noi-chang.md`).

Theo quyết định ngày 26/10/2026, trang **không có ma trận tuyến** (#24 không làm
ma trận).

**Tạo nhiều tuyến từ một ga (#39).** Ngăn của một ga có mục "Tạo tuyến từ [ga]": mỗi ga
khác một ô kèm khoảng cách theo lý trình và trạng thái ("Đã có hai chiều", "Đã có chiều
đi", "Đã có chiều về"). Admin tích các ga đến; "Tạo cả chiều về" (mặc định có tích) tạo
thêm chiều ngược lại; "Chọn hai ga kề" tích ga kề phía trước và phía sau còn thiếu tuyến.
Ga đã có đủ chiều cần tạo thì ô tích bị khóa. Nút "Tạo N tuyến" dùng API batch của #38 và
toast "Hoàn tác" xóa đúng các tuyến vừa tạo. Quyết định 27/10/2026: không làm cách chọn
nhiều ô trên ma trận vì dự án không có ma trận.

**Chèn ga thông minh (#38).** Form chèn ga có ô tên dạng combobox gợi ý 13 ga có thật
của đường sắt Thống Nhất mà hệ thống chưa có (Phủ Lý, Nam Định, Ninh Bình, Đông Hà,
Kim Liên, Tam Kỳ, Quảng Ngãi, Diêu Trì, Tuy Hòa, Tháp Chàm, Bình Thuận, Biên Hòa, Dĩ An;
lý trình theo bảng ga Bắc - Nam, tỉnh theo tỉnh sau sáp nhập 2025), ga nằm trong khoảng
đang chèn lên đầu. Chọn một ga thì điền tên "Ga …", mã, tỉnh và km; tên không có trong
danh mục thì mã sinh từ tên (bỏ tiền tố "Ga", bỏ mã đã có ga dùng) và km lấy điểm giữa
hai ga. Ô admin đã sửa tay (mã, tỉnh, km) không bị ghi đè khi gõ tiếp tên.

Lưu ga xong, ngăn chuyển sang "Nối [ga] vào tuyến": tuyến hai chiều tới ga kề phía trước
và phía sau (tích sẵn), tuyến tới hai đầu tuyến (không tích), mỗi chiều một nút. "Tạo N
tuyến" gọi `POST /api/admin/routes/batch` (một transaction, bỏ qua chiều đã có, một cặp
sai thì không tạo gì, tối đa 40 tuyến) và toast "Hoàn tác" gọi
`DELETE /api/admin/routes/batch?ids=` (tất cả hoặc không gì; tuyến đã có chuyến thì từ chối).

Làm ngày 26/10/2026 (#27): `controller/NetworkAdminApiController`,
`service/NetworkService`, `static/js/admin/line-map.js`, `stations.js`,
`station-panels.js`. Form ga cũ (`/admin/stations/new`, `/{id}/edit`) đã gỡ.

## Quản lý tuyến

Danh sách tuyến nằm ở trang Ga và tuyến; `/admin/routes` chuyển hướng về đó.
`/admin/routes/new` và `/admin/routes/{id}/edit` là hai form còn lại.

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

Giao diện (`RouteAdminController`): trang Thymeleaf thường, Post/Redirect/Get về Ga và tuyến.
Form thêm chọn ga đi/ga đến theo lý trình, tự điền khoảng cách theo lý trình khi admin chưa gõ tay,
tích "Tạo luôn chiều ngược" mặc định bật. Lỗi cùng ga và tuyến trùng hiện ở ô ga đến. Form sửa giữ
nguyên hai ga, chỉ đổi khoảng cách. Xóa nằm ở ngăn cặp ga, qua `DELETE /api/admin/routes/{id}`;
service chặn và báo "Không xóa được: tuyến đã có N chuyến.".

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

View "Theo ngày" (lịch theo giờ, Phi chọn 30/10/2026):

- `/admin/trains` mở tàu đầu tiên theo mã; mỗi tàu là một tab "mã + tên", trên
  điện thoại chỉ còn mã.
- **Bốn ô thông tin:** Vị trí ("Đang chạy A → B" hoặc ga đang đứng), Chuyến tới, Chặng mới đi từ
  (kèm "Sớm nhất ..." và "Lịch xếp đến ..."), Đã bán của các chuyến sắp chạy. Giờ sớm nhất là
  giờ đến của chuyến cuối cộng 30 phút; nếu mốc đó đã qua, hoặc tàu chưa có chuyến, thì lấy giờ
  hiện tại làm tròn lên 5 phút, vì chuyến mới phải khởi hành trong tương lai.
- **Lịch theo giờ:** mỗi ngày một hàng, trục ngang 24 giờ, mỗi chặng là một thanh dài đúng thời
  gian chạy (chặng qua đêm tách làm hai ở nửa đêm), dải dưới thanh là tỉ lệ đã bán. Màu theo trạng
  thái Sắp chạy / Đang chạy / Đã chạy / Đã hủy theo D8. Có vạch "Bây giờ", nhãn chờ khi hai chặng
  cách nhau từ 3 giờ, cột "Cuối ngày ở", ô nét đứt "Chặng mới" ở giờ sớm nhất (mở trình hướng dẫn),
  và câu cảnh báo khi chuỗi chặng bị ngắt do một chuyến đã hủy. Bấm thanh mở ngăn chi tiết chuyến.
  Chuyến đã đến nơi được ẩn sẵn sau nút "Hiện N chặng đã chạy".
- **Sơ đồ toa:** đầu tàu và bốn toa cố định, chỉ để xem.
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

### View "Bảng tính" `/admin/trains/{id}?view=bang`

Đầu trang Tàu có nút chia đoạn "Theo ngày / Bảng tính"; `?view=ngay|bang` giữ view khi
đổi tàu, và trình duyệt nhớ lựa chọn gần nhất trong `localStorage` (bị chặn thì mặc
định "Theo ngày"), nên bấm menu Tàu mở lại view đó. View Bảng tính dành cho admin
nhập nhiều chặng một lúc:

- **Bảng lịch:** Ngày, Giờ đi, Từ, Đến, Giờ đến (`+N` khi khác ngày), Km, Giá, Đã bán (đã
  bán/96), Trạng thái. Chặng đã chạy ẩn sau nút "Hiện N chặng đã chạy".
- **Dòng nháp** dưới bảng nối tiếp nhau: ga đi của dòng sau là ga đến của dòng trước,
  giờ đi sớm nhất là giờ đến cộng 30 phút. Giờ đi, giờ đến, giá tự tính theo công
  thức gợi ý (60 km/h làm tròn 5 phút; 1.000đ/km làm tròn 10.000đ) và sửa tay được;
  ô đã sửa tay không bị ghi đè khi chuỗi đổi. Lỗi hiện ngay dưới dòng (chưa có tuyến,
  đi sớm hơn quy định, giờ đến trước giờ đi, giá lẻ).
- **Điền nhanh tới [ga cuối] [nhịp]:** tạo một dòng nháp cho mỗi đoạn giữa hai ga kề
  trên đường đi, theo nhịp "mỗi ngày một chặng, 08:00" hoặc "chạy liên tục, nghỉ 30
  phút". Gọi `GET /api/admin/trains/{id}/plan?from=&to=&pace=DAILY|NONSTOP&firstDeparture=`
  (`TrainScheduleService.planLegs`); thiếu tuyến giữa hai ga kề trả 409 "Thiếu tuyến giữa
  hai ga kề nhau trên đường đi." API này cũng là phần lập kế hoạch của chế độ "Nhiều
  chặng tới một ga" ở trình hướng dẫn (#40).

Nút "Thêm N chuyến" (chỉ tính các dòng đã chọn ga đến) gửi cả lô tới
`POST /api/admin/trains/{id}/trips` (`TripCreationService.createLegs`) trong một
transaction, hoặc tạo hết hoặc không tạo gì. Server khóa dòng tàu (sửa và xóa tàu cũng
khóa dòng này), kiểm lại mọi luật nối chặng (ga đi trang gửi lên phải khớp ga đến của
chuyến cuối hiện tại, nếu không trả 409 để tải lại trang; giờ cắt về phút; giờ đi cách giờ đến ít nhất 30 phút và không ở
quá khứ, có tuyến giữa hai ga, giờ đến sau giờ đi, giá chia hết 1.000) và sinh 96 chỗ
`AVAILABLE` cho mỗi chuyến. Lỗi một chặng trả 409 kèm thông điệp dưới khóa
`legs[i].departureAt` (hoặc `destinationStationId`, `arrivalAt`, `basePrice`,
`originStationId`) và hiện ngay dưới dòng nháp thứ i trong số các dòng đã gửi. Tạo xong
trang tải lại với `?added=` để các dòng mới chớp màu, kèm toast "Đã thêm N chuyến cho
SE1." có nút "Xem" mở ngăn chi tiết chuyến. Bấm một dòng của bảng lịch mở ngăn đó.

### Trình hướng dẫn thêm chuyến `/admin/trains/{id}/trips/new` (#28)

Đầu lịch của view "Theo ngày" có nút "Thêm chuyến", hoặc "Thêm chuyến đầu tiên" khi tàu chưa
có chuyến chưa hủy (Phi chốt 27/10/2026, thay chữ "Nối chặng" của issue #28 AC1: hành động
tạo ra một chuyến, còn nối chặng là tên luật mà trình hướng dẫn tự áp). Nút mở trình hướng dẫn thêm **một** chặng vào cuối lịch, gồm 4 bước;
bấm một bước đã qua trên thanh bước để quay lại sửa. Tàu đã xóa trả trang 404.

- **Đi đâu:** tàu đã có chuyến thì ghi "Tàu đang ở [ga]. Chuyến mới đi tới đâu?"; tàu chưa
  có chuyến thì chọn ga xuất phát trước ("Xuất phát từ ga nào?"). Mỗi tuyến đi ra từ ga
  xuất phát là một thẻ: "Ra Bắc" hoặc "Vào Nam" theo lý trình, tên ga, "km · thời gian chạy".
  Bấm thẻ thì sang bước sau. Các ga chưa có tuyến nằm trong mục gập "N ga chưa có tuyến từ
  [ga]"; mỗi ga có nút "Tạo tuyến và chọn" (xem dưới).
- **Khi nào:** dải 14 ngày tính từ ngày của giờ sớm nhất, hiện 7 ngày mỗi lần với ‹ ›;
  ngày trước giờ sớm nhất bị khóa. Giờ đi mặc định là 08:00 đầu tiên không sớm hơn giờ
  sớm nhất. Ô giờ đi nhận dạng 24 giờ ("8", "845", "8h45" đều thành "08:45"), kèm nút
  "Sớm nhất hh:mm" (khi giờ sớm nhất rơi vào ngày đang chọn và khác 08:00), 06:00, 08:00,
  13:00, 20:00; nút trước giờ sớm nhất bị khóa. Đi sớm hơn thì báo ngay dưới ô, ví dụ "Sớm
  nhất 05/11 10:15: tàu đến Ga Đà Nẵng lúc 09:45 và cần 30 phút quay đầu." Giờ đến tự tính
  theo 60 km/h (làm tròn 5 phút), hiện khóa dạng "T5 05/11 · 09:45" với nút "Sửa"; khi sửa
  có ô giờ, nút −/+ số ngày và "Dùng giờ gợi ý hh:mm".
- **Giá:** gợi ý 1.000đ/km làm tròn 10.000đ, tối thiểu 10.000đ; sửa được, có "Dùng giá gợi
  ý". Giá phải chia hết 1.000đ.
- **Xác nhận:** "Ga đi → Ga đến · ngày giờ đi → giờ đến · giá", câu "1 chuyến, N km. Mỗi
  chuyến mở bán 96 chỗ." và nút "Thêm chuyến".

Cột bên phải xem trước 2 chuyến chưa hủy cuối của tàu, chặng mới tô màu, và câu "Sau đó tàu
ở [ga] từ [ngày giờ]". Dưới 1000px cột này xuống dưới.

Dữ liệu lấy từ `GET /api/admin/trains/{id}/next-leg?origin=` (`TrainScheduleService.nextLegOptions`;
`origin` chỉ đọc khi tàu chưa có chuyến). Giờ sớm nhất là con số banner đang hiện. Thêm
chuyến dùng chung `POST /api/admin/trains/{id}/trips` với View Bảng tính, gửi một chặng và
luôn kèm `originStationId`, nên lịch bị admin khác đổi trong lúc đó trả 409 "Tàu đã ở [ga].
Tải lại trang rồi thử lại." Lỗi `legs[0].x` từ server đưa admin về bước chứa ô x. Thêm xong
hiện "Đã thêm chuyến A → B", "Tàu ở B từ …", nút "Thêm chuyến từ B" (tải lại lựa chọn và về
bước 1) và "Về tàu SE1".

**Nhiều chặng tới một ga và Tạo tuyến và chọn (#40).** Bước 1 có nút chia đoạn "Một chặng /
Nhiều chặng tới một ga" (hiện khi đã có ga xuất phát). Ở chế độ nhiều chặng, mỗi ga trừ ga
xuất phát là một nút; ga mà chuỗi tuyến giữa các ga kề không tới được bị khóa kèm chữ
"Thiếu tuyến giữa đường" (`NextLegOptions.chainReach`, tính ở
`TrainScheduleService.nextLegOptions`). Chọn ga cuối thì sang bước 2 có hai thẻ nhịp chạy
(`role="radio"`): "Mỗi ngày một chặng" (đi 08:00, nghỉ đêm ở mỗi ga) và "Chạy liên tục"
(nghỉ 30 phút ở mỗi ga), cùng dải ngày và ô giờ đi của chặng đầu (cùng luật giờ sớm nhất với
chế độ một chặng). Bấm "Tiếp tục" gọi `GET /api/admin/trains/{id}/plan` (`planLegs`, cùng API
với "Điền nhanh" của View Bảng tính); đổi ga cuối, nhịp hoặc giờ đi thì kế hoạch và giá đã
sửa được tính lại. Bước 3 là bảng Chặng (hai ga, km), Khởi hành, Giá cơ bản (sửa được, báo lỗi
ngay dưới ô khi giá lẻ hoặc trống). Bước 4 liệt kê mọi chặng, tổng km và nút "Thêm N chuyến"
gửi cả lô tới `POST /api/admin/trains/{id}/trips`; lỗi `legs[i].basePrice` hiện dưới dòng thứ
i của bảng giá, lỗi giờ hoặc ga đưa admin về bước chứa chúng. Xong hiện "Đã thêm N chuyến cho
SE1" và nút "Nối tiếp từ [ga cuối]". Cột xem trước liệt kê từng chặng đã lập từ bước 3.

Trong mục gập "N ga chưa có tuyến từ [ga]" (chế độ một chặng), nút "Tạo tuyến và chọn" tạo
tuyến từ ga đang đứng tới ga đó qua `POST /api/admin/routes/batch` (km theo lý trình), chọn
ga đó làm ga đến và sang bước 2, kèm toast "Hoàn tác" xóa đúng tuyến vừa tạo
(`DELETE /api/admin/routes/batch?ids=`) rồi tải lại lựa chọn.

## Quản lý chuyến `/admin/trips`

Chuyến thuộc về tàu, nên admin thêm chuyến ở trang chi tiết tàu. `/admin/trips`
là danh sách mọi chuyến để tra cứu (#29):

- ba nhóm theo D8, so với giờ hiện tại: Sắp chạy (`SCHEDULED`, giờ đi sau bây
  giờ, xếp giờ tăng dần), Đã khởi hành (chưa hủy, giờ đi không sau bây giờ) và
  Đã hủy (`CANCELLED`), hai nhóm sau xếp giờ giảm dần; số đếm mỗi nhóm không
  phụ thuộc bộ lọc;
- lọc theo tàu, tìm theo tên hoặc mã ga không phân biệt dấu ("hue" ra "Huế");
  bộ lọc nằm trên URL (`?tab=&train=&q=&band=&sort=`); ô chọn tàu chỉ có tàu chưa xóa, chuyến
  của tàu đã xóa vẫn nằm trong danh sách;
- sổ tồn chỗ (Phi chọn 30/10/2026): Giờ đi, Tàu, Hành trình, Đã bán, Theo toa (số ghế thật đã bán
  của từng toa), Lấp đầy, Mức bán hoặc Trạng thái. Chip Tất cả, Sắp đầy (từ 90% chỗ), Bán chậm
  (dưới 30% chỗ và khởi hành trong 48 giờ, cùng luật với cảnh báo ở Tổng quan). Sắp xếp theo giờ đi
  (nhóm theo ngày) hoặc theo mức lấp đầy; hiện 40 chuyến một lần với "Hiện thêm";
- bấm một dòng mở ngăn chi tiết chuyến (dưới đây).

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
`GET /api/admin/trips/{id}`. Chân ngăn có nút "Hủy chuyến" theo luật dưới đây; hủy
xong trang tải lại với `?trip=<id>` để danh sách và số đếm lấy từ server, ngăn mở
lại ở trạng thái đã hủy kèm toast "Đã hủy chuyến …". `?trip=<id>` cũng dùng được
làm link mở thẳng một chuyến.

Chuyến không xóa được, chỉ hủy được (`CANCELLED`). Chỉ hủy được chuyến
`SCHEDULED`, chưa khởi hành, và chưa có booking `CONFIRMED` nào (D2, chốt
12/10/2026): hủy chuyến khách đã trả tiền thì phải hoàn tiền, mà hoàn tiền nằm
ngoài phạm vi. Ngăn chi tiết nêu lý do khi không hủy được ("Không hủy được: chuyến
đã khởi hành." hoặc "Không hủy được: chuyến đã có N booking. Hủy các booking đó
trước."); service kiểm tra lại khi nhận lệnh hủy và khóa dòng chuyến, API trả 409
nếu vi phạm.

## Quản lý booking `/admin/bookings`

Ba nhóm theo D8, so giờ đi với hiện tại: Sắp đi (chưa hủy, giờ đi sau bây giờ), Đã
đi (chưa hủy, giờ đi không sau bây giờ), Đã hủy. Mỗi nhóm có số đếm. Ô tìm theo mã booking, tên
khách hoặc tên hành khách không phân biệt hoa thường và dấu, tự gửi sau 300ms; số đếm không đổi
theo ô tìm.

Danh sách theo chuyến (Phi chọn 30/10/2026): booking nằm dưới chuyến của nó. Đầu mỗi chuyến có giờ
đi, tàu, tuyến, số đã bán và thanh theo toa; mỗi booking một dòng gồm mã, khách, từng hành khách với
chỗ ("Toa 1, A05"; booking đã hủy ghi "Đã trả chỗ") và tổng tiền. Sắp đi xếp giờ tăng dần, Đã đi và
Đã hủy xếp giờ giảm dần; hiện 10 chuyến một lần với "Hiện thêm". Bấm giờ đi mở ngăn chi tiết chuyến.

Bấm một dòng mở ngăn chi tiết bên phải và đổi địa chỉ thành `?open=BK…`, nên gửi
được link mở thẳng một booking. Ngăn có khách (tên, email, số điện thoại), chuyến,
giờ đi, thanh toán và bảng vé (họ tên kèm 4 số cuối CCCD, "Toa n · chỗ", giá). Dữ
liệu của ngăn lấy từ `GET /api/admin/bookings/{code}`.

`/admin/bookings?customer={userId}` chỉ hiện booking của khách đó (mọi nhóm), kèm dòng "Booking của
[tên]" và link "Xem tất cả"; ô tìm khi đó chỉ tìm trong booking của khách này. Trang Người dùng
link sang đây.

Admin hủy được booking chưa tới giờ đi. Chân ngăn có nút "Hủy booking"; bấm thì có hộp xác nhận "N
chỗ trên chuyến … sẽ mở bán lại.", rồi toast "Đã hủy BK…. N chỗ đã mở bán lại." và danh sách tải
lại. Chuyến đã khởi hành hoặc booking đã hủy thì không có nút (luật nghiệp vụ không đưa lên giao
diện; service vẫn chặn khi gọi API trực tiếp). Việc hủy là
`POST /api/admin/bookings/{code}/cancel`, gọi `BookingCancellationService`, service
duy nhất giữ luật hủy (booking `CONFIRMED` và chuyến chưa khởi hành đổi sang `CANCELLED`,
các chỗ của booking về `AVAILABLE`, trong một transaction), để khách tự hủy ở "Vé của
tôi" dùng lại đúng service đó; gọi cho booking đã khởi hành hoặc đã hủy trả 409. Nút
Chuyến trong ngăn mở ngăn chi tiết chuyến (xem "Quản lý chuyến").

Admin **không** đổi chỗ sau khi đã đặt, không sửa trạng thái thanh toán thủ công,
không hoàn tiền, không đổi hành khách.

## Danh sách người dùng `/admin/users`

Chỉ xem, hai tab Khách hàng và Quản trị (`?role=CUSTOMER|ADMIN`, mặc định khách) kèm
số đếm, người mới nhất lên đầu. Cột: họ tên, email, số điện thoại, ngày tạo, số
booking (tính cả booking đã hủy). "N booking" là link sang
`/admin/bookings?customer={id}`; khách chưa có booking ghi "Chưa có". Không có CRUD
người dùng.

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
