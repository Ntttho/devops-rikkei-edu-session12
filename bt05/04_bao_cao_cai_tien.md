# BÁO CÁO TÓM TẮT: TRƯỚC VÀ SAU REFACTOR
## ABC Bank eKYC - Bài tập 05

**Dự án:** API Đăng ký tài khoản eKYC - ABC Bank  
**Phạm vi Refactor:** RegistrationServiceImpl và các thành phần liên quan  
**Ngày thực hiện:** 16/07/2026  

---

## 1. BẢNG SO SÁNH TRƯỚC/SAU REFACTOR

| Tiêu chí đánh giá | Trước Refactor (v1) | Sau Refactor (v2) | Điểm trước | Điểm sau |
|-------------------|--------------------|--------------------|-----------|----------|
| **Cấu trúc lớp** | 5 class chính, RegistrationServiceImpl ôm đồm nhiều trách nhiệm | 8 class, mỗi class 1 trách nhiệm rõ ràng | 4/10 | 9/10 |
| **SRP** (Single Responsibility) | 1 Service làm: validate + nghiệp vụ + sinh số TK + masking | Tách riêng: Validator, Generator, MaskingUtil | 3/10 | 9/10 |
| **OCP** (Open/Closed) | Thêm tính năng phải sửa Service | Thêm Validator mới không sửa Service | 5/10 | 8/10 |
| **DIP** (Dependency Inversion) | Service phụ thuộc trực tiếp vào Random | Service phụ thuộc vào AccountNumberGenerator interface | 5/10 | 9/10 |
| **Code Smell** | 7 code smell được xác định | Đã giải quyết 6/7 (86%) | 3/10 | 9/10 |
| **Exception Handling** | Rải rác, không nhất quán format lỗi | Tập trung tại GlobalExceptionHandler, format chuẩn | 4/10 | 9/10 |
| **Logging** | Log cơ bản, không có requestId/traceId | Structured logging với MDC traceId, RequestLoggingFilter | 3/10 | 9/10 |
| **Testability** | Khó mock do nhiều logic trong 1 class | Dễ unit test từng component riêng | 4/10 | 9/10 |
| **Maintainability** | Sửa 1 tính năng có thể ảnh hưởng toàn bộ Service | Sửa từng component độc lập | 4/10 | 9/10 |
| **Bảo mật Log** | Có che giấu dữ liệu nhưng method private trong Service | DataMaskingUtil dùng được mọi nơi, consistent | 6/10 | 9/10 |
| **Cấu hình** | Magic number "1901" hardcode trong code | Đưa vào @Value từ application.properties | 3/10 | 9/10 |

---

## 2. CÁC CODE SMELL ĐÃ ĐƯỢC GIẢI QUYẾT

| # | Code Smell | Trước | Sau | Trạng thái |
|---|-----------|-------|-----|-----------|
| 1 | God Class - Service quá nhiều trách nhiệm | RegistrationServiceImpl: ~80 dòng, 5 trách nhiệm | Tách thành 4 class riêng | ✅ Đã giải quyết |
| 2 | Magic Number "1901" hardcode | `new StringBuilder("1901")` trong code | `@Value("${abcbank.account.prefix:1901}")` | ✅ Đã giải quyết |
| 3 | Validation logic trong Service | 15 dòng validate trong register() | Delegate cho RegistrationValidator | ✅ Đã giải quyết |
| 4 | Account generation trong Service | generateAccountNumber() trong Service | AccountNumberGenerator @Component | ✅ Đã giải quyết |
| 5 | Data masking utilities trong Service | 3 private method mask trong Service | DataMaskingUtil class riêng | ✅ Đã giải quyết |
| 6 | Không có Request ID/Trace ID | Không có | RequestLoggingFilter + MDC | ✅ Đã giải quyết |
| 7 | Exception message hardcode | String cứng trong code | Có thể mở rộng lấy từ properties | ⚠️ Cải thiện một phần |

---

## 3. SO SÁNH EXCEPTION HANDLING

### Trước Refactor
```
BusinessException → GlobalExceptionHandler (bắt)
MethodArgumentNotValidException → GlobalExceptionHandler (bắt)
Mọi exception khác → 500 Internal Server Error
```
- Không có ValidationException riêng
- ErrorResponse không có traceId, không có details Map
- Không xử lý JSON parse error, Method Not Allowed

### Sau Refactor
```
ValidationException (trùng dữ liệu) → 409 Conflict + details Map
BusinessException (lỗi nghiệp vụ) → HTTP tương ứng + traceId
MethodArgumentNotValidException → 400 + fieldErrors Map + traceId
HttpMessageNotReadableException → 400 + thông báo rõ ràng
HttpRequestMethodNotSupportedException → 405 + thông báo
Exception (unexpected) → 500 + traceId để debug
```

---

## 4. SO SÁNH LOGGING

| Khía cạnh | Trước | Sau |
|-----------|-------|-----|
| TraceId | Không có | MDC + X-Trace-Id header |
| Request logging | Thủ công trong từng method | RequestLoggingFilter tự động |
| Response logging | Không có | RequestLoggingFilter tự động |
| Thời gian xử lý | Không đo | Tự động tính trong Filter |
| Che giấu dữ liệu | Có (trong Service) | Có (DataMaskingUtil - tập trung) |
| Log level sử dụng | INFO, WARN | INFO, WARN, ERROR, DEBUG (đúng cấp) |

---

## 5. KẾT QUẢ ĐO LƯỜNG

| Chỉ số | Trước | Sau |
|--------|-------|-----|
| Số dòng code RegistrationServiceImpl | ~85 dòng | ~55 dòng |
| Số trách nhiệm của Service | 5 | 1 (orchestration) |
| Số class mới được tách ra | 0 | 4 (Validator, Generator, MaskingUtil, Filter) |
| Code smell còn lại | 7 | 1 |
| Loại exception được xử lý | 3 | 6 |
| Điểm trung bình (11 tiêu chí) | 3.9/10 | 8.9/10 |
| **Cải thiện** | | **+5.0 điểm (+128%)** |

---

## 6. KẾT LUẬN

Quá trình Refactor đã giúp hệ thống eKYC cải thiện đáng kể về nhiều mặt. Trước Refactor, `RegistrationServiceImpl` là một "God Class" vi phạm nghiêm trọng nguyên lý SRP (Single Responsibility Principle), khi phải đảm nhiệm đến 5 trách nhiệm khác nhau trong cùng một lớp. Sau Refactor, mỗi class chỉ có một lý do để thay đổi, giúp hệ thống dễ mở rộng và bảo trì hơn nhiều.

Exception Handling đã được tập trung hoàn toàn tại `GlobalExceptionHandler`, đảm bảo mọi lỗi đều trả về cùng định dạng `ErrorResponse` nhất quán với `traceId` để hỗ trợ debugging. Structured Logging với `RequestLoggingFilter` và MDC tự động ghi lại toàn bộ vòng đời của mỗi request mà không cần sửa code tại từng endpoint.

Việc tách `AccountNumberGenerator` thành `@Component` riêng và đưa prefix "1901" vào configuration giúp loại bỏ Magic Number, tạo nền tảng để dễ dàng thay thế bằng lời gọi API Core Banking thực sự trong tương lai mà không cần sửa `RegistrationServiceImpl`. Tổng thể, điểm chất lượng trung bình tăng từ 3.9/10 lên 8.9/10, tương đương mức cải thiện 128% - một kết quả ấn tượng chứng minh giá trị của việc đầu tư vào Refactor và Clean Code.
