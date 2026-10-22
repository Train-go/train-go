# Bản nháp: danh sách task chi tiết theo giao diện mới

Date: 2026-10-21

## Status

Đang tạo issue theo từng đợt. Phi chốt 6 câu hỏi và cách viết issue ngày 21/10/2026
(mục "Đã chốt"). Cả 4 đợt đã có issue trên board "TrainGo MVP": #19-#42.

## Vì sao phải làm lại

Các issue #4 đến #16 viết ngày 04/10 và sửa lần cuối ngày 19/10. Từ đó đến nay:

- **Giao diện admin đổi hẳn** (`docs/reports/ui-mau/admin/mau-2.html`):
  - menu chia nhóm;
  - trang Tàu có hai view và trình hướng dẫn nối chặng;
  - ga và tuyến nằm chung trên một sơ đồ dọc;
  - tab Tuyến có ma trận;
  - chi tiết chuyến và booking mở trong ngăn kéo.
- **Giao diện client có mẫu cho 4 bước** (`docs/reports/ui-mau/client/`). Trang chọn
  chuyến tự có ô tìm. Không có ô số hành khách: số chỗ chọn là số vé. Vé mặc định đứng tên
  người đang đăng nhập.
- **Mô hình chuyến đổi** (ADR 0011): ga có lý trình, chuyến tạo từ trang tàu và phải nối
  chặng.
- **Issue còn ghi điều đã bỏ:**
  - `.\mvnw.cmd test` trong mục "Khi nào coi là xong" (đã bỏ test, ADR 0007);
  - T06 "sơ đồ toa" như một trang riêng của admin;
  - T08 chưa đóng.
- **Mỗi issue gom cả backend lẫn giao diện** của một mảng lớn, nên không thấy việc nào thuộc
  server, việc nào thuộc màn hình client hay admin.

## Cách đọc danh sách

- Task chia theo **luồng**: một luồng là một việc người dùng làm từ đầu tới cuối, ví dụ
  "khách đặt vé".
- Trong mỗi luồng, task chia theo **tầng**:
  - `[Server]`: entity method, repository, service, controller, API JSON;
  - `[Admin]`: template Thymeleaf và JS của khu quản trị;
  - `[Client]`: template và JS của khu khách hàng;
  - `[DB]`: migration Flyway.
- Mỗi task gọn trong 0,5 đến 2 ngày.
  - Độ khó: **Dễ** khoảng nửa ngày đến 1 ngày, **Vừa** 1-2 ngày, **Khó** khoảng 2 ngày.
  - Task lớn hơn thì đã được tách nhỏ.
- **Ưu tiên:**
  - **MVP**: phải có để chạy trọn luồng `SPEC.md` mục 50;
  - **Thêm**: có trong mẫu giao diện nhưng làm sau khi MVP chạy được.
- **Xong khi** là kịch bản thử bằng tay rút gọn. Mọi task vẫn phải qua bốn điều kiện chung
  ở cuối file.
- Mẫu giao diện viết bằng React chỉ để xem. App thật dùng Thymeleaf và JavaScript thuần gọi
  `/api/...` (AGENTS.md). Task `[Admin]` và `[Client]` là dựng lại màn hình theo mẫu, không
  chép code React.

## Luồng 0. Nền dùng chung

| ID | Task | Tầng | Độ khó | Ưu tiên | Cần trước |
| --- | --- | --- | --- | --- | --- |
| N1 | Migration `V6` theo các quyết định ở mục "Cần Phi chốt" | DB | Vừa | MVP | Phi chốt Q1-Q4 |
| N2 | Khung admin mới | Admin | Khó | MVP | Phi chốt Q6 |
| N3 | Khung client và thanh bước đặt vé | Client | Vừa | MVP | Không |
| N4 | Cấu hình toa cố định `CoachLayout` | Server | Dễ | MVP | Không |
| N5 | Xử lý lỗi chung: trang lỗi và lỗi JSON | Server | Dễ | MVP | Không |

- **N1:**
  - Viết `V6__...` đổi đúng các cột Phi chốt, sửa entity tương ứng.
  - Sửa `docs/reports/erd.html`, `erd-hien-tai.md` cho khớp database.
  - Bỏ qua task này nếu Phi chốt giữ nguyên schema.
  - **Xong khi:** Flyway chạy V6 trên database đang có V5. Hibernate `validate` không lỗi.
    ERD và database cùng tên cột.
- **N2:**
  - `templates/admin/layout/base.html` có menu trái chia nhóm:
    - Vận hành: Tàu, Chuyến;
    - Dữ liệu nền: Ga và tuyến, Tuyến;
    - Bán vé: Booking, Người dùng.
  - Góc menu có ngày giờ hiện tại. Trên điện thoại, menu thành thanh trên cùng cuộn ngang.
  - Chuyển màu, chữ và các thành phần trong `docs/reports/ui-mau/admin/shared/admin.css`
    vào `static/css/admin/admin.css`: nút, nút chia đoạn, bảng, thẻ trạng thái, ô nhập có
    đơn vị.
  - `static/js/admin/admin.js` có ba hàm dùng chung:
    - mở/đóng ngăn kéo bên phải (`Esc` đóng, trả focus về nút đã mở);
    - hộp xác nhận;
    - toast có nút "Hoàn tác".
  - **Xong khi:** mọi trang admin cũ vẫn mở được trong khung mới. Ở 1440px và 390px không
    trang nào cuộn ngang. `Esc` đóng ngăn kéo.
