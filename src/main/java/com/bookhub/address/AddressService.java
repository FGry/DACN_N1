package com.bookhub.address;

import com.bookhub.user.UserService;
import com.bookhub.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) // Mặc định tất cả phương thức là chỉ đọc
public class AddressService {

    private final AddressRepository addressRepository;
    private final UserService userService;
    // Loại bỏ EntityManager vì không cần thiết cho CRUD cơ bản.

    public List<AddressDTO> getAddressesByUserId(Integer userId) {
        // Sử dụng phương thức tùy chỉnh của JpaRepository
        List<Address> addresses = addressRepository.findByUser_IdUser(userId);

        // Chuyển đổi từ Entity sang DTO
        return addresses.stream()
                .map(AddressDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional // Bật giao dịch ghi
    public AddressDTO saveOrUpdateAddress(AddressDTO dto) {
        // 1. Tìm User
        User user = userService.findUserById(dto.getUserId())
                .orElseThrow(() -> new RuntimeException("User không tồn tại. Vui lòng đăng nhập lại."));

        Address address;
        if (dto.getIdAddress() != null) {
            // 2. Cập nhật địa chỉ hiện có
            address = addressRepository.findById(dto.getIdAddress())
                    .orElseThrow(() -> new RuntimeException("Địa chỉ không tồn tại hoặc đã bị xóa."));
        } else {
            // 3. Thêm địa chỉ mới
            address = new Address();
        }

        // 4. Set dữ liệu
        address.setFullAddressDetail(dto.getFullAddressDetail());
        address.setPhone(dto.getPhone());
        address.setUser(user);

        // 5. Lưu vào CSDL
        Address savedAddress = addressRepository.save(address);
        return AddressDTO.fromEntity(savedAddress);
    }

    @Transactional // Bật giao dịch ghi
    public void deleteAddress(Integer addressId, Integer userId) {
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new RuntimeException("Địa chỉ không tồn tại."));

        // Kiểm tra quyền sở hữu
        if (!address.getUser().getIdUser().equals(userId)) {
            throw new RuntimeException("Truy cập bị từ chối: Bạn không có quyền xóa địa chỉ này.");
        }

        try {
            addressRepository.delete(address);
        } catch (DataIntegrityViolationException e) {
            throw new RuntimeException("Địa chỉ này không thể xóa vì đang được sử dụng trong ít nhất một đơn hàng.");
        }
    }
}