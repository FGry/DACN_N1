package com.bookhub.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class HomeController {
	@RequestMapping("")
	public String home() {
		return "mainInterface";
	}

	@RequestMapping("admin/home")
	public String homeadmin() {
		return "admin/home";
	}
}
