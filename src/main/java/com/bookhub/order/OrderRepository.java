package com.bookhub.order;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Integer> {

    // ĐÃ SỬA LỖI: Thay thế phương thức tự động bằng @Query để tránh lỗi phân tích tên thuộc tính có gạch dưới.
    @Query("SELECT o FROM Order o WHERE UPPER(o.status_order) = UPPER(?1)")
    List<Order> findByStatus_orderIgnoreCase(String statusOrder);

    // Tìm đơn hàng bằng ID và tải luôn thông tin chi tiết (JOIN FETCH)
    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.orderDetails od WHERE o.id_order = ?1")
    Optional<Order> findByIdWithDetails(Integer orderId);

    /**
     * Tìm kiếm đơn hàng (theo ID, username hoặc SĐT)
     */
    @Query("SELECT o FROM Order o WHERE " +
            "CAST(o.id_order AS string) LIKE CONCAT('%', ?1, '%') OR " +
            "o.user.username LIKE CONCAT('%', ?1, '%') OR " +
            "o.phone LIKE CONCAT('%', ?1, '%')")
    List<Order> searchOrders(String searchTerm);
}