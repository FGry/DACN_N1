package com.bookhub.user;

import com.bookhub.address.Address;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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

    /**
     * Helper: Lấy User Entity đầy đủ từ DB dựa trên Principal (Email) của Spring Security
     */
    private Optional<User> getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated() &&
                !authentication.getPrincipal().equals("anonymousUser")) {

            String email = authentication.getName();
            // Đảm bảo UserService có phương thức findUserByEmail
            return userService.findUserByEmail(email);
        }
        return Optional.empty();
    }


    // ===========================================
    // === PHẦN ENDPOINT DÀNH CHO ADMIN ===
    // ===========================================

    // 1. Xem danh sách người dùng
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/users")
    public String listUsers(Model model) {
        model.addAttribute("pageTitle", "Quản lý Người dùng");
        model.addAttribute("users", userService.getAllUsers());
        return "admin/user";
    }

    // 2. Xem chi tiết thông tin người dùng (API)
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/users/detail/{id}")
    @ResponseBody
    public UserDTO getUserDetail(@PathVariable Integer id) {
        return userService.getUserById(id);
    }

    // 3. Khóa/Mở khóa tài khoản
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/users/toggle-lock/{id}")
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

    // 4. Cập nhật quyền người dùng
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/users/update-role/{id}")
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

    // ====================================================
    // === PHẦN ENDPOINT DÀNH CHO USER/AUTH ===
    // ====================================================

    /** MAPPING: Hiển thị form Đăng ký (GET /register) */
    @GetMapping({"/register", "/user/register"}) // <--- ĐÃ SỬA: Thêm đường dẫn /user/register
    public String showRegisterForm() {
        return "user/register";
    }

    /** MAPPING: Xử lý Đăng ký (POST /register) */
    @PostMapping("/register") // Sử dụng /register để tương thích với link trong login.html
    public String registerUser(
            @RequestParam("firstName") String firstName,
            @RequestParam("lastName") String lastName,
            @RequestParam("email") String email,
            @RequestParam("phone") String phone,
            @RequestParam("password") String password,
            RedirectAttributes redirectAttributes) {

        if (userService.isEmailExist(email)) {
            redirectAttributes.addFlashAttribute("error", "Email đã tồn tại.");
            // Giữ nguyên /register để khớp với mapping GET
            return "redirect:/register";
        }

        try {
            userService.registerNewUser(firstName, lastName, email, phone, password);
            redirectAttributes.addFlashAttribute("success", "Đăng ký thành công! Vui lòng đăng nhập.");
            return "redirect:/login";

        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi đăng ký: " + e.getMessage());
            // Giữ nguyên /register để khớp với mapping GET
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
    public String logout() { // Xóa tham số HttpSession session
        // Để Spring Security xử lý quá trình đăng xuất. Mặc định nó sẽ chuyển hướng sau khi logout.
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

    /** MAPPING: Hiển thị form Thiết lập Địa chỉ */
    @GetMapping("/user/address_setup.html")
    public String showAddressSetup(Model model, RedirectAttributes redirectAttributes) {
        Optional<User> userOpt = getAuthenticatedUser();
        if (userOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Vui lòng đăng nhập để thiết lập địa chỉ.");
            return "redirect:/login";
        }

        User currentUser = userOpt.get();

        model.addAttribute("currentStreet", "");
        model.addAttribute("currentDistrict", "");
        model.addAttribute("currentCity", "");
        model.addAttribute("currentNotes", "");

        // Logic phân tích địa chỉ
        if (currentUser.getFirstAddress() != null) {
            String fullAddress = currentUser.getFirstAddress();
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

        return "user/address_setup";
    }

    /** MAPPING: Xử lý Lưu Địa chỉ */
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
            redirectAttributes.addFlashAttribute("success", "Địa chỉ đã được cập nhật thành công.");
            return "redirect:/";
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi khi cập nhật địa chỉ: " + e.getMessage());
            return "redirect:/user/address_setup.html";
        }
    }

    /** MAPPING: Trang Cài đặt/Settings */
    @GetMapping("/users/setting")
    public String profile() {
        return "user/setting";
    }
}