Train Ticket Booking - TrainGo
Website đặt vé tàu
Thành viên trong nhóm
Võ Đoàn Anh Phi - 25764121
Võ Huỳnh Thanh Phong - 25763691
Lê Thị Hoàng Oanh - 25766341

1. Tổng quan dự án
   Train Ticket Booking là hệ thống website hỗ trợ người dùng tìm kiếm và đặt vé tàu trực tuyến. Người dùng có thể tìm chuyến tàu phù hợp, lựa chọn toa tàu, vị trí ghế ngồi hoặc giường nằm trực quan, nhập thông tin hành khách, thực hiện thanh toán mô phỏng và nhận vé điện tử sau khi đặt vé thành công.
   Bên cạnh chức năng dành cho khách hàng, hệ thống còn hỗ trợ nhân viên kiểm tra và xác nhận vé của hành khách. Đối với quản trị viên, hệ thống cung cấp các chức năng quản lý thông tin phục vụ quá trình vận hành như ga tàu, tuyến đường, tàu, toa tàu, lịch trình chạy tàu và các đơn đặt vé.
   Dự án được xây dựng với mục tiêu mô phỏng quy trình đặt vé tàu trong thực tế ở phạm vi vừa đủ, tập trung vào các nghiệp vụ chính từ tìm kiếm chuyến tàu đến hoàn tất đặt vé. Hệ thống không hướng đến việc mô phỏng toàn bộ hoạt động của ngành đường sắt mà chủ yếu tập trung vào quy trình đặt vé và quản lý các dữ liệu liên quan.
2. Mục tiêu dự án
   Mục tiêu của dự án là xây dựng một hệ thống đặt vé tàu trực tuyến có thể mô phỏng tương đối đầy đủ quy trình đặt vé trong thực tế, từ lúc người dùng tìm kiếm chuyến tàu cho đến khi sử dụng vé để check-in.
   Quy trình chính của hệ thống:
   Tìm chuyến ↓ Chọn chuyến ↓ Chọn toa ↓ Chọn ghế / giường ↓ Nhập thông tin hành khách ↓ Kiểm tra booking ↓ Thanh toán ↓ Booking thành công ↓ Nhận vé
   Các mục tiêu cụ thể của hệ thống bao gồm:
   Xây dựng hệ thống đặt vé tàu trực tuyến có giao diện đơn giản, dễ sử dụng.
   Cho phép người dùng tìm kiếm chuyến tàu dựa trên ga đi, ga đến và ngày khởi hành.
   Hiển thị đầy đủ các thông tin cần thiết của chuyến tàu như thời gian khởi hành, thời gian đến, loại tàu và giá vé.
   Cho phép người dùng lựa chọn toa tàu và vị trí ghế ngồi hoặc giường nằm trực tiếp trên sơ đồ trực quan.
   Hạn chế tình trạng nhiều người cùng đặt một vị trí trong cùng thời điểm.
   Quản lý thông tin hành khách, đơn đặt vé và trạng thái của từng booking.
   Mô phỏng quy trình thanh toán để hoàn thiện luồng đặt vé.
   Tạo vé điện tử sau khi đặt vé thành công, kèm mã QR để phục vụ việc kiểm tra vé.
   Hỗ trợ nhân viên kiểm tra thông tin vé và thực hiện check-in cho hành khách.
   Cung cấp khu vực quản trị để Admin quản lý các dữ liệu của hệ thống như ga tàu, tuyến đường, tàu, toa tàu, lịch chạy và booking.
   Hiển thị vị trí các ga tàu và thông tin tuyến đường trên bản đồ để người dùng dễ theo dõi và tra cứu.
3. Đối tượng sử dụng
   TrainGo có hai nhóm người dùng:
   CUSTOMER
   ADMIN
   Mỗi role có phạm vi chức năng và quyền truy cập riêng.

