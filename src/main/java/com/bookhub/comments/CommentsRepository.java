package com.bookhub.comments;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface CommentsRepository extends JpaRepository<Comments, Integer> {

    // 1. Lấy comment theo sản phẩm (Sử dụng JPA Finder Method)
    // ⚠️ Giả định trường Product ID trong Entity Comments là 'product.idProducts'
    List<Comments> findByProduct_IdProducts(Integer idProduct);

    // 2. Lấy comment theo người dùng (Sử dụng JPQL)
    // ⚠️ Giả định trường User ID trong Entity Comments là 'user.idUser'
    @Query("SELECT c FROM Comments c WHERE c.user.idUser = :userId")
    List<Comments> findByUserId(@Param("userId") Integer userId);

    // 3. --- PHƯƠNG THỨC DUYỆT TẤT CẢ (Đã sửa lỗi NULL STATUS) ---
    /**
     * Cập nhật status='PUBLISHED' nếu status='PENDING' HOẶC status IS NULL.
     * Sử dụng CURRENT_TIMESTAMP để ghi lại thời điểm duyệt.
     */
    @Modifying
    @Transactional
    @Query("UPDATE Comments c SET c.status = 'PUBLISHED', c.replyDate = CURRENT_TIMESTAMP " +
            "WHERE c.status = 'PENDING' OR c.status IS NULL")
    int bulkApprovePendingComments();

    // 4. --- PHƯƠNG THỨC SẮP XẾP TÙY CHỈNH (NULLS FIRST) ---
    /**
     * Sắp xếp: 1. replyDate ASC NULLS FIRST (Chưa phản hồi lên trên), 2. date DESC (Mới nhất)
     * Thường dùng cho trang Admin.
     * ⚠️ Lưu ý: 'NULLS FIRST' không được hỗ trợ trong tất cả các phiên bản JPQL,
     * nhưng được hỗ trợ bởi Hibernate/Spring Data JPA trong nhiều trường hợp.
     */
    @Query(value = "SELECT c FROM Comments c ORDER BY c.replyDate ASC NULLS FIRST, c.date DESC",
            countQuery = "SELECT COUNT(c) FROM Comments c"
    )
    Page<Comments> findAllWithCustomSort(Pageable pageable);

    // 5. Lấy tất cả Comments/Reviews với chi tiết User và Product Image (Tránh N+1)
    /**
     * Lấy các bình luận, đồng thời JOIN FETCH các mối quan hệ (User, Product, Product.Images)
     * để tải dữ liệu đầy đủ trong một truy vấn duy nhất.
     */
    @Query("SELECT DISTINCT c FROM Comments c " +
            "JOIN FETCH c.user u " +
            "JOIN FETCH c.product p " +
            "LEFT JOIN FETCH p.images")
    List<Comments> findAllWithDetails();
}