package com.bookhub.order;

import com.bookhub.order.OrderService.RevenueStatsDTO;
import com.bookhub.order.OrderService.ProductSaleStats;
import com.bookhub.user.User;
import com.bookhub.user.UserService;
import lombok.*;
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
import java.util.ArrayList;
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

        // Gọi hàm đã sửa để tạo dữ liệu cho cả Tháng, Quý và Năm
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
    public ResponseEntity<byte[]> exportRevenueToExcel(@RequestParam(value = "year", required = false) Integer year) {

        Integer currentYear = (year != null) ? year : Year.now().getValue();

        try {
            OrderService.RevenueStatsDTO stats = orderService.getRevenueDashboardStats(currentYear);
            ByteArrayInputStream bis = orderService.exportRevenueData(stats, currentYear);

            String fileName = "ThongKeDoanhThu_" + currentYear + ".xlsx";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", fileName);
            headers.setContentLength(bis.available());

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

        @Getter @Setter @NoArgsConstructor // Khắc phục lỗi: @Setter cung cấp phương thức setTitle
        public static class TimeData {
            private String title; // <<< ĐÃ THÊM TRƯỜNG VÀ @Getter/@Setter
            private List<String> labels;
            private List<Double> revenue;

            // Constructor cho dữ liệu cơ bản (Monthly data)
            public TimeData(List<String> labels, List<Double> revenue) {
                this.labels = labels;
                this.revenue = revenue;
                this.title = null;
            }
        }
    }

    @Getter @Setter @AllArgsConstructor
    public static class ErrorResponse {
        private String message;
    }

    // --- HÀM ĐÃ SỬA: TẠO DỮ LIỆU BIỂU ĐỒ CHO THÁNG, QUÝ, NĂM ---
    private ChartData createChartData(List<RevenueStatsDTO.DataPoint> monthlyData) {
        ChartData chartData = new ChartData();
        Integer currentYear = Year.now().getValue();

        // Lấy dữ liệu theo tháng đã có (dưới dạng Triệu VNĐ)
        List<String> monthlyLabels = monthlyData.stream()
                .map(RevenueStatsDTO.DataPoint::getLabel)
                .collect(Collectors.toList());

        List<Double> monthlyRevenue = monthlyData.stream()
                .map(RevenueStatsDTO.DataPoint::getValue)
                .collect(Collectors.toList());

        // 1. Dữ liệu theo THÁNG
        ChartData.TimeData monthlyTimeData = new ChartData.TimeData(monthlyLabels, monthlyRevenue);
        // Lỗi đã được khắc phục: setTitle() đã có sẵn nhờ @Setter
        monthlyTimeData.setTitle("Biểu đồ Doanh thu theo Tháng (Năm " + currentYear + ")");
        chartData.setMonthly(monthlyTimeData);

        // 2. Dữ liệu theo QUÝ (Tổng hợp từ dữ liệu theo tháng)
        List<Double> quarterlyRevenue = new ArrayList<>();
        List<String> quarterlyLabels = List.of("Quý 1", "Quý 2", "Quý 3", "Quý 4");

        // Tổng hợp dữ liệu theo quý (3 tháng/quý)
        for (int q = 0; q < 4; q++) {
            double quarterTotal = 0.0;
            // Dữ liệu monthlyRevenue có thể không có đủ 12 tháng, nên ta phải kiểm tra kích thước
            int startIndex = q * 3;
            int endIndex = Math.min(startIndex + 3, monthlyRevenue.size());

            for (int i = startIndex; i < endIndex; i++) {
                if (monthlyRevenue.get(i) != null) {
                    quarterTotal += monthlyRevenue.get(i);
                }
            }
            quarterlyRevenue.add(quarterTotal);
        }

        ChartData.TimeData quarterlyTimeData = new ChartData.TimeData(quarterlyLabels, quarterlyRevenue);
        quarterlyTimeData.setTitle("Biểu đồ Doanh thu theo Quý (Năm " + currentYear + ")");
        chartData.setQuarterly(quarterlyTimeData);


        // 3. Dữ liệu theo NĂM (Tổng doanh thu của năm hiện tại)
        // Lấy tổng doanh thu của tất cả các tháng đã có
        double yearTotal = monthlyRevenue.stream().filter(v -> v != null).mapToDouble(Double::doubleValue).sum();

        ChartData.TimeData yearlyTimeData = new ChartData.TimeData(
                List.of(String.valueOf(currentYear)),
                List.of(yearTotal)
        );
        yearlyTimeData.setTitle("Biểu đồ Tổng Doanh thu theo Năm " + currentYear);
        chartData.setYearly(yearlyTimeData);

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