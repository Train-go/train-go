# Execution Plan: Thiết kế lại giao diện admin trong prototype v2

Date: 2026-10-29

## Status

Đã thay thế bởi `docs/plans/active/dua-giao-dien-moi-vao-app.md` (30/10/2026): prototype đã chốt theo hướng E, không còn hướng navy mô tả dưới đây. Trước đó: Active. Phi duyệt ngày 29/10/2026 ("thực hiện nhé"), nhận các đề xuất của bốn câu hỏi.
Đợt 0, Đợt 1 (làm lại theo hướng enterprise), trang Chuyến và trang Ga và tuyến xong;
chờ Phi xem. Còn: Booking, Người dùng, Tàu (Theo ngày, Bảng tính), Lịch sử.

## Outcome

Có một prototype admin chạy được trong `D:\Study\Project\VoyageGO\traingo-redesign-v2\admin\`,
theo hướng enterprise của `design-system/traingo/pages/admin.md` (menu navy, Fira Sans và
Fira Code, xanh navy `#1E40AF`, bo góc 8/10px, viền thay bóng, icon Lucide). Prototype phủ đủ 10 màn đang có trong app thật, giữ đúng hành vi
mà `docs/product/admin.md` mô tả, và chạy được luồng thật trên dữ liệu mẫu (mở ngăn chi
tiết, hủy booking, thêm chuyến, hoàn tác). Phi duyệt prototype này rồi mới có một plan
riêng để đưa vào app.

## Context

- Hành vi từng màn: `docs/product/admin.md` (bản gốc khi mâu thuẫn là `SPEC.md` mục 33-40).
- Khung admin hiện tại: `docs/decisions/0004-khu-vuc-quan-tri.md`,
  `docs/decisions/0012-giao-dien-admin-moi.md` (Bootstrap dùng chung, `admin.css` đặt
  biến `--bs-*` theo màu riêng của admin, menu trái ba nhóm, ngăn kéo phải, hộp xác nhận,
  toast). Mẫu bố cục: `docs/reports/ui-mau/admin/index.html`.
- Template hiện có: `app/src/main/resources/templates/admin/` (10 trang, 4 fragment).
  Menu: Tổng quan; Vận hành (Tàu, Chuyến); Dữ liệu nền (Ga và tuyến, Tuyến); Bán vé
  (Booking, Người dùng); Lịch sử ở chân menu, trên Đăng xuất.
- Hệ thiết kế v2: `traingo-redesign-v2/design-system/traingo/MASTER.md`,
  `css/traingo.css` (token), `css/consist.css` (sơ đồ đoàn tàu), `css/checkout.css`,
  `js/data.js` (ga, tàu, giá theo seed V5), `js/seats.js` (sơ đồ chỗ).
- Quy tắc câu chữ: không đưa luật nghiệp vụ hay chữ kỹ thuật lên màn hình, hành động
  không được phép thì ẩn (bộ nhớ dự án "ui-copy-no-internal-notes", ADR 0012 không đổi
  điều này). Câu báo lỗi lấy nguyên văn từ service.

## Scope

In scope:

- Khung admin v2: menu trái, thanh trên, khung nội dung, trạng thái đang chọn theo trang.
- Bộ thành phần dùng chung: bảng dày, tab có số đếm, ô tìm, ngăn kéo phải, hộp xác nhận,
  toast có "Hoàn tác", ô số liệu, nhãn trạng thái (Sắp chạy, Đang chạy, Đã chạy, Đã hủy,
  Đã thanh toán), trạng thái đang tải, trống, lỗi.
- 10 màn: Tổng quan, Tàu (Theo ngày, Bảng tính), Trình hướng dẫn thêm chuyến, Chuyến,
  Ga và tuyến, Tuyến, Booking, Người dùng, Lịch sử. Ngăn chi tiết chuyến và ngăn chi
  tiết booking dùng chung trên mọi trang.
