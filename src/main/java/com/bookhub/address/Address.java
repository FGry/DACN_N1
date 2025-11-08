package com.bookhub.address;

import com.bookhub.user.User;
import com.fasterxml.jackson.annotation.JsonIgnore; // Import cần thiết cho @JsonIgnore
import jakarta.persistence.*;

@Entity
@Table(name = "Address")
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_address")
    private Integer idAddress;

    @Column(name = "address", length = 255)
    private String fullAddressDetail;

    @Column(name = "phone", length = 11)
    private String phone;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Users_id_user", nullable = false)
    @JsonIgnore
    private User user;
    public Address() {}

    public Integer getIdAddress() { return idAddress; }
    public void setIdAddress(Integer idAddress) { this.idAddress = idAddress; }
    public String getFullAddressDetail() { return fullAddressDetail; }
    public void setFullAddressDetail(String fullAddressDetail) { this.fullAddressDetail = fullAddressDetail; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
}