- **N3:**
  - `templates/layout/base.html` và fragment:
    - navbar: Tìm vé, Vé của tôi, Hướng dẫn, Đăng nhập hoặc tên người dùng;
    - thanh 4 bước: Chọn chuyến, Chọn chỗ, Hành khách, Thanh toán;
    - footer.
  - Màu navy và cobalt theo mẫu client.
  - **Xong khi:** trang chủ và một trang trống dùng khung mới hiển thị đúng ở 1440px và
    390px.
- **N4:**
  - `model/dto/CoachLayout`:
    - toa 1-2 ghế `A01`-`A32`;
    - toa 3-4 giường `B01`-`B16`, 4 khoang;
    - không có phụ thu giường (Q4).
  - Có hàm sinh 96 chỗ cho một chuyến. Phần dữ liệu của T06 cũ chuyển vào đây.
  - **Xong khi:** gọi hàm sinh ra đúng 96 chỗ, đúng mã và đúng số khoang.
- **N5:**
  - `@ControllerAdvice`:
    - trang lỗi tiếng Việt cho 403, 404 (chuyến, booking, vé không có) và 500;
    - đường dẫn `/api/**` trả JSON `{ message, fields }` để JS hiện lỗi cạnh ô nhập.
  - **Xong khi:** gõ `/trips/999999` thấy trang 404 tiếng Việt. Gọi API với dữ liệu sai nhận
    JSON có `fields`.

## Luồng 1. Tài khoản (thay #4 T02)

| ID | Task | Tầng | Độ khó | Ưu tiên | Cần trước |
| --- | --- | --- | --- | --- | --- |
| A1 | Đăng nhập bằng bảng `users` và phân quyền | Server | Vừa | MVP | Không |
| A2 | Trang đăng nhập | Client | Dễ | MVP | A1, N3 |
| A3 | Đăng ký (D4) | Server + Client | Vừa | MVP | A1, N3 |
| A4 | Navbar và header theo người đang đăng nhập | Client + Admin | Dễ | MVP | A1, N2, N3 |

- **A1:**
  - `UserDetailsService` tìm theo email, không phân biệt hoa thường. Dùng
    `BCryptPasswordEncoder`.
  - Xóa `spring.security.user.*`.
  - `SecurityConfig`:
    - `/admin/**` và `/api/admin/**` chỉ cho `ADMIN`;
    - `/booking/**`, `/my-trips/**`, `/tickets/**` chỉ cho `CUSTOMER`;
    - trang chủ, `/trips/**` và `/api/trips/**` ai cũng xem được.
  - CSRF cho cả form lẫn `fetch`.
  - **Xong khi:**
    - `admin@traingo.vn` vào được `/admin`;
    - `an.nguyen@example.com` gõ `/admin/stations` bị chặn;
    - chưa đăng nhập mà mở `/my-trips` thì bị đưa về trang đăng nhập.
- **A2:**
  - Trang `/login` theo khung client.
  - Sai mật khẩu thì báo chung "Email hoặc mật khẩu không đúng".
  - Đăng nhập xong:
    - quay lại đúng trang đang đặt dở (ví dụ trang chọn chỗ);
    - admin thì vào thẳng `/admin`.
  - **Xong khi:** chọn chỗ khi chưa đăng nhập, đăng nhập xong quay lại đúng chuyến và đúng
    các chỗ đã chọn.
- **A3:**
  - Form `/register` theo D4. Lỗi hiện cạnh từng ô.
  - Tài khoản tạo qua form luôn là `CUSTOMER`. Đăng ký xong tự đăng nhập.
  - **Xong khi:**
    - email `An.Nguyen@Example.com` bị báo trùng;
    - mật khẩu 7 ký tự bị chặn;
    - hai ô mật khẩu khác nhau bị chặn.
- **A4:**
  - Navbar client hiện tên và "Đăng xuất" khi đã đăng nhập.
  - Header admin có tên admin và "Đăng xuất".
  - **Xong khi:** đăng xuất ở cả hai khu xong quay về trang chủ.

## Luồng 2. Ga và tuyến, phía admin (thay #6 T04)

Phần quản lý ga trên trang cũ `/admin/stations` đã chạy với database. Luồng này chuyển nó
sang giao diện sơ đồ.

| ID | Task | Tầng | Độ khó | Ưu tiên | Cần trước |
| --- | --- | --- | --- | --- | --- |
| G1 | `RouteService`: tạo, sửa, xóa tuyến theo D2 | Server | Vừa | MVP | N1 |
| G2 | API cho sơ đồ và ma trận | Server | Vừa | MVP | G1, N5 |
| G3 | Trang "Ga và tuyến": sơ đồ dọc Bắc - Nam | Admin | Khó | MVP | G2, N2 |
| G4 | Ngăn ga: xem, sửa, xóa ga | Admin | Vừa | MVP | G3 |
| G5 | Tạo tuyến từ một ga tới nhiều ga | Admin | Vừa | Thêm | G4 |
| G6 | Chèn ga giữa hai ga, tự điền tên, mã, tỉnh, km | Admin | Vừa | Thêm | G4 |
| G7 | Gợi ý tuyến sau khi thêm ga, có Hoàn tác | Admin | Vừa | Thêm | G6 |
| G8 | Ma trận ga × ga dùng chung | Admin | Khó | MVP | G2, N2 |
| G9 | Tab Tuyến: ma trận có ô bật ra | Admin | Vừa | MVP | G8 |
| G10 | Tab Tuyến: danh sách, tìm, form "Thêm tuyến" | Admin | Vừa | MVP | G1, N2 |
| G11 | Chọn nhiều ô để tạo nhiều tuyến một lần | Admin | Vừa | Thêm | G8 |

