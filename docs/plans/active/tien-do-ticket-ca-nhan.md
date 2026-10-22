# Tiến độ ticket cá nhân và bàn giao phiên

Ngày cập nhật: 2026-10-23. Baseline: `main`, commit `a138d97`.

## Trạng thái và nguồn

- Đã đọc issue trực tiếp bằng GitHub REST API: `thanhphong97` được assign 7 issue mở trong bảng bên dưới.
- Connector dùng `thanhphong97`; CLI dùng `phong-vht` và tài khoản CLI không có issue được assign trong repo. **Chờ người dùng xác nhận username**; danh sách hiện là ứng viên theo tài khoản connector, chưa khẳng định là tài khoản người dùng.
- Project: https://github.com/orgs/Train-go/projects/1. Chưa đọc được board riêng tư vì CLI thiếu scope `read:project`; không suy diễn cột trạng thái từ trạng thái issue.
- Đã đọc `README.md`, `AGENTS.md`, `docs/WORKFLOW.md`, các plan active và bản chia issue trong `docs/session/plan-task-chi-tiet.md`. Issue #19–#42 thay thế task cũ #4–#16; không triển khai lại task cũ.
- Đây là file theo dõi duy nhất cho các phiên cá nhân; issue là nguồn AC, board là nguồn trạng thái nhóm. Không chép nguyên các hướng dẫn sang nhiều nơi.
- Chưa triển khai code, chưa compile, chưa khởi động ứng dụng và chưa thử nghiệp vụ trong phiên lập hồ sơ này.

## Quyết định của người dùng

- Dùng **MySQL Server**, không dùng Docker để chạy hoặc kiểm chứng.
- Mỗi issue một branch riêng; hoàn thành phần việc đã kiểm chứng thì commit. Commit code và dòng `docs/WORKLOG.md` tương ứng cùng nhau. Chưa có yêu cầu push hoặc merge.
- Lưu tiến độ vào file để session tiếp theo đọc và tiếp tục.

## Môi trường đã kiểm tra

Nguồn: `app/src/main/resources/application.properties` đọc ngày 23/10/2026.

| Thành phần | Giá trị / kết quả quan sát |
| --- | --- |
| Ứng dụng | `server.port=8080` |
| JDBC | `jdbc:mysql://localhost:3306/traingo?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Ho_Chi_Minh` |
| Hibernate | `spring.jpa.hibernate.ddl-auto=validate` |
| Thông tin đăng nhập DB | Đã cấu hình trong properties; không sao chép vào hồ sơ |
| Windows service | `MySQL80`: `Running` |
| TCP localhost:3306 | Kết nối được |

Service chạy và TCP mở chưa chứng minh ứng dụng đăng nhập được database hoặc Flyway chạy thành công.
Khi triển khai, chạy từ `app/`: `.\mvnw.cmd compile`, sau đó `.\mvnw.cmd spring-boot:run` với MySQL Server hiện tại.
Không chạy lệnh Docker, không xóa database hoặc volume để xử lý lỗi migration.

## Danh sách và thứ tự đề xuất

Trạng thái thực hiện bên dưới là hồ sơ phiên, **không phải cột Project**. Cả 7 issue đang `open` tại lúc đọc API.