- Dữ liệu mẫu `js/admin-data.js` dựng từ `js/data.js`: người dùng theo seed V2/V3, chuyến
  của SE1, SE2, SE3, booking có vé, tàu đã xóa mềm cho trang Lịch sử.
- Mỗi màn có đủ trạng thái mà app có logic: đầy, trống, lỗi, đang tải.

Out of scope:

- Đưa thiết kế vào app thật (template Thymeleaf, `admin.css`, ADR mới). Làm ở plan sau,
  khi prototype đã duyệt.
- Thay đổi backend, API hay nghiệp vụ. Tính năng không có trong `docs/product/admin.md`.
- Dark mode, đa ngôn ngữ.

## Approach

Làm theo từng đợt; hết mỗi đợt Phi xem prototype rồi mới sang đợt sau.

1. **Đợt 0, nền:** `admin/` dùng lại token của `traingo.css` qua file `css/admin.css`
   riêng (không nạp `checkout.css` hay CSS trang chủ). Dựng khung menu và bộ thành phần
   dùng chung, mỗi thành phần đủ tám trạng thái (mặc định, hover, focus, active, disabled,
   loading, error, success). Trang `admin/thanh-phan.html` bày tất cả để duyệt một lần.
2. **Đợt 1, Tổng quan:** bốn ô số có liên kết, bộ lọc khoảng ngày nằm trên địa chỉ, hai
   biểu đồ SVG (doanh thu và booking; người dùng mới), khối Cần chú ý, lịch tàu 7 ngày.
3. **Đợt 2, Bán vé:** Booking (ba tab, tìm sau 300ms, ngăn chi tiết, hủy có xác nhận rồi
   toast, `?open=BK…`), Người dùng (hai tab, link "N booking").
4. **Đợt 3, Vận hành:** Chuyến (tab, lọc tàu, nhóm theo ngày, ngăn chi tiết chuyến có sơ
   đồ chỗ dùng lại `consist.css` và `seats.js` của phần khách), Tàu (tab tàu, banner vị
   trí, view Theo ngày và Bảng tính với dòng nháp và "Điền nhanh"), Trình hướng dẫn thêm
   chuyến (4 bước, xem trước bên phải).
5. **Đợt 4, Dữ liệu nền:** Ga và tuyến (sơ đồ đường tàu dọc theo lý trình, ngăn ga, ngăn
   cặp ga, chèn ga có combobox gợi ý), Tuyến (danh sách, form), Lịch sử (khôi phục tàu).

Điểm nhấn của bộ giao diện: ngăn chi tiết chuyến vẽ lại đúng đoàn tàu và sơ đồ chỗ mà
khách nhìn thấy khi đặt vé, nên admin và khách cùng nhìn một chiếc tàu.

## Risks And Recovery

- **Khối lượng lớn** (10 màn, có hai màn phức tạp là Bảng tính và sơ đồ Ga và tuyến):
  chia đợt như trên; nếu thời gian không đủ thì dừng sau đợt 2, các màn còn lại giữ thiết
  kế hiện tại của app.
- **Prototype lệch hành vi app thật:** mỗi màn đối chiếu với mục tương ứng của
  `docs/product/admin.md` và template thật trước khi dựng; câu lỗi chép nguyên văn từ
  service.
- **Hai hệ màu khi đưa vào app:** ADR 0012 đang cho admin màu riêng. Prototype không đụng
  app, nên không có gì cần khôi phục; quyết định đổi màu admin trong app sẽ là một ADR
  mới ở plan sau.

## Progress

- [x] Phi trả lời bốn câu hỏi ở mục "Decisions" (nhận đề xuất, 29/10/2026).
- [x] Đợt 0: khung menu (`admin/partials-nav.html`), `css/admin.css`, `js/admin.js`
      (ngăn chi tiết, hộp xác nhận, toast), dữ liệu mẫu `js/admin-data.js`,
      `admin/thanh-phan.html`.
- [x] Đợt 1: Tổng quan (`admin/index.html`, `js/admin-overview.js`); làm lại theo hướng
      enterprise ngày 29/10/2026.
