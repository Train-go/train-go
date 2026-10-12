# Execution Plan: Chia việc MVP TrainGo cho nhóm

Date: 2026-10-04

## Status

Đang thực hiện (Active). Phi duyệt ngày 04/10/2026. Plan này thay cho
`docs/plans/completed/giai-doan-2-du-lieu-va-man-hinh-admin.md`: phần database và module
ga của plan đó đã xong, hai task còn lại của nó trở thành T03 và T06 dưới đây.

Tiến độ theo dõi trên board "TrainGo MVP": https://github.com/orgs/Train-go/projects/1.
Mỗi task là một issue trong repo `Train-go/train-go` (cột Issue ở bảng dưới). Tám quyết
định D1-D8 theo dõi ở issue #2. Nội dung chi tiết nằm trong các file hướng dẫn ở repo;
issue chỉ link tới đó.

Người mới nên đọc `docs/plans/active/huong-dan-task/README.md` trước: file đó giải thích
các từ dùng trong plan này (đợt, phần việc, độ khó...) và các bước làm một task.

## Outcome

Luồng MVP của `SPEC.md` mục 50 chạy được từ đầu tới cuối trên ứng dụng thật. Ba thành
viên làm song song và ít phải sửa chung file:

```
Admin: ga -> tuyến -> tàu -> toa + ghế -> chuyến
Khách: tìm chuyến -> chọn chuyến -> chọn toa, chỗ -> giữ chỗ -> hành khách
       -> xem lại -> thanh toán mô phỏng -> vé điện tử -> chuyến của tôi, hủy
```

Mỗi task có một file hướng dẫn trong `docs/plans/active/huong-dan-task/`: task làm gì,
dùng bảng nào trong database, viết class và method nào ở từng tầng, kịch bản tự thử bằng
tay, và cách tự kiểm tra trước khi báo xong.

## Context

- Đã có: database đủ 11 bảng và dữ liệu mẫu (ADR 0005, migration V1-V3), entity và
  repository cho mọi bảng, module Quản lý ga làm mẫu
  (`docs/session/huong-dan-mvc-quan-ly-ga.pdf`). Danh sách tàu, tuyến, chuyến, booking,
  người dùng và dashboard admin đã đọc từ database qua `AdminService` (commit `9ba771b`).
- Code chia package theo tầng: `controller`, `service`, `repository`, `entity`, `model`,
  `common` (ADR 0006).
- Dự án không có test tự động; mỗi task tự thử bằng tay theo kịch bản trong hướng dẫn
  (ADR 0007).
- Yêu cầu gốc: `SPEC.md`, bản tóm tắt trong `docs/product/`.
- Quyết định kỹ thuật đã chốt: `docs/decisions/0001` đến `0007` (ADR 0002 đã được ADR
  0007 thay thế).
- Plan tổng của MVP: `docs/plans/active/mvp-traingo.md`.

## Task nào làm trước, task nào làm sau

Task chia thành bốn **đợt**. Task trong cùng một đợt làm song song được. Task ở đợt sau
phải đợi những task nó cần làm xong.

```
Đợt 1 (bắt đầu ngay, làm song song)
  T02 Đăng nhập ────────────────────────────────┐
  T03 Quản lý tàu    T04 Quản lý tuyến          │
  T05 Tìm chuyến ───────────────────────┐       │
  T06 Chi tiết tàu + sơ đồ toa ──┐      │       │
                                 │      │       │
Đợt 2                            v      v       v
  T07 Quản lý chuyến      T08 Thêm toa (T06)   T09 Chọn chỗ, giữ chỗ
                                                (T02, T05, T06)
  T10 Chuyến của tôi, hủy booking (T02)
                                                │
Đợt 3                                           v
  T13 Quản trị booking (T10)            T11 Hành khách, xem lại (T09)
                                                │
                                                v
                                        T12 Thanh toán, vé (T11)
Đợt 4
  T14 Hoàn thiện, kiểm thử xuyên suốt (tất cả)
```

Chuỗi việc dài nhất là `T02 -> T09 -> T11 -> T12 -> T14`. Chuỗi này quyết định ngày cả
nhóm xong, nên phải bắt đầu ngay và không được để trễ. Các task khác làm song song bên
cạnh được, vì dữ liệu mẫu đã có sẵn ga, tuyến, tàu, chuyến và booking.