- **G1:**
  - Danh sách xếp theo lý trình ga đi rồi ga đến.
  - Tạo một chiều hoặc cả hai chiều trong một lần.
  - Ga đi khác ga đến, không trùng cặp ga, khoảng cách lớn hơn 0.
  - Tuyến đã có chuyến:
    - không xóa được;
    - khi sửa chỉ đổi được khoảng cách.
  - Mỗi vi phạm là một exception có câu báo tiếng Việt.
  - **Xong khi:** xóa một tuyến đã có chuyến (ví dụ tuyến của chuyến đầu tiên của SE1) bị
    chặn và nêu số chuyến. Tạo trùng `HUE → DNG` bị chặn.
- **G2:**
  - `GET /api/admin/network`: ga theo lý trình, mọi tuyến, số chuyến sắp chạy và tổng số
    chuyến của từng tuyến.
  - Tuyến:
    - `POST /api/admin/routes` nhận một danh sách chiều và trả về id để hoàn tác;
    - `PUT /api/admin/routes/{id}` sửa km;
    - `DELETE /api/admin/routes/{id}`.
  - Ga: `POST`, `PUT`, `DELETE /api/admin/stations`, dùng lại `StationService`, trả lỗi theo
    từng ô.
  - **Xong khi:** gọi từng API bằng DevTools (hoặc qua trang G3) đúng dữ liệu. Lỗi trả JSON
    của N5.
- **G3:**
  - `/admin/stations` vẽ SVG:
    - ga đặt theo km (mỗi ga cách nhau tối thiểu 60px);
    - đoạn giữa hai ga kề có 3 kiểu nét: hai chiều, một chiều, chưa có tuyến;
    - cung bên trái cho tuyến dài;
    - chú thích bên dưới.
  - Bấm ga thì mở ngăn ga, bấm đoạn hoặc cung thì mở ngăn cặp ga (hai chiều, tạo chiều
    thiếu, xóa có chặn).
  - Ngăn phải mặc định hiện ma trận (G8).
  - Màn hình dưới 1100px: ngăn chuyển xuống dưới sơ đồ.
  - **Xong khi:** đủ 8 ga và 16 tuyến của V5. Bấm từng loại đối tượng mở đúng ngăn. Ở 390px
    không cuộn ngang.
- **G4:**
  - Xem km, tỉnh, địa chỉ của ga.
  - Sửa ngay trong ngăn. Mã ga bị khóa khi ga đã có tuyến.
  - Xóa có hộp xác nhận. Ga đang có tuyến thì nút xóa bị khóa, kèm lý do.
  - **Xong khi:** đổi km Huế thành 791 thì báo "Km 791 đã là lý trình của ga Đà Nẵng".
- **G5:**
  - Trong ngăn ga, tích các ga đến (mặc định tạo cả chiều về).
  - Nút "Chọn hai ga kề".
  - Nút tạo ghi rõ số tuyến sẽ tạo. Toast có "Hoàn tác".
  - **Xong khi:** từ Huế tích Hà Nội và Sài Gòn thì tạo đúng 4 tuyến. Hoàn tác xóa cả 4.
- **G6:**
  - Dấu + giữa hai ga mở form chèn ga.
  - Gõ tên thì gợi ý trong danh mục 13 ga thật trên đường sắt Thống Nhất (một file JS tĩnh,
    lấy từ khối `js-smart` của `docs/reports/ui-mau/admin/index.html`). Chọn một ga thì tự điền mã, tỉnh và km.
  - Tên không có trong danh mục:
    - mã sinh từ tên;
    - km lấy điểm giữa hai ga, sửa được.
  - **Xong khi:** chèn giữa Đà Nẵng và Nha Trang, gõ "Quảng" rồi chọn Quảng Ngãi thì ra mã
    `QNG`, km 928, tỉnh Quảng Ngãi.
- **G7:**
  - Lưu ga xong, ngăn chuyển sang danh sách tuyến gợi ý:
    - tuyến hai chiều với hai ga kề, tích sẵn;
    - tuyến với hai đầu tuyến, không tích.
  - Bấm một lần tạo hết. Toast có "Hoàn tác".
  - **Xong khi:** thêm Quảng Ngãi rồi tạo 4 tuyến với Đà Nẵng và Nha Trang trong một lần.
- **G8:**
  - Component JS dùng chung cho G3 và G9.
  - Hàng là ga đi, cột là ga đến. Ô có ba kiểu: có tuyến (hiện km), có chuyến sắp chạy, trống.
  - Phím mũi tên di chuyển giữa các ô. Ma trận cuộn ngang trong khung của nó.
  - **Xong khi:** đi hết ma trận bằng bàn phím. Ô `HUE → DNG` hiện 103 km.
- **G9:**
  - `/admin/routes` dùng ma trận. Bấm ô mở ô bật ra:
    - ô trống: form khoảng cách điền sẵn theo lý trình, tích "Tạo luôn chiều ngược";
    - ô có tuyến: km, số chuyến sắp chạy, "Tạo chiều ngược", "Xóa tuyến" có chặn.
  - `Esc` đóng ô bật ra và trả focus về ô.
  - **Xong khi:** ô Huế → Vinh tạo 2 tuyến 369 km. Xóa tuyến đã có chuyến bị khóa, kèm lý do.
