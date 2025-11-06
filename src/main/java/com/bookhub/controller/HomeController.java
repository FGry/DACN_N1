package com.bookhub.controller;

import com.bookhub.category.CategoryRepository; // THÊM IMPORT
import com.bookhub.product.ProductService;     // THÊM IMPORT
import com.bookhub.user.UserRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

	private final UserRepository userRepository;

	// === THÊM 2 TRƯỜNG NÀY ===
	private final CategoryRepository categoryRepository;
	private final ProductService productService;

	// === CẬP NHẬT CONSTRUCTOR ===
	public HomeController(UserRepository userRepository,
						  CategoryRepository categoryRepository,
						  ProductService productService) {
		this.userRepository = userRepository;
		this.categoryRepository = categoryRepository;
		this.productService = productService;
	}

	@GetMapping({"/", "/index", "/mainInterface.html"})
	public String home(Model model) {
		// Logic xác thực đã được chuyển sang GlobalModelAttributesAdvice.

		// === THÊM LOGIC TẢI DỮ LIỆU ===
		// 1. Tải danh sách danh mục
		model.addAttribute("allCategories", categoryRepository.findAll());

		// 2. Tải danh sách sản phẩm (ví dụ: lấy tất cả làm sản phẩm nổi bật)
		// Bạn có thể thay bằng một hàm khác như findFeaturedProducts() nếu có
		model.addAttribute("products", productService.findAllProducts());
		// === KẾT THÚC LOGIC THÊM ===

		return "mainInterface";
	}

	@GetMapping("/admin/home")
	public String homeadmin() {
		return "admin/home";
	}
}