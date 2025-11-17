package com.bookhub.order;

import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
public class OrderDTO {
    private Integer idOrder;
    private String orderCode; // Ví dụ: #DH001

    // Thông tin khách hàng
    private Integer userId;
    private String customerUsername; // Sử dụng username
    private String customerPhone;

    // Thông tin đơn hàng tổng hợp
    private Long totalAmount; // Tổng tiền (trường total)
    private String totalAmountFormatted;
    private Integer totalProducts; // Tổng số sản phẩm
    private String status;
    private LocalDate date;
    private String dateFormatted;

    // Chi tiết (cho modal)
    private String address;
    private String paymentMethod;
    private String note;

    // Chi tiết sản phẩm trong đơn hàng
    private List<OrderDetailDTO> productDetails;
}