3.1 Customer
Customer là người sử dụng hệ thống để tìm kiếm và đặt vé tàu.
Các chức năng:
Đăng ký tài khoản.
Đăng nhập.
Đăng xuất.
Tìm kiếm chuyến tàu.
Xem danh sách chuyến phù hợp.
Xem chi tiết chuyến.
Chọn toa.
Chọn ghế hoặc giường.
Nhập thông tin hành khách.
Thanh toán mô phỏng.
Nhận vé điện tử.
Xem danh sách booking.
Xem chi tiết booking.
Xem vé.
Customer là nhóm người dùng chính của hệ thống.
3.2 Admin
Admin chịu trách nhiệm quản lý các dữ liệu cần thiết để hệ thống booking hoạt động.
Các chức năng:
Xem Dashboard tổng quan.
Quản lý ga tàu.
Quản lý tuyến đường.
Quản lý tàu.
Cấu hình toa tàu.
Quản lý chuyến tàu.
Quản lý vé.
Xem danh sách tài khoản.
Admin không quản lý toàn bộ hoạt động vận hành đường sắt.
Các chức năng Admin chỉ tập trung vào dữ liệu cần thiết cho quá trình đặt vé. 4. Luồng nghiệp vụ chính
Luồng chính của Customer:
Landing Page ↓ Tìm chuyến ↓ Danh sách chuyến ↓ Chọn chuyến ↓Chọn toa ↓ Chọn ghế / giường ↓ Nhập thông tin hành khách ↓ Xem lại vé ↓ Mock Payment ↓ Xác nhận ↓ Electronic Ticket ( giống xuất pdf vé )
Stepper trên giao diện có thể sử dụng:
Trip → Seat → Passenger → Review → Payment → Ticket
Đây là luồng quan trọng nhất của toàn bộ project. 5. Landing Page
Landing Page vừa là trang giới thiệu TrainGo vừa là nơi bắt đầu quá trình đặt vé.
5.1 Navbar
Navbar dự kiến gồm:
Logo TrainGo.
Home.
How It Works.
My Trips.
Login / Profile.
Nếu người dùng chưa đăng nhập:
Login > Register
Nếu đã đăng nhập:
My Trips > Profile > Logout
5.2 Hero Section
Hero Section chứa form tìm chuyến.
Thông tin:
Ga đi.
Ga đến.
Ngày khởi hành.
Số lượng hành khách.
Ví dụ:
Khám phá Việt Nam bằng tàu
Từ [Sài Gòn]
Đến [Nha Trang]
Ngày đi
[20/08/2026]
Hành khách
[2] [Tìm chuyến]
Có thể hỗ trợ nút: ↔ để đổi nhanh ga đi và ga đến.
5.3 Popular Routes
Landing Page hiển thị một số tuyến phổ biến.
Ví dụ:
Sài Gòn → Nha Trang.
Sài Gòn → Đà Nẵng.
Hà Nội → Đà Nẵng.
Hà Nội → Lào Cai.
Khi Customer chọn một tuyến, hệ thống tự điền:
originStation
destinationStation
vào form tìm kiếm.
Popular Routes chủ yếu hỗ trợ UX và không yêu cầu thêm nghiệp vụ backend phức tạp.
5.4 How It Works
Landing Page có thể giới thiệu ngắn gọn:

1. Tìm chuyến
2. Chọn chỗ
3. Nhập hành khách
4. Thanh toán
5. Nhận vé
   Không cần xây dựng trang /how-it-works riêng nếu nội dung ít.
6. Railway Map - Optional
   Railway Map là tính năng mở rộng.
   Không phải yêu cầu bắt buộc của MVP.
   Nếu triển khai, map chỉ cần:
   Hiển thị vị trí các ga chính.
   Hiển thị tên ga.
   Hiển thị tỉnh hoặc thành phố.
   Cho phép click marker.
   Hiển thị thông tin cơ bản của ga.
   Ví dụ:
   Ga Nha Trang
   Khánh Hòa
   Không cần:
   Navigation.
   GPS.
   Theo dõi tàu real-time.
   Vẽ toàn bộ mạng lưới đường sắt.
   Tìm chuyến trực tiếp trên bản đồ.
   Công nghệ dự kiến:
   Leaflet
   OpenStreetMap
7. Tìm kiếm chuyến tàu
   Customer tìm chuyến dựa trên:
   Ga đi.
   Ga đến.
   Ngày đi.
   Số hành khách.
   Ví dụ:
   Sài Gòn
   ↓
   Nha Trang
   20/08/2026
   2 hành khách
   Điều kiện:
   Ga đi không được để trống.
   Ga đến không được để trống.
   Ga đi và ga đến không được giống nhau.
   Ngày đi không được nhỏ hơn ngày hiện tại.
   Số hành khách phải lớn hơn 0.
   Phiên bản đầu không cần:
   Filter khoảng giá.
   Filter loại toa.
   Filter theo giờ.
   Filter thời gian di chuyển.
   Sorting nâng cao.
8. Danh sách chuyến
   Sau khi tìm kiếm, hệ thống hiển thị những Trip phù hợp.
   Ví dụ:
   SE2
   Sài Gòn Nha Trang
   06:00 ---------------------- 13:30
   7 giờ 30 phút
   Còn 24 chỗ
   Giá từ: 350.000đ
   [Chọn chuyến]
   Thông tin chính:
   Mã tàu.
   Ga đi.
   Ga đến.
   Giờ khởi hành.
   Giờ đến.
   Thời gian di chuyển.
   Giá thấp nhất.
   Số chỗ còn lại.
   Customer chọn một Trip để tiếp tục booking.
9. Station
   Station đại diện cho ga tàu.
   Thông tin dự kiến:
   Station
   id
   name
   code
   city
   address
   latitude
   longitude
   Ví dụ:
   code: SGN
   name: Ga Sài Gòn
   city: Hồ Chí Minh
   latitude và longitude chỉ cần thiết nếu Railway Map được triển khai.
10. Route
    Route đại diện cho tuyến di chuyển giữa hai ga.
    Ví dụ:
    Sài Gòn → Nha Trang
    Thông tin:
    Route

