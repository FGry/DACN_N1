package com.bookhub.product;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Integer> {

    // Kiểm tra trùng tên (không phân biệt hoa thường)
    Optional<Product> findByTitleIgnoreCase(String title);

    // Dùng cho tìm kiếm theo từ khóa (Ví dụ: theo tiêu đề)
    List<Product> findByTitleContainingIgnoreCase(String title);

    // LỌC THEO DANH MỤC (Many-to-Many) - KHẮC PHỤC LỖI CANNOT FIND SYMBOL
    @Query("SELECT p FROM Product p JOIN p.categories c WHERE c.id_categories = :categoryId")
    List<Product> findByCategoriesId_categories(@Param("categoryId") Integer categoryId);
}