package com.bookhub.comments;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@Controller
@RequiredArgsConstructor
public class CommentsController {

    private final CommentsService commentsService;

    // ===================================
    // === PHẦN ENDPOINT DÀNH CHO ADMIN (Giữ nguyên) ===
    // ===================================

    // 1. Xem danh sách tất cả bình luận/đánh giá
    @GetMapping("/admin/comments")
    public String listAllCommentsForAdmin(
            @RequestParam(name = "page", defaultValue = "0") int pageNo,
            @RequestParam(name = "size", defaultValue = "20") int pageSize,
            Model model
    ) {
        if (pageNo < 0) pageNo = 0;
        if (pageSize <= 0) pageSize = 20;

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

        if (id == null || id <= 0) {
            throw new NoSuchElementException("ID không hợp lệ");
        }

        return commentsService.getCommentById(id);
    }

    // 3. Duyệt/Đăng đánh giá
    @GetMapping("/admin/comments/publish/{id}")
    public String publishComment(@PathVariable("id") Integer id, RedirectAttributes ra) {

        if (id == null || id <= 0) {
            ra.addFlashAttribute("errorMessage", "ID đánh giá không hợp lệ.");
            return "redirect:/admin/comments";
        }

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

        if (id == null || id <= 0) {
            ra.addFlashAttribute("errorMessage", "ID đánh giá không hợp lệ.");
            return "redirect:/admin/comments";
        }

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

        if (id == null || id <= 0) {
            ra.addFlashAttribute("errorMessage", "ID đánh giá không hợp lệ.");
            return "redirect:/admin/comments";
        }

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
            return ResponseEntity.status(500).body("Lỗi khi duyệt hàng loạt đánh giá.");
        }
    }

    // 7. Phản hồi đánh giá
    @PostMapping("/admin/comments/reply/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, String>> replyToComment(
            @PathVariable("id") Integer id,
            @RequestBody Map<String, String> payload) {

        String replyText = payload.get("reply");

        if (id == null || id <= 0) {
            return ResponseEntity.badRequest().body(Map.of("message", "ID không hợp lệ."));
        }

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
            return ResponseEntity.status(500).body(Map.of("message", "Lỗi khi xử lý phản hồi."));
        }
    }

    // =====================================
    // === PHẦN USER ===
    // =====================================

    @GetMapping("/comments/public")
    public String listAllComments(Model model) {
        List<CommentsDTO> comments = commentsService.getAllComments();
        model.addAttribute("comments", comments);
        return "comments/list";
    }

    @GetMapping("/comments/product/{id}")
    public String listCommentsByProduct(@PathVariable("id") Integer productId, Model model) {

        if (productId == null || productId <= 0) {
            model.addAttribute("comments", List.of());
            return "comments/product";
        }

        List<CommentsDTO> comments = commentsService.getCommentsByProduct(productId);
        model.addAttribute("comments", comments);
        model.addAttribute("productId", productId);
        return "comments/product";
    }

    @GetMapping("/comments/new")
    public String showCreateForm(Model model) {
        model.addAttribute("comment", new CommentsDTO());
        return "comments/new";
    }

    @PostMapping("/comments/review/save")
    public String saveReview(@ModelAttribute("newComment") CommentsDTO comment, RedirectAttributes ra) {
        Integer currentUserId = getCurrentAuthenticatedUserId();

        if (currentUserId == null) {
            ra.addFlashAttribute("errorMessage", "Vui lòng đăng nhập để đánh giá.");
            return "redirect:/products/" + comment.getProductId() + "#review";
        }

        if (comment.getRate() == null || comment.getRate() < 1 || comment.getRate() > 5) {
            ra.addFlashAttribute("errorMessage", "Vui lòng chọn số sao để gửi Đánh giá.");
            return "redirect:/products/" + comment.getProductId() + "#review";
        }

        boolean hasPurchased = hasUserPurchasedProduct(currentUserId, comment.getProductId());

        if (!hasPurchased) {
            ra.addFlashAttribute("errorMessage", "Bạn chỉ có thể đánh giá sản phẩm sau khi đã mua hàng.");
            return "redirect:/products/" + comment.getProductId() + "#review";
        }

        comment.setUserId(currentUserId);
        comment.setPurchaseVerified(hasPurchased);

        commentsService.createComment(comment);
        ra.addFlashAttribute("successMessage", "Đánh giá của bạn đã được gửi thành công và đang chờ duyệt.");
        return "redirect:/products/" + comment.getProductId() + "#review";
    }

    @PostMapping("/comments/comment-only/save")
    public String saveCommentOnly(@ModelAttribute("newComment") CommentsDTO comment, RedirectAttributes ra) {
        Integer currentUserId = getCurrentAuthenticatedUserId();

        if (currentUserId == null) {
            ra.addFlashAttribute("errorMessage", "Vui lòng đăng nhập để bình luận.");
            return "redirect:/products/" + comment.getProductId() + "#comment";
        }

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

    private Integer getCurrentAuthenticatedUserId() {
        return 1;
    }

    private boolean hasUserPurchasedProduct(Integer userId, Integer productId) {
        return userId != null && userId.equals(1);
    }
}
