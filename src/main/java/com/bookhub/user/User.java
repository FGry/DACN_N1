package com.bookhub.user;

import com.bookhub.address.Address;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "Users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_user")
    private Integer idUser;

    @Column(name = "username", length = 50, nullable = false)
    private String username;

    // 🚨 SỬA LỖI: Tăng độ dài cột 'password' lên 128 (BCrypt cần 60 ký tự)
    @Column(name = "password", length = 128, nullable = false)
    private String password;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "gender", length = 10, nullable = false)
    private String gender;

    @Column(name = "phone", length = 11, nullable = false)
    private String phone;

    @Column(name = "roles", length = 10, nullable = false)
    private String roles;

    @Column(name = "update_date", nullable = false)
    private LocalDate updateDate;

    @Column(name = "create_date", nullable = false)
    private LocalDate createDate;

    // THÊM TRƯỜNG MỚI: isLocked
    @Column(name = "is_locked", nullable = false)
    private Boolean isLocked = false;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    private List<Address> addresses;


    public User() {}

    public String getFirstAddress() {
        if (addresses != null && !addresses.isEmpty()) {
            return addresses.get(0).getFullAddressDetail();
        }
        return null;
    }

    // ===== Getters & Setters =====
    public Integer getIdUser() { return idUser; }
    public void setIdUser(Integer idUser) { this.idUser = idUser; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getRoles() { return roles; }
    public void setRoles(String roles) { this.roles = roles; }
    public LocalDate getUpdateDate() { return updateDate; }
    public void setUpdateDate(LocalDate updateDate) { this.updateDate = updateDate; }
    public LocalDate getCreateDate() { return createDate; }
    public void setCreateDate(LocalDate createDate) { this.createDate = createDate; }
    public List<Address> getAddresses() { return addresses; }
    public void setAddresses(List<Address> addresses) { this.addresses = addresses; }

    // ===== Getters & Setters MỚI cho isLocked =====
    public Boolean getIsLocked() { return isLocked; }
    public void setIsLocked(Boolean isLocked) { this.isLocked = isLocked; }
    // ===============================================
}