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
    String email;
    String gender;
    String phone;
    String roles;
    Boolean isLocked;
    LocalDate createDate;
    List<AddressDTO> addresses;

    public static UserDTO fromEntity(User user) {
        if (user == null) return null;

        List<AddressDTO> addressDtos = null;
        if (user.getAddresses() != null) {
            addressDtos = user.getAddresses().stream()

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
                .isLocked(user.getIsLocked())
                .createDate(user.getCreateDate())
                .addresses(addressDtos)
                .build();
    }

}