id
originStationId
destinationStationId
distance
Phiên bản đầu không xử lý ga trung gian.
Không cần:
RouteStation
Điều này giúp:
Database đơn giản hơn.
Search Trip dễ hơn.
Pricing đơn giản hơn.
Booking dễ xử lý hơn.
Không cần quản lý giờ đến tại nhiều ga. 11. Train và Trip
Train và Trip là hai khái niệm khác nhau.
11.1 Train
Train đại diện cho cấu hình vật lý của đoàn tàu.
Ví dụ:
SE2
│
├── Coach 01 – SEAT
├── Coach 02 – SEAT
├── Coach 03 – SLEEPER
└── Coach 04 – SLEEPER
Quan hệ:
Train
└── Coach
└── Seat
Một Train có nhiều Coach.
Một Coach có nhiều Seat.

11.2 Trip
Trip đại diện cho một lần chạy cụ thể của Train.
Ví dụ:
Train:
SE2

Route:
Sài Gòn → Nha Trang

Date:
20/08/2026

Departure:
06:00

Arrival:
13:30
Một Train có thể được sử dụng cho nhiều Trip khác nhau.
Thông tin Trip:
Trip

id
trainId
routeId
departureDate
departureTime
arrivalTime
basePrice
status
Trip Status:
SCHEDULED
COMPLETED
CANCELLED
Không cần:
BOARDING
DEPARTED
DELAYED
trong MVP.

12. Coach
    Coach đại diện cho một toa tàu.
    Thông tin:
    Coach

id
trainId
coachNumber
coachType
capacity
priceModifier
Coach Type:
SEAT
SLEEPER
Ví dụ:
Coach 01

Type:
SEAT

Capacity:
40

Price Modifier:
0
Hoặc:
Coach 03

Type:
SLEEPER

Price Modifier:
200.000đ
Coach được quản lý bên trong Train Detail.
Không cần module CRUD Coach hoàn toàn riêng biệt.

13. Seat / Bed
    Hệ thống dùng chung entity Seat để biểu diễn ghế hoặc giường.
    Thông tin:
    Seat

id
coachId
code
type
cabinNumber
Type:
SEAT
BED
Ví dụ ghế:
A01
A02
A03
Ví dụ giường:
B01
B02
B03
Admin không cần CRUD từng Seat.
Khi tạo Coach, hệ thống có thể tự sinh các vị trí dựa trên cấu hình.

14. Toa ghế
    Một toa ghế có thể hiển thị:
    TOA 01

[A01] [A02] [A03] [A04]

[A05] [A06] [A07] [A08]

[A09] [A10] [A11] [A12]

[A13] [A14] [A15] [A16]
Customer click trực tiếp vào vị trí muốn đặt.
Mục tiêu là tạo trải nghiệm chọn chỗ trực quan.
Không cần mô phỏng chính xác cấu trúc tàu thật.

15. Toa giường
    Một Coach loại SLEEPER được chia thành các cabin.
    Ví dụ:
    Cabin 01

[B01] [B02]

[B03] [B04]
Một toa có thể gồm:
Cabin 01
Cabin 02
Cabin 03
Cabin 04
Mỗi cabin có 4 giường.
Phiên bản đầu không phân chia:
Giường tầng 1.
Giường tầng 2.
Giường tầng 3.
VIP.
Phòng riêng.
Nhiều loại giá phức tạp.

16. TripSeat
    Seat chỉ đại diện cho vị trí vật lý trên Train.
    Trạng thái đặt chỗ phải được quản lý theo từng Trip.
    Ví dụ:
    Train SE2
    Coach 01
    Seat A01
    A01 có thể:
    Trip 20/08 → BOOKED

Trip 21/08 → AVAILABLE
Do đó hệ thống sử dụng:
TripSeat
Thông tin:
TripSeat

id
tripId
seatId
status
heldBy
heldUntil
Ràng buộc:
tripId + seatId
phải là duy nhất.

17. Trạng thái chỗ
    Các trạng thái nghiệp vụ:
    AVAILABLE
    HELD
    BOOKED
    Ngoài ra frontend có thể sử dụng:
    SELECTED
    nhưng SELECTED chỉ là trạng thái giao diện, không cần lưu database.

AVAILABLE
Vị trí đang trống và có thể chọn.

HELD
Vị trí đang được một Customer giữ tạm trong quá trình booking.

BOOKED
Vị trí đã thuộc về một booking được xác nhận.

SELECTED
Customer vừa click vào vị trí trên frontend.
Ví dụ:
AVAILABLE
↓
SELECTED
↓
HELD

18. Seat Holding
    Seat Holding giúp hạn chế hai Customer cùng đặt một chỗ.
    Ví dụ Customer A chọn A01:
    AVAILABLE
    ↓
    HELD
    Hệ thống giữ A01 trong:
    5 phút
    Frontend hiển thị countdown:
    04:59
    04:58
    04:57
    ...
    TripSeat lưu:
    heldBy
    heldUntil
    Nếu Customer thanh toán thành công:
    HELD
    ↓
    BOOKED
    Nếu timeout:
    HELD
    ↓
    AVAILABLE
    Customer khác không thể giữ một TripSeat đang:
    HELD
    hoặc:
    BOOKED
    Backend luôn kiểm tra lại trạng thái trước khi xác nhận booking.
    Phiên bản đầu không bắt buộc WebSocket.

