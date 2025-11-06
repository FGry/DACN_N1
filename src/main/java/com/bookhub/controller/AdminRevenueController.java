package com.bookhub.controller;

import com.bookhub.order.OrderService;
import com.bookhub.order.RevenueStatsDTO;
import com.bookhub.order.ProductSaleStats;
import com.bookhub.order.RevenueReportDTO;
import com.bookhub.order.RevenueStatsDTO.DataPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import java.time.LocalDate;
import java.time.Year;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.ArrayList;

@Controller
@RequiredArgsConstructor
public class AdminRevenueController {

    private final OrderService orderService;

    @GetMapping("/admin/revenue")
    @PreAuthorize("hasRole('ADMIN')")
    public String showRevenueDashboard(
            @RequestParam(value = "year", required = false) Integer filterYear,
            Model model) {

        Integer currentYear = (filterYear != null) ? filterYear : LocalDate.now().getYear();
        RevenueStatsDTO stats = orderService.getRevenueDashboardStats(currentYear);
        long totalRevenue = stats.getTotalRevenue();
        long totalDeliveredOrders = stats.getTotalDeliveredOrders();
        List<Integer> listYears = IntStream.range(LocalDate.now().getYear() - 4, LocalDate.now().getYear() + 1)
                .boxed().sorted(Collections.reverseOrder())
                .toList();

        model.addAttribute("currentYear", currentYear);
        model.addAttribute("listYears", listYears);
        model.addAttribute("totalRevenueFormatted", formatCurrency(totalRevenue));
        model.addAttribute("totalOrdersCount", totalDeliveredOrders);
        List<ProductSaleStats> topProducts = stats.getTopSellingProducts();
        List<Map<String, Object>> topProductsForView = prepareTopProductsForView(topProducts, totalRevenue);
        model.addAttribute("topSellingProducts", topProductsForView);
        model.addAttribute("chartData", getRealChartData(stats.getMonthlyRevenue()));
        model.addAttribute("revenueReports",Collections.emptyList());

        return "admin/revenue";
    }

    private String formatCurrency(long value) {
        if (value == 0) return "₫0";
        return String.format("%,d₫", value).replace(",", ".");
    }

    private List<Map<String, Object>> prepareTopProductsForView(List<ProductSaleStats> topProducts, long totalRevenue) {
        if (topProducts == null || topProducts.isEmpty() || totalRevenue == 0) {
            return Collections.emptyList();
        }

        return topProducts.stream().map(product -> {
            double saleRatio = (totalRevenue > 0)
                    ? (product.getTotalRevenue().doubleValue() / totalRevenue) * 100
                    : 0.0;

            return Map.<String, Object>of(
                    "name", product.getProductName(),
                    "quantitySold", product.getTotalQuantity(),
                    "totalRevenue", product.getTotalRevenue(),
                    "saleRatio", saleRatio
            );
        }).collect(Collectors.toList());
    }

    private Map<String, Object> getRealChartData(List<RevenueStatsDTO.DataPoint> monthlyData) {

        List<String> labels = monthlyData.stream()
                .map(DataPoint::getLabel)
                .collect(Collectors.toList());

        List<Double> revenueValues = monthlyData.stream()
                .map(DataPoint::getValue)
                .collect(Collectors.toList());
        Map<String, Object> chartData = new HashMap<>();

        // Dữ liệu Monthly (THỰC TẾ)
        chartData.put("monthly", Map.of(
                "title", "Biểu đồ Doanh thu theo tháng (Năm " + Year.now().getValue() + ")",
                "labels", labels,
                "revenue", revenueValues // Chỉ truyền Doanh thu
        ));

        // Dữ liệu Quarterly (GIẢ LẬP)
        chartData.put("quarterly", Map.of(
                "title", "Biểu đồ Doanh thu theo Quý (Dữ liệu giả lập)",
                "labels", List.of("Q1", "Q2", "Q3", "Q4"),
                "revenue", List.of(300.5, 350.0, 400.2, 450.0) // Chỉ truyền Doanh thu
        ));

        return chartData;
    }

}