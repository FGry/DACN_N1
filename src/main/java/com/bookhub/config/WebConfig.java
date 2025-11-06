package com.bookhub.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private String uploadPath = "file:D:/DoAnNhom1/DACN_N1/src/main/resources/static/images/";

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry
                .addResourceHandler("/uploads/**") // URL mà trình duyệt sẽ gọi
                .addResourceLocations(uploadPath);     // Nơi Spring Boot sẽ tìm file trên ổ cứng
    }

}