- **G10:**
  - Chế độ Danh sách có các cột: tuyến, km, chiều ngược đã có chưa, số chuyến sắp chạy,
    tổng số chuyến.
  - Ô tìm theo tên hoặc mã ga.
  - Ngăn "Thêm tuyến":
    - nút đổi chiều;
    - km tự điền theo lý trình, có nút quay về số gợi ý;
    - tích "Tạo luôn chiều ngược".
  - **Xong khi:** tìm "vinh" ra đúng các tuyến có Vinh. Thêm tuyến trùng thì báo lỗi ở ô
    ga đến.
- **G11:**
  - Nút "Chọn nhiều ô" và tích "Chọn kèm chiều ngược".
  - Thanh dưới cùng đếm số tuyến sẽ tạo. Toast có Hoàn tác.
  - **Xong khi:** chọn 2 ô kèm chiều ngược thì tạo 4 tuyến.

## Luồng 3. Tàu và chuyến, phía admin (thay #5 T03, #9 T07, phần admin của #8 T06)

| ID | Task | Tầng | Độ khó | Ưu tiên | Cần trước |
| --- | --- | --- | --- | --- | --- |
| T1 | `TrainService`: thêm, sửa tàu | Server | Dễ | MVP | Không |
| T2 | Lịch của tàu và luật nối chặng | Server | Vừa | MVP | N1 |
| T3 | Tạo chuyến: một hoặc nhiều chặng, sinh 96 chỗ | Server | Khó | MVP | T2, N4 |
| T4 | Hủy chuyến (D2) | Server | Dễ | MVP | T2 |
| T5 | Trang Tàu: tab tàu, nút đổi view, form tàu | Admin | Vừa | MVP | T1, N2 |
| T6 | View "Theo ngày": banner và lịch theo ngày | Admin | Vừa | MVP | T2, T5 |
| T7 | Trình hướng dẫn nối chặng, chế độ một chặng | Admin | Khó | MVP | T3, T6 |
| T8 | Trình hướng dẫn, chế độ nhiều chặng tới một ga | Admin | Vừa | Thêm | T7 |
| T9 | "Tạo tuyến và chọn" ngay ở bước chọn ga đến | Admin | Dễ | Thêm | T7, G2 |
| T10 | View "Bảng tính": bảng lịch và dòng nháp | Admin | Khó | Thêm | T3, T5 |
| T11 | View "Bảng tính": điền nhanh tới ga cuối | Admin | Vừa | Thêm | T10 |
| T12 | Ngăn chi tiết chuyến: sơ đồ chỗ, hành khách, hủy | Admin | Vừa | MVP | T4, N4 |
| T13 | Trang Chuyến: 3 nhóm, lọc tàu, tìm ga | Server + Admin | Vừa | MVP | T12 |

- **T1:**
  - Chuyển phần tàu ra khỏi `AdminService`.
  - Mã tàu gồm chữ rồi số (ví dụ `SE4`), không trùng. Mã không đổi được khi tàu đã có chuyến.
  - Không có xóa tàu.
  - **Xong khi:** thêm `SE4` được. Thêm lại `se4` bị báo trùng. Sửa mã `SE1` bị khóa.
- **T2:**
  - Lịch các chuyến của tàu theo giờ đi. Đánh dấu chỗ đứt chuỗi do một chuyến bị hủy.
  - Các phép tính:
    - chuyến cuối chưa hủy;
    - giờ đi sớm nhất = giờ đến chuyến cuối + 30 phút;
    - vị trí tàu lúc này (đang chạy hay đang ở ga nào);
    - giờ đến gợi ý theo 60 km/h, làm tròn 5 phút;
    - giá gợi ý khoảng 1.000đ/km, làm tròn 10.000đ;
    - đường đi qua các ga kề giữa hai ga (cho T8, T11).
  - **Xong khi:** với dữ liệu V5, ga đi của SE1 là ga đến của chuyến cuối, giờ sớm nhất bằng
    giờ đến chuyến đó cộng 30 phút (so bằng SQL). SE2 chưa có chuyến nào.
- **T3:**
  - `POST /api/admin/trains/{code}/trips` nhận một hoặc nhiều chặng.
  - Service kiểm tra lại luật nối chặng cho từng chặng, nối tiếp nhau, không tin dữ liệu
    trình duyệt gửi.
  - Mỗi chuyến sinh 96 `TripSeat` từ `CoachLayout`. Cả lô nằm trong một transaction.
  - Lỗi trả theo từng chặng.
  - **Xong khi:**
    - giờ đi sớm hơn giờ sớm nhất bị chặn với lý do "cần 30 phút quay đầu";
    - 3 chặng hợp lệ tạo 3 chuyến và 288 chỗ;
    - 1 chặng sai thì không chuyến nào được tạo.
- **T4:**
  - `Trip.cancel()` chỉ chạy được với chuyến `SCHEDULED`, chưa khởi hành, chưa có booking
    `CONFIRMED`.
  - **Xong khi:** hủy chuyến đã có booking bị chặn, kèm số booking.
- **T5:**
  - `/admin/trains/{code}`: tab SE1/SE2/SE3, nút "Theo ngày" / "Bảng tính".
  - Đường dẫn giữ view đang xem (`?view=bang`). Trình duyệt nhớ lần chọn gần nhất.
  - Ngăn "Thêm tàu" / "Sửa tàu".
  - **Xong khi:** đổi view rồi đổi tàu vẫn giữ view. Bấm menu Tàu mở lại view đã chọn.
