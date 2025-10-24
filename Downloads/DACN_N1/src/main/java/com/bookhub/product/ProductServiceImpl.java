package com.bookhub.product;

import com.bookhub.category.Category;
import com.bookhub.category.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    private final String UPLOAD_DIR = "C:/bookhub_uploads/products/";

    // Helper: Chuyển đổi Entity sang DTO (Giữ nguyên)
    private ProductDTO convertToDTO(Product product) {
        if (product == null) return null;
        return ProductDTO.builder()
                .idProducts(product.getIdProducts())
                .title(product.getTitle())
                .price(product.getPrice())
                .author(product.getAuthor())
                .publisher(product.getPublisher())
                .publicationYear(product.getPublicationYear())
                .pages(product.getPages())
                .stockQuantity(product.getStockQuantity())
                .language(product.getLanguage())
                .discount(product.getDiscount())
                .description(product.getDescription())
                .categoryNames(product.getCategories().stream()
                        .map(Category::getName)
                        .collect(Collectors.toList()))
                .selectedCategoryIds(product.getCategories().stream()
                        .map(Category::getId_categories)
                        .collect(Collectors.toList()))
                .imageLinks(product.getImages().stream()
                        .map(image -> {
                            String link = image.getImage_link();
                            return link.startsWith("/uploads/") ? link : "/uploads/products/" + link;
                        })
                        .collect(Collectors.toList()))
                .build();
    }

    // Helper: Chuyển đổi DTO sang Entity (Giữ nguyên)
    private Product convertToEntity(ProductDTO dto, Product existingProduct) {
        Product product = existingProduct != null ? existingProduct : new Product();

        // 1. Cập nhật các trường cơ bản
        product.setIdProducts(dto.getIdProducts());
        product.setTitle(dto.getTitle());
        product.setPrice(dto.getPrice());
        product.setAuthor(dto.getAuthor());
        product.setPublisher(dto.getPublisher());
        product.setPublicationYear(dto.getPublicationYear());
        product.setPages(dto.getPages());
        product.setStockQuantity(dto.getStockQuantity());
        product.setLanguage(dto.getLanguage());
        product.setDiscount(dto.getDiscount());
        product.setDescription(dto.getDescription());

        // 2. Cập nhật quan hệ ManyToMany với Category (Không đổi)
        if (dto.getSelectedCategoryIds() != null && !dto.getSelectedCategoryIds().isEmpty()) {
            List<Category> categories = categoryRepository.findAllById(dto.getSelectedCategoryIds());
            product.setCategories(categories);
        } else {
            product.setCategories(new ArrayList<>());
        }

        // 3. Cập nhật quan hệ OneToMany với ImageProduct (Không đổi)
        if (dto.getImageFiles() != null && !dto.getImageFiles().isEmpty() && !dto.getImageFiles().get(0).isEmpty()) {

            if (product.getImages() != null) {
                product.getImages().clear();
            } else {
                product.setImages(new ArrayList<>());
            }

            Path uploadPath = Paths.get(UPLOAD_DIR);

            try {
                if (!Files.exists(uploadPath)) {
                    Files.createDirectories(uploadPath);
                }

                for (MultipartFile file : dto.getImageFiles()) {
                    if (file.isEmpty()) continue;

                    String originalFilename = file.getOriginalFilename();
                    String fileExtension = "";
                    if (originalFilename != null && originalFilename.contains(".")) {
                        fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
                    }
                    String uniqueFilename = UUID.randomUUID().toString() + fileExtension;

                    Path filePath = uploadPath.resolve(uniqueFilename);

                    try (InputStream inputStream = file.getInputStream()) {
                        Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);
                    }

                    // Lưu webPath vào database
                    String webPath = "/uploads/products/" + uniqueFilename;

                    ImageProduct image = ImageProduct.builder()
                            .image_link(webPath)
                            .product(product)
                            .build();
                    product.getImages().add(image);
                }
            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException("Không thể lưu file ảnh.", e);
            }
        }
        return product;
    }

    // --- Implement Service Methods ---

    @Override
    @Transactional(readOnly = true)
    public List<ProductDTO> findAllProducts() {
        return productRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductDTO> searchProducts(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return findAllProducts();
        }
        // Gọi phương thức tìm kiếm mới, bao gồm tác giả
        return productRepository.searchByKeyword(keyword.trim().toLowerCase()).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // TRIỂN KHAI PHƯƠNG THỨC LỌC THEO CATEGORY ID
    @Override
    @Transactional(readOnly = true)
    public List<ProductDTO> findProductsByCategoryId(Integer categoryId) {
        return productRepository.findByCategoryId(categoryId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ProductDTO findProductById(Integer id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));
        return convertToDTO(product);
    }

    @Override
    public void saveProduct(ProductDTO productDTO) {
        // ... (Giữ nguyên logic saveProduct)
        Optional<Product> existingProductWithSameTitle = productRepository.findByTitleIgnoreCase(productDTO.getTitle());

        if (existingProductWithSameTitle.isPresent()) {
            Product foundProduct = existingProductWithSameTitle.get();
            if (productDTO.getIdProducts() == null || !foundProduct.getIdProducts().equals(productDTO.getIdProducts())) {
                throw new RuntimeException("Một sản phẩm với tên '" + productDTO.getTitle() + "' đã tồn tại.");
            }
        }

        Product existingProduct = null;
        if (productDTO.getIdProducts() != null) {
            existingProduct = productRepository.findById(productDTO.getIdProducts()).orElse(null);
        }

        Product product = convertToEntity(productDTO, existingProduct);
        productRepository.save(product);
    }


    @Override
    public void deleteProductById(Integer id) {
        productRepository.deleteById(id);
    }
}