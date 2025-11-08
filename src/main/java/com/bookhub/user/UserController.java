package com.bookhub.user;

import com.bookhub.address.AddressService;
import com.bookhub.address.AddressDTO;
import com.bookhub.order.OrderService;
import com.bookhub.order.OrderDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

@Controller
@RequiredArgsConstructor
public class UserController {

    // Dependency Injection
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final OrderService orderService;
    private final AddressService addressService; // Giữ lại để tải dữ liệu hiển thị

    /**
     * Helper: Lấy User Entity đầy đủ từ DB dựa trên Principal (Email) của Spring Security
     */
    private Optional<User> getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // Kiểm tra xem đã được xác thực và không phải là người dùng ẩn danh
        if (authentication != null && authentication.isAuthenticated() &&
                !authentication.getPrincipal().equals("anonymousUser")) {

            String email = authentication.getName();
            return userService.findUserByEmail(email);
        }
        return Optional.empty();
    }

    // --- MAPPINGS ADMIN ---
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/users")
    public String listUsers(Model model) {
        model.addAttribute("pageTitle", "Quản lý Người dùng");
        // Giả định UserDTO.fromEntity(User) tồn tại
        model.addAttribute("users", userService.getAllUsers());
        return "admin/user";
    }

    // --- MAPPINGS ĐĂNG KÝ/ĐĂNG NHẬP/ĐĂNG XUẤT ---

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


    /** MAPPING: Trang Cài đặt Người dùng (Hồ sơ, Mật khẩu, Địa chỉ, Đơn hàng) */
    @GetMapping("/user/profile")
    public String showUserSetting(Model model, RedirectAttributes redirectAttributes) {
        Optional<User> userOpt = getAuthenticatedUser();
        if (userOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Vui lòng đăng nhập.");
            return "redirect:/login";
        }

        User currentUser = userOpt.get();
        Integer currentUserId = currentUser.getIdUser();

        // 1. Dữ liệu Hồ sơ
        model.addAttribute("currentUser", currentUser);

        // 2. Dữ liệu Địa chỉ
        List<AddressDTO> userAddresses = addressService.getAddressesByUserId(currentUserId);
        model.addAttribute("userAddresses", userAddresses);

        // Cung cấp dữ liệu cho form thêm/sửa địa chỉ mới
        model.addAttribute("newAddressDetail", "");
        model.addAttribute("newAddressPhone", currentUser.getPhone());

        // Truyền các giá trị trống hoặc giả định cho các trường chi tiết mà template mong muốn
        model.addAttribute("currentCity", "");
        model.addAttribute("currentDistrict", "");
        model.addAttribute("currentStreet", userAddresses.isEmpty() ? "" : userAddresses.get(0).getFullAddressDetail());
        model.addAttribute("currentNotes", "");

        // 3. Dữ liệu Lịch sử Đơn hàng (Cần ID cho Javascript AJAX)
        model.addAttribute("currentUserId", currentUserId);

        // 4. Truyền thông báo FlashAttribute (Đã Sửa lỗi xung đột tên biến)

        // Tab Hồ sơ Cá nhân (profile-settings)
        if (model.asMap().containsKey("profileSuccess")) {
            model.addAttribute("profileSuccess", model.asMap().get("profileSuccess"));
        }
        if (model.asMap().containsKey("profileError")) {
            model.addAttribute("profileError", model.asMap().get("profileError"));
        }

        // Tab Đổi Mật Khẩu (security-settings)
        if (model.asMap().containsKey("securitySuccess")) {
            model.addAttribute("securitySuccess", model.asMap().get("securitySuccess"));
        }
        if (model.asMap().containsKey("securityError")) {
            model.addAttribute("securityError", model.asMap().get("securityError"));
        }

        // Tab Địa chỉ (address-settings)
        if (model.asMap().containsKey("addressSuccess")) {
            model.addAttribute("addressSuccess", model.asMap().get("addressSuccess"));
        }
        if (model.asMap().containsKey("addressError")) {
            model.addAttribute("addressError", model.asMap().get("addressError"));
        }

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

            if (!currentUserId.equals(idUser)) {
                throw new RuntimeException("Truy cập trái phép.");
            }

            userService.updateUser(currentUserId, username, email, phone, gender);
            redirectAttributes.addFlashAttribute("profileSuccess", "Cập nhật thông tin cá nhân thành công!");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("profileError", "Lỗi cập nhật: " + e.getMessage());
        }

        return "redirect:/user/profile#profile-settings";
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
            redirectAttributes.addFlashAttribute("securityError", "Mật khẩu mới và xác nhận mật khẩu không khớp.");
            return "redirect:/user/profile#security-settings";
        }

        // 2. Kiểm tra Mật khẩu hiện tại
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            redirectAttributes.addFlashAttribute("securityError", "Mật khẩu hiện tại không chính xác.");
            return "redirect:/user/profile#security-settings";
        }

        try {
            // 3. Mã hóa và cập nhật
            String encodedNewPassword = passwordEncoder.encode(newPassword);
            userService.updatePassword(user.getIdUser(), encodedNewPassword);

            redirectAttributes.addFlashAttribute("securitySuccess", "Thay đổi mật khẩu thành công! Vui lòng đăng nhập lại.");
            return "redirect:/logout"; // Thường redirect về logout sau khi đổi mật khẩu thành công

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("securityError", "Lỗi khi thay đổi mật khẩu: " + e.getMessage());
        }

        return "redirect:/user/profile#security-settings";
    }

}