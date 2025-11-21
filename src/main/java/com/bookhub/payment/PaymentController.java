package com.bookhub.payment;

import com.bookhub.order.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/payment/payos")
@RequiredArgsConstructor
public class PaymentController {

    private final OrderService orderService;

    // PayOS sẽ gọi URL này khi thanh toán xong (thành công hoặc hủy)
    @GetMapping("/return")
    public String handlePaymentReturn(
            @RequestParam("status") String status,
            @RequestParam("orderCode") String orderCodeString,
            RedirectAttributes redirectAttributes
    ) {
        try {
            Integer orderId = Integer.parseInt(orderCodeString);

            if ("PAID".equalsIgnoreCase(status)) {
                // Thanh toán thành công -> Cập nhật status đơn hàng
                orderService.confirmPayment(orderId);

                redirectAttributes.addFlashAttribute("successMessage",
                        "Thanh toán thành công! Đơn hàng #" + orderId + " đã được xác nhận.");
                return "redirect:/order/success/" + orderId;
            } else {
                // Thanh toán thất bại hoặc hủy
                orderService.cancelOrder(orderId); // Tùy logic, có thể hủy luôn đơn
                redirectAttributes.addFlashAttribute("errorMessage", "Thanh toán đã bị hủy hoặc thất bại.");
                return "redirect:/my-orders"; // Hoặc quay lại trang giỏ hàng
            }
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("errorMessage", "Có lỗi xảy ra khi xử lý kết quả thanh toán.");
            return "redirect:/";
        }
    }

    @GetMapping("/cancel")
    public String handlePaymentCancel(@RequestParam("orderCode") String orderCodeString, RedirectAttributes redirectAttributes) {
        try {
            Integer orderId = Integer.parseInt(orderCodeString);
            orderService.cancelOrder(orderId);
            redirectAttributes.addFlashAttribute("errorMessage", "Bạn đã hủy thanh toán.");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "redirect:/user/cart";
    }
}