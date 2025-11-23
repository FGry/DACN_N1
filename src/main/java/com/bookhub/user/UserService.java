package com.bookhub.user;

import com.bookhub.address.Address;
import com.bookhub.address.AddressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

// Đảm bảo import UserDTO
// import com.bookhub.user.UserDTO;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    private AddressRepository addressRepository;



    public Optional<User> findUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }



    public List<UserDTO> getAllUsers() {
        return userRepository.findAll().stream()
                .map(UserDTO::fromEntity)
                .collect(Collectors.toList());
    }

    public UserDTO getUserById(Integer id) {
        User user = userRepository.findByIdWithAddresses(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng với ID: " + id));

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
        return userRepository.findByIdUser(userId);
    }


    @Transactional
    public void saveUserAddress(Integer idUser, String city, String district, String street, String notes) {
        Optional<User> userOpt = userRepository.findByIdUser(idUser);
        if (userOpt.isEmpty()) {
            throw new RuntimeException("Người dùng không tồn tại");
        }

        User user = userOpt.get();
        String notesText = (notes != null && !notes.isEmpty()) ? notes : "không có";
        String fullAddress = String.format("%s, %s, %s (Ghi chú: %s)", street, district, city, notesText);

        List<Address> userAddresses = user.getAddresses();
        Address addressToSave;

        if (userAddresses != null && !userAddresses.isEmpty()) {
            addressToSave = userAddresses.get(0);
        } else {
            addressToSave = new Address();
            addressToSave.setUser(user);
        }

        addressToSave.setFullAddressDetail(fullAddress);
        addressToSave.setPhone(user.getPhone());

        addressRepository.save(addressToSave);

        user.setUpdateDate(LocalDate.now());
        userRepository.save(user);
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

    @Transactional(readOnly = true)
    public long countTotalUsers() {
        return userRepository.count();
    }

    // THÊM: Lấy hoạt động gần đây (Bạn cần tạo DTO/Class cho Activity)
    @Transactional(readOnly = true)
    public List<UserDTO> getRecentActivities() {

        return List.of();
    }
}