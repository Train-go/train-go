# Runbook: chạy TrainGo ở máy local

Cấu trúc theo `docs/templates/application-runbook.md`. Mỗi lệnh đều ghi rõ đã
chạy thật hay chưa, để không ai tin nhầm một câu lệnh chưa được kiểm chứng.

## Phạm vi

Ứng dụng Spring Boot nằm trong thư mục `app/`. Runbook này phục vụ việc chạy ứng
dụng ở máy cá nhân, không nói về triển khai production.

## Yêu cầu môi trường

| Thành phần | Trạng thái trên máy đã kiểm tra (23/08/2026) |
| --- | --- |
| JDK | `java` trên PATH là 17.0.12; `JAVA_HOME` trỏ vào JBR 21 của Android Studio |
| Maven | **Không có `mvn`** trên PATH. Dùng Maven Wrapper `mvnw` trong `app/` |
| Docker | CLI 29.7.2; Docker Desktop phải bật thủ công trước khi chạy ứng dụng |
| MySQL client | Không có trên PATH |
| Cổng 8080 | Ứng dụng chạy ở đây. Nếu Apache của XAMPP đang bật thì phải tắt trước |
| Cổng 3306 | MySQL Docker dùng cổng này. Service Windows `MySQL97` cũng giữ 3306, phải dừng trước |

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
| Cổng | host `3306` → container `3306` (đã đổi lại từ 3007 ngày 19/10/2026, xem bên dưới) |
| Database | `traingo` |
| User / password ứng dụng | `traingo` / `traingo` |
| Volume | `traingo-mysql-data` |
| Múi giờ | `--default-time-zone=+07:00`, khớp `serverTimezone` trong JDBC URL |

**Đã kiểm chứng ngày 24/08/2026.** Container `traingo-mysql` lên và giữ trạng thái
`healthy`, cổng `0.0.0.0:3306->3306/tcp`.

**Đã kiểm chứng ngày 01/10/2026** sau khi thêm múi giờ: `docker compose up -d`
tạo lại container, volume giữ nguyên, `SELECT @@global.time_zone` trả `+07:00`.
Máy nào đang có container cũ chỉ cần chạy lại `docker compose up -d`.

**Cổng phía máy: 3007 (12/10/2026), đổi lại 3306 (19/10/2026).** Máy dev có MySQL cài
sẵn chạy dạng service Windows `MySQL97`, giữ cổng 3306, nên container không mở được
cổng và báo:

```
ports are not available: exposing port TCP 0.0.0.0:3306 -> 127.0.0.1:0: listen tcp 0.0.0.0:3306: bind: Only one usage of each socket address (protocol/network address/port) is normally permitted.
```

Từ 19/10/2026 `compose.yaml` và JDBC URL trong `application.properties` dùng lại 3306.
Trước khi `docker compose up -d`, dừng service MySQL local bằng PowerShell chạy với
quyền Administrator: `Stop-Service MySQL97` (muốn không tự bật lại khi khởi động máy:
`Set-Service MySQL97 -StartupType Manual`). Volume và dữ liệu Docker giữ nguyên. Lệnh
`docker exec` ở mục "Xem dữ liệu trong database" chạy bên trong container nên không đổi.

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

Ứng dụng nghe ở `http://localhost:8080`. Trang `/` là landing page đầy đủ theo
`SPEC.md` mục 5.

**Đã kiểm chứng ngày 24/08/2026:**

```
Tomcat started on port 8080 (http) with context path '/'
Started TrainGoApplication in 2.756 seconds
```

`GET /` trả 200. Bootstrap được phục vụ từ chính ứng dụng tại
`/vendor/bootstrap/bootstrap.min.css`, không lấy từ CDN.

### Nếu cổng 8080 đang bị chiếm

Nếu Apache của XAMPP (hoặc tiến trình khác) đang giữ 8080, Tomcat nhúng không bind được
và ứng dụng chết ngay lúc khởi động với thông báo `Port 8080 was already in use`. Cổng
khai trong `app/src/main/resources/application.properties`:

```properties
server.port=8080
```

Muốn biết tiến trình nào đang giữ một cổng:

```powershell
netstat -ano | findstr :8080
Get-Process -Id <PID>
```

### Flyway tạo bảng và seed data lúc khởi động

Schema và dữ liệu demo nằm trong `app/src/main/resources/db/migration/`. Lần đầu
chạy trên database trống, Flyway tạo 8 bảng rồi nạp seed. Những lần sau nó chỉ
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

## Kiểm tra trước khi báo xong

Dự án không có test tự động (`docs/decisions/0007-bo-test-tu-dong.md`). Trước khi
mở pull request:

```powershell
cd app
.\mvnw.cmd compile          # code biên dịch được
.\mvnw.cmd spring-boot:run  # ứng dụng khởi động được trên MySQL
```

Sau đó đi hết mục "Kịch bản thử bằng tay" trong file hướng dẫn của task, trên trình
duyệt với dữ liệu mẫu.

Lần chạy đầu tiên trên một máy mới sẽ lâu vì Maven Wrapper tải Maven 3.9.16 và
toàn bộ dependency về `~/.m2/repository`.

## Dữ liệu thử

Người dùng nằm trong `V2__seed_demo_data.sql` và `V3__seed_more_demo_data.sql`.
Ga, tuyến, tàu, chuyến và booking nằm trong `V5__north_south_line.sql`: V5 xóa dữ
liệu cũ của V2, V3 rồi dựng lại quanh một trục Bắc - Nam
(`docs/decisions/0011-chuyen-tao-tu-tau-noi-chang.md`):

