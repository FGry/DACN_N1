package com.bookhub.statistics;

import com.bookhub.statistics.ProductSalesDTO;
import com.bookhub.statistics.StatisticDTO;
import com.bookhub.statistics.StatisticService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Controller
@RequestMapping("/admin/revenue")
@RequiredArgsConstructor
public class StatisticController {

    private final StatisticService statisticService;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    /**
     * Main endpoint to render statistics page
     */
    @GetMapping
    public String viewStatistics(Model model) {
        LocalDate today = LocalDate.now();

        try {
            StatisticDTO overallStats = statisticService.getOverallStatistics("month", today);
            List<ProductSalesDTO> productSales = statisticService.getProductSales("month", today);

            model.addAttribute("stats", overallStats);
            model.addAttribute("productSales", productSales);
            model.addAttribute("currentDate", today);

            log.info("Statistics page loaded successfully for {}", today);
            return "admin/statistics";

        } catch (Exception e) {
            log.error("Error loading statistics page", e);
            model.addAttribute("error", "Không thể tải dữ liệu thống kê");
            return "admin/statistics";
        }
    }

    /**
     * AJAX endpoint for overall statistics
     */
    @GetMapping("/api/overall")
    @ResponseBody
    public StatisticDTO getOverallStatsApi(
            @RequestParam("period") String period,
            @RequestParam(value = "date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        log.debug("Fetching overall stats for period: {}, date: {}", period, date);
        return statisticService.getOverallStatistics(period, date);
    }

    /**
     * AJAX endpoint for product sales details
     */
    @GetMapping("/api/products")
    @ResponseBody
    public List<ProductSalesDTO> getProductSalesApi(
            @RequestParam("period") String period,
            @RequestParam(value = "date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        log.debug("Fetching product sales for period: {}, date: {}", period, date);
        return statisticService.getProductSales(period, date);
    }

    /**
     * Export statistics to Excel file
     */
    @GetMapping("/export")
    public void exportToExcel(
            @RequestParam("period") String period,
            @RequestParam(value = "date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            HttpServletResponse response) {

        try {
            // Set response headers
            String fileName = generateFileName(period, date);
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=" + fileName);

            // Get data
            List<Object[]> salesData = statisticService.getRawProductSalesForExport(period, date);

            // Create workbook
            try (XSSFWorkbook workbook = createExcelWorkbook(salesData, period, date)) {
                workbook.write(response.getOutputStream());
            }

            log.info("Excel export completed successfully: {}", fileName);

        } catch (Exception e) {
            log.error("Error exporting to Excel", e);
            try {
                response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
                response.getWriter().write("Lỗi khi xuất file Excel: " + e.getMessage());
            } catch (IOException ioException) {
                log.error("Error writing error response", ioException);
            }
        }
    }

    /**
     * Generate Excel filename based on period and date
     */
    private String generateFileName(String period, LocalDate date) {
        String dateString = (date != null) ? date.format(DATE_FORMATTER) : LocalDate.now().format(DATE_FORMATTER);
        return String.format("BaoCaoThongKe_%s_%s.xlsx", period, dateString);
    }

    /**
     * Create Excel workbook with sales data
     */
    private XSSFWorkbook createExcelWorkbook(List<Object[]> salesData, String period, LocalDate date) {
        XSSFWorkbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Thống kê sản phẩm");

        // Create styles
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle dataStyle = createDataStyle(workbook);
        CellStyle numberStyle = createNumberStyle(workbook);

        // Create title row
        Row titleRow = sheet.createRow(0);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("BÁO CÁO THỐNG KÊ SẢN PHẨM BÁN RA");
        titleCell.setCellStyle(createTitleStyle(workbook));
        sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, 3));

        // Create info row
        Row infoRow = sheet.createRow(1);
        Cell infoCell = infoRow.createCell(0);
        String dateString = (date != null) ? date.format(DATE_FORMATTER) : LocalDate.now().format(DATE_FORMATTER);
        infoCell.setCellValue("Kỳ báo cáo: " + period + " - " + dateString);
        sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(1, 1, 0, 3));

        // Create header row
        Row headerRow = sheet.createRow(3);
        String[] headers = {"ID Sản phẩm", "Tên sản phẩm", "Số lượng bán", "Tổng doanh thu (₫)"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // Fill data rows
        int rowNum = 4;
        long totalQuantity = 0;
        long totalRevenue = 0;

        for (Object[] rowData : salesData) {
            Row row = sheet.createRow(rowNum++);

            // ID
            Cell idCell = row.createCell(0);
            idCell.setCellValue((Integer) rowData[0]);
            idCell.setCellStyle(dataStyle);

            // Name
            Cell nameCell = row.createCell(1);
            nameCell.setCellValue((String) rowData[1]);
            nameCell.setCellStyle(dataStyle);

            // Quantity
            Long quantity = ((Number) rowData[2]).longValue();
            Cell qtyCell = row.createCell(2);
            qtyCell.setCellValue(quantity);
            qtyCell.setCellStyle(numberStyle);
            totalQuantity += quantity;

            // Revenue
            Long revenue = ((Number) rowData[3]).longValue();
            Cell revCell = row.createCell(3);
            revCell.setCellValue(revenue);
            revCell.setCellStyle(numberStyle);
            totalRevenue += revenue;
        }

        // Add total row
        Row totalRow = sheet.createRow(rowNum);
        Cell totalLabelCell = totalRow.createCell(1);
        totalLabelCell.setCellValue("TỔNG CỘNG");
        totalLabelCell.setCellStyle(headerStyle);

        Cell totalQtyCell = totalRow.createCell(2);
        totalQtyCell.setCellValue(totalQuantity);
        totalQtyCell.setCellStyle(numberStyle);

        Cell totalRevCell = totalRow.createCell(3);
        totalRevCell.setCellValue(totalRevenue);
        totalRevCell.setCellStyle(numberStyle);

        // Auto-size columns
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
            sheet.setColumnWidth(i, sheet.getColumnWidth(i) + 1000);
        }

        return workbook;
    }

    /**
     * Create title style for Excel
     */
    private CellStyle createTitleStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 16);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    /**
     * Create header style for Excel
     */
    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 11);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    /**
     * Create data style for Excel
     */
    private CellStyle createDataStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    /**
     * Create number style for Excel
     */
    private CellStyle createNumberStyle(Workbook workbook) {
        CellStyle style = createDataStyle(workbook);
        style.setAlignment(HorizontalAlignment.RIGHT);
        DataFormat format = workbook.createDataFormat();
        style.setDataFormat(format.getFormat("#,##0"));
        return style;
    }
}