- [x] Ga và tuyến (`admin/ga-va-tuyen.html`, `js/admin-stations.js`), gộp cả trang Tuyến.
- [x] Chuyến (`admin/chuyen.html`, `js/admin-trips.js`), có thêm chuyến và tạo tuyến tại chỗ.
      Mô hình chung: `js/admin-network.js`.
- [ ] Đợt 2: Booking, Người dùng.
- [ ] Đợt 3: Chuyến, Tàu (Theo ngày, Bảng tính), Trình hướng dẫn thêm chuyến.
- [ ] Đợt 4: Ga và tuyến, Tuyến, Lịch sử.

## Decisions

- 2026-10-29: Phi nói "thực hiện nhé" mà không chọn lại, nên cả bốn câu theo đề xuất:
  token v2 không ảnh, không kính, nút chính xanh; menu trái ba nhóm; dòng 44px chữ 16px;
  Tổng quan trước.
- 2026-10-29: Phi yêu cầu hướng enterprise cho admin (`/hallmark` + `/ui-ux-pro-max`):
  `design-system/traingo/pages/admin.md` thay quyết định Q1 (admin dùng navy, Fira Sans và
  Fira Code, không dùng token màu khách nữa).
- 2026-10-29: "Tạo tuyến khó": tuyến được tạo ngay chỗ thiếu (trên đường tàu, trong bảng
  tuyến, trong ngăn Thêm chuyến). Trang Tuyến gộp vào Ga và tuyến, menu bỏ mục Tuyến. Thêm
  chuyến chuyển sang trang Chuyến (vẫn nối chặng từ ga tàu đang đứng). Cần Phi xác nhận khi
  đưa vào app.
- 2026-10-29: `.avatar` và `.link-btn` chuyển từ `checkout.css` sang `traingo.css` vì
  admin dùng chung; admin không nạp `checkout.css`.
- 2026-10-29: dữ liệu mẫu sinh quanh "bây giờ" để mọi khối Tổng quan có nội dung (SE1
  có một ngày trống, SE2 sắp hết lịch, SE3 không còn chuyến). Số liệu là mẫu, không
  phải số của app.

Bốn câu hỏi ban đầu:

- **Q1, màu và chất liệu.** Đề xuất: dùng đúng token của v2 nhưng bỏ ảnh và hiệu ứng kính;
  nút chính màu xanh, không dùng màu cam (cam dành cho hành động mua vé của khách). Cách
  khác: giữ cả cam cho nút chính để hai khu giống hệt nhau.
- **Q2, khung điều hướng.** Đề xuất: giữ menu trái ba nhóm như app (9 đích đến, quá nhiều
  cho thanh trên), thu thành menu mở bằng nút dưới 960px. Cách khác: thanh trên kèm menu
  thả xuống.
- **Q3, mật độ bảng.** Đề xuất: dòng 44px, chữ 16px, như các công cụ quản trị quen thuộc.
  Cách khác: dòng 52px, thoáng như phần khách.
- **Q4, thứ tự đợt.** Đề xuất như mục "Approach" (Tổng quan trước vì là trang đầu admin
  thấy). Cách khác: làm Booking và Chuyến trước vì dùng hằng ngày.

## Validation

- Focused proof: mỗi màn đi bằng Playwright trên server tĩnh: mở ngăn, lọc, tìm, hủy có
  xác nhận, hoàn tác, liên kết `?open=`/`?trip=` mở thẳng; chụp ở 1440, 1280, 768 và 375px,
  đo không có cuộn ngang trên phần tử thật sự cuộn.
- Integration or end-to-end proof: một vòng đầy đủ: khách đặt vé ở phần client v2, booking
  hiện trong Booking và Tổng quan của admin, hủy booking thì chỗ mở lại trên sơ đồ.
- Repository-required checks: `sloplint --scope app` của Hallmark không còn FAIL ở file mới;
  câu chữ qua `vi-ux-writing` và Phi duyệt. Prototype nằm ngoài repo nên không có
  `mvnw compile` ở plan này.

## Result

Chưa làm.