- **T6:**
  - Banner "Chặng tiếp theo đi từ X": vị trí tàu, lịch xếp đến lúc nào, giờ sớm nhất đi lại,
    nút "Nối chặng".
  - Lịch nhóm theo ngày. Ẩn hoặc hiện các chặng đã chạy.
  - Bấm một chuyến mở ngăn chi tiết (T12).
  - **Xong khi:** SE1 hiện đúng banner và các ngày. SE2 hiện "cần chặng đầu tiên".
- **T7:**
  - `/admin/trains/{code}/trips/new` có 4 bước: Đi đâu, Khi nào, Giá, Xác nhận.
    - Đi đâu: thẻ các ga có tuyến từ ga đang đứng, ghi chiều "Ra Bắc / Vào Nam", km và thời
      gian chạy.
    - Khi nào: dải ngày, giờ đi, giờ đến tự tính (sửa được).
    - Giá: gợi ý sẵn.
    - Xác nhận.
  - Bên phải xem trước lịch sau khi thêm.
  - Xong thì có màn hình "Nối tiếp từ X". Tàu chưa có chuyến thì chọn ga xuất phát trước.
  - **Xong khi:** tạo chặng đầu SE2 Hà Nội → Thanh Hóa. SE1 đi lúc 01:00 bị chặn, kèm lý do.
- **T8:**
  - Chọn ga cuối. Hai nhịp chạy: "mỗi ngày một chặng 08:00" hoặc "chạy liên tục, nghỉ 30
    phút".
  - Bảng sửa giá từng chặng. Tạo cả lô qua T3.
  - **Xong khi:** từ Đà Nẵng tới Sài Gòn tạo 2 chuyến đúng giờ.
- **T9:**
  - Ga chưa có tuyến từ ga đang đứng hiện ở mục gập "N ga chưa có tuyến".
  - Bấm "Tạo tuyến và chọn" thì tạo tuyến rồi chọn luôn ga đó. Toast có Hoàn tác.
  - **Xong khi:** từ Hà Nội tạo tuyến tới Đà Nẵng ngay trong trình hướng dẫn.
- **T10:**
  - Bảng lịch có cột: ngày, giờ đi, từ, đến, giờ đến, km, giá, vé đã bán, trạng thái.
  - Dưới bảng là các dòng nháp:
    - ga đi bị khóa theo chặng trước;
    - giờ chưa sửa thì đi theo chuỗi;
    - lỗi báo ngay dưới từng dòng.
  - Nút "Thêm dòng" và "Thêm N chuyến".
  - **Xong khi:** nhập 2 dòng nháp, dòng 2 tự nhận ga đi là ga đến của dòng 1. Tạo được 2
    chuyến.
- **T11:**
  - "Điền nhanh tới [ga cuối] [nhịp chạy]" tạo sẵn dòng nháp cho mọi ga kề trên đường đi.
  - **Xong khi:** Huế tới Sài Gòn tạo 3 dòng nháp đúng thứ tự.
- **T12:**
  - Ngăn dùng chung cho trang Tàu, trang Chuyến và trang Booking. Ngăn hiện:
    - thông tin chuyến;
    - số chỗ đã bán trên 96;
    - sơ đồ 4 toa (bấm chỗ đã bán để thấy hành khách);
    - bảng hành khách, link sang booking;
    - nút "Hủy chuyến", bị khóa kèm lý do khi không hủy được.
  - **Xong khi:** mở chuyến có vé thấy đúng chỗ đã bán. Hủy chuyến chưa có booking được.
- **T13:**
  - `/admin/trips` có 3 nhóm theo giờ đi so với hiện tại (D8): Sắp chạy, Đã khởi hành,
    Đã hủy.
  - Lọc theo tàu, tìm theo ga, nhóm theo ngày. Truy vấn lọc ở server.
  - Ghi chú "Chuyến mới được tạo ở trang của từng tàu".
  - **Xong khi:** số đếm của 3 nhóm khớp SQL đếm tay.

## Luồng 4. Khách tìm chuyến và đặt vé (thay #7 T05, #8 T06, #11 T09, #13 T11, #14 T12)

| ID | Task | Tầng | Độ khó | Ưu tiên | Cần trước |
| --- | --- | --- | --- | --- | --- |
| K1 | Tìm chuyến theo ga, ngày, bộ lọc, sắp xếp | Server | Vừa | MVP | N1 |
| K2 | Dải 7 ngày kèm giá thấp nhất hoặc "Hết vé" | Server | Dễ | Thêm | K1 |
| K3 | Trang chủ đọc dữ liệu V5, ô tìm nhanh | Client | Dễ | MVP | N3, K1 |
| K4 | Trang Chọn chuyến | Client | Khó | MVP | K1, N3 |
| K5 | API sơ đồ chỗ của chuyến | Server | Dễ | MVP | N4 |
| K6 | Trang Chọn chỗ | Client | Khó | MVP | K5, A1 |
| K7 | Bản nháp booking trong session (D7) | Server | Vừa | MVP | K5, A1 |
| K8 | Trang Hành khách: thẻ vé, đổi người đi | Client | Vừa | MVP | K7 |
| K9 | Thanh toán: khóa chỗ, tạo booking và vé (D5, D7) | Server | Khó | MVP | K7 |
| K10 | Trang Thanh toán: hóa đơn và kết quả | Client | Vừa | MVP | K9 |
| K11 | Trang vé điện tử | Server + Client | Vừa | MVP | K9 |

