package com.bookhub.product;

import com.bookhub.category.Category;
import com.bookhub.category.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final CategoryRepository categoryRepository;

    // 1. HIỂN THỊ TRANG CHÍNH (Đã sửa)
    @GetMapping
    public String listProducts(Model model) {
        // PHẦN QUAN TRỌNG:
        // Chỉ thêm "product" mới (rỗng) nếu model chưa có
        // (để không ghi đè dữ liệu cũ khi F5)
        if (!model.containsAttribute("product")) {
            model.addAttribute("product", new ProductDTO());
        }

        model.addAttribute("products", productService.findAllProducts());
        model.addAttribute("allCategories", categoryRepository.findAll());
        return "admin/productManage";
    }

    // 2. LẤY DỮ LIỆU SẢN PHẨM ĐỂ SỬA (Giữ nguyên)
    @GetMapping("/edit/{id}")
    @ResponseBody
    public ProductDTO showEditForm(@PathVariable("id") Integer id) {
        return productService.findProductById(id);
    }

    // 3. XỬ LÝ LƯU SẢN PHẨM (Đã sửa)
    @PostMapping("/save")
    public String saveProduct(@ModelAttribute("product") ProductDTO productDTO,
                              RedirectAttributes redirectAttributes,
                              Model model) { // Thêm Model
        try {
            productService.saveProduct(productDTO);
            // Nếu thành công -> Chuyển hướng
            redirectAttributes.addFlashAttribute("successMessage", "Lưu sản phẩm thành công!");
            return "redirect:/admin/products";

        } catch (RuntimeException e) {

            // KIỂM TRA LỖI TRÙNG TÊN
            if (e.getMessage() != null && e.getMessage().contains("Một sản phẩm với tên")) {

                // === PHẦN QUAN TRỌNG NHẤT ===

                // 1. Gửi lại thông báo lỗi
                model.addAttribute("errorMessage", "Sản phẩm đã tồn tại, vui lòng nhập tên sản phẩm khác");

                // 2. Gửi lại DANH SÁCH (cho bảng)
                model.addAttribute("products", productService.findAllProducts());

                // 3. Gửi lại DANH MỤC (cho dropdown)
                model.addAttribute("allCategories", categoryRepository.findAll());

                // 4. Gửi lại DTO (đã có sẵn trong model nhờ @ModelAttribute)
                model.addAttribute("product", productDTO);

                // 5. Gửi "CỜ" ĐỂ MỞ MODAL
                model.addAttribute("openFormModal", true);

                // 6. Trả về view, KHÔNG redirect
                return "admin/productManage";

            } else {
                // Nếu là lỗi khác (ví dụ: lỗi file)
                redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
                return "redirect:/admin/products";
            }
        }
    }


    // 4. XÓA SẢN PHẨM (Giữ nguyên)
    @GetMapping("/delete/{id}")
    public String deleteProduct(@PathVariable("id") Integer id, RedirectAttributes redirectAttributes) {
        try {
            productService.deleteProductById(id);
            redirectAttributes.addFlashAttribute("successMessage", "Xóa sản phẩm ID " + id + " thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi xóa sản phẩm: " + e.getMessage());
        }
        return "redirect:/admin/products";
    }
}