package com.bookhub.user;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public Optional<User> findUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public List<UserDTO> getAllUsers() {
        // Giả định UserDTO.fromEntity(User) tồn tại
        return userRepository.findAll().stream()
                .map(UserDTO::fromEntity)
                .collect(Collectors.toList());
    }

    public UserDTO getUserById(Integer id) {
        User user = userRepository.findByIdWithAddresses(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng với ID: " + id));

        // Giả định UserDTO.fromEntity(User) tồn tại
        return UserDTO.fromEntity(user);
    }

    @Transactional
    public void toggleLockUser(Integer id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng với ID: " + id));

        user.setIsLocked(!user.getIsLocked());
        user.setUpdateDate(LocalDate.now());
        userRepository.save(user);
    }

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

    public boolean isEmailExist(String email) {
        return userRepository.findByEmail(email).isPresent();
    }

    @Transactional
    public void updatePassword(Integer idUser, String encodedPassword) {
        User user = userRepository.findById(idUser)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));

        user.setPassword(encodedPassword);
        user.setUpdateDate(LocalDate.now());
        userRepository.save(user);
    }

    @Transactional
    public User registerNewUser(String firstName, String lastName, String email, String phone, String password) {
        if (isEmailExist(email)) {
            throw new RuntimeException("Email đã tồn tại.");
        }

        User newUser = new User();
        newUser.setUsername(firstName + " " + lastName);
        newUser.setPassword(passwordEncoder.encode(password)); // MÃ HÓA BCrypt
        newUser.setEmail(email);
        newUser.setPhone(phone);
        newUser.setGender("Other");
        newUser.setRoles("USER");
        LocalDate now = LocalDate.now();
        newUser.setCreateDate(now);
        newUser.setUpdateDate(now);

        return userRepository.save(newUser);
    }

    public Optional<User> findUserById(Integer userId) {
        // Giả định UserRepository có phương thức findByIdUser(Integer)
        return userRepository.findByIdUser(userId);
    }


    @Transactional
    public User updateUser(Integer idUser, String username, String email, String phone, String gender) {
        Optional<User> userOpt = userRepository.findByIdUser(idUser);
        if (userOpt.isEmpty()) {
            throw new RuntimeException("Người dùng không tồn tại");
        }

        User user = userOpt.get();
        user.setUsername(username);
        user.setPhone(phone);
        user.setGender(gender);
        user.setUpdateDate(LocalDate.now());

        return userRepository.save(user);
    }
}