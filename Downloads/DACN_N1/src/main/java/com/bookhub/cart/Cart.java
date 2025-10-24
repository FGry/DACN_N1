package com.bookhub.cart;

import com.bookhub.product.Product;
import com.bookhub.user.User;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;


@Entity
@Table(name = "Carts")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Cart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer id_cart;

    Integer quantity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Users_id_user")
    User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Products_id_products")
    Product product;
    @Column(length = 255)
            String description;
}