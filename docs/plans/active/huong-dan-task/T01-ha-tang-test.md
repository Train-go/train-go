# T01 Hạ tầng test

| Wave | Lane | Cỡ | Phụ thuộc | Mở khóa | Branch |
| --- | --- | --- | --- | --- | --- |
| 1 | B | S | Không | Test của mọi task sau, bean `Clock` cho T05 | `feat/t01-test-setup` |

## Mục tiêu

`.\mvnw.cmd test` chạy được trên H2 in-memory, không cần Docker hay MySQL
(`docs/decisions/0002-chien-luoc-test.md`), với schema do chính Flyway tạo. Có sẵn
một bean `Clock` để mọi task sau giả được "bây giờ" trong test.

Hiện trạng ngày 04/10/2026: thư mục `app/src/test` đã bị xóa trong working tree,
không còn file cấu hình H2 nào. `.\mvnw.cmd test` báo `BUILD SUCCESS` với 0 test.

## Phạm vi

Trong phạm vi:

- File cấu hình test cho H2.
- Bean `Clock` dùng chung.
- Một test khói (smoke test) chứng minh migration chạy được trên H2.

Ngoài phạm vi:

- Viết test cho từng tính năng: mỗi task tự viết test của mình.
- Khôi phục `AdminControllerTest`, `HomeControllerTest` đã bị xóa: hỏi Phi trước.
  Hai test này từng chặn link CDN và branding của template gốc lọt lại vào trang.

## Database

Không đổi schema. Test dùng đúng hai migration của ứng dụng:

```
src/main/resources/db/migration/V1__create_schema.sql   11 bảng
src/main/resources/db/migration/V2__seed_demo_data.sql  dữ liệu demo
```

Hai file này mới chỉ chạy trên MySQL 8.4. Việc chính của task là biết chúng có chạy
trên H2 chế độ MySQL hay không.

## Các file cần viết

### `app/src/test/resources/application.properties`

```properties
spring.datasource.url=jdbc:h2:mem:traingo;MODE=MySQL;DB_CLOSE_DELAY=-1
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.open-in-view=false
```

- Giữ `ddl-auto=validate` như ứng dụng thật, để test cũng bắt được entity lệch schema.
- Nếu `V2` không chạy được trên H2 vì hàm SQL riêng của MySQL, thêm
  `spring.flyway.target=1`. Khi đó test chỉ có schema, mỗi test tự tạo dữ liệu.
  **Không sửa `V2`**: máy cả nhóm đã chạy file này, sửa là Flyway từ chối khởi động.

### `common/config/TimeConfig.java`

```java
@Configuration
public class TimeConfig {

    public static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    @Bean
    Clock clock() {
        return Clock.system(ZONE);
    }
}
```

Service cần thời gian hiện tại thì nhận `Clock` qua constructor và gọi
`LocalDateTime.now(clock)`. Trong test, truyền
`Clock.fixed(Instant.parse("2026-10-05T03:00:00Z"), TimeConfig.ZONE)`.

### `src/test/java/com/voyagego/traingo/MigrationSmokeTest.java`

Một `@DataJpaTest` kiểm tra Flyway đã chạy và dữ liệu seed có mặt:

```java
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class MigrationSmokeTest {

    @Autowired StationRepository stationRepository;
    @Autowired TripSeatRepository tripSeatRepository;

    @Test
    void migrations_createSchemaAndSeedOnH2() {
        assertThat(stationRepository.count()).isEqualTo(8);
        assertThat(tripSeatRepository.count()).isEqualTo(10944);
    }
}
```

- `Replace.NONE` bắt test dùng URL H2 ở trên (có `MODE=MySQL`), không để Spring tự
  thay bằng một H2 mặc định.
- Nếu phải dùng `spring.flyway.target=1`, đổi test thành kiểm tra 11 bảng tồn tại và
  bảng `stations` đang trống.

## Bẫy đã biết

- Spring Boot 4 chuyển package của annotation test. Tìm `AutoConfigureTestDatabase`
  và `DataJpaTest` trong jar thật ở `~/.m2/repository` trước khi tin ví dụ trên mạng
  (`AGENTS.md`, mục "Bẫy của Spring Boot 4").
- Nếu test báo bảng không tồn tại, nghĩa là Flyway chưa chạy trong slice
  `@DataJpaTest`. Kiểm tra trong jar `spring-boot-flyway` xem slice có nạp
  auto-config của Flyway không, rồi thêm `@ImportAutoConfiguration` nếu cần.
- Class test phải kết thúc bằng `Test`, nếu không Surefire bỏ qua mà không báo lỗi.

## Xong khi

- [ ] Tắt Docker Desktop, chạy `.\mvnw.cmd test` vẫn xanh, có ít nhất 1 test chạy.
- [ ] Ghi rõ trong `docs/RUNBOOK.md`, mục "Chạy test": `V2` chạy được trên H2 hay
      phải dùng `target=1`.
- [ ] Bỏ dòng "Migration chưa chạy trên H2" ở `docs/RUNBOOK.md` và ADR 0005 nếu đã
      kiểm chứng xong.
- [ ] Một dòng `docs/WORKLOG.md` trong cùng commit.
