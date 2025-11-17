package com.bookhub.controller;

import com.bookhub.user.User;
import com.bookhub.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminSettingController {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder; // Dùng để kiểm tra và mã hóa mật khẩu

    private Optional<User> getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && !authentication.getPrincipal().equals("anonymousUser")) {
            String email = authentication.getName();
            // Sử dụng hàm đã thêm vào UserService
            return userService.findUserByEmail(email);
        }
        return Optional.empty();
    }
    @GetMapping("/setting")
    @PreAuthorize("hasRole('ADMIN')")
    public String showSettingsPage(Model model, RedirectAttributes redirectAttributes) {

        Optional<User> userOpt = getAuthenticatedUser();
        if (userOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy thông tin quản trị viên.");
            return "redirect:/logout";
        }

        User admin = userOpt.get();
        model.addAttribute("admin", new Object() {
            public String getFullName() { return admin.getUsername(); }
            public String getEmail() { return admin.getEmail(); }
            public String getPhone() { return admin.getPhone(); }
            public String getRole() { return admin.getRoles(); }
        });

        model.addAttribute("pageTitle", "Cài đặt & Bảo mật");

        // Truyền thông báo FlashAttribute (nếu có sau khi POST)
        if (model.asMap().containsKey("success")) {
            model.addAttribute("successMessage", model.asMap().get("success"));
        }
        if (model.asMap().containsKey("error")) {
            model.addAttribute("errorMessage", model.asMap().get("error"));
        }

        return "admin/setting";
    }

    // --- 2. Xử lý Cập nhật Hồ sơ Admin ---
    @PostMapping("/setting/profile/update")
    @PreAuthorize("hasRole('ADMIN')")
    public String updateAdminProfile(
            @RequestParam("fullName") String fullName,
            @RequestParam("phoneNumber") String phone,
            RedirectAttributes redirectAttributes) {

        Optional<User> userOpt = getAuthenticatedUser();
        if (userOpt.isEmpty()) {
            return "redirect:/logout";
        }

        User admin = userOpt.get();

        try {
            userService.updateUser(admin.getIdUser(), fullName, admin.getEmail(), phone, admin.getGender());
            redirectAttributes.addFlashAttribute("success", "Cập nhật hồ sơ thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi cập nhật hồ sơ: " + e.getMessage());
        }

        return "redirect:/admin/setting";
    }

    @PostMapping("/setting/security/change-password")
    @PreAuthorize("hasRole('ADMIN')")
    public String changeAdminPassword(
            @RequestParam("currentPassword") String currentPassword,
            @RequestParam("newPassword") String newPassword,
            @RequestParam("confirmPassword") String confirmPassword,
            RedirectAttributes redirectAttributes) {

        Optional<User> userOpt = getAuthenticatedUser();
        if (userOpt.isEmpty()) {
            return "redirect:/logout";
        }
        User admin = userOpt.get();
        if (!newPassword.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("error", "Mật khẩu mới và xác nhận mật khẩu không khớp.");
            return "redirect:/admin/setting";
        }
        if (!passwordEncoder.matches(currentPassword, admin.getPassword())) {
            redirectAttributes.addFlashAttribute("error", "Mật khẩu hiện tại không chính xác.");
            return "redirect:/admin/setting";
        }
        try {
            String encodedNewPassword = passwordEncoder.encode(newPassword);
            userService.updatePassword(admin.getIdUser(), encodedNewPassword);

            redirectAttributes.addFlashAttribute("success", "Thay đổi mật khẩu thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi khi thay đổi mật khẩu.");
        }

        return "redirect:/admin/setting";
    }
}