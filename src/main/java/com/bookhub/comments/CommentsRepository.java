package com.bookhub.comments;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CommentsRepository extends JpaRepository<Comments, Integer> {

    // Lấy comment theo sản phẩm
    List<Comments> findByProduct_IdProducts(Integer idProduct);

    // Lấy comment theo người dùng (sử dụng JPQL để tránh lỗi đặt tên)
    @Query("SELECT c FROM Comments c WHERE c.user.idUser = :userId")
    List<Comments> findByUserId(@Param("userId") Integer userId);

}
