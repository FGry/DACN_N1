package com.bookhub.statistics;

import com.bookhub.order.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface StatisticRepository extends JpaRepository<Order, Integer> {

    /**
     * Calculate overall statistics: total revenue, total invoices, average order value
     */
    @Query("SELECT " +
            "  COALESCE(SUM(o.total), 0) AS totalRevenue, " +
            "  COUNT(o.id_order) AS totalInvoices, " +
            "  COALESCE(AVG(o.total), 0) AS averageOrderValue " +
            "FROM Order o " +
            "WHERE o.status_order = 'DELIVERED' AND o.date BETWEEN ?1 AND ?2")
    List<Object[]> calculateOverallStats(LocalDate startDate, LocalDate endDate);

    /**
     * Get top selling products with statistics
     * Returns: Product ID, Product Title, Total Quantity Sold, Total Revenue
     */
    @Query("SELECT " +
            "  od.product.id, " +
            "  od.product.title, " +
            "  SUM(od.quantity), " +
            "  SUM(od.total) " +
            "FROM OrderDetail od " +
            "WHERE od.order.status_order = 'DELIVERED' AND od.order.date BETWEEN ?1 AND ?2 " +
            "GROUP BY od.product.id, od.product.title " +
            "ORDER BY SUM(od.quantity) DESC")
    List<Object[]> getTopSellingProducts(LocalDate startDate, LocalDate endDate);
}