- **K1:**
  - Tìm theo ga đi, ga đến, ngày (`departure_at` trong ngày đó).
  - Chỉ lấy chuyến `SCHEDULED` chưa khởi hành.
  - Mỗi chuyến trả: giờ đi, giờ đến, thời gian chạy, giá thấp nhất, số chỗ còn theo loại
    (ghế, giường).
  - Lọc theo khung giờ, loại chỗ, ẩn chuyến hết vé. Sắp xếp sớm nhất, rẻ nhất, nhanh nhất.
  - **Xong khi:** Huế → Đà Nẵng ngày có chuyến ra đúng chuyến và đúng số chỗ còn. Ngày
    không có chuyến thì trả danh sách rỗng.
- **K2:**
  - 7 ngày quanh ngày đang xem. Mỗi ngày có giá thấp nhất, hoặc "Hết vé" khi không còn chỗ.
  - **Xong khi:** ngày không có chuyến không hiện giá.
- **K3:**
  - Ô tìm ở trang chủ chỉ là lối tắt điền sẵn trang Chọn chuyến.
  - Danh sách ga và tuyến phổ biến lấy từ database.
  - Bỏ Lào Cai, Quy Nhơn, SNT khỏi mẫu cũ.
  - **Xong khi:** chọn Huế → Đà Nẵng ở trang chủ mở trang Chọn chuyến điền sẵn hai ga.
- **K4:**
  - `/trips` có ô ga đi, ga đến riêng, không đưa người dùng về trang chủ. Dải ngày có ‹ › và
    lịch. Bộ lọc và sắp xếp như K1.
  - Thẻ chuyến mở rộng ra từng loại chỗ, mỗi loại có giá và nút "Chọn".
  - Không có ô số hành khách.
  - Trạng thái trống: "Không có chuyến", có gợi ý đổi ngày.
  - **Xong khi:** đổi ngày bằng ‹ › tải đúng danh sách. Ở 390px không cuộn ngang.
- **K5:**
  - `GET /api/trips/{tripId}/seat-map`: 4 toa, trạng thái từng chỗ, giá của chuyến.
  - **Xong khi:** đặt một chỗ thành `BOOKED` bằng SQL thì API trả đúng chỗ đó.
- **K6:**
  - `/booking/{tripId}/seats`:
    - đoàn tàu nằm ngang 4 toa, toa hết chỗ bị khóa;
    - sơ đồ ghế hoặc khoang giường, chọn 1 đến 10 chỗ.
  - Cột tóm tắt: chuyến, các chỗ đã chọn, tổng tiền.
  - Bấm "Tiếp tục":
    - chưa đăng nhập thì sang trang đăng nhập rồi quay lại (A2);
    - chỗ vừa bị người khác đặt thì báo và tải lại sơ đồ.
  - **Xong khi:**
    - chọn chỗ thứ 11 bị chặn;
    - mở hai trình duyệt chọn cùng một chỗ, cả hai đều qua được bước này (chưa khóa chỗ,
      ADR 0010).
- **K7:**
  - `BookingDraft`, `BookingDraftStore`: chuyến, các chỗ, người đi của từng vé (mặc định là
    tài khoản đang đăng nhập).
  - Đổi chuyến thì xóa bản nháp cũ.
  - Chưa ghi gì vào database.
  - **Xong khi:** bỏ dở ở bước Hành khách thì database không có dòng mới nào.
- **K8:**
  - `/booking/passengers`: mỗi chỗ là một thẻ vé (thân trắng, cuống màu, răng cưa), đứng tên
    người đang đăng nhập.
  - "Đổi người đi" mở form nhỏ; các ô bắt buộc theo Q2.
  - Nút "Chọn lại chỗ", "Tiếp tục".
  - **Xong khi:** đổi tên vé 2 rồi quay lại, tên vẫn còn trong bản nháp.
- **K9:**
  - Một transaction:
    1. kiểm tra chuyến còn `SCHEDULED` và chưa khởi hành;
    2. `UPDATE trip_seats ... WHERE status = 'AVAILABLE'`, thiếu dòng nào thì rollback;
    3. tạo booking `CONFIRMED` / `PAID`, mã `BKyyyyMMnnn`;
    4. tạo vé `TK...-01`, `-02`, giá chốt lúc đặt.
  - Chỗ vừa bị người khác đặt thì quay về trang chọn chỗ với thông báo.
  - **Xong khi:**
    - đánh dấu một chỗ `BOOKED` bằng SQL ngay trước khi bấm "Thanh toán" thì không tạo gì
      và báo đúng câu;
    - hai trình duyệt cùng thanh toán một chỗ thì chỉ một người thành công.
- **K10:**
  - `/booking/payment`: hóa đơn gồm chuyến, các chỗ, người đi, tổng tiền, nút "Thanh toán
    [số tiền]", câu "Hủy được trước giờ tàu chạy, không mất phí", "Đổi chỗ".
  - Thành công thì hiện "Đặt vé thành công" và nút "Xem các vé".
  - **Xong khi:** bấm "Thanh toán" hai lần liên tiếp chỉ tạo một booking.
- **K11:**
  - `/tickets/{ticketId}` và trang các vé của một booking.
  - Vé ghi: mã vé, mã booking, người đi, tuyến, tàu, giờ đi, toa, chỗ.
  - Chỉ chủ booking xem được.
  - **Xong khi:** tài khoản khác gõ đúng đường dẫn vé bị chặn.

## Luồng 5. Vé của tôi, phía khách (thay #12 T10)

| ID | Task | Tầng | Độ khó | Ưu tiên | Cần trước |
| --- | --- | --- | --- | --- | --- |
| V1 | Truy vấn booking của khách theo 3 nhóm | Server | Dễ | MVP | A1 |
| V2 | `BookingCancellationService` dùng chung | Server | Vừa | MVP | V1 |
| V3 | Trang Vé của tôi và chi tiết booking | Client | Vừa | MVP | V1, N3 |
| V4 | Hủy booking phía khách | Client | Dễ | MVP | V2, V3 |

