package com.bookhub.controller;

import com.bookhub.category.CategoryRepository;
import com.bookhub.product.ProductService;
import com.bookhub.product.ProductServiceImpl;
import com.bookhub.user.UserRepository;
import com.bookhub.user.UserService; // Giả sử Service cho User
import com.bookhub.order.OrderService; // Giả sử Service cho Order
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;



@Controller
public class HomeController {

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final ProductServiceImpl productServiceImpl;
    private final UserService userService; // Thêm Service
    private final OrderService orderService; // Thêm Service

    // Constructor được Lombok hoặc bạn viết tay tự động điền các dependency trên
    public HomeController(UserRepository userRepository,
                          CategoryRepository categoryRepository,
                          ProductServiceImpl productServiceImpl,
                          UserService userService,
                          OrderService orderService) {
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.productServiceImpl = productServiceImpl;
        this.userService = userService;
        this.orderService = orderService;
    }


    @GetMapping({"/", "/index", "/mainInterface.html"})
    public String home(Model model) {
        model.addAttribute("allCategories", categoryRepository.findAll());
        model.addAttribute("products", productServiceImpl.findAllProducts());
        return "mainInterface";
    }

    @GetMapping("/admin/home")
    public String homeadmin(Model model) {


        model.addAttribute("totalUsers", userService.countTotalUsers());
        model.addAttribute("totalSales", orderService.countTotalSales());
        model.addAttribute("totalRevenue", orderService.calculateTotalRevenue());
        model.addAttribute("totalOrders", orderService.countNewOrdersThisMonth());

        model.addAttribute("salesByCategoryData", orderService.getSalesByCategory());


        model.addAttribute("recentActivities", userService.getRecentActivities());
        model.addAttribute("topSellingProducts", productServiceImpl.getTopSellingProducts());



        return "admin/home";
    }
}