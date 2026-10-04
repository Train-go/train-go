# Execution Plan: Chia việc MVP TrainGo cho nhóm

Date: 2026-10-04

## Status

Active. Phi duyệt ngày 04/10/2026. Plan này thay cho
`docs/plans/completed/giai-doan-2-du-lieu-va-man-hinh-admin.md`: phần S của plan đó đã
xong, Task A và Task B của nó là T03 và T06 dưới đây.

Theo dõi tiến độ trên GitHub Projects của org `Voyage-GO`, mỗi task một issue. Nội dung
chi tiết vẫn nằm ở các file hướng dẫn trong repo.

## Outcome

Luồng MVP của `SPEC.md` mục 50 chạy thông trên ứng dụng thật, do ba thành viên làm
song song mà ít đụng file của nhau:

```
Admin: ga -> tuyến -> tàu -> toa + ghế -> chuyến
Khách: tìm chuyến -> chọn chuyến -> chọn toa, chỗ -> giữ chỗ -> hành khách
       -> xem lại -> thanh toán mô phỏng -> vé điện tử -> chuyến của tôi, hủy
```

Mỗi task có một file hướng dẫn trong `docs/plans/active/huong-dan-task/`, gồm mục tiêu,
bảng và quan hệ database dùng tới, các class và method cần viết ở từng tầng
Model, View, Controller, test bắt buộc và điều kiện xong.

## Context

- Đã có: database đủ 11 bảng và seed demo (ADR 0005), entity và repository cho mọi
  bảng, module Quản lý ga làm mẫu (`docs/session/huong-dan-mvc-quan-ly-ga.pdf`).
- Yêu cầu: `SPEC.md`, bản chắt lọc trong `docs/product/`.
- Quyết định đã chốt: `docs/decisions/0001` đến `0005`.
- Plan cha: `docs/plans/active/mvp-traingo.md`.

## Bản đồ phụ thuộc

```
Wave 1 (bắt đầu ngay, song song)
  T01 Hạ tầng test ──> T02 Auth ────────────────┐
  T03 Quản lý tàu    T04 Quản lý tuyến          │
  T05 Tìm chuyến ───────────────────────┐       │
  T06 Chi tiết tàu + sơ đồ toa ──┐      │       │
                                 │      │       │
Wave 2                           v      v       v
  T07 Quản lý chuyến      T08 Thêm toa (T06)   T09 Chọn chỗ, giữ chỗ
                                                (T02, T05, T06)
  T10 Chuyến của tôi, hủy booking (T02)
                                                │
Wave 3                                          v
  T13 Quản trị booking (T10)            T11 Hành khách, xem lại (T09)
                                                │
                                                v
                                        T12 Thanh toán, vé (T11)
Wave 4
  T14 Hoàn thiện, kiểm thử xuyên suốt (tất cả)
```

Đường găng (chuỗi dài nhất quyết định ngày xong):
`T02 -> T09 -> T11 -> T12 -> T14`. Mọi việc khác đều làm song song được bên cạnh
chuỗi này, nhờ seed data đã có sẵn ga, tuyến, tàu, chuyến và booking mẫu.

## Danh sách task

Cỡ việc ước lượng thô: S khoảng 1 ngày, M 2-3 ngày, L 4-5 ngày.

