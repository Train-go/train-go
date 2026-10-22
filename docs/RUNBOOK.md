# Runbook: chạy TrainGo ở máy local

## Phạm vi và cấu hình

Ứng dụng Spring Boot nằm trong `app/`. Luồng vận hành hiện tại dùng **MySQL
Server cài trên Windows**, không cần Docker. Cấu hình có thẩm quyền nằm ở
`app/src/main/resources/application.properties`:

| Thành phần | Giá trị |
| --- | --- |
| Database | `traingo` trên `localhost:3306` |
| Windows service | `MySQL80`, phải ở trạng thái `Running` |
| Ứng dụng | `http://localhost:8080` |
| Build | Maven Wrapper trong `app/`; Java theo `pom.xml` |
| Schema | Flyway; Hibernate dùng `ddl-auto=validate` |

Tên đăng nhập và mật khẩu database lấy từ cấu hình datasource hoặc cấu hình
môi trường tương ứng; không chép thông tin bí mật vào tài liệu, log hay câu lệnh
được chia sẻ. Khi dùng MySQL client, dùng `-p` để nhập mật khẩu qua lời nhắc.

## Kiểm tra database trước khi chạy

```powershell
Get-Service MySQL80
Test-NetConnection localhost -Port 3306
```

Ngày 23/10/2026 đã kiểm tra service `MySQL80` đang `Running`, MySQL Server
8.0.46, schema `traingo` ở V5. Ảnh chụp trước migration có 8 booking, 11 vé,
tổng `tickets.ticket_price` là 3.660.000đ. Sau đó V6 đã áp thành công, Hibernate
validate qua và ứng dụng khởi động ở 8080. Snapshot mọi vé (hash các trường giữ
lại và giá), tổng tiền từng booking cùng số dòng trước/sau khớp hoàn toàn.

Nếu service chưa chạy, khởi động MySQL Server bằng công cụ quản lý Windows với
quyền thích hợp. Không dừng MySQL local để nhường cổng cho Docker. Nếu cổng 3306
không truy cập được, kiểm tra trạng thái service và cấu hình datasource trước.

## Biên dịch và chạy ứng dụng

Từ gốc repository:

```powershell
cd app
.\mvnw.cmd compile
.\mvnw.cmd spring-boot:run
```

Maven Wrapper dùng `JAVA_HOME`; kiểm tra Java thực tế nếu phiên bản trên PATH
và `JAVA_HOME` khác nhau. Lần chạy đầu có thể cần tải Maven và dependency.

Ứng dụng sẵn sàng khi log có `Started TrainGoApplication` và Tomcat nghe cổng
8080. Sau đó mở `http://localhost:8080` để kiểm tra trang thật. Dừng instance do
mình khởi động bằng `Ctrl+C` ở terminal đó; giữ MySQL Server đang chạy.

Nếu 8080 đang bị chiếm, xác định tiến trình trước khi xử lý:

```powershell
Get-NetTCPConnection -LocalPort 8080 -State Listen
Get-Process -Id <PID>
```

Không dừng tiến trình chưa xác định chủ sở hữu. Cổng ứng dụng khai trong
`app/src/main/resources/application.properties`.

## Flyway và bảo toàn dữ liệu

Migration nằm ở `app/src/main/resources/db/migration/`. Trên database trống,
Flyway chạy các migration theo thứ tự. Trên database hiện có, chỉ migration
chưa áp dụng được chạy. Không sửa V1–V5 đã chạy, không đổi checksum bằng tay,
không bật `ddl-auto=update`.

- V4 đưa schema xuống 8 bảng, gộp hành khách vào `tickets`, bỏ bảng toa/ghế và
  giữ chỗ tạm.
- V5 thêm lý trình ga, ngày-giờ chuyến và dữ liệu tuyến Bắc–Nam.
- V6 bỏ `tickets.date_of_birth`; bỏ CHECK `ck_trip_seats_price_modifier` trước
  khi bỏ `trip_seats.price_modifier`. V6 không cập nhật `ticket_price` hoặc
  `bookings.total_price`, không xóa booking/vé đã có.

Mỗi vé mới có giá bằng `Trip.basePrice`, dù chọn ghế hay giường. Giá vé cũ,
kể cả vé giường từng có phụ thu, vẫn giữ nguyên. Cấu hình toa bất biến trong
`CoachLayout`: toa 1–2 có A01–A32 (`SEAT`), toa 3–4 có B01–B16 (`SLEEPER`),
4 giường/cabin; tổng 96 chỗ mỗi chuyến. Hành khách chỉ nhập họ tên và CCCD.

