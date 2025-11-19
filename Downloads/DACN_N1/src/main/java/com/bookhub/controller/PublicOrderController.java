package com.bookhub.controller;

import com.bookhub.order.GuestCheckoutDTO;
import com.bookhub.order.OrderDTO;
import com.bookhub.order.OrderService;
import com.bookhub.util.QRCodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class PublicOrderController {

    private final OrderService orderService;
    private final QRCodeService qrCodeService;

    private final String BASE_URL = "http://localhost:8080";

    @PostMapping("/checkout/guest")
    public String handleGuestCheckout(GuestCheckoutDTO checkoutDTO, RedirectAttributes redirectAttributes) {
        if (checkoutDTO.getPhone() == null || checkoutDTO.getPhone().trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Vui lòng nhập số điện thoại.");
            return "redirect:/user/cart";
        }
        try {
            OrderDTO order = orderService.placeGuestOrder(checkoutDTO);
            redirectAttributes.addFlashAttribute("successOrder", order);
            return "redirect:/order/success/" + order.getIdOrder();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi đặt hàng: " + e.getMessage());
            return "redirect:/user/cart";
        }
    }

    @GetMapping("/order/success/{orderId}")
    public String showOrderSuccessPage(@PathVariable("orderId") Integer orderId, Model model) {
        OrderDTO order = (OrderDTO) model.asMap().get("successOrder");

        if (order != null && order.getGuestAccessToken() != null && order.getIdOrder().equals(orderId)) {
            String qrContentUrl = BASE_URL + "/order/view?token=" + order.getGuestAccessToken();
            String qrBase64 = qrCodeService.generateQRCodeBase64(qrContentUrl);

            model.addAttribute("qrCodeBase64", qrBase64);
            model.addAttribute("order", order);
            // ⭐ SỬA: 30 ngày
            model.addAttribute("qrExpiryTime", "30 ngày");
            return "user/orderSuccess";
        }

        model.addAttribute("errorMessage", "Thông tin đơn hàng không tồn tại hoặc đã hết hạn.");
        return "error/404";
    }

    @GetMapping("/order/view")
    public String viewOrderByToken(@RequestParam("token") String token, Model model) {
        try {
            OrderDTO order = orderService.getOrderByAccessToken(token);
            model.addAttribute("order", order);
            model.addAttribute("successMessage", "Truy cập đơn hàng thành công.");
            return "user/orderDetail";
        } catch (RuntimeException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "user/orderAccessError";
        }
    }
}