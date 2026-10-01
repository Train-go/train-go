# Runbook: chạy TrainGo ở máy local

Cấu trúc theo `docs/templates/application-runbook.md`. Mỗi lệnh đều ghi rõ đã
chạy thật hay chưa, để không ai tin nhầm một câu lệnh chưa được kiểm chứng.

## Phạm vi

Ứng dụng Spring Boot nằm trong thư mục `app/`. Runbook này phục vụ việc chạy ứng
dụng và bộ test ở máy cá nhân, không nói về triển khai production.

## Yêu cầu môi trường

| Thành phần | Trạng thái trên máy đã kiểm tra (23/08/2026) |
| --- | --- |
| JDK | `java` trên PATH là 17.0.12; `JAVA_HOME` trỏ vào JBR 21 của Android Studio |
| Maven | **Không có `mvn`** trên PATH. Dùng Maven Wrapper `mvnw` trong `app/` |
| Docker | CLI 29.7.2; Docker Desktop phải bật thủ công trước khi chạy ứng dụng |
| MySQL client | Không có trên PATH |
| Cổng 8080 | **Bị Apache của XAMPP chiếm.** Ứng dụng chạy ở 8081 |

Maven Wrapper dùng `JAVA_HOME` để chọn JDK, nên build hiện chạy bằng JDK 21 trong
khi `pom.xml` khai `java.version=17`. Build vẫn thành công, nhưng nếu gặp lỗi build
khó hiểu thì đây là nghi phạm đầu tiên. Cách xử lý sạch là cài một JDK riêng (17
hoặc 21) và trỏ `JAVA_HOME` vào đó thay vì mượn JDK của Android Studio.

## Khởi động database

```powershell
cd app
docker compose up -d
```

Compose khai báo trong `app/compose.yaml`:

| Mục | Giá trị |
| --- | --- |
| Image | `mysql:8.4` |
| Tên container | `traingo-mysql` |
| Cổng | host `3306` → container `3306` |
| Database | `traingo` |
| User / password ứng dụng | `traingo` / `traingo` |
| Volume | `traingo-mysql-data` |
| Múi giờ | `--default-time-zone=+07:00`, khớp `serverTimezone` trong JDBC URL |

**Đã kiểm chứng ngày 24/08/2026.** Container `traingo-mysql` lên và giữ trạng thái
`healthy`, cổng `0.0.0.0:3306->3306/tcp`.

**Đã kiểm chứng ngày 01/10/2026** sau khi thêm múi giờ: `docker compose up -d`
tạo lại container, volume giữ nguyên, `SELECT @@global.time_zone` trả `+07:00`.
Máy nào đang có container cũ chỉ cần chạy lại `docker compose up -d`.

Phải mở Docker Desktop trước. Nếu quên, lệnh thất bại với thông báo:

```
failed to connect to the docker API at npipe:////./pipe/dockerDesktopLinuxEngine
```

Đó là dấu hiệu Docker Desktop chưa bật, không phải lỗi cấu hình.

Kiểm tra container:

```powershell
docker compose ps
docker compose logs -f mysql
```

Dừng và giữ dữ liệu:

```powershell
docker compose down
```

Xóa cả dữ liệu (dùng khi cần làm sạch database, **mất toàn bộ dữ liệu đã seed**):

```powershell
docker compose down -v
```

## Chạy ứng dụng

```powershell
cd app
.\mvnw.cmd spring-boot:run
```

Ứng dụng nghe ở `http://localhost:8081`. Trang `/` là landing page đầy đủ theo
`SPEC.md` mục 5.

**Đã kiểm chứng ngày 24/08/2026:**

```
Tomcat started on port 8081 (http) with context path '/'
Started TrainGoApplication in 2.756 seconds
```

`GET /` trả 200. Bootstrap được phục vụ từ chính ứng dụng tại
`/vendor/bootstrap/bootstrap.min.css`, không lấy từ CDN.

### Vì sao cổng 8081 chứ không phải 8080

Apache của XAMPP trên máy dev đang giữ 8080, nên Tomcat nhúng không bind được và
ứng dụng chết ngay lúc khởi động với thông báo `Port 8080 was already in use`.
Cổng khai trong `app/src/main/resources/application.properties`:

```properties
server.port=8081
```

Muốn biết tiến trình nào đang giữ một cổng:

```powershell
netstat -ano | findstr :8080
Get-Process -Id <PID>
```

### Flyway tạo bảng và seed data lúc khởi động

Schema và dữ liệu demo nằm trong `app/src/main/resources/db/migration/`. Lần đầu
chạy trên database trống, Flyway tạo 11 bảng rồi nạp seed. Những lần sau nó chỉ
chạy migration mới chưa từng chạy. Sau đó Hibernate kiểm tra entity khớp schema
(`ddl-auto=validate`). Lý do và quy ước: `docs/decisions/0005-quan-ly-schema-bang-flyway.md`.

**Đã kiểm chứng ngày 01/10/2026** trên database trống:

```
Migrating schema `traingo` to version "1 - create schema"
Migrating schema `traingo` to version "2 - seed demo data"
Successfully applied 2 migrations to schema `traingo`, now at version v2 (execution time 00:00.564s)
Started TrainGoApplication in 5.534 seconds
```

Hai lỗi dễ gặp (chưa gặp thật trong dự án, nên chưa trích được nguyên văn
thông báo):

- Flyway báo checksum của một migration không khớp: ai đó đã sửa một migration
  đã chạy. Trả file về như cũ và viết migration mới. Nếu chỉ là máy dev, có thể
  reset database như mục "Làm mới dữ liệu demo".
