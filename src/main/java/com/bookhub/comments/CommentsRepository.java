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

    // --- PHƯƠNG THỨC DUYỆT TẤT CẢ (ĐÃ SỬA LỖI NULL STATUS) ---
    @Modifying
    @Transactional
    // Cập nhật status='PUBLISHED' nếu status='PENDING' HOẶC status IS NULL
    @Query("UPDATE Comments c SET c.status = 'PUBLISHED', c.replyDate = CURRENT_TIMESTAMP WHERE c.status = 'PENDING' OR c.status IS NULL")
    int bulkApprovePendingComments();

    // --- PHƯƠNG THỨC FIX SẮP XẾP (NULLS FIRST) ---
    // Sắp xếp: 1. replyDate ASC NULLS FIRST (Chưa phản hồi lên trên), 2. date DESC (Mới nhất)
    @Query(value = "SELECT c FROM Comments c ORDER BY c.replyDate ASC NULLS FIRST, c.date DESC",
            countQuery = "SELECT COUNT(c) FROM Comments c"
    )
    Page<Comments> findAllWithCustomSort(Pageable pageable);

    // Lấy ảnh của sản phẩm
    @Query("SELECT DISTINCT c FROM Comments c " +
            "JOIN FETCH c.user u " +
            "JOIN FETCH c.product p " +
            "LEFT JOIN FETCH p.images")
    List<Comments> findAllWithDetails();
}