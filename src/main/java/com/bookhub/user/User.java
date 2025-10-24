package com.bookhub.user;

import com.bookhub.address.Address;
import com.bookhub.cart.Cart;
import com.bookhub.comments.Comments;
import com.bookhub.order.Order;
import com.bookhub.voucher.Voucher;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "Users")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_user")
    Integer idUser;

    @Column(nullable = false, length = 50)
    String username;

    @Column(nullable = false, length = 50)
    String password;

    @Column(length = 255)
    String email;

    @Column(nullable = false, length = 10)
    String gender;

    @Column(nullable = false, length = 11)
    String phone;

    @Column(nullable = false, length = 10)
    String roles;

    @Column(nullable = false)
    Boolean isLocked = false;

    @Column(nullable = false)
    LocalDate updateDate;

    @Column(nullable = false)
    LocalDate createDate;

    // ----------------- Quan hệ 1-n -----------------
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    List<Address> addresses;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    List<Cart> carts;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    List<Comments> comments;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    List<Order> orders;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    List<Voucher> vouchers;
}