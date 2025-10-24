package com.bookhub.product;

import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import lombok.AccessLevel;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ImageProductDTO {
    Integer id_image_product;
    String image_link;
}