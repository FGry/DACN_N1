package com.bookhub.comments;

import com.bookhub.product.Product;
import com.bookhub.product.ProductRepository;
import com.bookhub.user.User;
import com.bookhub.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentsService {

    private final CommentsRepository commentsRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    // Lấy tất cả comment
    public List<CommentsDTO> getAllComments() {
        return commentsRepository.findAll()
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // Lấy comment theo sản phẩm
    public List<CommentsDTO> getCommentsByProduct(Integer productId) {
        return commentsRepository.findByProduct_IdProducts(productId)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // Tạo comment mới
    public CommentsDTO createComment(CommentsDTO dto) {
        User user = userRepository.findById(dto.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        Product product = productRepository.findById(dto.getProductId())
                .orElseThrow(() -> new RuntimeException("Product not found"));

        Comments comment = Comments.builder()
                .messages(dto.getMessages())
                .rate(dto.getRate())
                .date(LocalDate.now())
                .user(user)
                .product(product)
                .build();

        return convertToDTO(commentsRepository.save(comment));
    }

    // Xóa comment
    public void deleteComment(Integer id) {
        commentsRepository.deleteById(id);
    }

    // Chuyển Entity sang DTO
    private CommentsDTO convertToDTO(Comments comment) {
        return CommentsDTO.builder()
                .id(comment.getIdComment())
                .messages(comment.getMessages())
                .rate(comment.getRate())
                .date(comment.getDate())
                .userId(comment.getUser().getIdUser())
                .productId(comment.getProduct().getIdProducts())
                .build();
    }
}
