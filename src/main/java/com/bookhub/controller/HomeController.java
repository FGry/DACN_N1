package com.bookhub.controller;

import com.bookhub.product.ProductDTO;
import com.bookhub.product.ProductService;
import com.bookhub.user.UserRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class HomeController {

	private final UserRepository userRepository;
	private final ProductService productService;

	public HomeController(UserRepository userRepository, ProductService productService) {
		this.userRepository = userRepository;
		this.productService = productService;
	}

	@GetMapping({"/", "/index", "/mainInterface.html"})
	public String home(Model model) {
		try {
			// Lấy TẤT CẢ sản phẩm từ Service
			List<ProductDTO> allProducts = productService.findAllProducts();

			List<ProductDTO> featuredProducts;
			List<ProductDTO> allProductsForHome; // Danh sách sản phẩm tối đa 12 cho mục "Tất cả sản phẩm" trên trang chủ

			// 1. Logic cho Sản phẩm nổi bật (4 sản phẩm đầu tiên)
			if (allProducts.size() > 4) {
				featuredProducts = allProducts.subList(0, 4);
			} else {
				featuredProducts = allProducts;
			}

			// 2. Logic cho mục "Tất cả sản phẩm" trên trang chủ (Giới hạn 12 sản phẩm)
			if (allProducts.size() > 12) {
				allProductsForHome = allProducts.subList(0, 12);
			} else {
				allProductsForHome = allProducts;
			}

			// 3. Thêm SẢN PHẨM NỔI BẬT vào Model
			model.addAttribute("products", featuredProducts);

			// 4. Thêm DANH SÁCH RÚT GỌN 12 SẢN PHẨM vào Model
			model.addAttribute("allProductsForHome", allProductsForHome);

		} catch (Exception e) {
			model.addAttribute("products", List.of());
			model.addAttribute("allProductsForHome", List.of());
			System.err.println("Error loading products for home page: " + e.getMessage());
		}

		return "mainInterface";
	}

	@GetMapping("/admin/home")
	public String homeadmin() {
		return "admin/home";
	}
}