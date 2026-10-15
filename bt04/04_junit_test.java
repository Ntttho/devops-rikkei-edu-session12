// ============================================================
// FILE: RegistrationServiceTest.java
// Package: com.abcbank.ekyc.service
// Mô tả: Unit test cho RegistrationServiceImpl sử dụng JUnit 5 + Mockito
// ============================================================

package com.abcbank.ekyc.service;

import com.abcbank.ekyc.dto.RegistrationRequest;
import com.abcbank.ekyc.dto.RegistrationResponse;
import com.abcbank.ekyc.entity.CustomerRegistration;
import com.abcbank.ekyc.entity.RegistrationStatus;
import com.abcbank.ekyc.exception.BusinessException;
import com.abcbank.ekyc.repository.CustomerRegistrationRepository;
import com.abcbank.ekyc.service.impl.RegistrationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit test cho RegistrationServiceImpl
 *
 * <p>Sử dụng @ExtendWith(MockitoExtension.class) để tích hợp Mockito,
 * mock toàn bộ dependency (repository) và chỉ test logic của Service.</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Test: RegistrationServiceImpl")
class RegistrationServiceTest {

    // Mock dependency - không gọi database thật
    @Mock
    private CustomerRegistrationRepository registrationRepository;

    // Class cần test - Mockito tự inject @Mock vào đây
    @InjectMocks
    private RegistrationServiceImpl registrationService;

    // Dữ liệu mẫu dùng chung cho các test
    private RegistrationRequest validRequest;
    private CustomerRegistration savedRegistration;

    /**
     * Chuẩn bị dữ liệu trước mỗi test (@BeforeEach)
     */
    @BeforeEach
    void setUp() {
        // Tạo request hợp lệ mẫu
        validRequest = RegistrationRequest.builder()
                .fullName("Nguyễn Văn An")
                .phone("0912345678")
                .email("an.nguyen@gmail.com")
                .citizenId("012345678901")
                .build();

        // Tạo entity mô phỏng kết quả lưu từ repository
        savedRegistration = CustomerRegistration.builder()
                .id(UUID.randomUUID())
                .fullName("Nguyễn Văn An")
                .phone("0912345678")
                .email("an.nguyen@gmail.com")
                .citizenId("012345678901")
                .accountNumber("19011234567890")
                .status(RegistrationStatus.ACTIVE)
                .build();
    }

    // =========================================================================
    // TEST: Trường hợp thành công
    // =========================================================================

    @Test
    @DisplayName("TC-SVC-01: Đăng ký thành công - trả về status ACTIVE và accountId")
    void test_register_success() {
        // GIVEN: Mock repository trả về không trùng lặp và lưu thành công
        when(registrationRepository.existsByCitizenId(anyString())).thenReturn(false);
        when(registrationRepository.existsByPhone(anyString())).thenReturn(false);
        // Mock save() lần 1 (PENDING) và lần 2 (ACTIVE) đều trả về savedRegistration
        when(registrationRepository.save(any(CustomerRegistration.class)))
                .thenReturn(savedRegistration);

        // WHEN: Gọi phương thức cần test
        RegistrationResponse response = registrationService.register(validRequest);

        // THEN: Verify kết quả trả về
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(RegistrationStatus.ACTIVE);
        assertThat(response.getAccountId()).isNotNull();
        assertThat(response.getMessage()).contains("thanh cong");

        // Verify repository được gọi đúng số lần
        verify(registrationRepository, times(1)).existsByCitizenId("012345678901");
        verify(registrationRepository, times(1)).existsByPhone("0912345678");
        verify(registrationRepository, times(2)).save(any(CustomerRegistration.class));
    }

    @Test
    @DisplayName("TC-SVC-02: Đăng ký thành công - số tài khoản được sinh ra")
    void test_register_accountNumberGenerated() {
        // GIVEN
        when(registrationRepository.existsByCitizenId(anyString())).thenReturn(false);
        when(registrationRepository.existsByPhone(anyString())).thenReturn(false);
        when(registrationRepository.save(any())).thenReturn(savedRegistration);

        // WHEN
        RegistrationResponse response = registrationService.register(validRequest);

        // THEN: accountNumber phải tồn tại và đúng 14 chữ số
        assertThat(response.getAccountNumber()).isNotNull();
        assertThat(response.getAccountNumber()).hasSize(14);
        assertThat(response.getAccountNumber()).containsOnlyDigits();
    }

