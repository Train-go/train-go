# TrainGo

TrainGo là website đặt vé tàu: tìm chuyến, chọn ghế hoặc giường trên sơ đồ toa,
thanh toán mô phỏng và nhận vé điện tử.

Repository trên GitHub là `Train-go/train-go` (trước đây là `Voyage-GO/voyage-go`); tên
sản phẩm hiển thị ở mọi nơi là **TrainGo**, xem `docs/decisions/0003-he-thong-giao-dien.md`.
Tiến độ và phân công: project https://github.com/orgs/Train-go/projects/1 và
`docs/plans/active/chia-viec-mvp.md`.

## Bắt đầu từ đâu

| Bạn muốn | Đọc file |
| --- | --- |
| Nắm toàn bộ dự án trong một lần đọc | `docs/session/tai-lieu-phan-tich-traingo.md` |
| Hiểu sản phẩm phải làm gì | `docs/product/overview.md` |
| Hiểu dữ liệu và quan hệ giữa các entity | `docs/product/domain-model.md` |
| Hiểu luồng đặt vé, giữ chỗ, thanh toán | `docs/product/booking-flow.md` |
| Chạy ứng dụng và database ở máy mình | `docs/RUNBOOK.md` |
| Biết đang làm tới đâu và tiếp theo làm gì | `docs/plans/active/mvp-traingo.md` |
| Biết vì sao dự án được cấu trúc như hiện tại | `docs/decisions/` |
| Yêu cầu gốc của môn học | `SPEC.md` |

Tài liệu phân tích còn có bản HTML mở bằng trình duyệt, in ra PDF được:
`docs/session/tai-lieu-phan-tich-traingo.html`.

Mã nguồn ứng dụng nằm trong `app/`.

```powershell
cd app
Get-Service MySQL80      # MySQL Server local phải ở trạng thái Running
.\mvnw.cmd spring-boot:run
```

Ứng dụng chạy ở `http://localhost:8080`, kết nối MySQL Server local tại
`localhost:3306`, database `traingo`, theo `app/src/main/resources/application.properties`.
Xem `docs/RUNBOOK.md` để kiểm tra cấu hình và migration; không cần Docker.
