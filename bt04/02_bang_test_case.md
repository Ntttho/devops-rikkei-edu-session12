# BẢNG DANH SÁCH TEST CASE - API ĐĂNG KÝ TÀI KHOẢN eKYC
## ABC Bank - Bài tập 04

**Module:** Đăng ký mở tài khoản cơ bản  
**API Endpoint:** `POST /api/v1/registrations/register`  
**Người thiết kế:** QA Team  
**Ngày:** 16/07/2026  

---

## 1. POSITIVE TEST CASE (Trường hợp hợp lệ)

| ID | Mô tả | fullName | phone | email | citizenId | Kết quả mong đợi | HTTP |
|----|-------|----------|-------|-------|-----------|-----------------|------|
| TC-POS-01 | Đăng ký thành công với dữ liệu chuẩn | Nguyễn Văn An | 0912345678 | an.nguyen@gmail.com | 012345678901 | `{"status":"ACTIVE", accountNumber có 14 chữ số}` | 201 |
| TC-POS-02 | Họ tên có dấu đầy đủ | Trần Thị Bích Ngọc | 0987654321 | ngoc.tran@yahoo.com | 079345678901 | status = ACTIVE | 201 |
| TC-POS-03 | SĐT đầu số 03x (Viettel) | Lê Minh Tuấn | 0345678901 | tuan.le@outlook.com | 048234567890 | status = ACTIVE | 201 |
| TC-POS-04 | SĐT đầu số 07x (Mobifone) | Phạm Thị Hoa | 0765432109 | hoa.pham@edu.vn | 001987654321 | status = ACTIVE | 201 |
| TC-POS-05 | Họ tên đúng 100 ký tự (giá trị biên trên hợp lệ) | [chuỗi 100 ký tự] | 0856789012 | test100@gmail.com | 034123456789 | status = ACTIVE | 201 |

---

## 2. NEGATIVE TEST CASE (Trường hợp lỗi)

### 2.1. Lỗi trường fullName

| ID | Mô tả | fullName | phone | email | citizenId | Kết quả mong đợi | HTTP |
|----|-------|----------|-------|-------|-----------|-----------------|------|
| TC-NEG-01 | fullName để trống | `` (empty) | 0912345678 | test@gmail.com | 012345678901 | error: "Ho ten khong duoc de trong" | 400 |
| TC-NEG-02 | fullName chỉ có khoảng trắng | `   ` | 0912345678 | test@gmail.com | 012345678901 | error: "Ho ten khong duoc de trong" | 400 |
| TC-NEG-03 | fullName quá 100 ký tự | [101 ký tự] | 0912345678 | test@gmail.com | 012345678901 | error: "Ho ten toi da 100 ky tu" | 400 |

### 2.2. Lỗi trường phone

| ID | Mô tả | fullName | phone | email | citizenId | Kết quả mong đợi | HTTP |
|----|-------|----------|-------|-------|-----------|-----------------|------|
| TC-NEG-04 | phone để trống | Nguyễn Văn A | `` | test@gmail.com | 012345678901 | error: "So dien thoai khong duoc de trong" | 400 |
| TC-NEG-05 | SĐT đầu số 01 (đã hủy) | Nguyễn Văn A | 0123456789 | test@gmail.com | 012345678901 | error: "So dien thoai khong hop le" | 400 |
| TC-NEG-06 | SĐT chỉ 9 chữ số | Nguyễn Văn A | 091234567 | test@gmail.com | 012345678901 | error: "So dien thoai khong hop le" | 400 |
| TC-NEG-07 | SĐT 11 chữ số | Nguyễn Văn A | 09123456789 | test@gmail.com | 012345678901 | error: "So dien thoai khong hop le" | 400 |
| TC-NEG-08 | SĐT chứa chữ cái | Nguyễn Văn A | 091234ABCD | test@gmail.com | 012345678901 | error: "So dien thoai khong hop le" | 400 |
| TC-NEG-09 | SĐT trùng đã tồn tại | Nguyễn Văn B | 0912345678 | b@gmail.com | 098765432109 | error: "So dien thoai da lien ket" | 409 |

### 2.3. Lỗi trường email

| ID | Mô tả | fullName | phone | email | citizenId | Kết quả mong đợi | HTTP |
|----|-------|----------|-------|-------|-----------|-----------------|------|
| TC-NEG-10 | email để trống | Nguyễn Văn A | 0912345678 | `` | 012345678901 | error: "Email khong duoc de trong" | 400 |
| TC-NEG-11 | email thiếu @ | Nguyễn Văn A | 0912345678 | testgmail.com | 012345678901 | error: "Dinh dang email khong hop le" | 400 |
| TC-NEG-12 | email thiếu domain | Nguyễn Văn A | 0912345678 | test@  | 012345678901 | error: "Dinh dang email khong hop le" | 400 |
| TC-NEG-13 | email có khoảng trắng | Nguyễn Văn A | 0912345678 | test @gmail.com | 012345678901 | error: "Dinh dang email khong hop le" | 400 |