    @Test
    @DisplayName("TC-SVC-03: Dữ liệu entity được lưu đúng vào repository")
    void test_register_savesCorrectData() {
        // GIVEN
        when(registrationRepository.existsByCitizenId(anyString())).thenReturn(false);
        when(registrationRepository.existsByPhone(anyString())).thenReturn(false);
        when(registrationRepository.save(any())).thenReturn(savedRegistration);

        // WHEN
        registrationService.register(validRequest);

        // THEN: Dùng ArgumentCaptor để capture đối số truyền vào save()
        ArgumentCaptor<CustomerRegistration> captor =
                ArgumentCaptor.forClass(CustomerRegistration.class);
        verify(registrationRepository, atLeastOnce()).save(captor.capture());

        // Verify lần save đầu tiên: dữ liệu đúng với request
        CustomerRegistration capturedEntity = captor.getAllValues().get(0);
        assertThat(capturedEntity.getFullName()).isEqualTo("Nguyễn Văn An");
        assertThat(capturedEntity.getPhone()).isEqualTo("0912345678");
        assertThat(capturedEntity.getEmail()).isEqualTo("an.nguyen@gmail.com");
        assertThat(capturedEntity.getCitizenId()).isEqualTo("012345678901");
        // Lần save đầu tiên phải là PENDING
        assertThat(capturedEntity.getStatus()).isEqualTo(RegistrationStatus.PENDING);
    }

    // =========================================================================
    // TEST: Trường hợp trùng lặp (Business Rule Violation)
    // =========================================================================

    @Test
    @DisplayName("TC-SVC-04: Ném BusinessException khi CCCD đã tồn tại (BR-01)")
    void test_register_duplicate_citizenId_throwsBusinessException() {
        // GIVEN: Mock trả về true - CCCD đã tồn tại
        when(registrationRepository.existsByCitizenId("012345678901")).thenReturn(true);

        // WHEN & THEN: Kỳ vọng BusinessException được ném ra
        assertThatThrownBy(() -> registrationService.register(validRequest))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("CCCD");

        // Verify: Repository existsByCitizenId được gọi 1 lần
        verify(registrationRepository, times(1)).existsByCitizenId("012345678901");
        // Verify: save() không được gọi (bị chặn trước)
        verify(registrationRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC-SVC-05: Ném BusinessException với HTTP 409 khi CCCD trùng")
    void test_register_duplicate_citizenId_statusCode409() {
        // GIVEN
        when(registrationRepository.existsByCitizenId(anyString())).thenReturn(true);

        // WHEN & THEN: Kiểm tra status code của exception
        assertThatThrownBy(() -> registrationService.register(validRequest))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    assertThat(((BusinessException) ex).getStatusCode()).isEqualTo(409);
                });
    }

    @Test
    @DisplayName("TC-SVC-06: Ném BusinessException khi SĐT đã tồn tại (BR-02)")
    void test_register_duplicate_phone_throwsBusinessException() {
        // GIVEN: CCCD chưa tồn tại, nhưng SĐT đã tồn tại
        when(registrationRepository.existsByCitizenId(anyString())).thenReturn(false);
        when(registrationRepository.existsByPhone("0912345678")).thenReturn(true);

        // WHEN & THEN
        assertThatThrownBy(() -> registrationService.register(validRequest))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("dien thoai");

        // Verify thứ tự kiểm tra: CCCD trước, SĐT sau
        verify(registrationRepository, times(1)).existsByCitizenId(anyString());
        verify(registrationRepository, times(1)).existsByPhone("0912345678");
        verify(registrationRepository, never()).save(any());
    }

    // =========================================================================
    // TEST: Edge Cases
    // =========================================================================

    @Test
    @DisplayName("TC-SVC-07: Số tài khoản bắt đầu bằng 1901 (mã ABC Bank)")
    void test_register_accountNumber_startsWithBankPrefix() {
        // GIVEN
        when(registrationRepository.existsByCitizenId(anyString())).thenReturn(false);
        when(registrationRepository.existsByPhone(anyString())).thenReturn(false);
        when(registrationRepository.save(any())).thenReturn(savedRegistration);

        // WHEN
        RegistrationResponse response = registrationService.register(validRequest);

        // THEN: Số tài khoản phải bắt đầu bằng 1901
        assertThat(response.getAccountNumber()).startsWith("1901");
    }
}


// ============================================================
// FILE: RegistrationControllerTest.java
// Package: com.abcbank.ekyc.controller
// Mô tả: Integration test cho RegistrationController sử dụng @WebMvcTest
// ============================================================

package com.abcbank.ekyc.controller;

import com.abcbank.ekyc.dto.RegistrationRequest;
import com.abcbank.ekyc.dto.RegistrationResponse;
import com.abcbank.ekyc.entity.RegistrationStatus;
import com.abcbank.ekyc.exception.BusinessException;
import com.abcbank.ekyc.service.RegistrationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Test Controller sử dụng @WebMvcTest - chỉ load Spring MVC context,
 * không load toàn bộ ApplicationContext (nhanh hơn @SpringBootTest).
 */
@WebMvcTest(RegistrationController.class)
@DisplayName("Integration Test: RegistrationController")
class RegistrationControllerTest {

