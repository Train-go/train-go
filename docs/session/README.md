# Vật phẩm của phiên làm việc

Thư mục này chứa những thứ sinh ra trong một phiên làm việc nhưng chưa đủ tư cách
trở thành tài liệu chính thức: plan nháp đang chờ duyệt, và tài liệu tổng hợp
phục vụ một mục đích cụ thể như thuyết trình hay nộp bài.

## Hai loại file

| Đặt tên | Là gì |
| --- | --- |
| `plan-<slug>.md` | Plan nháp, chờ duyệt trước khi thành plan chính thức |
| `<slug>.md`, `<slug>.html` | Tài liệu của phiên làm việc, không phải plan |

## Vòng đời của một plan

```text
docs/session/plan-<slug>.md
  -> thảo luận và chỉnh sửa cho tới khi được duyệt
  -> docs/plans/active/<slug>.md      (bản nháp bị xóa tại bước này)
  -> docs/plans/completed/<slug>.md   (sau khi kết quả đã được kiểm chứng)
```

**Một plan chỉ sống ở một nơi tại một thời điểm.** Khi bản nháp được nâng lên
`docs/plans/active/`, bản nháp trong thư mục này phải bị xóa, không giữ lại làm
bản lưu.

Quy tắc này không phải để cho gọn thư mục. `docs/WORKFLOW.md` yêu cầu tránh tạo
hồ sơ công việc song song (`avoid parallel task records without an independent
audience`), và hai bản của cùng một plan chính là thứ nó cấm: sửa tiến độ ở một
bản mà quên bản kia thì không ai còn biết bản nào đúng.

## Khi nào cần bản nháp

Cần, khi việc đủ lớn để phải bàn trước khi làm: có nhiều lựa chọn kỹ thuật chưa
chốt, ảnh hưởng tới phần việc của người khác, hoặc cần được duyệt trước khi bắt
đầu sửa code.

Không cần, khi việc gọn trong một phiên và có thể khôi phục từ chính diff của nó
— sửa một lỗi rõ ràng, đổi tên biến, thêm một test. Harness gọi đó là ephemeral
plan: làm, kiểm chứng, báo cáo, không để lại hồ sơ.

## Viết bản nháp bằng gì

Dùng `docs/templates/exec-plan.md`, giống hệt plan chính thức. Nhờ vậy bước nâng
cấp chỉ là di chuyển file và cập nhật mục `Status`, không phải viết lại từ đầu.

## Tài liệu trong thư mục này

`tai-lieu-phan-tich-traingo.md` — tài liệu phân tích nghiệp vụ: bản đồ tài liệu,
quy trình, mô hình dữ liệu và từ điển dữ liệu, 35 quy tắc nghiệp vụ, 25 đặc tả
use case, 22 màn hình, kế hoạch và ma trận truy vết.

Là bản tổng hợp có đóng dấu ngày, **không phải bản gốc**. Khi mâu thuẫn,
`SPEC.md` và `docs/product/` mới là bản gốc.

`tai-lieu-phan-tich-traingo.html` — bản HTML tự chứa, mở trực tiếp bằng trình
duyệt từ ổ đĩa, không cần mạng. `Ctrl+P` ra bản in đã ẩn mục lục và ngắt trang
theo chương.

**Không sửa tay file HTML.** Sửa bản Markdown rồi sinh lại:

```powershell
python docs/session/render-doc.py
```

`huong-dan-mvc-quan-ly-ga.md`, `.html`, `.pdf`: hướng dẫn MVC qua module Quản
lý ga, gồm cách dùng màn hình, giải thích từng file code, đường đi của request,
lỗi thường gặp và bài tập. Viết cho thành viên sắp làm màn hình quản trị tiếp
theo. Sửa bản Markdown, rồi sinh lại HTML bằng `render-doc.py` và PDF bằng cách
in HTML ra PDF.

`render-doc.py` — bộ chuyển Markdown sang HTML viết riêng cho thư mục này. Không
dùng thư viện ngoài và không gọi mạng, vì trang phải mở được khi không có wifi.
Nó chỉ xử lý tập cú pháp Markdown mà các tài liệu ở đây dùng, không phải một bộ
Markdown đầy đủ. Truyền tên file để sinh tài liệu khác:

```powershell
python docs/session/render-doc.py ten-file-khac.md
```
