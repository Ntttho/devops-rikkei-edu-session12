# MÃ NGUỒN GỐC (TRƯỚC REFACTOR) - BÀI TẬP 05
## RegistrationServiceImpl.java - Phiên bản v1 (Chưa Refactor)

> **Lưu ý:** File này lưu lại code gốc từ Bài 03 để so sánh với phiên bản sau refactor.

---

```java
package com.abcbank.ekyc.service.impl;

import com.abcbank.ekyc.dto.RegistrationRequest;
import com.abcbank.ekyc.dto.RegistrationResponse;
import com.abcbank.ekyc.entity.CustomerRegistration;
import com.abcbank.ekyc.entity.RegistrationStatus;
import com.abcbank.ekyc.exception.BusinessException;
import com.abcbank.ekyc.repository.CustomerRegistrationRepository;
import com.abcbank.ekyc.service.RegistrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Random;

// CODE SMELL #1: Class này đang làm quá nhiều việc (vi phạm SRP):
// - Validate dữ liệu trùng lặp
// - Xử lý nghiệp vụ chính
// - Sinh số tài khoản
// - Che giấu dữ liệu nhạy cảm trong log
// => Nên tách thành các lớp riêng biệt

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class RegistrationServiceImpl implements RegistrationService {

    private final CustomerRegistrationRepository registrationRepository;
    
    // CODE SMELL #2: Magic Number - "1901" hardcode trong code
    // Nên đưa vào constants hoặc @Value từ application.properties
    private final Random random = new Random();

    @Override
    public RegistrationResponse register(RegistrationRequest request) {

        log.info("[eKYC-Register] Bat dau xu ly dang ky cho CCCD: {}",
                 maskCitizenId(request.getCitizenId()));

        // CODE SMELL #3: Validation logic đặt trong Service - vi phạm SRP
        // Nên chuyển vào một RegistrationValidator riêng
        if (registrationRepository.existsByCitizenId(request.getCitizenId())) {
            log.warn("[eKYC-Register] CCCD {} da duoc su dung",
                     maskCitizenId(request.getCitizenId()));
            throw new BusinessException(
                "So CCCD nay da duoc su dung de dang ky tai khoan.",
                409
            );
        }

        if (registrationRepository.existsByPhone(request.getPhone())) {
            log.warn("[eKYC-Register] So dien thoai {} da duoc dang ky",
                     maskPhone(request.getPhone()));
            throw new BusinessException(
                "So dien thoai nay da lien ket voi mot tai khoan khac.",
                409
            );
        }

        CustomerRegistration registration = CustomerRegistration.builder()
                .fullName(request.getFullName())
                .phone(request.getPhone())
                .email(request.getEmail())
                .citizenId(request.getCitizenId())
                .status(RegistrationStatus.PENDING)
                .build();

        registration = registrationRepository.save(registration);

        // CODE SMELL #4: Magic Number "1901" và logic sinh số tài khoản
        // nên tách vào AccountNumberGenerator service riêng
        String accountNumber = generateAccountNumber();

        registration.setAccountNumber(accountNumber);
        registration.setStatus(RegistrationStatus.ACTIVE);
        registration = registrationRepository.save(registration);

        return RegistrationResponse.builder()
                .accountId(registration.getId())
                .accountNumber(registration.getAccountNumber())
                .status(registration.getStatus())
                .message("Dang ky tai khoan thanh cong! So tai khoan: " + accountNumber)
                .build();
    }

    // CODE SMELL #5: Private helper methods làm class phình to
    // maskCitizenId, maskPhone, maskAccountNumber, generateAccountNumber
    // Đây là các utility - nên tách vào class riêng (DataMaskingUtil, AccountNumberGenerator)
    
    private String generateAccountNumber() {
        StringBuilder sb = new StringBuilder("1901"); // Magic number
        for (int i = 0; i < 10; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }

    private String maskCitizenId(String citizenId) {
        if (citizenId == null || citizenId.length() < 6) return "***";
        return citizenId.substring(0, 6) + "***" + citizenId.substring(9);
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) return "***";
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }

    private String maskAccountNumber(String accountNumber) {
        if (accountNumber == null || accountNumber.length() < 8) return "***";
        return accountNumber.substring(0, 4) + "****"
             + accountNumber.substring(accountNumber.length() - 4);
    }
}
```

---

## Danh sách Code Smell đã xác định trong code gốc

| # | Code Smell | Loại | Mức độ | Vị trí |
|---|-----------|------|--------|--------|
| 1 | God Class - Service làm quá nhiều việc | SRP Violation | High | RegistrationServiceImpl |
| 2 | Magic Number "1901" hardcode | Magic Number | Medium | generateAccountNumber() |
| 3 | Validation logic trong Service | SRP Violation | High | register() method |
| 4 | Account generation trong Service | SRP Violation | Medium | generateAccountNumber() |
| 5 | Data masking utilities trong Service | SRP Violation | Low | maskXxx() methods |
| 6 | Không có Request ID/Trace ID | Missing Observability | High | Toàn bộ |
| 7 | Exception message hardcode | Magic String | Low | register() method |
