package com.bookhub.voucher; // (Hoặc package controller của bạn)

import com.bookhub.voucher.VoucherService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class UserVoucherController {

    private final VoucherService voucherService;

    /**
     * Hiển thị trang voucher (voucher.html) cho người dùng đã đăng nhập.
     * Sửa thành "isAuthenticated()" để khắc phục lỗi 403.
     */
    @PreAuthorize("isAuthenticated()") // <-- ĐÃ SỬA
    @GetMapping("/user/voucher")
    public String userVoucherPage(Model model) {

        // (TÙY CHỌN NÂNG CẤP):
        // model.addAttribute("vouchers", voucherService.getAvailablePublicVouchers());

        // Trả về tệp: /resources/templates/user/voucher.html
        return "user/voucher";
    }
}