- **V1:**
  - Ba nhóm theo giờ đi so với hiện tại (D8): Sắp đi, Đã đi, Đã hủy.
  - Chỉ lấy booking của người đang đăng nhập.
  - **Xong khi:** `an.nguyen@example.com` thấy đúng số booking như SQL đếm.
- **V2:**
  - Trong một transaction: booking `CONFIRMED` → `CANCELLED`, các chỗ `BOOKED` →
    `AVAILABLE`.
  - Chặn khi chuyến đã khởi hành.
  - Admin (Q2) gọi lại đúng service này.
  - **Xong khi:** hủy xong, chỗ hiện trống lại trên sơ đồ chọn chỗ.
- **V3:**
  - `/my-trips` có 3 tab. `/my-trips/{bookingId}` hiện chi tiết và link tới các vé.
  - Kiểm tra quyền sở hữu.
  - **Xong khi:** gõ id booking của người khác bị chặn.
- **V4:**
  - Nút hủy có hộp xác nhận. Không hủy được thì hiện lý do.
  - **Xong khi:** hủy booking của chuyến đã khởi hành bị chặn.

## Luồng 6. Booking và người dùng, phía admin (thay #15 T13)

| ID | Task | Tầng | Độ khó | Ưu tiên | Cần trước |
| --- | --- | --- | --- | --- | --- |
| Q1 | Truy vấn booking cho admin: 3 nhóm, tìm, lọc theo khách | Server | Dễ | MVP | Không |
| Q2 | Trang Booking và ngăn chi tiết, hủy booking | Admin | Vừa | MVP | Q1, V2, N2 |
| Q3 | Trang Người dùng | Server + Admin | Dễ | MVP | N2 |
| Q4 | Tổng quan (làm cuối) | Server + Admin | Dễ | MVP | Q1 |

- **Q1:**
  - Ba nhóm như V1.
  - Tìm theo mã booking hoặc tên khách. Lọc theo một khách (`?customer=`).
  - **Xong khi:** tìm "BK2026" ra mọi booking.
- **Q2:**
  - `/admin/bookings` có tab, ô tìm.
  - Ngăn chi tiết: khách, chuyến (bấm mở ngăn chuyến T12), giờ đi, tiền, bảng vé.
  - Nút "Hủy booking", bị khóa kèm lý do khi chuyến đã khởi hành.
  - Admin không đổi chỗ, không sửa thanh toán.
  - **Xong khi:** hủy một booking sắp đi, mở lại chuyến đó thấy chỗ đã trống.
- **Q3:**
  - `/admin/users` có hai tab Khách hàng / Quản trị.
  - Cột: họ tên, email, ngày tạo, số booking. Số booking là link sang trang Booking đã lọc
    theo khách đó.
  - Chỉ xem.
  - **Xong khi:** bấm "2 booking" của một khách mở đúng 2 booking.
- **Q4:**
  - Bốn con số: tổng booking, tổng chuyến, tổng người dùng, tổng doanh thu, đọc từ database.
  - Bố cục làm sau cùng.
  - **Xong khi:** bốn số khớp SQL.

## Luồng 7. Hoàn thiện (thay #16 T14)

| ID | Task | Tầng | Độ khó | Ưu tiên | Cần trước |
| --- | --- | --- | --- | --- | --- |
| H1 | Đi trọn luồng `SPEC.md` mục 50 trên app thật | Cả nhóm | Vừa | MVP | Mọi task MVP |
| H2 | Dọn code cũ: `AdminController`, `AdminService`, template admin cũ | Server + Admin | Dễ | MVP | Các trang admin đã chuyển |
| H3 | Rà giao diện ở 1440, 1024, 390, 320px và bàn phím | Admin + Client | Vừa | MVP | H1 |
| H4 | README, RUNBOOK, ảnh màn hình cho báo cáo | Cả nhóm | Dễ | MVP | H1 |

## Thứ tự làm và chia người

```
Đợt 1 (song song, không chờ nhau)
  N2 N3 N4 N5        A1 -> A2 A3 A4        G1 -> G2        T1 T2        K1 K5
Đợt 2
  G3 G4 G8 G9 G10    T3 T4 T5 T6 -> T7 -> T12 T13        K3 K4 K6 K7 -> K8
  V1 V2 V3 V4        Q1
Đợt 3
  K9 -> K10 K11      Q2 Q3
Đợt 4
  Ưu tiên "Thêm": G5 G6 G7 G11 T8 T9 T10 T11 K2, rồi Q4, H1-H4
```

Chuỗi dài nhất vẫn là phần đặt vé: `A1 -> K6 -> K7 -> K8 -> K9 -> K10 -> H1`.

Gợi ý chia theo phần việc cũ, mỗi người làm trọn cả server lẫn giao diện của luồng mình:

| Phần việc | Luồng | Số task MVP | Số task Thêm |
| --- | --- | --- | --- |
| A (thành viên mới) | Luồng 2 Ga và tuyến, Q3, Q4 | 9 | 4 |
| B | Luồng 3 Tàu và chuyến, N4 | 10 | 4 |
| C | Luồng 1, Luồng 4, Luồng 5, Q1, Q2 | 20 | 1 |

Phần C đang nặng nhất. Nếu cần cân lại, chuyển K3, K4 (trang chủ, trang Chọn chuyến)
sang người làm phần A. Hai task này chỉ đọc dữ liệu, không đụng transaction. N2, N3, N5
và H1-H4 chia chung.

