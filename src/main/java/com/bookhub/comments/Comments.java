package com.bookhub.comments;

import com.bookhub.product.Product;
import com.bookhub.user.User;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Entity
@Table(name = "comments")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Comments {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_comment")
    Integer idComment;

    @Column(name = "date")
    LocalDate date;

    @Column(name = "messages", columnDefinition = "TEXT")
    String messages;

    @Column(name = "rate")
    Integer rate;

    @Column(name = "status", length = 10)
    String status;

    @Column(name = "reply", columnDefinition = "TEXT")
    String reply;

    // ánh xạ với user
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    User user;

    // ánh xạ với product
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    Product product;
}