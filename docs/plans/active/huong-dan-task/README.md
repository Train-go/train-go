# Đọc trước khi làm task

Thư mục này có 12 file hướng dẫn, mỗi file cho một task (T02 đến T14, trừ T08; T01 đã bỏ
cùng test tự động ngày 12/10/2026, T08 đã bỏ ngày 19/10/2026 cùng bảng toa và ghế, xem
`docs/decisions/0009-bo-bang-toa-va-ghe.md`). File `T09-chon-cho-giu-cho.md` giữ tên cũ cho các
link khỏi gãy, nhưng nội dung là task "Chọn chỗ": tính năng giữ chỗ đã bỏ ngày 19/10/2026,
chỗ chỉ khóa lúc thanh toán ở T12 (`docs/decisions/0010-bo-giu-cho.md`). Danh sách task,
thứ tự làm và ai làm phần nào nằm ở `docs/plans/active/chia-viec-mvp.md`. Tiến độ theo
dõi trên board: https://github.com/orgs/Train-go/projects/1.

Nếu chưa quen Spring MVC, hãy đọc trước tài liệu mẫu `docs/session/huong-dan-mvc-quan-ly-ga.pdf`.
Module Quản lý ga trong tài liệu đó là ví dụ hoàn chỉnh mà mọi task đều làm theo.

## Một file hướng dẫn gồm những gì

Mọi file đều có cùng thứ tự:

1. **Tóm tắt một câu** và một bảng thông tin nhanh: đợt, phần việc, độ khó, task cần
   xong trước, task đang chờ task này, tên branch.
2. **Task này làm gì**: người dùng thấy gì khi task xong.
3. **Phạm vi**: phần nào làm, phần nào để task khác.
4. **Dữ liệu dùng tới**: các bảng trong database và quan hệ giữa chúng.
5. **Các bước làm**: đi lần lượt từ Model, Repository, Service, Controller tới giao diện.
   Mỗi bước ghi rõ tạo file nào, viết method nào.
6. **Luật và lỗi phải xử lý**.
7. **Kịch bản thử bằng tay**: các bước tự thử trên trình duyệt, kể cả trường hợp lỗi.
8. **Lỗi hay gặp**: những chỗ người trước đã vấp.
9. **Kiểm tra trước khi báo xong**: danh sách để tự tick.

## Cách làm một task, từ đầu tới cuối

1. Vào board, kéo thẻ của task sang cột **Đang làm** và gán tên mình vào issue.
2. Lấy code mới nhất: `git checkout main`, `git pull`.
3. Tạo branch đúng tên ghi trong hướng dẫn: `git checkout -b feat/t03-train-admin`.
4. Đọc hết file hướng dẫn một lượt rồi mới viết code.
5. Code theo "Các bước làm". Chạy ứng dụng, tự thử trên trình duyệt sau mỗi bước lớn.
6. Trong thư mục `app`, chạy `.\mvnw.cmd compile` cho tới khi không lỗi, rồi chạy ứng
   dụng và đi hết mục "Kịch bản thử bằng tay". Dự án không có test tự động
   (`docs/decisions/0007-bo-test-tu-dong.md`).
7. Thêm một dòng vào `docs/WORKLOG.md` (ngày, loại thay đổi, làm gì, đã kiểm tra gì).
8. Commit, push branch, mở pull request. Phần mô tả PR ghi `Closes #<số issue>` để issue
   tự đóng khi merge.
9. Nhờ một người khác đọc PR. Sửa theo góp ý, rồi merge. Thẻ trên board tự sang **Xong**.

## Giải thích từ ngữ

### Từ dùng khi chia việc

