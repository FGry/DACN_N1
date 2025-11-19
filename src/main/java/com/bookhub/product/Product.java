package com.bookhub.product;

import com.bookhub.cart.Cart;
import com.bookhub.category.Category;
import com.bookhub.comments.Comments;
import com.bookhub.order.OrderDetail;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "Products")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_products")
    Integer idProducts;

    @Column(nullable = false, length = 255)
    String title;

    @Column(nullable = false)
    Long price;

    String author;
    String publisher;

    @Column(name = "publication_year")
    LocalDate publicationYear;

    Integer pages;

    @Column(name = "stock_quantity")
    Integer stockQuantity;

    String language;
    Integer discount;

    // THÊM TRƯỜNG NÀY
    @Column(columnDefinition = "TEXT")
    String description;

    // --- PHẦN SỬA ĐỔI QUAN HỆ CATEGORY ---
    @ManyToMany(fetch = FetchType.LAZY, targetEntity = Category.class)
    @JoinTable(
            name = "Categories_Products",
            joinColumns = @JoinColumn(name = "Products_id_products"),
            inverseJoinColumns = @JoinColumn(name = "Categories_id_categories")
    )
    List<Category> categories;
    // --- KẾT THÚC SỬA ĐỔI ---

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY, targetEntity = ImageProduct.class)
    List<ImageProduct> images;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY, targetEntity = Comments.class)
    List<Comments> comments;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY, targetEntity = OrderDetail.class)
    List<OrderDetail> orderDetails;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY, targetEntity = Cart.class)
    List<Cart> carts;

}