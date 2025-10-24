package com.bookhub.user;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    // Lấy tất cả người dùng và chuyển đổi sang DTO
    public List<UserDTO> getAllUsers() {
        return userRepository.findAll().stream()
                .map(UserDTO::fromEntity)
                .collect(Collectors.toList());
    }

    // Lấy thông tin chi tiết người dùng
    public UserDTO getUserById(Integer id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng với ID: " + id));
        return UserDTO.fromEntity(user);
    }

    // Khóa/Mở khóa tài khoản
    @Transactional
    public void toggleLockUser(Integer id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng với ID: " + id));

        user.setIsLocked(!user.getIsLocked()); // Đảo ngược trạng thái
        user.setUpdateDate(LocalDate.now());
        userRepository.save(user);
    }

    // Cập nhật quyền người dùng
    @Transactional
    public void updateUserRole(Integer id, String newRole) {
        if (!newRole.equals("ADMIN") && !newRole.equals("USER")) {
            throw new IllegalArgumentException("Quyền không hợp lệ: " + newRole);
        }

        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng với ID: " + id));

        user.setRoles(newRole);
        user.setUpdateDate(LocalDate.now());
        userRepository.save(user);
    }
}