### 2.4. Lỗi trường citizenId

| ID | Mô tả | fullName | phone | email | citizenId | Kết quả mong đợi | HTTP |
|----|-------|----------|-------|-------|-----------|-----------------|------|
| TC-NEG-14 | citizenId để trống | Nguyễn Văn A | 0912345678 | test@gmail.com | `` | error: "So CCCD khong duoc de trong" | 400 |
| TC-NEG-15 | CCCD chỉ 11 chữ số | Nguyễn Văn A | 0912345678 | test@gmail.com | 01234567890 | error: "So CCCD phai la 12 chu so" | 400 |
| TC-NEG-16 | CCCD 13 chữ số | Nguyễn Văn A | 0912345678 | test@gmail.com | 0123456789012 | error: "So CCCD phai la 12 chu so" | 400 |
| TC-NEG-17 | CCCD chứa chữ cái | Nguyễn Văn A | 0912345678 | test@gmail.com | 01234567890A | error: "So CCCD phai la 12 chu so" | 400 |
| TC-NEG-18 | CCCD trùng đã tồn tại | Nguyễn Văn B | 0987654321 | b@gmail.com | 012345678901 | error: "So CCCD da duoc su dung" | 409 |

### 2.5. Lỗi nhiều trường cùng lúc

| ID | Mô tả | Các trường lỗi | Kết quả mong đợi | HTTP |
|----|-------|----------------|-----------------|------|
| TC-NEG-19 | Request body trống hoàn toàn | Tất cả | 4 thông báo lỗi validation | 400 |
| TC-NEG-20 | Cả email và phone sai định dạng | phone, email | 2 thông báo lỗi validation | 400 |

---

## 3. BOUNDARY TEST CASE (Trường hợp biên)

| ID | Mô tả | Trường kiểm tra | Giá trị | Kết quả mong đợi | HTTP |
|----|-------|----------------|---------|-----------------|------|
| TC-BDR-01 | fullName đúng 1 ký tự (biên dưới hợp lệ) | fullName | "A" | Thành công | 201 |
| TC-BDR-02 | fullName đúng 100 ký tự (biên trên hợp lệ) | fullName | [100 ký tự "A"] | Thành công | 201 |
| TC-BDR-03 | fullName 101 ký tự (vượt biên trên) | fullName | [101 ký tự "A"] | Lỗi 400 | 400 |
| TC-BDR-04 | CCCD đúng 12 chữ số (biên hợp lệ) | citizenId | "000000000000" | Thành công | 201 |
| TC-BDR-05 | CCCD 11 chữ số (dưới biên) | citizenId | "00000000000" | Lỗi 400 | 400 |
| TC-BDR-06 | CCCD 13 chữ số (trên biên) | citizenId | "0000000000000" | Lỗi 400 | 400 |
| TC-BDR-07 | SĐT bắt đầu 090 (biên đầu số hợp lệ) | phone | "0901234567" | Thành công | 201 |
| TC-BDR-08 | SĐT bắt đầu 099 (biên đầu số hợp lệ) | phone | "0991234567" | Thành công | 201 |

---

## 4. TRACEABILITY MATRIX

| Test Case ID | FR liên quan | Business Rule | Mức độ ưu tiên |
|-------------|-------------|--------------|----------------|
| TC-POS-01 đến TC-POS-05 | FR-REG-01 | - | P1 - Phải pass |
| TC-NEG-01 đến TC-NEG-03 | FR-REG-01 | - | P1 |
| TC-NEG-04 đến TC-NEG-09 | FR-REG-01 | BR-02 | P1 |
| TC-NEG-10 đến TC-NEG-13 | FR-REG-01 | - | P1 |
| TC-NEG-14 đến TC-NEG-18 | FR-REG-01 | BR-01, BR-03 | P1 |
| TC-BDR-01 đến TC-BDR-08 | FR-REG-01 | BR-03 | P2 |

---

## 5. KẾT QUẢ KIỂM THỬ (Điền sau khi chạy)

| Đợt test | Ngày | Tổng TC | Pass | Fail | Skip | Tỷ lệ pass |
|---------|------|---------|------|------|------|-----------|
| Lần 1 | | 28 | | | | |
| Lần 2 (sau fix) | | 28 | | | | |
