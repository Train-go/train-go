# T02 Đăng ký, đăng nhập, phân quyền

> **Tóm tắt:** sau task này, khách tự đăng ký tài khoản, đăng nhập bằng email, đăng xuất;
> mỗi người chỉ vào được những trang dành cho vai trò của mình.

| Đợt | Phần việc | Độ khó | Cần xong trước | Task đang chờ task này | Tên branch |
| --- | --- | --- | --- | --- | --- |
| 1 | C | Khó (khoảng 4-5 ngày) | Quyết định D4 | T09, T10, T11, T12 | `feat/t02-auth` |

Các từ như session, CSRF token, 401, 403 được giải thích trong `README.md` cùng thư mục.
Cách chia package theo tầng (`controller/`, `service/`, `model/request/`...) nằm ở
`docs/decisions/0006-cau-truc-package-theo-tang.md`.

## 1. Task này làm gì

- Khách đăng ký tài khoản, đăng nhập bằng email và mật khẩu, đăng xuất.
- Hệ thống biết ai đang đăng nhập ở mọi trang: thanh menu (navbar) đổi theo trạng thái,
  header trang quản trị hiện tên thật của admin.
- Backend chặn theo vai trò (role). Khách hàng (Customer) gõ tay `/admin/stations` thì bị
  chặn. Người chưa đăng nhập vào trang đặt vé thì được đưa tới trang đăng nhập.
- Bỏ tài khoản tạm `admin` / `admin`. Từ nay đăng nhập bằng tài khoản trong bảng `users`.

Task này phải làm sớm nhất có thể: T09, T10, T11, T12 đều cần biết ai đang đăng nhập.
Task này chậm thì cả chuỗi đặt vé chậm theo.

## 2. Phạm vi

Làm:

- Đăng ký, đăng nhập, đăng xuất, giữ phiên đăng nhập (session).
- Luật phân quyền theo role cho **mọi** đường dẫn đang có và sắp có.
- Navbar và header trang quản trị.

Không làm (theo `SPEC.md` mục 41): quên mật khẩu, đặt lại mật khẩu, xác thực email, đăng
nhập Google, OAuth, trang hồ sơ.

## 3. Dữ liệu dùng tới

Chỉ dùng bảng `users`, đã có từ V1:

```
users
  id            BIGINT PK
  full_name     VARCHAR(100)  NOT NULL
  email         VARCHAR(255)  NOT NULL, UNIQUE (uk_users_email)
  password_hash VARCHAR(100)  NOT NULL     BCrypt, dạng $2a$10$...
  role          VARCHAR(20)   NOT NULL     CUSTOMER | ADMIN
  created_at    DATETIME      NOT NULL

Được tham chiếu bởi: bookings.customer_id
```

Đọc sơ đồ: mỗi người dùng có họ tên, email không trùng, mật khẩu đã mã hóa bằng BCrypt
(không bao giờ lưu mật khẩu gốc) và một role: `CUSTOMER` hoặc `ADMIN`. Bảng `bookings` trỏ về bảng
này để biết booking của ai.

Dữ liệu mẫu có sẵn `admin@traingo.vn` / `Admin@123` và các khách hàng mật khẩu
`Customer@123` (`docs/RUNBOOK.md`). Email luôn được lưu chữ thường.

## 4. Các bước làm

### Bước 1. Model: tạo request đăng ký

Entity `entity/User` và `entity/UserRole` đã có, **không sửa field**.

Tạo class mới `model/request/RegisterRequest`:

| Field | Luật kiểm tra |
| --- | --- |
| `fullName` | `@NotBlank`, `@Size(max = 100)` |
| `email` | `@NotBlank`, `@Email`, `@Size(max = 255)` |
| `password` | `@NotBlank`, `@Size(min = 8, max = 72)` (D4; BCrypt chỉ dùng 72 byte đầu) |
| `confirmPassword` | `@NotBlank` |

Việc so `password` với `confirmPassword` làm ở controller (bước 4), rồi gắn lỗi vào ô
`confirmPassword`. Làm vậy để thông báo "không khớp" hiện ngay dưới ô nhập lại.

### Bước 2. Repository: tìm người dùng theo email

Thêm vào `repository/UserRepository`:

```java
Optional<User> findByEmail(String email);
boolean existsByEmail(String email);
```

### Bước 3. Service: người đang đăng nhập và việc đăng ký

**`service/TrainGoUserDetails`** là class implement `UserDetails`. Đây là thông tin về người
đang đăng nhập mà Spring Security giữ trong session. Các task sau đều dùng class này, nên
đây là **quy ước chung** của cả nhóm:

```java
public class TrainGoUserDetails implements UserDetails {
    private final Long id;
    private final String email;
    private final String fullName;
    private final String passwordHash;
    private final UserRole role;

    public Long getId() { ... }
    public String getFullName() { ... }
    public UserRole getRole() { ... }
    // getUsername() trả email, getPassword() trả passwordHash,
    // getAuthorities() trả List.of(new SimpleGrantedAuthority("ROLE_" + role))
}
```

