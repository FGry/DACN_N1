package com.bookhub.cart;

import com.bookhub.category.CategoryRepository;
import com.bookhub.user.User;
import com.bookhub.user.UserRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.beans.factory.annotation.Autowired;

import java.security.Principal;
import java.util.Optional;

@Controller
public class UserCartController {

    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;


    @Autowired
    public UserCartController(CategoryRepository categoryRepository, UserRepository userRepository) {
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/user/cart")
    public String userCartPage(Model model, Principal principal) {


        model.addAttribute("allCategories", categoryRepository.findAll());

        boolean isLoggedIn = (principal != null);
        model.addAttribute("isLoggedIn", isLoggedIn);

        if (isLoggedIn) {
            Optional<User> userOptional = userRepository.findByEmail(principal.getName());
            if (userOptional.isPresent()) {
                User user = userOptional.get();
                model.addAttribute("currentUser", user);
                model.addAttribute("username", user.getUsername());
            }
        }


        return "user/cart";
    }
}