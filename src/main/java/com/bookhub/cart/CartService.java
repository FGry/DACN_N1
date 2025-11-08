package com.bookhub.cart;

import com.bookhub.product.Product;
import com.bookhub.product.ProductRepository;
import com.bookhub.user.User;
import com.bookhub.user.UserRepository;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

// Giả định: Các Repository cần thiết đã được định nghĩa trong package tương ứng
// CartRepository: cần findByUserAndProduct và findByUser
// UserRepository: cần findById
// ProductRepository: cần findById

@Service
@RequiredArgsConstructor
public class CartService {

    // Khai báo các Repository cần thiết (được inject qua Lombok @RequiredArgsConstructor)
    private final CartRepository cartRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    // --- 1. Logic Thêm Sản Phẩm vào Giỏ Hàng (cho người dùng đã đăng nhập) ---

    /**
     * Thêm một sản phẩm vào giỏ hàng của người dùng.
     * Nếu sản phẩm đã tồn tại, tăng số lượng. Ngược lại, tạo mục giỏ hàng mới.
     *
     * @param userId ID của người dùng
     * @param productId ID của sản phẩm
     * @param quantityToAdd Số lượng muốn thêm (mặc định thường là 1)
     * @return Đối tượng Cart (mục giỏ hàng) đã được lưu/cập nhật
     */
    @Transactional
    public Cart addProductToCart(Integer userId, Integer productId, Integer quantityToAdd) {

        // 1. Tìm User và Product
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found with ID: " + productId));

        // 2. Kiểm tra tồn kho
        if (product.getStockQuantity() < quantityToAdd) {
            throw new RuntimeException("Not enough stock for product: " + product.getTitle());
        }

        // 3. Kiểm tra mục giỏ hàng đã tồn tại chưa
        Optional<Cart> existingCartItem = cartRepository.findByUserAndProduct(user, product);

        Cart cartItem;

        if (existingCartItem.isPresent()) {
            // 4a. Nếu đã tồn tại -> Cập nhật số lượng
            cartItem = existingCartItem.get();
            int newQuantity = cartItem.getQuantity() + quantityToAdd;

            // Kiểm tra số lượng mới không vượt quá tồn kho
            if (newQuantity > product.getStockQuantity()) {
                throw new RuntimeException("Cannot add more. Total quantity exceeds stock.");
            }

            cartItem.setQuantity(newQuantity);
        } else {
            // 4b. Nếu chưa tồn tại -> Tạo mới mục giỏ hàng
            cartItem = Cart.builder()
                    .user(user)
                    .product(product)
                    .quantity(quantityToAdd)
                    .build();
        }

        // 5. Lưu vào cơ sở dữ liệu và trả về
        return cartRepository.save(cartItem);
    }

    // --- 2. Logic Hợp nhất Giỏ hàng Tạm thời (Guest Cart Merge) ---

    /**
     * Hợp nhất giỏ hàng tạm thời (Guest Cart) với giỏ hàng vĩnh viễn của người dùng.
     * Logic: Lặp qua từng mục tạm thời, nếu đã có trong giỏ vĩnh viễn thì tăng số lượng,
     * nếu chưa có thì thêm mục mới.
     *
     * @param userId ID của người dùng đã đăng nhập
     * @param guestCartData Danh sách các đối tượng {productId, quantity} từ Frontend (Local Storage)
     * @return Danh sách các mục giỏ hàng đã được hợp nhất
     */
    @Transactional
    public List<Cart> mergeGuestCart(Integer userId, List<GuestCartItemDto> guestCartData) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        for (GuestCartItemDto item : guestCartData) {
            Product product = productRepository.findById(item.getProductId())
                    .orElse(null); // Bỏ qua nếu sản phẩm không tìm thấy

            if (product != null) {
                // Tái sử dụng logic tìm kiếm và cập nhật
                Optional<Cart> existingCartItem = cartRepository.findByUserAndProduct(user, product);

                if (existingCartItem.isPresent()) {
                    // Cập nhật số lượng: Cộng dồn số lượng từ giỏ tạm thời
                    Cart cartItem = existingCartItem.get();
                    int newQuantity = cartItem.getQuantity() + item.getQuantity();

                    // Kiểm tra tồn kho trước khi lưu
                    if (newQuantity <= product.getStockQuantity()) {
                        cartItem.setQuantity(newQuantity);
                        cartRepository.save(cartItem);
                    }
                    // Nếu vượt quá tồn kho, có thể logging và chỉ giữ lại số lượng tối đa là stock

                } else {
                    // Tạo mục giỏ hàng mới
                    if (item.getQuantity() <= product.getStockQuantity()) {
                        Cart newCartItem = Cart.builder()
                                .user(user)
                                .product(product)
                                .quantity(item.getQuantity())
                                .build();
                        cartRepository.save(newCartItem);
                    }
                }
            }
        }

        // Trả về toàn bộ giỏ hàng mới của người dùng
        return cartRepository.findByUser(user);
    }

    // --- DTO để nhận dữ liệu từ Guest Cart ---

    /**
     * DTO (Data Transfer Object) để nhận dữ liệu từ giỏ hàng tạm thời (Local Storage)
     * khi người dùng đăng nhập và gửi request hợp nhất.
     */
    @Data
    @Builder
    public static class GuestCartItemDto {
        private Integer productId;
        private Integer quantity;
    }
}