| ID | Task | Wave | Lane | Cỡ | Phụ thuộc | Hướng dẫn |
| --- | --- | --- | --- | --- | --- | --- |
| T01 | Hạ tầng test | 1 | B | S | Không | `T01-ha-tang-test.md` |
| T02 | Đăng ký, đăng nhập, phân quyền | 1 | C | L | T01 (mềm) | `T02-dang-ky-dang-nhap-phan-quyen.md` |
| T03 | Quản lý tàu | 1 | A | S | Không | `T03-quan-ly-tau.md` |
| T04 | Quản lý tuyến | 1 | A | M | D2 | `T04-quan-ly-tuyen.md` |
| T05 | Tìm chuyến, danh sách, chi tiết chuyến | 1 | B | M | T01 (bean `Clock`), D6, D8 | `T05-tim-chuyen.md` |
| T06 | Chi tiết tàu và sơ đồ toa | 1 | B | M | Không | `T06-chi-tiet-tau-so-do-toa.md` |
| T07 | Quản lý chuyến | 2 | A | M | D2, D8 | `T07-quan-ly-chuyen.md` |
| T08 | Thêm toa vào tàu | 2 | C | M | T06, D3 | `T08-them-toa.md` |
| T09 | Chọn chỗ và giữ chỗ | 2 | B | L | T02, T05, T06, D1, D6, D7 | `T09-chon-cho-giu-cho.md` |
| T10 | Chuyến của tôi và hủy booking | 2 | C | M | T02, D8 | `T10-chuyen-cua-toi-huy-booking.md` |
| T11 | Hành khách và xem lại booking | 3 | B | M | T09, D5, D7 | `T11-hanh-khach-xem-lai.md` |
| T12 | Thanh toán mô phỏng và vé điện tử | 3 | C | L | T11, D5, D7 | `T12-thanh-toan-ve-dien-tu.md` |
| T13 | Quản trị booking, người dùng, dashboard | 3 | A | M | T10 | `T13-quan-tri-booking-nguoi-dung.md` |
| T14 | Hoàn thiện và kiểm thử xuyên suốt | 4 | Cả nhóm | M | Tất cả | `T14-hoan-thien.md` |

"Mềm" nghĩa là làm được trước, nhưng có task kia thì dễ hơn.

## Chia người theo lane

Mỗi lane là một chuỗi task nối tiếp, cùng một người làm từ đầu tới cuối, để người
đó hiểu sâu phần mình và ít phải đọc code người khác. Nhóm có một thành viên mới và
hai thành viên đã vững backend, nên lane A nhẹ hơn hẳn, còn lane B và C ngang nhau.
Phi gán tên sau:

| Lane | Phụ trách | Chuỗi task | Khối lượng | Dành cho |
| --- | --- | --- | --- | --- |
| A | Màn hình quản trị dữ liệu | T03 -> T04 -> T07 -> T13 | Khoảng 8-9 ngày | Thành viên mới: mọi task là CRUD và danh sách theo đúng mẫu module ga, độ khó tăng dần |
| B | Luồng tìm và chọn chỗ của khách | T01 -> T05 -> T06 -> T09 -> T11 | Khoảng 13 ngày | Một thành viên vững: JavaScript `fetch`, giữ chỗ chống trùng |
| C | Tài khoản và lõi booking | T02 -> T08 -> T10 -> T12 | Khoảng 14 ngày | Một thành viên vững: Spring Security, transaction nhiều bảng |

T14 chia đều khi tới wave 4.

Ba lane ở wave 1 không chặn nhau. Ở wave 2, lane B cần T02 của lane C xong mới bắt đầu
T09; lane C cần T06 của lane B xong mới bắt đầu T08. Ở wave 3, lane C cần T11 của lane
B xong mới bắt đầu T12; trong lúc chờ, lane C dựng trước trang vé và trang lỗi của T14.

Người làm lane A nên được một người lane B hoặc C đọc pull request của từng task, vì
đây là chỗ học nhanh nhất: so code của mình với module ga và với góp ý cụ thể.

## Quyết định cần Phi chốt

Mỗi quyết định đều có đề xuất. Cột "Cần trước" là task đầu tiên bị chặn nếu chưa
chốt. Chốt xong thì ghi vào mục Decisions bên dưới và, nếu là luật sản phẩm, vào
`docs/product/`.

