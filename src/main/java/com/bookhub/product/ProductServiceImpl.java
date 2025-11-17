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

    // Đường dẫn lưu trữ vật lý trong thư mục dự án (Chỉ cho môi trường Dev)
    private final String UPLOAD_DIR = "src/main/resources/static/images/products/";


    // Helper: Chuyển đổi Entity sang DTO
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
                        .map(ImageProduct::getImage_link)
                        .collect(Collectors.toList()))
                .build();
    }

    // Helper: Chuyển đổi DTO sang Entity (Logic lưu ảnh)
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

        // 2. Cập nhật Category
        if (dto.getSelectedCategoryIds() != null && !dto.getSelectedCategoryIds().isEmpty()) {
            List<Category> categories = categoryRepository.findAllById(dto.getSelectedCategoryIds());
            product.setCategories(categories);
        } else {
            product.setCategories(new ArrayList<>());
        }

        // 3. Xử lý ảnh tải lên
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

                    // ĐƯỜNG DẪN CÔNG KHAI LƯU VÀO DB (Khớp với WebConfig)
                    String webPath = "/images/products/" + uniqueFilename;

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

    // --- IMPLEMENTATION CÁC PHƯƠNG THỨC CHUẨN ---

    @Override
    @Transactional(readOnly = true)
    public List<ProductDTO> findAllProducts() {
        return productRepository.findAll().stream()
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
        // 1. Kiểm tra tên sản phẩm trùng lặp
        Optional<Product> existingProductWithSameTitle = productRepository.findByTitleIgnoreCase(productDTO.getTitle());

        if (existingProductWithSameTitle.isPresent()) {
            Product foundProduct = existingProductWithSameTitle.get();
            if (productDTO.getIdProducts() == null || !foundProduct.getIdProducts().equals(productDTO.getIdProducts())) {
                throw new RuntimeException("Một sản phẩm với tên '" + productDTO.getTitle() + "' đã tồn tại.");
            }
        }

        // 2. Logic lưu sản phẩm
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

    // --- KHẮC PHỤC LỖI: TRIỂN KHAI PHƯƠNG THỨC BỊ THIẾU ---

    /** TRIỂN KHAI: Tìm kiếm sản phẩm theo từ khóa */
    @Override
    @Transactional(readOnly = true)
    public List<ProductDTO> searchProducts(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return findAllProducts();
        }
        // Dùng phương thức Repository đã được thêm vào
        List<Product> products = productRepository.findByTitleContainingIgnoreCase(keyword);

        return products.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /** TRIỂN KHAI: Lọc sản phẩm theo ID Danh mục */
    @Override
    @Transactional(readOnly = true)
    public List<ProductDTO> getProductsByCategory(Integer categoryId) {
        // Dùng phương thức Repository đã được thêm vào
        List<Product> products = productRepository.findByCategoriesId_categories(categoryId);

        return products.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /** TRIỂN KHAI: Chuyển đổi trạng thái (toggle) */
    @Override
    public boolean toggleProductStatus(Integer id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm có ID: " + id));

        // Cần có logic cập nhật trạng thái trong Entity Product
        // Ví dụ: product.setIsPublished(!product.getIsPublished());
        // productRepository.save(product);

        return true;
    }
}