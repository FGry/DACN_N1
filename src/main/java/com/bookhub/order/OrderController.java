package com.bookhub.admin.order;

import com.bookhub.order.OrderDTO;
import com.bookhub.order.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/carts")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    // ĐÃ CẬP NHẬT: Nhận tham số status và searchTerm
    @GetMapping
    public String listOrders(
            @RequestParam(value = "filterStatus", required = false) String filterStatus,
            @RequestParam(value = "searchTerm", required = false) String searchTerm,
            Model model) {

        List<OrderDTO> orders;

        if (searchTerm != null && !searchTerm.isEmpty()) {
            // Ưu tiên tìm kiếm nếu có searchTerm
            orders = orderService.searchOrders(searchTerm);
        } else if (filterStatus != null && !filterStatus.isEmpty()) {
            // Sau đó đến lọc theo trạng thái
            orders = orderService.filterOrders(filterStatus);
        } else {
            // Mặc định: lấy tất cả
            orders = orderService.findAllOrders();
        }

        model.addAttribute("orders", orders);
        model.addAttribute("statuses", List.of("PENDING", "CONFIRMED", "SHIPPING", "DELIVERED", "CANCELLED"));
        return "admin/cartManage";
    }

    @GetMapping("/detail/{id}")
    @ResponseBody
    public OrderDTO getOrderDetail(@PathVariable("id") Integer id) {
        return orderService.findOrderById(id);
    }

    // ... (Giữ nguyên các phương thức POST/GET khác)
    @PostMapping("/update-status/{id}")
    public String updateOrderStatus(@PathVariable("id") Integer id,
                                    @RequestParam("newStatus") String newStatus,
                                    RedirectAttributes redirectAttributes) {
        try {
            orderService.updateOrderStatus(id, newStatus.toUpperCase());
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật trạng thái đơn hàng #" + id + " thành công!");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi cập nhật trạng thái: " + e.getMessage());
        }
        return "redirect:/admin/carts";
    }

    @GetMapping("/cancel/{id}")
    public String cancelOrder(@PathVariable("id") Integer id, RedirectAttributes redirectAttributes) {
        try {
            orderService.cancelOrder(id);
            redirectAttributes.addFlashAttribute("successMessage", "Đã hủy đơn hàng #" + id + " thành công!");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi hủy đơn hàng: " + e.getMessage());
        }
        return "redirect:/admin/carts";
    }
}