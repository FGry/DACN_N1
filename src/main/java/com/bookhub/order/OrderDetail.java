package com.bookhub.order;

import com.bookhub.product.Product;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Table(name = "Order_details")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OrderDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer id_order_detail;

    Integer discount;
    Long price_date;
    Long quantity;
    Long total;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Orders_id_order")
    Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Products_id_products")
    Product product;
}
