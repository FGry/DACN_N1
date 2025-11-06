package com.bookhub.user;

import com.bookhub.address.Address;
import com.bookhub.user.User;
import com.bookhub.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder; // Đã thêm
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Controller
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder; // ĐÃ INJECT PasswordEncoder

    /**
     * Helper: Lấy User Entity đầy đủ từ DB dựa trên Principal (Email) của Spring Security
     */
    private Optional<User> getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated() &&
                !authentication.getPrincipal().equals("anonymousUser")) {

            String email = authentication.getName();
            return userService.findUserByEmail(email);
        }
        return Optional.empty();
    }


    // ===========================================
    // === PHẦN ENDPOINT DÀNH CHO ADMIN (GIỮ NGUYÊN) ===
    // ===========================================

    // ... (Giữ nguyên các endpoint /admin/users, /admin/users/toggle-lock/{id}, etc.)
    // *LƯU Ý*: Các hàm này không thay đổi, chỉ liệt kê để cấu trúc đầy đủ.

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/users")
    public String listUsers(Model model) {
        model.addAttribute("pageTitle", "Quản lý Người dùng");
        model.addAttribute("users", userService.getAllUsers());
        return "admin/user";
    }

    // ... (Các hàm CRUD Admin khác)

    // ====================================================
    // === PHẦN ENDPOINT DÀNH CHO USER/AUTH ===
    // ====================================================

    /** MAPPING: Hiển thị form Đăng ký (GET /register) */
    @GetMapping("/register")
    public String showRegisterForm() {
        return "user/register";
    }

    /** MAPPING: Xử lý Đăng ký (POST /register) */
    @PostMapping("/register")
    public String registerUser(
            @RequestParam("firstName") String firstName,
            @RequestParam("lastName") String lastName,
            @RequestParam("email") String email,
            @RequestParam("phone") String phone,
            @RequestParam("password") String password,
            RedirectAttributes redirectAttributes) {

        if (userService.isEmailExist(email)) {
            redirectAttributes.addFlashAttribute("error", "Email đã tồn tại.");
            return "redirect:/register";
        }

        try {
            userService.registerNewUser(firstName, lastName, email, phone, password);
            redirectAttributes.addFlashAttribute("success", "Đăng ký thành công! Vui lòng đăng nhập.");
            return "redirect:/login";

        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi đăng ký: " + e.getMessage());
            return "redirect:/register";
        }
    }

    /** MAPPING: Hiển thị form Đăng nhập (GET /login) */
    @GetMapping("/login")
    public String showLoginForm() {
        return "login";
    }

    /** MAPPING: Xử lý Đăng xuất */
    @GetMapping("/logout")
    public String logout() {
        return "redirect:/";
    }

    /** MAPPING: Hiển thị trang Hồ sơ cá nhân */
    @GetMapping("/user/profile")
    public String showProfile(Model model, RedirectAttributes redirectAttributes) {
        Optional<User> userOpt = getAuthenticatedUser();

        if (userOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Vui lòng đăng nhập để xem hồ sơ.");
            return "redirect:/login";
        }

        model.addAttribute("user", userOpt.get());
        return "user/profile";
    }

    /** MAPPING: Cập nhật Hồ sơ cá nhân */
    @PostMapping("/user/profile/update")
    public String updateProfile(
            @RequestParam("idUser") Integer idUser,
            @RequestParam("username") String username,
            @RequestParam("email") String email,
            @RequestParam("phone") String phone,
            @RequestParam("gender") String gender,
            RedirectAttributes redirectAttributes) {

        Optional<User> currentUserOpt = getAuthenticatedUser();
        if (currentUserOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Phiên đăng nhập đã hết hạn.");
            return "redirect:/login";
        }

        try {
            Integer currentUserId = currentUserOpt.get().getIdUser();

            // Kiểm tra bảo mật: đảm bảo người dùng chỉ cập nhật hồ sơ của chính họ
            if (!currentUserId.equals(idUser)) {
                throw new RuntimeException("Truy cập trái phép.");
            }

            userService.updateUser(currentUserId, username, email, phone, gender);
            redirectAttributes.addFlashAttribute("success", "Cập nhật thông tin cá nhân thành công!");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi cập nhật: " + e.getMessage());
        }

        return "redirect:/user/profile";
    }

    // ❗ CHÚ Ý: XÓA HOẶC BỎ QUA GET MAPPING /user/address_setup.html NẾU NÓ ĐÃ TỒN TẠI TRƯỚC ĐÓ

    /** MAPPING: Xử lý Lưu Địa chỉ (SỬ DỤNG TRONG TAB SETTINGS) */
    @PostMapping("/user/address/save")
    public String saveAddress(
            @RequestParam("city") String city,
            @RequestParam("district") String district,
            @RequestParam("street") String street,
            @RequestParam(value = "notes", required = false) String notes,
            RedirectAttributes redirectAttributes) {

        Optional<User> userOpt = getAuthenticatedUser();
        if (userOpt.isEmpty()) {
            return "redirect:/login";
        }

        Integer userId = userOpt.get().getIdUser();

        try {
            userService.saveUserAddress(userId, city, district, street, notes);
            // THAY ĐỔI REDIRECT VỀ SETTINGS VÀ DÙNG addressSuccess
            redirectAttributes.addFlashAttribute("addressSuccess", "Địa chỉ đã được cập nhật thành công!");
            return "redirect:/user/profile";
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("addressError", "Lỗi khi cập nhật địa chỉ: " + e.getMessage());
            return "redirect:/user/profile";
        }
    }

    /** MAPPING: Trang Cài đặt/Settings (TẢI DỮ LIỆU CẢ MẬT KHẨU VÀ ĐỊA CHỈ) */
    @GetMapping("/users/setting")
    public String showUserSetting(Model model, RedirectAttributes redirectAttributes) {
        Optional<User> userOpt = getAuthenticatedUser();
        if (userOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Vui lòng đăng nhập.");
            return "redirect:/login";
        }

        User currentUser = userOpt.get();

        // 1. Tải thông tin địa chỉ hiện tại (để điền vào form)
        model.addAttribute("currentCity", "");
        model.addAttribute("currentDistrict", "");
        model.addAttribute("currentStreet", "");
        model.addAttribute("currentNotes", "");
        model.addAttribute("currentUser", currentUser);

        if (currentUser.getFirstAddress() != null) {
            String fullAddress = currentUser.getFirstAddress();

            // Logic phân tích địa chỉ
            Pattern pattern = Pattern.compile("^(.*?), (.*?), (.*?) \\(Ghi chú: (.*)\\)$");
            Matcher matcher = pattern.matcher(fullAddress);

            if (matcher.find()) {
                model.addAttribute("currentStreet", matcher.group(1));
                model.addAttribute("currentDistrict", matcher.group(2));
                model.addAttribute("currentCity", matcher.group(3));
                model.addAttribute("currentNotes", matcher.group(4).equals("không có") ? "" : matcher.group(4));
            } else {
                String[] parts = fullAddress.split(", ");
                if (parts.length >= 3) {
                    model.addAttribute("currentStreet", parts[0]);
                    model.addAttribute("currentDistrict", parts[1]);
                    model.addAttribute("currentCity", parts[2]);
                }
            }
        }

        // 2. Truyền thông báo FlashAttribute
        // Thông báo cho Tab Đổi Mật Khẩu
        if (model.asMap().containsKey("success")) {
            model.addAttribute("successMessage", model.asMap().get("success"));
        }
        if (model.asMap().containsKey("error")) {
            model.addAttribute("errorMessage", model.asMap().get("error"));
        }

        // Thông báo cho Tab Địa Chỉ
        if (model.asMap().containsKey("addressSuccess")) {
            model.addAttribute("addressSuccess", model.asMap().get("addressSuccess"));
        }
        if (model.asMap().containsKey("addressError")) {
            model.addAttribute("addressError", model.asMap().get("addressError"));
        }

        return "user/setting";
    }

    /** MAPPING: Xử lý Thay đổi Mật khẩu Cá nhân */
    @PostMapping("/user/setting/change-password")
    public String changeUserPassword(
            @RequestParam("currentPassword") String currentPassword,
            @RequestParam("newPassword") String newPassword,
            @RequestParam("confirmPassword") String confirmPassword,
            RedirectAttributes redirectAttributes) {

        Optional<User> userOpt = getAuthenticatedUser();
        if (userOpt.isEmpty()) {
            return "redirect:/logout";
        }
        User user = userOpt.get();

        // 1. Kiểm tra Mật khẩu mới và Xác nhận
        if (!newPassword.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Mật khẩu mới và xác nhận mật khẩu không khớp.");
            return "redirect:/users/setting";
        }

        // 2. Kiểm tra Mật khẩu hiện tại
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            redirectAttributes.addFlashAttribute("errorMessage", "Mật khẩu hiện tại không chính xác.");
            return "redirect:/users/setting";
        }

        try {
            // 3. Mã hóa và cập nhật
            String encodedNewPassword = passwordEncoder.encode(newPassword);
            userService.updatePassword(user.getIdUser(), encodedNewPassword); // Dùng updatePassword từ UserService

            redirectAttributes.addFlashAttribute("successMessage", "Thay đổi mật khẩu thành công! Vui lòng đăng nhập lại.");

            // Đăng xuất người dùng để áp dụng mật khẩu mới
            return "redirect:/logout";

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi thay đổi mật khẩu: " + e.getMessage());
        }

        return "redirect:/users/setting";
    }
}