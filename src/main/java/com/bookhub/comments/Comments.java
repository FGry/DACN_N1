package com.bookhub.comments;

import com.bookhub.product.Product;
import com.bookhub.user.User;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Entity
@Table(name = "comments") // nên để tên bảng chữ thường cho thống nhất
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Comments {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_comment") // tên cột trong DB là id_comment
    Integer idComment;

    @Column(name = "date")
    LocalDate date;

    @Column(name = "messages", columnDefinition = "TEXT")
    String messages;

    @Column(name = "rate")
    Integer rate;

    // ánh xạ với user
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id") // nên đổi cho thống nhất
            User user;

    // ánh xạ với product
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    Product product;
}
