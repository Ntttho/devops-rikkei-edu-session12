# PHÂN TÍCH NGHIỆP VỤ: MỞ TÀI KHOẢN NGÂN HÀNG TRỰC TUYẾN (eKYC)
## ABC Bank - Zero Manual Operation

**Phiên bản:** 1.0  
**Ngày:** 10/07/2026  
**Người phân tích:** Business Analyst Team

---

## 1. ACTORS (CÁC TÁC NHÂN)

| Actor | Mô tả | Vai trò |
|-------|-------|---------|
| **Khách hàng (Customer)** | Cá nhân cư trú tại Việt Nam, có CCCD hợp lệ | Người dùng cuối, thực hiện đăng ký và xác thực danh tính qua app |
| **Mobile App (ABC Bank App)** | Ứng dụng di động iOS/Android | Giao diện thu thập thông tin, chụp ảnh, quét khuôn mặt |
| **eKYC Engine** | Hệ thống xử lý trung tâm | OCR CCCD, Liveness check, đối chiếu dữ liệu, ra quyết định tự động |
| **Core Banking System (CBS)** | Hệ thống lõi ngân hàng | Tạo tài khoản, cấp số tài khoản, quản lý số dư |
| **National ID Database** | Cơ sở dữ liệu định danh quốc gia (VNeID / C06) | Xác minh tính hợp lệ của CCCD và thông tin công dân |
| **Admin / Back-office** | Nhân viên vận hành (vai trò tối thiểu) | Xử lý các trường hợp ngoại lệ không tự động được |
| **Compliance Officer** | Cán bộ tuân thủ | Giám sát quy trình AML/KYC theo quy định NHNN |
| **Notification Service** | Dịch vụ gửi thông báo | Gửi SMS/Email/Push khi tài khoản được kích hoạt |

---

## 2. BUSINESS FLOW (LUỒNG NGHIỆP VỤ)

### 2.1. Luồng chính - Mở tài khoản thành công

```
[Bắt đầu]
    │
    ▼
[1] Khách hàng tải ABC Bank App và mở màn hình Đăng ký
    │
    ▼
[2] Nhập thông tin cơ bản: Họ tên, SĐT, Email, Số CCCD
    │
    ▼
[3] Hệ thống kiểm tra định dạng + trùng lặp (SĐT/CCCD đã tồn tại?)
    │
    ▼
[4] Khách hàng chụp ảnh mặt trước + mặt sau CCCD
    │
    ▼
[5] OCR trích xuất dữ liệu từ ảnh CCCD
    │
    ▼
[6] Đối chiếu dữ liệu OCR với thông tin đã nhập + CSDL quốc gia
    │
    ▼
[7] Khách hàng thực hiện Liveness Check (quét khuôn mặt, chớp mắt/xoay đầu)
    │
    ▼
[8] So khớp khuôn mặt với ảnh trên CCCD (Face Matching)
    │
    ▼
[9] Hệ thống tự động tạo tài khoản → Cấp số tài khoản
    │
    ▼
[10] Gửi thông báo kích hoạt thành công qua SMS/Email/Push
    │
    ▼
[Kết thúc - Tài khoản ACTIVE]
```

### 2.2. Luồng ngoại lệ

| Bước | Tình huống | Xử lý |
|------|-----------|-------|
| 3 | SĐT hoặc CCCD đã tồn tại | Thông báo lỗi, hướng dẫn đăng nhập hoặc liên hệ CSKH |
| 5 | OCR không đọc được ảnh | Yêu cầu chụp lại, hướng dẫn ánh sáng/góc chụp |
| 6 | Dữ liệu không khớp | Từ chối tự động, ghi nhận hồ sơ chờ xử lý thủ công |
| 8 | Face Matching dưới ngưỡng tin cậy | Yêu cầu quét lại tối đa 3 lần, sau đó chuyển manual review |
| 9 | Core Banking lỗi | Retry 3 lần, nếu thất bại → thông báo và lưu trạng thái PENDING |

---

## 3. FUNCTIONAL REQUIREMENTS (YÊU CẦU CHỨC NĂNG)

### Module 1: Đăng ký tài khoản cơ bản
| Mã | Yêu cầu | Mô tả |
|----|---------|-------|
| FR-01 | Đăng ký thông tin | Cho phép nhập họ tên, SĐT, email, số CCCD |
| FR-02 | Kiểm tra trùng lặp | Kiểm tra SĐT và CCCD chưa được đăng ký |
| FR-03 | Xác thực OTP | Gửi và xác minh OTP qua SMS trước khi tiếp tục |

