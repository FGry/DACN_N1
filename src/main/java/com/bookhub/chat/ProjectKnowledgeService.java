package com.bookhub.chat;

import com.bookhub.product.ProductDTO;
import com.bookhub.product.ProductService;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProjectKnowledgeService {

    private final ProductService productService;

    public ProjectKnowledgeService(ProductService productService) {
        this.productService = productService;
    }

    /** Tạo kiến thức tổng quan gửi vào Gemini */
    public String buildKnowledge() {
        List<ProductDTO> products = productService.findAllProducts();
        if (products.isEmpty()) {
            return "Hệ thống chưa có sản phẩm nào.";
        }

        String productText = products.stream()
                .map(p -> """
                        • %s
                          - Giá: %s₫
                          - Tác giả: %s
                          - Nhà xuất bản: %s
                          - Năm xuất bản: %s
                          - Ngôn ngữ: %s
                          - Trang: %s
                          - Tồn kho: %s
                          - Giảm giá: %s%%
                          - Mô tả: %s
                          - Danh mục: %s
                          - Ảnh: %s
                        """.formatted(
                        p.getTitle(),
                        p.getPrice(),
                        p.getAuthor(),
                        p.getPublisher(),
                        p.getPublicationYear(),
                        p.getLanguage(),
                        p.getPages(),
                        p.getStockQuantity(),
                        p.getDiscount(),
                        p.getDescription(),
                        p.getCategoryNames(),
                        p.getImageLinks()
                ))
                .collect(Collectors.joining("\n"));

        return """
                DỮ LIỆU DỰ ÁN BOOKHUB:
                =========================

                DANH SÁCH SẢN PHẨM:
                %s

                QUY TẮC TRẢ LỜI:
                - Luôn trả lời ngắn gọn trước, dài khi được hỏi thêm.
                - Khi khách hỏi “sản phẩm này thế nào?” → mô tả + giá + tác giả + đặc điểm nổi bật.
                - Khi khách hỏi “khác gì so với thị trường?” → phân tích dựa trên giá, mô tả, discount.
                - Không bịa thông tin không có trong DB.
                - Nếu không tìm thấy sản phẩm → nói rõ.
                """.formatted(productText);
    }

    /** Lấy N sản phẩm ngẫu nhiên */
    public List<ProductDTO> getRandomProducts(int n) {
        List<ProductDTO> products = productService.findAllProducts();
        if (products == null || products.isEmpty()) {
            return Collections.emptyList();
        }
        List<ProductDTO> shuffled = new java.util.ArrayList<>(products);
        Collections.shuffle(shuffled);
        return shuffled.stream().limit(n).collect(Collectors.toList());
    }

    /** Format danh sách sản phẩm ngắn gọn [CẬP NHẬT] */
    public String formatProducts(List<ProductDTO> products) {
        if (products.isEmpty()) return "Hệ thống chưa có sản phẩm nào.";

        // SỬA LỖI: Dùng "\n\n" (2 lần xuống dòng) thay vì "\n" để ép buộc ngắt dòng hiển thị
        return products.stream()
                .map(p -> String.format("• %s (%s₫)", p.getTitle(), p.getPrice()))
                .collect(Collectors.joining("\n\n"));
    }
}