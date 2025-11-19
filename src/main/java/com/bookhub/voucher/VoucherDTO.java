package com.bookhub.voucher;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

// DTO này có các trường khớp 1:1 với mảng JS trong voucher.html
@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter

public class VoucherDTO {
    private Integer id;
    private String code;
    private String title;           // <--- THÊM
    private String description;     // <--- THÊM
    private String discountValue;   // Ví dụ: "50%" hoặc "100K" (dạng string hiển thị)
    private String discountType;    // Ví dụ: "Giảm giá" (dạng string hiển thị)
    private String category;        // Ví dụ: "Khách mới"
    private String type;            // Ví dụ: "newbie" (Dùng để lọc trên FE)
    private String minOrder;        // Giá trị đơn tối thiểu (Long)
    private String endDate;         // Chuỗi "YYYY-MM-DD" (cho tính toán JS)
    private String status;          // Ví dụ: "active"
    private String requirements;    // <--- THÊM
    private Integer quantity;       // <--- THÊM

    // THÊM CÁC THÔNG SỐ ĐỂ TÍNH TOÁN VÀ HIỂN THỊ
    private String endDateDisplay;  // Ngày hết hạn định dạng đẹp (vd: 31/12/2024)
    private String minOrderDisplay; // Đơn tối thiểu định dạng đẹp (vd: 200.000đ)
}