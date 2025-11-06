package com.bookhub.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final String STATIC_UPLOAD_ROOT = "src/main/resources/static/images/products/";

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Lấy đường dẫn tuyệt đối cho thư mục vật lý bạn đang dùng
        String absolutePath = Paths.get("src/main/resources/static/images/products/").toAbsolutePath().toString();

        // SỬA TÊN RESOURCE HANDLER TỪ /images/products/** THÀNH /uploads/products/**
        registry.addResourceHandler("/uploads/products/**")
                // Ánh xạ tới thư mục vật lý của bạn
                .addResourceLocations("file:" + absolutePath + "/");

        // Giữ lại cấu hình mặc định cho các tài nguyên tĩnh
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/");
    }
}