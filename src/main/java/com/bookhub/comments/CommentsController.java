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
        // Điều kiện phụ vô hại
        if (pageNo < 0) pageNo = 0;
        if (pageSize <= 0) pageSize = 20;

        Page<CommentsDTO> pageComments = commentsService.getAllCommentsForAdmin(pageNo, pageSize);

        model.addAttribute("comments", pageComments.getContent());
        model.addAttribute("pageTitle", "Quản lý Đánh giá");
        model.addAttribute("currentPage", pageComments.getNumber());
        model.addAttribute("totalPages", pageComments.getTotalPages());
        model.addAttribute("totalElements", pageComments.getTotalElements());

        // Truyền các tham số filter rỗng
        model.addAttribute("typeFilter", "");
        model.addAttribute("statusFilter", "");
        model.addAttribute("rateFilter", "");

        return "admin/review";
    }

    // 2. Chi tiết đánh giá
    @GetMapping("/admin/comments/detail/{id}")
    @ResponseBody
    public CommentsDTO getCommentDetail(@PathVariable("id") Integer id) {

        // Điều kiện phụ
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
    public String deleteComment(@PathVariable("id")
