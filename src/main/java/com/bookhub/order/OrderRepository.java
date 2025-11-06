package com.bookhub.order;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Integer> {

    @Query("SELECT o FROM Order o " +
            "LEFT JOIN FETCH o.user u " +
            "LEFT JOIN FETCH o.orderDetails od")
    List<Order> findAllWithUserAndDetails();

    @Query("SELECT SUM(o.total) FROM Order o WHERE o.status_order = 'DELIVERED' " +
            "AND (:year IS NULL OR FUNCTION('YEAR', o.date) = :year)")
    Optional<Long> sumTotalDeliveredOrders(@Param("year") Integer year);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.status_order = 'DELIVERED' AND (:year IS NULL OR FUNCTION('YEAR', o.date) = :year)")
    Long countDeliveredOrders(@Param("year") Integer year);

    @Query("SELECT new com.bookhub.order.ProductSaleStats(" +
            "od.product.title, SUM(od.quantity), SUM(od.price_date * od.quantity)) " +
            "FROM OrderDetail od JOIN od.order o " +
            "WHERE o.status_order = 'DELIVERED' " +
            "AND (:year IS NULL OR FUNCTION('YEAR', o.date) = :year) " +
            "AND od.product IS NOT NULL " +
            "GROUP BY od.product.title " +
            "ORDER BY SUM(od.quantity) DESC")
    List<ProductSaleStats> findAllSellingProductsByYear(@Param("year") Integer year);

    
    @Query("SELECT FUNCTION('MONTH', o.date), SUM(o.total) " +
            "FROM Order o " +
            "WHERE o.status_order = 'DELIVERED' " +
            "AND (:year IS NULL OR FUNCTION('YEAR', o.date) = :year) " +
            "GROUP BY FUNCTION('MONTH', o.date) " +
            "ORDER BY FUNCTION('MONTH', o.date) ASC")
    List<Object[]> findMonthlyRevenueAndProfit(@Param("year") Integer year);


    @Query("SELECT o FROM Order o WHERE UPPER(o.status_order) = UPPER(?1)")
    List<Order> findByStatus_orderIgnoreCase(String statusOrder);

    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.orderDetails od WHERE o.id_order = ?1")
    Optional<Order> findByIdWithDetails(Integer orderId);

    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.user u WHERE " +
            "CAST(o.id_order AS string) LIKE CONCAT('%', ?1, '%') OR " +
            "u.username LIKE CONCAT('%', ?1, '%') OR " +
            "o.phone LIKE CONCAT('%', ?1, '%')")
    List<Order> searchOrders(String searchTerm);
}