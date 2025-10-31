package com.bookhub.comments;

import com.bookhub.product.Product;
import com.bookhub.product.ProductRepository;
import com.bookhub.user.User;
import com.bookhub.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentsService {

    private final CommentsRepository commentsRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    /** Lấy chi tiết comment/review theo ID */
    public CommentsDTO getCommentById(Integer id) {
        return commentsRepository.findById(id)
                .map(this::convertToDTO)
                .orElseThrow(() -> new NoSuchElementException("Review not found with id: " + id));
    }

    /** Cập nhật trạng thái (Duyệt/Từ chối) */
    @Transactional
    public void updateCommentStatus(Integer id, String newStatus) {
        Comments comment = commentsRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Review not found with id: " + id));

        comment.setStatus(newStatus);
        commentsRepository.save(comment);
    }

    /** Phản hồi bình luận/đánh giá */
    @Transactional
    public void replyToComment(Integer id, String replyText) {
        Comments comment = commentsRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Review not found with id: " + id));


        if (!"PUBLISHED".equals(comment.getStatus())) {
            throw new IllegalStateException("Không thể phản hồi bình luận có trạng thái '" + comment.getStatus() + "'. Bình luận phải ở trạng thái 'PUBLISHED' (Đã duyệt).");
        }

        comment.setReply(replyText);
        comment.setReplyDate(LocalDateTime.now()); // Đặt thời gian phản hồi
        commentsRepository.save(comment);
    }

    /** Duyệt tất cả bình luận đang chờ (PENDING) hàng loạt */
    @Transactional
    public int bulkApprovePendingComments() {
        return commentsRepository.bulkApprovePendingComments();
    }


    /** Lấy tất cả comment cho trang Admin (có phân trang và sắp xếp) */
    public Page<CommentsDTO> getAllCommentsForAdmin(int pageNo, int pageSize) {
        Pageable pageable = PageRequest.of(pageNo, pageSize);
        Page<Comments> pageComments = commentsRepository.findAllWithCustomSort(pageable);

        return pageComments.map(this::convertToDTO);
    }

    /** Lấy tất cả comment (không phân trang) - Thường dùng cho mục đích nội bộ hoặc API đơn giản */
    public List<CommentsDTO> getAllComments() {
        return commentsRepository.findAllWithDetails().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /** Lấy comment/review đã duyệt của một sản phẩm để hiển thị công khai */
    public List<CommentsDTO> getCommentsByProduct(Integer productId) {
        return commentsRepository.findByProduct_IdProducts(productId).stream()
                .filter(c -> "PUBLISHED".equals(c.getStatus())) // Chỉ lấy những comment đã duyệt
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /** Tạo mới comment/review */
    @Transactional
    public CommentsDTO createComment(CommentsDTO dto) {
        User user = userRepository.findById(dto.getUserId())
                .orElseThrow(() -> new NoSuchElementException("User not found with id: " + dto.getUserId()));

        Product product = productRepository.findById(dto.getProductId())
                .orElseThrow(() -> new NoSuchElementException("Product not found with id: " + dto.getProductId()));

        String status;
        String type;


        if (dto.getRate() == null || dto.getRate() == 0) {
            // Bình luận (Comment)
            status = "PUBLISHED";
            type = "COMMENT";
        } else {
            // Đánh giá (Review)
            status = "PENDING";
            type = "REVIEW";
        }

        Comments comment = Comments.builder()
                .messages(dto.getMessages())
                .rate(dto.getRate())
                .date(LocalDate.now())
                .user(user)
                .product(product)
                .status(status)
                .type(type)
                .replyDate(null)
                // FIX: Lấy giá trị purchaseVerified đã được gán từ Controller
                .purchaseVerified(dto.getPurchaseVerified())
                .build();

        return convertToDTO(commentsRepository.save(comment));
    }

    /** Xóa comment/review theo ID */
    public void deleteComment(Integer id) {
        commentsRepository.deleteById(id);
    }

    /** Chuyển đổi Entity Comments sang DTO CommentsDTO */
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

                .productTitle(productTitle)
                .productCode(productCode)
                .userName(userName)
                .status(status)
                .reply(reply)
                .replyDate(comment.getReplyDate())
                .productImageUrl(productImageUrl)
                .purchaseVerified(comment.getPurchaseVerified()) // Đảm bảo DTO cũng có trường này
                .build();
    }
}
