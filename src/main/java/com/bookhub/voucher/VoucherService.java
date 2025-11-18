package com.bookhub.voucher;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

// Gửi file code hoàn chỉnh: VoucherService.java
@Service
@RequiredArgsConstructor
@Transactional
public class VoucherService {

    private final VoucherRepository voucherRepository;
    private final ObjectMapper objectMapper;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // ==========================================================
    // === CÁC PHƯƠNG THỨC PUBLIC & MAPPING DTO ===
    // ==========================================================

    @Transactional(readOnly = true)
    public List<Voucher> getAvailablePublicVouchers() {
        return voucherRepository.findAvailablePublicVouchers(LocalDate.now());
    }

    @Transactional(readOnly = true)
    public List<VoucherDTO> getAvailablePublicVoucherDTOs() {
        List<Voucher> vouchers = getAvailablePublicVouchers();
        return vouchers.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    private VoucherDTO mapToDTO(Voucher voucher) {
        VoucherDTO dto = new VoucherDTO();
        dto.setId(voucher.getId_vouchers());
        dto.setCode(voucher.getCodeName());

        String discountStr = "";
        if ("PERCENT".equalsIgnoreCase(voucher.getDiscountType())) {
            discountStr = voucher.getDiscountPercent() + "%";
        } else if ("FIXED".equalsIgnoreCase(voucher.getDiscountType())) {
            discountStr = String.format("%,d₫", voucher.getDiscountValue());
        }

        dto.setDiscountValue(discountStr);
        dto.setDiscountType(voucher.getDiscountType());

        String valueDisplay = "";
        if ("PERCENT".equalsIgnoreCase(voucher.getDiscountType())) {
            valueDisplay = "Giảm " + voucher.getDiscountPercent() + "%";
        } else if ("FIXED".equalsIgnoreCase(voucher.getDiscountType())) {
            valueDisplay = String.format("Giảm ngay %,d₫", voucher.getDiscountValue());
        }

        dto.setTitle(String.format("Ưu đãi %s: %s", voucher.getCodeName(), valueDisplay));
        dto.setDescription("Áp dụng cho đơn hàng sách giáo khoa và tham khảo. Nhanh tay săn ngay!");
        dto.setCategory(voucher.getDiscountType().equalsIgnoreCase("PERCENT") ? "Giảm giá" : "Ưu đãi tiền mặt");
        dto.setType(voucher.getDiscountType().equalsIgnoreCase("PERCENT") ? "percent" : "fixed");
        dto.setRequirements(String.format("Đơn tối thiểu %s. Áp dụng toàn quốc.", String.format("%,d₫", voucher.getMin_order_value())));

        dto.setMinOrder(voucher.getMin_order_value() != null ? String.valueOf(voucher.getMin_order_value()) : "0");
        dto.setMinOrderDisplay(String.format("%,d₫", voucher.getMin_order_value()));

        // SỬA LỖI GETTER: Sử dụng getEnd_date/getStart_date
        dto.setEndDate(voucher.getEnd_date() != null ? voucher.getEnd_date().toString() : null);
        dto.setEndDateDisplay(voucher.getEnd_date() != null ? voucher.getEnd_date().format(dateFormatter) : "N/A");

        dto.setQuantity(voucher.getQuantity());

        LocalDate today = LocalDate.now();
        if (voucher.getEnd_date().isBefore(today)) {
            dto.setStatus("expired");
        } else if (voucher.getEnd_date().isBefore(today.plusDays(7))) {
            dto.setStatus("ending");
        } else {
            dto.setStatus("active");
        }

        return dto;
    }

    public String getVouchersAsJson(List<Voucher> vouchers) {
        List<VoucherDTO> dtos = vouchers.stream().map(this::mapToDTO).collect(Collectors.toList());
        try {
            return objectMapper.writeValueAsString(dtos);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Lỗi chuyển đổi danh sách voucher sang JSON", e);
        }
    }

    // ==========================================================
    // === CORE LOGIC: TÍNH TOÁN VÀ XÁC THỰC VOUCHER ===
    // ==========================================================

    /**
     * XÁC THỰC VÀ TÍNH TOÁN GIẢM GIÁ
     */
    @Transactional(readOnly = true)
    public BigDecimal calculateDiscount(String code, BigDecimal cartTotal) {
        if (!StringUtils.hasText(code)) {
            return BigDecimal.ZERO;
        }

        Optional<Voucher> voucherOpt = voucherRepository.findByCodeNameIgnoreCase(code);

        // 1. Kiểm tra tồn tại
        if (!voucherOpt.isPresent()) {
            throw new RuntimeException("Mã voucher không tồn tại.");
        }

        Voucher voucher = voucherOpt.get();

        // 2. Kiểm tra ngày hết hạn
        if (voucher.getEnd_date().isBefore(LocalDate.now()) || voucher.getStart_date().isAfter(LocalDate.now())) {
            throw new RuntimeException("Voucher đã hết hạn sử dụng hoặc chưa đến ngày bắt đầu.");
        }

        // 3. Kiểm tra số lượng
        if (voucher.getQuantity() <= 0) {
            throw new RuntimeException("Voucher đã hết lượt sử dụng.");
        }

        // 4. Kiểm tra điều kiện Đơn hàng tối thiểu (FIX LỖI MIN ORDER)
        BigDecimal minOrderValue = voucher.getMin_order_value() != null
                ? new BigDecimal(voucher.getMin_order_value())
                : BigDecimal.ZERO;

        if (cartTotal.compareTo(minOrderValue) < 0) {
            String minOrderFormatted = String.format("%,d₫", voucher.getMin_order_value());
            throw new RuntimeException(String.format("Đơn hàng chưa đạt giá trị tối thiểu %s.", minOrderFormatted));
        }

        // 5. Tính toán giảm giá chính xác
        BigDecimal discountAmount = BigDecimal.ZERO;

        if ("PERCENT".equalsIgnoreCase(voucher.getDiscountType()) && voucher.getDiscountPercent() != null) {
            BigDecimal discountPercent = new BigDecimal(voucher.getDiscountPercent()).divide(new BigDecimal(100), 4, RoundingMode.HALF_UP);
            discountAmount = cartTotal.multiply(discountPercent);

            if (voucher.getMaxDiscount() != null && voucher.getMaxDiscount() > 0) {
                BigDecimal maxDiscount = new BigDecimal(voucher.getMaxDiscount());
                if (discountAmount.compareTo(maxDiscount) > 0) {
                    discountAmount = maxDiscount;
                }
            }

        } else if ("FIXED".equalsIgnoreCase(voucher.getDiscountType()) && voucher.getDiscountValue() != null) {
            discountAmount = new BigDecimal(voucher.getDiscountValue());
        }

        if (discountAmount.compareTo(cartTotal) > 0) {
            discountAmount = cartTotal;
        }

        return discountAmount.setScale(0, RoundingMode.HALF_UP);
    }

    /**
     * TRỪ SỐ LƯỢNG VOUCHER
     */
    public void reduceVoucherQuantity(String code) {
        if (StringUtils.hasText(code)) {
            voucherRepository.findByCodeNameIgnoreCase(code).ifPresent(voucher -> {
                if (voucher.getQuantity() > 0) {
                    voucher.setQuantity(voucher.getQuantity() - 1);
                    voucherRepository.save(voucher);
                }
            });
        }
    }

    // ==========================================================
    // === ADMIN METHODS (CRUD) - ĐÃ FIX findAllForAdmin ===
    // ==========================================================

    @Transactional(readOnly = true)
    public List<Voucher> findAllForAdmin() {
        return voucherRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Voucher findByIdForAdmin(Integer id) {
        return voucherRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Voucher với ID: " + id));
    }

    public void saveAdminVoucher(Voucher voucher) {
        // 1. Check for duplicate codeName
        Optional<Voucher> existingVoucher = voucherRepository.findByCodeNameIgnoreCase(voucher.getCodeName());
        if (existingVoucher.isPresent()) {
            Voucher found = existingVoucher.get();
            if (voucher.getId_vouchers() == null || !found.getId_vouchers().equals(voucher.getId_vouchers())) {
                throw new RuntimeException("Mã Voucher '" + voucher.getCodeName() + "' đã tồn tại.");
            }
        }

        // 2. Validate dates
        if (voucher.getStart_date().isAfter(voucher.getEnd_date())) {
            throw new RuntimeException("Ngày bắt đầu phải xảy ra trước ngày kết thúc.");
        }

        // 3. Ensure data consistency based on discountType
        if ("PERCENT".equalsIgnoreCase(voucher.getDiscountType())) {
            if (voucher.getDiscountPercent() == null || voucher.getDiscountPercent() < 0 || voucher.getDiscountPercent() > 100) {
                throw new RuntimeException("Phần trăm giảm giá phải nằm trong khoảng từ 0 đến 100.");
            }
            if (voucher.getMaxDiscount() != null && voucher.getMaxDiscount() < 0) {
                throw new RuntimeException("Giá trị giảm tối đa không được là số âm.");
            }
            voucher.setDiscountValue(null);

        } else if ("FIXED".equalsIgnoreCase(voucher.getDiscountType())) {
            if (voucher.getDiscountValue() == null || voucher.getDiscountValue() <= 0) {
                throw new RuntimeException("Giá trị giảm cố định phải lớn hơn 0.");
            }
            voucher.setDiscountPercent(null);
            voucher.setMaxDiscount(null);
        } else {
            throw new RuntimeException("Loại giảm giá không hợp lệ.");
        }

        // 4. Other basic validations (Bắt lỗi Min Order ở Admin)
        if (voucher.getQuantity() == null || voucher.getQuantity() < 0) { throw new RuntimeException("Số lượng voucher không được là số âm."); }

        if (voucher.getMin_order_value() != null && voucher.getMin_order_value() < 0) {
            throw new RuntimeException("Giá trị đơn hàng tối thiểu không được là số âm.");
        }

        // Ensure defaults if null
        if (voucher.getMaxDiscount() == null) voucher.setMaxDiscount(0L);
        if (voucher.getMin_order_value() == null) voucher.setMin_order_value(0L);

        // 5. Save the voucher
        voucherRepository.save(voucher);
    }

    public void deleteAdminVoucherById(Integer id) {
        if (!voucherRepository.existsById(id)) {
            throw new RuntimeException("Không tìm thấy Voucher với ID: " + id + " để xóa.");
        }
        voucherRepository.deleteById(id);
    }
}