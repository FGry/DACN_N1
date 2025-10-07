package com.bookhub.voucher;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/user")
public class VoucherController {
	@GetMapping("/voucher")
	public String voucher() {
		return "user/voucher";
	}
}
