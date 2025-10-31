package com.bookhub.user;

import com.bookhub.address.Address;
import com.bookhub.address.AddressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder; // Đã thêm
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

    // Đã thêm: PasswordEncoder để xử lý hash mật khẩu
    private final PasswordEncoder passwordEncoder;

    @Autowired
    private AddressRepository addressRepository;

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

    public boolean isEmailExist(String email) {
        return userRepository.findByEmail(email).isPresent();
    }

    @Transactional
    public User registerNewUser(String firstName, String lastName, String email, String phone, String password) {
        if (isEmailExist(email)) {
            throw new RuntimeException("Email đã tồn tại.");
        }

        User newUser = new User();
        newUser.setUsername(firstName + " " + lastName);
        // SỬA LỖI BẢO MẬT: Hash mật khẩu trước khi lưu
        newUser.setPassword(passwordEncoder.encode(password));
        newUser.setEmail(email);
        newUser.setPhone(phone);
        newUser.setGender("Other");
        newUser.setRoles("USER");
        // Đã thêm isLocked = false trong Entity User
        LocalDate now = LocalDate.now();
        newUser.setCreateDate(now);
        newUser.setUpdateDate(now);

        return userRepository.save(newUser);
    }

    public Optional<User> authenticate(String email, String rawPassword) {
        Optional<User> userOpt = userRepository.findByEmail(email);

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            // SỬA LỖI BẢO MẬT: So sánh mật khẩu thô với mật khẩu đã hash
            if (passwordEncoder.matches(rawPassword, user.getPassword())) {

                // Spring Security sẽ xử lý việc xác thực chính thức,
                // nhưng phương thức này vẫn được giữ cho tính tương thích với Controller cũ.
                return userOpt;
            }
        }
        return Optional.empty();
    }

    public Optional<User> findUserById(Integer userId) {
        return userRepository.findByIdUser(userId);
    }

    // FIX LỖI CẬP NHẬT: Hàm này giờ sẽ cập nhật địa chỉ nếu đã tồn tại
    @Transactional
    public void saveUserAddress(Integer idUser, String city, String district, String street, String notes) {
        Optional<User> userOpt = userRepository.findByIdUser(idUser);
        if (userOpt.isEmpty()) {
            throw new RuntimeException("Người dùng không tồn tại");
        }

        User user = userOpt.get();
        // FIX: Xử lý ghi chú rỗng/null
        String notesText = (notes != null && !notes.isEmpty()) ? notes : "không có";
        // Ghép chuỗi địa chỉ chi tiết
        String fullAddress = String.format("%s, %s, %s (Ghi chú: %s)", street, district, city, notesText);


        List<Address> userAddresses = user.getAddresses();
        Address addressToSave;

        if (userAddresses != null && !userAddresses.isEmpty()) {
            // FIX: Cập nhật địa chỉ đầu tiên (địa chỉ mặc định)
            addressToSave = userAddresses.get(0);
        } else {
            // Nếu chưa có địa chỉ, tạo mới
            addressToSave = new Address();
            addressToSave.setUser(user); // Thiết lập khóa ngoại chỉ khi tạo mới
        }

        // Cập nhật thông tin
        addressToSave.setFullAddressDetail(fullAddress);
        addressToSave.setPhone(user.getPhone()); // Luôn cập nhật SĐT từ User

        // Lưu (hoặc cập nhật) vào Repository của Address
        addressRepository.save(addressToSave);

        // Cập nhật ngày tháng cho User
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
}