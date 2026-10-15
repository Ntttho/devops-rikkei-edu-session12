# MÃ NGUỒN SAU REFACTOR - BÀI TẬP 05
## Phiên bản v2 - Đã áp dụng Clean Code & Design Pattern

---

## CÁC THAY ĐỔI SO VỚI PHIÊN BẢN V1

| Class | Thay đổi |
|-------|---------|
| `RegistrationServiceImpl` | Gọn lại - chỉ còn orchestration logic |
| `RegistrationValidator` | **[MỚI]** Tách validation logic riêng |
| `AccountNumberGenerator` | **[MỚI]** Tách logic sinh số tài khoản |
| `DataMaskingUtil` | **[MỚI]** Tách utility che giấu dữ liệu |
| `GlobalExceptionHandler` | Nâng cấp - xử lý đầy đủ hơn |
| `ErrorResponse` | Nâng cấp - thêm traceId và details |
| `RequestLoggingFilter` | **[MỚI]** Logging có cấu trúc tự động |

---

## 1. DataMaskingUtil.java (Utility mới)

```java
package com.abcbank.ekyc.util;

/**
 * Utility class che giấu (mask) dữ liệu nhạy cảm trong log
 * và response để bảo vệ thông tin cá nhân của khách hàng.
 *
 * <p>Tuân thủ NĐ 13/2023 về bảo vệ dữ liệu cá nhân: không log
 * thông tin nhạy cảm dưới dạng plaintext.</p>
 */
public final class DataMaskingUtil {

    // Utility class - không cho phép khởi tạo
    private DataMaskingUtil() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Che giấu số CCCD: chỉ hiển thị 6 số đầu và 3 số cuối
     * Ví dụ: "012345678901" -> "012345***901"
     */
    public static String maskCitizenId(String citizenId) {
        if (citizenId == null || citizenId.length() < 9) return "***";
        return citizenId.substring(0, 6) + "***" + citizenId.substring(9);
    }

    /**
     * Che giấu số điện thoại: hiển thị 3 số đầu và 3 số cuối
     * Ví dụ: "0912345678" -> "091****678"
     */
    public static String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) return "***";
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }

    /**
     * Che giấu địa chỉ email: hiển thị ký tự đầu và domain
     * Ví dụ: "user@gmail.com" -> "u***@gmail.com"
     */
    public static String maskEmail(String email) {
        if (email == null || !email.contains("@")) return "***";
        String local = email.split("@")[0];
        String domain = email.split("@")[1];
        return local.charAt(0) + "***@" + domain;
    }
}
```

---

## 2. AccountNumberGenerator.java (Component mới)

```java
package com.abcbank.ekyc.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Random;

/**
 * Component sinh số tài khoản ngân hàng tự động.
 *
 * <p>Tuân thủ SRP: chỉ chịu trách nhiệm sinh số tài khoản,
 * tách hoàn toàn khỏi RegistrationServiceImpl.</p>
 *
 * <p>Trong production, component này sẽ gọi Core Banking API
 * thay vì sinh số ngẫu nhiên.</p>
 */
@Component
public class AccountNumberGenerator {

    // Prefix số tài khoản ABC Bank - lấy từ config, không hardcode
    @Value("${abcbank.account.prefix:1901}")
    private String accountPrefix;

    // Số chữ số phần suffix (sau prefix)
    @Value("${abcbank.account.suffix-length:10}")
    private int suffixLength;

    private final Random random = new Random();

    /**
     * Sinh số tài khoản ngân hàng
     *
     * @return Chuỗi số tài khoản (prefix + suffix ngẫu nhiên)
     */
    public String generate() {
        StringBuilder sb = new StringBuilder(accountPrefix);
        for (int i = 0; i < suffixLength; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }
}
```

---

## 3. ValidationException.java (Exception tùy chỉnh)

```java
package com.abcbank.ekyc.exception;

import java.util.Map;

/**
 * Exception cho lỗi validation nghiệp vụ với thông tin trường cụ thể.
 *
 * <p>Khác với BusinessException (lỗi quy tắc nghiệp vụ),
 * ValidationException dành cho lỗi trùng lặp dữ liệu đầu vào.</p>
 */
public class ValidationException extends RuntimeException {

    /** Map chứa tên trường -> thông báo lỗi */
    private final Map<String, String> fieldErrors;

    public ValidationException(String field, String message) {
        super(message);
        this.fieldErrors = Map.of(field, message);
    }

    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }
}
```

---

## 4. RegistrationValidator.java (Component mới - tách từ Service)

