package com.bookhub.order;

import com.bookhub.user.User;
import com.bookhub.voucher.Voucher;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.util.List;

// Gửi file code hoàn chỉnh: Order.java
@Entity
@Table(name = "Orders")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer id_order;

    String address;
    LocalDate date;
    String phone;
    String status_order;
    Long total;
    String payment_method;
    String note;

    // === THÊM CÁC TRƯỜNG VOUCHER ĐỂ LƯU CHI TIẾT ===
    @Column(name = "voucher_code")
    String voucherCode; // Lưu mã code đã dùng (cho báo cáo nếu voucher bị xóa)

    @Column(name = "discount_amount")
    Long discountAmount; // Số tiền giảm giá chính thức
    // ===============================================

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Users_id_user")
    User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Vouchers_id_vouchers")
    Voucher voucher; // Link tới Entity Voucher

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    List<OrderDetail> orderDetails;
}