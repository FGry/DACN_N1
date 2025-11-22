package com.bookhub.voucher; // (Hoặc package controller của bạn)

import com.bookhub.user.User; // Import User entity
import com.bookhub.user.UserRepository; // Import UserRepository
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.security.Principal;
import java.util.List;
import java.util.Optional; // Cần thiết
import com.bookhub.category.CategoryRepository;

@Controller
@RequiredArgsConstructor
public class UserVoucherController {

    private final VoucherService voucherService;
    private final UserRepository userRepository; // Dùng để tìm kiếm User Entity
    private final CategoryRepository categoryRepository;

    /**
     * Hiển thị trang voucher (voucher.html) cho người dùng đã đăng nhập.
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/user/voucher")
    public String userVoucherPage(Model model, Principal principal) {

        boolean isAuthenticated = (principal != null);

        // 1. Logic kiểm tra trạng thái đăng nhập
        model.addAttribute("isAuthenticated", isAuthenticated);
        model.addAttribute("allCategories", categoryRepository.findAll()); // <<< THÊM 3: Đưa categories vào Model

        if (isAuthenticated) {
            String usernameOrEmail = principal.getName();

            // 2. TÌM KIẾM USER ENTITY ĐẦY ĐỦ để lấy TÊN HIỂN THỊ
            // Giả định UserRepository có phương thức findByEmail (hoặc findByUsername)
            Optional<User> userOptional = userRepository.findByEmail(usernameOrEmail);

            if (userOptional.isPresent()) {
                User user = userOptional.get();
                // 3. Lấy tên hiển thị từ Entity User (Giả định có getFullName())
                // Nếu User Entity của bạn chỉ có firstName và lastName, bạn có thể nối chuỗi:
                // String displayName = user.getFirstName() + " " + user.getLastName();
                String displayName = user.getUsername(); // <-- SỬ DỤNG TÊN HIỂN THỊ
                model.addAttribute("username", displayName);
                model.addAttribute("currentUser", user);
            } else {
                // Trường hợp không tìm thấy User (nên rất hiếm)
                model.addAttribute("username", usernameOrEmail);
            }
        }

        // Lấy danh sách Voucher có sẵn và chuyển thành JSON string
        List<Voucher> availableVouchers = voucherService.getAvailablePublicVouchers();
        String vouchersJson = voucherService.getVouchersAsJson(availableVouchers);

        // Truyền JSON string vào model để dùng trong Thymeleaf/JS
        model.addAttribute("vouchersJson", vouchersJson);

        return "user/voucher";
    }


}