package com.bookhub.product;

import com.bookhub.category.CategoryDTO;
import com.bookhub.comments.CommentsDTO;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.util.List;

@Data // Bao gồm @Getter, @Setter, @ToString, @EqualsAndHashCode
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProductDTO {

    Integer idProducts;
    String title;
    Long price;
    String author;
    String publisher;
    LocalDate publicationYear;
    Integer pages;
    Integer stockQuantity;
    String language;
    Integer discount;

    List<ImageProductDTO> images;
    String categoryName;
    String detailDescription;
    Double averageRating;
    Long totalReviews;


    List<CommentsDTO> comments;
    List<CategoryDTO> categories;
}