19. Passenger Information
    Sau khi chọn chỗ, Customer nhập thông tin hành khách.
    Mỗi hành khách gồm:
    Họ tên.
    Ngày sinh.
    CCCD hoặc Passport.
    Ghế hoặc giường.
    Ví dụ:
    Nguyễn Văn A

12/05/2000

079xxxxxxxxx

Seat A01
Phiên bản đầu không cần:
Adult / Child pricing.
Người cao tuổi.
Sinh viên.
Chính sách giảm giá.
Giá đặc biệt.

20. Quy tắc hành khách và chỗ
    Số hành khách phải bằng số chỗ được chọn.
    Ví dụ:
    Passengers = 2
    Customer phải chọn:
    2 Seats / Beds
    Điều kiện:
    numberOfPassengers
    =
    numberOfSelectedSeats
    Mỗi hành khách phải được gán đúng một vị trí.
    Ví dụ:
    Nguyễn Văn A → A01
    Trần Văn B → A02
    Một TripSeat không thể được gán cho hai hành khách.

21. Giá vé
    Trip có giá cơ bản:
    basePrice
    Coach có:
    priceModifier
    Công thức:
    Ticket Price
    =
    Trip Base Price

- Coach Price Modifier
  Ví dụ:
  Trip Base Price:
  350.000đ
  Toa ghế:
  priceModifier = 0

    350.000 + 0
    = 350.000đ
    Toa giường:
    priceModifier = 200.000

    350.000 + 200.000
    = 550.000đ
    Không triển khai Dynamic Pricing.
    Giá tại thời điểm đặt phải được lưu lại trong booking/passenger để việc Admin thay đổi giá Trip sau này không làm thay đổi booking cũ.

22. Booking
    Booking đại diện cho một lần đặt vé.
    Một Booking:
    Thuộc một Customer.
    Thuộc một Trip.
    Có một hoặc nhiều hành khách.
    Thông tin:
    Booking

id
bookingCode
customerId
tripId
totalPrice
bookingStatus
paymentStatus
paidAt
createdAt
Booking Status:
PENDING
CONFIRMED
CANCELLED
Payment Status:
UNPAID
PAID
Không bắt buộc trạng thái:
COMPLETED
Nếu:
Booking = CONFIRMED
Trip = COMPLETED
thì UI có thể tự xếp booking vào nhóm Completed.

23. BookingPassenger
    BookingPassenger lưu thông tin từng hành khách trong booking.
    Thông tin:
    BookingPassenger

id
bookingId
tripSeatId
fullName
dateOfBirth
identityNumber
ticketPrice
Mỗi BookingPassenger gắn với một TripSeat.
Ví dụ:
Booking BK001
│
├── Nguyễn Văn A → A01
└── Trần Văn B → A02

24. Booking Review
    Trước khi thanh toán, Customer phải xác nhận lại booking.
    Thông tin:
    Tuyến.
    Ngày đi.
    Giờ đi.
    Train.
    Coach.
    Seat / Bed.
    Hành khách.
    Giá từng vé.
    Tổng tiền.
    Ví dụ:
    Sài Gòn → Nha Trang

20/08/2026
06:00

Train SE2

Passenger
Nguyen Van A

Coach 02
Seat A12

Ticket Price
350.000đ

Total
350.000đ

[Quay lại]

[Tiếp tục thanh toán]

25. Mock Payment
    TrainGo sử dụng Mock Payment.
    Không kết nối với ngân hàng hoặc cổng thanh toán thật.
    Ví dụ:
    Booking:
    BK202608001

Total:
350.000 VND

[Thanh toán]
Sau khi thanh toán mô phỏng:
UNPAID
↓
PAID
Booking:
PENDING
↓
CONFIRMED
TripSeat:
HELD
↓
BOOKED
Sau đó hệ thống tạo Ticket.
Không cần:
Form nhập thẻ.
Banking thật.
E-Wallet thật.
Payment Gateway.
Callback.
Webhook.
Refund thật.
Không bắt buộc tạo bảng Payment riêng.
Có thể lưu trực tiếp:
paymentStatus
paidAt
trong Booking.

26. Transaction khi xác nhận Booking
    Khi thanh toán thành công, các bước quan trọng phải được xử lý cùng nhau:
    Payment Success
    ↓
    Booking → CONFIRMED
    ↓
    TripSeat → BOOKED
    ↓
    Generate Ticket
    Không được xảy ra trường hợp:
    Booking = CONFIRMED

nhưng

TripSeat = AVAILABLE
hoặc:
TripSeat = BOOKED

nhưng

Booking không được xác nhận
Spring Service nên sử dụng transaction:
@Transactional
cho nghiệp vụ xác nhận booking.

