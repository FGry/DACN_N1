package com.bookhub.product;

import com.bookhub.category.CategoryRepository;
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
    private final CategoryRepository categoryRepository;

    /**
     * Xử lý trang chủ (/)
     */
    @GetMapping("/")
    public String showHomePage(Model model) {
        model.addAttribute("allCategories", categoryRepository.findAll());
        List<ProductDTO> products = productService.findAllProducts();
        model.addAttribute("products", products);
        return "mainInterface";
    }

    /**
     * Xử lý trang Sản phẩm (/products)
     */
    @GetMapping("/products")
    public String listPublicProducts(
            Model model,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "categoryId", required = false) Integer categoryId
    ) {
        List<ProductDTO> products;
        String title;

        if (categoryId != null) {
            products = productService.findProductsByCategoryId(categoryId);
            try {
                title = "Sản phẩm: " + categoryRepository.findById(categoryId)
                        .map(cat -> cat.getName())
                        .orElse("Không rõ");
            } catch (Exception e) {
                title = "Sản phẩm theo danh mục";
            }
        }
        else if (keyword != null && !keyword.trim().isEmpty()) {
            products = productService.searchProducts(keyword.trim());
            title = "Kết quả tìm kiếm cho '" + keyword + "'";
        } else {
            products = productService.findAllProducts();
            title = "Tất cả sản phẩm của BookStore";
        }

        model.addAttribute("products", products);
        model.addAttribute("pageTitle", title);
        model.addAttribute("allCategories", categoryRepository.findAll());
        return "user/product";
    }

    /**
     * Xử lý trang Chi tiết sản phẩm (/product_detail/{id})
     */
    @GetMapping("/product_detail/{id}")
    public String viewProductDetail(@PathVariable("id") Integer id, Model model) {
        try {
            ProductDTO product = productService.findProductById(id);
            model.addAttribute("product", product);

            List<CommentsDTO> publishedComments = commentsService.getCommentsByProduct(id);
            model.addAttribute("publishedComments", publishedComments);

            CommentsDTO newComment = new CommentsDTO();
            newComment.setProductId(id);
            model.addAttribute("newComment", newComment);

            // ⭐ BẮT BUỘC PHẢI THÊM DÒNG NÀY ĐỂ MENU HOẠT ĐỘNG ⭐
            model.addAttribute("allCategories", categoryRepository.findAll());

            return "user/product_detail";

        } catch (RuntimeException e) {
            model.addAttribute("errorMessage", "Sản phẩm không tồn tại.");
            // Cũng cần allCategories cho trang lỗi để navbar hiển thị
            model.addAttribute("allCategories", categoryRepository.findAll());
            return "error/404"; // Giả định bạn có trang 404
        }
    }

    /**
     * Xử lý trang Giới thiệu (/about)
     */
    @GetMapping("/about")
    public String showAboutPage(Model model) {
        model.addAttribute("allCategories", categoryRepository.findAll());
        return "user/about";
    }

    /**
     * Xử lý trang Liên hệ (/contact)
     */
    @GetMapping("/contact")
    public String showContactPage(Model model) {
        model.addAttribute("allCategories", categoryRepository.findAll());
        return "user/contact";
    }

    /**
     * Xử lý trang Voucher (/voucher)
     */
    @GetMapping("/voucher")
    public String showVoucherPage(Model model) {
        model.addAttribute("allCategories", categoryRepository.findAll());
        return "user/voucher";
    }
}