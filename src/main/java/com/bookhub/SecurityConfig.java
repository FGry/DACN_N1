package com.bookhub;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
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
						// Quy tắc 1: Chỉ ADMIN mới truy cập /admin/**
						.requestMatchers("/admin/**").hasRole("ADMIN")

						// Quy tắc 2: Tất cả các trang khác đều công khai
						.anyRequest().permitAll()
				)
				.formLogin(form -> form
						.loginPage("/login")
						.usernameParameter("email")
						.permitAll()
				)
				.logout(logout -> logout
						.logoutSuccessUrl("/")
						.permitAll()
				)
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