27. Electronic Ticket
    Sau khi booking thành công, hệ thống tạo vé điện tử.
    Mỗi hành khách có thể có một Ticket riêng.
    Ví dụ:
    TRAIN TICKET

Booking
BK202608001

Sài Gòn
↓
Nha Trang

Train
SE2

Departure
20/08/2026 - 06:00

Coach
02

Seat
A12

Passenger
Nguyen Van A
Thông tin chính:
Ticket Code.
Booking Code.
Passenger.
Route.
Train.
Departure Date.
Departure Time.
Coach.
Seat / Bed.
Ticket được xem trực tiếp trên website.

28. QR Code - Optional
    QR Code không phải yêu cầu bắt buộc của MVP.
    Nếu triển khai, QR Code được hiển thị trên Electronic Ticket.
    QR chỉ cần chứa:
    ticketCode
    hoặc:
    ticketId
    Không chứa trực tiếp:
    CCCD.
    Passport.
    Thông tin thanh toán.
    Toàn bộ thông tin cá nhân.
    QR Code hiện tại không đi kèm workflow kiểm tra vé hoặc check-in.
    QR scanning cũng được xem là tính năng Optional.

29. Export Ticket PDF - Optional
    Nếu còn thời gian, Customer có thể tải vé điện tử dạng PDF.
    Không bắt buộc trong MVP.
    Thứ tự ưu tiên:
    Electronic Ticket trên Web
    ↓
    QR Code
    ↓
    Export PDF

30. My Trips
    Customer có trang quản lý booking.
    Có thể chia:
    Upcoming
    Completed
    Cancelled
    Thông tin:
    Route.
    Departure Date.
    Train.
    Coach.
    Seat / Bed.
    Booking Status.
    Payment Status.
    Action:
    View Booking.
    View Ticket.
    Cancel Booking.
    Không cần filter hoặc search nâng cao.

31. Booking Cancellation
    Customer có thể hủy booking khi:
    Booking = CONFIRMED
    và Trip chưa bắt đầu.
    Khi Customer hủy:
    CONFIRMED
    ↓
    CANCELLED
    Các TripSeat của booking:
    BOOKED
    ↓
    AVAILABLE
    Phiên bản đầu:
    Không tính phí hủy.
    Không refund.
    Không hoàn tiền qua ngân hàng.
    Không cần rule 24h / 48h.
    Vì đây là Mock Payment, việc hủy chỉ thay đổi trạng thái booking và trả lại chỗ.

32. Các quy tắc nghiệp vụ chính
    32.1 Một Booking chỉ thuộc một Trip
    1 Booking
    ↓
    1 Trip
    Không hỗ trợ:
    Multi-trip Booking.
    Connecting Train.
    Round Trip trong cùng Booking.

32.2 Một Booking có thể chứa nhiều hành khách
Ví dụ:
Booking BK001
│
├── Passenger A
├── Passenger B
└── Passenger C

32.3 Mỗi hành khách có một TripSeat
Passenger A → A01
Passenger B → A02
Passenger C → A03

32.4 Một TripSeat chỉ có thể thuộc một booking confirmed
Backend phải kiểm tra trước khi xác nhận.

32.5 Trip đã CANCELLED không thể booking
Nếu:
Trip.status = CANCELLED
thì Customer không được phép:
Chọn chỗ.
Tạo booking.
Thanh toán.

32.6 Trip trong quá khứ không thể booking
Customer chỉ tìm được Trip phù hợp với ngày yêu cầu và chưa diễn ra.

32.7 Booking chỉ được Confirm khi Payment thành công
Payment Status = PAID

→ Booking = CONFIRMED

33. Admin Dashboard
    Dashboard chỉ hiển thị overview cơ bản.
    Ví dụ:
    Total Bookings

Total Trips

Total Users

Total Revenue
Không cần biểu đồ phức tạp.
Dashboard chủ yếu giúp Admin có góc nhìn tổng quan.

34. Station Management
    Admin có thể:
    Xem danh sách Station.
    Thêm Station.
    Chỉnh sửa Station.
    Xóa Station nếu chưa được sử dụng.
    Thông tin:
    Station

name
code
city
address
latitude
longitude
Validation:
code không được trùng.
name không được để trống.

35. Route Management
    Admin có thể:
    Xem Route.
    Tạo Route.
    Chỉnh sửa Route.
    Xóa Route.
    Thông tin:
    Route

originStation
destinationStation
distance
Điều kiện:
originStation
!=
destinationStation
Ví dụ:
SGN-NTR

Sài Gòn → Nha Trang
Không quản lý ga trung gian.

36. Train Management
    Admin có thể:
    Xem danh sách Train.
    Thêm Train.
    Chỉnh sửa Train.
    Xem Train Detail.
    Thêm Coach vào Train.
    Cấu hình Coach.
    Ví dụ:
    SE2
    │
    ├── Coach 01 – SEAT
    ├── Coach 02 – SEAT
    ├── Coach 03 – SLEEPER
    └── Coach 04 – SLEEPER
    Coach được quản lý trong Train Detail.
    Không cần:
    /admin/coaches
    riêng.

