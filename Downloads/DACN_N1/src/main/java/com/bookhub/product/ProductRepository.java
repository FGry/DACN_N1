package com.bookhub.product;

import com.bookhub.product.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Integer> {

    Optional<Product> findByTitleIgnoreCase(String title);

    /**
     * Tìm kiếm sản phẩm theo từ khóa trong tiêu đề, mô tả HOẶC tác giả.
     */
    @Query("SELECT p FROM Product p WHERE LOWER(p.title) LIKE %:keyword% OR LOWER(p.description) LIKE %:keyword% OR LOWER(p.author) LIKE %:keyword%")
    List<Product> searchByKeyword(@Param("keyword") String keyword);

    /**
     * Tìm sản phẩm theo Category ID.
     */
    @Query("SELECT p FROM Product p JOIN p.categories c WHERE c.id_categories = :categoryId")
    List<Product> findByCategoryId(@Param("categoryId") Integer categoryId);
}