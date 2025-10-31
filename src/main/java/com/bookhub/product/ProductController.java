package com.bookhub.product;

import com.bookhub.category.CategoryRepository;
import com.bookhub.comments.CommentsDTO;
import com.bookhub.comments.CommentsService;
import com.bookhub.user.User; // Cần import User Entity
import com.bookhub.user.UserService; // Cần import UserService
import jakarta.servlet.http.HttpSession; // Cần import HttpSession
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.List;
import java.util.Optional;

@Controller
@RequiredArgsConstructor
public class ProductController {

	private final ProductService productService;
	private final CategoryRepository categoryRepository;
	private final CommentsService commentsService;
	private final UserService userService; // Đã thêm: UserService để lấy thông tin người dùng

	private static final String USER_SESSION_KEY = "currentUserId";

	/**
	 * Helper: Thiết lập thông tin người dùng vào Model (Đã sao chép từ Auth/User Controller).
	 */
	private void setUserInfoToModel(HttpSession session, Model model) {
		Integer userId = (Integer) session.getAttribute(USER_SESSION_KEY);
		if (userId != null) {
			Optional<User> userOpt = userService.findUserById(userId);
			if (userOpt.isPresent()) {
				User user = userOpt.get();
				model.addAttribute("isLoggedIn", true);
				model.addAttribute("currentUser", user);
			} else {
				session.invalidate();
				model.addAttribute("isLoggedIn", false);
			}
		} else {
			// KHẮC PHỤC LỖI: Đảm bảo biến này luôn được đặt là false nếu chưa đăng nhập
			model.addAttribute("isLoggedIn", false);
		}
	}


	// =====================================
	// === 1. ENDPOINTS PUBLIC / VIEW & API ===
	// =====================================

	/** * [PUBLIC] View cho trang danh sách sản phẩm (GET /products) */
	@GetMapping("/products")
	public String listPublicProducts(
			HttpSession session, // Đã thêm: HttpSession
			Model model,
			@RequestParam(name = "keyword", required = false) String keyword,
			@RequestParam(name = "categoryId", required = false) Integer categoryId
	) {
		// KHẮC PHỤC LỖI: Gọi helper để truyền isLoggedIn và currentUser vào Model
		setUserInfoToModel(session, model);

		List<ProductDTO> products;
		String title;

		if (categoryId != null) {
			products = productService.getProductsByCategory(categoryId);
			title = "Sản phẩm theo danh mục";
		}
		else if (keyword != null && !keyword.trim().isEmpty()) {
			products = productService.searchProducts(keyword.trim());
			title = "Kết quả tìm kiếm";
		} else {
			products = productService.findAllProducts();
			title = "Tất cả sản phẩm của BookStore";
		}

		model.addAttribute("products", products);
		model.addAttribute("pageTitle", title);

		return "user/product";
	}

	/** * [PUBLIC] Hiển thị chi tiết sản phẩm (GET /product_detail/{id}) */
	@GetMapping("/product/{id}")
	public String viewProductDetail(@PathVariable("id") Integer id, HttpSession session, Model model) { // Đã thêm HttpSession

		// KHẮC PHỤC LỖI: Gọi helper để truyền isLoggedIn và currentUser vào Model
		setUserInfoToModel(session, model);

		try {
			ProductDTO product = productService.findProductById(id);
			model.addAttribute("product", product);

			List<CommentsDTO> publishedComments = commentsService.getCommentsByProduct(id);
			model.addAttribute("publishedComments", publishedComments);

			CommentsDTO newComment = new CommentsDTO();
			newComment.setProductId(id);
			// Gán userId từ session nếu có, hoặc để null
			Integer userId = (Integer) session.getAttribute(USER_SESSION_KEY);
			newComment.setUserId(userId);
			model.addAttribute("newComment", newComment);

			return "user/product_detail";

		} catch (RuntimeException e) {
			model.addAttribute("errorMessage", "Sản phẩm không tồn tại.");
			return "error/404";
		}
	}

	/** * [API PUBLIC] Lấy chi tiết sản phẩm bằng ID (GET /api/products/{id}) */
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

