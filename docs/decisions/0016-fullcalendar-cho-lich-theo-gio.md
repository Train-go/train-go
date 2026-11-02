# 0016 Lịch theo giờ của trang Tàu dùng FullCalendar

Date: 2026-11-01

## Status

Accepted. Phi chọn kiểu "lịch tuần dọc" ngày 01/11/2026, thay cho bản `vis-timeline` thử trong cùng
ngày. Là ngoại lệ có chủ đích với việc ADR `0012` gỡ bớt thư viện khỏi `static/vendor/`; vẫn không dùng CDN.

## Context

Bảng "Lịch theo giờ" cũ tự vẽ bằng CSS: mỗi ngày một hàng, mỗi chuyến đặt theo phần trăm của ngày. Khi
một ngày có nhiều chuyến ngắn hoặc chuyến đã hủy trùng giờ với chuyến đang chạy, các thanh đè lên nhau
và chữ bị cắt.

Bản thử với `vis-timeline` (một trục ngang liên tục) sửa được chuyện đè nhau, nhưng ở chế độ 7 ngày một
ngày chỉ còn khoảng 150px: chuyến 3 giờ rộng 19px, chuyến 30 phút rộng 3px, không đọc được chữ. Đổi màu
hay cỡ chữ không sửa được, vì giới hạn nằm ở việc trải 168 giờ trên một trục ngang.

## Decision

- Dùng FullCalendar 6.1.15, bản "Standard Bundle" (`index.global.min.js`, giấy phép MIT, CSS tự chèn),
  đặt ở `static/vendor/fullcalendar/` cùng `LICENSE.md`. Chỉ trang Tàu, view "Theo ngày", nạp nó.
  Không dùng các gói premium (resource timeline).
- Lịch tuần dọc: mỗi ngày một cột, giờ chạy từ trên xuống, một giờ cao 3rem (48px). Chuyến 30 phút vẫn
  đủ một dòng "giờ đi – giờ đến + tuyến"; chuyến dài hiện thêm "Đã bán n/96" và dải tỉ lệ đã bán.
  Chuyến trùng giờ đứng cạnh nhau trong cột. Chuyến qua đêm tách thành hai khối ở hai cột.
- Thanh lọc (Phi chốt cùng ngày): nút trước, sau, "Hôm nay"; một nút ghi khoảng đang xem theo dd/mm
  ("01/11 – 07/11/2026", "Tháng 11/2026") và mở bảng chọn ngày của trình duyệt (`showPicker()` trên một
  `input type="date"` ẩn, vì chữ của ô ngày gốc theo ngôn ngữ trình duyệt, bản tiếng Anh ghi 11/03/2026);
  dropdown "Ngày / Tuần / Tháng", nhớ lựa chọn cuối trong `localStorage`, mặc định Tuần.
- Ngày là lưới giờ của ngày chọn; Tuần là 7 ngày bắt đầu từ ngày chọn (không phải Thứ Hai đến Chủ
  nhật, để mặc định luôn thấy các chuyến sắp chạy); Tháng là lưới tháng bắt đầu Thứ Hai, mỗi ô tối đa ba
  chuyến một dòng rồi "+N chuyến". "+N chuyến" và số ngày mở ngày đó ở chế độ Ngày.
- Lưới giờ cao 560px, tự cuộn tới chuyến đầu tiên của khoảng đang xem.
- Khi khung hẹp hơn 560px (điện thoại), Tuần và Tháng chuyển sang danh sách theo ngày, vì bảy cột
  không vừa màn hình; Ngày vẫn là lưới.
- Chuyến đã chạy và chuyến đã hủy ẩn sẵn, có nút "Hiện N chuyến đã chạy" và "Hiện N chuyến đã hủy".
- Dữ liệu lấy từ `GET /api/admin/trains/{id}/timeline` (`TrainHoursView.Timeline`); giao diện vẽ ở
  `static/js/admin/train-timeline.js`. Bấm (hoặc Tab rồi Enter) vào chuyến mở ngăn chuyến, vào ô
  "Chuyến mới" mở trình hướng dẫn.
- Bỏ cột "Cuối ngày ở" và nhãn "chờ N giờ" của bảng cũ: ô "Vị trí" ở đầu trang đã cho biết tàu đang ở đâu.

## Consequences

Thêm một thư viện khoảng 280 KB (chưa nén) và một lệnh gọi API cho trang. FullCalendar gắn class `fc`
lên chính phần tử lịch, nên CSS của trang viết `.tl .fc ...`, không phải `.tl-canvas .fc ...`. Nâng cấp
thư viện là thay file trong `static/vendor/fullcalendar/` và thử lại trang Tàu ở 1440px và 375px.
