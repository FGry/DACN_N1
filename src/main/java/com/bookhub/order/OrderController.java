package com.bookhub.order;

import com.bookhub.order.OrderService.RevenueStatsDTO;
import com.bookhub.order.OrderService.ProductSaleStats;
import com.bookhub.user.User;
import com.bookhub.user.UserService;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.http.HttpStatus;


import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Year;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import jakarta.validation.Valid;

@Controller
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final UserService userService;

    private Optional<User> getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated() &&
                !authentication.getPrincipal().equals("anonymousUser")) {
            String email = authentication.getName();
            return userService.findUserByEmail(email);
        }
        return Optional.empty();
    }

    @GetMapping("/admin/revenue")
    public String listRevenueStats(@RequestParam(value = "year", required = false) Integer year,
                                   Model model) {
        Integer currentYear = (year != null) ? year : Year.now().getValue();
        RevenueStatsDTO stats = orderService.getRevenueDashboardStats(currentYear);

        model.addAttribute("currentYear", currentYear);
        model.addAttribute("totalOrdersCount", stats.getTotalDeliveredOrders());

        String totalRevenueFormatted = String.format("%,d₫", stats.getTotalRevenue())
                .replace(",", ".");
        model.addAttribute("totalRevenueFormatted", totalRevenueFormatted);

        List<ProductStatsDTO> topSellingProducts = mapTopProducts(stats.getTopSellingProducts(), stats.getTotalRevenue());
        model.addAttribute("topSellingProducts", topSellingProducts);

        ChartData chartData = createChartData(stats.getMonthlyRevenue());
        model.addAttribute("chartData", chartData);

        List<Integer> listYears = IntStream.rangeClosed(Year.now().getValue() - 5, Year.now().getValue())
                .boxed()
                .sorted(java.util.Collections.reverseOrder())
                .collect(Collectors.toList());
        model.addAttribute("listYears", listYears);

        return "admin/revenue";
    }


    @GetMapping("/admin/revenue/export")
    public ResponseEntity<byte[]> exportRevenueToExcel(@RequestParam(value = "year", required = false) Integer year,
                                                       RedirectAttributes redirectAttributes) {

        Integer currentYear = (year != null) ? year : Year.now().getValue();

        try {
            OrderService.RevenueStatsDTO stats = orderService.getRevenueDashboardStats(currentYear);
            ByteArrayInputStream bis = orderService.exportRevenueData(stats, currentYear);

            String fileName = "ThongKeDoanhThu_" + currentYear + ".xlsx";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", fileName);

            return ResponseEntity
                    .ok()
                    .headers(headers)
                    .body(bis.readAllBytes());

        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/admin/carts")
    public String listOrders(
            @RequestParam(value = "filterStatus", required = false) String filterStatus,
            @RequestParam(value = "searchTerm", required = false) String searchTerm,
            Model model) {

        List<OrderDTO> orders;

        if (searchTerm != null && !searchTerm.isEmpty()) {
            orders = orderService.searchOrders(searchTerm);
        } else if (filterStatus != null && filterStatus.isEmpty() == false) {
            orders = orderService.filterOrders(filterStatus);
        } else {
            orders = orderService.findAllOrders();
        }

        model.addAttribute("orders", orders);
        model.addAttribute("statuses", List.of("PENDING", "CONFIRMED", "SHIPPING", "DELIVERED", "CANCELLED"));
        return "admin/cart";
    }

    @GetMapping("/admin/carts/detail/{id}")
    @ResponseBody
    public OrderDTO getOrderDetailAdmin(@PathVariable("id") Integer id) {
        return orderService.findOrderById(id);
    }

    @PostMapping("/admin/carts/update-status/{id}")
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

    @GetMapping("/admin/carts/cancel/{id}")
    public String cancelOrder(@PathVariable("id") Integer id, RedirectAttributes redirectAttributes) {
        try {
            orderService.cancelOrder(id);
            redirectAttributes.addFlashAttribute("successMessage", "Đã hủy đơn hàng #" + id + " thành công!");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi hủy đơn hàng: " + e.getMessage());
        }
        return "redirect:/admin/carts";
    }

    // --- PUBLIC/USER API MAPPINGS (AJAX cho Checkout và Lịch sử Đơn hàng) ---

    @PostMapping("/api/orders")
    @ResponseBody
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> createOrder(@Valid @RequestBody OrderCreationRequest request) {
        try {
            User authenticatedUser = getAuthenticatedUser()
                    .orElseThrow(() -> new RuntimeException("Lỗi xác thực: Không tìm thấy người dùng đang đăng nhập."));

            OrderDTO createdOrder = orderService.createOrder(request, authenticatedUser);

            return ResponseEntity.status(HttpStatus.CREATED).body(createdOrder);

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Tạo đơn hàng thất bại: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(new ErrorResponse("Lỗi hệ thống khi tạo đơn hàng."));
        }
    }

    @GetMapping("/api/users/{userId}/orders")
    @ResponseBody
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<OrderDTO>> getUserOrdersHistory(@PathVariable("userId") Integer userId) {
        try {
            User currentUser = getAuthenticatedUser()
                    .orElseThrow(() -> new RuntimeException("Lỗi xác thực."));

            if (!currentUser.getIdUser().equals(userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            List<OrderDTO> orders = orderService.findOrdersByUserId(userId);
            return ResponseEntity.ok(orders);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/api/orders/detail/{id}")
    @ResponseBody
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<OrderDTO> getOrderDetailUser(@PathVariable("id") Integer id) {
        try {
            User currentUser = getAuthenticatedUser()
                    .orElseThrow(() -> new RuntimeException("Lỗi xác thực."));

            OrderDTO orderDTO = orderService.findOrderById(id);

            // Kiểm tra quyền: Đảm bảo đơn hàng thuộc về người dùng hiện tại
            if (!orderDTO.getUserId().equals(currentUser.getIdUser())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            return ResponseEntity.ok(orderDTO);

        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @Getter @Setter
    private static class ProductStatsDTO {
        private String name;
        private Long quantitySold;
        private Long totalRevenue;
        private Double saleRatio;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    private static class ChartData {
        private TimeData monthly;
        private TimeData quarterly;
        private TimeData yearly;

        @Getter @Setter @AllArgsConstructor @NoArgsConstructor
        public static class TimeData {
            private List<String> labels;
            private List<Double> revenue;
        }
    }

    @Getter @Setter @AllArgsConstructor
    public static class ErrorResponse {
        private String message;
    }

    private ChartData createChartData(List<RevenueStatsDTO.DataPoint> monthlyData) {
        ChartData chartData = new ChartData();

        List<String> labels = monthlyData.stream()
                .map(RevenueStatsDTO.DataPoint::getLabel)
                .collect(Collectors.toList());

        List<Double> revenue = monthlyData.stream()
                .map(RevenueStatsDTO.DataPoint::getValue)
                .collect(Collectors.toList());

        ChartData.TimeData monthlyTimeData = new ChartData.TimeData(labels, revenue);
        chartData.setMonthly(monthlyTimeData);

        chartData.setQuarterly(new ChartData.TimeData());
        chartData.setYearly(new ChartData.TimeData());

        return chartData;
    }

    private List<ProductStatsDTO> mapTopProducts(List<ProductSaleStats> stats, Long totalRevenue) {
        double safeTotalRevenue = (totalRevenue != null && totalRevenue > 0) ? totalRevenue.doubleValue() : 1.0;

        return stats.stream().map(s -> {
            ProductStatsDTO dto = new ProductStatsDTO();
            dto.setName(s.getTitle());
            dto.setQuantitySold(s.getQuantitySold());
            dto.setTotalRevenue(s.getTotalRevenue());

            if (s.getTotalRevenue() != null) {
                double saleRatio = (s.getTotalRevenue().doubleValue() / safeTotalRevenue) * 100;
                dto.setSaleRatio(saleRatio);
            } else {
                dto.setSaleRatio(0.0);
            }

            return dto;
        }).collect(Collectors.toList());
    }
}