37. Coach Configuration
    Khi Admin thêm Coach:
    Coach Type:
    SEAT / SLEEPER

Capacity:
...

Price Modifier:
...
Hệ thống có thể tự động tạo Seat.
Ví dụ Coach SEAT 16 chỗ:
A01
A02
...
A16
Không cần Admin thêm từng Seat thủ công.

38. Trip Management
    Admin tạo Trip dựa trên:
    Train.
    Route.
    Departure Date.
    Departure Time.
    Arrival Time.
    Base Price.
    Ví dụ:
    Train:
    SE2

Route:
Sài Gòn → Nha Trang

Date:
20/08/2026

Departure:
06:00

Arrival:
13:30

Base Price:
350.000đ
Khi tạo Trip, hệ thống tạo các TripSeat tương ứng với Seat của Train.
Ví dụ:
Train SE2
↓
40 Seats
↓
Create Trip
↓
40 TripSeat
Ban đầu:
TripSeat.status = AVAILABLE

39. Booking Management
    Admin có thể:
    Xem danh sách Booking.
    Search theo Booking Code.
    Xem Booking Detail.
    Xem Customer.
    Xem Passenger.
    Xem Trip.
    Xem Seat.
    Xem Payment Status.
    Hủy Booking.
    Admin không cần:
    Thay đổi Seat sau khi booking.
    Chỉnh sửa Payment thủ công.
    Refund.
    Đổi hành khách.

40. User Management
    Admin chỉ cần xem danh sách người dùng.
    Thông tin:
    Name.
    Email.
    Role.
    Created At.
    Role:
    CUSTOMER
    ADMIN
    Không cần CRUD User hoàn chỉnh.
    Tài khoản Admin có thể được tạo bằng Seed Data.

41. Authentication & Authorization
    Hệ thống sử dụng:
    Spring Security
    Session-based Authentication
    Role:
    CUSTOMER
    ADMIN
    Customer:
    Search Trip
    Booking
    My Trips
    Ticket
    Admin:
    Dashboard
    Station Management
    Route Management
    Train Management
    Trip Management
    Booking Management
    User List
    Backend phải kiểm tra quyền.
    Không chỉ ẩn UI frontend.
    Phiên bản đầu hỗ trợ:
    Register.
    Login.
    Logout.
    Session.
    Role-based Authorization.
    Không cần:
    Forgot Password.
    Reset Password.
    Email Verification.
    Google Login.
    OAuth.

42. Các Entity chính
    Database dự kiến gồm:
    User

Station

Route

Train

Coach

Seat

Trip

TripSeat

Booking

BookingPassenger

Ticket
Không cần:
Staff
CheckIn
RouteStation
TripStation
BookingSeat
Payment
trong MVP.

42.1 Quan hệ tổng quát
Station
↑
Route
↓
Station
Train:
Train
└── Coach
└── Seat
Trip:
Train
└── Trip
└── TripSeat
└── Seat
Booking:
User
└── Booking
│
├── Trip
│
└── BookingPassenger
│
├── TripSeat
└── Ticket

43. Kiến trúc hệ thống
    TrainGo sử dụng kiến trúc web application với Spring Boot và Thymeleaf.
    Browser
    ↓
    Thymeleaf
    HTML / CSS / JavaScript
    ↓
    Spring MVC
    ↓
    Controller
    ↓
    Service
    ↓
    Repository
    ↓
    Spring Data JPA / Hibernate
    ↓
    MySQL

43.1 Controller Layer
Controller:
Nhận HTTP Request.
Nhận dữ liệu Form.
Validate dữ liệu cơ bản.
Gọi Service.
Trả View.
Redirect.
Không đặt business logic phức tạp trong Controller.

43.2 Service Layer
Service xử lý nghiệp vụ.
Ví dụ:
AuthService

TripService

TrainService

SeatService

BookingService

TicketService
Service xử lý:
Search Trip.
Seat Holding.
Create Booking.
Confirm Booking.
Cancel Booking.
Generate Ticket.

43.3 Repository Layer
Repository giao tiếp với database.
Ví dụ:
UserRepository

StationRepository

RouteRepository

TrainRepository

CoachRepository

SeatRepository

TripRepository

TripSeatRepository

BookingRepository

TicketRepository
Repository sử dụng Spring Data JPA.

44. Tech Stack
    Frontend
    HTML5.
    CSS3.
    JavaScript.
    Thymeleaf.
    Bootstrap.
    Backend
    Java.
    Spring Boot.
    Spring MVC.
    Database
    MySQL.
    ORM
    Spring Data JPA.
    Hibernate.
    Authentication & Authorization
    Spring Security.
    Session-based Authentication.
    Validation
    Bean Validation.
    Ví dụ:
    @NotBlank
    @NotNull
    @Email
    @Size
    @Min
    Optional Library
    QR:
    ZXing
    Map:
    Leaflet
    OpenStreetMap

