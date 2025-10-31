package com.bookhub.user;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

// Import các lớp cần thiết (đảm bảo đúng package của bạn)
import com.bookhub.user.User;
import com.bookhub.user.UserRepository;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * Spring sẽ tự động tiêm (inject) UserRepository vào constructor này
     */
    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Phương thức cốt lõi được gọi bởi Spring Security để tải thông tin người dùng.
     * Giá trị 'email' nhận được từ form đăng nhập (nhờ cấu hình usernameParameter("email")).
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {

        // 1. Tìm User trong DB BẰNG EMAIL
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy người dùng với email: " + email));

        // 2. Lấy Roles từ trường String và chuyển đổi thành Authorities
        Collection<? extends GrantedAuthority> authorities = mapRoleStringToAuthorities(user.getRoles());

        // 3. Trả về đối tượng Spring Security UserDetails
        return new org.springframework.security.core.userdetails.User(
                // Sử dụng Email làm Principal (tên đăng nhập/hiển thị)
                user.getEmail(),
                // Mật khẩu đã được mã hóa BCrypt
                user.getPassword(),
                authorities
        );
    }

    /**
     * Chuyển đổi String Role (VD: "ADMIN") thành Collection<GrantedAuthority> (VD: [ROLE_ADMIN])
     */
    private Collection<? extends GrantedAuthority> mapRoleStringToAuthorities(String roleString) {
        if (roleString == null || roleString.trim().isEmpty()) {
            return Collections.emptyList();
        }

        // Rất quan trọng: Thêm tiền tố "ROLE_" để khớp với hasRole("ADMIN")
        return List.of(new SimpleGrantedAuthority("ROLE_" + roleString.toUpperCase()));
    }
}