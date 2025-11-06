package com.bookhub.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Controller tùy chỉnh để xử lý các mã lỗi HTTP như 404 và 403.
 * Khi response.sendError() được gọi (như trong SecurityConfig),
 * Spring Boot sẽ chuyển hướng đến /error, và Controller này sẽ tiếp nhận.
 */
@Controller
public class CustomErrorController implements ErrorController {

    private static final String PATH = "/error";

    @RequestMapping(PATH)
    public String handleError(HttpServletRequest request) {
        // Lấy mã trạng thái HTTP
        Object status = request.getAttribute(jakarta.servlet.RequestDispatcher.ERROR_STATUS_CODE);

        if (status != null) {
            int statusCode = Integer.parseInt(status.toString());

            // Nếu lỗi là 404, trả về view error/404.html
            if(statusCode == 404) {
                // Trả về tên file view (resources/templates/error/404.html)
                return "error/404";
            }

            // Nếu lỗi là 403, trả về view error/403.html
            if(statusCode == 403) {
                // Trả về tên file view (resources/templates/error/403.html)
                return "error/403";
            }
        }

        // Mặc định, trả về view cho các lỗi khác (ví dụ: 500)
        return "error/error";
    }

    public String getErrorPath() {
        return PATH;
    }
}
