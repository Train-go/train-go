# Tiến độ ticket cá nhân và bàn giao phiên

Ngày cập nhật: 2026-10-23. Baseline: `origin/main`, commit `a138d97` sau fetch. Branch đang làm: `feat/auth-login`; nền trước thay đổi #22 là `803752a`, chứa #19 chưa merge.

## Trạng thái và nguồn

- Đã đọc issue trực tiếp bằng GitHub REST API: `thanhphong97` được assign 7 issue mở trong bảng bên dưới.
- Người dùng xác nhận ngày 23/10/2026: dùng `thanhphong97` cho TrainGo; `phong-vht` chỉ dùng cho thư mục work. Danh sách 7 issue bên dưới đúng tài khoản cần theo dõi. Connector dùng đúng `thanhphong97`; CLI hiện dùng `phong-vht`, không lấy danh tính CLI làm assignee cho dự án này.
- Project: https://github.com/orgs/Train-go/projects/1. Chưa đọc được board riêng tư vì CLI thiếu scope `read:project`; không suy diễn cột trạng thái từ trạng thái issue.
- Đã đọc `README.md`, `AGENTS.md`, `docs/WORKFLOW.md`, các plan active và bản chia issue trong `docs/session/plan-task-chi-tiet.md`. Issue #19–#42 thay thế task cũ #4–#16; không triển khai lại task cũ.
- Đây là file theo dõi duy nhất cho các phiên cá nhân; issue là nguồn AC, board là nguồn trạng thái nhóm. Không chép nguyên các hướng dẫn sang nhiều nơi.
- #19 đã triển khai và kiểm chứng local; xem checkpoint lịch sử. #22 đang triển khai phần server trên branch riêng, UI chờ #20/#21. Chưa push, mở PR hoặc đóng issue; các ticket còn lại chưa triển khai.

## Quyết định của người dùng