45. Yêu cầu phi chức năng
    Ngoài chức năng chính, hệ thống cần đáp ứng một số yêu cầu cơ bản.

45.1 Responsive
Các trang Customer phải sử dụng được trên:
Desktop.
Tablet.
Mobile.
Đặc biệt:
Landing Page.
Search.
Seat Selection.
My Trips.
Ticket.

45.2 Validation
Backend phải validate dữ liệu.
Không chỉ phụ thuộc vào validation frontend.
Ví dụ:
Email hợp lệ.
Station bắt buộc.
Ngày hợp lệ.
Số hành khách hợp lệ.
Trip tồn tại.
Seat tồn tại.
TripSeat còn khả dụng.

45.3 Security
Password phải được hash.
Customer không truy cập trang Admin.
Customer không xem booking của người khác.
Backend kiểm tra role.
Backend kiểm tra ownership.

45.4 Data Consistency
Các nghiệp vụ booking quan trọng phải sử dụng transaction.
Đặc biệt:
Confirm Booking.
Cancel Booking.
Seat Holding.

45.5 Error Handling
Hệ thống cần xử lý các trường hợp:
Trip không tồn tại.
Trip bị hủy.
Ghế vừa bị người khác đặt.
Seat Holding hết hạn.
Booking không tồn tại.
Booking không thuộc Customer.
Form không hợp lệ.

46. In Scope
    Public
    Landing Page.
    Search Form.
    Popular Routes.
    How It Works.
    Login.
    Register.

Customer
Register.
Login / Logout.
Search Trip.
Trip Results.
Trip Detail.
Coach Selection.
Interactive Seat Selection.
Interactive Sleeper Selection.
Trip Seat Availability.
Seat Holding.
Passenger Information.
Booking Review.
Booking.
Mock Payment.
Electronic Ticket.
My Trips.
Booking Cancellation.

Admin
Dashboard cơ bản.
Station Management.
Route Management.
Train Management.
Coach Configuration.
Automatic Seat Generation.
Trip Management.
Booking Management.
User List.

47. Optional
    Các tính năng chỉ triển khai sau khi MVP ổn định:
    QR Code trên Ticket.
    QR scanning.
    Railway Map.
    Export Ticket PDF.
    Các tính năng Optional không được làm ảnh hưởng đến tiến độ luồng booking chính.

48. Out of Scope
    Phiên bản đầu không triển khai:
    Staff Role.
    Staff Portal.
    Ticket Verification Workflow.
    Passenger Check-in.
    Thanh toán ngân hàng thật.
    Payment Gateway.
    Refund thật.
    Tích hợp hệ thống Đường sắt Việt Nam.
    GPS Tracking.
    Theo dõi tàu real-time.
    Multi-train Booking.
    Connecting Train.
    Round-trip trong một Booking.
    Ga trung gian phức tạp.
    RouteStation.
    TripStation.
    Dynamic Pricing.
    Adult / Child Pricing.
    Giá người cao tuổi.
    Student Discount.
    Loyalty Point.
    Voucher.
    Promotion System.
    Food Ordering.
    Quản lý bảo trì tàu.
    Quản lý lịch làm việc nhân viên.
    Email Notification.
    SMS Notification.
    Hóa đơn điện tử.
    Accounting System.
    Advanced Analytics.
    Advanced Search Filter.
    OAuth.
    Forgot Password.
    Reset Password.

49. Các màn hình dự kiến
    Public
    /
    Landing Page:
    Hero.
    Search.
    Popular Routes.
    How It Works.
    Railway Map nếu triển khai.
    Authentication:
    /login

/register

Booking
/trips

/trips/:id

/booking/:tripId/seats

/booking/passengers

/booking/review

/booking/payment

/booking/success

Customer
/my-trips

/my-trips/:bookingId

/tickets/:ticketId
Có thể bổ sung:
/profile
nếu cần.

Admin
/admin

/admin/stations

/admin/routes

/admin/trains

/admin/trains/:id

/admin/trips

/admin/bookings

/admin/bookings/:id

/admin/users
Coach và Seat được quản lý trong:
/admin/trains/:id
Không có:
/staff

50. MVP
    MVP cần hoàn thành được luồng xuyên suốt:
    Admin
    ↓
    Tạo Station
    ↓
    Tạo Route
    ↓
    Tạo Train
    ↓
    Cấu hình Coach + Seat
    ↓
    Tạo Trip
    ↓
    Customer
    ↓
    Tìm chuyến
    ↓
    Chọn Trip
    ↓
    Chọn Coach
    ↓
    Chọn Seat / Bed
    ↓
    Seat Holding
    ↓
    Nhập Passenger
    ↓
    Booking Review
    ↓
    Mock Payment
    ↓
    Booking Confirmed
    ↓
    Electronic Ticket
    ↓
    My Trips
    Các feature MVP:
    Authentication.
    Role-based Authorization.
    Station Management.
    Route Management.
    Train Management.
    Coach Configuration.
    Seat Generation.
    Trip Management.
    TripSeat.
    Search Trip.
    Trip Detail.
    Interactive Coach.
    Interactive Seat / Bed Selection.
    Seat Holding.
    Passenger Information.
    Booking Review.
    Booking.
    Mock Payment.
    Electronic Ticket.
    My Trips.
    Booking Cancellation.
    Optional:
    QR Code.
    QR scanning.
    Railway Map.
    Export Ticket PDF.

