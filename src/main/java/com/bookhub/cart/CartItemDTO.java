package com.bookhub.cart; // Hoặc package DTO của bạn

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO này dùng để ánh xạ (map) dữ liệu JSON
 * từ sessionStorage/localStorage của trình duyệt.
 */
@Data
@NoArgsConstructor
public class CartItemDTO {
    // Tên các trường này PHẢI KHỚP với tên trong JavaScript
    private Integer idProducts;
    private Integer quantity;
    private Long price;
    private String title;
    private List<String> imageLinks;
}