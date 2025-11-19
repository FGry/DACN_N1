package com.bookhub.voucher; // Đặt vào package voucher của bạn

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;

@RestController
@RequestMapping("/api/vouchers")
@RequiredArgsConstructor
public class VoucherApiController {

    private final VoucherService voucherService;

    // API để xác thực và tính toán giảm giá
    // Endpoint: /api/vouchers/apply?code=xxx&total=yyy
    @GetMapping("/apply")
    public ResponseEntity<?> applyVoucher(
            @RequestParam("code") String code,
            @RequestParam("total") Long total) {

        try {
            // Chuyển tổng tiền từ Long sang BigDecimal để tính toán chính xác
            BigDecimal cartTotal = new BigDecimal(total);

            // Gọi Service để tính toán giảm giá (logic đã có trong VoucherService)
            BigDecimal discountAmount = voucherService.calculateDiscount(code, cartTotal);

            // Thành công: Trả về số tiền giảm giá
            return ResponseEntity.ok(new VoucherDiscountResponse(
                    code,
                    discountAmount.longValue() // Trả về số tiền giảm giá (Long)
            ));

        } catch (RuntimeException e) {
            // Xử lý lỗi từ Service (ví dụ: hết hạn, không đủ min order)
            // Trả về HTTP 400 Bad Request kèm thông báo lỗi
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            // Xử lý các lỗi khác (500 Internal Server Error)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorResponse("Lỗi hệ thống: " + e.getMessage()));
        }
    }

    // DTO dùng để trả lời thành công
    private static record VoucherDiscountResponse(String code, Long discountAmount) {}

    // DTO dùng để trả lời lỗi (Phải có trường message)
    private static record ErrorResponse(String message) {}
}