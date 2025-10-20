package com.bookhub.user;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public String listUsers(Model model) {
        model.addAttribute("pageTitle", "Quản lý Người dùng");
        model.addAttribute("users", userService.getAllUsers());
        return "admin/user"; // <<< ĐÃ SỬA: View sẽ tìm file user.html trong thư mục /admin
    }

    @GetMapping("/detail/{id}")
    @ResponseBody
    public UserDTO getUserDetail(@PathVariable Integer id) {
        return userService.getUserById(id);
    }

    @GetMapping("/toggle-lock/{id}")
    public String toggleLockUser(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        try {
            userService.toggleLockUser(id);
            UserDTO user = userService.getUserById(id);
            String action = user.getIsLocked() ? "khóa" : "mở khóa";
            redirectAttributes.addFlashAttribute("successMessage", "Đã " + action + " tài khoản người dùng <strong>" + user.getUsername() + "</strong> thành công!");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/update-role/{id}")
    public String updateRole(@PathVariable Integer id,
                             @RequestParam("newRole") String newRole,
                             RedirectAttributes redirectAttributes) {
        try {
            userService.updateUserRole(id, newRole);
            UserDTO user = userService.getUserById(id);
            redirectAttributes.addFlashAttribute("successMessage", "Đã cập nhật quyền của người dùng <strong>" + user.getUsername() + "</strong> thành **" + newRole + "**.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Cập nhật quyền thất bại: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }
}