    @Autowired
    private MockMvc mockMvc;  // Công cụ gửi HTTP request giả trong test

    @MockBean
    private RegistrationService registrationService;  // Mock Service, không gọi thật

    @Autowired
    private ObjectMapper objectMapper;  // Chuyển đổi object <-> JSON

    private RegistrationRequest validRequest;
    private RegistrationResponse successResponse;

    private static final String REGISTER_URL = "/api/v1/registrations/register";

    @BeforeEach
    void setUp() {
        // Dữ liệu request hợp lệ
        validRequest = RegistrationRequest.builder()
                .fullName("Nguyễn Văn An")
                .phone("0912345678")
                .email("an.nguyen@gmail.com")
                .citizenId("012345678901")
                .build();

        // Response mô phỏng khi thành công
        successResponse = RegistrationResponse.builder()
                .accountId(UUID.randomUUID())
                .accountNumber("19011234567890")
                .status(RegistrationStatus.ACTIVE)
                .message("Dang ky tai khoan thanh cong!")
                .build();
    }

    // =========================================================================
    // TEST: Trường hợp thành công
    // =========================================================================

    @Test
    @DisplayName("TC-CTRL-01: POST request hợp lệ → HTTP 201 Created")
    void test_register_validRequest_returns201() throws Exception {
        // GIVEN: Mock Service trả về response thành công
        when(registrationService.register(any(RegistrationRequest.class)))
                .thenReturn(successResponse);

        // WHEN & THEN: Gửi POST request và kiểm tra response
        mockMvc.perform(post(REGISTER_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andDo(print())  // In request/response ra console để debug
                .andExpect(status().isCreated())  // HTTP 201
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.accountId").isNotEmpty())
                .andExpect(jsonPath("$.accountNumber").value("19011234567890"));

        // Verify Service được gọi 1 lần
        verify(registrationService, times(1)).register(any());
    }

    // =========================================================================
    // TEST: Validation thất bại → HTTP 400
    // =========================================================================

    @Test
    @DisplayName("TC-CTRL-02: fullName rỗng → HTTP 400 Bad Request")
    void test_register_blankFullName_returns400() throws Exception {
        // GIVEN: Request với fullName rỗng
        validRequest.setFullName("");

        // WHEN & THEN
        mockMvc.perform(post(REGISTER_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));

        // Service không được gọi khi validation thất bại
        verify(registrationService, never()).register(any());
    }

    @Test
    @DisplayName("TC-CTRL-03: Email sai định dạng → HTTP 400 Bad Request")
    void test_register_invalidEmail_returns400() throws Exception {
        // GIVEN: Email không có @
        validRequest.setEmail("invalidemail.com");

        mockMvc.perform(post(REGISTER_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("email")));
    }

    @Test
    @DisplayName("TC-CTRL-04: SĐT sai định dạng (đầu số 01) → HTTP 400")
    void test_register_invalidPhone_returns400() throws Exception {
        // GIVEN: SĐT bắt đầu bằng 01 (đã không còn sử dụng)
        validRequest.setPhone("0123456789");

        mockMvc.perform(post(REGISTER_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("TC-CTRL-05: CCCD không đủ 12 số → HTTP 400 Bad Request")
    void test_register_invalidCitizenId_returns400() throws Exception {
        // GIVEN: CCCD chỉ 11 chữ số
        validRequest.setCitizenId("01234567890");

        mockMvc.perform(post(REGISTER_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value(org.hamcrest.Matchers.containsString("CCCD")));
    }

    @Test
    @DisplayName("TC-CTRL-06: Request body trống hoàn toàn → HTTP 400")
    void test_register_emptyBody_returns400() throws Exception {
        mockMvc.perform(post(REGISTER_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest());
    }

    // =========================================================================
    // TEST: Business Rule Violation → HTTP 409
    // =========================================================================

    @Test
    @DisplayName("TC-CTRL-07: CCCD đã tồn tại → HTTP 409 Conflict")
    void test_register_duplicateCitizenId_returns409() throws Exception {
        // GIVEN: Service ném BusinessException với status 409
        when(registrationService.register(any(RegistrationRequest.class)))
                .thenThrow(new BusinessException("So CCCD da duoc su dung", 409));

        // WHEN & THEN
        mockMvc.perform(post(REGISTER_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isConflict())  // HTTP 409
                .andExpect(jsonPath("$.error").value("BUSINESS_RULE_VIOLATION"))
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @DisplayName("TC-CTRL-08: SĐT đã tồn tại → HTTP 409 Conflict")
    void test_register_duplicatePhone_returns409() throws Exception {
        // GIVEN
        when(registrationService.register(any()))
                .thenThrow(new BusinessException("So dien thoai da lien ket", 409));

        mockMvc.perform(post(REGISTER_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isConflict());
    }
}
