package com.bookhub;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import jakarta.servlet.http.HttpServletResponse;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

	@Bean
	BCryptPasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
				.authorizeHttpRequests(auth -> auth
						// 1. CÁC ĐƯỜNG DẪN CÔNG KHAI
						.requestMatchers(
								// FIX LỖI ẢNH: Cho phép truy cập vào các đường dẫn tài nguyên tĩnh
								"/css/**", "/js/**", "/images/**", "/webjars/**",
								"/uploads/**",

								"/",
								"/login",
								"/register",
								"/products/**",
								"/api/orders"
						).permitAll()

						// 2. CÁC ĐƯỜNG DẪN CỦA USER (Yêu cầu phải ĐĂNG NHẬP)
						.requestMatchers(
								"/user/**",
								"/api/users/**",
								"/api/orders/detail/**"
						).authenticated()

						// 3. QUY TẮC ADMIN: Chỉ ADMIN mới truy cập /admin/**
						.requestMatchers("/admin/**").hasRole("ADMIN")

						// 4. Các request còn lại: Yêu cầu xác thực (Fallback)
						.anyRequest().authenticated()
				)

				// --- CẤU HÌNH LOGIN ---
				.formLogin(form -> form
						.loginPage("/login")
						.usernameParameter("email")
						.successHandler((request, response, authentication) -> {
							// Kiểm tra vai trò để chuyển hướng
							boolean isAdmin = authentication.getAuthorities().stream()
									.anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

							if (isAdmin) {
								response.sendRedirect("/admin/home");
							} else {
								response.sendRedirect("/");
							}
						})
						.permitAll()
				)

				// --- CẤU HÌNH LOGOUT ---
				.logout(logout -> logout
						.logoutRequestMatcher(new AntPathRequestMatcher("/logout"))
						.logoutSuccessUrl("/")
						.invalidateHttpSession(true)  // FIX: Hủy phiên HTTP
						.clearAuthentication(true)    // FIX: Xóa ngữ cảnh xác thực
						.deleteCookies("JSESSIONID")  // NÊN CÓ
						.permitAll()
				)

				// --- XỬ LÝ NGOẠI LỆ ---
				.exceptionHandling(exceptions -> exceptions
						.accessDeniedHandler((request, response, accessDeniedException) -> {
							String requestUri = request.getRequestURI();
							if (requestUri.startsWith("/admin/")) {
								response.sendError(HttpServletResponse.SC_NOT_FOUND);
							} else {
								response.sendError(HttpServletResponse.SC_FORBIDDEN);
							}
						})
				)
				.csrf(csrf -> csrf.disable());

		return http.build();
	}
}