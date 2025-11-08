package com.bookhub.cart; // Đã sửa package

import com.bookhub.order.Order; // THÊM
import com.bookhub.order.OrderService; // THÊM
import com.bookhub.user.User;
import com.bookhub.user.UserService;
import com.bookhub.voucher.Voucher;
import com.bookhub.voucher.VoucherService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Controller
@RequiredArgsConstructor
public class CheckoutController {

    private final VoucherService voucherService;
    private final UserService userService;

    // === THÊM ORDER SERVICE ===
    private final OrderService orderService;

    /**
     * Hiển thị trang Thanh toán.
     * (Giữ nguyên)
     */
    @GetMapping("/checkout")
    public String checkoutPage(Model model, Principal principal) {

        if (principal != null) {
            String email = principal.getName();
            Optional<User> userOpt = userService.findUserByEmail(email);

            if (userOpt.isPresent()) {
                User user = userOpt.get();
                model.addAttribute("loggedInUser", user);

                // Lấy voucher công khai
                List<Voucher> publicVouchers = voucherService.getAvailablePublicVouchers();

                // (Sau này bạn có thể thêm voucher riêng của user)

                model.addAttribute("availableVouchers", publicVouchers);
            }
        }
        return "user/checkout";
    }

    /**
     * Xử lý đơn hàng khi người dùng nhấn "Xác nhận Đặt hàng".
     * (CẬP NHẬT LOGIC)
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
            // 1. Lấy thông tin người dùng (nếu đã đăng nhập)
            User user = null;
            if (principal != null) {
                user = userService.findUserByEmail(principal.getName()).orElse(null);
            }

            // 2. === GỌI ORDER SERVICE ĐỂ XỬ LÝ ===
            Order newOrder = orderService.processOrder(
                    customerName,
                    customerPhone,
                    customerAddress,
                    cartItemsJson,
                    voucherCode,
                    user // user có thể là null nếu là khách
            );
            // === KẾT THÚC GỌI SERVICE ===

            // 3. Nếu thành công:
            redirectAttributes.addFlashAttribute("successMessage", "Đặt hàng thành công! Mã đơn hàng của bạn là: #DH" + newOrder.getId_order());

            // TODO: Bạn nên tạo một trang "đặt hàng thành công" (order_success.html)
            // Trang này sẽ chứa JavaScript để xóa sessionStorage('bookstoreCart')

            return "redirect:/"; // Tạm thời chuyển về trang chủ

        } catch (Exception e) {
            // 4. Nếu thất bại:
            e.printStackTrace(); // In lỗi ra console server
            redirectAttributes.addFlashAttribute("errorMessage", "Đặt hàng thất bại: " + e.getMessage());
            // Quay lại trang thanh toán
            return "redirect:/checkout";
        }
    }
}