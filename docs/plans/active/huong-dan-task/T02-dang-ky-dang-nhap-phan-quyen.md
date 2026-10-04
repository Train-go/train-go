# T02 Đăng ký, đăng nhập, phân quyền

| Wave | Lane | Cỡ | Phụ thuộc | Mở khóa | Branch |
| --- | --- | --- | --- | --- | --- |
| 1 | C | L | T01 (mềm), D4 | T09, T10, T11, T12 | `feat/t02-auth` |

## Mục tiêu

- Khách đăng ký tài khoản, đăng nhập bằng email và mật khẩu, đăng xuất.
- Người đăng nhập được nhận diện ở mọi trang: navbar đổi theo trạng thái, header
  admin hiện tên thật.
- Backend chặn theo role: Customer gõ tay `/admin/stations` bị chặn; khách chưa
  đăng nhập vào trang đặt vé thì được đưa tới trang đăng nhập.
- Bỏ tài khoản tạm `admin` / `admin`. Đăng nhập bằng tài khoản trong bảng `users`.

Đây là task đầu đường găng: T09, T10, T11, T12 đều cần biết ai đang đăng nhập.

## Phạm vi

Trong phạm vi: đăng ký, đăng nhập, đăng xuất, session, phân quyền theo role cho
**mọi** URL hiện có và sắp có, navbar, header admin.

Ngoài phạm vi (theo `SPEC.md` mục 41): quên mật khẩu, đặt lại mật khẩu, xác thực
email, đăng nhập Google, OAuth, trang hồ sơ.

## Database

Chỉ dùng bảng `users`, đã có từ V1:

```
users
  id            BIGINT PK
  full_name     VARCHAR(100)  NOT NULL
  email         VARCHAR(255)  NOT NULL, UNIQUE (uk_users_email)
  password_hash VARCHAR(100)  NOT NULL     BCrypt, dạng $2a$10$...
  role          VARCHAR(20)   NOT NULL     CUSTOMER | ADMIN
  created_at    DATETIME      NOT NULL

Được tham chiếu bởi: bookings.customer_id, trip_seats.held_by_user_id
```

Seed có sẵn `admin@traingo.vn` / `Admin@123` và ba khách `Customer@123`
(`docs/RUNBOOK.md`). Email luôn lưu chữ thường.

## Model

Có sẵn, không sửa field: `auth/model/User`, `auth/model/UserRole`.

Viết mới `auth/model/RegisterForm`:

| Field | Validation |
| --- | --- |
| `fullName` | `@NotBlank`, `@Size(max = 100)` |
| `email` | `@NotBlank`, `@Email`, `@Size(max = 255)` |
| `password` | `@NotBlank`, `@Size(min = 8, max = 72)` (D4; BCrypt chỉ dùng 72 byte đầu) |
| `confirmPassword` | `@NotBlank` |

Việc so `password` với `confirmPassword` làm ở controller, rồi gắn lỗi vào ô
`confirmPassword` để thông báo hiện đúng chỗ.

## Repository

Thêm vào `auth/repository/UserRepository`:

```java
Optional<User> findByEmail(String email);
boolean existsByEmail(String email);
```

## Service

`auth/service/TrainGoUserDetails`: class implement `UserDetails`, là thứ Spring
Security giữ trong session. Đây là **hợp đồng chung** cho các task sau:

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

Controller của task khác lấy người đang đăng nhập bằng
`@AuthenticationPrincipal TrainGoUserDetails me`, rồi dùng `me.getId()` để kiểm tra
quyền sở hữu. Không truyền id người dùng qua form hay URL.

`auth/service/TrainGoUserDetailsService implements UserDetailsService`:

- `loadUserByUsername(String email)`: chuẩn hóa `trim().toLowerCase(Locale.ROOT)`,
  `findByEmail`, không thấy thì ném `UsernameNotFoundException`.

`auth/service/RegistrationService`:

- `@Transactional User register(RegisterForm form)`: chuẩn hóa email; nếu
  `existsByEmail` thì ném `DuplicateEmailException`; mã hóa mật khẩu bằng
  `PasswordEncoder`; lưu `new User(fullName, email, hash, UserRole.CUSTOMER)`.
- Không bao giờ log hay trả mật khẩu gốc.

## Controller

`auth/controller/AuthController`:

| Method | URL | Handler | Kết quả |
| --- | --- | --- | --- |
| GET | `/login` | `loginPage()` | View `auth/login` |
| GET | `/register` | `registerForm(Model)` | View `auth/register` với `RegisterForm` rỗng |
| POST | `/register` | `register(@Valid RegisterForm, BindingResult, RedirectAttributes)` | Lỗi: hiện lại form. Xong: `redirect:/login?registered` |

`POST /login` và `POST /logout` do Spring Security xử lý, không viết handler.

## SecurityConfig (chủ sở hữu: T02)

Viết lại `common/config/SecurityConfig` với ma trận quyền cho **mọi** URL của MVP,
kể cả URL chưa có, để các task sau không phải sửa file này:

