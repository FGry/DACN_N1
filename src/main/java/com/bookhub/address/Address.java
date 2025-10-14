package com.bookhub.address;

import com.bookhub.user.User;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Table(name = "Addresses")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer id_address;

    @Column(length = 255)
    String address;

    @Column(length = 11)
    String phone;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Users_id_user")
    User user;
}
