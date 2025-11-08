package com.bookhub.cart;

import com.bookhub.cart.CartService.GuestCartItemDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.ui.Model;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/user") // ĐẶT /user VÌ ĐÂY LÀ TRANG CỦA NGƯỜI DÙNG CUỐI
@RequiredArgsConstructor
public class CartController {

	private final CartService cartService;

	// --- Giả định DTOs cho Response ---
	public record CartResponseDto(
			Integer idCart,
			Integer quantity,
			Integer productId,
			String productTitle
	) {}

	private Integer getCurrentAuthenticatedUserId() {
		return 1;
	}

	// ======================================
	// === ENDPOINT XEM GIỎ HÀNG (VIEW) ===
	// ======================================

	/**
	 * Chuyển hướng đến trang giỏ hàng (cart.html)
	 * URL truy cập: /user/cart
	 */
	@GetMapping("/cart") // ĐÃ SỬA THÀNH /cart
	public String viewCart(Model model) {
		return "user/cart";
	}

	// ==========================================
	// === ENDPOINTS API (Vẫn là @ResponseBody) ===
	// ==========================================

	// Đã sửa đường dẫn API thành /carts
	@PostMapping("/carts")
	@ResponseBody
	public ResponseEntity<?> addProductToCart(@RequestBody AddToCartRequest request) {
		try {
			Integer userId = getCurrentAuthenticatedUserId();

			Cart updatedCartItem = cartService.addProductToCart(
					userId,
					request.getProductId(),
					request.getQuantityToAdd()
			);

			// Chuyển đổi sang DTO để trả về
			CartResponseDto responseDto = new CartResponseDto(
					updatedCartItem.getId_cart(),
					updatedCartItem.getQuantity(),
					updatedCartItem.getProduct().getIdProducts(),
					updatedCartItem.getProduct().getTitle()
			);

			return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
		} catch (RuntimeException e) {
			// Xử lý lỗi (ví dụ: User not found, Product not found, Not enough stock)
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
		}
	}

	@PostMapping("/carts/merge") // Đã sửa đường dẫn API thành /carts/merge
	@ResponseBody
	public ResponseEntity<?> mergeGuestCart(@RequestBody List<GuestCartItemDto> guestCartData) {
		try {
			Integer userId = getCurrentAuthenticatedUserId();

			List<Cart> mergedCarts = cartService.mergeGuestCart(userId, guestCartData);

			// Chuyển đổi danh sách Cart sang danh sách DTO để trả về
			List<CartResponseDto> responseDtos = mergedCarts.stream()
					.map(item -> new CartResponseDto(
							item.getId_cart(),
							item.getQuantity(),
							item.getProduct().getIdProducts(),
							item.getProduct().getTitle()
					))
					.collect(Collectors.toList());

			return ResponseEntity.ok(responseDtos);
		} catch (RuntimeException e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Cart merge failed: " + e.getMessage());
		}
	}

	public static class AddToCartRequest {
		private Integer productId;
		private Integer quantityToAdd;

		public Integer getProductId() { return productId; }
		public Integer getQuantityToAdd() { return quantityToAdd; }

	}
}