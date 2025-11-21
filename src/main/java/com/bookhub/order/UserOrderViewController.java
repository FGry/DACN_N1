package com.bookhub.order;

import com.bookhub.user.User;
import com.bookhub.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
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
    private final OrderRepository orderRepository;

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
    @Transactional(readOnly = true) // Giữ session mở để Thymeleaf đọc dữ liệu
    public String showOrderSuccessPage(@PathVariable("orderId") Integer orderId, Model model, RedirectAttributes redirectAttributes) {
        try {
            // 1. Lấy danh sách sản phẩm (List DTO) để hiển thị bảng sản phẩm
            List<OrderDetailDTO> orderDetails = orderService.getOrderDetailsByOrderId(orderId);

            // 2. Lấy Order Entity có kèm Voucher và User để hiển thị thông tin chung
            Order order = orderRepository.findByIdWithFullDetails(orderId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng với ID: " + orderId));

            model.addAttribute("orderDetails", orderDetails);
            model.addAttribute("order", order);

            return "user/order_success";
        } catch (Exception e) {
            e.printStackTrace(); // Xem log lỗi nếu còn
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy đơn hàng hoặc lỗi hệ thống.");
            return "redirect:/my-orders";
        }
    }

    // ============================================================
    // 2. CÁC TRANG DÀNH CHO KHÁCH VÃNG LAI (GUEST)
    // ============================================================

    @GetMapping(value = "/order/qrcode/{orderToken}", produces = MediaType.IMAGE_PNG_VALUE)
    public @ResponseBody byte[] getOrderQRCode(@PathVariable String orderToken) {
        try {
            String BASE_URL = "http://localhost:8080"; // Cấu hình domain thực tế khi deploy
            String orderUrl = BASE_URL + "/order/guest/view/" + orderToken;
            return QRCodeGenerator.generateQRCodeImage(orderUrl);
        } catch (Exception e) {
            e.printStackTrace();
            return new byte[0];
        }
    }

    @GetMapping("/order/guest/view/{orderToken}")
    @Transactional(readOnly = true)
    public String viewGuestOrder(@PathVariable String orderToken, Model model) {
        try {
            Order order = orderService.getOrderByToken(orderToken);

            LocalDate orderDate = order.getDate();
            LocalDate expireDate = orderDate.plusDays(30);

            if (LocalDate.now().isAfter(expireDate)) {
                model.addAttribute("errorMessage", "Liên kết này đã hết hạn (Quá 30 ngày).");
                return "error/404";
            }

            List<OrderDetailDTO> orderDetails = orderService.getOrderDetailsByOrder(order);
            model.addAttribute("order", order);
            model.addAttribute("orderDetails", orderDetails);

            return "user/guest-order-details";
        } catch (RuntimeException e) {
            model.addAttribute("errorMessage", "Đơn hàng không tồn tại hoặc mã truy cập không hợp lệ.");
            return "error/404";
        }
    }
}