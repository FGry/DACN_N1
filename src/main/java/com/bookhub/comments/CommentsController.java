package com.bookhub.comments;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page; // Thêm import Page
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@Controller
@RequestMapping("/admin/comments")
@RequiredArgsConstructor
public class CommentsController {

    private final CommentsService commentsService;

    /** Hiển thị danh sách tất cả đánh giá (Admin) - CÓ PHÂN TRANG */
    @GetMapping
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

        // Truyền các tham số filter rỗng để Thymeleaf không bị lỗi
        model.addAttribute("typeFilter", "");
        model.addAttribute("statusFilter", "");
        model.addAttribute("rateFilter", "");

        return "admin/review";
    }

    /** Chi tiết đánh giá  */
    @GetMapping("/detail/{id}")
    @ResponseBody
    public CommentsDTO getCommentDetail(@PathVariable("id") Integer id) {
        return commentsService.getCommentById(id);
    }

    /** Duyệt/Đăng đánh giá (Thay đổi trạng thái) */
    @GetMapping("/publish/{id}")
    public String publishComment(@PathVariable("id") Integer id, RedirectAttributes ra) {
        try {
            commentsService.updateCommentStatus(id, "PUBLISHED");
            ra.addFlashAttribute("successMessage", "Đánh giá #" + id + " đã được DUYỆT thành công.");
        } catch (NoSuchElementException e) {
            ra.addFlashAttribute("errorMessage", "Không tìm thấy đánh giá #" + id + ".");
        }
        return "redirect:/admin/comments";
    }

    /** Ẩn/Gỡ đánh giá (Thay đổi trạng thái) */
    @GetMapping("/hide/{id}")
    public String hideComment(@PathVariable("id") Integer id, RedirectAttributes ra) {
        try {
            commentsService.updateCommentStatus(id, "HIDDEN");
            ra.addFlashAttribute("successMessage", "Đánh giá #" + id + " đã được ẨN thành công.");
        } catch (NoSuchElementException e) {
            ra.addFlashAttribute("errorMessage", "Không tìm thấy đánh giá #" + id + ".");
        }
        return "redirect:/admin/comments";
    }

    /** Xóa đánh giá */
    @GetMapping("/delete/{id}")
    public String deleteComment(@PathVariable("id") Integer id, RedirectAttributes ra) {
        try {
            commentsService.deleteComment(id);
            ra.addFlashAttribute("successMessage", "Đánh giá #" + id + " đã được XÓA thành công.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "Lỗi khi xóa đánh giá #" + id + ": " + e.getMessage());
        }
        return "redirect:/admin/comments";
    }

    // --- PHƯƠNG THỨC MỚI: DUYỆT TẤT CẢ ĐANG CHỜ ---
    @PostMapping("/approve-all")
    public ResponseEntity<String> approveAllPendingComments() {
        try {
            // Lệnh SQL update sẽ duyệt cả Bình luận và Đánh giá nếu status = 'PENDING'
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


    /** Phản hồi đánh giá (qua AJAX từ Modal) */
    @PostMapping("/reply/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, String>> replyToComment(
            @PathVariable("id") Integer id,
            @RequestBody Map<String, String> payload) {

        String replyText = payload.get("reply");

        try {
            if (replyText == null || replyText.trim().isEmpty()) {
                // Trả về lỗi 400 Bad Request
                return ResponseEntity.badRequest().body(Map.of("message", "Nội dung phản hồi không được để trống."));
            }
            commentsService.replyToComment(id, replyText);
            return ResponseEntity.ok(Map.of("message", "Phản hồi đã được gửi thành công."));
        } catch (IllegalStateException e) {
            // Lỗi 400 nếu bình luận chưa được duyệt (PUBLISHED)
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            System.err.println("Error replying to comment: " + e.getMessage());
            return ResponseEntity.status(500).body(Map.of("message", "Lỗi khi xử lý phản hồi."));
        }
    }

    // ... các phương thức public và private khác
    @GetMapping("/public")
    public String listAllComments(Model model) {
        List<CommentsDTO> comments = commentsService.getAllComments();
        model.addAttribute("comments", comments);
        return "comments/list";
    }

    @GetMapping("/product/{id}")
    public String listCommentsByProduct(@PathVariable("id") Integer productId, Model model) {
        List<CommentsDTO> comments = commentsService.getCommentsByProduct(productId);
        model.addAttribute("comments", comments);
        model.addAttribute("productId", productId);
        return "comments/product";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("comment", new CommentsDTO());
        return "comments/new";
    }

    @PostMapping("/save")
    public String saveComment(@ModelAttribute("comment") CommentsDTO comment) {
        commentsService.createComment(comment);
        // Chuyển hướng về trang chi tiết sản phẩm sau khi gửi bình luận
        return "redirect:/products/" + comment.getProductId();
    }
}