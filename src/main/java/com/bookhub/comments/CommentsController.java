package com.bookhub.comments;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/comments")
@RequiredArgsConstructor
public class CommentsController {

    private final CommentsService commentsService;

    // Hiển thị tất cả comments (dành cho admin)
    @GetMapping
    public String listAllComments(Model model) {
        List<CommentsDTO> comments = commentsService.getAllComments();
        model.addAttribute("comments", comments);
        return "comments/list"; // file: templates/comments/list.html
    }

    // Hiển thị comment theo sản phẩm
    @GetMapping("/product/{id}")
    public String listCommentsByProduct(@PathVariable("id") Integer productId, Model model) {
        List<CommentsDTO> comments = commentsService.getCommentsByProduct(productId);
        model.addAttribute("comments", comments);
        model.addAttribute("productId", productId);
        return "comments/product"; // file: templates/comments/product.html
    }

    // Hiển thị form thêm comment mới
    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("comment", new CommentsDTO());
        return "comments/new"; // file: templates/comments/new.html
    }

    // Xử lý khi người dùng gửi form thêm comment
    @PostMapping("/save")
    public String saveComment(@ModelAttribute("comment") CommentsDTO comment) {
        commentsService.createComment(comment);
        return "redirect:/comments"; // quay lại danh sách sau khi thêm
    }

    // Xóa comment (chỉ admin hoặc user sở hữu mới được)
    @GetMapping("/delete/{id}")
    public String deleteComment(@PathVariable("id") Integer id) {
        commentsService.deleteComment(id);
        return "redirect:/comments";
    }
}
