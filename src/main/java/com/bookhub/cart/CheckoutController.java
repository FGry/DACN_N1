package com.bookhub.cart; // Giữ package hiện tại của bạn

import com.bookhub.order.Order;
import com.bookhub.order.OrderService;
import com.bookhub.payment.PayOSService; // IMPORT SERVICE PAYOS
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

@Controller
@RequiredArgsConstructor
public class CheckoutController {

    private final VoucherService voucherService;
    private final UserService userService;
    private final OrderService orderService;
    private final PayOSService payOSService; // 1. INJECT PAYOS SERVICE

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
            BigDecimal discountAmount = voucherService.calculateDiscount(voucherCode, cartTotal);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "discountAmount", discountAmount.longValue(),
                    "message", "Áp dụng mã giảm giá thành công!"
            ));

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "discountAmount", 0,
                    "message", e.getMessage()
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
    // === XỬ LÝ SUBMIT ĐƠN HÀNG ===
    // ==========================================================
    @PostMapping("/order/submit")
    public String submitOrder(
            @RequestParam("customerName") String customerName,
            @RequestParam("customerPhone") String customerPhone,
            @RequestParam("customerAddress") String customerAddress,
            @RequestParam("cartItemsJson") String cartItemsJson,
            @RequestParam(name = "voucherCode", required = false) String voucherCode,
            @RequestParam(name = "paymentMethod", defaultValue = "COD") String paymentMethod, // 2. NHẬN PAYMENT METHOD
            Principal principal,
            RedirectAttributes redirectAttributes
    ) {
        try {
            User user = null;
            if (principal != null) {
                user = userService.findUserByEmail(principal.getName()).orElse(null);
            }

            // 3. GỌI ORDER SERVICE (Hàm processOrder đã được sửa để nhận paymentMethod)
            Order newOrder = orderService.processOrder(
                    customerName,
                    customerPhone,
                    customerAddress,
                    cartItemsJson,
                    voucherCode,
                    user,
                    paymentMethod // Truyền vào đây
            );

            // 4. PHÂN NHÁNH XỬ LÝ
            if ("PAYOS".equals(paymentMethod)) {
                // Nếu chọn PayOS -> Gọi Service tạo link và Redirect sang PayOS
                String checkoutUrl = payOSService.createPaymentLink(newOrder);
                return "redirect:" + checkoutUrl;
            } else {
                // Nếu chọn COD -> Redirect sang trang thành công nội bộ
                redirectAttributes.addFlashAttribute("successMessage", "Đặt hàng thành công! Mã đơn hàng của bạn là: #DH" + newOrder.getId_order());
                return "redirect:/order/success/" + newOrder.getId_order();
            }

        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("errorMessage", "Đặt hàng thất bại: " + e.getMessage());
            return "redirect:/checkout";
        }
    }
}