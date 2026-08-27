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

**Đã kiểm chứng ngày 24/08/2026.** Container `traingo-mysql` lên và giữ trạng thái
`healthy`, cổng `0.0.0.0:3306->3306/tcp`.

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

### Ứng dụng cần MySQL đang chạy

`spring.jpa.hibernate.ddl-auto=update` mở kết nối ngay lúc khởi động. Không có
MySQL thì ứng dụng chết với:

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

Chưa có seed data. Chưa có tài khoản admin. Chưa có ga, tuyến, tàu hay chuyến mẫu.
Đây là việc thuộc kế hoạch trong `docs/plans/active/`, không phải thứ có thể suy ra
từ code hiện tại.

## Bằng chứng khi có sự cố

- Log ứng dụng in ra console nơi chạy `spring-boot:run`.
- Log database: `docker compose logs mysql`.
- Báo cáo test: `app/target/surefire-reports/`, mỗi class test một file `.txt`
  và một file `.xml`.

## Điều chưa biết

- Chưa có quy trình seed dữ liệu. Chưa có tài khoản admin, ga, tuyến, tàu hay
  chuyến mẫu.
- Chưa quyết định cách quản lý schema khi dự án lớn hơn: tiếp tục dùng
  `ddl-auto=update` hay chuyển sang Flyway.
- Chưa kiểm chứng trên máy của hai thành viên còn lại. Cổng 8081 là để né XAMPP
  trên máy này; máy khác có thể không cần, nhưng để nguyên thì cả nhóm dùng chung
  một cổng.
- `JAVA_HOME` vẫn trỏ vào JBR của Android Studio. Build chạy được nhưng đây là
  chỗ nên dọn.
