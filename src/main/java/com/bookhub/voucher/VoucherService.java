package com.bookhub.voucher;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class VoucherService {

    private final VoucherRepository voucherRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public List<Voucher> getAvailablePublicVouchers() {
        return voucherRepository.findAvailablePublicVouchers(LocalDate.now());
    }

    public String getVouchersAsJson(List<Voucher> vouchers) {
        // ... (Giữ nguyên logic JSON)
        return "[]"; // Placeholder
    }

    private VoucherDTO mapToDTO(Voucher voucher) {
        // ... (Giữ nguyên logic DTO)
        VoucherDTO dto = new VoucherDTO();
        dto.setId(voucher.getId_vouchers());
        dto.setCode(voucher.getCodeName());
        dto.setDiscountType(voucher.getDiscountType());
        dto.setDiscountValue(voucher.getDiscountValueStr());
        dto.setMinOrder(voucher.getMin_order_value() != null ? String.valueOf(voucher.getMin_order_value()) : "0");
        dto.setEndDate(voucher.getEnd_date() != null ? voucher.getEnd_date().toString() : null);
        return dto;
    }

    // --- ADMIN METHODS ---
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