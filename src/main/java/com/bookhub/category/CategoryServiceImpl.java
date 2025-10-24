package com.bookhub.category;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;

    // Helper: Chuyển đổi Entity sang DTO
    private CategoryDTO convertToDTO(Category category) {
        if (category == null) return null;
        return CategoryDTO.builder()
                .idCategories(category.getId_categories())
                .name(category.getName())
                .description(category.getDescription())
                .build();
    }

    // Helper: Chuyển đổi DTO sang Entity
    private Category convertToEntity(CategoryDTO dto, Category existingCategory) {
        Category category = (existingCategory != null) ? existingCategory : new Category();
        category.setId_categories(dto.getIdCategories());
        category.setName(dto.getName());
        category.setDescription(dto.getDescription());
        // Không set 'products' ở đây vì Category là 'mappedBy'
        return category;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryDTO> findAllCategories() {
        return categoryRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryDTO findCategoryById(Integer id) {
        return categoryRepository.findById(id)
                .map(this::convertToDTO)
                .orElseThrow(() -> new RuntimeException("Category not found with id: " + id));
    }

    @Override
    public void saveCategory(CategoryDTO categoryDTO) {
        // 1. Kiểm tra tên danh mục trùng lặp (không phân biệt hoa thường)
        Optional<Category> existingCategoryWithSameName = categoryRepository.findByNameIgnoreCase(categoryDTO.getName());

        if (existingCategoryWithSameName.isPresent()) {
            Category foundCategory = existingCategoryWithSameName.get();
            // Logic kiểm tra trùng lặp tương tự như ProductServiceImpl
            if (categoryDTO.getIdCategories() == null || !foundCategory.getId_categories().equals(categoryDTO.getIdCategories())) {
                throw new RuntimeException("Một danh mục với tên '" + categoryDTO.getName() + "' đã tồn tại.");
            }
        }

        // 2. Logic lưu danh mục
        Category existingCategory = null;
        if (categoryDTO.getIdCategories() != null) {
            existingCategory = categoryRepository.findById(categoryDTO.getIdCategories()).orElse(null);
        }

        Category category = convertToEntity(categoryDTO, existingCategory);
        categoryRepository.save(category);
    }

    @Override
    public void deleteCategoryById(Integer id) {
        // Bạn có thể thêm logic kiểm tra nếu danh mục đang được sử dụng bởi sản phẩm trước khi xóa
        // (Hiện tại sẽ xóa được do @ManyToMany không có ràng buộc khóa ngoại cứng)
        categoryRepository.deleteById(id);
    }
}