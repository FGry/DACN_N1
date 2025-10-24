package com.bookhub; // Ghi chú: Thay đổi package này cho đúng với dự án của bạn

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    // Đường dẫn thư mục gốc bạn đã chọn ở ServiceImpl (loại bỏ phần /products)
    // Ví dụ cho Windows:
    private String uploadPath = "file:C:/bookhub_uploads/";



    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry
                .addResourceHandler("/uploads/**") // URL mà trình duyệt sẽ gọi
                .addResourceLocations(uploadPath);     // Nơi Spring Boot sẽ tìm file trên ổ cứng
    }
}