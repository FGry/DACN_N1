package com.bookhub.statistics;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.NumberFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StatisticService {

    private final StatisticRepository statisticRepository;
    private static final NumberFormat VN_CURRENCY_FORMATTER = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));

    /**
     * Calculate date range based on period type
     */
    private LocalDate[] getDateRange(String period, LocalDate date) {
        if (date == null) {
            date = LocalDate.now();
        }

        LocalDate startDate;
        LocalDate endDate;

        switch (period.toLowerCase()) {
            case "day":
                startDate = endDate = date;
                break;
            case "week":
                startDate = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                endDate = date.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
                break;
            case "year":
                startDate = date.with(TemporalAdjusters.firstDayOfYear());
                endDate = date.with(TemporalAdjusters.lastDayOfYear());
                break;
            case "month":
            default:
                startDate = date.with(TemporalAdjusters.firstDayOfMonth());
                endDate = date.with(TemporalAdjusters.lastDayOfMonth());
                break;
        }

        log.debug("Date range for period '{}': {} to {}", period, startDate, endDate);
        return new LocalDate[]{startDate, endDate};
    }

    /**
     * Format currency in Vietnamese format
     */
    private String formatCurrency(Long amount) {
        if (amount == null || amount == 0) {
            return "0₫";
        }
        return String.format("%,d₫", amount).replace(",", ".");
    }

    /**
     * Format average value (can be decimal)
     */
    private String formatAverage(Double amount) {
        if (amount == null || amount == 0) {
            return "0₫";
        }
        return String.format("%,.0f₫", amount).replace(",", ".");
    }

    /**
     * Generate period text for display
     */
    private String getPeriodText(String period, LocalDate[] range) {
        switch (period.toLowerCase()) {
            case "day":
                return range[0].toString();
            case "week":
                return "Tuần " + range[0].format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            case "year":
                return "Năm " + range[0].getYear();
            case "month":
            default:
                return "Tháng " + range[0].getMonthValue() + "/" + range[0].getYear();
        }
    }

    /**
     * Get overall statistics for a given period
     */
    public StatisticDTO getOverallStatistics(String period, LocalDate date) {
        LocalDate[] range = getDateRange(period, date);
        List<Object[]> statsResult = statisticRepository.calculateOverallStats(range[0], range[1]);

        if (statsResult.isEmpty()) {
            log.warn("No statistics found for period '{}' on date {}", period, date);
            return createEmptyStatistics(period, range);
        }

        Object[] stats = statsResult.get(0);

        Long totalRevenue = stats[0] != null ? ((Number) stats[0]).longValue() : 0L;
        Long totalInvoices = stats[1] != null ? ((Number) stats[1]).longValue() : 0L;
        Double averageOrderValue = stats[2] != null ? ((Number) stats[2]).doubleValue() : 0.0;

        String periodText = getPeriodText(period, range);

        return StatisticDTO.builder()
                .period(periodText)
                .totalRevenue(totalRevenue)
                .totalInvoices(totalInvoices)
                .averageOrderValue(averageOrderValue)
                .totalRevenueFormatted(formatCurrency(totalRevenue))
                .averageOrderValueFormatted(formatAverage(averageOrderValue))
                .build();
    }

    /**
     * Create empty statistics when no data is available
     */
    private StatisticDTO createEmptyStatistics(String period, LocalDate[] range) {
        return StatisticDTO.builder()
                .period(getPeriodText(period, range))
                .totalRevenue(0L)
                .totalInvoices(0L)
                .averageOrderValue(0.0)
                .totalRevenueFormatted("0₫")
                .averageOrderValueFormatted("0₫")
                .build();
    }

    /**
     * Get product sales statistics
     */
    public List<ProductSalesDTO> getProductSales(String period, LocalDate date) {
        LocalDate[] range = getDateRange(period, date);
        List<Object[]> topProductsResult = statisticRepository.getTopSellingProducts(range[0], range[1]);

        if (topProductsResult.isEmpty()) {
            log.info("No product sales found for period '{}' on date {}", period, date);
            return List.of();
        }

        // Calculate total revenue for percentage calculation
        long grandTotalRevenue = topProductsResult.stream()
                .mapToLong(row -> ((Number) row[3]).longValue())
                .sum();

        log.info("Found {} products with total revenue: {}", topProductsResult.size(), grandTotalRevenue);

        return topProductsResult.stream()
                .map(row -> buildProductSalesDTO(row, grandTotalRevenue))
                .collect(Collectors.toList());
    }

    /**
     * Build ProductSalesDTO from raw query result
     */
    private ProductSalesDTO buildProductSalesDTO(Object[] row, long grandTotalRevenue) {
        Integer productId = (Integer) row[0];
        String productName = (String) row[1];
        Long quantitySold = ((Number) row[2]).longValue();
        Long productRevenue = ((Number) row[3]).longValue();

        double percentage = (grandTotalRevenue > 0)
                ? (productRevenue * 100.0 / grandTotalRevenue)
                : 0.0;

        return ProductSalesDTO.builder()
                .idProduct(productId)
                .productName(productName)
                .quantitySold(quantitySold)
                .totalRevenue(productRevenue)
                .totalRevenueFormatted(formatCurrency(productRevenue))
                .percentageOfTotal(percentage)
                .build();
    }

    /**
     * Get raw product sales data for Excel export
     */
    public List<Object[]> getRawProductSalesForExport(String period, LocalDate date) {
        LocalDate[] range = getDateRange(period, date);
        log.info("Exporting product sales for period '{}' from {} to {}", period, range[0], range[1]);
        return statisticRepository.getTopSellingProducts(range[0], range[1]);
    }
}