```java
package com.abcbank.ekyc.service;

import com.abcbank.ekyc.dto.RegistrationRequest;
import com.abcbank.ekyc.exception.ValidationException;
import com.abcbank.ekyc.repository.CustomerRegistrationRepository;
import com.abcbank.ekyc.util.DataMaskingUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Component chịu trách nhiệm validate nghiệp vụ cho yêu cầu đăng ký.
 *
 * <p>Tuân thủ SRP: chỉ làm nhiệm vụ validation, không xử lý
 * nghiệp vụ chính. Được inject vào RegistrationServiceImpl.</p>
 *
 * <p>Kiểm tra:
 * <ul>
 *   <li>Trùng lặp số CCCD (BR-01)</li>
 *   <li>Trùng lặp số điện thoại (BR-02)</li>
 * </ul>
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RegistrationValidator {

    private final CustomerRegistrationRepository registrationRepository;

    /**
     * Thực hiện toàn bộ validation nghiệp vụ cho yêu cầu đăng ký
     *
     * @param request DTO chứa thông tin đăng ký cần validate
     * @throws ValidationException nếu CCCD hoặc SĐT đã tồn tại
     */
    public void validate(RegistrationRequest request) {
        validateCitizenId(request.getCitizenId());
        validatePhone(request.getPhone());
    }

    /**
     * Kiểm tra số CCCD chưa được đăng ký (BR-01)
     */
    private void validateCitizenId(String citizenId) {
        if (registrationRepository.existsByCitizenId(citizenId)) {
            log.warn("[Validator] CCCD {} da ton tai trong he thong",
                     DataMaskingUtil.maskCitizenId(citizenId));
            throw new ValidationException(
                "citizenId",
                "So CCCD nay da duoc su dung de dang ky tai khoan. "
                + "Vui long lien he CSKH neu co sai sot."
            );
        }
    }

    /**
     * Kiểm tra số điện thoại chưa được đăng ký (BR-02)
     */
    private void validatePhone(String phone) {
        if (registrationRepository.existsByPhone(phone)) {
            log.warn("[Validator] So dien thoai {} da ton tai trong he thong",
                     DataMaskingUtil.maskPhone(phone));
            throw new ValidationException(
                "phone",
                "So dien thoai nay da lien ket voi mot tai khoan khac. "
                + "Vui long su dung so dien thoai khac."
            );
        }
    }
}
```

---

## 5. RegistrationServiceImpl.java (Đã refactor - gọn hơn nhiều)

```java
package com.abcbank.ekyc.service.impl;

import com.abcbank.ekyc.dto.RegistrationRequest;
import com.abcbank.ekyc.dto.RegistrationResponse;
import com.abcbank.ekyc.entity.CustomerRegistration;
import com.abcbank.ekyc.entity.RegistrationStatus;
import com.abcbank.ekyc.repository.CustomerRegistrationRepository;
import com.abcbank.ekyc.service.AccountNumberGenerator;
import com.abcbank.ekyc.service.RegistrationService;
import com.abcbank.ekyc.service.RegistrationValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service điều phối luồng đăng ký tài khoản eKYC.
 *
 * <p>Phiên bản v2 - Đã Refactor:
 * <ul>
 *   <li>Validation được delegate cho {@link RegistrationValidator}</li>
 *   <li>Sinh số tài khoản được delegate cho {@link AccountNumberGenerator}</li>
 *   <li>Che giấu dữ liệu log được delegate cho DataMaskingUtil</li>
 *   <li>Service chỉ còn orchestration logic (tuân thủ SRP)</li>
 * </ul>
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class RegistrationServiceImpl implements RegistrationService {

    // Các dependency được inject qua constructor (nhờ @RequiredArgsConstructor)
    private final CustomerRegistrationRepository registrationRepository;
    private final RegistrationValidator validator;          // [REFACTOR] Tách ra
    private final AccountNumberGenerator accountNumberGenerator; // [REFACTOR] Tách ra

    @Override
    public RegistrationResponse register(RegistrationRequest request) {

        log.info("[Service] Bat dau xu ly dang ky eKYC");

        // BUOC 1: Delegate validation cho Validator (SRP)
        // ValidationException hoặc BusinessException sẽ được GlobalExceptionHandler bắt
        validator.validate(request);

        // BUOC 2: Tao entity và luu vao CSDL (trang thai PENDING)
        CustomerRegistration registration = buildRegistration(request);
        registration = registrationRepository.save(registration);
        log.info("[Service] Da luu ho so, registrationId: {}", registration.getId());

        // BUOC 3: Sinh so tai khoan (delegate cho AccountNumberGenerator)
        String accountNumber = accountNumberGenerator.generate();

        // BUOC 4: Cap nhat so tai khoan va trang thai ACTIVE
        registration.setAccountNumber(accountNumber);
        registration.setStatus(RegistrationStatus.ACTIVE);
        registration = registrationRepository.save(registration);

        log.info("[Service] Dang ky thanh cong, accountId: {}", registration.getId());

        // BUOC 5: Tra ve response
        return buildResponse(registration);
    }

    /** Tao entity CustomerRegistration tu request (Builder pattern) */
    private CustomerRegistration buildRegistration(RegistrationRequest request) {
        return CustomerRegistration.builder()
                .fullName(request.getFullName())
                .phone(request.getPhone())
                .email(request.getEmail())
                .citizenId(request.getCitizenId())
                .status(RegistrationStatus.PENDING)
                .build();
    }

    /** Tao RegistrationResponse tu entity (tra ve cho Controller) */
    private RegistrationResponse buildResponse(CustomerRegistration registration) {
        return RegistrationResponse.builder()
                .accountId(registration.getId())
                .accountNumber(registration.getAccountNumber())
                .status(registration.getStatus())
                .message("Dang ky tai khoan thanh cong! "
                       + "So tai khoan cua ban: " + registration.getAccountNumber()
                       + ". Thong tin se duoc gui qua SMS/Email.")
                .build();
    }
}
```

