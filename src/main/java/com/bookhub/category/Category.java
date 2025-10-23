package com.bookhub.category;

import com.bookhub.product.Product;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import java.util.List;

@Entity
@Table(name = "Categories")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer id_categories;

    @Column(nullable = false, length = 40)
    String name;

    String description;

    // Sửa lỗi: Thêm targetEntity = Product.class
    @ManyToMany(targetEntity = Product.class)
    @JoinTable(
            name = "Categories_Products",
            joinColumns = @JoinColumn(name = "Categories_id_categories"),
            inverseJoinColumns = @JoinColumn(name = "Products_id_products")
    )
    List<Product> products;
}