- Hibernate báo schema validation thiếu cột hoặc sai kiểu cột: entity và
  migration lệch nhau. Sửa cho khớp, không bật lại `ddl-auto=update`.

### Ứng dụng cần MySQL đang chạy

Flyway mở kết nối ngay lúc khởi động. Không có MySQL thì ứng dụng chết với:

```
org.hibernate.HibernateException: Unable to determine Dialect without JDBC metadata
```

Thông báo này nói về Hibernate nhưng nguyên nhân thật là không kết nối được
database. Kiểm tra `docker ps` trước khi đi tìm lỗi trong code.

Vì `spring-boot-devtools` có trong dependency, ứng dụng tự khởi động lại khi file
class trong `target/classes` thay đổi. Trong IntelliJ hoặc VS Code, việc này xảy
ra khi bạn build lại project.

## Chạy test

```powershell
cd app
.\mvnw.cmd test
```

**Đã kiểm chứng ngày 24/08/2026**: `BUILD SUCCESS`, `Tests run: 6, Failures: 0,
Errors: 0, Skipped: 0`.

Bộ test dùng H2 in-memory khai báo trong `app/src/test/resources/application.properties`,
nên **không cần Docker và không cần MySQL** để chạy test.

Các biến thể hay dùng:

```powershell
.\mvnw.cmd test -Dtest=BookingServiceTest
.\mvnw.cmd test "-Dtest=BookingServiceTest#confirmBooking_shouldFail_whenSeatAlreadyBooked"
.\mvnw.cmd clean test
```

Lần chạy đầu tiên trên một máy mới sẽ lâu vì Maven Wrapper tải Maven 3.9.16 và
toàn bộ dependency về `~/.m2/repository`.

## Dữ liệu thử

Seed data nằm trong `V2__seed_demo_data.sql`:

| Bảng | Số dòng | Nội dung |
| --- | --- | --- |
| `users` | 4 | 1 admin, 3 khách |
| `stations` | 8 | Sài Gòn, Nha Trang, Quy Nhơn, Đà Nẵng, Huế, Vinh, Hà Nội, Lào Cai |
| `routes` | 8 | SGN-NTR, SGN-DNG, HNI-DNG, HNI-LCI, mỗi cặp hai chiều |
| `trains` | 8 | Mỗi chiều một tàu: SNT1/SNT2, SE22/SE21, SE19/SE20, SP1/SP2 |
| `coaches`, `seats` | 32, 768 | Mỗi tàu: toa 1-2 ghế 32 chỗ, toa 3-4 giường 16 chỗ (+200.000đ) |
| `trips` | 114 | Mỗi tàu một chuyến mỗi ngày trong 14 ngày tới; 2 chuyến đã chạy; SP1 ngày thứ 5 bị hủy |
| `trip_seats` | 10.944 | Đủ chỗ cho mọi chuyến |
| `bookings` | 4 | 2 sắp đi, 1 đã đi, 1 đã hủy, đều đã thanh toán |

Tài khoản:

| Email | Mật khẩu | Role |
| --- | --- | --- |
| `admin@traingo.vn` | `Admin@123` | `ADMIN` |
| `an.nguyen@example.com`, `binh.tran@example.com`, `cuong.le@example.com` | `Customer@123` | `CUSTOMER` |

Cho tới khi làm xong Auth, trang login vẫn dùng tài khoản tạm `admin` / `admin`
trong `application.properties`. Các tài khoản trên chỉ mới nằm trong bảng
`users`.

Ga Quy Nhơn, Huế và Vinh không có tuyến nào, nên xóa được. Các ga còn lại đang
được tuyến dùng nên màn hình sẽ từ chối xóa.

### Làm mới dữ liệu demo

Ngày của chuyến tính từ lúc seed chạy, nên sau 14 ngày danh sách chuyến sắp tới
sẽ cạn. Reset database rồi chạy lại ứng dụng. **Lệnh đầu xóa toàn bộ dữ liệu
trong database dev**, kể cả dữ liệu bạn tự thêm:

```powershell
cd app
docker compose down -v
docker compose up -d
.\mvnw.cmd spring-boot:run
```

Flyway thấy database trống nên chạy lại V1 và V2 với ngày mới.

### Xem dữ liệu trong database

Máy dev không có MySQL client, nên dùng client có sẵn trong container:

```powershell
docker exec -it traingo-mysql mysql -utraingo -ptraingo --default-character-set=utf8mb4 traingo
```

Ví dụ `SHOW TABLES;` hoặc `SELECT * FROM flyway_schema_history;`. Phải có
`--default-character-set=utf8mb4`, nếu không chữ tiếng Việt sẽ hiện sai.

## Bằng chứng khi có sự cố

- Log ứng dụng in ra console nơi chạy `spring-boot:run`.
- Log database: `docker compose logs mysql`.
- Báo cáo test: `app/target/surefire-reports/`, mỗi class test một file `.txt`
  và một file `.xml`.

## Điều chưa biết

- Migration mới chỉ được chạy trên MySQL 8.4, chưa chạy trên H2 của bộ test.
- Chưa kiểm chứng trên máy của hai thành viên còn lại. Cổng 8081 là để né XAMPP
  trên máy này; máy khác có thể không cần, nhưng để nguyên thì cả nhóm dùng chung
  một cổng.
- `JAVA_HOME` vẫn trỏ vào JBR của Android Studio. Build chạy được nhưng đây là
  chỗ nên dọn.
