package com.bookhub.product;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductDTO {
    private Integer idProducts;
    private String title;
    private Long price;
    private String author;
    private String publisher;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate publicationYear;

    private Integer pages;
    private Integer stockQuantity;
    private String language;
    private Integer discount;

    private List<String> categoryNames;
    private List<Integer> selectedCategoryIds;

    private List<MultipartFile> imageFiles;
    private List<String> imageLinks;

    private String description;

    // ================== THÊM TRƯỜNG MỚI ==================
    private Double averageRating; // Số sao trung bình
    private Integer soldCount;     // Số lượng đã bán
    private Long discountedPrice;  // Giá sau khi giảm
    // ======================================================
}