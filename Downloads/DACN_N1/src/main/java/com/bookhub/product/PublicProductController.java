package com.bookhub.product;

import com.bookhub.comments.CommentsDTO;
import com.bookhub.comments.CommentsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class PublicProductController {

    private final ProductService productService;
    private final CommentsService commentsService;

    // HIỂN THỊ DANH SÁCH SẢN PHẨM (Xử lý Tìm kiếm và Lọc)
    @GetMapping("/products")
    public String listPublicProducts(
            Model model,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "categoryId", required = false) Integer categoryId
    ) {
        List<ProductDTO> products;
        String title;

        if (categoryId != null) {
            // LỌC THEO DANH MỤC
            products = productService.findProductsByCategoryId(categoryId);
            title = "Sản phẩm theo danh mục";
        }
        else if (keyword != null && !keyword.trim().isEmpty()) {
            // Thực hiện tìm kiếm (theo title, description, author)
            products = productService.searchProducts(keyword.trim());
            // XÓA DÒNG KẾT QUẢ CHI TIẾT
            title = "Danh sách Sản phẩm";
        } else {
            // Hiển thị tất cả
            products = productService.findAllProducts();
            title = "Tất cả sản phẩm của BookStore";
        }

        model.addAttribute("products", products);
        model.addAttribute("pageTitle", title);

        return "user/product";
    }

    // HIỂN THỊ CHI TIẾT SẢN PHẨM (Giữ nguyên)
    @GetMapping("/product_detail/{id}")
    public String viewProductDetail(@PathVariable("id") Integer id, Model model) {
        try {
            ProductDTO product = productService.findProductById(id);
            model.addAttribute("product", product);

            List<CommentsDTO> publishedComments = commentsService.getCommentsByProduct(id);
            model.addAttribute("publishedComments", publishedComments);

            CommentsDTO newComment = new CommentsDTO();
            newComment.setProductId(id);
            newComment.setUserId(1);
            model.addAttribute("newComment", newComment);

            return "user/product_detail";

        } catch (RuntimeException e) {
            model.addAttribute("errorMessage", "Sản phẩm không tồn tại.");
            return "error/404";
        }
    }
}