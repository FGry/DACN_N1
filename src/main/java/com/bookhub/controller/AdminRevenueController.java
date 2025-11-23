package com.bookhub.controller;

import com.bookhub.order.OrderService;
import com.bookhub.order.OrderService.RevenueStatsDTO;
import com.bookhub.order.OrderService.ProductSaleStats;
import com.bookhub.order.OrderService.RevenueStatsDTO.DataPoint;

import lombok.RequiredArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.MediaType;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Year;
import java.util.List;
import java.util.ArrayList;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Controller
@RequestMapping("/admin/revenue")
@RequiredArgsConstructor
public class AdminRevenueController {

    private final OrderService orderService;


    @GetMapping
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


    @GetMapping("/export") // Map tới /admin/revenue/export
    public ResponseEntity<byte[]> exportRevenueToExcel(@RequestParam(value = "year", required = false) Integer year) {

        Integer currentYear = (year != null) ? year : Year.now().getValue();

        try {
            RevenueStatsDTO stats = orderService.getRevenueDashboardStats(currentYear);
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

        @Getter @Setter @NoArgsConstructor
        public static class TimeData {
            private String title;
            private List<String> labels;
            private List<Double> revenue;

            public TimeData(List<String> labels, List<Double> revenue) {
                this.labels = labels;
                this.revenue = revenue;
                this.title = null;
            }
        }
    }

    private List<ProductStatsDTO> mapTopProducts(List<ProductSaleStats> stats, Long totalRevenue) {
        double safeTotalRevenue = (totalRevenue != null && totalRevenue > 0) ? totalRevenue.doubleValue() : 1.0;

        return stats.stream().map(s -> {
            ProductStatsDTO dto = new ProductStatsDTO();

            dto.setName(s.getTitle());

            Long quantitySoldObject = s.getQuantitySold();
            Long productRevenueObject = s.getTotalRevenue();

            dto.setQuantitySold(quantitySoldObject != null ? quantitySoldObject : 0L);
            dto.setTotalRevenue(productRevenueObject != null ? productRevenueObject : 0L);

            double saleRatio = Optional.ofNullable(productRevenueObject)
                    .map(Long::doubleValue)
                    .map(revenue -> (revenue / safeTotalRevenue) * 100)
                    .orElse(0.0);

            dto.setSaleRatio(saleRatio);

            return dto;
        }).collect(Collectors.toList());
    }

    private ChartData createChartData(List<DataPoint> monthlyData) {
        ChartData chartData = new ChartData();
        Integer currentYear = Year.now().getValue();

        List<String> monthlyLabels = monthlyData.stream().map(DataPoint::getLabel).collect(Collectors.toList());
        List<Double> monthlyRevenue = monthlyData.stream().map(DataPoint::getValue).collect(Collectors.toList());

        ChartData.TimeData monthlyTimeData = new ChartData.TimeData(monthlyLabels, monthlyRevenue);
        monthlyTimeData.setTitle("Biểu đồ Doanh thu theo Tháng (Năm " + currentYear + ")");
        chartData.setMonthly(monthlyTimeData);

        // Tính toán dữ liệu theo QUÝ
        List<Double> quarterlyRevenue = new ArrayList<>();
        List<String> quarterlyLabels = List.of("Quý 1", "Quý 2", "Quý 3", "Quý 4");

        for (int q = 0; q < 4; q++) {
            double quarterTotal = 0.0;
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

        // Dữ liệu theo NĂM
        double yearTotal = monthlyRevenue.stream().filter(v -> v != null).mapToDouble(Double::doubleValue).sum();

        ChartData.TimeData yearlyTimeData = new ChartData.TimeData(
                List.of(String.valueOf(currentYear)),
                List.of(yearTotal)
        );
        yearlyTimeData.setTitle("Biểu đồ Tổng Doanh thu theo Năm " + currentYear);
        chartData.setYearly(yearlyTimeData);

        return chartData;
    }
}