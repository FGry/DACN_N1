package com.bookhub.category;

import com.fasterxml.jackson.annotation.JsonProperty; // FIX 2a: Import cần thiết
import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * Data Transfer Object (DTO) cho Category.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CategoryDTO {

    // FIX 2b: Đảm bảo tên trường khớp khi nhận JSON
    @JsonProperty("idCategories")
    Integer idCategories;

    String name;

    String description;
}