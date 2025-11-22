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

    @GetMapping("/return")
    public String handlePaymentReturn(
            @RequestParam("status") String status,
            @RequestParam("orderCode") String orderCodeString,
            RedirectAttributes redirectAttributes
    ) {
        try {
            // --- GIẢI MÃ ORDER ID ---
            // Lấy orderCode chia cho 10000 để ra ID gốc
            long orderCode = Long.parseLong(orderCodeString);
            int orderId = (int) (orderCode / 10000L);

            if ("PAID".equalsIgnoreCase(status)) {
                // Thanh toán thành công
                orderService.confirmPayment(orderId);
                redirectAttributes.addFlashAttribute("successMessage",
                        "Thanh toán thành công! Đơn hàng #" + orderId + " đã được xác nhận.");
                return "redirect:/order/success/" + orderId;
            } else {
                // Thanh toán thất bại hoặc hủy -> Gọi hàm Hủy đơn để hoàn kho
                orderService.cancelOrder(orderId);
                redirectAttributes.addFlashAttribute("errorMessage", "Giao dịch thất bại hoặc đã bị hủy.");
                return "redirect:/user/cart";
            }
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi xử lý kết quả thanh toán: " + e.getMessage());
            return "redirect:/";
        }
    }

    @GetMapping("/cancel")
    public String handlePaymentCancel(@RequestParam("orderCode") String orderCodeString, RedirectAttributes redirectAttributes) {
        try {
            // --- GIẢI MÃ ORDER ID ---
            long orderCode = Long.parseLong(orderCodeString);
            int orderId = (int) (orderCode / 10000L);

            // Gọi hàm hủy đơn để hoàn kho
            orderService.cancelOrder(orderId);

            redirectAttributes.addFlashAttribute("errorMessage", "Bạn đã hủy thanh toán. Đơn hàng đã được hủy.");
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi hủy đơn hàng.");
        }
        return "redirect:/user/cart";
    }
}