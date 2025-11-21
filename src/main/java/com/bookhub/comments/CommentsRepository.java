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

    // Lấy comment theo sản phẩm
    List<Comments> findByProduct_IdProducts(Integer idProduct);

    // Lấy comment theo người dùng
    @Query("SELECT c FROM Comments c WHERE c.user.idUser = :userId")
    List<Comments> findByUserId(@Param("userId") Integer userId);

    @Modifying
    @Transactional
    @Query("UPDATE Comments c SET c.status = 'PUBLISHED', c.replyDate = CURRENT_TIMESTAMP WHERE c.status = 'PENDING' OR c.status IS NULL")
    int bulkApprovePendingComments();

    @Query(value = "SELECT c FROM Comments c ORDER BY c.replyDate ASC NULLS FIRST, c.date DESC",
            countQuery = "SELECT COUNT(c) FROM Comments c"
    )
    Page<Comments> findAllWithCustomSort(Pageable pageable);

    @Query("SELECT DISTINCT c FROM Comments c " +
            "JOIN FETCH c.user u " +
            "JOIN FETCH c.product p " +
            "LEFT JOIN FETCH p.images")
    List<Comments> findAllWithDetails();

    // --- MỚI: TÍNH TRUNG BÌNH SAO CỦA SẢN PHẨM ---
    // Chỉ lấy những đánh giá đã duyệt (PUBLISHED) và có số sao > 0
    @Query("SELECT AVG(c.rate) FROM Comments c WHERE c.product.idProducts = :productId AND c.status = 'PUBLISHED' AND c.rate > 0")
    Double getAverageRatingByProductId(@Param("productId") Integer productId);
}