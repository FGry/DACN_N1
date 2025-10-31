package com.bookhub.user;

import com.bookhub.address.Address;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Controller
@RequiredArgsConstructor
// Bỏ @RequestMapping("/admin/users") để cho phép sử dụng các đường dẫn /admin/users và /users
public class UserController {

    private final UserService userService; // Đảm bảo UserService đã được định nghĩa
    private static final String USER_SESSION_KEY = "currentUserId";

    /**
     * Helper: Thiết lập thông tin người dùng vào Model.
     */
    private void setUserInfoToModel(HttpSession session, Model model) {
        Integer userId = (Integer) session.getAttribute(USER_SESSION_KEY);
        if (userId != null) {
            Optional<User> userOpt = userService.findUserById(userId);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                model.addAttribute("isLoggedIn", true);
                model.addAttribute("currentUser", user);
            } else {
                session.invalidate();
                model.addAttribute("isLoggedIn", false);
            }
        } else {
            model.addAttribute("isLoggedIn", false);
        }
    }

    // ===========================================
    // === PHẦN ENDPOINT DÀNH CHO ADMIN (GIỮ NGUYÊN) ===
    // ===========================================

    @GetMapping("/admin/users")
    public String listUsers(Model model) {
        model.addAttribute("pageTitle", "Quản lý Người dùng");
        model.addAttribute("users", userService.getAllUsers());
        return "admin/user";
    }

    @GetMapping("/admin/users/detail/{id}")
    @ResponseBody
    public UserDTO getUserDetail(@PathVariable Integer id) {
        return userService.getUserById(id);
    }

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

    @GetMapping({"/", "/index", "/mainInterface.html"})
    public String home(HttpSession session, Model model) {
        setUserInfoToModel(session, model);
        return "mainInterface";
    }

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
            RedirectAttributes redirectAttributes,
            HttpSession session) {

        if (userService.isEmailExist(email)) {
            redirectAttributes.addFlashAttribute("error", "Email đã tồn tại.");
            return "redirect:/register"; // Đã sửa: Redirect về GET /register
        }

        try {
            User registeredUser = userService.registerNewUser(
                    firstName, lastName, email, phone, password
            );

            session.setAttribute(USER_SESSION_KEY, registeredUser.getIdUser());
            redirectAttributes.addFlashAttribute("success", "Đăng ký thành công! Vui lòng nhập địa chỉ.");

            return "redirect:/user/address_setup.html"; // Giữ lại /user/ cho các trang sau đăng ký

        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi đăng ký: " + e.getMessage());
            return "redirect:/register"; // Đã sửa: Redirect về GET /register
        }
    }

    /** MAPPING: Hiển thị form Đăng nhập */
    @GetMapping("/login")
    public String showLoginForm(HttpSession session) {
        if (session.getAttribute(USER_SESSION_KEY) != null) {
            return "redirect:/";
        }
        return "login";
    }

    /** MAPPING: Xử lý Đăng nhập */
    @PostMapping("/login")
    public String loginUser(
            @RequestParam("email") String email,
            @RequestParam("password") String password,
            RedirectAttributes redirectAttributes,
            HttpSession session) {

        Optional<User> userOpt = userService.authenticate(email, password);

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            session.setAttribute(USER_SESSION_KEY, user.getIdUser());
            redirectAttributes.addFlashAttribute("success", "Đăng nhập thành công!");
            return "redirect:/";
        } else {
            redirectAttributes.addFlashAttribute("error", "Email hoặc mật khẩu không chính xác.");
            return "redirect:/login";
        }
    }

    /** MAPPING: Xử lý Đăng xuất */
    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/";
    }

    /** MAPPING: Hiển thị trang Hồ sơ cá nhân */
    @GetMapping("/user/profile")
    public String showProfile(HttpSession session, Model model) {
        Integer userId = (Integer) session.getAttribute(USER_SESSION_KEY);
        if (userId == null) {
            return "redirect:/login";
        }

        Optional<User> userOpt = userService.findUserById(userId);
        if (userOpt.isEmpty()) {
            session.invalidate();
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
            RedirectAttributes redirectAttributes,
            HttpSession session) {

        Integer currentUserId = (Integer) session.getAttribute(USER_SESSION_KEY);
        if (currentUserId == null || !currentUserId.equals(idUser)) {
            redirectAttributes.addFlashAttribute("error", "Bạn không có quyền chỉnh sửa hồ sơ này.");
            return "redirect:/login";
        }

        try {
            userService.updateUser(idUser, username, email, phone, gender);
            redirectAttributes.addFlashAttribute("success", "Cập nhật thông tin cá nhân thành công!");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi cập nhật: " + e.getMessage());
        }

        return "redirect:/user/profile";
    }

    /** MAPPING: Hiển thị form Thiết lập Địa chỉ */
    @GetMapping("/user/address_setup.html")
    public String showAddressSetup(HttpSession session, Model model) {
        Integer userId = (Integer) session.getAttribute(USER_SESSION_KEY);
        if (userId == null) {
            return "redirect:/login";
        }
        setUserInfoToModel(session, model);

        User currentUser = (User) model.getAttribute("currentUser");

        model.addAttribute("currentStreet", "");
        model.addAttribute("currentDistrict", "");
        model.addAttribute("currentCity", "");
        model.addAttribute("currentNotes", "");

        if (currentUser != null && currentUser.getFirstAddress() != null) {
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

        return "user/address_setup"; // Trả về view: /templates/user/address_setup.html
    }

    /** MAPPING: Xử lý Lưu Địa chỉ */
    @PostMapping("/user/address/save")
    public String saveAddress(
            @RequestParam("city") String city,
            @RequestParam("district") String district,
            @RequestParam("street") String street,
            @RequestParam(value = "notes", required = false) String notes,
            RedirectAttributes redirectAttributes,
            HttpSession session) {

        Integer userId = (Integer) session.getAttribute(USER_SESSION_KEY);
        if (userId == null) {
            return "redirect:/login";
        }

        try {
            userService.saveUserAddress(userId, city, district, street, notes);
            redirectAttributes.addFlashAttribute("success", "Địa chỉ đã được cập nhật thành công.");
            return "redirect:/";
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi khi cập nhật địa chỉ: " + e.getMessage());
            return "redirect:/user/address_setup.html";
        }
    }

    /** MAPPING: Trang Cài đặt/Settings (Giữ nguyên) */
    @GetMapping("/users/setting")
    public String profile() {
        return "user/setting";
    }
}