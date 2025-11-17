package com.bookhub.category;


import java.util.List;

public interface CategoryService {

    // Phương thức lấy tất cả danh mục (cho cả productManage.html và dropdown)
    List<CategoryDTO> findAllCategories();

    // Phương thức lấy chi tiết danh mục theo ID (cho chức năng sửa)
    CategoryDTO findCategoryById(Integer id);

    // Phương thức lưu (thêm mới hoặc cập nhật) danh mục
    void saveCategory(CategoryDTO categoryDTO);

    // Phương thức xóa danh mục
    void deleteCategoryById(Integer id);
}