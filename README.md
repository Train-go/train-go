# TrainGo

TrainGo là website đặt vé tàu: tìm chuyến, chọn ghế hoặc giường trên sơ đồ toa,
thanh toán mô phỏng và nhận vé điện tử.

`voyage-go` là tên repository trên GitHub; tên sản phẩm hiển thị ở mọi nơi là
**TrainGo** — xem `docs/decisions/0003-he-thong-giao-dien.md`.

## Bắt đầu từ đâu

| Bạn muốn | Đọc file |
| --- | --- |
| Nắm toàn bộ dự án trong một lần đọc | `docs/session/tai-lieu-phan-tich-traingo.md` |
| Hiểu sản phẩm phải làm gì | `docs/product/overview.md` |
| Hiểu dữ liệu và quan hệ giữa các entity | `docs/product/domain-model.md` |
| Hiểu luồng đặt vé, giữ chỗ, thanh toán | `docs/product/booking-flow.md` |
| Chạy ứng dụng, database và test ở máy mình | `docs/RUNBOOK.md` |
| Biết đang làm tới đâu và tiếp theo làm gì | `docs/plans/active/mvp-traingo.md` |
| Biết vì sao dự án được cấu trúc như hiện tại | `docs/decisions/` |
| Yêu cầu gốc của môn học | `SPEC.md` |

Tài liệu phân tích còn có bản HTML mở bằng trình duyệt, in ra PDF được:
`docs/session/tai-lieu-phan-tich-traingo.html`.

Mã nguồn ứng dụng nằm trong `app/`.

```powershell
cd app
docker compose up -d      # MySQL cho môi trường dev
.\mvnw.cmd spring-boot:run
.\mvnw.cmd test
```

Ứng dụng chạy ở `http://localhost:8081`.
