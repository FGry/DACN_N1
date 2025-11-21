package com.bookhub.order;

import com.bookhub.order.OrderDTO;
import com.bookhub.order.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.http.MediaType;

import java.util.List;

@Controller
@RequestMapping("/admin/carts")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @GetMapping
    public String listOrders(
            @RequestParam(value = "filterStatus", required = false) String filterStatus,
            @RequestParam(value = "searchTerm", required = false) String searchTerm,
            Model model) {
        List<OrderDTO> orders;
        if (searchTerm != null && !searchTerm.isEmpty()) {
            orders = orderService.searchOrders(searchTerm);
        } else if (filterStatus != null && !filterStatus.isEmpty()) {
            orders = orderService.filterOrders(filterStatus);
        } else {
            orders = orderService.findAllOrders();
        }
        model.addAttribute("orders", orders);
        model.addAttribute("statuses", List.of("PENDING", "CONFIRMED", "SHIPPING", "DELIVERED", "CANCELLED"));
        return "admin/cart";
    }

    @GetMapping("/detail/{id}")
    @ResponseBody
    public OrderDTO getOrderDetail(@PathVariable("id") Integer id) {
        return orderService.findOrderById(id);
    }

    @PostMapping("/update-status/{id}")
    public String updateOrderStatus(@PathVariable("id") Integer id, @RequestParam("newStatus") String newStatus, RedirectAttributes redirectAttributes) {
        try {
            orderService.updateOrderStatus(id, newStatus.toUpperCase());
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật thành công!");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/carts";
    }

    @GetMapping("/cancel/{id}")
    public String cancelOrder(@PathVariable("id") Integer id, RedirectAttributes redirectAttributes) {
        try {
            orderService.cancelOrder(id);
            redirectAttributes.addFlashAttribute("successMessage", "Đã hủy đơn hàng #" + id);
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/carts";
    }

    // ===============================================
    // === CÁC ENDPOINT MỚI CHO KHÁCH VÃNG LAI ===
    // ===============================================

    @GetMapping(value = "/order/qrcode/{orderToken}", produces = MediaType.IMAGE_PNG_VALUE)
    public @ResponseBody byte[] getOrderQRCode(@PathVariable String orderToken) {
        try {
            // LƯU Ý: Đổi 'localhost:8080' thành domain thật khi deploy
            String BASE_URL = "http://localhost:8080";
            String orderUrl = BASE_URL + "/order/guest/view/" + orderToken;
            return QRCodeGenerator.generateQRCodeImage(orderUrl);
        } catch (Exception e) {
            e.printStackTrace();
            return new byte[0];
        }
    }

    @GetMapping("/order/guest/view/{orderToken}")
    public String viewGuestOrder(@PathVariable String orderToken, Model model) {
        try {
            Order order = orderService.getOrderByToken(orderToken);
            List<OrderDetailDTO> orderDetails = orderService.getOrderDetailsByOrder(order);
            model.addAttribute("order", order);
            model.addAttribute("orderDetails", orderDetails);
            return "user/guest-order-details";
        } catch (RuntimeException e) {
            return "error/404";
        }
    }
}