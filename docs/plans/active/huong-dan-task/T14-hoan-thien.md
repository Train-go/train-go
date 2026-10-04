# T14 Hoàn thiện và kiểm thử xuyên suốt

| Wave | Lane | Cỡ | Phụ thuộc | Mở khóa | Branch |
| --- | --- | --- | --- | --- | --- |
| 4 | Cả nhóm | M | T01-T13 | Buổi demo, đóng plan MVP | `feat/t14-polish` (hoặc mỗi mục một branch) |

## Mục tiêu

MVP chạy thông từ đầu tới cuối như `SPEC.md` mục 50, thông báo lỗi dễ hiểu ở mọi trường
hợp của mục 45.5, giao diện khách dùng được trên điện thoại, và có một test tự động chạy
trọn luồng đặt vé.

## Các phần việc

Chia đều cho ba người khi tới wave 4:

| Phần | Gợi ý người làm | Việc |
| --- | --- | --- |
| Trang lỗi | Lane C | `templates/error/404.html`, `403.html`, `500.html` theo khung khách; Spring Boot tự chọn theo mã lỗi |
| Test xuyên suốt | Lane C | Một `@SpringBootTest` chạy cả luồng (bên dưới) |
| Responsive | Lane B | Landing, tìm chuyến, chọn chỗ, chuyến của tôi, vé ở 375, 768, 1280px (`SPEC.md` mục 45.1) |
| Thông báo lỗi | Lane B | Rà đủ 7 trường hợp của `SPEC.md` mục 45.5 |
| Dữ liệu demo | Lane A | Làm mới seed trước buổi demo, chuẩn bị kịch bản demo |
| Tài liệu | Lane A | Cập nhật plan, RUNBOOK, đóng plan MVP |

## Test xuyên suốt

`src/test/java/com/voyagego/traingo/BookingFlowIntegrationTest.java`:

- `@SpringBootTest` + `@AutoConfigureMockMvc`, chạy trên H2 (T01), với `Clock` cố định
  ghi đè bằng `@TestConfiguration`.
- Một `MockHttpSession` đi qua đúng các bước người dùng làm:
  1. Đăng nhập là một khách hàng.
  2. `POST /booking/{tripId}/seats` với 2 chỗ.
  3. `POST /booking/passengers` với 2 hành khách.
  4. `POST /booking/review`.
  5. `POST /booking/payment`.
- Kiểm tra database sau cùng: booking `CONFIRMED` / `PAID`, 2 chỗ `BOOKED`, 2 vé.
- Ca thứ hai: dời `Clock` qua 6 phút trước bước 5. Thanh toán phải bị từ chối, và database
  không có chỗ `BOOKED` hay vé nào của booking đó. Ca này chứng minh rollback trên database
  thật, điều mà test Mockito của T12 không chứng minh được.

ADR 0002 (mục Follow-Up) đã ghi việc thêm test này khi luồng đặt vé hoàn chỉnh.

## Thông báo lỗi phải có (`SPEC.md` mục 45.5)

| Trường hợp | Ở đâu | Kết quả mong đợi |
| --- | --- | --- |
| Chuyến không tồn tại | T05 | Trang 404 tiếng Việt |
| Chuyến bị hủy | T05, T09 | Nhãn "Đã hủy", không chọn chỗ được |
| Chỗ vừa bị người khác giữ | T09 | Thông báo, sơ đồ tải lại |
| Hết thời gian giữ chỗ | T11, T12 | Thông báo, quay về chi tiết chuyến |
| Booking không tồn tại | T10, T12 | 404 |
| Booking của người khác | T10, T12 | 404 |
| Form không hợp lệ | Mọi form | Lỗi dưới từng ô, giữ dữ liệu đã nhập |

Không có trường hợp nào được hiện stack trace.

## Dữ liệu demo

Ngày trong seed tính từ lúc database được tạo, và chỉ kéo dài 14 ngày. Một hai ngày trước
buổi demo, làm mới dữ liệu theo `docs/RUNBOOK.md`, mục "Làm mới dữ liệu demo".

Kịch bản demo gợi ý: admin tạo một chuyến mới (T07) → khách tìm và đặt 2 chỗ trên chuyến
đó → xem vé → hủy trong "Chuyến của tôi" → admin thấy booking đã hủy và dashboard đổi số.

## Xong khi

- [ ] `BookingFlowIntegrationTest` xanh, cả hai ca.
- [ ] Bảy trường hợp lỗi ở trên đều thử bằng tay và đạt.
- [ ] Năm trang khách không tràn ngang ở 375px; chữ tiếng Việt không bị cắt dấu.
- [ ] Kịch bản demo chạy trọn trên MySQL với dữ liệu mới.
- [ ] `docs/plans/active/mvp-traingo.md` có mục Result với bằng chứng, rồi chuyển sang
      `docs/plans/completed/`. Plan chia việc này cũng chuyển theo.
- [ ] `.\mvnw.cmd test` xanh. Một dòng `docs/WORKLOG.md` cho mỗi phần việc.

Chỉ sau khi mọi ô trên đều xong mới bắt đầu tính năng tùy chọn: mã QR, quét QR, bản đồ,
xuất PDF vé (`SPEC.md` mục 47).