### Module 2: Upload & Đọc CCCD (OCR)
| Mã | Yêu cầu | Mô tả |
|----|---------|-------|
| FR-04 | Upload ảnh CCCD | Chụp/tải ảnh mặt trước và mặt sau |
| FR-05 | OCR trích xuất | Tự động đọc số CCCD, họ tên, ngày sinh, địa chỉ |
| FR-06 | Kiểm tra chất lượng ảnh | Phát hiện ảnh mờ, chói, thiếu góc |

### Module 3: Xác thực khuôn mặt (Liveness)
| Mã | Yêu cầu | Mô tả |
|----|---------|-------|
| FR-07 | Liveness Detection | Phát hiện khuôn mặt thật, chống ảnh/video giả |
| FR-08 | Face Matching | So khớp khuôn mặt với ảnh trên CCCD (ngưỡng ≥ 85%) |
| FR-09 | Giới hạn thử lại | Tối đa 3 lần quét khuôn mặt mỗi phiên |

### Module 4: Đối chiếu & Kích hoạt
| Mã | Yêu cầu | Mô tả |
|----|---------|-------|
| FR-10 | Đối chiếu dữ liệu | So sánh OCR ↔ nhập tay ↔ CSDL quốc gia |
| FR-11 | Tạo tài khoản tự động | Gọi Core Banking tạo tài khoản khi hợp lệ |
| FR-12 | Cấp số tài khoản | Sinh số tài khoản 10-14 chữ số tự động |
| FR-13 | Gửi thông báo | SMS/Email/Push khi kích hoạt thành công |

### Module 5: Quản trị & Giám sát
| Mã | Yêu cầu | Mô tả |
|----|---------|-------|
| FR-14 | Dashboard giám sát | Theo dõi số lượng đăng ký, tỷ lệ thành công/thất bại |
| FR-15 | Xử lý ngoại lệ | Back-office xem và xử lý hồ sơ chờ duyệt thủ công |

---

## 4. NON-FUNCTIONAL REQUIREMENTS (YÊU CẦU PHI CHỨC NĂNG)

| Mã | Nhóm | Yêu cầu | Tiêu chí đo lường |
|----|------|---------|-------------------|
| NFR-01 | Bảo mật | Mã hóa dữ liệu CCCD và ảnh khuôn mặt (AES-256) | 100% dữ liệu nhạy cảm được mã hóa at-rest và in-transit |
| NFR-02 | Bảo mật | Tuân thủ Luật An ninh mạng và NĐ 13/2023 về bảo vệ dữ liệu cá nhân | Audit pass 100% |
| NFR-03 | Hiệu năng | Thời gian phản hồi API đăng ký | ≤ 2 giây (P95) |
| NFR-04 | Hiệu năng | Thời gian xử lý OCR + Face Matching | ≤ 10 giây (P95) |
| NFR-05 | Khả dụng | Uptime hệ thống eKYC | ≥ 99.9% |
| NFR-06 | Khả dụng | Hỗ trợ 5.000 phiên đăng ký đồng thời | Load test pass |
| NFR-07 | Khả mở rộng | Kiến trúc microservice, scale horizontal | Thêm node trong 15 phút |
| NFR-08 | Tuân thủ | Đáp ứng Thông tư 23/2014/TT-NHNN về eKYC | Được NHNN phê duyệt |
| NFR-09 | Khả dụng sử dụng | App hỗ trợ iOS 14+, Android 8+ | Test trên 20 thiết bị phổ biến |
| NFR-10 | Khôi phục | RPO ≤ 1 giờ, RTO ≤ 4 giờ | DR drill hàng quý |

---

## 5. ASSUMPTIONS & BUSINESS RULES

### 5.1. Giả định (Assumptions)

| Mã | Giả định |
|----|----------|
| A-01 | Khách hàng có CCCD gắn chip hợp lệ, còn hiệu lực |
| A-02 | Khách hàng có kết nối internet ổn định và thiết bị có camera |
| A-03 | ABC Bank đã tích hợp API với CSDL định danh quốc gia (VNeID) |
| A-04 | Core Banking System hỗ trợ API tạo tài khoản real-time |
| A-05 | Khách hàng đủ 18 tuổi trở lên |

