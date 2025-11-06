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

// ===== IMPORT MỚI ĐƯỢC THÊM VÀO =====
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import jakarta.servlet.http.HttpServletResponse; // Đã đổi sang jakarta
import java.io.IOException;
import java.text.NumberFormat;
import java.util.Locale;
// ===== KẾT THÚC IMPORT MỚI =====

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

        // ===== BẮT ĐẦU DEBUG [1] =====
        System.out.println("DEBUG [TRANG WEB]: Lấy dữ liệu cho trang web. Số lượng Top Products: " + (topProducts != null ? topProducts.size() : "NULL"));
        // ===== KẾT THÚC DEBUG [1] =====

        List<Map<String, Object>> topProductsForView = prepareTopProductsForView(topProducts, totalRevenue);
        model.addAttribute("topSellingProducts", topProductsForView);
        model.addAttribute("chartData", getRealChartData(stats.getMonthlyRevenue()));
        model.addAttribute("revenueReports",Collections.emptyList());

        return "admin/revenue";
    }

    // ===== BẮT ĐẦU CODE MỚI THÊM VÀO =====

    /**
     * Endpoint xử lý yêu cầu xuất báo cáo doanh thu ra file Excel.
     * Được kích hoạt bởi nút "Xuất Excel" trên giao diện.
     *
     * @param year Năm cần xuất báo cáo
     * @param response Đối tượng HttpServletResponse để ghi file Excel vào
     * @throws IOException Nếu có lỗi trong quá trình ghi file
     */
    @GetMapping("/admin/revenue/export")
    @PreAuthorize("hasRole('ADMIN')")
    public void exportRevenueReport(
            @RequestParam("year") int year,
            HttpServletResponse response) throws IOException {

        // --- 1. LẤY DỮ LIỆU ---
        RevenueStatsDTO stats = orderService.getRevenueDashboardStats(year);
        long totalRevenue = stats.getTotalRevenue();
        long totalOrders = stats.getTotalDeliveredOrders();
        List<ProductSaleStats> topProducts = stats.getTopSellingProducts();

        // ===== BẮT ĐẦU DEBUG [2] =====
        System.out.println("DEBUG [XUẤT EXCEL]: Lấy dữ liệu cho file Excel. Số lượng Top Products: " + (topProducts != null ? topProducts.size() : "NULL"));
        // ===== KẾT THÚC DEBUG [2] =====

        // Chuẩn bị định dạng tiền tệ
        Locale vn = new Locale("vi", "VN");
        NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(vn);

        // --- 2. TẠO FILE EXCEL BẰNG APACHE POI ---
        Workbook workbook = new XSSFWorkbook();

        // --- Sheet 1: Tổng quan ---
        Sheet summarySheet = workbook.createSheet("Tổng quan Doanh thu " + year);
        summarySheet.setColumnWidth(0, 7000); // Tăng độ rộng cột A
        summarySheet.setColumnWidth(1, 5000); // Tăng độ rộng cột B

        // Tạo font và style cho tiêu đề
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setFontHeightInPoints((short) 14);
        CellStyle headerStyle = workbook.createCellStyle();
        headerStyle.setFont(headerFont);

        // Tiêu đề
        Row titleRow = summarySheet.createRow(0);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("Báo cáo Doanh thu năm " + year);
        titleCell.setCellStyle(headerStyle);

        // Dữ liệu tổng quan
        Row revenueRow = summarySheet.createRow(2);
        revenueRow.createCell(0).setCellValue("Tổng Doanh Thu:");
        revenueRow.createCell(1).setCellValue(currencyFormatter.format(totalRevenue));

        Row orderRow = summarySheet.createRow(3);
        orderRow.createCell(0).setCellValue("Tổng Số Đơn Hàng Đã Giao:");
        orderRow.createCell(1).setCellValue(totalOrders);


        // --- Sheet 2: Top Sản phẩm Bán chạy ---
        Sheet productSheet = workbook.createSheet("Top Sản phẩm Bán chạy");
        String[] headers = {"#", "Tên sản phẩm", "Số lượng bán", "Tổng doanh thu", "Tỷ lệ (%)"};

        // Tạo hàng tiêu đề
        Row headerRow = productSheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle); // Sử dụng lại style tiêu đề
        }

        // Đổ dữ liệu
        int rowNum = 1;

        // Thêm kiểm tra null để an toàn
        if (topProducts != null) {
            for (ProductSaleStats product : topProducts) {
                Row row = productSheet.createRow(rowNum++);
                row.createCell(0).setCellValue(rowNum - 1);
                row.createCell(1).setCellValue(product.getProductName());
                row.createCell(2).setCellValue(product.getTotalQuantity());
                row.createCell(3).setCellValue(currencyFormatter.format(product.getTotalRevenue()));

                // Tính toán lại tỷ lệ (giống hệt hàm prepareTopProductsForView)
                double saleRatio = (totalRevenue > 0)
                        ? (product.getTotalRevenue().doubleValue() / totalRevenue) * 100
                        : 0.0;

                row.createCell(4).setCellValue(String.format("%.1f%%", saleRatio));
            }
        } // Kết thúc kiểm tra if (topProducts != null)

        // Tự động điều chỉnh độ rộng cột cho sheet sản phẩm
        for (int i = 0; i < headers.length; i++) {
            productSheet.autoSizeColumn(i);
        }

        // --- 3. THIẾT LẬP RESPONSE ĐỂ TẢI FILE ---
        String fileName = "BaoCaoDoanhThu_" + year + ".xlsx";

        // Báo cho trình duyệt biết đây là file Excel
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        // Báo cho trình duyệt tải file về với tên chỉ định
        response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");

        // Ghi workbook ra OutputStream của response
        workbook.write(response.getOutputStream());

        // Đóng workbook để giải phóng tài nguyên
        workbook.close();
    }

    // ===== KẾT THÚC CODE MỚI THÊM VÀO =====


    // --- CÁC PHƯƠNG THỨC HELPER HIỆN CÓ (Giữ nguyên) ---

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