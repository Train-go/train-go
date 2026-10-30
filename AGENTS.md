# Agent Instructions

<!-- HARNESS:BEGIN -->
## Harness

Start with the requested outcome, then use the repository as the system of
record. Read `docs/WORKFLOW.md` and only relevant product, design, plan, code,
and validation material.

- Answers, explanations, reviews, diagnoses, plans, and status reports are
  read-only. Inspect only what is needed and do not mutate repository or Harness
  state.
- For a bounded change, use an ephemeral plan: inspect the affected behavior and
  proof, implement, and validate. No control-plane operation is required.
- Create or update one file under `docs/plans/active/` when work spans sessions,
  needs coordination, has meaningful dependencies, or requires recovery steps.
  Move it to `docs/plans/completed/` only after validation.
- Before editing, identify repository authority for each new externally
  observable policy. If materially different choices remain open, stop before
  edits; configurable defaults are not authority.
- Report reusable agent friction. Change guidance, tools, runbooks, or validation
  for that purpose only when explicitly asked to use `$improve-harness`.
- Also pause when product intent remains ambiguous, recovery is difficult,
  validation is weakened, or authority is insufficient.
- Claim completion only with relevant executable or observable evidence. Report
  the outcome, important changes, validation, and unresolved risks.

Harness has no task database or orchestration lifecycle. Use repository-owned
plans and behavior-level proof; do not create parallel control-plane state.
<!-- HARNESS:END -->

## Dự án TrainGo

Phần dưới đây do nhóm sở hữu, không thuộc Harness core.

### Bản đồ repository

- `SPEC.md` — yêu cầu gốc của môn học, là bản gốc khi có mâu thuẫn.
- `docs/product/` — bản chắt lọc của spec để làm việc hằng ngày; bắt đầu ở
  `docs/product/overview.md`.
- `docs/decisions/` — quyết định kỹ thuật đã chốt, phải tuân theo.
- `docs/plans/active/mvp-traingo.md` — kế hoạch MVP đang chạy.
- `docs/WORKLOG.md` — nhật ký bắt buộc của mọi task làm thay đổi repository.
- `docs/session/` — plan nháp và tài liệu của phiên làm việc.
- `docs/RUNBOOK.md` — cách chạy ứng dụng và database ở máy local.
- `app/` — ứng dụng Spring Boot, package gốc `com.voyagego.traingo`.
- `app/src/main/resources/db/migration/`: schema và seed data, do Flyway chạy
  lúc khởi động (`docs/decisions/0005-quan-ly-schema-bang-flyway.md`).

### Nhật ký thay đổi bắt buộc

- Mọi task có tạo, sửa, đổi tên hoặc xóa file trong repository phải thêm một
  bản ghi vào `docs/WORKLOG.md` trước khi được xem là hoàn thành.
- Ghi **một dòng cho mỗi task**, không ghi từng lệnh terminal hay từng lần lưu
  file. Bản ghi phải có ngày, loại thay đổi theo Conventional Commits, kết quả,
  phạm vi chính và bằng chứng kiểm chứng.
- Bản ghi trong `docs/WORKLOG.md` phải nằm trong cùng commit với thay đổi mà nó
  mô tả. Không được commit code trước rồi để nhật ký cho một commit sau.
- Yêu cầu chỉ đọc như hỏi đáp, giải thích, review, chẩn đoán hoặc báo cáo trạng
  thái không làm thay đổi repository thì không cần ghi nhật ký.
- `docs/WORKLOG.md` là chỉ mục kiểm toán, không thay thế tài liệu có thẩm quyền.
  Khi hành vi, kiến trúc, cách vận hành hoặc quyết định thay đổi, vẫn phải cập
  nhật tương ứng `docs/product/`, `docs/decisions/`, plan đang active hoặc
  `docs/RUNBOOK.md`.
- Không được tuyên bố hoàn thành một task thay đổi repository nếu thiếu bản ghi
  hoặc thiếu bằng chứng kiểm chứng đã nêu trong bản ghi.

### Hai khu vực giao diện

Dự án có **hai hệ giao diện tách rời**, không được trộn:

| | Landing / khách | Admin |
| --- | --- | --- |
| Template | `templates/layout/base.html`, `templates/fragments/` | `templates/admin/layout/base.html`, `templates/admin/fragments/` |
| CSS | `/css/app.css` + `/css/pages/<trang>.css` | `/css/admin/admin.css` + `/css/admin/pages/<trang>.css` |
| JS | `/js/landing.js` | `/js/admin/admin.js` |

**Không bao giờ nạp `app.css` và `admin.css` trên cùng một trang.** Hai khu dùng
chung một file Bootstrap, nhưng mỗi file đặt các biến `--bs-*` theo màu và font
riêng của khu mình; nạp chung là khu này mang màu của khu kia, và triệu chứng
chỉ thấy bằng mắt chứ không có lỗi biên dịch nào.

