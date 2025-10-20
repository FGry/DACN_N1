package com.bookhub.user;

import com.bookhub.address.AddressDTO;
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
    String email;
    String gender;
    String phone;
    String roles;
    Boolean isLocked;
    LocalDate createDate;
    List<AddressDTO> addresses;

    // Phương thức chuyển đổi từ Entity sang DTO
    public static UserDTO fromEntity(User user) {
        return UserDTO.builder()
                .idUser(user.getIdUser())
                .username(user.getUsername())
                .email(user.getEmail())
                .gender(user.getGender())
                .phone(user.getPhone())
                .roles(user.getRoles())
                .isLocked(user.getIsLocked())
                .createDate(user.getCreateDate())

                .addresses(user.getAddresses().stream()
                        .map(AddressDTO::fromEntity)
                        .collect(Collectors.toList()))
                .build();
    }
}