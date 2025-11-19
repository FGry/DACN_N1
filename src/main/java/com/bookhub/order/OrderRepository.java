package com.bookhub.order;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Integer> {

    // Phương thức truy vấn tất cả đơn hàng với thông tin User và chi tiết
    @Query("SELECT o FROM Order o " +
            "LEFT JOIN FETCH o.user u " +
            "LEFT JOIN FETCH o.orderDetails od")
    List<Order> findAllWithUserAndDetails();

    // Phương thức tính tổng doanh thu (đơn hàng đã giao)
    @Query("SELECT SUM(o.total) FROM Order o WHERE o.status_order = 'DELIVERED' " +
            "AND (:year IS NULL OR FUNCTION('YEAR', o.date) = :year)")
    Optional<Long> sumTotalDeliveredOrders(@Param("year") Integer year);

    // Phương thức đếm số lượng đơn hàng đã giao
    @Query("SELECT COUNT(o) FROM Order o WHERE o.status_order = 'DELIVERED' AND (:year IS NULL OR FUNCTION('YEAR', o.date) = :year)")
    Long countDeliveredOrders(@Param("year") Integer year);

    // Phương thức lấy top sản phẩm bán chạy theo năm
    @Query("SELECT od.product.title, SUM(od.quantity), SUM(od.total) " +
            "FROM OrderDetail od JOIN od.order o " +
            "WHERE o.status_order = 'DELIVERED' " +
            "AND (:year IS NULL OR FUNCTION('YEAR', o.date) = :year) " +
            "AND od.product IS NOT NULL " +
            "GROUP BY od.product.title " +
            "ORDER BY SUM(od.quantity) DESC")
    List<Object[]> findAllSellingProductsByYear(@Param("year") Integer year);

    // Phương thức lấy doanh thu hàng tháng
    @Query("SELECT FUNCTION('MONTH', o.date), SUM(o.total) " +
            "FROM Order o " +
            "WHERE o.status_order = 'DELIVERED' " +
            "AND (:year IS NULL OR FUNCTION('YEAR', o.date) = :year) " +
            "GROUP BY FUNCTION('MONTH', o.date) " +
            "ORDER BY FUNCTION('MONTH', o.date) ASC")
    List<Object[]> findMonthlyRevenueAndProfit(@Param("year") Integer year);

    // [PHƯƠNG THỨC MỚI] Lấy tổng doanh thu hàng năm cho tất cả các năm đã hoạt động
    @Query("SELECT FUNCTION('YEAR', o.date), SUM(o.total) " +
            "FROM Order o WHERE o.status_order = 'DELIVERED' " +
            "GROUP BY FUNCTION('YEAR', o.date) ORDER BY FUNCTION('YEAR', o.date) ASC")
    List<Object[]> findRevenueByYear(); // Không cần tham số year

    // Phương thức tìm đơn hàng theo trạng thái
    @Query("SELECT o FROM Order o WHERE UPPER(o.status_order) = UPPER(?1)")
    List<Order> findByStatus_orderIgnoreCase(String statusOrder);

    // Phương thức tìm đơn hàng theo ID kèm chi tiết
    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.orderDetails od WHERE o.id_order = ?1")
    Optional<Order> findByIdWithDetails(Integer orderId);

    // Phương thức tìm kiếm đơn hàng theo mã, tên người dùng, hoặc SĐT
    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.user u WHERE " +
            "CAST(o.id_order AS string) LIKE CONCAT('%', ?1, '%') OR " +
            "u.username LIKE CONCAT('%', ?1, '%') OR " +
            "o.phone LIKE CONCAT('%', ?1, '%')")
    List<Order> searchOrders(String searchTerm);

    List<Order> findByUser_IdUser(Integer userId);

    @Query("SELECT COUNT(o.id_order) " +
            "FROM Order o JOIN o.orderDetails od " +
            "WHERE o.user.idUser = :userId " +
            "AND od.product.idProducts = :productId " +
            "AND o.status_order = 'DELIVERED'")
    Long countDeliveredPurchases(@Param("userId") Integer userId, @Param("productId") Integer productId);
}