Trang admin viết markup Bootstrap thường (`btn btn-primary`, `form-control`,
`offcanvas`, `modal`) và gọi các hàm dùng chung `window.TG` trong `admin.js`.
Bảng đối chiếu thành phần mẫu sang Bootstrap ở ADR 0012.

Thư viện nằm ở `static/vendor/`, font ở `static/fonts/`, đều tải về sẵn. Không
thêm link CDN vào bất kỳ trang nào, kể cả trang admin.

Chi tiết và lý do: `docs/decisions/0004-khu-vuc-quan-tri.md`,
`docs/decisions/0012-giao-dien-admin-moi.md`.

### Quy trình plan

- Việc đủ lớn để phải bàn trước khi làm thì viết bản nháp
  `docs/session/plan-<slug>.md` trước, dùng `docs/templates/exec-plan.md`.
- Được duyệt rồi mới chuyển sang `docs/plans/active/<slug>.md`, và **xóa bản
  nháp** — một plan chỉ sống ở một nơi tại một thời điểm.
- Việc gọn trong một phiên, khôi phục được từ chính diff của nó, thì không cần
  bản nháp lẫn plan chính thức.

Chi tiết: `docs/session/README.md`.

### Quy tắc kiểm tra

Dự án không có test tự động (quyết định ngày 12/10/2026). Không tạo thư mục
`app/src/test` và không thêm dependency test.

- Không được nói "đã xong", "đã fix" hay "chạy được" nếu chưa làm đủ ba việc và
  dẫn kết quả thật: `.\mvnw.cmd compile` chạy không lỗi; ứng dụng khởi động được
  bằng `.\mvnw.cmd spring-boot:run`; đã thử thật trên trình duyệt với MySQL và dữ
  liệu mẫu. Phân biệt rõ: đã sửa code, đã biên dịch, đã chạy ứng dụng, đã thử tay.
- Mỗi task có mục "Kịch bản thử bằng tay" trong file hướng dẫn. Phải đi hết kịch
  bản đó trước khi mở pull request.
- Nghiệp vụ rủi ro phải thử kỹ, kể cả trường hợp lỗi: giữ chỗ (hai người cùng giữ
  một ghế, hết hạn giữ chỗ), xác nhận booking, hủy booking, số hành khách bằng số
  chỗ, công thức giá, phân quyền theo role và theo quyền sở hữu. Tình huống khó tạo
  bằng tay thì dùng SQL, ví dụ lùi `held_until` về quá khứ.

Chi tiết và lý do: `docs/decisions/0007-bo-test-tu-dong.md` (thay cho ADR 0002).

### Quy tắc về cấu trúc code

- Chia package theo tầng, dưới `com.voyagego.traingo`: `controller`, `entity`,
  `model` (`dto`, `request`, `response`), `repository`, `service`, cùng
  `common` (`config`, `exception`). Mỗi nghiệp vụ là một file trong tầng đó
  (`StationService`, `StationRepository`...), không tạo package riêng cho
  từng nghiệp vụ. Enum nằm cùng entity; exception nghiệp vụ nằm ở
  `common/exception`; object nhận từ form hoặc JSON đặt hậu tố `Request`.
- Controller chỉ nhận request, validate cơ bản, gọi service và trả view hoặc
  JSON. Không đặt nghiệp vụ phức tạp trong controller.
- Service giữ nghiệp vụ và transaction. Repository chỉ truy cập dữ liệu.
- Trang thường dùng Thymeleaf. Phần tương tác — sơ đồ ghế, giữ chỗ, đếm ngược —
  dùng JavaScript thuần trong `static/js/` gọi endpoint `@RestController` dưới
  `/api/...`. Không dựng dự án frontend riêng.
- Dùng constructor injection, không dùng `@Autowired` trên field.
- Schema chỉ đổi bằng một migration Flyway mới. Không sửa migration đã chạy,
  không bật lại `ddl-auto=update`.

Chi tiết và lý do: `docs/decisions/0006-cau-truc-package-theo-tang.md`; các
phần còn lại của cấu trúc dự án ở
`docs/decisions/0001-cau-truc-du-an-va-tech-stack.md`.

### Bẫy của Spring Boot 4 so với tài liệu 3.x

Tài liệu và ví dụ trên mạng phần lớn viết cho Spring Boot 3.x. Khi gặp lỗi không
tìm thấy class hoặc annotation, kiểm tra jar thật trong `~/.m2/repository` trước
khi tin bài viết trên mạng. Đã gặp:

- Hibernate 7 đòi cột `ENUM` của MySQL cho `@Enumerated(EnumType.STRING)`; dự án
  lưu enum dạng `VARCHAR`, nên entity phải khai thêm
  `@JdbcTypeCode(SqlTypes.VARCHAR)` (ADR 0005).
- Spring Security 7 đã bỏ `AntPathRequestMatcher`; dùng `PathPatternRequestMatcher`.

### Ngôn ngữ

- Tài liệu trong `docs/`, `SPEC.md` và giao diện người dùng: tiếng Việt.
- Định danh trong code, comment và commit message: tiếng Anh.
