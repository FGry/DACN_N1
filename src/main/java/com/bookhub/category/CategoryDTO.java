package com.bookhub.category;

import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * Data Transfer Object (DTO) cho Category.
 * Dùng để truyền dữ liệu giữa các tầng trong ứng dụng
 * mà không cần mang theo toàn bộ quan hệ Entity (như List<Product>).
 */
@Data // Bao gồm @Getter, @Setter, @ToString, @EqualsAndHashCode
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CategoryDTO {

    Integer idCategories; // Ánh xạ từ id_categories

    String name;

    String description;

    // Không cần List<Product> trong DTO này để tránh lỗi vòng lặp/lấy dữ liệu quá mức.
}