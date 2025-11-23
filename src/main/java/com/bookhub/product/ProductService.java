package com.bookhub.product;

import com.bookhub.product.ProductDTO;
import java.util.List;

public interface ProductService {
    List<ProductDTO> findAllProducts();

    /** Phương thức tìm kiếm theo từ khóa */
    List<ProductDTO> searchProducts(String keyword);

    /** Lọc sản phẩm theo ID Danh mục */
    List<ProductDTO> getProductsByCategory(Integer categoryId); // Tên chuẩn hóa

    ProductDTO findProductById(Integer id);
    void saveProduct(ProductDTO productDTO);
    void deleteProductById(Integer id);

    /** Chuyển đổi trạng thái hiển thị của sản phẩm */
    boolean toggleProductStatus(Integer id);


}