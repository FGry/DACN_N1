package com.bookhub.cart;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/user")
public class CartController {
	@GetMapping("/cart")
	public String cart() {
		return "/user/cart";
	}
}
