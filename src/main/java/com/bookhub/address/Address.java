package com.bookhub.address;

import com.bookhub.user.User;
import jakarta.persistence.*;

@Entity
@Table(name = "Address")
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_address")
    private Integer idAddress;

    @Column(name = "address", length = 255)
    private String fullAddressDetail; //Tên của chi tiết địa chỉ (ví dụ: Số 1 Đường A, P. B, Q. C)

    @Column(name = "phone", length = 11)
    private String phone;

    // MỐI QUAN HỆ: Khóa ngoại trỏ đến User
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Users_id_user", nullable = false) // Tên cột khóa ngoại
    private User user;

    // ===== Constructors (Không thay đổi) =====
    public Address() {}

    // ===== Getters & Setters =====
    public Integer getIdAddress() { return idAddress; }
    public void setIdAddress(Integer idAddress) { this.idAddress = idAddress; }
    public String getFullAddressDetail() { return fullAddressDetail; }
    public void setFullAddressDetail(String fullAddressDetail) { this.fullAddressDetail = fullAddressDetail; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
}