- GitHub cho TrainGo: `thanhphong97`. `phong-vht` dành cho thư mục work. Khi dùng CLI/API phải kiểm tra tài khoản thực thi; không tự đổi tài khoản toàn cục ảnh hưởng thư mục work.
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
| [#19](https://github.com/Train-go/train-go/issues/19) | Nền dữ liệu V6, một giá, CoachLayout | `feat/data-v6-coach-layout` | Không có | Đã kiểm chứng local; chưa PR |
| [#22](https://github.com/Train-go/train-go/issues/22) | Đăng nhập, phân quyền, đăng xuất | `feat/auth-login` | Server đã triển khai; UI chờ #20, #21 | Server kiểm chứng local; toàn issue chưa xong |
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

- Đã thêm V6 và `model/dto/CoachLayout.java`, bỏ hai field/getter cũ và cập nhật constructor entity; V1–V5 giữ nguyên.
- Tạo migration V6 mới, bỏ `tickets.date_of_birth`, bỏ CHECK trước khi bỏ `trip_seats.price_modifier`; không sửa V1–V5.
- Sửa `entity/Ticket.java`, `TripSeat.java`, comment giá trong `Trip.java`; thêm `CoachLayout` và constructor TripSeat nhận Seat.
- Cấu hình: toa 1–2 mỗi toa A01–A32 loại SEAT; toa 3–4 mỗi toa B01–B16 loại SLEEPER, 4 giường/khoang; tổng 96 chỗ.
- Đồng bộ ERD, `docs/product/domain-model.md`, `booking-flow.md` và phần giá trong RUNBOOK nếu có.
- Chấp nhận: nâng V5→V6 thành công, Hibernate validate qua; schema không còn hai cột; số booking/vé và ticket_price đã lưu không thay đổi; xác minh đủ 96 chỗ đúng khoang.

### #22 — đăng nhập

- Đã nối `users`/BCrypt, email không phân biệt hoa/thường; phân quyền ADMIN và CUSTOMER cho các nhóm trang, ADMIN cho API quản trị. API bị chặn trả JSON 401/403; CSRF vẫn bật.
- Đã sửa `repository/UserRepository.java`, thêm `TrainGoUserDetails`/service, encoder, success handler theo role và `CurrentUserModelAdvice`. Giữ login sinh tự động với trường `email`; `AuthController`/trang riêng và tích hợp shell chờ #20/#21.
- Gỡ ba cấu hình `spring.security.user.*`; phân quyền cả trang và API, giữ CSRF cho POST logout; GET API chưa đăng nhập trả 401 JSON thay vì HTML redirect. POST thiếu CSRF có thể bị 403 trước kiểm tra đăng nhập.
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
- [x] Người dùng xác nhận `thanhphong97` cho TrainGo; `phong-vht` chỉ dùng cho thư mục work (23/10/2026).
- [ ] Đọc cột Project khi có quyền `read:project`; issue state không thay thế trạng thái board.
- [x] Tạo branch `plan/assigned-issues-progress`; hồ sơ nằm trong commit checkpoint trên branch này, tra hash bằng Git history sau khi commit.
- [x] Tạo đủ 7 branch issue trong bảng ở local, ban đầu cùng xuất phát từ `a138d97`. Branch #19 đã fast-forward tới `ee3c012` để nhận hồ sơ rồi triển khai; sáu branch còn lại chưa có code/dependency mới. Mang commit hồ sơ mới nhất và dependency cần thiết sang trước khi bắt đầu.
- [x] Đọc lại issue #19 và bình luận: còn mở, assign thanhphong97, không có bình luận mới hoặc dependency.
- [x] Triển khai #19, compile, Flyway V6/Hibernate validate, SQL, probe Java, trình duyệt và review đều đạt; xem checkpoint chi tiết bên dưới.

## Kết quả #19 — 23/10/2026

- Phạm vi: V6 bỏ ngày sinh trên vé và modifier chỗ (bỏ CHECK trước); Ticket nhận issuedAt tường minh; TripSeat nhận CoachLayout.Seat; layout bất biến 4 toa/96 chỗ. Không tạo chuyến, thêm phone hoặc sửa V1–V5.
- AC1: `.\mvnw.cmd compile` PASS, 44 source; `.\mvnw.cmd spring-boot:run` khởi động ở 8080 trên MySQL80 8.0.46; Flyway V5→V6 SUCCESS và Hibernate validate qua.
- AC2: hai cột đã biến mất; 8 booking, 11 vé, 1.728 chỗ giữ nguyên. Mỗi vé (hash mọi trường giữ lại, giá) và tổng tiền từng booking khớp snapshot trước/sau; tổng giá vé vẫn 3.660.000đ.
- AC3: probe Java tạm PASS 4 toa/96 chỗ theo thứ tự, không trùng cặp toa+mã, đúng mọi khoang, list bất biến; locale ar-EG vẫn mã ASCII; TripSeat sao chép đúng và AVAILABLE; Ticket giữ issuedAt truyền vào và giá lịch sử độc lập giá chuyến hiện tại.
- AC4–AC5: sửa ERD Markdown/HTML đúng tên cột đang dùng, bỏ cột giả ở users; công thức giá vé mới bằng Trip.basePrice, chỉ thu họ tên+CCCD (tên mặc định từ tài khoản). Vé cũ không tính lại. Đã render/xem ERD, kiểm tra 26 bảng HTML và 8 box SVG không đè chữ.
- Thử tay bằng browser: `/` và `/admin/stations`, `/admin/routes`, `/admin/trains`, `/admin/trips`, `/admin/bookings`, `/admin/users`, mỗi trang ở 1440px và 390px: 14/14 trả 200, không pageerror, không tràn ngang. Trang booking có đúng 8 booking. Không thêm test tự động/dependency test.
- Sau kiểm chứng đã dừng đúng instance ứng dụng của phiên (PID 21048), cổng 8080 được trả lại; MySQL Server vẫn chạy. Maven kết thúc do chủ động dừng, không phải lỗi khởi động.
- Review: không còn finding; đối chiếu caller, chữ ký constructor, enum VARCHAR và phạm vi migration. `git diff --check` đạt.
- Bằng chứng và backup chỉ ở máy local, ngoài Git: `C:/Users/mrcaf/AppData/Local/Temp/traingo-issue19-proof/` chứa `before.sql`, snapshot trước/sau, `app-run.log`, `browser-results.json`, ảnh và `layout-result.txt`. Backup có dữ liệu riêng tư; không commit. File tạm có thể được hệ điều hành dọn.
- Phục hồi: database máy này đã ở V6; không chạy lại code cũ ánh xạ hai cột vừa bỏ. Nếu cần phục hồi dữ liệu đã bỏ, dùng backup ngoài repo sau khi cân nhắc dữ liệu phát sinh; không reset DB hoặc sửa lịch sử Flyway.
- Commit triển khai: `94b5bf990db7744e694c3c1c40f59a8a5522fd20` (`feat(data): simplify ticket identity and add fixed coach layout`). Chưa push/PR/review từ thành viên khác; GitHub issue còn mở. Không coi kiểm chứng local là đã merge.
- Tiếp theo: #22 có thể bắt đầu phần server; UI cần #20/#21. Khi chuyển branch, mang hồ sơ mới nhất sang và đọc lại issue. Vì DB local đã V6, branch chạy ứng dụng phải chứa thay đổi #19 tương ứng; không chạy entity cũ trên schema V6.

## Checkpoint #22 — 23/10/2026, phần server

- [x] Kiểm tra branch và fetch: `origin/main` vẫn `a138d97`; fast-forward `feat/auth-login` từ `a138d97` lên `803752a` để nhận hồ sơ mới nhất và code #19 (`94b5bf9`) tương thích schema V6 local. #19 chưa merge; diff #22 cần xét từ nền `803752a`, không coi mọi thay đổi so main là của #22.
- [x] Đọc lại issue/dependency: #22 còn mở, assign `thanhphong97`; #20/#21 còn mở, assign `VoAnhPhi`, chưa có remote branch tương ứng. Không nhận phần việc shell của người khác.
- [x] Triển khai repository ignore-case, principal/service đọc `users`, BCrypt, role trang/API, JSON 401/403 `{message, fields: {}}`, `currentUser`, success handler và gỡ tài khoản tạm.
- [x] Compile cuối PASS 48 source Java lúc 01:57:58, sau review. Ứng dụng khởi động bằng Maven trên MySQL80 8.0.46, Flyway V6/Hibernate validate qua; log `Started TrainGoApplication` sau 7,582 giây (PID 1192 của phiên); reload sau sửa đạt 1,561/0,904 giây.
- [x] Thử trình duyệt lượt sạch 53/53 PASS: tài khoản seed và email khác hoa/thường; email không tồn tại/mật khẩu sai cùng lỗi chung, `admin`/`admin` thất bại; role trang/API và JSON tiếng Việt 401/403 đúng hợp đồng.
- [x] Saved request `/my-trips?tab=upcoming` giữ nguyên URL sau login CUSTOMER và không bị GET API khách trả 401 ghi đè; ADMIN luôn về `/admin/trains`. Trang My Trips chưa có nên 404 sau redirect không phải bằng chứng hoàn thành trang.
- [x] Logout thiếu CSRF trả 403 và vẫn giữ đăng nhập; logout hợp lệ ở hai role về `/`, session không còn quyền truy cập trang bảo vệ. Chưa thay thế thử logout qua cả hai UI sau khi tích hợp shell.
- [x] Sáu lượt trang/viewport cho home, login, admin ở 1440px/390px không tràn ngang hoặc pageerror. Probe Java tạm đọc hai user thật từ MySQL: principal/advice PASS id, họ tên, email, role, authority; khách có `currentUser=null`.
- [x] Review phần server không finding; `git diff --check` đạt; docs và một dòng WORKLOG đi cùng code. Không thêm migration, thư mục test hoặc dependency test.
- [x] Dừng đúng instance TrainGo PID 1192 sau khi xác minh command line; cổng 8080 đã trả lại, MySQL80 vẫn Running.

Commit chứa checkpoint này có tiêu đề `feat(auth): implement database login and role authorization`;
tra hash bằng `git log -1 --format=%H --grep="feat(auth): implement database login and role authorization" feat/auth-login`.
Danh tính commit dùng `thanhphong97` với email GitHub noreply theo profile đã xác minh,
chỉ áp dụng cho lệnh commit, không đổi Git/CLI toàn cục. Chưa push/PR/merge;
toàn issue #22 vẫn chưa hoàn thành.

Bằng chứng local ngoài Git: `C:/Users/mrcaf/AppData/Local/Temp/traingo-issue22-proof/`
chứa `results.json`, ảnh và kết quả kiểm chứng. File tạm có thể được hệ điều hành
dọn; không commit dữ liệu riêng tư hoặc cấu hình database. Không thêm test tự
động, thư mục `app/src/test` hoặc dependency test.

Quyết định kỹ thuật trong phạm vi server: giữ login mặc định để có luồng đăng nhập
hoạt động trong khi chưa có shell, không khai `loginPage` khi chưa có controller
và template. Principal giữ dữ liệu cần cho template, tách khỏi persistence context.
Request cache chỉ lưu GET không phải API để polling không thay đích quay lại;
CUSTOMER giữ đúng URL/query gốc, không thêm tham số `continue`; ADMIN bỏ cache và
luôn về `/admin/trains`. CSRF giữ nguyên cho thao tác ghi/logout.

Giới hạn: lỗi tiếng Việt, giữ email khi sai, nút ẩn/hiện mật khẩu, redirect người
đã đăng nhập khỏi trang login, navbar khách và sidebar admin còn chờ #20/#21.
Header admin cũ vẫn tên cố định và form logout. Quy tắc public không tạo endpoint:
404 ở `/register`, `/trips` hoặc `/my-trips` chưa triển khai không chứng minh trang
đã hoàn thành. Saved redirect về My Trips chỉ chứng minh chuyển URL. Không làm
đăng ký/phone (#23) hoặc ownership booking trong checkpoint này.

## Tiếp tục session sau

Tiếp tục **#22 — Đăng nhập, phân quyền, đăng xuất** trên `feat/auth-login` với
phần server hiện có; UI phụ thuộc #20/#21. Đây là hồ sơ bền vững duy nhất; không
tạo phase plan, journal hoặc trạng thái Harness song song.

Đưa câu này cho agent: “Tiếp tục issue #22 cho thanhphong97. Vào đúng git root
D:/IUH/Documents/HK1-2026_2027/Web/project/train-go/train-go, đọc README.md,
AGENTS.md, docs/WORKFLOW.md và hồ sơ mới nhất trên feat/auth-login. Dùng MySQL
Server theo application.properties, không Docker. Đọc lại #22 và dependency
#20/#21; giữ code #19 để tương thích DB V6 local. Server đã triển khai, xem bằng
chứng và phần UI còn thiếu trong checkpoint. Sau kiểm chứng cập nhật tài liệu,
hồ sơ và docs/WORKLOG.md cùng commit. Không push/merge khi chưa được yêu cầu.”

Checklist nối phiên:

1. Kiểm tra working tree, HEAD và lịch sử hồ sơ trước khi sửa; giữ thay đổi người khác. Không fast-forward lại theo baseline cũ hoặc lùi schema.
2. Kiểm tra lại assignee, issue/PR và #20/#21. Chỉ tích hợp UI khi khung đã có; nếu chưa, ghi blocker thật. #19 vẫn là dependency branch chưa merge, đọc lại remote trước khi mở PR.
3. Khi shell sẵn sàng, thêm login riêng và hành vi UI theo AC #22, dùng `currentUser` ở navbar/sidebar; không làm đăng ký/phone hoặc nghiệp vụ ownership thuộc ticket sau.
4. Compile, chạy MySQL Server, thử khách/CUSTOMER/ADMIN, saved request, API, lỗi đăng nhập, CSRF/logout; UI 1440px và 390px, bàn phím. Không thêm test tự động theo ADR 0007.
5. Cập nhật ba tài liệu auth/RUNBOOK/hồ sơ và một dòng WORKLOG cùng commit trên branch issue. Chỉ đánh dấu hoàn thành toàn #22 khi cả server và UI có đủ bằng chứng; không dùng `Closes #22` cho checkpoint một phần.

## Giới hạn và câu hỏi còn mở

- Token thiếu `read:project`. Để đọc board bằng CLI, chủ tài khoản có thể chạy `gh auth refresh -s read:project` và hoàn tất xác thực; phiên này chưa thay quyền đăng nhập.
- `ck` và `npm` không tìm thấy trên PATH. Chưa tạo scaffold plan/phase bằng `ck plan create`; không giả lập trạng thái CLI. File này là hồ sơ tiến độ người dùng yêu cầu theo workflow repo.
- Không có công cụ CBM list_projects trong session; chưa kiểm tra hoặc tạo index. Chỉ đối chiếu file và Git trực tiếp.
