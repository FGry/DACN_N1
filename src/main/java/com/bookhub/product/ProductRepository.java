package com.bookhub.product;

import com.bookhub.product.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional; // <-- THÊM IMPORT

@Repository
public interface ProductRepository extends JpaRepository<Product, Integer> {

    // THÊM PHƯƠNG THỨC NÀY:
    /**
     * Tìm kiếm sản phẩm theo tiêu đề (title), không phân biệt hoa thường.
     * @param title Tiêu đề sản phẩm
     * @return Optional chứa Product nếu tìm thấy
     */
    Optional<Product> findByTitleIgnoreCase(String title);
}