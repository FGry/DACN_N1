package com.bookhub.product;

import com.bookhub.product.ProductDTO;
import java.util.List;

public interface ProductService {
    List<ProductDTO> findAllProducts();
    ProductDTO findProductById(Integer id);
    void saveProduct(ProductDTO productDTO);
    void deleteProductById(Integer id);
}