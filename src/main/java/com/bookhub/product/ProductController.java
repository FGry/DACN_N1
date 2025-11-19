package com.bookhub.product;

import com.bookhub.category.CategoryRepository;
import com.bookhub.comments.CommentsDTO;
import com.bookhub.comments.CommentsService;
import com.bookhub.order.OrderService; // ⬅️ NEW: Cần cho logic kiểm tra đơn hàng
import com.bookhub.user.User;
import com.bookhub.user.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class ProductController {

	private final ProductService productService;
	private final CategoryRepository categoryRepository;
	private final CommentsService commentsService;
	private final UserService userService;
	private final OrderService orderService; // ⬅️ Đã thêm OrderService

	@GetMapping("/products")
	public String listPublicProducts(
			Model model,
			@RequestParam(name = "keyword", required = false) String keyword,
			@RequestParam(name = "categoryId", required = false) Integer categoryId
	) {
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
		model.addAttribute("allCategories", categoryRepository.findAll());

		return "user/product";
	}

	/** * Helper: Lấy User Entity từ Principal
	 */
	private Optional<User> getAuthenticatedUser(Principal principal) {
		if (principal == null) {
			return Optional.empty();
		}
		return userService.findUserByEmail(principal.getName());
	}


	/** * [PUBLIC] Hiển thị chi tiết sản phẩm (GET /products/{id})
	 */
	@GetMapping("/products/{id}")
	public String viewProductDetail(@PathVariable("id") Integer id, Principal principal, Model model) {

		try {
			ProductDTO product = productService.findProductById(id);
			model.addAttribute("product", product);

			Optional<User> userOpt = getAuthenticatedUser(principal);
			Integer currentUserId = userOpt.map(User::getIdUser).orElse(null);

			// 1. Lấy tất cả đánh giá đã duyệt
			List<CommentsDTO> allPublishedComments = commentsService.getCommentsByProduct(id);

			// 2. Phân loại đánh giá
			CommentsDTO myReview = null;
			boolean canReview = false;

			if (currentUserId != null) {
				// 2a. Lấy đánh giá của bản thân (nếu có)
				Optional<CommentsDTO> existingReview = allPublishedComments.stream()
						.filter(c -> currentUserId.equals(c.getUserId()) && c.getRate() != null && c.getRate() > 0)
						.findFirst();

				if (existingReview.isPresent()) {
					myReview = existingReview.get();
				}

				// 2b. Kiểm tra quyền đánh giá (Chỉ đánh giá khi đã giao hàng VÀ chưa có đánh giá nào cho lần mua đó)
				canReview = orderService.hasDeliveredProduct(currentUserId, id);

				// Nếu đã có đánh giá, người dùng chỉ có thể CHỈNH SỬA, không thể TẠO mới.
				if (myReview != null) {
					// Vẫn đặt canReview=true để hiển thị form EDIT
					// Logic chỉnh sửa/tạo mới sẽ được xử lý trong View
				}
			}

			// 3. Truyền biến vào Model
			model.addAttribute("newComment", new CommentsDTO()); // Cần cho form tạo mới
			model.addAttribute("myReview", myReview); // Đánh giá của riêng người dùng
			model.addAttribute("canReview", canReview); // ⬅️ FIX: Luôn là boolean (true/false)
			model.addAttribute("isLoggedIn", principal != null); // Có đăng nhập không

			// Danh sách đầy đủ các comments (bao gồm cả review và comment, bao gồm cả của mình)
			// View sẽ tự lọc (reviewed/comment-only) và loại trừ myReview.
			model.addAttribute("publishedComments", allPublishedComments);

			return "user/product_detail";

		} catch (RuntimeException e) {
			model.addAttribute("errorMessage", "Sản phẩm không tồn tại.");
			// Quay về trang lỗi hoặc danh sách sản phẩm thay vì 'error/404'
			return "redirect:/products";
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