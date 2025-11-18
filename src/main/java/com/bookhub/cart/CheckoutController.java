package com.bookhub.cart;

import com.bookhub.order.Order;
import com.bookhub.order.OrderService;
import com.bookhub.user.User;
import com.bookhub.user.UserService;
import com.bookhub.voucher.VoucherDTO;
import com.bookhub.voucher.VoucherService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

// Gửi file code hoàn chỉnh: CheckoutController.java
@Controller
@RequiredArgsConstructor
public class CheckoutController {

    private final VoucherService voucherService;
    private final UserService userService;
    private final OrderService orderService;

    /**
     * Hiển thị trang Thanh toán.
     */
    @GetMapping("/checkout")
    public String checkoutPage(Model model, Principal principal) {

        List<VoucherDTO> availableVouchers = voucherService.getAvailablePublicVoucherDTOs();
        model.addAttribute("availableVouchers", availableVouchers);

        if (principal != null) {
            String email = principal.getName();
            Optional<User> userOpt = userService.findUserByEmail(email);

            if (userOpt.isPresent()) {
                User user = userOpt.get();
                model.addAttribute("loggedInUser", user);
                model.addAttribute("isLoggedIn", true);
            }
        }
        return "user/checkout";
    }

    // ==========================================================
    // === API KIỂM TRA VOUCHER BẰNG AJAX (FE) ===
    // ==========================================================
    @PostMapping("/api/vouchers/check")
    @ResponseBody
    public ResponseEntity<?> checkVoucherApi(@RequestBody Map<String, String> payload) {
        String voucherCode = payload.get("code");
        String totalStr = payload.get("cartTotal");

        try {
            BigDecimal cartTotal = new BigDecimal(totalStr);

            // Gọi Service tính toán (sẽ ném RuntimeException nếu voucher không hợp lệ)
            BigDecimal discountAmount = voucherService.calculateDiscount(voucherCode, cartTotal);

            // Trả về JSON thành công
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "discountAmount", discountAmount.longValue(), // Trả về Long (số nguyên VNĐ)
                    "message", "Áp dụng mã giảm giá thành công!"
            ));

        } catch (RuntimeException e) {
            // Trả về JSON lỗi nếu calculateDiscount ném ngoại lệ
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "discountAmount", 0,
                    "message", e.getMessage() // Thông báo lỗi từ Service
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "discountAmount", 0,
                    "message", "Có lỗi hệ thống: " + e.getMessage()
            ));
        }
    }
    // ==========================================================


    /**
     * Xử lý đơn hàng khi người dùng nhấn "Xác nhận Đặt hàng".
     */
    @PostMapping("/order/submit")
    public String submitOrder(
            @RequestParam("customerName") String customerName,
            @RequestParam("customerPhone") String customerPhone,
            @RequestParam("customerAddress") String customerAddress,
            @RequestParam("cartItemsJson") String cartItemsJson,
            @RequestParam(name = "voucherCode", required = false) String voucherCode,
            Principal principal,
            RedirectAttributes redirectAttributes
    ) {
        try {
            User user = null;
            if (principal != null) {
                user = userService.findUserByEmail(principal.getName()).orElse(null);
            }

            // Gọi Order Service để xử lý. OrderService phải re-validate voucher và giảm số lượng.
            Order newOrder = orderService.processOrder(
                    customerName,
                    customerPhone,
                    customerAddress,
                    cartItemsJson,
                    voucherCode,
                    user
            );

            redirectAttributes.addFlashAttribute("successMessage", "Đặt hàng thành công! Mã đơn hàng của bạn là: #DH" + newOrder.getId_order());
            return "redirect:/order/success/" + newOrder.getId_order();

        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("errorMessage", "Đặt hàng thất bại: " + e.getMessage());
            return "redirect:/checkout";
        }
    }
}