| ID | Câu hỏi | Đề xuất | Cần trước |
| --- | --- | --- | --- |
| D1 | Chỗ hết thời gian giữ được trả lại bằng cách nào? | Kiểm tra lười: chỗ `HELD` có `heldUntil` đã qua được coi là trống ở mọi query và được phép giữ đè. Job định kỳ dọn dẹp là tùy chọn, không bắt buộc | T09 |
| D2 | Xóa hoặc hủy dữ liệu đang được dùng? | Tuyến: chặn xóa khi có chuyến dùng; tuyến đã có chuyến chỉ sửa được khoảng cách, không đổi ga. Tàu: không có xóa. Chuyến: không xóa, chỉ hủy (`CANCELLED`), và chặn hủy khi chuyến đã có booking `CONFIRMED` | T04, T07 |
| D3 | Thêm toa vào tàu đã có chuyến thì sao? | Sinh thêm `TripSeat` `AVAILABLE` cho mọi chuyến `SCHEDULED` chưa khởi hành của tàu đó, trong cùng transaction | T08 |
| D4 | Đăng ký cần gì? | Họ tên, email, mật khẩu, nhập lại mật khẩu. Mật khẩu ít nhất 8 ký tự. Email không trùng, không phân biệt hoa thường | T02 |
| D5 | Mã booking và mã vé sinh thế nào? | Booking `BK` + năm tháng + số thứ tự 3 chữ số, lấy số tiếp theo của tháng đó (`BK202610005`). Vé: mã booking đổi `BK` thành `TK` + `-` + thứ tự hành khách (`TK202610005-01`), giống seed | T11 |
| D6 | Mỗi booking tối đa mấy hành khách? | 1 đến 10, khớp ô "Hành khách" đang có ở landing | T05 |
| D7 | Dữ liệu giữa các bước đặt vé nằm ở đâu? | Bấm "Tiếp tục" ở trang chọn chỗ thì giữ chỗ và lưu một bản nháp booking trong session. Bước hành khách và xem lại chỉ sửa bản nháp. Bấm "Tiếp tục thanh toán" mới tạo booking `PENDING` trong database. Bấm "Thanh toán" thì xác nhận | T09 |
| D8 | Khi nào một chuyến coi là đã khởi hành hoặc hoàn thành? | Mọi luật đặt, giữ, hủy đều so thời điểm khởi hành (ngày + giờ) với hiện tại. Trạng thái `COMPLETED` chỉ để hiển thị; gán bằng một job chạy mỗi giờ là tùy chọn | T05 |

## Quy ước chung cho mọi task

- **Branch**: `feat/t03-train-admin`, `feat/t09-seat-holding`... Tiếng Anh, không dấu.
  Một task một branch, merge vào `main` qua pull request.
- **Mẫu code**: làm theo module ga (`docs/session/huong-dan-mvc-quan-ly-ga.pdf`).
  Màn hình admin của feature nào nằm trong package feature đó (ADR 0001).
- **Entity**: được thêm method nghiệp vụ (ví dụ `Trip.cancel()`), không được thêm
  hay sửa field ánh xạ cột. Đổi schema phải bàn với nhóm và viết migration mới
  `V3__...`; không sửa `V1`, `V2`.
- **Thời gian**: mọi chỗ cần "bây giờ" lấy từ bean `java.time.Clock` (T01 tạo), không
  gọi thẳng `LocalDateTime.now()`. Nhờ vậy test giả được thời điểm hết hạn giữ chỗ.
- **File dùng chung**, chỉ chủ sở hữu được sửa, người khác nhờ chủ sở hữu:

| File | Chủ sở hữu |
| --- | --- |
| `common/config/TimeConfig.java` (bean `Clock`) | T01 |
| `common/config/SecurityConfig.java` | T02 (khai sẵn luật cho mọi URL tương lai) |
| `templates/fragments/navbar.html`, `templates/admin/fragments/header.html` | T02 |
| `auth/service/TrainGoUserDetails.java` (người đang đăng nhập) | T02 |
| `train/service/TrainNotFoundException.java` | T03; T06 dùng lại |
| `static/js/seat-map.js` (hàm vẽ sơ đồ toa) | T06, T09 mở rộng |
| `booking/model/BookingDraft.java`, `booking/service/BookingDraftStore.java`, `static/js/hold-countdown.js` | T09, T11 mở rộng |
| `booking/controller/BookingFlowController.java` | T11, T12 thêm handler |
| `booking/service/BookingCancellationService.java` | T10, T13 dùng lại |
| Method `Trip.departureAt()` | Task nào làm trước thì thêm (T05 hoặc T07) |
| `admin/controller/AdminController.java`, `admin/service/AdminService.java` | Ai chuyển màn hình nào ra package riêng thì gỡ đúng phần của màn hình đó |
| `docs/WORKLOG.md` | Mọi task thêm một dòng; conflict thì giữ cả hai dòng |