51. Feature nổi bật
    TrainGo không tập trung vào số lượng CRUD.
    Project tập trung vào những nghiệp vụ có giá trị kỹ thuật và trải nghiệm rõ ràng.

51.1 Interactive Train Coach
Customer có thể xem cấu trúc toa và chọn trực tiếp ghế hoặc giường.
Ví dụ:
Coach 01

[A01] [A02] [A03] [A04]

[A05] [A06] [A07] [A08]

51.2 Trip-based Seat Availability
Trạng thái chỗ được quản lý theo từng chuyến.
SE2 - A01

20/08 → BOOKED

21/08 → AVAILABLE
Đây là nghiệp vụ quan trọng để quản lý đúng việc một Train được sử dụng cho nhiều Trip.

51.3 Seat Holding
AVAILABLE
↓
HELD
↓
BOOKED
Hoặc:
AVAILABLE
↓
HELD
↓
AVAILABLE
nếu hết thời gian.

51.4 Booking Workflow
Search
↓
Trip
↓
Coach
↓
Seat
↓
Passenger
↓
Review
↓
Payment
↓
Ticket

51.5 Electronic Ticket
Sau khi booking thành công, Customer nhận được vé điện tử.
QR Code có thể được bổ sung sau.

51.6 Role-based System
Hệ thống có hai role:
CUSTOMER
ADMIN
Customer tập trung vào booking.
Admin tập trung vào dữ liệu vận hành booking.

52. Trải nghiệm sản phẩm mong muốn
    TrainGo cần mang cảm giác của một website đặt vé thật nhưng vẫn đơn giản.
    Định hướng:
    Simple

- Visual
- Fast
- Clear
  Customer không cần hiểu cấu trúc dữ liệu bên trong.
  Luồng sử dụng cần tự nhiên:
  Tôi muốn đi đâu → có chuyến nào → còn chỗ nào → chọn chỗ → nhập hành khách → thanh toán → nhận vé.
  Những nghiệp vụ phức tạp được hệ thống xử lý phía sau.

53. Định hướng UI
    Phong cách:
    Modern Travel / Transportation
    Đặc điểm:
    Background sáng.
    Card đơn giản.
    Typography rõ.
    Màu primary dễ nhận diện.
    Whitespace hợp lý.
    Responsive.
    Trạng thái Seat rõ ràng.
    Booking Flow tập trung.
    Hạn chế yếu tố gây nhiễu.
    Landing Page ưu tiên:
    Branding.
    Search.
    Travel experience.
    Booking Page ưu tiên:
    Functional UI.
    Rõ trạng thái.
    Ít bước thừa.
    Admin ưu tiên:
    Table.
    Form.
    Search.
    Filter cơ bản.
    CRUD rõ ràng.

54. Project Summary
    TrainGo là website mô phỏng hệ thống đặt vé tàu trực tuyến với hai nhóm người dùng:
    Customer
    Admin
    Luồng chính:
    Search Trip
    ↓
    Select Trip
    ↓
    Select Coach
    ↓
    Select Seat / Bed
    ↓
    Passenger Information
    ↓
    Booking Review
    ↓
    Mock Payment
    ↓
    Electronic Ticket
    Project tập trung vào:
    Search Trip.
    Interactive Train Coach.
    Interactive Seat / Bed Selection.
    Trip-based Seat Availability.
    Seat Holding.
    Booking Workflow.
    Mock Payment.
    Electronic Ticket.
    My Trips.
    Booking Cancellation.
    Role-based Authorization.
    Các tính năng mở rộng:
    QR Code.
    QR scanning.
    Railway Map.
    Export Ticket PDF.
    TrainGo chủ động loại bỏ:
    Staff.
    Ticket Verification.
    Check-in.
    Thanh toán thật.
    Refund.
    Ga trung gian.
    GPS.
    Dynamic Pricing.
    Loyalty.
    Voucher.
    Advanced Analytics.
    Các nghiệp vụ vận hành đường sắt không phục vụ trực tiếp cho quá trình booking.
    Mục tiêu cuối cùng là xây dựng một luồng đặt vé tàu hoàn chỉnh, trực quan, dễ sử dụng và dễ demo, đồng thời thể hiện được các nghiệp vụ backend quan trọng như:
    Authentication.
    Authorization.
    Data Relationship.
    Transaction.
    Seat Availability.
    Seat Holding.
    Booking.
    Business Validation.
    Ưu tiên của project là:
    Hoàn thiện tốt luồng chính trước khi mở rộng thêm tính năng.
