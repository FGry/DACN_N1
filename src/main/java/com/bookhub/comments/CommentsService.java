package com.bookhub.comments;

import com.bookhub.product.Product;
import com.bookhub.product.ProductRepository;
import com.bookhub.user.User;
import com.bookhub.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentsService {

    private final CommentsRepository commentsRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;


    public CommentsDTO getCommentById(Integer id) {
        return commentsRepository.findById(id)
                .map(this::convertToDTO)
                .orElseThrow(() -> new NoSuchElementException("Review not found with id: " + id));
    }

    @Transactional
    public void updateCommentStatus(Integer id, String newStatus) {
        Comments comment = commentsRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Review not found with id: " + id));

        comment.setStatus(newStatus);
        commentsRepository.save(comment);
    }


    @Transactional
    public void replyToComment(Integer id, String replyText) {
        Comments comment = commentsRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Review not found with id: " + id));

        if (!"PUBLISHED".equals(comment.getStatus())) {
            throw new IllegalStateException("Không thể phản hồi bình luận có trạng thái '" + comment.getStatus() + "'. Bình luận phải ở trạng thái 'PUBLISHED' (Đã duyệt).");
        }

        comment.setReply(replyText);
        commentsRepository.save(comment);
    }

    // Lấy tất cả comments
    public List<CommentsDTO> getAllComments() {

        return commentsRepository.findAllWithDetails().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<CommentsDTO> getCommentsByProduct(Integer productId) {
        // (Giữ nguyên)
        return commentsRepository.findByProduct_IdProducts(productId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public CommentsDTO createComment(CommentsDTO dto) {
        // (Giữ nguyên)
        User user = userRepository.findById(dto.getUserId())
                .orElseThrow(() -> new NoSuchElementException("User not found with id: " + dto.getUserId()));

        Product product = productRepository.findById(dto.getProductId())
                .orElseThrow(() -> new NoSuchElementException("Product not found with id: " + dto.getProductId()));

        Comments comment = Comments.builder()
                .messages(dto.getMessages())
                .rate(dto.getRate())
                .date(LocalDate.now())
                .user(user)
                .product(product)
                .status("PENDING")
                .build();

        return convertToDTO(commentsRepository.save(comment));
    }


    public void deleteComment(Integer id) {
        commentsRepository.deleteById(id);
    }


    private CommentsDTO convertToDTO(Comments comment) {
        String productTitle = "Sản phẩm không rõ";
        String productCode = "N/A";
        String userName = "Ẩn danh";
        String productImageUrl = null;

        if (comment.getUser() != null) {
            userName = comment.getUser().getUsername();
        }

        if (comment.getProduct() != null) {
            productTitle = comment.getProduct().getTitle();
            productCode = comment.getProduct().getIdProducts().toString();

            if (comment.getProduct().getImages() != null && !comment.getProduct().getImages().isEmpty()) {
                productImageUrl = comment.getProduct().getImages().get(0).getImage_link();
            }
        }

        String status = comment.getStatus() != null ? comment.getStatus() : "PENDING";
        String reply = comment.getReply() != null ? comment.getReply() : "";

        return CommentsDTO.builder()
                .id(comment.getIdComment())
                .messages(comment.getMessages())
                .rate(comment.getRate())
                .date(comment.getDate())
                .userId((comment.getUser() != null) ? comment.getUser().getIdUser() : null)
                .productId((comment.getProduct() != null) ? comment.getProduct().getIdProducts() : null)

                // CÁC TRƯỜNG CHO ADMIN UI
                .productTitle(productTitle)
                .productCode(productCode)
                .userName(userName)
                .status(status)
                .reply(reply)
                .productImageUrl(productImageUrl)
                .build();
    }
}