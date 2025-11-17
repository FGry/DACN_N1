package com.bookhub.order; // Hoặc package controller của bạn

import com.bookhub.user.User;
import com.bookhub.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

@Controller
@RequiredArgsConstructor
public class UserOrderViewController {

    private final OrderService orderService;
    private final UserService userService;

    /**
     * Hiển thị trang "Lịch sử Đơn hàng" của người dùng.
     * Yêu cầu người dùng phải đăng nhập.
     */
    @GetMapping("/my-orders")
    public String showMyOrdersPage(Model model, Principal principal, RedirectAttributes redirectAttributes) {

        // 1. Kiểm tra đăng nhập
        if (principal == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Vui lòng đăng nhập để xem lịch sử đơn hàng.");
            return "redirect:/login"; // Cần trang đăng nhập
        }

        try {
            // 2. Lấy thông tin User từ Principal
            String email = principal.getName();
            User user = userService.findUserByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản người dùng."));

            // 3. Gọi Service để lấy đơn hàng
            List<OrderDTO> userOrders = orderService.findOrdersByUserId(user.getIdUser());

            // 4. Đưa vào Model
            model.addAttribute("orders", userOrders);
            model.addAttribute("loggedInUser", user);

            // 5. Trả về view
            return "user/my_orders"; // Tệp HTML (Bước 5)

        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi tải lịch sử đơn hàng: " + e.getMessage());
            return "redirect:/"; // Về trang chủ nếu lỗi
        }
    }

    /**
     * Hiển thị trang chi tiết đơn hàng (và trang "đặt hàng thành công")
     * Dùng chung cho cả lúc đặt hàng thành công và lúc xem lại chi tiết.
     */
    @GetMapping("/order/success/{orderId}")
    public String showOrderSuccessPage(
            @PathVariable("orderId") Integer orderId,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        try {
            // Lấy chi tiết các sản phẩm trong đơn
            List<OrderDetailDTO> orderDetails = orderService.getOrderDetailsByOrderId(orderId);

            // Lấy thông tin tổng quát của đơn hàng (người nhận, tổng tiền...)
            Order order = orderService.getOrderById(orderId);

            model.addAttribute("orderDetails", orderDetails);
            model.addAttribute("order", order);

            // Trả về tệp: /resources/templates/user/order_success.html (Bước 6)
            return "user/order_success";

        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy đơn hàng.");
            return "redirect:/my-orders"; // Quay về trang lịch sử nếu lỗi
        }
    }
}