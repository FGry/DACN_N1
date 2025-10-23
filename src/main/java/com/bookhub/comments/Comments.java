package com.bookhub.comments;

import com.bookhub.product.Product;
import com.bookhub.user.User;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.time.LocalDateTime;

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
    LocalDate date; // Thời điểm bình luận/đánh giá được tạo

    @Column(name = "messages", columnDefinition = "TEXT")
    String messages;

    @Column(name = "rate")
    Integer rate;

    @Column(name = "status", length = 10)
    String status;

    @Column(name = "type", length = 10, nullable = false)
    String type;

    @Column(name = "purchase_verified", nullable = false)
    Boolean purchaseVerified = false;

    @Column(name = "reply", columnDefinition = "TEXT")
    String reply;

    @Column(name = "reply_date")
    LocalDateTime replyDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    Product product;
}