package com.bookhub.order;

import com.bookhub.order.OrderService.RevenueStatsDTO;
import com.bookhub.order.OrderService.ProductSaleStats;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Year;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @GetMapping("/revenue")
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


    //xuat excel
    @GetMapping("/revenue/export")
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
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi xuất file Excel: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    // ===============================================
    // === HELPER DTOs FOR CONTROLLER ONLY ===
    // ===============================================

    @Getter
    @Setter
    private static class ProductStatsDTO {
        private String name;
        private Long quantitySold;
        private Long totalRevenue;
        private Double saleRatio;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    private static class ChartData {
        private TimeData monthly;
        private TimeData quarterly;
        private TimeData yearly;

        @Getter
        @Setter
        @AllArgsConstructor
        @NoArgsConstructor
        private static class TimeData {
            private List<String> labels;
            private List<Double> revenue;
        }
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


    @GetMapping("/carts")
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

    @GetMapping("/carts/detail/{id}")
    @ResponseBody
    public OrderDTO getOrderDetail(@PathVariable("id") Integer id) {
        return orderService.findOrderById(id);
    }

    @PostMapping("/carts/update-status/{id}")
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

    @GetMapping("/carts/cancel/{id}")
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