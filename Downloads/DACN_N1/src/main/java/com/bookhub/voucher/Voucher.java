package com.bookhub.voucher;

import com.bookhub.order.Order;
import com.bookhub.user.User;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "Vouchers") // Tên bảng trong DB
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Voucher {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer id_vouchers; // Khóa chính

    // Tên trường Java dùng camelCase, @Column dùng để map với tên cột DB (snake_case)
    @Column(name = "code_name", unique = true, nullable = false)
    String codeName;

    // --- CÁC TRƯỜNG TỐI ƯU HÓA THEO DB MỚI ---

    // Loại giảm giá (dùng chung cho cả hai loại)
    @Column(nullable = false, length = 10) // Ví dụ: PERCENT, FIXED
            String discountType;

    // Dùng cho loại PERCENT
    @Column(name = "discount_percent", nullable = true)
    Integer discountPercent;   // Giá trị %

    @Column(name = "max_discount", nullable = true)
    Long maxDiscount;         // Giảm tối đa (BIGINT trong DB)

    // Dùng cho loại FIXED
    @Column(name = "discount_value", nullable = true)
    Long discountValue;       // Số tiền giảm trực tiếp (BIGINT trong DB)

    // Điều kiện áp dụng (dùng chung)
    @Column(name = "min_order_value", nullable = true)
    Long min_order_value;     // Đơn tối thiểu (BIGINT trong DB)

    // --- Các trường khác từ DB mới ---
    @Column(nullable = false) // Nên bắt buộc
            LocalDate start_date;

    @Column(nullable = false) // Nên bắt buộc
    LocalDate end_date;

    @Column(nullable = false) // Số lượng
    Integer quantity;

    // Chuỗi hiển thị (giữ lại vì nó là trường cần thiết cho FE)
    @Column(nullable = false)
    String discountValueStr;

    // --- Quan hệ ---
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Users_id_user") // Khóa ngoại
            User user;

    @OneToMany(mappedBy = "voucher", cascade = CascadeType.ALL)
    List<Order> orders;
}