- **Xong một task** khi: chạy thật trên trình duyệt với MySQL và seed, `.\mvnw.cmd test`
  xanh với các test bắt buộc của task, có dòng `docs/WORKLOG.md` trong cùng commit,
  và pull request được một người khác đọc qua.

## Theo dõi tiến độ

Đề xuất: **dùng GitHub Projects**, mỗi task một issue, thay vì Google Sheet. Lý do:

- Nhóm đã làm việc qua GitHub và pull request. Issue gắn với branch và PR; PR ghi
  `Closes #12` là issue tự đóng, cột trạng thái tự chuyển.
- Mỗi issue chỉ cần tiêu đề, người làm, wave và link tới file hướng dẫn trong repo.
  Nội dung chi tiết vẫn nằm trong repo, đi cùng code.

Google Sheet hợp lý nếu giảng viên cần một bảng để chấm, hoặc có thành viên ít dùng
GitHub. Khi đó chỉ ghi các cột ID, Task, Wave, Lane, Người làm, Trạng thái, Branch/PR,
Hạn, Link hướng dẫn. Không chép nội dung hướng dẫn sang Sheet, vì hai bản sẽ lệch
nhau ngay khi một bên được sửa.

## Risks And Recovery

- Đường găng dồn vào lane C (T02, T12). T02 trễ là T09, T11, T12 trễ theo. Phòng: T02
  bắt đầu ngày đầu tiên; lane B làm T05, T06 trước, không đợi.
- D7 là hợp đồng giữa T09, T11, T12. Chốt muộn là ba task viết ba kiểu khác nhau.
  Phòng: chốt D1, D7 trước khi wave 2 bắt đầu.
- Hai người cùng sửa một entity (ví dụ `TripSeat` ở T09 và T12). Phòng: mỗi task chỉ
  thêm method của mình; conflict khi merge chỉ là hai method khác nhau, giữ cả hai.
- Hạ tầng test bị xóa trong working tree (`app/src/test`). Phòng: T01 dựng lại cấu
  hình test; nếu nhóm quyết định bỏ test thì phải sửa `AGENTS.md` và ADR 0002 cho
  khớp, không để quy tắc và thực tế lệch nhau.

## Progress

- [x] Database, seed, module Quản lý ga (01/10/2026, commit `9748dcd`).
- [x] Bản nháp chia việc và 14 file hướng dẫn (04/10/2026).
- [x] Phi duyệt cách chia: lane A cho thành viên mới, lane B và C chia đều (04/10/2026).
- [ ] Tạo GitHub Project và 14 issue.
- [ ] Gán người cho ba lane.
- [ ] Chốt D1-D8.
- [ ] Wave 1: T01-T06.
- [ ] Wave 2: T07-T10.
- [ ] Wave 3: T11-T13.
- [ ] Wave 4: T14.

## Decisions

- 2026-10-04: Duyệt 14 task, 4 wave, 3 lane như trên. Lane A dành cho thành viên mới
  (chỉ CRUD và danh sách theo mẫu module ga); lane B và C khối lượng tương đương cho hai
  thành viên đã vững backend. Chưa gán tên.
- 2026-10-04: Theo dõi bằng GitHub Projects của org `Voyage-GO`, mỗi task một issue
  thật trong repo `voyage-go`.
- D1-D8: chưa chốt. Ghi vào đây khi chốt, kèm ngày.

## Validation

- Mỗi task: điều kiện xong ở cuối file hướng dẫn của task đó.
- Toàn MVP: T14 chạy trọn luồng mục Outcome trên trình duyệt với MySQL, cộng một test
  `@SpringBootTest` cho luồng đặt vé.

## Result

Chưa bắt đầu.
