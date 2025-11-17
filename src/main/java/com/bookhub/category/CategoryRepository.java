package com.bookhub.category;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Integer> {

    /**
     * Tìm kiếm danh mục theo tên (name), không phân biệt hoa thường.
     * @param name Tên danh mục
     * @return Optional chứa Category nếu tìm thấy
     */
    Optional<Category> findByNameIgnoreCase(String name);
}