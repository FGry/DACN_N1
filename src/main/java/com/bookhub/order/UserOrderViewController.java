package com.bookhub.order; // Hãy kiểm tra package này có đúng với dự án của bạn không

import com.bookhub.user.User;
import com.bookhub.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class UserOrderViewController {

    private final OrderService orderService;
    private final UserService userService;

    // ============================================================
    // 1. CÁC TRANG DÀNH CHO USER ĐÃ ĐĂNG NHẬP
    // ============================================================

    @GetMapping("/my-orders")
    public String showMyOrdersPage(Model model, Principal principal, RedirectAttributes redirectAttributes) {
        if (principal == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Vui lòng đăng nhập để xem lịch sử đơn hàng.");
            return "redirect:/login";
        }
        try {
            String email = principal.getName();
            User user = userService.findUserByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản người dùng."));
            List<OrderDTO> userOrders = orderService.findOrdersByUserId(user.getIdUser());
            model.addAttribute("orders", userOrders);
            model.addAttribute("loggedInUser", user);
            return "user/my_orders";
        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/";
        }
    }

    @GetMapping("/order/success/{orderId}")
    public String showOrderSuccessPage(@PathVariable("orderId") Integer orderId, Model model, RedirectAttributes redirectAttributes) {
        try {
            List<OrderDetailDTO> orderDetails = orderService.getOrderDetailsByOrderId(orderId);
            Order order = orderService.getOrderById(orderId);
            model.addAttribute("orderDetails", orderDetails);
            model.addAttribute("order", order);
            return "user/order_success";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy đơn hàng.");
            return "redirect:/my-orders";
        }
    }

    // ============================================================
    // 2. CÁC TRANG DÀNH CHO KHÁCH VÃNG LAI (GUEST)
    // ============================================================

    /**
     * Endpoint tạo hình ảnh QR Code
     */
    @GetMapping(value = "/order/qrcode/{orderToken}", produces = MediaType.IMAGE_PNG_VALUE)
    public @ResponseBody byte[] getOrderQRCode(@PathVariable String orderToken) {
        try {
            // Thay đổi localhost bằng domain thật khi deploy
            String BASE_URL = "http://localhost:8080";
            String orderUrl = BASE_URL + "/order/guest/view/" + orderToken;
            return QRCodeGenerator.generateQRCodeImage(orderUrl);
        } catch (Exception e) {
            e.printStackTrace();
            return new byte[0];
        }
    }

    /**
     * Endpoint xem chi tiết đơn hàng cho khách vãng lai (CÓ KIỂM TRA HẠN 30 NGÀY)
     */
    @GetMapping("/order/guest/view/{orderToken}")
    public String viewGuestOrder(@PathVariable String orderToken, Model model) {
        try {
            Order order = orderService.getOrderByToken(orderToken);

            // === LOGIC KIỂM TRA 30 NGÀY ===
            LocalDate orderDate = order.getDate();
            LocalDate expireDate = orderDate.plusDays(30);

            if (LocalDate.now().isAfter(expireDate)) {
                // Nếu quá 30 ngày -> Báo lỗi
                model.addAttribute("errorMessage", "Liên kết này đã hết hạn (Quá 30 ngày). Vì lý do bảo mật, chúng tôi không thể hiển thị chi tiết.");
                return "error/404"; // Hoặc trang thông báo lỗi của bạn
            }
            // ==============================

            List<OrderDetailDTO> orderDetails = orderService.getOrderDetailsByOrder(order);
            model.addAttribute("order", order);
            model.addAttribute("orderDetails", orderDetails);

            // Trả về giao diện xem chi tiết riêng cho khách (file guest-order-details.html bạn đã tạo)
            return "user/guest-order-details";
        } catch (RuntimeException e) {
            model.addAttribute("errorMessage", "Đơn hàng không tồn tại hoặc mã truy cập không hợp lệ.");
            return "error/404";
        }
    }
}