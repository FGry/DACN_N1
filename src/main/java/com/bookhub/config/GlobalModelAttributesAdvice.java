package com.bookhub.config;

import com.bookhub.user.UserRepository;
import com.bookhub.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.Optional;

@ControllerAdvice
@RequiredArgsConstructor
public class GlobalModelAttributesAdvice {

    private final UserRepository userRepository;

    /**
     * Phương thức này chạy trước mọi Controller để tự động thêm thông tin người dùng vào Model.
     */
    @ModelAttribute
    public void addGlobalUserAttributes(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null &&
                authentication.isAuthenticated() &&
                !authentication.getPrincipal().equals("anonymousUser")) {

            String email = authentication.getName();
            // Tìm kiếm User Entity đầy đủ
            Optional<User> userOpt = userRepository.findByEmail(email);

            if (userOpt.isPresent()) {
                model.addAttribute("isLoggedIn", true);
                model.addAttribute("currentUser", userOpt.get());
            } else {
                model.addAttribute("isLoggedIn", false);
            }
        } else {
            model.addAttribute("isLoggedIn", false);
        }
    }
}