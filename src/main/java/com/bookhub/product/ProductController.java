package com.bookhub.product;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/user")
public class ProductController {
	@GetMapping("/product")
	public String product() {
		return "/user/product";
	}
}
