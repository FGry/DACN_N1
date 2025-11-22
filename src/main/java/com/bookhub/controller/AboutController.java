package com.bookhub.controller;

import com.bookhub.category.CategoryRepository;
import com.bookhub.user.User;
import com.bookhub.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.security.Principal;
import java.util.Optional;

@Controller
@RequiredArgsConstructor
public class AboutController {

    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository; // Cần thiết để lấy thông tin User cho Navbar

    @GetMapping("/about")
    public String about(Model model, Principal principal) {

        // --- LOGIC CHUNG CHO NAVBAR ---
        model.addAttribute("allCategories", categoryRepository.findAll());

        boolean isLoggedIn = (principal != null);
        model.addAttribute("isLoggedIn", isLoggedIn);

        if (isLoggedIn) {
            Optional<User> userOptional = userRepository.findByEmail(principal.getName());
            userOptional.ifPresent(user -> model.addAttribute("currentUser", user));
        }
        // --- KẾT THÚC LOGIC CHUNG ---

        return "/user/about";
    }
}