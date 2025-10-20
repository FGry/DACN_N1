package com.bookhub.comments;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@Controller
@RequestMapping("/admin/comments") // ✅ ĐÃ ĐỔI Ở ĐÂY
@RequiredArgsConstructor
public class CommentsController {

    private final CommentsService commentsService;

    /** Hiển thị danh sách tất cả đánh giá (Admin) */
    @GetMapping
    public String listAllCommentsForAdmin(Model model) {
        List<CommentsDTO> comments = commentsService.getAllComments();
        model.addAttribute("comments", comments);
        model.addAttribute("pageTitle", "Quản lý Đánh giá");
        return "admin/review";
    }

    /** Chi tiết đánh giá  */
    @GetMapping("/detail/{id}")
    @ResponseBody
    public CommentsDTO getCommentDetail(@PathVariable("id") Integer id) {
        return commentsService.getCommentById(id);
    }

    /** Duyệt/Đăng đánh giá */
    @GetMapping("/publish/{id}")
    public String publishComment(@PathVariable("id") Integer id, RedirectAttributes ra) {
        try {
            commentsService.updateCommentStatus(id, "PUBLISHED");
            ra.addFlashAttribute("successMessage", "Đánh giá #DG" + id + " đã được Duyệt (Đăng) thành công!");
        } catch (NoSuchElementException e) {
            ra.addFlashAttribute("errorMessage", "Không tìm thấy đánh giá: " + e.getMessage());
        }
        return "redirect:/admin/comments";
    }

    /** Ẩn đánh giá */
    @GetMapping("/hide/{id}")
    public String hideComment(@PathVariable("id") Integer id, RedirectAttributes ra) {
        try {
            commentsService.updateCommentStatus(id, "HIDDEN");
            ra.addFlashAttribute("successMessage", "Đánh giá #DG" + id + " đã được Ẩn thành công!");
        } catch (NoSuchElementException e) {
            ra.addFlashAttribute("errorMessage", "Không tìm thấy đánh giá: " + e.getMessage());
        }
        return "redirect:/admin/comments";
    }

    /** Xóa đánh giá */
    @GetMapping("/delete/{id}")
    public String deleteComment(@PathVariable("id") Integer id, RedirectAttributes ra) {
        try {
            commentsService.deleteComment(id);
            ra.addFlashAttribute("successMessage", "Đánh giá #DG" + id + " đã được Xóa thành công!");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "Lỗi khi xóa đánh giá: " + e.getMessage());
        }
        return "redirect:/admin/comments";
    }

    /** Gửi phản hồi của admin */
    @PostMapping("/reply/{id}")
    public ResponseEntity<String> replyToComment(@PathVariable("id") Integer id,
                                                 @RequestBody Map<String, String> requestBody) {
        try {
            String replyText = requestBody.get("reply");
            if (replyText == null || replyText.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Nội dung phản hồi không được để trống.");
            }
            commentsService.replyToComment(id, replyText);
            return ResponseEntity.ok("Phản hồi đã được gửi thành công.");
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            System.err.println("Error replying to comment: " + e.getMessage());
            return ResponseEntity.status(500).body("Lỗi khi xử lý phản hồi.");
        }
    }



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
        return "redirect:/products/" + comment.getProductId();
    }
}
