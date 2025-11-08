package com.bookhub.comments;

import com.bookhub.user.User;
import com.bookhub.user.UserService;
import com.bookhub.order.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

@Controller
@RequiredArgsConstructor
public class CommentsController {

    private final CommentsService commentsService;
    private final UserService userService;
    private final OrderService orderService;


    /** Lấy User Entity đầy đủ từ DB dựa trên Principal (Email) của Spring Security */
    private Optional<User> getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated() &&
                !authentication.getPrincipal().equals("anonymousUser")) {

            String email = authentication.getName();
            return userService.findUserByEmail(email);
        }
        return Optional.empty();
    }

    private Integer getCurrentAuthenticatedUserId() {
        Optional<User> userOpt = getAuthenticatedUser();
        return userOpt.map(User::getIdUser).orElse(null);
    }

    private boolean hasUserPurchasedProduct(Integer userId, Integer productId) {
        if (userId == null || productId == null) {
            return false;
        }
        return orderService.hasDeliveredProduct(userId, productId);
    }

    // 1. Xem danh sách tất cả bình luận/đánh giá
    @GetMapping("/admin/comments")
    public String listAllCommentsForAdmin(
            @RequestParam(name = "page", defaultValue = "0") int pageNo,
            @RequestParam(name = "size", defaultValue = "20") int pageSize,
            Model model
    ) {
        Page<CommentsDTO> pageComments = commentsService.getAllCommentsForAdmin(pageNo, pageSize);

        model.addAttribute("comments", pageComments.getContent());
        model.addAttribute("pageTitle", "Quản lý Đánh giá");
        model.addAttribute("currentPage", pageComments.getNumber());
        model.addAttribute("totalPages", pageComments.getTotalPages());
        model.addAttribute("totalElements", pageComments.getTotalElements());

        model.addAttribute("typeFilter", "");
        model.addAttribute("statusFilter", "");
        model.addAttribute("rateFilter", "");

        return "admin/review";
    }

    // 2. Chi tiết đánh giá
    @GetMapping("/admin/comments/detail/{id}")
    @ResponseBody
    public CommentsDTO getCommentDetail(@PathVariable("id") Integer id) {
        return commentsService.getCommentById(id);
    }

    // 3. Duyệt/Đăng đánh giá
    @GetMapping("/admin/comments/publish/{id}")
    public String publishComment(@PathVariable("id") Integer id, RedirectAttributes ra) {
        try {
            commentsService.updateCommentStatus(id, "PUBLISHED");
            ra.addFlashAttribute("successMessage", "Đánh giá #" + id + " đã được DUYỆT thành công.");
        } catch (NoSuchElementException e) {
            ra.addFlashAttribute("errorMessage", "Không tìm thấy đánh giá #" + id + ".");
        }
        return "redirect:/admin/comments";
    }

    // 4. Ẩn/Gỡ đánh giá
    @GetMapping("/admin/comments/hide/{id}")
    public String hideComment(@PathVariable("id") Integer id, RedirectAttributes ra) {
        try {
            commentsService.updateCommentStatus(id, "HIDDEN");
            ra.addFlashAttribute("successMessage", "Đánh giá #" + id + " đã được ẨN thành công.");
        } catch (NoSuchElementException e) {
            ra.addFlashAttribute("errorMessage", "Không tìm thấy đánh giá #" + id + ".");
        }
        return "redirect:/admin/comments";
    }

    // 5. Xóa đánh giá
    @GetMapping("/admin/comments/delete/{id}")
    public String deleteComment(@PathVariable("id") Integer id, RedirectAttributes ra) {
        try {
            commentsService.deleteComment(id);
            ra.addFlashAttribute("successMessage", "Đánh giá #" + id + " đã được XÓA thành công.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "Lỗi khi xóa đánh giá #" + id + ": " + e.getMessage());
        }
        return "redirect:/admin/comments";
    }

    // 6. DUYỆT HÀNG LOẠT
    @PostMapping("/admin/comments/approve-all")
    public ResponseEntity<String> approveAllPendingComments() {
        try {
            int count = commentsService.bulkApprovePendingComments();
            if (count > 0) {
                return ResponseEntity.ok("Đã duyệt thành công " + count + " đánh giá đang chờ.");
            } else {
                return ResponseEntity.ok("Không có đánh giá/bình luận nào ở trạng thái Chờ duyệt để duyệt.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Lỗi khi duyệt hàng loạt đánh giá. Vui lòng kiểm tra Server Log.");
        }
    }


    // 7. Phản hồi đánh giá
    @PostMapping("/admin/comments/reply/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, String>> replyToComment(
            @PathVariable("id") Integer id,
            @RequestBody Map<String, String> payload) {

        String replyText = payload.get("reply");

        try {
            if (replyText == null || replyText.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Nội dung phản hồi không được để trống."));
            }
            commentsService.replyToComment(id, replyText);
            return ResponseEntity.ok(Map.of("message", "Phản hồi đã được gửi thành công."));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            System.err.println("Error replying to comment: " + e.getMessage());
            return ResponseEntity.status(500).body(Map.of("message", "Lỗi khi xử lý phản hồi."));
        }
    }

    // 8. Xem danh sách tất cả bình luận công khai
    @GetMapping("/comments/public")
    public String listAllComments(Model model) {
        List<CommentsDTO> comments = commentsService.getAllComments();
        model.addAttribute("comments", comments);
        return "comments/list";
    }

    // 9. Xem bình luận theo sản phẩm
    @GetMapping("/comments/product/{id}")
    public String listCommentsByProduct(@PathVariable("id") Integer productId, Model model) {
        List<CommentsDTO> comments = commentsService.getCommentsByProduct(productId);
        model.addAttribute("comments", comments);
        model.addAttribute("productId", productId);
        return "comments/product";
    }

    // 10. Hiển thị form tạo mới
    @GetMapping("/comments/new")
    public String showCreateForm(Model model) {
        model.addAttribute("comment", new CommentsDTO());
        return "comments/new";
    }

    /** 11a. Lưu ĐÁNH GIÁ (Phải có rate > 0) - Dùng ID thực tế & Kiểm tra đã mua hàng */
    @PostMapping("/comments/review/save")
    public String saveReview(@ModelAttribute("newComment") CommentsDTO comment, RedirectAttributes ra) {
        Integer currentUserId = getCurrentAuthenticatedUserId();

        if (currentUserId == null) {
            ra.addFlashAttribute("errorMessage", "Vui lòng đăng nhập để đánh giá.");
            return "redirect:/products/" + comment.getProductId() + "#review";
        }

        // --- LOGIC BẮT BUỘC CÓ SAO (RATE) ---
        if (comment.getRate() == null || comment.getRate() < 1 || comment.getRate() > 5) {
            ra.addFlashAttribute("errorMessage", "Vui lòng chọn số sao để gửi Đánh giá.");
            return "redirect:/products/" + comment.getProductId() + "#review";
        }

        // --- LOGIC KIỂM TRA ĐÃ MUA HÀNG (SỬ DỤNG SERVICE) ---
        boolean hasPurchased = hasUserPurchasedProduct(currentUserId, comment.getProductId());

        if (!hasPurchased) {
            ra.addFlashAttribute("errorMessage", "Bạn chỉ có thể đánh giá sản phẩm sau khi đơn hàng đã được giao.");
            return "redirect:/products/" + comment.getProductId() + "#review";
        }

        comment.setUserId(currentUserId);
        comment.setPurchaseVerified(hasPurchased);

        commentsService.createComment(comment);
        ra.addFlashAttribute("successMessage", "Đánh giá của bạn đã được gửi thành công và đang chờ duyệt.");
        return "redirect:/products/" + comment.getProductId() + "#review";
    }

    /** 11b. Lưu BÌNH LUẬN (Bắt buộc không có rate, chỉ có messages) - Dùng ID thực tế */
    @PostMapping("/comments/comment-only/save")
    public String saveCommentOnly(@ModelAttribute("newComment") CommentsDTO comment, RedirectAttributes ra) {
        Integer currentUserId = getCurrentAuthenticatedUserId();

        if (currentUserId == null) {
            ra.addFlashAttribute("errorMessage", "Vui lòng đăng nhập để bình luận.");
            return "redirect:/products/" + comment.getProductId() + "#comment";
        }

        // --- LOGIC BẮT BUỘC KHÔNG CÓ SAO (RATE) & CÓ TIN NHẮN ---
        comment.setRate(null);

        if (comment.getMessages() == null || comment.getMessages().trim().isEmpty()) {
            ra.addFlashAttribute("errorMessage", "Nội dung bình luận không được để trống.");
            return "redirect:/products/" + comment.getProductId() + "#comment";
        }

        comment.setUserId(currentUserId);
        comment.setPurchaseVerified(false);

        commentsService.createComment(comment);
        ra.addFlashAttribute("successMessage", "Bình luận của bạn đã được gửi thành công và đang chờ duyệt.");
        return "redirect:/products/" + comment.getProductId() + "#comment";
    }
}