## Danh sách task

Độ khó là ước lượng để chia việc cho đều, không phải hạn chót: Dễ khoảng 1 ngày, Vừa
khoảng 2-3 ngày, Khó khoảng 4-5 ngày.

T01 (Hạ tầng test, issue #3) đã bỏ ngày 12/10/2026 cùng với test tự động (ADR 0007).
Số thứ tự các task còn lại giữ nguyên để khớp với issue và tên branch.

| ID | Task | Đợt | Phần việc | Độ khó | Cần xong trước | Hướng dẫn | Issue |
| --- | --- | --- | --- | --- | --- | --- | --- |
| T02 | Đăng ký, đăng nhập, phân quyền | 1 | C | Khó | Quyết định D4 | `T02-dang-ky-dang-nhap-phan-quyen.md` | #4 |
| T03 | Quản lý tàu | 1 | A | Dễ | Không | `T03-quan-ly-tau.md` | #5 |
| T04 | Quản lý tuyến | 1 | A | Vừa | Quyết định D2 | `T04-quan-ly-tuyen.md` | #6 |
| T05 | Tìm chuyến, danh sách, chi tiết chuyến | 1 | B | Vừa | Quyết định D6, D8 | `T05-tim-chuyen.md` | #7 |
| T06 | Chi tiết tàu và sơ đồ toa | 1 | B | Vừa | Không | `T06-chi-tiet-tau-so-do-toa.md` | #8 |
| T07 | Quản lý chuyến | 2 | A | Vừa | Quyết định D2, D8; nên có T03, T04 | `T07-quan-ly-chuyen.md` | #9 |
| T08 | Thêm toa vào tàu | 2 | C | Vừa | T06, quyết định D3 | `T08-them-toa.md` | #10 |
| T09 | Chọn chỗ và giữ chỗ | 2 | B | Khó | T02, T05, T06, quyết định D1, D6, D7 | `T09-chon-cho-giu-cho.md` | #11 |
| T10 | Chuyến của tôi và hủy booking | 2 | C | Vừa | T02, quyết định D8 | `T10-chuyen-cua-toi-huy-booking.md` | #12 |
| T11 | Hành khách và xem lại booking | 3 | B | Vừa | T09, quyết định D5, D7 | `T11-hanh-khach-xem-lai.md` | #13 |
| T12 | Thanh toán mô phỏng và vé điện tử | 3 | C | Khó | T11, quyết định D5, D7 | `T12-thanh-toan-ve-dien-tu.md` | #14 |
| T13 | Quản trị booking, người dùng, dashboard | 3 | A | Vừa | T10 | `T13-quan-tri-booking-nguoi-dung.md` | #15 |
| T14 | Hoàn thiện và kiểm thử xuyên suốt | 4 | Cả nhóm | Vừa | Tất cả | `T14-hoan-thien.md` | #16 |

## Chia người theo phần việc

Mỗi **phần việc** là một chuỗi task nối tiếp nhau, do cùng một người làm từ đầu tới cuối.
Người đó hiểu sâu phần của mình và ít phải đọc code người khác. Nhóm có một thành viên
mới và hai thành viên đã vững backend, nên phần việc A nhẹ hơn hẳn, còn B và C ngang nhau.
Phi gán tên sau:

| Phần việc | Mảng phụ trách | Chuỗi task | Khối lượng | Dành cho |
| --- | --- | --- | --- | --- |
| A | Màn hình quản trị dữ liệu | T03 -> T04 -> T07 -> T13 | Khoảng 8-9 ngày | Thành viên mới: toàn màn hình thêm, sửa, xóa, xem danh sách theo đúng mẫu module ga, khó dần từng bước |
| B | Khách tìm chuyến và chọn chỗ | T05 -> T06 -> T09 -> T11 | Khoảng 12 ngày | Một thành viên vững: JavaScript `fetch`, giữ chỗ không cho hai người trùng |
| C | Tài khoản và lõi đặt vé | T02 -> T08 -> T10 -> T12 | Khoảng 14 ngày | Một thành viên vững: Spring Security, transaction trên nhiều bảng |

T14 chia đều cho cả nhóm khi tới đợt 4.

Ở đợt 1, ba phần việc không phải chờ nhau. Ở đợt 2, phần B cần T02 của phần C xong mới bắt
đầu T09; phần C cần T06 của phần B xong mới bắt đầu T08. Ở đợt 3, phần C cần T11 của phần B
xong mới bắt đầu T12; trong lúc chờ, phần C làm trước trang vé và trang lỗi của T14.

Người làm phần A nên được người làm phần B hoặc C đọc pull request của từng task. Đó là
cách học nhanh nhất: so code của mình với module ga và với góp ý cụ thể.

## Quyết định nghiệp vụ

Phi chốt cả tám quyết định ngày 12/10/2026, theo đúng đề xuất, riêng D1 chọn hẳn cách
không có job dọn dẹp. Issue #2 đã tick đủ. Luật sản phẩm tương ứng nằm ở
`docs/product/booking-flow.md`, `admin.md` và `auth.md`. Cột "Áp dụng từ" là task đầu tiên
dùng quyết định đó. Muốn đổi một quyết định thì bàn với nhóm, rồi sửa bảng này, file
`docs/product/` tương ứng và các hướng dẫn task có nhắc tới nó trong cùng một commit.

| ID | Câu hỏi | Đã chốt | Áp dụng từ |
| --- | --- | --- | --- |
| D1 | Chỗ hết 5 phút giữ thì trả lại bằng cách nào? | Không có chương trình chạy ngầm. Chỗ `HELD` đã quá `heldUntil` được coi là trống ở mọi nơi, và người khác được giữ đè. Không làm job dọn dẹp định kỳ | T09 |
| D2 | Dữ liệu đang được dùng thì xóa hay hủy thế nào? | Tuyến: không cho xóa khi đã có chuyến; tuyến đã có chuyến chỉ sửa được khoảng cách, không đổi ga. Tàu: không có chức năng xóa. Chuyến: không xóa, chỉ hủy (`CANCELLED`), và không cho hủy khi đã có booking `CONFIRMED` | T04, T07 |
| D3 | Thêm toa vào tàu đã có chuyến thì các chuyến đó ra sao? | Tạo thêm `TripSeat` `AVAILABLE` cho mọi chuyến `SCHEDULED` chưa khởi hành của tàu, trong cùng transaction | T08 |
| D4 | Đăng ký cần những gì? | Họ tên, email, mật khẩu, nhập lại mật khẩu. Mật khẩu ít nhất 8 ký tự. Email không được trùng, không phân biệt chữ hoa chữ thường | T02 |
| D5 | Mã booking và mã vé tạo thế nào? | Booking: `BK` + năm tháng + số thứ tự 3 chữ số, lấy số tiếp theo trong tháng (`BK202610005`). Vé: đổi `BK` thành `TK`, thêm `-` và thứ tự hành khách (`TK202610005-01`), giống dữ liệu mẫu | T11 |
| D6 | Mỗi booking tối đa mấy hành khách? | Từ 1 đến 10, khớp ô "Hành khách" đang có ở trang chủ | T05 |
| D7 | Giữa các bước đặt vé, dữ liệu được lưu ở đâu? | Bấm "Tiếp tục" ở trang chọn chỗ thì giữ chỗ và lưu một bản nháp booking trong session. Bước hành khách và bước xem lại chỉ sửa bản nháp. Bấm "Tiếp tục thanh toán" mới tạo booking `PENDING` trong database. Bấm "Thanh toán" thì xác nhận | T09 |
| D8 | Khi nào coi một chuyến là đã khởi hành hoặc đã hoàn thành? | Mọi luật đặt, giữ, hủy đều so thời điểm khởi hành (ngày + giờ) với hiện tại. Trạng thái `COMPLETED` chỉ để hiển thị; có thể thêm job mỗi giờ để gán, nhưng không bắt buộc | T05 |

## Quy ước chung cho mọi task

- **Branch**: mỗi task một branch, tên tiếng Anh không dấu, ghi sẵn trong file hướng dẫn,
  ví dụ `feat/t03-train-admin`. Gộp vào `main` qua pull request.
- **Mẫu code**: làm theo module ga (`docs/session/huong-dan-mvc-quan-ly-ga.pdf`). Màn hình
  quản trị của nghiệp vụ nào có controller riêng của nghiệp vụ đó (ADR 0001, 0006).
- **Entity**: được thêm method xử lý nghiệp vụ, ví dụ `Trip.cancel()`. Không được thêm hay
  sửa field ứng với cột trong database. Muốn đổi bảng thì bàn với nhóm và viết migration
  mới `V4__...`; không sửa `V1`, `V2`, `V3`, vì máy của cả nhóm đã chạy các file đó.
- **Thời gian**: chỗ nào cần biết "bây giờ" thì gọi `LocalDateTime.now()` (hoặc
  `LocalDate.now()`). Database và ứng dụng cùng chạy giờ `+07:00`.
- **File dùng chung**: chỉ người phụ trách được sửa; người khác cần sửa thì nhờ người đó.

| File | Người phụ trách |
| --- | --- |
| `common/config/SecurityConfig.java` | T02 (khai sẵn quyền cho mọi đường dẫn sẽ có) |
| `templates/fragments/navbar.html`, `templates/admin/fragments/header.html` | T02 |
| `service/TrainGoUserDetails.java` (thông tin người đang đăng nhập) | T02 |
| `common/exception/TrainNotFoundException.java` | T03; T06 dùng lại |
| `static/js/seat-map.js` (hàm vẽ sơ đồ toa) | T06; T09 mở rộng |
| `model/dto/BookingDraft.java`, `service/BookingDraftStore.java`, `static/js/hold-countdown.js` | T09; T11 mở rộng |
| `controller/BookingFlowController.java` | T11; T12 thêm handler |
| `service/BookingCancellationService.java` | T10; T13 dùng lại |
| Method `Trip.departureAt()` | Task nào làm trước thì thêm (T05 hoặc T07) |
| `controller/AdminController.java`, `service/AdminService.java` | Ai chuyển màn hình nào sang controller riêng thì gỡ đúng phần của màn hình đó |
| `docs/WORKLOG.md` | Mọi task thêm một dòng; khi gộp bị trùng chỗ thì giữ cả hai dòng |

- **Một task chỉ coi là xong** khi: `.\mvnw.cmd compile` chạy không lỗi, ứng dụng khởi
  động được bằng `.\mvnw.cmd spring-boot:run`, đã đi hết mục "Kịch bản thử bằng tay" trong
  hướng dẫn trên trình duyệt với MySQL và dữ liệu mẫu, có một dòng `docs/WORKLOG.md` trong
  cùng commit, và pull request đã được một người khác đọc qua.

## Theo dõi tiến độ

Board: https://github.com/orgs/Train-go/projects/1 (riêng tư, chỉ thành viên org xem được).

- View **Board** có ba cột: Cần làm (xám), Đang làm (vàng), Xong (xanh lá). Bắt đầu task thì
  kéo thẻ sang Đang làm. Pull request có `Closes #<số issue>` được gộp xong thì thẻ tự sang
  Xong.
- Mỗi thẻ hiện **Đợt** (Đợt 1 đến 4, mỗi đợt một màu) và **Độ khó** (Dễ xanh lá, Vừa vàng,
  Khó đỏ).
- View **Danh sách** hiện cùng thông tin dưới dạng bảng.

Không dùng Google Sheet, để chỉ có một nơi theo dõi. Nếu giảng viên cần một bảng để chấm,
xuất từ view Danh sách, và không chép nội dung hướng dẫn sang đó, vì hai bản sẽ lệch nhau
ngay khi một bên được sửa.

## Risks And Recovery

- Chuỗi việc dài nhất dồn vào phần C (T02, T12). T02 trễ thì T09, T11, T12 trễ theo. Cách
  phòng: T02 bắt đầu ngay ngày đầu; phần B làm T05, T06 trước, không ngồi chờ.
- D7 là quy ước chung giữa T09, T11 và T12, dễ bị mỗi task hiểu một kiểu. Cách phòng: người
  làm T11 và T12 đọc `model/dto/BookingDraft.java` và `service/BookingDraftStore.java` của
  T09 trước khi viết code, không tự tạo bản nháp riêng.
- Hai người cùng sửa một entity, ví dụ `TripSeat` ở T09 và T12. Cách phòng: mỗi task chỉ thêm
  method của mình; khi gộp có trùng chỗ thì chỉ là hai method khác nhau, giữ cả hai.
- Không có test tự động, nên lỗi giữ chỗ trùng, transaction và phân quyền dễ lọt qua vì nhìn
  bằng mắt khó thấy. Cách phòng: kịch bản thử bằng tay của T02, T09, T10, T12 có bước thử hai
  trình duyệt và bước SQL (ví dụ lùi `held_until` về quá khứ); người đọc PR tự chạy lại các
  bước đó với nghiệp vụ rủi ro.

## Progress

- [x] Database, dữ liệu mẫu, module Quản lý ga (01/10/2026, commit `9748dcd`).
- [x] Bản nháp chia việc và 14 file hướng dẫn (04/10/2026).
- [x] Phi duyệt cách chia: phần việc A cho thành viên mới, B và C chia đều (04/10/2026).
- [x] Tạo board "TrainGo MVP", issue #2 cho D1-D8 và 14 issue #3-#16 cho T01-T14 (04/10/2026).
- [x] Board dùng tiếng Việt và màu: ba cột trạng thái, trường Đợt và Độ khó. Viết lại
      14 hướng dẫn, mô tả issue và plan cho dễ hiểu; thêm `huong-dan-task/README.md`
      (04/10/2026).
- [x] Admin đọc dữ liệu thật từ database, code chia package theo tầng (PR #17, commit
      `f30c667`).
- [x] Bỏ test tự động và T01; ghép hướng dẫn dễ hiểu với cấu trúc package theo tầng và phạm
      vi mới của T03, T04, T05, T07, T13 (12/10/2026).
- [ ] Gán người cho từng issue.
- [x] Chốt D1-D8 (issue #2, 12/10/2026).
- [ ] Đợt 1: T02-T06.
- [ ] Đợt 2: T07-T10.
- [ ] Đợt 3: T11-T13.
- [ ] Đợt 4: T14.

## Decisions

- 2026-10-04: Duyệt 14 task, 4 đợt, 3 phần việc như trên. Phần việc A dành cho thành viên
  mới (chỉ màn hình thêm, sửa, xóa, xem danh sách theo mẫu module ga); B và C khối lượng
  tương đương cho hai thành viên đã vững backend. Chưa gán tên.
- 2026-10-04: Theo dõi bằng GitHub Projects của org `Train-go`, mỗi task một issue thật
  trong repo `Train-go/train-go` (repo trước đây tên `Voyage-GO/voyage-go`; GitHub vẫn tự
  chuyển hướng tên cũ).
- 2026-10-04: Board dùng tiếng Việt: trạng thái Cần làm, Đang làm, Xong; trường Đợt; trường
  Độ khó thay cho "Cỡ" (Dễ, Vừa, Khó). Trường Lane đã được xóa khỏi board; khi gán người,
  cột người được gán thay vai trò đó.
- 2026-10-12: Bỏ test tự động (ADR 0007, thay ADR 0002). T01 bỏ, issue #3 đóng. Mỗi hướng dẫn
  thay mục test bằng "Kịch bản thử bằng tay". Bỏ quy ước bean `Clock`, dùng
  `LocalDateTime.now()`.
- 2026-10-12: Hướng dẫn và plan theo cấu trúc package theo tầng (ADR 0006). Vì danh sách
  admin đã đọc database (commit `9ba771b`), T03 và T07 chuyển phần tàu, chuyến từ
  `AdminService` sang service riêng; T13 chỉ còn tìm kiếm, chi tiết, hủy booking và tách
  controller.
- 2026-10-12: Chốt D1-D8 như bảng "Quyết định nghiệp vụ". D1 chọn kiểm tra hạn giữ khi đọc,
  không có job dọn dẹp, để chỗ được trả đúng giờ và không thêm code chạy ngầm. D2 là luật
  nhóm tự thêm vì `SPEC.md` mục 35 chỉ ghi "Xóa Route": chặn xóa dữ liệu đã có dữ liệu khác
  dựa vào, và không hủy chuyến đã có khách trả tiền vì hoàn tiền nằm ngoài phạm vi. D8 giữ
  job chuyển `COMPLETED` là phần không bắt buộc của T07.

## Validation

- Mỗi task: làm đủ mục "Kiểm tra trước khi báo xong" ở cuối file hướng dẫn của task.
- Toàn MVP: T14 đi trọn luồng ở mục Outcome trên trình duyệt với MySQL, theo kịch bản thử
  bằng tay của T14.

## Result

Chưa bắt đầu.
