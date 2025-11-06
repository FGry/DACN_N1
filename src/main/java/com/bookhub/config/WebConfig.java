package com.bookhub.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {

        // 1. Lấy đường dẫn vật lý (Nơi ảnh được LƯU)
        // Đường dẫn này phải khớp với UPLOAD_DIR trong ProductServiceImpl
        String physicalUploadDir = "src/main/resources/static/images/products/";
        Path uploadPath = Paths.get(physicalUploadDir);
        String absolutePath = uploadPath.toFile().getAbsolutePath();

        // 2. Ánh xạ đường dẫn web (Nơi trình duyệt TÌM ảnh)

        // *** SỬA LỖI Ở ĐÂY ***
        // Đổi "/uploads/products/**" thành "/images/products/**"
        // để khớp với đường dẫn webPath trong ProductServiceImpl
        registry.addResourceHandler("/images/products/**")
                .addResourceLocations("file:/" + absolutePath + "/");

        // *** XÓA CẤU HÌNH GÂY XUNG ĐỘT ***
        // Spring Boot đã tự động cấu hình "classpath:/static/"
        // nên không cần thêm dòng "registry.addResourceHandler("/**")..."
    }
}