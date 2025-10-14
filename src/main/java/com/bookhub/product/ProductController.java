package com.bookhub.product;

import com.bookhub.comments.CommentsDTO;
import com.bookhub.comments.CommentsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {

	private final ProductService productService;
	private final CommentsService commentsService;


	@GetMapping("/{id}")
	public String showProductDetail(@PathVariable("id") Integer id, Model model) {

		// Gọi Service để lấy chi tiết sản phẩm và các bình luận
		ProductDTO product = productService.getProductDetail(id);

		if (product == null) {
			return "error/404"; // Chuyển hướng đến trang 404 (cần có file 404.html trong templates/error/)
		}

		model.addAttribute("product", product);
		// Chuẩn bị object rỗng cho form thêm comment mới
		model.addAttribute("newComment", new CommentsDTO());

		// Trả về tên template. templates/user/review.html
		return "user/review";
	}
	@PostMapping("/{productId}/comment/save")
	public String saveComment(@PathVariable("productId") Integer productId,
							  @ModelAttribute("newComment") CommentsDTO commentDTO) {


		commentsService.createComment(commentDTO);

		return "redirect:/products/" + productId;
	}
}