Controller của task khác lấy người đang đăng nhập bằng tham số
`@AuthenticationPrincipal TrainGoUserDetails me`, rồi dùng `me.getId()` để kiểm tra
booking có đúng của người đó không. **Không** nhận id người dùng từ form hay đường dẫn,
vì người dùng có thể tự sửa số đó để xem dữ liệu của người khác.

**`service/TrainGoUserDetailsService`** implement `UserDetailsService`. Spring Security gọi
nó khi có người đăng nhập:

- `loadUserByUsername(String email)`: chuẩn hóa email bằng `trim().toLowerCase(Locale.ROOT)`,
  tìm bằng `findByEmail`, không thấy thì ném `UsernameNotFoundException`.

**`service/RegistrationService`**:

- `@Transactional User register(RegisterRequest form)`: chuẩn hóa email; nếu
  `existsByEmail` thì ném `DuplicateEmailException` (đặt trong `common/exception/`); mã hóa
  mật khẩu bằng `PasswordEncoder`; rồi lưu `new User(fullName, email, hash, UserRole.CUSTOMER)`.
- Không bao giờ ghi log hay trả về mật khẩu gốc.

### Bước 4. Controller: trang đăng nhập và đăng ký

Tạo `controller/AuthController`:

| Method | Đường dẫn | Handler | Kết quả |
| --- | --- | --- | --- |
| GET | `/login` | `loginPage()` | Trang `auth/login` |
| GET | `/register` | `registerForm(Model)` | Trang `auth/register` với `RegisterRequest` rỗng |
| POST | `/register` | `register(@Valid RegisterRequest, BindingResult, RedirectAttributes)` | Lỗi thì hiện lại form. Xong thì chuyển tới `redirect:/login?registered` |

Bạn **không** viết handler cho `POST /login` và `POST /logout`: Spring Security tự xử lý
hai đường dẫn này.

### Bước 5. SecurityConfig: luật phân quyền cho mọi đường dẫn

Task này là chủ của file `common/config/SecurityConfig`. Viết lại nó với bảng quyền cho
**mọi** đường dẫn của MVP, kể cả đường dẫn chưa có. Nhờ vậy các task sau không phải sửa
file này, và không ai giẫm lên code của ai:

| Đường dẫn | Ai được vào |
| --- | --- |
| `/admin/**`, `/api/admin/**` | `hasRole("ADMIN")` |
| `/booking/**`, `/my-trips/**`, `/tickets/**` | `hasRole("CUSTOMER")` |
| Mọi đường dẫn còn lại, gồm `/`, `/trips/**`, `/api/trips/**`, `/login`, `/register`, `/error` và file tĩnh | Ai cũng vào được |

