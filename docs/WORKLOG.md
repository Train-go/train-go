# Nhật ký thay đổi TrainGo

File này là chỉ mục kiểm toán cho các task làm thay đổi repository. Mục tiêu là
giúp nhóm và Harness biết thay đổi nào đã được thực hiện, vì sao có thay đổi đó
và bằng chứng nào đã được chạy.

Quy tắc bắt buộc áp dụng từ commit đưa file này vào dự án. Những dòng về thay
đổi cũ hơn, nếu có, chỉ là dữ liệu khởi tạo được truy vết từ Git và kết quả test.

## Quy tắc ghi

- Thêm một dòng cho mỗi task, đặt dòng mới nhất ở đầu bảng.
- Dùng loại thay đổi theo Conventional Commits như `feat`, `fix`, `docs`,
  `refactor`, `test`, `chore`, `perf`, `build` hoặc `ci`.
- Ghi kết quả và phạm vi chính, không ghi lại từng lệnh thao tác.
- Bằng chứng phải là test, kiểm tra cú pháp, kiểm tra giao diện hoặc quan sát
  runtime đã thực sự chạy. Nếu chưa kiểm chứng được, ghi rõ giới hạn.
- Bản ghi phải được commit cùng thay đổi mà nó mô tả.
- Khi bảng dài quá 200 dòng, chuyển các năm cũ sang
  `docs/worklog/<nam>.md` và giữ năm hiện tại trong file này.

## Nhật ký

| Ngày | Loại | Kết quả và phạm vi | Bằng chứng |
| --- | --- | --- | --- |
| 2026-09-02 | `feat(design)` | Tải local bốn font OFL gồm Be Vietnam Pro, Inter, Onest và IBM Plex Sans; thêm stylesheet `@font-face` cùng Font Lab để so sánh mà chưa đổi font sản phẩm. | Kiểm tra magic bytes WOFF2 và bốn file OFL; JavaScript syntax; Maven: 15 test vượt qua; kiểm tra giao diện desktop và mobile. |
| 2026-09-02 | `docs(harness)` | Bổ sung quy định bắt buộc ghi lại mọi task thay đổi repository, tạo `docs/WORKLOG.md` và cập nhật bản đồ tài liệu. | Đối chiếu `AGENTS.md`, `docs/WORKFLOW.md` và bản đồ tài liệu hiện có; `git diff --check`. |
| 2026-09-02 | `feat(design)` | Thêm Color Lab và hệ token Railway Blue đầy đủ cho client, admin và semantic status. | Commit `52949fa`; Maven: 14 test vượt qua; route `/design/color-palettes.html` trả HTTP 200. |
