package com.bookhub.product;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class PublicProductController {

    private final ProductService productService;

    // 1. HIỂN THỊ DANH SÁCH SẢN PHẨM (Giữ nguyên)
    // URL: http://localhost:8080/products
    @GetMapping("/products")
    public String listPublicProducts(Model model) {
        List<ProductDTO> products = productService.findAllProducts();
        model.addAttribute("products", products);

        // Trả về view: src/main/resources/templates/user/product.html
        return "user/product";
    }

    // 2. HIỂN THỊ CHI TIẾT SẢN PHẨM (ĐÃ SỬA URL)
    // URL mới: http://localhost:8080/product_detail/1
    @GetMapping("/product_detail/{id}")
    public String viewProductDetail(@PathVariable("id") Integer id, Model model) {
        try {
            ProductDTO product = productService.findProductById(id);
            model.addAttribute("product", product);

            // Trả về view: src/main/resources/templates/user/product-detail.html
            return "user/product_detail";

        } catch (RuntimeException e) {
            // Xử lý khi không tìm thấy sản phẩm
            model.addAttribute("errorMessage", "Sản phẩm không tồn tại.");
            // Giả sử bạn có trang error/404.html
            return "error/404";
        }
    }
}