Dòng cuối là "Ai cũng vào được" chứ không phải "Đã đăng nhập": người chưa đăng nhập gõ
sai đường dẫn thấy trang 404, không bị đẩy về trang đăng nhập. `/api/trips/**` công
khai vì sơ đồ chỗ chỉ để xem, giống nhau với mọi người (chốt 24/10/2026, khi gộp #21 với
#22). Lý do và cái giá của lựa chọn này:
`docs/decisions/0013-quyen-truy-cap-mac-dinh-mo.md`.

Cấu hình thêm:

- Bean `PasswordEncoder` trả `new BCryptPasswordEncoder()`. Mật khẩu trong dữ liệu mẫu là
  BCrypt thuần, nên không dùng `DelegatingPasswordEncoder`.
- `formLogin`: `loginPage("/login")`, `usernameParameter("email")`,
  `defaultSuccessUrl("/", false)`, `failureUrl("/login?error")`, `permitAll()`.
- `logout`: `logoutSuccessUrl("/?logout")`. Đăng xuất phải là `POST`, nên nút đăng xuất
  là một form có `th:action` (để có CSRF token).
- Request tới `/api/**` khi chưa đăng nhập phải trả **401**, không chuyển sang trang login.
  Vì sao: nếu chuyển trang, đoạn JavaScript `fetch` ở T06, T09 nhận về HTML của trang login
  với mã 200 và tưởng là thành công. Dùng
  `exceptionHandling(...).defaultAuthenticationEntryPointFor(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED), <matcher cho /api/**>)`.
  Spring Security 7 đã bỏ `AntPathRequestMatcher`; hãy tìm `PathPatternRequestMatcher`
  trong jar thật.
- Xóa ba dòng `spring.security.user.*` trong `application.properties`.

### Bước 6. Giao diện: trang đăng nhập, đăng ký, navbar, header

- `pom.xml`: thêm `org.thymeleaf.extras:thymeleaf-extras-springsecurity6`. Spring Boot
  4.1.1 đã quản lý version, nên không cần ghi version.
- `templates/auth/login.html`, `templates/auth/register.html`: dùng khung trang khách
  `layout/base.html` và `app.css`, **không** dùng khung admin. Trang login hiện thông báo
  theo đường dẫn: `?error` thì "Email hoặc mật khẩu không đúng", `?registered` thì "Đăng ký
  thành công, mời đăng nhập", và có thông báo cho `?logout`.
- `templates/fragments/navbar.html` (chủ sở hữu: T02): khai
  `xmlns:sec="http://www.thymeleaf.org/extras/spring-security"` rồi hiện theo trạng thái:

| Trạng thái | Cách kiểm tra trong template | Navbar hiện |
| --- | --- | --- |
| Chưa đăng nhập | `sec:authorize="isAnonymous()"` | Đăng nhập, Đăng ký |
| Đã đăng nhập | `sec:authorize="isAuthenticated()"` | Chuyến của tôi, tên người dùng (`sec:authentication="principal.fullName"`), nút Đăng xuất (form `POST /logout`) |
| Là admin | `sec:authorize="hasRole('ADMIN')"` | Thêm link Quản trị tới `/admin` |

- `templates/admin/fragments/header.html`: thay `${adminName}` bằng
  `sec:authentication="principal.fullName"`, rồi xóa file
  `controller/AdminShellModelAdvice.java`.

## 5. Luật và lỗi phải xử lý

- Email đã có (không phân biệt hoa thường): báo lỗi ngay dưới ô email.
- Mật khẩu nhập lại không khớp: báo lỗi dưới ô nhập lại.
- Sai mật khẩu và email không tồn tại hiện **cùng một** thông báo. Vì sao: nếu hai thông
  báo khác nhau, người lạ thử nhiều email là biết email nào đã đăng ký.
- Customer vào `/admin/**` thì nhận 403. Trang lỗi đẹp làm ở T14; tạm thời dùng trang mặc
  định.

## 6. Kịch bản thử bằng tay

Phân quyền theo role là chỗ dễ sai nhất của task, nên hãy thử đủ các trường hợp dưới đây.
Chạy ứng dụng (`.\mvnw.cmd spring-boot:run`), dùng cửa sổ ẩn danh để thử khi chưa đăng
nhập. Vì sao phải gõ thẳng đường dẫn: ẩn nút trên menu không chặn được ai, chỉ backend
chặn mới có tác dụng.

| Ai | Làm gì | Kết quả mong đợi |
| --- | --- | --- |
| Chưa đăng nhập | Mở `/admin` | Chuyển tới `/login` |
| Chưa đăng nhập | Mở `/api/admin/trains/1/layout` | 401, không chuyển tới `/login` (xem tab Network của DevTools) |
| `an.nguyen@example.com` | Mở `/admin/stations` | 403 |
| `admin@traingo.vn` | Mở `/admin/stations` | Trang danh sách ga |
| Chưa đăng nhập | Mở `/` và `/trips` | Vào được bình thường |

Thử đăng ký:

1. Đăng ký với email `An.Nguyen@Example.com`: bị báo trùng, vì email đó đã có (chỉ khác
   hoa thường).
2. Nhập mật khẩu và ô nhập lại khác nhau: lỗi hiện dưới ô nhập lại.
3. Đăng ký một email mới: chuyển tới trang login, thấy "Đăng ký thành công".
4. Kiểm tra mật khẩu được lưu ở dạng mã hóa, không phải chuỗi gốc. Mở MySQL bằng
   `docker exec -it traingo-mysql mysql -utraingo -ptraingo --default-character-set=utf8mb4 traingo`
   rồi chạy:

```sql
SELECT email, password_hash, role FROM users ORDER BY id DESC LIMIT 1;
```

   Cột `password_hash` phải bắt đầu bằng `$2a$`, cột `role` là `CUSTOMER`.

5. Đăng nhập sai mật khẩu, rồi đăng nhập bằng email không tồn tại: cả hai lần hiện cùng
   một thông báo.

## 7. Lỗi hay gặp

- Role trong database là `ADMIN`, nhưng Spring Security so với `ROLE_ADMIN`.
  `hasRole("ADMIN")` tự thêm tiền tố `ROLE_`, nên authority phải là `"ROLE_" + role`.
- Quên `usernameParameter("email")`: form gửi ô tên `email` nhưng Spring đọc ô tên
  `username`, nên đăng nhập luôn thất bại mà không báo lý do.
- Sửa `SecurityConfig` mà quên mở `/vendor/**`: mọi trang mất CSS, log không báo gì
  (`docs/decisions/0003-he-thong-giao-dien.md`).

## 8. Kiểm tra trước khi báo xong

- [ ] Đăng nhập `admin@traingo.vn` / `Admin@123` vào được `/admin`, header hiện
      "Quản trị viên".
- [ ] Đăng nhập `an.nguyen@example.com` / `Customer@123`, gõ `/admin` thì bị chặn.
- [ ] Đăng ký tài khoản mới, đăng nhập bằng nó, rồi đăng xuất được.
- [ ] Tài khoản tạm `admin` / `admin` không còn đăng nhập được.
- [ ] Mọi dòng trong kịch bản ở mục 6 cho đúng kết quả mong đợi.
- [ ] Đã cập nhật `docs/product/auth.md` (mục "Trạng thái hiện tại của code") và
      `docs/RUNBOOK.md` (bỏ tài khoản tạm).
- [ ] `.\mvnw.cmd compile` chạy không lỗi và ứng dụng khởi động được
      (`.\mvnw.cmd spring-boot:run`).
- [ ] Có một dòng trong `docs/WORKLOG.md`, nằm trong cùng commit với code.