### 5.2. Quy tắc nghiệp vụ (Business Rules)

| Mã | Quy tắc | Mô tả |
|----|---------|-------|
| BR-01 | Một CCCD chỉ mở 01 tài khoản | Không cho phép trùng số CCCD |
| BR-02 | Một SĐT chỉ liên kết 01 tài khoản | Tránh đăng ký trùng |
| BR-03 | CCCD phải đủ 12 chữ số | Định dạng CCCD mới |
| BR-04 | Face Matching ≥ 85% mới tự động duyệt | Dưới ngưỡng → manual review |
| BR-05 | OCR confidence ≥ 90% mới tiếp tục | Dưới ngưỡng → chụp lại |
| BR-06 | Tài khoản tự động ACTIVE khi tất cả bước pass | Không cần giao dịch viên |
| BR-07 | Hồ sơ thất bại lưu trữ 90 ngày | Phục vụ audit và khiếu nại |
| BR-08 | Khách hàng trong danh sách AML bị từ chối | Kiểm tra trước khi kích hoạt |

---

## 6. USER STORIES

| ID | User Story | Ưu tiên | Acceptance Criteria |
|----|-----------|---------|---------------------|
| US-01 | **As a** khách hàng mới, **I want to** tải app và đăng ký thông tin cơ bản, **So that** tôi có thể bắt đầu quy trình mở tài khoản online. | Must | Nhập đủ 4 trường; validate định dạng; nhận OTP xác nhận SĐT |
| US-02 | **As a** khách hàng, **I want to** chụp ảnh 2 mặt CCCD qua app, **So that** hệ thống xác minh danh tính của tôi. | Must | Hướng dẫn góc chụp; kiểm tra chất lượng ảnh trước khi gửi |
| US-03 | **As a** hệ thống eKYC, **I want to** tự động OCR dữ liệu từ CCCD, **So that** giảm nhập liệu thủ công. | Must | Trích xuất đúng ≥ 90% trường; hiển thị kết quả cho KH xác nhận |
| US-04 | **As a** khách hàng, **I want to** quét khuôn mặt để xác thực liveness, **So that** ngân hàng biết tôi là người thật. | Must | Phát hiện ảnh giả; hoàn thành trong ≤ 30 giây |
| US-05 | **As a** hệ thống, **I want to** so khớp khuôn mặt với ảnh CCCD, **So that** đảm bảo đúng chủ thể. | Must | Ngưỡng matching ≥ 85%; ghi log kết quả |
| US-06 | **As a** hệ thống, **I want to** đối chiếu dữ liệu với CSDL quốc gia, **So that** xác minh CCCD hợp lệ. | Must | Gọi API VNeID; xử lý timeout gracefully |
| US-07 | **As a** khách hàng, **I want to** nhận số tài khoản ngay khi xác thực thành công, **So that** tôi sử dụng dịch vụ ngân hàng ngay. | Must | Nhận SMS/Email trong 1 phút; trạng thái ACTIVE |
| US-08 | **As a** khách hàng, **I want to** được thông báo rõ lý do khi đăng ký thất bại, **So that** tôi biết cách khắc phục. | Must | Thông báo lỗi cụ thể bằng tiếng Việt |
| US-09 | **As a** back-office staff, **I want to** xem danh sách hồ sơ chờ duyệt thủ công, **So that** xử lý các trường hợp ngoại lệ. | Should | Lọc theo ngày, trạng thái; xem chi tiết hồ sơ |
| US-10 | **As a** compliance officer, **I want to** xem báo cáo tuân thủ KYC, **So that** đáp ứng yêu cầu NHNN. | Should | Export PDF/Excel; lọc theo khoảng thời gian |
| US-11 | **As a** khách hàng, **I want to** chụp lại ảnh CCCD nếu bị từ chối, **So that** hoàn tất đăng ký mà không phải bắt đầu lại. | Should | Giữ thông tin đã nhập; chỉ yêu cầu chụp lại |
| US-12 | **As a** admin, **I want to** theo dõi dashboard tỷ lệ thành công eKYC, **So that** phát hiện sự cố kịp thời. | Could | Biểu đồ real-time; cảnh báo khi tỷ lệ lỗi > 5% |
| US-13 | **As a** khách hàng, **I want to** lưu nháp hồ sơ đăng ký, **So that** tiếp tục sau khi bị gián đoạn. | Could | Lưu 24 giờ; khôi phục từ bước đang dở |
| US-14 | **As a** hệ thống, **I want to** kiểm tra danh sách AML, **So that** tuân thủ quy định phòng chống rửa tiền. | Must | Tích hợp AML screening trước khi kích hoạt |
| US-15 | **As a** khách hàng, **I want to** đăng ký bằng eID trên VNeID, **So that** rút ngắn thời gian xác thực. | Won't | Giai đoạn 2 - ngoài scope hiện tại |