| Bảng | Số dòng | Nội dung |
| --- | --- | --- |
| `users` | 12 | 1 admin, 11 khách (3 ở V2, 8 ở V3) |
| `stations` | 8 | Hà Nội, Thanh Hóa, Vinh, Đồng Hới, Huế, Đà Nẵng, Nha Trang, Sài Gòn, có lý trình từ km 0 đến km 1726 |
| `routes` | 16 | 7 chặng giữa hai ga kề nhau, mỗi chặng hai chiều, cộng HNI-SGN hai chiều |
| `trains` | 3 | SE1, SE2, SE3; chỉ SE1 có chuyến |
| `trips` | 18 | SE1 mỗi ngày một chặng, khởi hành 08:00, từ 4 ngày trước lúc seed chạy: 7 ngày xuống Nam, 7 ngày lên Bắc, rồi lặp lại. 4 chuyến đầu đã chạy (`COMPLETED`) |
| `trip_seats` | 1.728 | 96 chỗ mỗi chuyến, theo cấu hình toa cố định: toa 1-2 ghế 32 chỗ, toa 3-4 giường 16 chỗ (+200.000đ) |
| `bookings` | 8 | Đều trên chuyến của SE1: 2 trên chuyến đã chạy, 5 sắp đi, 1 đã hủy; đều đã thanh toán |
| `tickets` | 11 | Mỗi hành khách một chỗ và một mã vé |

Tài khoản:

| Email | Mật khẩu | Role |
| --- | --- | --- |
| `admin@traingo.vn` | `Admin@123` | `ADMIN` |
| `an.nguyen@example.com`, `binh.tran@example.com`, `cuong.le@example.com` | `Customer@123` | `CUSTOMER` |
| `dung.pham@example.com`, `duc.vu@example.com`, `lan.hoang@example.com`, `huy.dang@example.com`, `ha.bui@example.com`, `son.ngo@example.com`, `linh.do@example.com`, `bao.ly@example.com` | `Customer@123` | `CUSTOMER` |

Cho tới khi làm xong Auth, trang login vẫn dùng tài khoản tạm `admin` / `admin`
trong `application.properties`. Các tài khoản trên chỉ mới nằm trong bảng
`users`.

Ga nào trong dữ liệu mẫu cũng có tuyến, nên màn hình từ chối xóa. Muốn thử xóa
thì thêm một ga mới (ví dụ Quảng Ngãi, km 928) rồi xóa nó.

### Làm mới dữ liệu demo

Ngày của chuyến tính từ lúc seed chạy, nên khoảng 2 tuần sau danh sách chuyến sắp
tới sẽ cạn. Reset database rồi chạy lại ứng dụng. **Lệnh đầu xóa toàn bộ dữ liệu
trong database dev**, kể cả dữ liệu bạn tự thêm:

```powershell
cd app
docker compose down -v
docker compose up -d
.\mvnw.cmd spring-boot:run
```

Flyway thấy database trống nên chạy lại V1 đến V4 với ngày mới.

Máy đã chạy V2 từ trước thì lần chạy ứng dụng đầu tiên sau khi pull chỉ áp V3. V3 gắn
booking vào chuyến thứ N còn sắp chạy của từng tàu, nên nếu V2 đã chạy nhiều ngày trước,
những booking cần chuyến ở xa sẽ không được tạo. **Đã gặp ngày 12/10/2026** trên database
seed ngày 02/10: mỗi tàu chỉ còn 4 chuyến sắp chạy, V3 tạo 8 trong 16 booking, nên
`bookings` có 12 dòng thay vì 20. Muốn đủ số liệu như bảng trên thì làm mới dữ liệu như
các lệnh ở đầu mục này.

V4 (19/10/2026) đưa database từ 11 xuống 8 bảng, gộp ba thay đổi (ADR 0008, 0009, 0010):
gộp `booking_passengers` và `tickets` thành `tickets`; bỏ `coaches` và `seats` (mỗi dòng
`trip_seats` giữ `coach_number`, `coach_type`, `seat_code`, `cabin_number`, `price_modifier`, và cột
`seat_id` biến mất); bỏ giữ chỗ (`trip_seats` mất `held_by_user_id`, `held_until`, trạng thái `HELD`,
còn `tickets.ticket_code` và `issued_at` là `NOT NULL`). Máy đã chạy V3 chỉ cần pull rồi chạy ứng dụng,
Flyway tự áp V4 và giữ nguyên dữ liệu. V4 xóa bảng và cột nên không đảo ngược được bằng migration:
nếu cần dữ liệu cũ, hãy `mysqldump` trước khi chạy ứng dụng lần đầu sau khi pull. Truy vấn cũ dùng
`booking_passengers`, `seats`, `coaches`, `trip_seats.seat_id`, `held_by_user_id` hoặc `held_until` sẽ lỗi.

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
## Điều chưa biết

- Chưa kiểm chứng trên máy của hai thành viên còn lại. Máy này phải tắt XAMPP
  Apache và service `MySQL97` để rảnh cổng 8080 và 3306; máy khác có thể không cần.
- `JAVA_HOME` vẫn trỏ vào JBR của Android Studio. Build chạy được nhưng đây là
  chỗ nên dọn.
