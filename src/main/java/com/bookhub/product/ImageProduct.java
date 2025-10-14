package com.bookhub.product;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
@Entity
@Table(name = "Image_products")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ImageProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer id_image_product;

    String image_link;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Products_id_products")
    Product product;
}
