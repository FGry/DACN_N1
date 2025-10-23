package com.bookhub.product;

// Import các lớp gốc
import com.bookhub.comments.CommentsDTO;
import com.bookhub.comments.CommentsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import org.springframework.http.ResponseEntity;


@Controller
@RequiredArgsConstructor
public class ProductController {

	// --- TIÊM SERVICE ---
	private final ProductService productService;


	// --- 1. PHƯƠNG THỨC VIEW CHO USER (THÊM MỚI CHO THYMELEAF) ---

	/**
	 * Phục vụ trang danh sách sản phẩm cho người dùng
	 */
	@GetMapping("/product.html") // <-- ĐƯỜNG DẪN TRUY CẬP TRỰC TIẾP
	public String productList() {
		return "product"; // Thymeleaf sẽ tìm file product.html
	}

	/**
	 * Phục vụ trang chi tiết sản phẩm cho người dùng
	 */
	@GetMapping("/product-detail.html") // <-- ĐƯỜNG DẪN TRUY CẬP TRỰC TIẾP (Dùng JS để lấy ID)
	public String productDetail() {
		return "product-detail"; // Thymeleaf sẽ tìm file product-detail.html
	}


	// --- 2. PHƯƠNG THỨC VIEW CHO ADMIN (ĐÃ CÓ) ---
	@GetMapping("/admin/products")
	public String about() {
		return "/admin/products";
	}


	// --- 3. CÁC PHƯƠNG THỨC API CÔNG KHAI (USER) ---

	/**
	 * API Endpoint để lấy tất cả sản phẩm
	 */
	@GetMapping("/api/products")
	@ResponseBody
	public ResponseEntity<List<ProductDTO>> getAllProducts() {
		List<ProductDTO> products = productService.findAllProducts();
		return ResponseEntity.ok(products);
	}

	/**
	 * API Endpoint để lấy một sản phẩm theo ID
	 */
	@GetMapping("/api/products/{id}")
	@ResponseBody
	public ResponseEntity<ProductDTO> getProductById(@PathVariable Integer id) {
		try {
			ProductDTO product = productService.findProductById(id);
			return ResponseEntity.ok(product);
		} catch (RuntimeException e) {
			return ResponseEntity.notFound().build();
		}
	}
}