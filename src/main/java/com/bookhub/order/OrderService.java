package com.bookhub.order;

import com.bookhub.product.ProductRepository;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.Locale;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // ===============================================
    // === INNER STATIC CLASSES (DTOs MỚI TẠO) ===
    // ===============================================

    /** DTO cho dữ liệu sản phẩm bán chạy. */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductSaleStats {
        private String title;
        private Long quantitySold;
        private Long totalRevenue;
    }

    /** DTO tổng hợp dữ liệu cho Dashboard/Báo cáo. */
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class RevenueStatsDTO {
        Long totalRevenue;
        Long totalDeliveredOrders;
        List<ProductSaleStats> topSellingProducts;
        List<DataPoint> monthlyRevenue;

        @Getter
        @Setter
        public static class DataPoint {
            String label;
            Double value;
        }
    }


    // ===============================================
    // === CHỨC NĂNG THỐNG KÊ ===
    // ===============================================

    public long getTotalRevenue(Integer year) {
        return orderRepository.sumTotalDeliveredOrders(year)
                .orElse(0L);
    }

    public long getTotalDeliveredOrders(Integer year) {
        return orderRepository.countDeliveredOrders(year);
    }

    private List<RevenueStatsDTO.DataPoint> getMonthlyRevenueData(Integer year) {
        List<Object[]> rawData = orderRepository.findMonthlyRevenueAndProfit(year);

        return rawData.stream().map(row -> {
            Integer month = (Integer) row[0];
            Long revenue = (Long) row[1];

            String monthLabel = YearMonth.of(year, month).getMonth().getDisplayName(TextStyle.SHORT, Locale.forLanguageTag("vi-VN"));
            Double revenueInMillions = revenue / 1_000_000.0;

            RevenueStatsDTO.DataPoint dataPoint = new RevenueStatsDTO.DataPoint();
            dataPoint.setLabel(monthLabel);
            dataPoint.setValue(revenueInMillions);

            return dataPoint;
        }).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public RevenueStatsDTO getRevenueDashboardStats(Integer year) {
        RevenueStatsDTO stats = new RevenueStatsDTO();

        stats.setTotalRevenue(getTotalRevenue(year));
        stats.setTotalDeliveredOrders(getTotalDeliveredOrders(year));

        // Lấy dữ liệu thô (Object[]) từ Repository
        List<Object[]> rawTopProducts = orderRepository.findAllSellingProductsByYear(year);

        // MAPPING thủ công từ Object[] sang DTO
        List<ProductSaleStats> allTopProducts = rawTopProducts.stream().map(row ->
                new ProductSaleStats(
                        (String) row[0],         // title
                        (Long) row[1],           // quantitySold
                        (Long) row[2]            // totalRevenue
                )
        ).collect(Collectors.toList());


        List<ProductSaleStats> top5Products = allTopProducts.stream()
                .limit(5)
                .collect(Collectors.toList());

        stats.setTopSellingProducts(top5Products);
        stats.setMonthlyRevenue(getMonthlyRevenueData(year));

        return stats;
    }


    // ===============================================
    // === NEW: EXPORT TO EXCEL FUNCTIONALITY ===
    // ===============================================

    public ByteArrayInputStream exportRevenueData(RevenueStatsDTO stats, Integer year) throws IOException {

        DecimalFormat currencyFormatter = new DecimalFormat("#,###₫");
        DecimalFormat numberFormatter = new DecimalFormat("#,###");

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            // --- SHEET 1: TỔNG QUAN DOANH THU ---
            Sheet summarySheet = workbook.createSheet("TỔNG QUAN NĂM " + year);

            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.BLUE_GREY.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            summarySheet.setColumnWidth(0, 8000);
            summarySheet.setColumnWidth(1, 8000);

            Row titleRow = summarySheet.createRow(0);
            titleRow.createCell(0).setCellValue("BÁO CÁO DOANH THU NĂM " + year);

            Row row1 = summarySheet.createRow(2);
            row1.createCell(0).setCellValue("TỔNG DOANH THU (Đơn hàng DELIVERED)");
            row1.createCell(1).setCellValue(currencyFormatter.format(stats.getTotalRevenue()));

            Row row2 = summarySheet.createRow(3);
            row2.createCell(0).setCellValue("TỔNG SỐ ĐƠN HÀNG ĐÃ GIAO");
            row2.createCell(1).setCellValue(numberFormatter.format(stats.getTotalDeliveredOrders()));


            // --- SHEET 2: TOP SẢN PHẨM BÁN CHẠY (Đã đảm bảo logic ghi dữ liệu) ---
            Sheet topProductsSheet = workbook.createSheet("TOP SẢN PHẨM BÁN CHẠY");

            Row headerRow = topProductsSheet.createRow(0);
            String[] headers = {"#", "Tên sản phẩm", "Số lượng bán", "Tổng doanh thu", "Tỷ lệ (%)"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            topProductsSheet.setColumnWidth(0, 1500);
            topProductsSheet.setColumnWidth(1, 12000);
            topProductsSheet.setColumnWidth(2, 4000);
            topProductsSheet.setColumnWidth(3, 5000);
            topProductsSheet.setColumnWidth(4, 3000);

            // Cell Style cho giá trị tiền tệ
            CellStyle currencyStyle = workbook.createCellStyle();
            currencyStyle.setDataFormat(workbook.createDataFormat().getFormat("#,##0\"₫\""));

            // Cell Style cho phần trăm (định dạng trong Excel)
            CellStyle percentStyle = workbook.createCellStyle();
            percentStyle.setDataFormat(workbook.createDataFormat().getFormat("0.00%"));


            int rowNum = 1;

            double safeTotalRevenue = (stats.getTotalRevenue() != null && stats.getTotalRevenue() > 0) ? stats.getTotalRevenue().doubleValue() : 1.0;

            for (ProductSaleStats product : stats.getTopSellingProducts()) {
                Row row = topProductsSheet.createRow(rowNum++);

                row.createCell(0).setCellValue(rowNum - 1);
                row.createCell(1).setCellValue(product.getTitle());
                row.createCell(2).setCellValue(product.getQuantitySold());

                Cell revenueCell = row.createCell(3);
                revenueCell.setCellValue(product.getTotalRevenue());
                revenueCell.setCellStyle(currencyStyle);

                double saleRatio = 0.0;
                if (product.getTotalRevenue() != null && safeTotalRevenue > 1.0) {
                    saleRatio = product.getTotalRevenue().doubleValue() / safeTotalRevenue;
                }

                Cell percentCell = row.createCell(4);
                percentCell.setCellValue(saleRatio);
                percentCell.setCellStyle(percentStyle);
            }

            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());

        } catch (IOException e) {
            throw new IOException("Lỗi khi tạo file Excel: " + e.getMessage(), e);
        }
    }

    // ===============================================
    // === CÁC HÀM CRUD & MAPPING CŨ (Giữ nguyên) ===
    // ===============================================

    public List<OrderDTO> findAllOrders() {
        List<Order> orders = orderRepository.findAllWithUserAndDetails();
        return orders.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public OrderDTO findOrderById(Integer id) {
        Order order = orderRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng với ID: " + id));
        return mapToDetailDTO(order);
    }

    public List<OrderDTO> filterOrders(String status) {
        if (status == null || status.isEmpty()) {
            return findAllOrders();
        }
        return orderRepository.findByStatus_orderIgnoreCase(status).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public List<OrderDTO> searchOrders(String searchTerm) {
        return orderRepository.searchOrders(searchTerm).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }


    @Transactional
    public void updateOrderStatus(Integer id, String newStatus) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng để cập nhật trạng thái."));
        order.setStatus_order(newStatus);
        orderRepository.save(order);
    }

    @Transactional
    public void cancelOrder(Integer id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng để hủy."));
        if ("DELIVERED".equalsIgnoreCase(order.getStatus_order()) || "CANCELLED".equalsIgnoreCase(order.getStatus_order())) {
            throw new RuntimeException("Không thể hủy đơn hàng đã giao hoặc đã hủy.");
        }
        order.setStatus_order("CANCELLED");
        orderRepository.save(order);
    }

    // --- MAPPING HELPERS ---

    private OrderDTO mapToDTO(Order entity) {
        OrderDTO dto = new OrderDTO();
        dto.setIdOrder(entity.getId_order());
        dto.setOrderCode("#DH" + entity.getId_order());
        dto.setCustomerUsername(entity.getUser().getUsername());
        dto.setCustomerPhone(entity.getPhone());
        dto.setTotalAmount(entity.getTotal());
        dto.setTotalAmountFormatted(String.format("%,dđ", entity.getTotal()).replace(",", "."));
        dto.setStatus(entity.getStatus_order());
        dto.setDate(entity.getDate());
        dto.setDateFormatted(entity.getDate().format(DATE_FORMATTER));

        if (entity.getOrderDetails() != null) {
            long count = entity.getOrderDetails().stream().mapToLong(OrderDetail::getQuantity).sum();
            dto.setTotalProducts((int) count);
        } else {
            dto.setTotalProducts(0);
        }
        return dto;
    }

    private OrderDTO mapToDetailDTO(Order entity) {
        OrderDTO dto = mapToDTO(entity);
        dto.setAddress(entity.getAddress());
        dto.setPaymentMethod(entity.getPayment_method());
        dto.setNote(entity.getNote());
        dto.setUserId(entity.getUser().getIdUser());

        List<OrderDetailDTO> detailDTOs = entity.getOrderDetails().stream().map(this::mapDetailToDTO).collect(Collectors.toList());
        dto.setProductDetails(detailDTOs);

        return dto;
    }

    private OrderDetailDTO mapDetailToDTO(OrderDetail detail) {
        OrderDetailDTO dto = new OrderDetailDTO();
        dto.setQuantity(detail.getQuantity().intValue());
        dto.setPriceAtDate(detail.getPrice_date());
        dto.setPriceAtDateFormatted(String.format("%,dđ", detail.getPrice_date()).replace(",", "."));

        if (detail.getProduct() != null) {
            dto.setProductName(detail.getProduct().getTitle());
            dto.setProductAuthor(detail.getProduct().getAuthor());
        }
        return dto;
    }
}