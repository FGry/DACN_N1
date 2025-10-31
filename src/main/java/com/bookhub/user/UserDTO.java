package com.bookhub.user;

import com.bookhub.address.AddressDTO;
import com.bookhub.address.Address; // Cần import Address Entity để thực hiện mapping
import lombok.*;
import lombok.experimental.FieldDefaults;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserDTO {

    Integer idUser;
    String username;
    // Đã loại bỏ 'password' vì lý do bảo mật
    String email;
    String gender;
    String phone;
    String roles;
    Boolean isLocked; // Trường đã thêm
    LocalDate createDate;
    List<AddressDTO> addresses;

    // --- PHƯƠNG THỨC CHUYỂN ĐỔI ---

    /**
     * Phương thức tĩnh chuyển đổi từ User Entity sang UserDTO.
     * Lưu ý: Cần đảm bảo Address Entity có phương thức fromEntity(Address address).
     */
    public static UserDTO fromEntity(User user) {
        if (user == null) return null;

        // Kiểm tra Address có bị Lazy Load hay không
        List<AddressDTO> addressDtos = null;
        if (user.getAddresses() != null) {
            addressDtos = user.getAddresses().stream()
                    // Giả định bạn có một phương thức tĩnh 'fromEntity' trong AddressDTO
                    // Nếu không, bạn cần tạo lớp Mapper riêng hoặc sửa AddressDTO.java
                    .map(AddressDTO::fromEntity)
                    .collect(Collectors.toList());
        }

        return UserDTO.builder()
                .idUser(user.getIdUser())
                .username(user.getUsername())
                .email(user.getEmail())
                .gender(user.getGender())
                .phone(user.getPhone())
                .roles(user.getRoles())
                .isLocked(user.getIsLocked()) // Ánh xạ trường isLocked
                .createDate(user.getCreateDate())
                .addresses(addressDtos)
                .build();
    }

    // Phương thức giả định cho AddressDTO (Nếu bạn chưa thêm vào AddressDTO.java):
    // Bạn cần đảm bảo đã thêm: public static AddressDTO fromEntity(Address address) { ... } vào AddressDTO.java
}