| Issue | Phạm vi | Branch theo issue | Phụ thuộc / khả năng bắt đầu | Tiến độ |
| --- | --- | --- | --- | --- |
| [#19](https://github.com/Train-go/train-go/issues/19) | Nền dữ liệu V6, một giá, CoachLayout | `feat/data-v6-coach-layout` | Không có; đề xuất làm đầu tiên | Chưa làm |
| [#22](https://github.com/Train-go/train-go/issues/22) | Đăng nhập, phân quyền, đăng xuất | `feat/auth-login` | Server làm ngay; UI chờ #20, #21 | Chưa làm |
| [#23](https://github.com/Train-go/train-go/issues/23) | Đăng ký và số điện thoại | `feat/auth-register` | #19 và #22; exception dùng quy ước #21 | Chờ phụ thuộc |
| [#24](https://github.com/Train-go/train-go/issues/24) | Ma trận và danh sách tuyến | `feat/route-admin` | Server làm ngay; UI chờ #20, #21 | Chưa làm |
| [#27](https://github.com/Train-go/train-go/issues/27) | Sơ đồ dọc Ga và tuyến | `feat/station-network-map` | #20, #21, #24 | Chờ phụ thuộc |
| [#35](https://github.com/Train-go/train-go/issues/35) | Booking và Người dùng admin | `feat/admin-bookings-users` | #20, #29, #31; cần phone từ #23 cho chi tiết khách | Chờ phụ thuộc |
| [#36](https://github.com/Train-go/train-go/issues/36) | Bảng tính tàu và nhập nhiều chặng | `feat/train-sheet-view` | #28, #29; JS dùng network API #27; kế thừa trang tàu #25 | Chờ phụ thuộc |

Thứ tự: #19 → phần server #22 → #23 khi dependency sẵn; #24 → #27 là nhánh công việc khác; #35 và #36 chỉ tích hợp khi có module dùng chung.
#19 không phải dependency bắt buộc của server #22; chọn làm trước để gỡ nền dữ liệu cho nhóm.
Đọc lại assignee và dependency trước mỗi lần bắt đầu; không nhận thêm ticket người khác chỉ vì nó đang chặn mình.

## Điểm bắt đầu và bằng chứng cần thu

Các đường dẫn Java bên dưới tính từ `app/src/main/java/com/voyagego/traingo/`; template và JS tính từ `app/src/main/resources/`. File mới là dự kiến theo issue, phải đối chiếu lại sau khi dependency merge.

### #19 — dữ liệu và cấu hình toa

- Hiện migration chỉ tới `V5__north_south_line.sql`, chưa có `model/dto/CoachLayout.java`.
- Tạo migration V6 mới, bỏ `tickets.date_of_birth`, bỏ CHECK trước khi bỏ `trip_seats.price_modifier`; không sửa V1–V5.
- Sửa `entity/Ticket.java`, `TripSeat.java`, comment giá trong `Trip.java`; thêm `CoachLayout` và constructor TripSeat nhận Seat.
- Cấu hình: toa 1–2 mỗi toa A01–A32 loại SEAT; toa 3–4 mỗi toa B01–B16 loại SLEEPER, 4 giường/khoang; tổng 96 chỗ.
- Đồng bộ ERD, `docs/product/domain-model.md`, `booking-flow.md` và phần giá trong RUNBOOK nếu có.
- Chấp nhận: nâng V5→V6 thành công, Hibernate validate qua; schema không còn hai cột; số booking/vé và ticket_price đã lưu không thay đổi; xác minh đủ 96 chỗ đúng khoang.

### #22 — đăng nhập

- `common/config/SecurityConfig.java` hiện dùng form login mặc định; `/admin/**` mới yêu cầu authenticated, chưa kiểm tra role ADMIN.
- Sửa `repository/UserRepository.java`; thêm UserDetails/UserDetailsService, BCrypt encoder, success handler theo role, `CurrentUserModelAdvice`, `AuthController` và trang login.
- Gỡ ba cấu hình `spring.security.user.*`; phân quyền cả trang và API, giữ CSRF cho POST logout; API chưa đăng nhập trả 401 thay vì HTML redirect.
- Chấp nhận: email khác hoa thường đăng nhập được; sai email/mật khẩu cùng câu lỗi; khách không vào admin, admin không vào trang dành CUSTOMER; saved request quay về đúng trang; logout ở cả hai khu.

### #23 — đăng ký

- `entity/User.java` hiện chưa có phoneNumber; thêm migration V7 sau V6, không dùng lại số migration đã có.
- Thêm `RegisterRequest`, `UserService`, xử lý email trùng, controller và template đăng ký; cập nhật auth và ERD.
- Luật theo issue: số điện thoại 10 chữ số bắt đầu 0; tên tối đa 100 ký tự; mật khẩu ít nhất 8 ký tự và xác nhận khớp; email lưu chữ thường, không trùng không phân biệt hoa thường; role CUSTOMER, hash BCrypt, tự đăng nhập.
- Chấp nhận: thử từng lỗi riêng, hai yêu cầu đăng ký trùng email, giữ form trừ mật khẩu; SQL kiểm tra tài khoản mới và phone của 11 khách mẫu; admin được để phone trống.

### #24 — tuyến

- Hiện tuyến còn được đọc qua `controller/AdminController.java` và `service/AdminService.java`; chưa có RouteService riêng.
- Tạo request/response, `RouteService`, controller trang/API và exception theo #21; mở rộng Route/Trip repository; tách phần đọc tuyến khỏi AdminController/AdminService.
- UI: viết lại `templates/admin/routes.html`; thêm fragment `route-matrix.html`, `static/js/admin/route-matrix.js`, `routes.js`. Ma trận dùng lại ở #27; không chép React từ mẫu vào ứng dụng.
- Chấp nhận: tạo hai chiều và hoàn tác đúng id; chặn trùng cặp, ga đi=ga đến, km không dương; tuyến có chuyến DELETE trả 409; tìm không dấu; phím mũi tên/Enter/Esc và focus; 390px không tràn trang. Tiêu đề issue cho phép cân nhắc đổi UI: giữ AC cho đến khi có quyết định cụ thể.

### #27 — mạng lưới ga

- Tận dụng `StationService`, `StationAdminController`, Station/Route repository; dùng lại RouteService và ma trận từ #24.
- Đọc đầy đủ sub-task trong issue trước khi tạo network API, sơ đồ và ngăn ga/cặp ga/chèn ga.
- Chấp nhận: đủ 8 ga, khoảng cách tối thiểu 60px, tuyến dài có cung; mọi đối tượng dùng bàn phím được; sửa/xóa đúng ràng buộc, chèn ga lấy km chưa dùng; 1440px cạnh nhau, dưới 1100px xếp dọc, 390px không tràn.

### #35 — admin booking và người dùng

- Hiện có danh sách đọc qua AdminController/AdminService và DTO AdminBooking/AdminUser; không coi danh sách cũ là hoàn thành issue mới.
- Tạo BookingAdminService/UserAdminService, controller/API và response cần thiết; sửa booking/user template, thêm bookings.js; chỉ gỡ DTO cũ khi hết nơi dùng, giữ số tổng cho #37.
- Dùng đúng BookingCancellationService từ #31 và ngăn chi tiết chuyến từ #29; không tự viết lại nghiệp vụ hủy.
- Chấp nhận: 3 nhóm và số đếm khớp SQL, tìm không dấu và tên người đi, deep link `?open=`, lọc `?customer=`, che CCCD còn 4 số cuối, hủy trả chỗ và chặn chuyến đã đi bằng 409; người dùng chỉ xem.

### #36 — bảng tính tàu

- Mở rộng TrainScheduleService/API của #25/#28; thêm PlannedLeg, adjacentPath, planLegs và API lập kế hoạch; dùng API tạo lô của #28.
- Tách day/sheet fragment của trang tàu, thêm train-sheet.js; lưu view bằng localStorage có try/catch, URL `?view=bang`.
- Chấp nhận: dòng sau nối ga và giờ từ dòng trước, không ghi đè ô đã sửa tay; điền Huế→Sài Gòn đúng 3 chặng; lỗi gắn đúng dòng đã gửi; lô sai không tạo chuyến nào; hai chặng tạo 192 TripSeat; giữ view khi đổi tàu; thử 1440/390px.

## Quy trình branch, commit và cập nhật

1. Kiểm tra `git status`, HEAD, assignee và nhánh có sẵn trước khi chuyển nhánh. Không ghi đè hoặc reset công việc khác.
2. Branch hồ sơ: `plan/assigned-issues-progress`. Mỗi issue dùng branch riêng trong bảng; không gom nhiều issue vào một branch.
3. Trước khi bắt đầu branch issue, đồng bộ commit hồ sơ mới nhất và các dependency đã merge bằng thao tác Git phù hợp; không giả định branch đã chứa dependency chỉ vì đã tồn tại.
4. Chưa sửa sản phẩm thì chỉ commit hồ sơ; không dùng `Closes #...` cho commit kế hoạch.
5. Mỗi issue: ghi checklist AC, sửa code, compile, chạy ứng dụng với MySQL Server, thử tay đúng AC, cập nhật dòng tiến độ và WORKLOG, kiểm tra diff/lint phù hợp, commit Conventional Commits trên branch issue.
6. Ghi hash commit, PR nếu có, bằng chứng, blocker và bước kế tiếp ở checkpoint dưới. Chỉ ghi hoàn thành khi đủ bằng chứng. Không push/merge nếu chưa có chỉ dẫn.
7. Hồ sơ trên các branch có thể lệch nhau: khi nối phiên, xác định commit hồ sơ mới nhất qua `git log --all -- docs/plans/active/tien-do-ticket-ca-nhan.md` và mang bản mới nhất sang branch đang làm; không tạo bản sao thứ hai.

## Kiểm chứng bắt buộc

- ADR 0007: không thêm test tự động, `app/src/test` hoặc dependency test.
- Mỗi issue cần kết quả thật của compile, ứng dụng khởi động với MySQL Server và kịch bản thử tay trong issue; ghi riêng từng mức, không dùng kế hoạch thay bằng chứng.
- UI thử 1440px và 390px cùng bàn phím; nghiệp vụ hủy/đặt chỗ, transaction, role và ownership phải thử cả nhánh lỗi liên quan.
- Migration chạy trên database hiện tại có dữ liệu; không xóa dữ liệu để che lỗi. Đọc RUNBOOK trước khi chạy và chỉ dừng process do phiên mình khởi tạo.
- Một dòng WORKLOG cho mỗi task, cùng commit; PR khi được yêu cầu cần review và `Closes #<issue>`.

## Checkpoint phiên 2026-10-23

- [x] Đọc repo và issue; xác định danh sách 7 ticket của connector và các dependency.
- [x] Kiểm tra properties, service MySQL80 và TCP 3306.
- [ ] Xác nhận username người dùng.
- [ ] Đọc cột Project khi có quyền `read:project`; issue state không thay thế trạng thái board.
- [x] Tạo branch `plan/assigned-issues-progress`; hồ sơ nằm trong commit checkpoint trên branch này, tra hash bằng Git history sau khi commit.
- [x] Tạo đủ 7 branch issue trong bảng ở local, cùng xuất phát từ `a138d97`; chưa có code hoặc dependency mới trên các branch này. Mang commit hồ sơ mới nhất và dependency cần thiết sang trước khi bắt đầu; kiểm tra danh tính trước khi triển khai.
- [ ] Bắt đầu #19 sau khi xác nhận phạm vi tài khoản; chưa có issue nào được triển khai trong phiên này.

## Tiếp tục session sau

Đưa câu này cho agent: “Đọc `docs/plans/active/tien-do-ticket-ca-nhan.md`, kiểm tra commit hồ sơ mới nhất trên các branch, cập nhật issue/assignee/dependency rồi tiếp tục ticket đầu tiên đủ điều kiện. Dùng MySQL Server theo application.properties, không Docker. Mỗi issue một branch, kiểm chứng rồi commit.”

Lần tiếp theo: vào đúng git root `D:/IUH/Documents/HK1-2026_2027/Web/project/train-go/train-go`, đọc README/AGENTS/WORKFLOW và hồ sơ; giữ thay đổi người khác; xác nhận username còn thiếu; truy vấn lại issue; kiểm tra runtime rồi làm #19 nếu vẫn được giao và chưa có người triển khai.

## Giới hạn và câu hỏi còn mở

- Username chưa xác nhận: `thanhphong97` hay tài khoản khác? CLI `phong-vht` không có assignment.
- Token thiếu `read:project`. Để đọc board bằng CLI, chủ tài khoản có thể chạy `gh auth refresh -s read:project` và hoàn tất xác thực; phiên này chưa thay quyền đăng nhập.
- `ck` và `npm` không tìm thấy trên PATH. Chưa tạo scaffold plan/phase bằng `ck plan create`; không giả lập trạng thái CLI. File này là hồ sơ tiến độ người dùng yêu cầu theo workflow repo.
- Không có công cụ CBM list_projects trong session; chưa kiểm tra hoặc tạo index. Chỉ đối chiếu file và Git trực tiếp.
