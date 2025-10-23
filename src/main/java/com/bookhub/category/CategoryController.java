package com.bookhub.category;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController // Sử dụng @RestController để tự động chuyển đổi object thành JSON
@RequestMapping("/admin/categories")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // FIX 1: Thêm CORS để cho phép frontend gọi API
public class CategoryController {

    private final CategoryService categoryService; // Giả định CategoryService tồn tại

    // 1. ENDPOINT: Lấy tất cả danh mục (cho Modal quản lý và Dropdown sản phẩm)
    // URL: GET /admin/categories/all
    @GetMapping("/all")
    public List<CategoryDTO> getAllCategories() {
        return categoryService.findAllCategories();
    }

    // 2. ENDPOINT: Lấy chi tiết danh mục để sửa
    // URL: GET /admin/categories/edit/{id}
    @GetMapping("/edit/{id}")
    public CategoryDTO getCategoryForEdit(@PathVariable("id") Integer id) {
        return categoryService.findCategoryById(id);
    }

    // 3. ENDPOINT: Lưu/Cập nhật danh mục
    // URL: POST /admin/categories/save
    // @RequestBody: Bắt buộc để nhận JSON từ AJAX
    @PostMapping("/save")
    public ResponseEntity<String> saveCategory(@RequestBody CategoryDTO categoryDTO) {
        try {
            categoryService.saveCategory(categoryDTO);
            return ResponseEntity.ok("Category saved successfully");
        } catch (Exception e) {
            // Log lỗi chi tiết
            System.err.println("Error saving category: " + e.getMessage());
            // Trả về lỗi 400 (Bad Request) với thông báo chi tiết
            return ResponseEntity.badRequest().body("Error saving category: " + e.getMessage());
        }
    }

    // 4. ENDPOINT: Xóa danh mục
    // URL: DELETE /admin/categories/delete/{id}
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<String> deleteCategory(@PathVariable("id") Integer id) {
        try {
            categoryService.deleteCategoryById(id);
            return ResponseEntity.ok("Category deleted successfully");
        } catch (Exception e) {
            // Log lỗi chi tiết
            System.err.println("Error deleting category: " + e.getMessage());
            // Trả về lỗi 400 (Bad Request) với thông báo chi tiết
            return ResponseEntity.badRequest().body("Error deleting category: " + e.getMessage());
        }
    }
}