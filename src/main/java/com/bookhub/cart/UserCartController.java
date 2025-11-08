package com.bookhub.cart; // (Hoặc package controller của bạn)

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
// import org.springframework.security.access.prepost.PreAuthorize; // <-- ĐÃ XÓA

@Controller
public class UserCartController {

    /**
     * Hiển thị trang giỏ hàng (cart.html) cho BẤT KỲ AI.
     * Đã XÓA @PreAuthorize để cho phép khách (guest) thanh toán.
     */
    // @PreAuthorize("hasRole('USER')") // <-- ĐÃ XÓA
    @GetMapping("/user/cart")
    public String userCartPage() {
        // Trả về tệp: /resources/templates/user/cart.html
        // Tệp này sẽ tự tải giỏ hàng từ localStorage/sessionStorage
        return "user/cart";
    }
}