| URL | Quyền |
| --- | --- |
| `/`, `/trips`, `/trips/**`, `/login`, `/register`, `/error` | Ai cũng vào được |
| `/css/**`, `/js/**`, `/images/**`, `/fonts/**`, `/vendor/**`, `/design/**`, `/favicon.ico` | Ai cũng vào được |
| `/admin/**`, `/api/admin/**` | `hasRole("ADMIN")` |
| `/booking/**`, `/my-trips/**`, `/tickets/**`, `/api/trips/**` | `hasRole("CUSTOMER")` |
| Mọi URL còn lại | Đã đăng nhập |

Cấu hình thêm:

- Bean `PasswordEncoder` trả `new BCryptPasswordEncoder()`. Hash trong seed là BCrypt
  thuần, không dùng `DelegatingPasswordEncoder`.
- `formLogin`: `loginPage("/login")`, `usernameParameter("email")`,
  `defaultSuccessUrl("/", false)`, `failureUrl("/login?error")`, `permitAll()`.
- `logout`: `logoutSuccessUrl("/?logout")`. Đăng xuất phải là `POST`, dùng form có
  `th:action` (CSRF).
- Request `/api/**` chưa đăng nhập phải trả **401**, không redirect sang trang login.
  Nếu không, `fetch` ở T06, T09 nhận về HTML của trang login với status 200. Dùng
  `exceptionHandling(...).defaultAuthenticationEntryPointFor(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED), <matcher cho /api/**>)`.
  Spring Security 7 đã bỏ `AntPathRequestMatcher`; tìm
  `PathPatternRequestMatcher` trong jar thật.
- Xóa ba dòng `spring.security.user.*` trong `application.properties`.

## View

- `pom.xml`: thêm `org.thymeleaf.extras:thymeleaf-extras-springsecurity6` (Boot 4.1.1
  đã quản lý version).
- `templates/auth/login.html`, `templates/auth/register.html`: dùng khung khách
  `layout/base.html` và `app.css`, **không** dùng khung admin. Trang login hiện thông
  báo khi URL có `?error` ("Email hoặc mật khẩu không đúng"), `?registered` ("Đăng
  ký thành công, mời đăng nhập"), `?logout`.
- `templates/fragments/navbar.html` (chủ sở hữu: T02), khai
  `xmlns:sec="http://www.thymeleaf.org/extras/spring-security"`:
  - chưa đăng nhập (`sec:authorize="isAnonymous()"`): Đăng nhập, Đăng ký;
  - đã đăng nhập: Chuyến của tôi, tên người dùng
    (`sec:authentication="principal.fullName"`), nút Đăng xuất (form `POST /logout`);
  - `sec:authorize="hasRole('ADMIN')"`: thêm link Quản trị tới `/admin`.
- `templates/admin/fragments/header.html`: thay `${adminName}` bằng
  `sec:authentication="principal.fullName"`, rồi xóa
  `admin/controller/AdminShellModelAdvice.java`.

## Quy tắc và lỗi phải xử lý

- Email trùng (không phân biệt hoa thường): báo dưới ô email.
- Mật khẩu nhập lại không khớp: báo dưới ô nhập lại.
- Sai mật khẩu và email không tồn tại cho cùng một thông báo, để không lộ email nào
  đã đăng ký.
- Customer vào `/admin/**`: 403. Trang lỗi đẹp làm ở T14; tạm thời trang mặc định.

## Test bắt buộc

Theo `AGENTS.md`, phân quyền theo role bắt buộc có test:

- `@WebMvcTest` kèm `@Import(SecurityConfig.class)`:
  - khách chưa đăng nhập `GET /admin` bị redirect tới `/login`;
  - `@WithMockUser(roles = "CUSTOMER")` `GET /admin/stations` nhận 403;
  - `@WithMockUser(roles = "ADMIN")` `GET /admin/stations` nhận 200;
  - khách chưa đăng nhập `GET /api/admin/trains/1/layout` nhận 401, không phải 302.
- Unit test `RegistrationServiceTest` (Mockito): email `An@Example.com` trùng với
  `an@example.com` thì ném `DuplicateEmailException`; mật khẩu lưu là hash, khác
  chuỗi gốc.

## Bẫy đã biết

- Role trong database là `ADMIN`, nhưng Spring Security so `ROLE_ADMIN`.
  `hasRole("ADMIN")` tự thêm tiền tố; authority phải là `"ROLE_" + role`.
- Quên `usernameParameter("email")` thì form gửi ô `email` mà Spring đọc ô
  `username`: đăng nhập luôn thất bại mà không báo lý do.
- Sửa `SecurityConfig` mà quên mở `/vendor/**` là mọi trang mất CSS, log không báo
  gì (`docs/decisions/0003-he-thong-giao-dien.md`).

## Xong khi

- [ ] Đăng nhập `admin@traingo.vn` / `Admin@123` vào được `/admin`, header hiện
      "Quản trị viên".
- [ ] Đăng nhập `an.nguyen@example.com` / `Customer@123`, gõ `/admin` bị chặn.
- [ ] Đăng ký tài khoản mới, đăng nhập bằng nó, đăng xuất.
- [ ] `admin` / `admin` không còn đăng nhập được.
- [ ] Test bắt buộc ở trên xanh trong `.\mvnw.cmd test`.
- [ ] Cập nhật `docs/product/auth.md` (mục "Trạng thái hiện tại của code") và
      `docs/RUNBOOK.md` (bỏ tài khoản tạm). Một dòng `docs/WORKLOG.md`.