## Đã chốt (21/10/2026)

| ID | Câu hỏi | Phi chốt | Làm ở |
| --- | --- | --- | --- |
| Q1 | Đổi tên cột theo ERD 19/10 bằng `V6`? | Không đổi. Sửa ERD theo database | #19 |
| Q2 | Thông tin người đi trên vé | Họ tên và số CCCD. Bỏ ngày sinh (`tickets.date_of_birth`) | #19, Đợt 3 |
| Q3 | Số điện thoại trên thẻ vé | Thêm cột `users.phone_number`, nhập khi đăng ký | #23 |
| Q4 | Phụ thu giường | Bỏ. Mọi chỗ cùng giá `trips.base_price` (bỏ `trip_seats.price_modifier`) | #19 |
| Q5 | Ô "Hành khách" ở trang chủ (D6) | Bỏ ô, chỉ sửa câu chữ của D6: số chỗ chọn là số vé, tối đa 10 | #26 |
| Q6 | ADR cho giao diện admin mới | Làm trong task khung admin, sau khi xem giao diện | #20 |

## Cách viết issue (Phi chốt 21/10/2026)

- Mỗi issue là một lát cắt dọc, đi từ database, service tới giao diện. Không tách issue theo
  tầng. Các task N, A, G, T, K, V, Q, H ở trên được gom lại thành issue.
- Nội dung issue theo mẫu Phi gửi:
  - Mô tả;
  - bảng Đợt / Độ khó / Branch / Cần xong trước;
  - ✅ Acceptance Criteria (AC1, AC2...);
  - 🔖 Sub-tasks chia theo giai đoạn, ghi đường dẫn file, tên class, tên method;
  - Không làm trong task này;
  - Tham khảo;
  - Khi nào coi là xong.
- Issue tự đủ để làm, không cần mở file hướng dẫn trong `huong-dan-task/`.
- Tạo theo từng đợt. Issue cũ #4-#16 đã đóng ngày 21/10/2026 theo lựa chọn của Phi, mỗi issue
  có bình luận chỉ sang issue thay thế hoặc đợt sẽ thay nó. Thẻ của chúng đã được archive
  khỏi board, để board không còn lẫn task cũ và mới.

| Issue | Tên | Gom các task |
| --- | --- | --- |
| #19 | Nền dữ liệu: vé chỉ ghi họ tên và CCCD, một giá cho mọi chỗ, cấu hình toa | N1, N4 |
| #20 | Khung giao diện admin mới | N2 |
| #21 | Khung giao diện client và trang lỗi | N3, N5 |
| #22 | Đăng nhập, phân quyền, đăng xuất | A1, A2, A4 |
| #23 | Đăng ký tài khoản và số điện thoại | A3 |
| #24 | Tab Tuyến: ma trận và danh sách tuyến | G1, phần tuyến của G2, G8, G9, G10 |
| #25 | Trang Tàu: lịch theo ngày, thêm và sửa tàu | T1, phần lịch của T2, T5 (chưa có nút đổi view), T6 |
| #26 | Tìm chuyến: trang chủ và trang Chọn chuyến | K1, K3, K4 |
| #27 | Đợt 2. Trang Ga và tuyến: sơ đồ dọc Bắc - Nam | phần ga và network của G2, G3, G4 |
| #28 | Đợt 2. Tạo chuyến: trình hướng dẫn nối chặng | phần gợi ý của T2, T3, T7 |
| #29 | Đợt 2. Chi tiết chuyến, hủy chuyến và trang Chuyến | T4, T12, T13 |
| #30 | Đợt 2. Chọn chỗ: sơ đồ toa và bản nháp booking | K5, K6, K7 |
| #31 | Đợt 2. Vé của tôi và hủy booking | V1, V2, V3, V4 |
| #32 | Đợt 3. Hành khách: thẻ vé ghi họ tên và CCCD | K8 |
| #33 | Đợt 3. Thanh toán mô phỏng: khóa chỗ, tạo booking và vé | K9, K10 |
| #34 | Đợt 3. Vé điện tử | K11 |
| #35 | Đợt 3. Booking và Người dùng phía admin | Q1, Q2, Q3 |
| #36 | Đợt 3. Trang Tàu: view Bảng tính và nhập nhiều chặng | T5 (nút đổi view), T10, T11, phần lập kế hoạch của T2 |
| #37 | Đợt 4. Trang Tổng quan | Q4 |
| #38 | Đợt 4. Chèn ga tự điền và gợi ý tuyến | G6, G7 |
| #39 | Đợt 4. Tạo nhiều tuyến một lần | G5, G11 |
| #40 | Đợt 4. Trình hướng dẫn: nhiều chặng tới một ga, tạo tuyến ngay khi chọn | T8, T9 |
| #41 | Đợt 4. Dải ngày có giá thấp nhất | K2 |
| #42 | Đợt 4. Hoàn thiện: kiểm thử xuyên suốt, dọn code, tài liệu | H1, H2, H3, H4 |

## Điều kiện chung để một task được coi là xong

1. `.\mvnw.cmd compile` không lỗi.
2. `.\mvnw.cmd spring-boot:run` khởi động được với MySQL và dữ liệu mẫu.
3. Đã đi hết mục "Xong khi" trên trình duyệt. Task giao diện thử thêm ở 390px.
4. Có một dòng `docs/WORKLOG.md` trong cùng commit. Pull request ghi `Closes #<số>` và được
   một người khác đọc qua.
