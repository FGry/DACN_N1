package com.bookhub.controller;

import com.bookhub.user.UserRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

	private final UserRepository userRepository;

	public HomeController(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	@GetMapping({"/", "/index", "/mainInterface.html"})
	public String home(Model model) {
		// Logic xác thực đã được chuyển sang GlobalModelAttributesAdvice.
		return "mainInterface";
	}

	@GetMapping("/admin/home")
	public String homeadmin() {
		return "admin/home";
	}
}