---

## 6. ErrorResponse.java (Nâng cấp - thêm traceId và details)

```java
package com.abcbank.ekyc.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * DTO chuẩn cho mọi response lỗi trong hệ thống eKYC.
 *
 * <p>Phiên bản v2: thêm traceId để hỗ trợ distributed tracing,
 * thêm details Map cho validation errors từng trường.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL) // Không serialize field null
public class ErrorResponse {

    /** Thoi diem xay ra loi (ISO 8601) */
    private LocalDateTime timestamp;

    /** HTTP status code */
    private int status;

    /** Ten loi ngan gon (SNAKE_CASE) */
    private String error;

    /** Thong bao loi chi tiet cho nguoi dung */
    private String message;

    /**
     * Chi tiet loi theo tung truong (danh cho validation errors)
     * Null neu khong co loi tung truong
     */
    private Map<String, String> details;

    /** Duong dan API noi xay ra loi */
    private String path;

    /**
     * ID duy nhat de trace log trong he thong phan tan
     * Sinh ngau nhien moi request, giup debug nhanh
     */
    @Builder.Default
    private String traceId = UUID.randomUUID().toString();
}
```

---

## 7. GlobalExceptionHandler.java (Nâng cấp)

```java
package com.abcbank.ekyc.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Global Exception Handler - Xu ly tap trung moi loi (phien ban v2)
 *
 * <p>Cai tien so voi v1:
 * <ul>
 *   <li>Xu ly ValidationException (tu RegistrationValidator)</li>
 *   <li>Xu ly HttpMessageNotReadableException (JSON parse error)</li>
 *   <li>Xu ly HttpRequestMethodNotSupportedException (sai HTTP method)</li>
 *   <li>Tat ca response dung ErrorResponse voi traceId</li>
 *   <li>Log day du cac loai loi</li>
 * </ul>
 * </p>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** Xu ly loi validation dau vao (@Valid that bai) */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex, WebRequest request) {

        // Gom tat ca loi theo tung truong vao Map<fieldName, message>
        Map<String, String> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        FieldError::getDefaultMessage,
                        (existing, replacement) -> existing // giu loi dau tien neu trung
                ));

        log.warn("[ExceptionHandler] Validation that bai: {}", fieldErrors);

        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("VALIDATION_FAILED")
                .message("Du lieu dau vao khong hop le. Vui long kiem tra lai cac truong.")
                .details(fieldErrors)
                .path(extractPath(request))
                .build();

        return ResponseEntity.badRequest().body(error);
    }

    /** Xu ly ValidationException (tu RegistrationValidator - trung lap du lieu) */
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleDomainValidationException(
            ValidationException ex, WebRequest request) {

        log.warn("[ExceptionHandler] Loi validation nghiep vu: {}", ex.getFieldErrors());

        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.CONFLICT.value())
                .error("DUPLICATE_DATA")
                .message(ex.getMessage())
                .details(ex.getFieldErrors())
                .path(extractPath(request))
                .build();

        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    /** Xu ly loi nghiep vu chung (BusinessException) */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(
            BusinessException ex, WebRequest request) {

        log.error("[ExceptionHandler] Loi nghiep vu: {}", ex.getMessage());

        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(ex.getStatusCode())
                .error("BUSINESS_RULE_VIOLATION")
                .message(ex.getMessage())
                .path(extractPath(request))
                .build();

        return ResponseEntity.status(ex.getStatusCode()).body(error);
    }

    /** Xu ly JSON parse error (request body khong dung dinh dang JSON) */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleJsonParseError(
            HttpMessageNotReadableException ex, WebRequest request) {

        log.warn("[ExceptionHandler] JSON parse error: {}", ex.getMessage());

        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("INVALID_JSON")
                .message("Request body khong dung dinh dang JSON. Vui long kiem tra lai.")
                .path(extractPath(request))
                .build();

        return ResponseEntity.badRequest().body(error);
    }

    /** Xu ly sai HTTP method (vi du GET thay vi POST) */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotAllowed(
            HttpRequestMethodNotSupportedException ex, WebRequest request) {

        log.warn("[ExceptionHandler] Sai HTTP method: {}", ex.getMethod());

        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.METHOD_NOT_ALLOWED.value())
                .error("METHOD_NOT_ALLOWED")
                .message("HTTP method " + ex.getMethod() + " khong duoc ho tro cho endpoint nay.")
                .path(extractPath(request))
                .build();

        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(error);
    }

    /** Xu ly moi loi khong luong truoc (loi he thong) */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex, WebRequest request) {

        log.error("[ExceptionHandler] Loi he thong: {}", ex.getMessage(), ex);

        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("INTERNAL_SERVER_ERROR")
                .message("He thong dang gap su co. Vui long thu lai sau.")
                .path(extractPath(request))
                .build();

        return ResponseEntity.internalServerError().body(error);
    }

    private String extractPath(WebRequest request) {
        return request.getDescription(false).replace("uri=", "");
    }
}
```