---

## 7. USE CASE

### 7.1. Bảng tóm tắt

| Mã | Tên Use Case | Actor chính |
|----|-------------|-------------|
| UC-01 | Đăng ký thông tin cơ bản | Khách hàng |
| UC-02 | Xác thực OTP qua SMS | Khách hàng |
| UC-03 | Upload và OCR CCCD | Khách hàng |
| UC-04 | Xác thực Liveness | Khách hàng |
| UC-05 | Đối chiếu dữ liệu tự động | eKYC Engine |
| UC-06 | Tạo tài khoản tự động | eKYC Engine |
| UC-07 | Gửi thông báo kích hoạt | Notification Service |
| UC-08 | Xử lý hồ sơ ngoại lệ | Back-office Staff |
| UC-09 | Giám sát dashboard eKYC | Admin |
| UC-10 | Kiểm tra tuân thủ AML | Compliance Officer |

### 7.2. Chi tiết Use Case tiêu biểu

#### UC-01: Đăng ký thông tin cơ bản

- **Actor chính:** Khách hàng
- **Mô tả:** Khách hàng nhập thông tin cá nhân để bắt đầu quy trình mở tài khoản.
- **Precondition:** App đã cài đặt; khách hàng chưa có tài khoản ABC Bank.
- **Main Flow:**
  1. Khách hàng mở màn hình Đăng ký
  2. Nhập họ tên, SĐT, email, số CCCD
  3. Hệ thống validate định dạng
  4. Hệ thống kiểm tra trùng lặp SĐT/CCCD
  5. Lưu thông tin tạm, chuyển sang bước OTP
- **Alternative Flow:**
  - 4a. SĐT đã tồn tại → Hiển thị thông báo, đề xuất Đăng nhập
  - 4b. CCCD đã tồn tại → Từ chối, hướng dẫn liên hệ CSKH
- **Postcondition:** Thông tin được lưu tạm, trạng thái PENDING.

#### UC-06: Tạo tài khoản tự động

- **Actor chính:** eKYC Engine
- **Mô tả:** Hệ thống tự động tạo tài khoản khi tất cả bước xác thực thành công.
- **Precondition:** OCR, Liveness, Face Matching, đối chiếu CSDL đều PASS.
- **Main Flow:**
  1. eKYC Engine gửi yêu cầu tạo tài khoản đến Core Banking
  2. Core Banking sinh số tài khoản
  3. Cập nhật trạng thái ACTIVE
  4. Kích hoạt Notification Service gửi thông báo
- **Alternative Flow:**
  - 2a. Core Banking timeout → Retry 3 lần → Nếu vẫn lỗi, lưu PENDING và thông báo KH
- **Postcondition:** Tài khoản ACTIVE, khách hàng nhận số tài khoản.

---

## 8. RỦI RO NGHIỆP VỤ & KPI

### Rủi ro

| Rủi ro | Mức độ | Giảm thiểu |
|--------|--------|------------|
| Giả mạo danh tính bằng deepfake | Cao | Liveness detection nâng cao, giới hạn retry |
| OCR sai với CCCD cũ/mờ | Trung bình | Hướng dẫn chụp, kiểm tra confidence score |
| Core Banking downtime | Cao | Queue + retry, trạng thái PENDING |
| Không tuân thủ NHNN | Cao | Review pháp lý trước go-live |

### KPI đề xuất

| KPI | Mục tiêu |
|-----|----------|
| Tỷ lệ mở tài khoản tự động (không cần GDV) | ≥ 95% |
| Thời gian trung bình hoàn tất eKYC | ≤ 5 phút |
| Tỷ lệ gian lận phát hiện | ≥ 99% |
| Điểm hài lòng khách hàng (CSAT) | ≥ 4.5/5 |

---

*Tài liệu được sinh từ chuỗi Prompt phân tích nghiệp vụ eKYC ABC Bank.*
