package com.bookhub.product;

import com.bookhub.category.CategoryRepository;
import com.bookhub.comments.CommentsDTO;
import com.bookhub.comments.CommentsService;
import com.bookhub.user.User;
import com.bookhub.user.UserService; // Vẫn cần để tìm User cho Comments
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal; // Dùng để lấy thông tin đăng nhập từ Spring Security
import java.util.List;
import java.util.Optional;

@Controller
@RequiredArgsConstructor
public class ProductController {

	private final ProductService productService;
	private final CategoryRepository categoryRepository;
	private final CommentsService commentsService;
	private final UserService userService; // Giữ lại để lấy User Entity cho Comments

	@GetMapping("/products")
	public String listPublicProducts(
			Model model,
			@RequestParam(name = "keyword", required = false) String keyword,
			@RequestParam(name = "categoryId", required = false) Integer categoryId
	) {
		// *** KHÔNG CẦN LOGIC XÁC THỰC Ở ĐÂY NỮA ***

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

	/** * [PUBLIC] Hiển thị chi tiết sản phẩm (GET /products/{id})
	 */
	@GetMapping("/products/{id}")
	public String viewProductDetail(@PathVariable("id") Integer id, Principal principal, Model model) {

		// GlobalAdvice đã thêm isLoggedIn và currentUser.
		// Principal được dùng để lấy User ID cho form Comment.

		try {
			ProductDTO product = productService.findProductById(id);
			model.addAttribute("product", product);

			List<CommentsDTO> publishedComments = commentsService.getCommentsByProduct(id);
			model.addAttribute("publishedComments", publishedComments);

			CommentsDTO newComment = new CommentsDTO();
			newComment.setProductId(id);

			// Lấy userId từ Principal cho form comment
			if (principal != null) {
				// Lấy email/username từ Principal và tìm User Entity
				Optional<User> userOpt = userService.findUserByEmail(principal.getName());
				userOpt.ifPresent(user -> newComment.setUserId(user.getIdUser()));
			}
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
			productService.toggleProductStatus(id);
			redirectAttributes.addFlashAttribute("successMessage", "Cập nhật trạng thái sản phẩm ID " + id + " thành công.");
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi cập nhật trạng thái sản phẩm: " + e.getMessage());
		}
		return "redirect:/admin/products";
	}
}