	/** * [API PUBLIC] Lấy TẤT CẢ sản phẩm (GET /api/products) */
	@GetMapping("/api/products")
	@ResponseBody
	public ResponseEntity<List<ProductDTO>> getAllProductsAPI() {
		List<ProductDTO> products = productService.findAllProducts();
		return ResponseEntity.ok(products);
	}


	// =====================================
	// === 2. ENDPOINTS ADMIN (VIEW & ACTION) ===
	// =====================================

	/** * [ADMIN] View cho trang quản lý sản phẩm (GET /admin/products) */
	@GetMapping("/admin/products")
	public String listAdminProducts(Model model) {

		if (!model.containsAttribute("product")) {
			model.addAttribute("product", new ProductDTO());
		}

		model.addAttribute("products", productService.findAllProducts());
		model.addAttribute("allCategories", categoryRepository.findAll());
		return "admin/products";
	}

	/** * [ADMIN] LẤY DỮ LIỆU SẢN PHẨM ĐỂ SỬA (GET /admin/products/edit/{id}) */
	@GetMapping("/admin/products/edit/{id}")
	@ResponseBody
	public ProductDTO showEditForm(@PathVariable("id") Integer id) {
		return productService.findProductById(id);
	}

	/** * [ADMIN] XỬ LÝ LƯU SẢN PHẨM (POST /admin/products/save) */
	@PostMapping("/admin/products/save")
	public String saveProduct(
			@Valid @ModelAttribute("product") ProductDTO productDTO,
			BindingResult bindingResult,
			RedirectAttributes redirectAttributes,
			Model model) {

		// 1. KIỂM TRA VALIDATION (Bắt lỗi null/format)
		if (bindingResult.hasErrors()) {
			model.addAttribute("errorMessage", "Lỗi nhập liệu: Vui lòng điền đầy đủ và chính xác các trường bắt buộc.");
			model.addAttribute("products", productService.findAllProducts());
			model.addAttribute("allCategories", categoryRepository.findAll());
			model.addAttribute("product", productDTO);
			model.addAttribute("openFormModal", true);
			return "admin/products";
		}

		// 2. XỬ LÝ LOGIC VÀ LỖI TRÙNG TÊN
		try {
			productService.saveProduct(productDTO);
			redirectAttributes.addFlashAttribute("successMessage", "Lưu sản phẩm thành công!");
			return "redirect:/admin/products";

		} catch (RuntimeException e) {
			if (e.getMessage() != null && e.getMessage().contains("Một sản phẩm với tên")) {

				model.addAttribute("errorMessage", e.getMessage());
				model.addAttribute("products", productService.findAllProducts());
				model.addAttribute("allCategories", categoryRepository.findAll());
				model.addAttribute("product", productDTO);
				model.addAttribute("openFormModal", true);
				return "admin/products";

			} else {
				redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
				return "redirect:/admin/products";
			}
		}
	}

	/** * [ADMIN] XÓA SẢN PHẨM (GET /admin/products/delete/{id}) */
	@GetMapping("/admin/products/delete/{id}")
	public String deleteProduct(@PathVariable("id") Integer id, RedirectAttributes redirectAttributes) {
		try {
			productService.deleteProductById(id);
			redirectAttributes.addFlashAttribute("successMessage", "Xóa sản phẩm ID " + id + " thành công!");
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi xóa sản phẩm: " + e.getMessage());
		}
		return "redirect:/admin/products";
	}

	/** * [ADMIN] CHUYỂN ĐỔI TRẠNG THÁI SẢN PHẨM (GET /admin/products/toggle-status/{id}) */
	@GetMapping("/admin/products/toggle-status/{id}")
	public String toggleProductStatus(@PathVariable("id") Integer id, RedirectAttributes redirectAttributes) {
		try {
			// Phương thức này tồn tại trong ProductService
			productService.toggleProductStatus(id);
			redirectAttributes.addFlashAttribute("successMessage", "Cập nhật trạng thái sản phẩm ID " + id + " thành công.");
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi cập nhật trạng thái sản phẩm: " + e.getMessage());
		}
		return "redirect:/admin/products";
	}
}