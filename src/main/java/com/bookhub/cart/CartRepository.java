package com.bookhub.cart;

import com.bookhub.product.Product;
import com.bookhub.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<Cart, Integer> {

    /**
     * Tìm kiếm một mục giỏ hàng (Cart) cụ thể dựa trên Người dùng (User) và Sản phẩm (Product).
     * Phương thức này là BẮT BUỘC cho logic thêm/cập nhật sản phẩm trong CartService.
     *
     * @param user Đối tượng User (người sở hữu giỏ hàng).
     * @param product Đối tượng Product (sản phẩm trong giỏ hàng).
     * @return Optional chứa mục Cart nếu tìm thấy.
     */
    Optional<Cart> findByUserAndProduct(User user, Product product);

    /**
     * Tìm kiếm tất cả các mục giỏ hàng (Cart) thuộc về một Người dùng (User) cụ thể.
     * Phương thức này là BẮT BUỘC cho logic hợp nhất (merge) để trả về giỏ hàng đã được cập nhật.
     *
     * @param user Đối tượng User (người sở hữu giỏ hàng).
     * @return Danh sách các mục Cart.
     */
    List<Cart> findByUser(User user);

    // Tùy chọn: Phương thức sử dụng JPQL với JOIN FETCH để tải Product cùng lúc
    // Giúp tránh vấn đề N+1 Select khi hiển thị giỏ hàng.
    @Query("SELECT c FROM Cart c JOIN FETCH c.product p WHERE c.user = :user")
    List<Cart> findByUserWithProducts(User user);

    /**
     * Xóa tất cả các mục giỏ hàng thuộc về một Người dùng.
     * Có thể hữu ích khi hoàn tất đơn hàng.
     *
     * @param user Đối tượng User (người sở hữu giỏ hàng).
     */
    void deleteAllByUser(User user);
}