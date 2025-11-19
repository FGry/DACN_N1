package com.bookhub.controller;

import com.bookhub.category.CategoryRepository;
import com.bookhub.product.ProductDTO; // Cần import ProductDTO
import com.bookhub.product.ProductService;
import com.bookhub.user.UserRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class HomeController {

	private final UserRepository userRepository;
	private final CategoryRepository categoryRepository;
	private final ProductService productService;

	public HomeController(UserRepository userRepository,
						  CategoryRepository categoryRepository,
						  ProductService productService) {
		this.userRepository = userRepository;
		this.categoryRepository = categoryRepository;
		this.productService = productService;
	}

	@GetMapping({"/", "/index", "/mainInterface.html"})
	public String home(Model model) {
		try {
			// 1. LẤY TẤT CẢ DỮ LIỆU SẢN PHẨM (dùng cho cả nổi bật và phân trang)
			List<ProductDTO> allProducts = productService.findAllProducts();

			// 2. TẠO DANH SÁCH SẢN PHẨM NỔI BẬT (Featured Products)
			List<ProductDTO> featuredProducts;
			if (allProducts.size() > 4) {
				featuredProducts = allProducts.subList(0, 4); // Lấy 4 sản phẩm đầu
			} else {
				featuredProducts = allProducts;
			}

			// 3. TẠO DANH SÁCH SẢN PHẨM CHO MỤC TẤT CẢ (All Products for Home)
			// LƯU Ý: Frontend script yêu cầu biến này tên là allProductsForHome
			List<ProductDTO> allProductsForHome;
			if (allProducts.size() > 12) {
				// Nếu muốn chỉ hiển thị 12 sản phẩm đầu tiên cho lần tải đầu tiên:
				allProductsForHome = allProducts.subList(0, 12);
			} else {
				// HOẶC: Nếu muốn dùng TẤT CẢ sản phẩm để script phân trang:
				allProductsForHome = allProducts; // Nên dùng tất cả
			}


			// === TRUYỀN DỮ LIỆU VÀO MODEL ===

			// 4. TRUYỀN DANH MỤC (Category)
			model.addAttribute("allCategories", categoryRepository.findAll());

			// 5. TRUYỀN SẢN PHẨM NỔI BẬT (products)
			model.addAttribute("products", featuredProducts);

			// 6. TRUYỀN TẤT CẢ SẢN PHẨM CHO PHÂN TRANG (allProductsForHome)
			model.addAttribute("allProductsForHome", allProductsForHome);

		} catch (Exception e) {
			// Xử lý lỗi: Trả về danh sách rỗng để tránh lỗi NullPointer trên frontend
			model.addAttribute("allCategories", List.of());
			model.addAttribute("products", List.of());
			model.addAttribute("allProductsForHome", List.of());
			System.err.println("Error loading products/categories for home page: " + e.getMessage());
		}

		return "mainInterface";
	}

	@GetMapping("/admin/home")
	public String homeadmin() {
		return "admin/home";
	}
}