| Từ | Nghĩa |
| --- | --- |
| Đợt | Thứ tự thời gian. Task cùng đợt làm song song được; task đợt sau phải đợi task nó cần làm xong |
| Phần việc | Chuỗi task do cùng một người làm từ đầu tới cuối: A, B hoặc C. Phần việc A dành cho thành viên mới |
| Độ khó | Dễ (khoảng 1 ngày), Vừa (khoảng 2-3 ngày), Khó (khoảng 4-5 ngày). Chỉ là ước lượng để chia việc cho đều, không phải hạn chót |
| Cần xong trước | Task hoặc quyết định phải xong thì mới bắt đầu task này được. "Nên có" nghĩa là làm trước được, nhưng có thì dễ hơn |
| Task đang chờ | Những task chỉ bắt đầu được sau khi task này xong |
| D1-D9 | Các quyết định nghiệp vụ Phi đã chốt, xem bảng trong `chia-viec-mvp.md` (issue #2). D1-D8 chốt ngày 12/10/2026; D9 (ga, tuyến, tàu và chuyến nối chặng) chốt ngày 20/10/2026, chi tiết ở `docs/decisions/0011-chuyen-tao-tu-tau-noi-chang.md` |

### Từ kỹ thuật

| Từ | Nghĩa |
| --- | --- |
| Entity | Class Java ứng với một bảng trong database, ví dụ `Station` ứng với bảng `stations`. Mỗi object là một dòng |
| Repository | Interface đọc và ghi database. Viết tên method đúng quy tắc là Spring tự sinh câu SQL |
| Service | Class chứa luật nghiệp vụ, ví dụ "mã ga không được trùng" |
| Controller | Class nhận request từ trình duyệt, gọi service rồi trả về trang HTML hoặc JSON |
| View, template | File HTML trong `templates/`, dùng Thymeleaf để điền dữ liệu |
| Form object | Class chứa dữ liệu người dùng gửi lên từ form, kèm luật kiểm tra như `@NotBlank` |
| DTO, `record` | Class nhỏ chỉ để chở dữ liệu ra view hoặc ra JSON, không ánh xạ bảng nào |
| Transaction | Một nhóm thao tác database phải cùng thành công hoặc cùng không xảy ra. Đánh dấu bằng `@Transactional` |
| Rollback | Khi transaction gặp lỗi, mọi thay đổi trong nó bị hủy như chưa từng xảy ra |
| `LAZY` | Hibernate chưa đọc dữ liệu của bảng liên quan cho tới khi code cần tới. Dự án tắt `open-in-view`, nên đọc muộn ngoài service sẽ lỗi `LazyInitializationException` |
| `@EntityGraph` | Bảo Hibernate đọc luôn dữ liệu của bảng liên quan trong cùng một câu `SELECT`, để tránh lỗi trên |
| Migration, Flyway | File SQL có đánh số trong `db/migration/` tạo và sửa bảng. Không sửa file đã chạy; muốn đổi bảng thì viết file mới |
| Seed | Dữ liệu mẫu có sẵn: 8 ga trên trục Bắc - Nam, 16 tuyến, 3 tàu (SE1, SE2, SE3), 18 chuyến (đều của SE1), 12 tài khoản, 8 booking. Ngày chuyến tính từ lúc seed chạy. Xem `docs/RUNBOOK.md`, mục "Dữ liệu thử" |
| Lý trình, `kmMark` | Số km tính từ ga Hà Nội của một ga, ví dụ Huế là 688. Mọi danh sách ga và tuyến xếp theo lý trình, tức từ Bắc vào Nam |
| Nối chặng | Chuyến mới của một tàu phải xuất phát từ ga mà chuyến cuối chưa hủy của tàu vừa đến, và sau giờ đến ít nhất 30 phút (D9) |
| Session | Vùng nhớ phía server gắn với từng trình duyệt. Dùng để nhớ ai đang đăng nhập và bản nháp booking |
| CSRF token | Mã bí mật mà mọi form `POST` phải gửi kèm. Dùng `th:action` là Thymeleaf tự chèn; thiếu thì server trả 403 |
| Flash message | Thông báo sống qua đúng một lần chuyển trang, ví dụ "Đã thêm ga ..." sau khi lưu |
| Post/Redirect/Get | Lưu xong thì chuyển hướng sang trang khác, để bấm F5 không gửi lại form lần nữa |
| 200, 302, 401, 403, 404 | Mã trả về của server: thành công, chuyển hướng, chưa đăng nhập, không có quyền, không tìm thấy |
| Kịch bản thử bằng tay | Danh sách bước tự thử trên trình duyệt hoặc bằng SQL để chắc tính năng chạy đúng, thay cho test tự động |
| Xem database | `docker exec -it traingo-mysql mysql -utraingo -ptraingo --default-character-set=utf8mb4 traingo`, rồi gõ câu SQL như `SELECT * FROM trains;` |
| Pull request (PR) | Yêu cầu gộp branch của mình vào `main`, để người khác đọc trước khi gộp |