Trước khi áp migration xóa cột, sao lưu database bằng công cụ MySQL phù hợp và
lưu bản sao ngoài repository. Khôi phục cần bản sao đó; không sửa migration
cũ để đảo ngược. Nếu Flyway báo checksum sai, khôi phục file migration gốc và
viết migration mới. Nếu Hibernate báo thiếu cột/sai kiểu, đối chiếu entity và
schema với lịch sử Flyway.

## Xem dữ liệu bằng MySQL client

Dùng MySQL client của bản MySQL Server đã cài (hoặc MySQL Workbench). Nếu
`mysql` không có trên PATH, chọn executable trong thư mục cài MySQL Server.
Lấy username từ cấu hình của máy:

```powershell
$databaseUser = Read-Host "Username trong cấu hình datasource"
mysql --host=localhost --port=3306 --user=$databaseUser -p --default-character-set=utf8mb4 traingo
```

Truy vấn chỉ đọc để đối chiếu:

```sql
SELECT VERSION();
SELECT version, description, success
FROM flyway_schema_history ORDER BY installed_rank;
SHOW COLUMNS FROM trip_seats;
SHOW COLUMNS FROM tickets;
SELECT COUNT(*) AS bookings FROM bookings;
SELECT COUNT(*) AS tickets, SUM(ticket_price) AS ticket_total FROM tickets;
```

Sau V6 phải không còn hai cột đã bỏ; số lượng booking/vé và giá từng vé cũ phải
khớp ảnh chụp trước migration. Khi đối chiếu giá, kiểm tra cả vé từng có phụ
thu; không dùng giá chuyến hiện tại để tính lại lịch sử.

## Dữ liệu thử

User demo đến từ V2/V3; V5 dựng lại ga, tuyến, tàu, chuyến và booking quanh trục
Bắc–Nam. Số dòng seed dự kiến: 12 user, 8 ga, 16 tuyến, 3 tàu, 18 chuyến,
1.728 chỗ, 8 booking, 11 vé. Dữ liệu tự thêm có thể làm số dòng khác đi.

Ngày chuyến được tính lúc seed chạy, nên danh sách chuyến tương lai sẽ cạn dần.
Khi module tạo chuyến (#28) sẵn sàng, tạo chuyến mới bằng luồng admin để thử;
không xóa/reset database đang có dữ liệu
chỉ để đổi ngày demo. Thông tin đăng nhập ứng dụng tạm lấy từ cấu hình security
hiện hành; tài khoản demo trong bảng `users` được mô tả ở migration V2/V3.

## Kiểm tra trước khi báo hoàn thành

Dự án không có test tự động (`docs/decisions/0007-bo-test-tu-dong.md`). Cần có
bằng chứng riêng cho biên dịch, khởi động trên MySQL và thử trên trình duyệt:

1. `compile` không lỗi.
2. Ứng dụng khởi động trên MySQL Server local; log Flyway và lịch sử schema khớp.
3. Đi hết “Kịch bản thử bằng tay” trong hướng dẫn task bằng dữ liệu thật.
4. Với thay đổi giá/hành khách: kiểm tra ghế và giường cùng giá chuyến, chỉ nhập
   họ tên + CCCD; đối chiếu vé/booking cũ giữ nguyên giá sau migration.

## Bằng chứng và lịch sử môi trường

Log ứng dụng nằm ở terminal chạy Maven; lỗi database xem trong công cụ/log của
MySQL Server local. Ghi rõ thời điểm và phiên bản schema khi thu thập bằng chứng.

Ngày 23/10/2026, issue #19: `compile` thành công với 44 source Java; V5 → V6
thành công trên MySQL Server 8.0.46. Đã thử trang chủ và sáu danh sách admin
(ga, tuyến, tàu, chuyến, booking, người dùng) ở 1440px và 390px: 14/14 lượt
trả 200, không lỗi JavaScript hoặc tràn ngang trang. Danh sách hiện đủ 8 booking.
Probe Java tạm kiểm tra đủ 96 chỗ, mọi khoang, tính bất biến, mã chỗ dưới locale
Ả Rập, sao chép TripSeat và giá/thời điểm Ticket. Không thêm bộ test vào repo.

Runbook cũ từng kiểm chứng Docker MySQL vào 08–10/2026, có thay đổi cổng giữa
3007 và 3306. Đó là lịch sử của môi trường trước; không phải hướng dẫn vận hành
hiện tại. Bằng chứng khởi động/browser từ môi trường cũ không chứng minh kết quả
trên MySQL Server local sau V6. Máy của các thành viên khác cần kiểm tra riêng.