---

## 8. RequestLoggingFilter.java (Component mới - Structured Logging)

```java
package com.abcbank.ekyc.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

/**
 * HTTP Filter tu dong log moi request/response.
 *
 * <p>Chức năng:
 * <ul>
 *   <li>Sinh traceId duy nhat moi request va luu vao MDC</li>
 *   <li>Log thong tin request: method, path, IP, User-Agent</li>
 *   <li>Log thong tin response: status code, thoi gian xu ly</li>
 *   <li>Dam bao MDC duoc xoa sau khi xu ly xong (tranh memory leak)</li>
 * </ul>
 * </p>
 *
 * <p>TraceId se xuat hien trong moi dong log, giup trace loi
 * trong he thong phan tan.</p>
 */
@Slf4j
@Component
@Order(1) // Uu tien cao nhat - chay truoc moi Filter khac
public class RequestLoggingFilter implements Filter {

    private static final String TRACE_ID_KEY = "traceId";

    @Override
    public void doFilter(ServletRequest servletRequest,
                         ServletResponse servletResponse,
                         FilterChain chain) throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        // Sinh traceId moi va dua vao MDC (se xuat hien trong moi dong log)
        String traceId = UUID.randomUUID().toString().substring(0, 8);
        MDC.put(TRACE_ID_KEY, traceId);

        // Truyen traceId qua response header de client co the trace
        response.setHeader("X-Trace-Id", traceId);

        long startTime = System.currentTimeMillis();

        // Log thong tin request
        log.info("[REQUEST] {} {} | IP: {} | UA: {}",
                request.getMethod(),
                request.getRequestURI(),
                getClientIp(request),
                truncate(request.getHeader("User-Agent"), 50));

        try {
            // Cho phep request tiep tuc xu ly
            chain.doFilter(servletRequest, servletResponse);
        } finally {
            // Log thong tin response (luon chay du co exception hay khong)
            long duration = System.currentTimeMillis() - startTime;
            log.info("[RESPONSE] {} {} | Status: {} | Time: {}ms",
                    request.getMethod(),
                    request.getRequestURI(),
                    response.getStatus(),
                    duration);

            // Xoa MDC sau khi xu ly xong - rat quan trong de tranh memory leak
            MDC.remove(TRACE_ID_KEY);
        }
    }

    /** Lay IP thuc cua client (co xu ly qua proxy/load balancer) */
    private String getClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isEmpty()) {
            return forwardedFor.split(",")[0].trim(); // Lay IP dau tien trong chain
        }
        return request.getRemoteAddr();
    }

    /** Cat ngan chuoi de tranh log qua dai */
    private String truncate(String value, int maxLength) {
        if (value == null) return "N/A";
        return value.length() > maxLength ? value.substring(0, maxLength) + "..." : value;
    }
}
```
