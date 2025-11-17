package com.bookhub.voucher;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class VoucherService {

    private final VoucherRepository voucherRepository;
    private final ObjectMapper objectMapper;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy"); // Format ngày

    /**
     * Lấy danh sách Voucher công khai còn hiệu lực (Entity).
     */
    @Transactional(readOnly = true)
    public List<Voucher> getAvailablePublicVouchers() {
        // Lưu ý: Trong DB hiện tại không có trường 'title', 'description', 'category', 'type', 'requirements'.
        // Cần thêm các trường này vào Entity hoặc hardcode/tính toán khi ánh xạ DTO.
        // TẠM THỜI: Chỉ lấy Entity có sẵn.
        return voucherRepository.findAvailablePublicVouchers(LocalDate.now());
    }

    /**
     * Lấy danh sách Voucher công khai còn hiệu lực (DTO) để truyền cho giao diện người dùng.
     */
    @Transactional(readOnly = true)
    public List<VoucherDTO> getAvailablePublicVoucherDTOs() {
        List<Voucher> vouchers = getAvailablePublicVouchers();
        return vouchers.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Ánh xạ từ Voucher Entity sang VoucherDTO (Dữ liệu hiển thị).
     * Cần TẠO DỮ LIỆU GIẢ/TÍNH TOÁN cho các trường thiếu (title, description, category, type, requirements, status).
     */
    private VoucherDTO mapToDTO(Voucher voucher) {
        VoucherDTO dto = new VoucherDTO();
        dto.setId(voucher.getId_vouchers());
        dto.setCode(voucher.getCodeName());
        dto.setDiscountValue(voucher.getDiscountValueStr()); // Ví dụ: "50%"
        dto.setDiscountType(voucher.getDiscountType()); // Ví dụ: "PERCENT"

        // === CÁC TRƯỜNG CẦN THÊM VÀO ENTITY HOẶC TẠO RA TỪ LOGIC ===
        // *LƯU Ý*: Các trường này (title, description, category, type, requirements)
        // không có trong Entity Voucher.java hiện tại, nên cần phải TẠO DỮ LIỆU MẪU
        // HOẶC dùng một logic để tạo ra chúng từ dữ liệu có sẵn.
        // TÔI SẼ TẠO DỮ LIỆU MẪU ĐỂ FE CHẠY ĐƯỢC.
        dto.setTitle(String.format("Ưu đãi %s: %s", voucher.getCodeName(), voucher.getDiscountValueStr()));
        dto.setDescription("Áp dụng cho đơn hàng sách giáo khoa và tham khảo. Nhanh tay săn ngay!");
        dto.setCategory(voucher.getDiscountType().equalsIgnoreCase("PERCENT") ? "Giảm giá" : "Ưu đãi tiền mặt");
        dto.setType(voucher.getDiscountType().equalsIgnoreCase("PERCENT") ? "discount" : "fixedvalue");
        dto.setRequirements(String.format("Đơn tối thiểu %s. Áp dụng toàn quốc.", String.format("%,d₫", voucher.getMin_order_value())));
        // ==========================================================

        // Thống nhất cách hiển thị đơn tối thiểu
        dto.setMinOrder(voucher.getMin_order_value() != null ? String.valueOf(voucher.getMin_order_value()) : "0");
        dto.setMinOrderDisplay(String.format("%,d₫", voucher.getMin_order_value()));

        // Ngày kết thúc
        dto.setEndDate(voucher.getEnd_date() != null ? voucher.getEnd_date().toString() : null); // YYYY-MM-DD cho JS
        dto.setEndDateDisplay(voucher.getEnd_date() != null ? voucher.getEnd_date().format(dateFormatter) : "N/A"); // Dạng dd/MM/yyyy

        // Số lượng
        dto.setQuantity(voucher.getQuantity());

        // Status
        LocalDate today = LocalDate.now();
        if (voucher.getEnd_date().isBefore(today)) {
            dto.setStatus("expired");
        } else if (voucher.getEnd_date().isBefore(today.plusDays(7))) { // Hết hạn trong 7 ngày tới
            dto.setStatus("ending");
        } else {
            dto.setStatus("active");
        }

        return dto;
    }

    // --- Phương thức getVouchersAsJson (Dùng để truyền JSON qua Controller) ---
    public String getVouchersAsJson(List<Voucher> vouchers) {
        List<VoucherDTO> dtos = vouchers.stream().map(this::mapToDTO).collect(Collectors.toList());
        try {
            return objectMapper.writeValueAsString(dtos);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Lỗi chuyển đổi danh sách voucher sang JSON", e);
        }
    }

    // --- ADMIN METHODS (Giữ nguyên) ---
    @Transactional(readOnly = true)
    public List<Voucher> findAllForAdmin() {
        return voucherRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Voucher findByIdForAdmin(Integer id) {
        return voucherRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Voucher với ID: " + id)); // TIẾNG VIỆT
    }

    // **PHƯƠNG THỨC LƯU VOUCHER ADMIN**
    public void saveAdminVoucher(Voucher voucher) {
        // 1. Check for duplicate codeName
        Optional<Voucher> existingVoucher = voucherRepository.findByCodeNameIgnoreCase(voucher.getCodeName());
        if (existingVoucher.isPresent()) {
            Voucher found = existingVoucher.get();
            if (voucher.getId_vouchers() == null || !found.getId_vouchers().equals(voucher.getId_vouchers())) {
                throw new RuntimeException("Mã Voucher '" + voucher.getCodeName() + "' đã tồn tại."); // TIẾNG VIỆT
            }
        }

        // 2. Validate dates (Phòng vệ)
        if (voucher.getStart_date().isAfter(voucher.getEnd_date())) {
            throw new RuntimeException("Ngày bắt đầu phải xảy ra trước ngày kết thúc."); // TIẾNG VIỆT
        }

        // 3. Ensure data consistency based on discountType
        if ("PERCENT".equalsIgnoreCase(voucher.getDiscountType())) {
            if (voucher.getDiscountPercent() == null || voucher.getDiscountPercent() < 0 || voucher.getDiscountPercent() > 100) {
                throw new RuntimeException("Phần trăm giảm giá phải nằm trong khoảng từ 0 đến 100."); // TIẾNG VIỆT
            }
            if (voucher.getMaxDiscount() != null && voucher.getMaxDiscount() < 0) {
                throw new RuntimeException("Giá trị giảm tối đa không được là số âm."); // TIẾNG VIỆT
            }
            voucher.setDiscountValue(null);

        } else if ("FIXED".equalsIgnoreCase(voucher.getDiscountType())) {
            if (voucher.getDiscountValue() == null || voucher.getDiscountValue() <= 0) {
                throw new RuntimeException("Giá trị giảm cố định phải lớn hơn 0."); // TIẾNG VIỆT
            }
            voucher.setDiscountPercent(null);
            voucher.setMaxDiscount(null);
        } else {
            throw new RuntimeException("Loại giảm giá không hợp lệ."); // TIẾNG VIỆT
        }

        // 4. Other basic validations (Phòng vệ)
        if (voucher.getQuantity() == null || voucher.getQuantity() < 0) { throw new RuntimeException("Số lượng voucher không được là số âm."); } // TIẾNG VIỆT
        if (voucher.getMin_order_value() != null && voucher.getMin_order_value() < 0) { throw new RuntimeException("Giá trị đơn hàng tối thiểu không được là số âm.");} // TIẾNG VIỆT

        // Ensure defaults if null
        if (voucher.getMaxDiscount() == null) voucher.setMaxDiscount(0L);
        if (voucher.getMin_order_value() == null) voucher.setMin_order_value(0L);

        // 5. Save the voucher
        voucherRepository.save(voucher);
    }

    public void deleteAdminVoucherById(Integer id) {
        if (!voucherRepository.existsById(id)) {
            throw new RuntimeException("Không tìm thấy Voucher với ID: " + id + " để xóa."); // TIẾNG VIỆT
        }
        voucherRepository.deleteById(id);
    }
}